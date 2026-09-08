/**
 * Entitlement module — the "buyout" side of the flow in disbursement_flow.png:
 * records a farmer's crop delivery/weighbridge outcome as an Entitlement,
 * issues the paper-trail Receipt, and publishes {@link zm.agriswift.entitlement.api.CropBuyoutRecorded}
 * once validation passes. Disbursement (a different module) listens for that
 * event and takes it from there — entitlement never calls disbursement directly.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "farmer::api", "referencedata"}
)
package zm.agriswift.entitlement;