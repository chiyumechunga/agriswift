package zm.agriswift.blockchain.api;

import java.time.Instant;

/**
 * Domain event published when the blockchain confirms a hash commitment.
 *
 * <p>Consumed by downstream modules ({@code audit}, {@code ledger},
 * {@code notification}) via {@code @ApplicationModuleListener}.
 * Spring Modulith + {@code spring-modulith-events-jdbc} persists every
 * publication to the {@code event_publication} table, guaranteeing
 * at-least-once cross-module delivery across JVM restarts.</p>
 *
 * <p>No annotation is required on this class. Modulith externalisation
 * is driven entirely by the consumer-side annotation.</p>
 */
public record AnchorNotarized(
        String anchorId,
        String anchorType,
        String entityId,
        String payloadHash,
        Instant committedAt
) {}