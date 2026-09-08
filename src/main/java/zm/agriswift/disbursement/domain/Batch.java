package zm.agriswift.disbursement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;

import java.math.BigDecimal;
import java.util.UUID;

/** Batches a set of payments under one treasury-facing reference (FR-33). */
@Entity
@Table(name = "batches")
public class Batch extends CreationAuditedEntity {

    public enum Status { PENDING, PROCESSING, COMPLETED, PARTIALLY_FAILED }

    @Getter
    @Id
    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "batch_reference", nullable = false, unique = true, length = 40)
    private String batchReference;

    @Column(name = "total_farmers", nullable = false)
    private int totalFarmers = 0;

    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    protected Batch() {
        // JPA
    }

}
