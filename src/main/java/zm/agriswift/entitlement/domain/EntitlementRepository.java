package zm.agriswift.entitlement.domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    List<Entitlement> findByFarmerId(UUID farmerId);
}

