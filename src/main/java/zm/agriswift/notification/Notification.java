package zm.agriswift.notification;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@IdClass(Notification.NotificationId.class)
@Getter                                  // resolves all "never accessed" warnings
public class Notification {              // NO extends: created_at is part of the PK

    public enum Channel { SMS, USSD, VOICE, EMAIL }

    public enum Type {
        OTP, FARMER_ID_DELIVERY, PAYMENT_AUTH_REQUEST, PAYMENT_CONFIRMATION,
        FAILURE_ALERT, ACCOUNT_UPDATE, RECEIPT_DISPATCH
    }

    public enum Status { QUEUED, SENT, DELIVERED, FAILED }

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @Id
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;           // partition key = identity; set in factory

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private Type notificationType;

    @Column(name = "related_payment_id")
    private UUID relatedPaymentId;

    @Column(name = "recipient_address_masked", length = 20)
    private String recipientAddressMasked;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.QUEUED;

    @Column(name = "gateway_message_id", length = 80)
    private String gatewayMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Notification() { /* JPA */ }

    public static Notification queue(UUID farmerId, Channel channel, Type type,
                                     UUID relatedPaymentId, String content) {
        Notification n = new Notification();
        n.notificationId = UUID.randomUUID();
        n.createdAt = Instant.now();     // PK component: must be assigned pre-insert
        n.farmerId = farmerId;
        n.channel = channel;
        n.notificationType = type;
        n.relatedPaymentId = relatedPaymentId;
        n.content = content;
        return n;
    }

    // Lifecycle transitions (DDD aggregate behaviour; also silences "never used")
    public void markSent(String gatewayMessageId) {
        this.status = Status.SENT;
        this.gatewayMessageId = gatewayMessageId;
        this.sentAt = Instant.now();
    }

    public void markDelivered() {
        this.status = Status.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }

    public record NotificationId(UUID notificationId, Instant createdAt)
            implements java.io.Serializable { }
}