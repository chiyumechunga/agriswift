/**
 * Public API of the Entitlement module.
 *
 * <p>This named interface exposes the domain events and read-only ports
 * that other bounded contexts (such as Disbursement and Blockchain) are
 * allowed to consume.
 *
 * <h2>Exposed Contracts</h2>
 * <ul>
 *   <li>{@link zm.agriswift.entitlement.api.CropBuyoutRecorded} - The domain event
 *       published when a farmer's crop delivery is validated at the depot and an
 *       entitlement is created. The Disbursement module listens to this event
 *       to initiate the payout flow.</li>
 *
 *   <li>{@link zm.agriswift.entitlement.api.EntitlementLookup} - The outbound port
 *       for querying entitlement summaries and statuses without leaking internal
 *       domain aggregates or JPA repositories.</li>
 * </ul>
 *
 * <p><b>Module Boundary Rule:</b> External modules must depend on
 * {@code "entitlement::api"} in their {@code @ApplicationModule} configuration,
 * never on the base {@code entitlement} package.
 */
@org.springframework.modulith.NamedInterface("api")
package zm.agriswift.entitlement.api;