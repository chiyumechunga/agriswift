package zm.agriswift.disbursement.internal.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // NOT lombok.Value
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.domain.PaymentEvent;
import zm.agriswift.disbursement.domain.PaymentRepository;
import zm.agriswift.disbursement.web.StatusCallbackController;
import zm.agriswift.disbursement.web.StatusCallbackRequest;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@Profile({"dev", "poc", "default"}) // Only runs in development/PoC
public class MockSwitchSimulator {

    private static final Logger log = LoggerFactory.getLogger(MockSwitchSimulator.class);

    private final PaymentRepository paymentRepository;
    private final StatusCallbackController callbackController;
    private final double successRate;

    public MockSwitchSimulator(
            PaymentRepository paymentRepository,
            StatusCallbackController callbackController,
            @Value("${agriswift.mock.switch.success-rate:0.8}") double successRate) {
        this.paymentRepository = paymentRepository;
        this.callbackController = callbackController;
        this.successRate = successRate;
    }

    @Async
    @EventListener
    public void onPaymentExecuted(PaymentEvent event) throws InterruptedException {
        if (event.getEventType() == Payment.Status.EXECUTED) {
            log.info("[MOCK SWITCH] Payment {} executed. Simulating 5s network delay...", event.getPaymentId());
            TimeUnit.SECONDS.sleep(5);

            Payment payment = paymentRepository.findById(event.getPaymentId()).orElse(null);
            if (payment != null) {
                boolean isSuccess = ThreadLocalRandom.current().nextDouble() < successRate;
                String status = isSuccess ? "SUCCEEDED" : "FAILED";
                String reason = isSuccess ? null : "Mocked Network Timeout";

                log.info("[MOCK SWITCH] Firing mock {} callback for UETR: {}", status, payment.getUetr());
                StatusCallbackRequest mockRequest = new StatusCallbackRequest(
                        payment.getUetr(), status, reason, "MOCK_SIGNATURE_FOR_POC"
                );
                callbackController.onZechlCallback(mockRequest);
            }
        }
    }
}