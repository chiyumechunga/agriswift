package zm.agriswift.identity.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.identity.domain.FarmerCredential;

import java.util.UUID;

public interface FarmerCredentialRepository extends JpaRepository<FarmerCredential, UUID> {
}