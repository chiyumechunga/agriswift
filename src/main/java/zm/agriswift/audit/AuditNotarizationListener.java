package zm.agriswift.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import zm.agriswift.blockchain.api.AnchorNotarized;

/**
 * Records an immutable audit trail entry whenever a blockchain commitment
 * is confirmed on-chain.
 *
 * <p>{@code @ApplicationModuleListener} (note: package
 * {@code org.springframework.modulith.events}) combines
 * {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async} +
 * {@code @Transactional(REQUIRES_NEW)}. Spring Modulith persists the event
 * to {@code event_publication} BEFORE this method runs; if it throws, the
 * event is resent on restart (at-least-once).</p>
 */
@Component
class AuditNotarizationListener {

    private static final Logger log = LoggerFactory.getLogger(AuditNotarizationListener.class);

    private final AuditRecorder auditRecorder;

    AuditNotarizationListener(AuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    @ApplicationModuleListener
    void onAnchorNotarized(AnchorNotarized event) {
        // userId = null → system-initiated action (per AuditRecorder contract)
        auditRecorder.record(
                null,
                "BLOCKCHAIN_NOTARIZATION",
                event.anchorType(),
                event.anchorId());

        log.info("Audit trail recorded: anchor={} type={} hash={} at={}",
                event.anchorId(), event.anchorType(),
                event.payloadHash(), event.committedAt());
    }
}