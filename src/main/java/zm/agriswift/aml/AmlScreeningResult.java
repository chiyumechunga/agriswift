package zm.agriswift.aml;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aml_screening_results")
public class AmlScreeningResult {

    public enum MatchStatus { CLEAR, POTENTIAL_MATCH, CONFIRMED_MATCH, REJECTED }

    @Id
    @Column(name = "screening_id")
    private UUID screeningId;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "watchlist_source", nullable = false, length = 60)
    private String watchlistSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    private MatchStatus matchStatus;

    @Column(name = "screened_at", nullable = false)
    private Instant screenedAt;

    protected AmlScreeningResult() {
        // JPA
    }

    public static AmlScreeningResult record(UUID farmerId, UUID paymentId, String watchlistSource,
                                            MatchStatus matchStatus) {
        AmlScreeningResult r = new AmlScreeningResult();
        r.screeningId = UUID.randomUUID();
        r.farmerId = farmerId;
        r.paymentId = paymentId;
        r.watchlistSource = watchlistSource;
        r.matchStatus = matchStatus;
        r.screenedAt = Instant.now();
        return r;
    }
}

