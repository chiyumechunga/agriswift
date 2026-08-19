package zm.agriswift.ledger;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum EntryType { ACCRUAL, SETTLEMENT }

    public enum DcIndicator { DEBIT, CREDIT }

    @Id
    @Column(name = "entry_id")
    private UUID entryId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dc_indicator", nullable = false)
    private DcIndicator dcIndicator;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    protected LedgerEntry() {
        // JPA
    }

    public static LedgerEntry of(UUID paymentId, EntryType entryType, String accountCode,
                                 BigDecimal amount, DcIndicator dcIndicator) {
        LedgerEntry e = new LedgerEntry();
        e.entryId = UUID.randomUUID();
        e.paymentId = paymentId;
        e.entryType = entryType;
        e.accountCode = accountCode;
        e.amount = amount;
        e.dcIndicator = dcIndicator;
        e.postedAt = Instant.now();
        return e;
    }
}
