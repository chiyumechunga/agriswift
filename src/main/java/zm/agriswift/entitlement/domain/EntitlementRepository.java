package zm.agriswift.entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.entitlement.domain.Entitlement;

import java.util.List;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    List<Entitlement> findByFarmer_FarmerId(UUID farmerId);
}

