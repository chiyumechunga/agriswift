package zm.agriswift.disbursement;
import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;

import java.time.Instant;
import java.util.UUID;

/** Append-only transition history for a payment. Revoke UPDATE/DELETE at the DB grant level too (see migration). */
@Entity
@Table(name = "payment_events")
public class PaymentEvent extends CreationAuditedEntity {

    @Getter
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Getter
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private Payment.Status eventType;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;


    protected PaymentEvent() {
        // JPA
    }

    public PaymentEvent(UUID paymentId, Payment.Status eventType) {
        this.eventId = UUID.randomUUID();
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.eventTimestamp = Instant.now();
    }

}
