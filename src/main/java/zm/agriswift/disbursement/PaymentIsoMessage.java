package zm.agriswift.disbursement;
import jakarta.persistence.*;
import zm.agriswift.common.CreationAuditedEntity;

import java.time.Instant;
import java.util.UUID;

/** Raw ISO 20022 (pain.001 / pacs.008 / pacs.002) payloads, kept off the high-write payments table. */
@Entity
@Table(name = "payment_iso_messages")
public class PaymentIsoMessage extends CreationAuditedEntity {

    public enum Direction { OUTBOUND, INBOUND }

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private Direction direction;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentIsoMessage() {
        // JPA
    }

    public PaymentIsoMessage(UUID paymentId, Direction direction, String messageType, String payloadJson) {
        this.messageId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.direction = direction;
        this.messageType = messageType;
        this.payloadJson = payloadJson;
    }
}

