package zm.agriswift.identity.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.security.PiiCipher;
import zm.agriswift.identity.domain.FarmerCredential;
import zm.agriswift.identity.internal.integration.infrastructure.EmailService;
import zm.agriswift.identity.internal.repository.FarmerCredentialRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_CODE_PREFIX = "agriswift:otp:code:";
    private static final String OTP_LIMIT_PREFIX = "agriswift:otp:limit:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final FarmerCredentialRepository credentialRepository;
    private final PiiCipher piiCipher;

    private final SecureRandom secureRandom = new SecureRandom();

    public void generateAndSendOtp(String identifier) {
        // 1. Resolve the credential (NRC/phone via blind index, email normalized)
        FarmerCredential credential = resolveCredential(identifier);

        // 2. Ensure an email exists for OTP delivery
        String targetEmail = credential.getEmail();
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalStateException("No email address registered for this account. Please use PIN login.");
        }

        // 3. Atomic rate limit per email (SET NX PX) — closes the old hasKey/set race
        String limitKey = OTP_LIMIT_PREFIX + targetEmail;
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(limitKey, "1", RATE_LIMIT))) {
            throw new IllegalStateException("Please wait 60 seconds before requesting another code.");
        }

        // 4. Uniform 000000–999999 (nextInt(bound) is exclusive; 999999 was previously unreachable)
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // Store ONLY the SHA-256 digest: a Redis snapshot leak must not expose live codes
        String codeKey = OTP_CODE_PREFIX + canonicalKey(identifier);
        redisTemplate.opsForValue().set(codeKey, otpDigest(otp), OTP_TTL);

        // 5. Dispatch (log the credential id, never the raw NRC/MSISDN/email)
        emailService.sendOtpEmail(targetEmail, otp);
        log.info("OTP dispatched for credential {} via email on file.", credential.getFarmerId());
    }

    public boolean verifyOtp(String identifier, String providedOtp) {
        if (providedOtp == null) {
            return false;
        }
        String codeKey = OTP_CODE_PREFIX + canonicalKey(identifier);
        String storedDigest = redisTemplate.opsForValue().get(codeKey);
        if (storedDigest == null) {
            return false;
        }
        // Constant-time comparison of digests; single-use on success (replay protection)
        if (MessageDigest.isEqual(storedDigest.getBytes(StandardCharsets.UTF_8),
                otpDigest(providedOtp).getBytes(StandardCharsets.UTF_8))) {
            redisTemplate.delete(codeKey);
            return true;
        }
        return false;
    }

    /**
     * Resolves whatever the farmer typed (NRC, phone, email) to their credential using the
     * deterministic Argon2id blind index computed at registration (or the normalized email).
     *
     * <p>FIX (JLS 11.2.3): {@link PiiCipher#generateBlindIndex(String)} is Argon2id/BouncyCastle
     * backed and declares no checked exceptions, so the old {@code catch (GeneralSecurityException)}
     * was a compile-time error. The try-block is deliberately narrowed to the KDF call: wrapping the
     * repository lookups too would let a {@code catch (RuntimeException)} swallow the
     * "No account found" {@link IllegalArgumentException} and change the API contract.
     */
    private FarmerCredential resolveCredential(String identifier) {
        String canonical = canonicalKey(Objects.requireNonNull(identifier, "identifier"));

        if (canonical.contains("@")) {
            return credentialRepository.findByEmail(canonical)
                    .orElseThrow(() -> new IllegalArgumentException("No account found for this identifier."));
        }

        final byte[] blind;
        try {
            blind = piiCipher.generateBlindIndex(canonical);
        } catch (RuntimeException e) {                 // Argon2id/BC failures are unchecked
            throw new DomainException("Failed to process identifier", e);
        }

        return credentialRepository.findByNationalIdHash(blind)
                .or(() -> credentialRepository.findByMobileNumberHash(blind))
                .orElseThrow(() -> new IllegalArgumentException("No account found for this identifier."));
    }

    /** Same normalization policy as registration — third ad-hoc copy; centralize (see review notes). */
    private String canonicalKey(String identifier) {
        String trimmed = identifier.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
    }

    /** SHA-256 hex digest so OTPs are never at rest in the clear. (SHA-256 is JCA-mandated, so this catch is legal.) */
    static String otpDigest(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available per JCA spec", e);
        }
    }
}