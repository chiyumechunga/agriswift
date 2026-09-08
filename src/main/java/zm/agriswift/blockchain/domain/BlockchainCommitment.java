package zm.agriswift.blockchain.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "blockchain_commitments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockchainCommitment {

    @Id
    @Column(name = "anchor_id")
    private String anchorId;

    @Column(name = "anchor_type", nullable = false)
    private String anchorType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR) // Tells Hibernate to use text/varchar, not OID
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommitStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    public static BlockchainCommitment create(String anchorId, String anchorType, String entityId, String payloadJson, String payloadHash) {
        BlockchainCommitment c = new BlockchainCommitment();
        c.anchorId = Objects.requireNonNull(anchorId);
        c.anchorType = Objects.requireNonNull(anchorType);
        c.entityId = Objects.requireNonNull(entityId);
        c.payloadJson = Objects.requireNonNull(payloadJson);
        c.payloadHash = Objects.requireNonNull(payloadHash);
        c.status = CommitStatus.PENDING;
        c.createdAt = Instant.now();
        return c;
    }

    public void markSubmitted() {
        if (this.status == CommitStatus.PENDING) {
            this.status = CommitStatus.SUBMITTED;
        }
    }

    /**
     * Idempotent state transition. Safely handles FireFly redeliveries.
     */
    public void markCommitted(String onChainHash) {
        if (this.status == CommitStatus.COMMITTED) {
            return; // Already processed, ignore duplicate event
        }
        if (!this.payloadHash.equals(onChainHash)) {
            throw new IllegalStateException("CRITICAL: Hash mismatch for anchor " + anchorId + ". Blockchain data corrupted.");
        }
        this.status = CommitStatus.COMMITTED;
        this.committedAt = Instant.now();
    }
}