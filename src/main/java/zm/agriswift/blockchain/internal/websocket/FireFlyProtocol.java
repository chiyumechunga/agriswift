package zm.agriswift.blockchain.internal.websocket;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.internal.FireFlyProperties;

import java.util.Map;

@Component
public class FireFlyProtocol {

    private final FireFlyProperties properties;
    private final ObjectMapper objectMapper;

    public FireFlyProtocol(FireFlyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String startMessage() {
        return objectMapper.writeValueAsString(Map.of(
                "type", "start",
                "namespace", properties.getNamespace(),
                "name", properties.getSubscriptionName()
        ));
    }

    public String ackMessage(String eventId) {
        return objectMapper.writeValueAsString(Map.of(
                "type", "ack",
                "id", eventId
        ));
    }
}