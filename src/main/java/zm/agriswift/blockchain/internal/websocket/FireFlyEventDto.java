package zm.agriswift.blockchain.internal.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

/**
 * Deserialised FireFly WebSocket event envelope.
 *
 * <p>Jackson 3.x keeps its annotations in the legacy
 * {@code com.fasterxml.jackson.annotation} package (the shared
 * {@code jackson-annotations} artifact), while the runtime lives in
 * {@code tools.jackson.databind}. Mixing them exactly like this is
 * correct on Spring Boot 4.1.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FireFlyEventDto(
        @JsonProperty("id")          String id,
        @JsonProperty("type")        String type,
        @JsonProperty("namespace")   String namespace,
        @JsonProperty("name")        String name,
        @JsonProperty("data")        JsonNode data,
        @JsonProperty("transaction") TransactionInfo transaction
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionInfo(
            @JsonProperty("id") String id
    ) {}

    /**
     * Extracts the Go chaincode payload regardless of how FireFly's
     * FFI mapper nested it (data / data.data / data.value).
     */
    public JsonNode getAnchorPayload() {
        if (data == null || data.isNull()) return null;
        if (data.has("data")  && data.get("data").isObject())
            return data.get("data");
        if (data.has("value") && data.get("value").isObject())
            return data.get("value");
        return data.isObject() ? data : null;
    }
}