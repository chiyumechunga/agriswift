package zm.agriswift.aml.internal;
import zm.agriswift.aml.AmlScreeningResult;
import zm.agriswift.aml.AmlScreeningResultRepository;
import zm.agriswift.disbursement.PaymentStatusChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class PaymentAmlScreeningListener {

    private final WatchlistProvider watchlistProvider;
    private final AmlScreeningResultRepository repository;

    PaymentAmlScreeningListener(WatchlistProvider watchlistProvider, AmlScreeningResultRepository repository) {
        this.watchlistProvider = watchlistProvider;
        this.repository = repository;
    }

    @ApplicationModuleListener
    void on(PaymentStatusChanged event) {
        AmlScreeningResult.MatchStatus status = watchlistProvider.screen(event.farmerId());
        repository.save(AmlScreeningResult.record(
                event.farmerId(), event.paymentId(), "DEFAULT_WATCHLIST", status));
    }
}
