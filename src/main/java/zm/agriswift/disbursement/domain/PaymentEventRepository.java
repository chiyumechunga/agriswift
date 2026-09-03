package zm.agriswift.disbursement;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.disbursement.domain.PaymentEvent;

import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
}
