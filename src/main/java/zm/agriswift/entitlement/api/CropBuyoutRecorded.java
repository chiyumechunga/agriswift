package zm.agriswift.entitlement.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published once an entitlement passes validation. This is the single
 * trigger that starts the disbursement flow shown in disbursement_flow.png —
 * disbursement listens for it and creates a PENDING payment in response.
 * Kept as a flat, serializable record (no JPA entities) since it also has to
 * survive round-tripping through Modulith's JPA-backed event publication
 * registry.
 */
public record CropBuyoutRecorded(
        UUID entitlementId,
        UUID farmerId,
        BigDecimal paymentAmount,
        String currencyCode
) {
}
