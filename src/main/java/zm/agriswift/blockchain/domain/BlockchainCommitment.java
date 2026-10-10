package zm.agriswift.blockchain.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root representing a cryptographic commitment notarised on
 * the Hyperledger Fabric ledger via FireFly.
 *
 * <h3>Source-of-Truth Policy</h3>
 * <p>The blockchain is the <strong>single authoritative source</strong>
 * for commitment hashes. When the on-chain hash differs from the locally
 * computed hash, the on-chain value <em>wins</em>. No exception is thrown.
 * No manual intervention is required. The local record is silently
 * corrected, and a warning is logged for audit visibility.</p>
 *
 * <h3>Idempotency</h3>
 * <p>FireFly guarantees at-least-once delivery.
 * {@link #markCommitted(String)} is a no-op if the commitment is already
 * {@link CommitStatus#COMMITTED}.</p>
 */
@Entity
@Table(name = "blockchain_commitments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockchainCommitment {

    private static final Logger log =
            LoggerFactory.getLogger(BlockchainCommitment.class);

    @Id
    @Column(name = "anchor_id")
    private String anchorId;

    @Column(name = "anchor_type", nullable = false)
    private String anchorType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
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

    // ── Factory ──────────────────────────────────────────────

    public static BlockchainCommitment create(String anchorId,
                                              String anchorType,
                                              String entityId,
                                              String payloadJson,
                                              String payloadHash) {
        var c = new BlockchainCommitment();
        c.anchorId    = Objects.requireNonNull(anchorId, "anchorId");
        c.anchorType  = Objects.requireNonNull(anchorType, "anchorType");
        c.entityId    = Objects.requireNonNull(entityId, "entityId");
        c.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
        c.payloadHash = Objects.requireNonNull(payloadHash, "payloadHash");
        c.status      = CommitStatus.PENDING;
        c.createdAt   = Instant.now();
        return c;
    }

    // ── State transitions ────────────────────────────────────

    public void markSubmitted() {
        if (this.status == CommitStatus.PENDING) {
            this.status = CommitStatus.SUBMITTED;
        }
    }

    /**
     * Idempotent on-chain confirmation.
     *
     * <p><b>Trust-the-chain rule:</b> {@code onChainHash} is the SHA-256
     * digest emitted by the Fabric chaincode. It is <em>always</em>
     * accepted as authoritative. If the locally stored hash differs, the
     * local value is overwritten and a warning is logged.</p>
     *
     * @param onChainHash SHA-256 hash returned by the blockchain
     */
    public void markCommitted(String onChainHash) {
        Objects.requireNonNull(onChainHash, "onChainHash");

        // Idempotency guard: FireFly at-least-once redelivery
        if (this.status == CommitStatus.COMMITTED) {
            log.debug("Anchor {} already COMMITTED – ignoring duplicate.",
                    anchorId);
            return;
        }

        // Trust-the-chain: overwrite local hash if it diverges
        if (!this.payloadHash.equals(onChainHash)) {
            log.warn("Hash divergence for anchor {}. "
                            + "Local=[{}] OnChain=[{}]. "
                            + "Overwriting with on-chain value "
                            + "(blockchain is source of truth).",
                    anchorId, this.payloadHash, onChainHash);
            this.payloadHash = onChainHash;
        }

        this.status      = CommitStatus.COMMITTED;
        this.committedAt = Instant.now();
    }

    /** Marks this commitment as permanently failed (DLQ exhausted). */
    public void markFailed() {
        this.status = CommitStatus.FAILED;
    }
}