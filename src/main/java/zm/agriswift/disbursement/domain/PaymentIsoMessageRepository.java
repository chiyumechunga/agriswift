package zm.agriswift.disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.disbursement.domain.PaymentIsoMessage;

import java.util.UUID;

public interface PaymentIsoMessageRepository extends JpaRepository<PaymentIsoMessage, UUID> {}