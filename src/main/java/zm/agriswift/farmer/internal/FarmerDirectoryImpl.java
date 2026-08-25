package zm.agriswift.farmer.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
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
                farmer.getKycStatus() == KycStatus.VERIFIED,   // enum type, not the private field
                farmer.isActive());
    }

    private byte[] blindIndex(String value) {
        try {
            return piiCipher.generateBlindIndex(value);
        } catch (GeneralSecurityException e) {
            throw new DomainException("Failed to derive blind index", e);
        }
    }

    private String decrypt(byte[] ciphertext) {
        if (ciphertext == null) return null;
        try {
            return piiCipher.decrypt(ciphertext); // adjust to PiiCipher's actual method name
        } catch (GeneralSecurityException e) {
            throw new DomainException("Failed to decrypt PII", e);
        }
    }
}