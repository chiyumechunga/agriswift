package zm.agriswift.blockchain.internal.websocket;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireFlyEventDto(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("name") String name,
        @JsonProperty("data") JsonNode data,
        @JsonProperty("transaction") TransactionInfo transaction
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionInfo(
            @JsonProperty("id") String id
    ) {
    }

    /**
     * Safely extracts the Go chaincode payload regardless of how
     * FireFly's FFI mapper nested it (data / data.data / data.value).
     */
    public JsonNode getAnchorPayload() {
        if (data == null || data.isNull()) {
            return null;
        }
        if (data.has("anchorId")) {
            return data;
        }
        if (data.has("data")) {
            return data.get("data");
        }
        if (data.has("value")) {
            return data.get("value");
        }
        return data;
    }
}