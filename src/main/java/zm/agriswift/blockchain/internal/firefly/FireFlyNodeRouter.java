package zm.agriswift.blockchain.internal.firefly;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.domain.BlockchainCommitment;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FireFlyNodeRouter {

    private final WebClient fireflyWebClient;
    private final ObjectMapper objectMapper;

    /**
     * Maps to ffi.json -> methods -> RecordAnchor
     * Param: payloadJSON (string)
     */
    public void invokeRecordAnchor(BlockchainCommitment commitment) {
        try {
            // 1. Build the Go AnchorPayload struct equivalent
            Map<String, String> goPayload = Map.of(
                    "anchorId", commitment.getAnchorId(),
                    "anchorType", commitment.getAnchorType(),
                    "entityId", commitment.getEntityId(),
                    "payloadJSON", commitment.getPayloadJson() // The raw domain event JSON
            );

            // 2. Serialize to string (Go expects a JSON string that it will Unmarshal)
            String payloadJSONString = objectMapper.writeValueAsString(goPayload);

            // 3. Call FireFly FFI REST API
            // Body format required by FireFly custom contracts: {"payloadJSON": "..."}
            Map<String, String> requestBody = Map.of("payloadJSON", payloadJSONString);

            fireflyWebClient.post()
                    .uri("/apis/agriswift/1.0.0/invoke/RecordAnchor")
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Block for simplicity; in high-throughput, use async Mono

            log.info("🚀 Submitted anchor {} to FireFly.", commitment.getAnchorId());

        } catch (Exception e) {
            log.error("❌ Failed to submit anchor {} to FireFly.", commitment.getAnchorId(), e);
            throw new RuntimeException("FireFly submission failed", e);
        }
    }
}