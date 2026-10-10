package zm.agriswift.disbursement.api;

/**
 * Cross-module projection of the payment lifecycle.
 *
 * <p>This enum is the <em>only</em> status type allowed to cross the
 * disbursement module boundary. It MUST mirror
 * {@code zm.agriswift.disbursement.domain.Payment.Status} one-to-one;
 * the mapping is performed at the single publisher
 * ({@code StatusCallbackController}) via
 * {@code PaymentStatus.valueOf(domainStatus.name())} and is guarded by
 * {@code PaymentStatusMirrorTest}.</p>
 */
public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    EXECUTED,
    SUCCEEDED,
    FAILED,
    ACKNOWLEDGED
}