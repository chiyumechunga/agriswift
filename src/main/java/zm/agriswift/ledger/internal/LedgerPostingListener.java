package zm.agriswift.ledger.internal;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.api.PaymentStatusChanged;
import zm.agriswift.ledger.LedgerEntry;
import zm.agriswift.ledger.LedgerEntryRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * "Ledger entries posted" box: posts a self-balancing debit/credit pair per
 * payment status transition. SUCCEEDED settles farmer payable against bank
 * settlement clearing; FAILED reverses the earlier accrual.
 */
@Component
class LedgerPostingListener {

    private final LedgerEntryRepository ledgerEntryRepository;

    LedgerPostingListener(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @ApplicationModuleListener
    void on(PaymentStatusChanged event) {
        if (event.newStatus() == Payment.Status.SUCCEEDED) {
            ledgerEntryRepository.save(LedgerEntry.of(
                    event.paymentId(), LedgerEntry.EntryType.SETTLEMENT, "FARMER_PAYABLE",
                    event.amount(), LedgerEntry.DcIndicator.DEBIT));
            ledgerEntryRepository.save(LedgerEntry.of(
                    event.paymentId(), LedgerEntry.EntryType.SETTLEMENT, "BANK_SETTLEMENT",
                    event.amount(), LedgerEntry.DcIndicator.CREDIT));
        } else if (event.newStatus() == Payment.Status.FAILED) {
            ledgerEntryRepository.save(LedgerEntry.of(
                    event.paymentId(), LedgerEntry.EntryType.ACCRUAL, "TREASURY_CASH",
                    event.amount(), LedgerEntry.DcIndicator.DEBIT));
            ledgerEntryRepository.save(LedgerEntry.of(
                    event.paymentId(), LedgerEntry.EntryType.ACCRUAL, "FARMER_PAYABLE",
                    event.amount(), LedgerEntry.DcIndicator.CREDIT));
        }
    }
}

