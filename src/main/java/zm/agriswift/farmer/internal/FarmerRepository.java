package zm.agriswift.farmer.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.farmer.domain.Farmer;

import java.util.Optional;
import java.util.UUID;

public interface FarmerRepository extends JpaRepository <Farmer, UUID> {
    Optional<Farmer> findByFarmerCode(String farmerCode);
    Optional<Farmer> findByNationalIdHash(byte[] nationalIdHash);
    Optional<Farmer> findByMobileNumberHash(byte[] mobileNumberHash);
    Optional<Farmer> findByEmail(String email);
}
