package zm.agriswift.entitlement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /** The receipt issued for a given entitlement (1:1, unique FK). */
    Optional<Receipt> findByEntitlement_EntitlementId(UUID entitlementId);

    /** Officer/farmer lookup by the printed serial, e.g. reprint or dispute. */
    Optional<Receipt> findBySystemSerialNumber(String systemSerialNumber);

    /** QR scan at reconciliation points — validates the farmer's paper trail. */
    Optional<Receipt> findByQrCodeToken(String qrCodeToken);

    /** Cheap guard so double-issuance fails with a domain error, not a constraint violation. */
    boolean existsByEntitlement_EntitlementId(UUID entitlementId);
}