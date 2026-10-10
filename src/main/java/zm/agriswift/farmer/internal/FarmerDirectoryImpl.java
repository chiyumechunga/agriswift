package zm.agriswift.farmer.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.security.PiiCipher;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.farmer.api.dto.PreferredPayoutAccount;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.FarmerPaymentAccount;
import zm.agriswift.farmer.domain.KycStatus;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class FarmerDirectoryImpl implements FarmerDirectory {

    private final FarmerRepository farmerRepository;
    private final FarmerPaymentAccountRepository accountRepository;
    private final PiiCipher piiCipher;

    FarmerDirectoryImpl(FarmerRepository farmerRepository,
                        FarmerPaymentAccountRepository accountRepository,
                        PiiCipher piiCipher) {
        this.farmerRepository = farmerRepository;
        this.accountRepository = accountRepository;
        this.piiCipher = piiCipher;
    }

    @Override
    public Optional<FarmerSummary> findById(UUID farmerId) {
        return farmerRepository.findById(farmerId).map(this::toSummary);
    }

    @Override
    public Optional<FarmerSummary> findByNationalId(String nationalId) {
        return farmerRepository.findByNationalIdHash(blindIndex(nationalId)).map(this::toSummary);
    }

    @Override
    public Optional<FarmerSummary> findByMobileNumber(String mobileNumber) {
        return farmerRepository.findByMobileNumberHash(blindIndex(mobileNumber)).map(this::toSummary);
    }

    @Override
    public Optional<FarmerSummary> findByEmail(String email) {
        return farmerRepository.findByEmail(email).map(this::toSummary);
    }

    @Override
    public Optional<PreferredPayoutAccount> findPreferredPayoutAccount(UUID farmerId) {
        return accountRepository.findByFarmer_FarmerIdAndPreferredTrue(farmerId)
                .filter(FarmerPaymentAccount::isActive)
                .map(acc -> new PreferredPayoutAccount(
                        acc.getAccountId(),
                        acc.getAccountType().name(),
                        acc.getProvider().getProviderId()));
    }

    private FarmerSummary toSummary(Farmer farmer) {
        return new FarmerSummary(
                farmer.getFarmerId(),
                farmer.getFarmerCode(),
                farmer.getFullName(),
                decrypt(farmer.getMobileNumberCiphertext()),
                farmer.getEmail(),
                farmer.getKycStatus() == KycStatus.VERIFIED,
                farmer.isActive());
    }

    /**
     * Derives the deterministic Argon2id blind index used for exact-match lookups.
     *
     * <p>FIX (JLS 11.2.3): {@link PiiCipher#generateBlindIndex(String)} is backed by
     * BouncyCastle's {@code Argon2BytesGenerator}, which signals failures exclusively
     * via <em>unchecked</em> exceptions and therefore declares no checked exception.
     * Catching {@link GeneralSecurityException} here was a compile-time error
     * ("never thrown in the corresponding try block"). We still translate failures at
     * the adapter boundary so crypto/infrastructure errors never escape as raw
     * runtime exceptions into the application layer.
     */
    private byte[] blindIndex(String value) {
        try {
            return piiCipher.generateBlindIndex(value);
        } catch (RuntimeException e) {           // Argon2id/BC failures are unchecked
            throw new DomainException("Failed to derive blind index", e);
        }
    }

    /**
     * AES-256-GCM decryption. {@link PiiCipher#decrypt(byte[])} uses the JCA
     * {@code Cipher} API and legitimately declares {@link GeneralSecurityException}
     * (e.g. {@code AEADBadTagException} on tampered ciphertext); {@link
     * IllegalArgumentException} covers structurally corrupt payloads (< 12-byte IV).
     */
    private String decrypt(byte[] ciphertext) {
        if (ciphertext == null) return null;
        try {
            return piiCipher.decrypt(ciphertext);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new DomainException("Failed to decrypt PII", e);
        }
    }
}