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
import zm.agriswift.blockchain.internal.websocket.FireFlyEventDto;
import zm.agriswift.common.exception.NotFoundException;

/**
 * Application Service that processes inbound {@code AnchorRecorded}
 * chaincode events arriving via the FireFly WebSocket.
 *
 * <h3>SRP</h3>
 * <ul>
 *   <li>Parse the FireFly event envelope.</li>
 *   <li>Load the {@link BlockchainCommitment} aggregate.</li>
 *   <li>Apply the idempotent {@code markCommitted} transition
 *       (trust-the-chain hash policy).</li>
 *   <li>Publish {@link AnchorNotarized} via
 *       {@link ApplicationEventPublisher}. Because downstream consumers
 *       use {@code @ApplicationModuleListener}, Spring Modulith
 *       automatically persists the event to the {@code event_publication}
 *       table for reliable, at-least-once cross-module delivery.</li>
 * </ul>
 *
 * <h3>Modulith Event Externalisation</h3>
 * <p>No special annotation is required on this publisher side. Spring
 * Modulith intercepts {@code publishEvent()} transparently when at least
 * one {@code @ApplicationModuleListener} exists in another module.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnchorEventHandlerService {

    private final BlockchainCommitmentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Processes a single {@code AnchorRecorded} event from FireFly.
     * Idempotent: duplicate COMMITTED events are silently ignored.
     *
     * @param dto the deserialised FireFly WebSocket event envelope
     */
    @Transactional
    public void handleAnchorRecorded(FireFlyEventDto dto) {
        JsonNode anchorPayload = dto.getAnchorPayload();
        if (anchorPayload == null) {
            log.warn("FireFly event {} contained no anchor payload – skipping.",
                    dto.id());
            return;
        }

        String anchorId    = anchorPayload.get("anchorId").asString();
        String onChainHash = anchorPayload.get("payloadHash").asString();

        log.info("Processing AnchorRecorded: anchorId={} onChainHash={}",
                anchorId, onChainHash);

        // Load the aggregate (throws NotFoundException if absent → DLQ path)
        BlockchainCommitment commitment = repository.findByAnchorId(anchorId)
                .orElseThrow(() -> new NotFoundException(
                        "BlockchainCommitment not found for anchorId=" + anchorId));

        // Idempotent – trusts the on-chain hash unconditionally
        commitment.markCommitted(onChainHash);
        repository.save(commitment);

        // Spring Modulith intercepts this and persists to event_publication
        // BEFORE any @ApplicationModuleListener executes.
        eventPublisher.publishEvent(new AnchorNotarized(
                commitment.getAnchorId(),
                commitment.getAnchorType(),
                commitment.getEntityId(),
                commitment.getPayloadHash(),      // on-chain authoritative hash
                commitment.getCommittedAt()
        ));

        log.info("AnchorNotarized event published for anchorId={}", anchorId);
    }
}