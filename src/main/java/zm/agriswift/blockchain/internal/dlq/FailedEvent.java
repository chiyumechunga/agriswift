package zm.agriswift.blockchain.internal.dlq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blockchain_failed_events")
@Getter @Setter
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "tx_id")
    private String txId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "raw_payload", columnDefinition = "text", nullable = false)
    private String rawPayload;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "anchor_id", length = 255)
    private String anchorId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    // Read-only in JPA: the DB default + trigger own this column
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}