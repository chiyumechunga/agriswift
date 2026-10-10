package zm.agriswift.blockchain.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.internal.dlq.FailedEvent;
import zm.agriswift.blockchain.internal.dlq.FailedEventRepository;
import zm.agriswift.blockchain.internal.websocket.FireFlyEventDto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reprocesses poison-pill events parked in the local DLQ after
 * exhausting their 3 FireFly-level retries.
 *
 * <h3>SRP</h3>
 * <p>The scheduler ({@code BlockchainRetryScheduler}) owns <em>when</em>.
 * This service owns <em>what</em>: fetch → deserialise → reprocess →
 * backoff / permanent-failure.</p>
 *
 * <h3>Exponential Backoff Schedule</h3>
 * <pre>
 *   DLQ retry 1 →   60 s
 *   DLQ retry 2 →    5 min
 *   DLQ retry 3 →   25 min
 *   DLQ retry 4 →    2 hours
 *   DLQ retry 5 →   10 hours  →  PERMANENTLY FAILED
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqRecoveryService {

    private static final int MAX_DLQ_RETRIES = 5;

    private final FailedEventRepository failedEventRepository;
    private final AnchorEventHandlerService eventHandlerService;
    private final ObjectMapper objectMapper;

    /**
     * Fetches eligible DLQ entries (backoff elapsed, retries remaining)
     * and attempts reprocessing. Each entry is independent: one failure
     * does not roll back the batch.
     *
     * @return number of successfully recovered events
     */
    public int recoverEligibleFailures() {
        List<FailedEvent> eligible = failedEventRepository
                .findByRetryCountLessThanAndNextRetryAtBefore(
                        MAX_DLQ_RETRIES, Instant.now());

        if (eligible.isEmpty()) {
            log.debug("DLQ scan: no eligible failures.");
            return 0;
        }

        log.info("DLQ scan: {} eligible failure(s).", eligible.size());
        int recovered = 0;

        for (FailedEvent failure : eligible) {
            try {
                reprocess(failure);
                failedEventRepository.delete(failure);
                recovered++;
                log.info("DLQ recovery succeeded for anchor {}.",
                        failure.getAnchorId());
            } catch (Exception ex) {
                escalate(failure, ex);
            }
        }
        return recovered;
    }

    private void reprocess(FailedEvent failure) {
        FireFlyEventDto dto = objectMapper.readValue(
                failure.getRawPayload(), FireFlyEventDto.class);
        eventHandlerService.handleAnchorRecorded(dto);
    }

    @Transactional
    protected void escalate(FailedEvent failure, Exception cause) {
        int next = failure.getRetryCount() + 1;
        failure.setRetryCount(next);
        failure.setErrorMessage(cause.getMessage());
        failure.setUpdatedAt(Instant.now());

        if (next >= MAX_DLQ_RETRIES) {
            failure.setNextRetryAt(Instant.MAX);   // never retried again
            log.error("DLQ event for anchor {} PERMANENTLY FAILED after "
                            + "{} retries. Last error: {}",
                    failure.getAnchorId(), next, cause.getMessage());
        } else {
            Duration backoff = Duration.ofSeconds(60)
                    .multipliedBy((long) Math.pow(5, next - 1));
            failure.setNextRetryAt(Instant.now().plus(backoff));
            log.warn("DLQ retry {}/{} for anchor {} scheduled at {}.",
                    next, MAX_DLQ_RETRIES,
                    failure.getAnchorId(), failure.getNextRetryAt());
        }
        failedEventRepository.save(failure);
    }
}