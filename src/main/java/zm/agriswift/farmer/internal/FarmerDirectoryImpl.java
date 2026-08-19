package zm.agriswift.farmer.internal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.farmer.api.dto.PreferredPayoutAccount;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.farmer.domain.FarmerPaymentAccount;

import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class FarmerDirectoryImpl implements FarmerDirectory {

    private final FarmerRepository farmerRepository;
    private final FarmerPaymentAccountRepository accountRepository;

    FarmerDirectoryImpl(FarmerRepository farmerRepository,
                        FarmerPaymentAccountRepository accountRepository) {
        this.farmerRepository = farmerRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<FarmerSummary> findById(UUID farmerId) {
        return farmerRepository.findById(farmerId)
                .map(this::toSummary);
    }

    @Override
    public Optional<FarmerSummary> findByNationalIdHash(byte[] hash) {
        return farmerRepository.findByNationalIdHash(hash)
                .map(this::toSummary);
    }

    @Override
    public Optional<FarmerSummary> findByMobileNumberHash(byte[] hash) {
        return farmerRepository.findByMobileNumberHash(hash)
                .map(this::toSummary);
    }


    @Override
    public Optional<FarmerSummary> findByEmail(String email) {
        // Assuming email is stored in plain text (or encrypted with searchable encryption),
        // For simplicity, we assume email is plain text and has an index.
        return farmerRepository.findByEmail(email).map(this::toSummary);
    }


    @Override
    public Optional<PreferredPayoutAccount> findPreferredPayoutAccount(UUID farmerId) {
        return accountRepository.findByFarmer_FarmerIdAndPreferredTrue(farmerId)
                .filter(FarmerPaymentAccount::isActive)
                .map(acc -> new PreferredPayoutAccount(
                        acc.getAccountId(),
                        acc.getAccountType().name(),
                        acc.getProvider().getProviderId()
                ));
    }

    private FarmerSummary toSummary(Farmer farmer) {
        return new FarmerSummary(
                farmer.getFarmerId(),
                farmer.getFarmerCode(),
                farmer.getFullName(),
                farmer.getKycStatus() == Farmer.kycStatus.VERIFIED,
                farmer.isActive()
        );
    }
}