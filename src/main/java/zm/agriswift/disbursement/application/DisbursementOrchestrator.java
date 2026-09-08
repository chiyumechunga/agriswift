package zm.agriswift.disbursement.application;

import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.domain.PaymentEvent;
import zm.agriswift.disbursement.domain.PaymentEventRepository;
import zm.agriswift.disbursement.domain.PaymentRepository;
import zm.agriswift.disbursement.application.PayoutGatewayRouter;
import zm.agriswift.entitlement.api.CropBuyoutRecorded;
import zm.agriswift.farmer.api.FarmerDirectory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * "Disbursement orchestrator" box: listens for a validated crop buyout,
 * resolves the farmer's preferred payout account (bank vs. mobile money),
 * creates the PENDING payment, and hands it to the {@link PayoutGatewayRouter}.
 */
@Component
class DisbursementOrchestrator {

    private final FarmerDirectory farmerDirectory;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PayoutGatewayRouter router;

    DisbursementOrchestrator(FarmerDirectory farmerDirectory,
                             PaymentRepository paymentRepository,
                             PaymentEventRepository paymentEventRepository,
                             PayoutGatewayRouter router) {
        this.farmerDirectory = farmerDirectory;
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.router = router;
    }

    @ApplicationModuleListener // Handles @Async + @TransactionalEventListener + REQUIRES_NEW transaction automatically
    void on(CropBuyoutRecorded event) {
        var account = farmerDirectory.findPreferredPayoutAccount(event.farmerId())
                .orElseThrow(() -> new IllegalStateException(
                        "No preferred payout account for farmer " + event.farmerId()));

        Payment.ChannelType channel = "BANK_TRANSFER".equals(account.accountTypeCode())
                ? Payment.ChannelType.BANK_TRANSFER
                : Payment.ChannelType.MOBILE_MONEY;

        Payment payment = Payment.createPending(
                event.farmerId(),
                event.entitlementId(),
                account.accountId(),
                event.paymentAmount(),
                channel,
                account.providerId());

        paymentRepository.save(payment);
        paymentEventRepository.save(new PaymentEvent(payment.getPaymentId(), Payment.Status.INITIATED));

        router.route(payment);

        // Persist final state after routing
        paymentRepository.save(payment);
        paymentEventRepository.save(new PaymentEvent(payment.getPaymentId(), payment.getStatus()));
    }
}