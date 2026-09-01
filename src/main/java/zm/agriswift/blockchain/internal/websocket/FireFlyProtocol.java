package zm.agriswift.blockchain.internal.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FireFlyProtocol {
    public record StartCommand(
            @JsonProperty("type") String type,
            @JsonProperty("namespace") String namespace,
            @JsonProperty("name") String name,
            @JsonProperty("ephemeral") boolean ephemeral
    ) {
        public static StartCommand create(String namespace, String name) {
            return new StartCommand("start", namespace, name, false);
        }
    }

    public record AckCommand(
            @JsonProperty("type") String type,
            @JsonProperty("id") String id
    ) {
        public static AckCommand forEvent(String eventId) {
            return new AckCommand("ack", eventId);
        }
    }
}