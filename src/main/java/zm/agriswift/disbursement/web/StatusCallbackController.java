package zm.agriswift.disbursement.web;

import zm.agriswift.disbursement.domain.Payment;
import zm.agriswift.disbursement.domain.PaymentEvent;
import zm.agriswift.disbursement.domain.PaymentEventRepository;
import zm.agriswift.disbursement.domain.PaymentRepository;
import zm.agriswift.disbursement.api.PaymentStatusChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.agriswift.disbursement.internal.CallbackSignatureVerifier;

/**
 * "Async status callback" box: the NFS switch / MNO calls back here once the
 * transfer actually settles (or fails), asynchronously and out-of-band from
 * the original submit. Signature verification is what tells us this call
 * really came from ZECHL and not a spoofed client.
 */
@RestController
@RequestMapping("/api/v1/disbursement/callbacks")
public class StatusCallbackController {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final CallbackSignatureVerifier signatureVerifier;
    private final ApplicationEventPublisher events;

    StatusCallbackController(PaymentRepository paymentRepository,
                             PaymentEventRepository paymentEventRepository,
                             CallbackSignatureVerifier signatureVerifier,
                             ApplicationEventPublisher events) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.signatureVerifier = signatureVerifier;
        this.events = events;
    }

    @PostMapping("/zechl")
    @Transactional
    public ResponseEntity<Void> onZechlCallback(@RequestBody StatusCallbackRequest request) {
        if (!signatureVerifier.isValid(request)) {
            return ResponseEntity.status(401).build();
        }

        Payment payment = paymentRepository.findByUetr(request.uetr())
                .orElseThrow(() -> new IllegalArgumentException("Unknown UETR: " + request.uetr()));

        if ("SUCCEEDED".equals(request.status())) {
            payment.markSucceeded();
        } else if ("FAILED".equals(request.status())) {
            payment.markFailed(request.failureReason());
        }
        paymentRepository.save(payment);
        paymentEventRepository.save(new PaymentEvent(payment.getPaymentId(), payment.getStatus()));

        events.publishEvent(new PaymentStatusChanged(
                payment.getPaymentId(),
                payment.getFarmerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus()));

        return ResponseEntity.ok().build();
    }
}
