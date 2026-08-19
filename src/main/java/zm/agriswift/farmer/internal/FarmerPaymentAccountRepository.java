package zm.agriswift.farmer.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zm.agriswift.farmer.domain.FarmerPaymentAccount;

import java.util.Optional;
import java.util.UUID;

public interface FarmerPaymentAccountRepository extends JpaRepository<FarmerPaymentAccount, UUID> {
    // Internal query methods
    Optional<FarmerPaymentAccount> findByFarmer_FarmerIdAndPreferredTrue(UUID farmerId);
    @Modifying
    @Query("UPDATE FarmerPaymentAccount a SET a.preferred = false WHERE a.farmer.farmerId = :farmerId AND a.preferred = true")
    void demotePreferredAccounts(@Param("farmerId") UUID farmerId);
}