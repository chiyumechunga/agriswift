package zm.agriswift.disbursement.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after the async status callback is verified and applied.
 * Ledger (accrual/settlement postings) and notification (farmer SMS) both
 * react to this independently.
 *
 * <p>Carries the api-level {@link PaymentStatus} so that no consumer ever
 * needs visibility into {@code disbursement.domain.Payment}.</p>
 */
public record PaymentStatusChanged(
        UUID paymentId,
        UUID farmerId,
        BigDecimal amount,
        String currencyCode,
        PaymentStatus newStatus
) {
}