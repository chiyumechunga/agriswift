package zm.agriswift.blockchain.domain;

public enum CommitStatus {
    PENDING,    // Created locally, not yet sent to FireFly
    SUBMITTED,  // Sent to FireFly, waiting for WebSocket confirmation
    COMMITTED,  // Confirmed on-chain via WebSocket
    FAILED      // Permanently failed (e.g., max DLQ retries exceeded)
}