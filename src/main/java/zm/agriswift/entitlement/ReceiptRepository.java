package zm.agriswift.entitlement;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.entitlement.Receipt;

import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
}
