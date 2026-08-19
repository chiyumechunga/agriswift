package zm.agriswift.referencedata;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A price is valid for a date range, never mutated in place — FR-13 must be
 * able to prove which price applied to a payment made months ago. See the
 * {@code crop_prices_no_overlap} EXCLUDE constraint in the schema, which this
 * entity relies on rather than re-implements at the application layer.
 */
@Entity
@Table(name = "crop_prices")
public class CropPrice {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crop_price_id")
    private Long cropPriceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_type_id", nullable = false)
    private CropType cropType;

    @Column(name = "season", nullable = false, length = 20)
    private String season;

    @Getter
    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerKg;

    @Getter
    @Column(name = "max_moisture_pct", precision = 4, scale = 2)
    private BigDecimal maxMoisturePct;

    @Column(name = "board_resolution_ref", length = 100)
    private String boardResolutionRef;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    protected CropPrice() {
        // JPA
    }

}

