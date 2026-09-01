package zm.agriswift.blockchain.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import zm.agriswift.blockchain.api.AnchorNotarized;
import zm.agriswift.blockchain.domain.BlockchainCommitment;
import zm.agriswift.blockchain.domain.BlockchainCommitmentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnchorEventHandlerService {

    private final BlockchainCommitmentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handleAnchorRecorded(@org.jetbrains.annotations.UnknownNullability JsonNode event) {
        // DTO helper safely unwraps FireFly's FFI envelope (data / data.data / data.value)
        JsonNode eventData = event.getAnchorPayload();

        if (eventData == null || eventData.isNull()) {
            throw new IllegalArgumentException("FireFly event missing anchor payload");
        }

        // Jackson 3: asText() was renamed to asString()
        String anchorId = eventData.path("anchorId").asString();
        String onChainHash = eventData.path("payloadHash").asString();

        if (anchorId.isEmpty()) {
            throw new IllegalArgumentException("Missing anchorId in FireFly event payload");
        }

        BlockchainCommitment commitment = repository.findByAnchorId(anchorId)
                .orElseThrow(() -> new IllegalStateException(
                        "Received AnchorRecorded for unknown local anchor: " + anchorId));

        // Idempotent state update (verifies hash, ignores duplicates)
        commitment.markCommitted(onChainHash);
        repository.save(commitment);

        // Shout to the rest of the Modular Monolith
        eventPublisher.publishEvent(new AnchorNotarized(
                commitment.getAnchorId(),
                commitment.getAnchorType(),
                commitment.getEntityId(),
                onChainHash,
                commitment.getCommittedAt()
        ));

        log.info("Anchor {} committed on-chain.", anchorId);
    }
}