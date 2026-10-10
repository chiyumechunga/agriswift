package zm.agriswift.blockchain.internal.dlq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DLQ entry for blockchain events that exhausted 3 FireFly-level
 * retries without successful processing.
 */
@Entity
@Table(name = "blockchain_failed_events", indexes = {
        @Index(name = "idx_dlq_retry",
                columnList = "retry_count, next_retry_at")
})
@Getter @Setter @NoArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "anchor_id", nullable = false)
    private String anchorId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "tx_id")
    private String txId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}