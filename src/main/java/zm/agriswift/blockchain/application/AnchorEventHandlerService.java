package zm.agriswift.blockchain.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import zm.agriswift.blockchain.api.AnchorNotarized;
import zm.agriswift.blockchain.domain.BlockchainCommitmentRepository;

@Slf4j
@Service
@NullMarked
@RequiredArgsConstructor
public class AnchorEventHandlerService {

    private final BlockchainCommitmentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void handleAnchorRecorded(JsonNode blockchainEvent) {
        JsonNode eventData = unwrapAnchorPayload(blockchainEvent);

        String anchorId = eventData.path("anchorId").asString();
        String onChainHash = eventData.path("payloadHash").asString();

        if (anchorId.isEmpty()) {
            throw new IllegalArgumentException("Missing anchorId in FireFly event payload");
        }

        var commitment = repository.findByAnchorId(anchorId)
                .orElseThrow(() -> new IllegalStateException(
                        "Received AnchorRecorded for unknown local anchor: " + anchorId));

        commitment.markCommitted(onChainHash);
        repository.save(commitment);

        eventPublisher.publishEvent(new AnchorNotarized(
                commitment.getAnchorId(),
                commitment.getAnchorType(),
                commitment.getEntityId(),
                onChainHash,
                commitment.getCommittedAt()
        ));

        log.info("Anchor {} committed on-chain (hash={}).", anchorId, onChainHash);
    }

    /** Unwraps FireFly's FFI envelope: output.data / output.data.data / output.data.value / output inline. */
    private JsonNode unwrapAnchorPayload(JsonNode blockchainEvent) {
        JsonNode output = blockchainEvent.path("output");
        JsonNode data = output.path("data");
        if (data.isObject()) {
            if (data.path("data").isObject()) return data.path("data");
            if (data.path("value").isObject()) return data.path("value");
            return data;
        }
        return output;
    }
}