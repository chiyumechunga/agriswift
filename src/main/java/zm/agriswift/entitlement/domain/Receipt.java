package zm.agriswift.entitlement;

import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;
import zm.agriswift.entitlement.domain.Entitlement;

import java.time.Instant;
import java.util.UUID;

/** Mirrors FRA's paper "Produce Receipt and Certification Note" process. */
@Entity
@Table(name = "receipts")
public class Receipt extends CreationAuditedEntity {

    public enum Status { ISSUED, ACTIVE, VOIDED, RECONCILED }

    @Getter
    @Id
    @Column(name = "receipt_id")
    private UUID receiptId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_id", nullable = false, unique = true)
    private Entitlement entitlement;

    @Getter
    @Column(name = "system_serial_number", unique = true, length = 40)
    private String systemSerialNumber;

    @Column(name = "qr_code_token", nullable = false, unique = true)
    private String qrCodeToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ISSUED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Receipt() {
        // JPA
    }

}