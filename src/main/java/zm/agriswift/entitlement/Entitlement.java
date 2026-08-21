package zm.agriswift.entitlement;

import lombok.Getter;
import zm.agriswift.farmer.domain.Farmer;
import zm.agriswift.referencedata.CropPrice;
import zm.agriswift.referencedata.CropType;
import zm.agriswift.referencedata.Depot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single crop-delivery event at a depot. {@code paymentAmount} and
 * {@code moistureCheckPassed} are computed in the service layer (FR-13,
 * SRS §6.2.1) and re-verified by a DB trigger as a defense-in-depth net —
 * see {@code calculate_entitlement_payment()} in the Flyway migration.
 * A rejected (too-wet) delivery is still recorded, never discarded, so
 * FR-14's audit trail includes farmers turned away at the depot.
 */
@Entity
@Table(name = "entitlements")
public class Entitlement {

    public enum ValidationStatus { PENDING, VALIDATED, REJECTED }

    @Getter
    @Id
    @Column(name = "entitlement_id")
    private UUID entitlementId;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

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

    public Entitlement(UUID entitlementId, Farmer farmer, Depot depot, CropType cropType,
                       CropPrice cropPrice, BigDecimal cropWeightKg, BigDecimal moistureContentPct,
                       LocalDate deliveryDate) {
        this.entitlementId = entitlementId;
        this.farmer = farmer;
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
        this.paymentAmount = Boolean.TRUE.equals(moistureCheckPassed)
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
