package zm.agriswift.blockchain.internal.scheduler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zm.agriswift.blockchain.application.AnchorEventHandlerService;
import zm.agriswift.blockchain.internal.dlq.FailedEvent;
import zm.agriswift.blockchain.internal.dlq.FailedEventRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainRetryScheduler {

    private static final int MAX_RETRIES = 5;
    private final FailedEventRepository failedEventRepo;
    private final AnchorEventHandlerService eventProcessingService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void retryPoisonPills() {
        List<FailedEvent> failures = failedEventRepo.findByRetryCountLessThan(MAX_RETRIES);
        if (failures.isEmpty()) return;

        log.info("⏱️ Found {} failed blockchain events in DLQ. Attempting retry...", failures.size());

        for (FailedEvent failure : failures) {
            try {
                JsonNode originalEvent = objectMapper.readValue(failure.getRawPayload(), JsonNode.class);
                eventProcessingService.handleAnchorRecorded(originalEvent);

                failedEventRepo.delete(failure);
                log.info("✅ Successfully recovered DLQ event {}", failure.getTxId());

            } catch (Exception e) {
                failure.setRetryCount(failure.getRetryCount() + 1);
                failure.setErrorMessage(e.getMessage());
                failedEventRepo.save(failure);
                log.error("❌ DLQ Retry failed for event {}. Attempt {}/{}",
                        failure.getTxId(), failure.getRetryCount(), MAX_RETRIES);
            }
        }
    }
}