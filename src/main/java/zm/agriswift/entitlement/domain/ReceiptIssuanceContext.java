package zm.agriswift.entitlement.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit context captured at the moment a receipt is issued.
 *
 * <p>Adding a new field here (e.g. {@code capturedGps}, {@code agentSignature})
 * is a non-breaking change: no method signatures need to move, no callers need
 * to be rewritten. This is the Open/Closed fix for receipt issuance.
 */
public record ReceiptIssuanceContext(
        UUID issuedByAgentId,
        String deviceHwId,
        String networkUsed
) {
    public ReceiptIssuanceContext {
        Objects.requireNonNull(issuedByAgentId, "issuedByAgentId");
        Objects.requireNonNull(deviceHwId,      "deviceHwId");
        Objects.requireNonNull(networkUsed,     "networkUsed");
    }
}