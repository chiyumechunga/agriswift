/**
 * Ledger module: double-entry bookkeeping against the chart of accounts.
 * Reacts to {@link zm.agriswift.disbursement.api.PaymentStatusChanged} —
 * the "Ledger entries posted" box in disbursement_flow.png — rather than
 * being called synchronously by disbursement.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "referencedata", "disbursement"}
)

package zm.agriswift.ledger;