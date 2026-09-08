package zm.agriswift.disbursement.api;

import zm.agriswift.disbursement.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after the async status callback is verified and applied.
 * Ledger (accrual/settlement postings) and notification (farmer SMS) both
 * react to this independently — see disbursement_flow.png's fan-out from
 * "Async status callback" to "Ledger entries posted" and "Farmer notified".
 */
public record PaymentStatusChanged(
        UUID paymentId,
        UUID farmerId,
        BigDecimal amount,
        String currencyCode,
        Payment.Status newStatus
) {
}
