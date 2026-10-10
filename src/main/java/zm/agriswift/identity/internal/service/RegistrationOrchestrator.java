package zm.agriswift.identity.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.security.PiiCipher;
import zm.agriswift.farmer.FarmerOnboarding;
import zm.agriswift.identity.domain.FarmerCredential;
import zm.agriswift.identity.internal.integration.InrisClient;
import zm.agriswift.identity.internal.integration.InrisProfile;
import zm.agriswift.identity.internal.repository.FarmerCredentialRepository;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationOrchestrator {

    private final FarmerOnboarding farmerOnboarding;
    private final InrisClient inrisClient;
    private final FarmerCredentialRepository farmerCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final PiiCipher piiCipher;

    @Transactional
    public UUID registerFarmer(FarmerRegisterRequest request) {

        // NORMALIZE at the write boundary; null-safe (email is optional in the schema).
        // Prevents Postgres UNIQUE violations from case/whitespace differences.
        final String normalizedEmail = request.email() == null
                ? null
                : request.email().trim().toLowerCase(Locale.ROOT);

        // 1. e-KYC
        InrisProfile profile = inrisClient.fetchProfile(request.nrc())
                .orElseThrow(() -> new IllegalArgumentException("NRC not found in national registry."));

        // 2. Farmer module via exposed API
        UUID farmerId = farmerOnboarding.registerSelfRegisteredFarmer(
                new FarmerOnboarding.SelfRegistrationRequest(
                        request.nrc(),
                        request.phone(),
                        profile.firstName(),
                        profile.middleName(),
                        profile.lastName(),
                        profile.dateOfBirth(),
                        normalizedEmail,
                        "ENGLISH"
                )
        );

        // 3. Deterministic blind indexes — raw NRC/phone never touch this table.
        //
        //    FIX (JLS 11.2.3): PiiCipher#generateBlindIndex is Argon2id/BouncyCastle-backed
        //    and declares NO checked exceptions, so catching GeneralSecurityException here
        //    was a compile-time error ("never thrown in body of corresponding try statement").
        //    Argon2id failures surface as unchecked exceptions; translate them at this
        //    boundary so crypto/plumbing faults never escape as raw runtime exceptions.
        final byte[] nationalIdHash;
        final byte[] mobileHash;
        try {
            nationalIdHash = piiCipher.generateBlindIndex(request.nrc());
            mobileHash = request.phone() != null
                    ? piiCipher.generateBlindIndex(request.phone())
                    : null;
        } catch (RuntimeException e) {
            throw new DomainException("Failed to secure PII during registration", e);
        }

        // 4. Credentials
        FarmerCredential credential = FarmerCredential.builder()
                .farmerId(farmerId)
                .nationalIdHash(nationalIdHash)      // byte[] blind index, not raw NRC
                .mobileNumberHash(mobileHash)        // byte[] blind index, not raw MSISDN
                .email(normalizedEmail)
                .pinHash(passwordEncoder.encode(request.pin()))
                .build();

        farmerCredentialRepository.save(credential);

        log.info("Successfully onboarded farmer {} via self-registration portal.", farmerId);
        return farmerId;
    }
}