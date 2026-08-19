/**
 * Farmer module – core aggregate for farmer identity, KYC records, and payout destinations.
 *
 * <h2>Public API</h2>
 * <p>
 * The module exposes a single public interface, {@link zm.agriswift.farmer.FarmerDirectory},
 * which provides read‑only access to farmer summaries and preferred payout accounts.
 * All other classes (entities, repositories, services, handlers) are package‑private and
 * must NOT be referenced from outside this module.
 *
 * <h2>Encapsulation & SOLID Design</h2>
 * <ul>
 *   <li><b>SRP:</b> The farmer entity (<strong>{@code Farmer}</strong>) holds only identity and KYC state;
 *       authentication (PIN, lockouts) is owned by the <strong>Identity module</strong>.
 *   </li>
 *   <li><b>OCP:</b> New onboarding channels are added by implementing the
 *       {@link zm.agriswift.farmer.internal.handlers.OnboardingHandler} strategy –
 *       no changes to the entity or existing services.
 *   </li>
 *   <li><b>DIP:</b> Dependencies are injected via constructors; external modules depend only on
 *       the {@code FarmerDirectory} abstraction, never on concrete repositories.
 *   </li>
 *   <li><b>ISP:</b> Callers receive focused DTOs ({@code FarmerSummary}, {@code PreferredPayoutAccount})
 *       and not the full entity.
 *   </li>
 * </ul>
 *
 * <h2>PII & Security</h2>
 * <p>
 * All personally identifiable information (national ID, mobile number) is stored as ciphertext
 * (AES‑GCM) plus a deterministic HMAC‑SHA256 blind index for equality lookups.
 * The {@link zm.agriswift.farmer.internal.PiiCipher} component handles encryption and hashing;
 * the actual decryption key resides in an HSM/KMS outside the application.
 * This implementation is a placeholder and MUST be replaced with a production‑grade
 * KMS/HSM client before going live.
 *
 * <h2>Module Dependencies</h2>
 * <p>
 * This module depends on:
 * <ul>
 *   <li>{@code common} – for {@code BaseEntity}, {@code DomainException}, {@code NotFoundException}, etc.</li>
 *   <li>{@code referencedata} – for {@code Depot}, {@code PaymentProvider}, and their repositories.</li>
 * </ul>
 * It does <strong>not</strong> depend on the Identity module; instead, Identity depends on this module
 * via the {@code FarmerDirectory} API.
 *
 * <h2>Web Layer</h2>
 * <p>
 * REST controllers are located in the {@code web} package and expose endpoints under
 * {@code /api/farmers}. They delegate all operations to the package‑private services
 * and contain no business logic.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "referencedata"}
)
package zm.agriswift.farmer;