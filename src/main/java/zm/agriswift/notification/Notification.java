package zm.agriswift.notification;


import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@IdClass(Notification.NotificationId.class)
public class Notification {

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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    protected Notification() {
        // JPA
    }

    public static Notification queue(UUID farmerId, Channel channel, Type type,
                                     UUID relatedPaymentId, String content) {
        Notification n = new Notification();
        n.notificationId = UUID.randomUUID();
        n.createdAt = Instant.now();
        n.farmerId = farmerId;
        n.channel = channel;
        n.notificationType = type;
        n.relatedPaymentId = relatedPaymentId;
        n.content = content;
        return n;
    }

    public record NotificationId(UUID notificationId, Instant createdAt) implements java.io.Serializable {
    }
}
