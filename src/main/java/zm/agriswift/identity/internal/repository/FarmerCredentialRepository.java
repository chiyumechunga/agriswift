package zm.agriswift.identity.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.identity.domain.FarmerCredential;

import java.util.Optional;
import java.util.UUID;

public interface FarmerCredentialRepository extends JpaRepository<FarmerCredential, UUID> {

    Optional<FarmerCredential> findByFarmerId(UUID farmerId);

    Optional<FarmerCredential> findByEmail(String email);

    Optional<FarmerCredential> findByNationalIdHash(byte[] nationalIdHash);

    Optional<FarmerCredential> findByMobileNumberHash(byte[] mobileNumberHash);
}