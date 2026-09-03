package zm.agriswift.disbursement.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.PaymentEvent;
import zm.agriswift.disbursement.domain.PaymentRepository;
import zm.agriswift.disbursement.StatusCallbackRequest;

import java.util.concurrent.TimeUnit;

@Component
@Profile({"dev", "poc", "default"}) // Only runs in development/PoC
public class MockSwitchSimulator {

    private static final Logger log = LoggerFactory.getLogger(MockSwitchSimulator.class);
    private final PaymentRepository paymentRepository;
    private final StatusCallbackController callbackController;

    public MockSwitchSimulator(PaymentRepository paymentRepository, StatusCallbackController callbackController) {
        this.paymentRepository = paymentRepository;
        this.callbackController = callbackController;
    }

    @Async
    @EventListener
    public void onPaymentExecuted(PaymentEvent event) throws InterruptedException {
        if (event.getEventType() == Payment.Status.EXECUTED) {
            log.info("[MOCK SWITCH] Payment {} executed. Simulating 5s network delay...", event.getPaymentId());
            TimeUnit.SECONDS.sleep(5);

            Payment payment = paymentRepository.findById(event.getPaymentId()).orElse(null);
            if (payment != null) {
                log.info("[MOCK SWITCH] Firing mock SUCCEEDED callback for UETR: {}", payment.getUetr());
                StatusCallbackRequest mockRequest = new StatusCallbackRequest(
                        payment.getUetr(), "SUCCEEDED", null, "MOCK_SIGNATURE_FOR_POC"
                );
                callbackController.onZechlCallback(mockRequest);
            }
        }
    }
}