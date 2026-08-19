package zm.agriswift.disbursement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByUetr(UUID uetr);
    List<Payment> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);
}
