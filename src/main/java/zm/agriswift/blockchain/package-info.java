/**
 * Blockchain Integration Module (Anti-Corruption Layer).
 *
 * <h2>Resilience: NACK → 3 Retries → Local DLQ</h2>
 * FireFly has no explicit NACK. The WS handler omits the ACK on failure
 * (implicit NACK). After 3 local failures the event is parked in
 * {@code blockchain_failed_events} and ACKed. A
 * {@code BlockchainRetryScheduler → DlqRecoveryService} pipeline retries
 * with exponential backoff (60 s → 5 min → 25 min → 2 h → 10 h).
 *
 * <h2>Source-of-Truth</h2>
 * The blockchain hash is authoritative. Local divergence is overwritten
 * silently with a warning log. No manual intervention.
 *
 * <h2>Modulith Boundaries</h2>
 * Cross-module communication is exclusively via
 * {@link zm.agriswift.blockchain.api.AnchorNotarized}, consumed through
 * {@code @ApplicationModuleListener} and persisted via the Modulith
 * Event Registry ({@code spring-modulith-events-jdbc}).
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common",
                "disbursement::api",
                "entitlement::api"
        }
)
package zm.agriswift.blockchain;