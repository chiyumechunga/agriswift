package zm.agriswift.blockchain.api;

import java.time.Instant;

/**
 * Published internally when the blockchain confirms a hash.
 * Other modules (Audit, Notification) listen to this.
 */
public record AnchorNotarized(
        String anchorId,
        String anchorType,
        String entityId,
        String payloadHash,
        Instant committedAt
) {}