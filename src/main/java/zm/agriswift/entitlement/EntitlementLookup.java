package zm.agriswift.entitlement;
import java.util.Optional;
import java.util.UUID;

/** Public API other modules use to read entitlement state without touching the repository directly. */
public interface EntitlementLookup {
    Optional<EntitlementView> findById(UUID entitlementId);

    record EntitlementView(UUID entitlementId, UUID farmerId, java.math.BigDecimal paymentAmount, String status) {
    }
}

