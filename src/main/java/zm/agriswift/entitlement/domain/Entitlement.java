package zm.agriswift.entitlement.domain;

import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;
import zm.agriswift.referencedata.CropPrice;
import zm.agriswift.referencedata.CropType;
import zm.agriswift.referencedata.Depot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "entitlements")
public class Entitlement extends CreationAuditedEntity {

    public enum ValidationStatus { PENDING, VALIDATED, REJECTED }

    @Getter
    @Id
    @Column(name = "entitlement_id")
    private UUID entitlementId;

    /**
     * ID-only reference. The Farmer aggregate is internal to the farmer module
     * (Spring Modulith: never import another module's domain entity).
     * The FK to farmers(farmer_id) stays enforced at DB level by the migration.
     */
    @Getter
    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    // referencedata entities live in their module's exposed root package, so these are legal
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_type_id", nullable = false)
    private CropType cropType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_price_id", nullable = false)
    private CropPrice cropPrice;

    @Column(name = "crop_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal cropWeightKg;

    @Column(name = "moisture_content_pct", precision = 4, scale = 2)
    private BigDecimal moistureContentPct;

    @Getter
    @Column(name = "moisture_check_passed")
    private Boolean moistureCheckPassed;

    @Getter
    @Column(name = "payment_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    protected Entitlement() {
        // JPA
    }

    public Entitlement(UUID entitlementId, UUID farmerId, Depot depot, CropType cropType,
                       CropPrice cropPrice, BigDecimal cropWeightKg, BigDecimal moistureContentPct,
                       LocalDate deliveryDate) {
        this.entitlementId = entitlementId;
        this.farmerId = farmerId;
        this.depot = depot;
        this.cropType = cropType;
        this.cropPrice = cropPrice;
        this.cropWeightKg = cropWeightKg;
        this.moistureContentPct = moistureContentPct;
        this.deliveryDate = deliveryDate;
    }

    /** Mirrors calculate_entitlement_payment(): weight x price, zeroed if too wet. */
    public void applyMoistureRuleAndPrice() {
        BigDecimal maxMoisture = cropPrice.getMaxMoisturePct();
        this.moistureCheckPassed = moistureContentPct == null || maxMoisture == null
                || moistureContentPct.compareTo(maxMoisture) <= 0;
        this.paymentAmount = moistureCheckPassed
                ? cropWeightKg.multiply(cropPrice.getPricePerKg())
                : BigDecimal.ZERO;
    }

    public void validate(boolean ziamisMatch) {
        this.validationStatus = (Boolean.TRUE.equals(moistureCheckPassed) && ziamisMatch)
                ? ValidationStatus.VALIDATED
                : ValidationStatus.REJECTED;
        this.validatedAt = Instant.now();
    }
}