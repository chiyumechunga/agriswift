package zm.agriswift.blockchain.internal.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zm.agriswift.blockchain.application.DlqRecoveryService;

/**
 * Infrastructure trigger. Owns ONLY the scheduling concern.
 * All business orchestration lives in {@link DlqRecoveryService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainRetryScheduler {

    private final DlqRecoveryService dlqRecoveryService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryPoisonPills() {
        log.debug("DLQ retry cycle triggered.");
        int recovered = dlqRecoveryService.recoverEligibleFailures();
        if (recovered > 0) {
            log.info("DLQ cycle complete: {} event(s) recovered.", recovered);
        }
    }
}