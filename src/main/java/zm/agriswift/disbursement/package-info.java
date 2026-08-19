/**
 * Disbursement module — the orchestrator/gateway-router/callback chain in
 * disbursement_flow.png. Listens for {@link zm.agriswift.entitlement.CropBuyoutRecorded},
 * creates a PENDING payment, routes it to bank transfer or mobile money via
 * the ZECHL NFS switch, and reconciles the async status callback. Publishes
 * {@link PaymentStatusChanged} for ledger/notification/aml to react to —
 * it never calls those modules directly.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "farmer", "entitlement", "referencedata"}
)

package zm.agriswift.disbursement;