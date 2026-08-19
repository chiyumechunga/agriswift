package zm.agriswift.farmer.api;

import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.farmer.api.dto.PreferredPayoutAccount;

import java.util.Optional;
import java.util.UUID;

public interface FarmerDirectory {

    Optional<FarmerSummary> findById(UUID farmerId);

    // Raw identifier lookups – hashing is done internally
    Optional<FarmerSummary> findByMobileNumber(String mobileNumber);

    Optional<FarmerSummary> findByEmail(String email);

    Optional<FarmerSummary> findByNationalId(String nationalId);

    Optional<PreferredPayoutAccount> findPreferredPayoutAccount(UUID farmerId);
}