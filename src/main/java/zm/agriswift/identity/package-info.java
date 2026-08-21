/**
 * Identity module – authentication and authorization for both staff and farmers.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link zm.agriswift.identity.api.UserDirectory} – staff user lookup</li>
 *   <li>{@link zm.agriswift.identity.api.SecurityContextAccessor} – current user context</li>
 * </ul>
 *
 * <p>Farmer authentication is handled separately via {@code FarmerAuthService}
 * and uses the Farmer module's {@code FarmerDirectory} to resolve farmer identity.
 * Farmers receive a JWT with {@code principalType=FARMER} and their {@code farmerId}.
 *
 * <p>Least privilege is enforced by:
 * <ul>
 *   <li>Separate authentication flows for staff vs. farmers</li>
 *   <li>Scoped roles (FARMER role grants minimal permissions)</li>
 *   <li>Resource‑level checks using {@code farmerId} from the security context</li>
 * </ul>
 *
 * <p>SOLID design: SRP (separate auth services), OCP (extensible token providers),
 * DIP (depends on abstractions), ISP (external modules get only what they need).
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "referencedata", "farmer", "farmer :: farmer", "farmer :: dto"}  // now depends on farmer for lookup
)
package zm.agriswift.identity;