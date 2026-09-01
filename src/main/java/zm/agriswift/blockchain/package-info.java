/**
 * Blockchain Integration Module (Anti-Corruption Layer).
 *
 * <h2>Architectural Pattern: The "Thin Ledger" (Notarization)</h2>
 * <p>
 * This module serves as the bridge between the Spring Boot domain model and the
 * Hyperledger Fabric distributed ledger, orchestrated via Hyperledger FireFly.
 * It strictly adheres to the "Thin Ledger" pattern: business logic and PII remain
 * in Postgres, while only cryptographic proofs (SHA-256 hashes) are notarized on-chain.
 *
 * <h2>Event-Driven Flow</h2>
 * <ul>
 *   <li><b>Outbound (REST):</b> Listens to internal domain events (e.g., {@code PaymentStatusChanged},
 *       {@code CropBuyoutRecorded}), computes their SHA-256 hash, and submits a
 *       {@code RecordAnchor} transaction to the FireFly Supernode.</li>
 *   <li><b>Inbound (WebSocket):</b> Maintains a persistent WebSocket connection to FireFly
 *       to listen for {@code AnchorRecorded} chaincode events. Upon receipt, it updates
 *       the local {@code BlockchainCommitment} aggregate and sends an {@code ACK} to
 *       advance the FireFly event cursor.</li>
 * </ul>
 *
 * <h2>Resilience & Dead Letter Queue (DLQ)</h2>
 * <p>
 * To prevent blocking the WebSocket stream, business processing exceptions (poison pills)
 * are caught, persisted to a local {@code FailedEvent} DLQ table, and immediately ACKed.
 * A background {@code BlockchainRetryScheduler} periodically attempts to reprocess these
 * failed events without disrupting real-time notarizations.
 *
 * <h2>Module Boundaries</h2>
 * <p>
 * This module depends on the public APIs of {@code disbursement} and {@code entitlement}
 * to listen for events. However, those modules <strong>must never</strong> depend on this
 * blockchain module. Communication back to the core is strictly via the
 * {@link zm.agriswift.blockchain.api.AnchorNotarized} Spring Application Event.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common",        // For BaseEntity, DomainException, etc.
                "disbursement",  // To listen to PaymentStatusChanged events
                "entitlement"    // To listen to CropBuyoutRecorded events
        }
)
package zm.agriswift.blockchain;