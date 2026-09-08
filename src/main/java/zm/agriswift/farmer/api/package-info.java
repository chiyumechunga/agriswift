/**
 * Public API of the Farmer module.
 *
 * <p>Exposes read-only ports and DTOs for other bounded contexts:
 * <ul>
 *   <li>{@link zm.agriswift.farmer.api.FarmerDirectory} — farmer summaries and preferred payout accounts</li>
 *   <li>{@link zm.agriswift.farmer.api.AmlScreeningPort} — AML screening contract</li>
 *   <li>{@link zm.agriswift.farmer.api.dto.FarmerSummary} — lightweight read-only DTO</li>
 *   <li>{@link zm.agriswift.farmer.api.dto.PreferredPayoutAccount} — payout channel DTO</li>
 * </ul>
 *
 * <p>Other modules reference this as {@code "farmer::api"} in their
 * {@code @ApplicationModule(allowedDependencies)} declarations.
 */
@org.springframework.modulith.NamedInterface("api")
package zm.agriswift.farmer.api;