package zm.agriswift.disbursement.domain;
import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mutable current-state row, optimized for "what's this payment's status
 * right now" reads. Full transition history lives in {@link PaymentEvent},
 * append-only — the same world-state/history split the SRS's blockchain
 * layer (Fabric) makes internally, kept deliberately in sync on the
 * Postgres side.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    public enum Status { INITIATED, AUTHORIZED, EXECUTED, SUCCEEDED, FAILED, ACKNOWLEDGED }

    public enum ChannelType { MOBILE_MONEY, BANK_TRANSFER }

    @Getter
    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Getter
    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Getter
    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    @Getter
    @Column(name = "payment_account_id", nullable = false)
    private UUID paymentAccountId;

    @Getter
    @Column(name = "batch_id")
    private UUID batchId;

    @Getter
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Getter
    @Column(name = "currency", nullable = false, columnDefinition = "bpchar(3)")
    private String currency = "ZMW";

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Getter
    @Column(name = "provider_id", nullable = false)
    private Short providerId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.INITIATED;

    @Getter
    @Column(name = "uetr", nullable = false, unique = true)
    private UUID uetr; //Unique End-to-End Transaction Reference

    @Getter
    @Column(name = "retry_count", nullable = false)
    private short retryCount = 0;

    @Getter
    @Column(name = "failure_reason")
    private String failureReason;

    protected Payment() {
        // JPA
    }

    public static Payment createPending(UUID farmerId, UUID entitlementId, UUID paymentAccountId,
                                        BigDecimal amount, ChannelType channelType, Short providerId) {
        Payment p = new Payment();
        p.paymentId = UUID.randomUUID();
        p.farmerId = farmerId;
        p.entitlementId = entitlementId;
        p.paymentAccountId = paymentAccountId;
        p.amount = amount;
        p.channelType = channelType;
        p.providerId = providerId;
        p.uetr = UUID.randomUUID();
        p.status = Status.INITIATED;
        return p;
    }

    public void markExecuted() {
        this.status = Status.EXECUTED;
    }

    public void markSucceeded() {
        this.status = Status.SUCCEEDED;
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason;
    }

}
