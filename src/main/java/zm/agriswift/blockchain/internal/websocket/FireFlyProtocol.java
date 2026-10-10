package zm.agriswift.blockchain.internal.websocket;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.internal.FireFlyProperties;

import java.util.Map;

/**
 * Serialises the FireFly WebSocket subscription protocol frames.
 *
 * <h3>FireFly WS Protocol (v1)</h3>
 * <ul>
 *   <li><b>start</b> – client subscribes to a named event stream.</li>
 *   <li><b>ack</b>   – client confirms successful processing; advances the
 *       FireFly event cursor so the event is never redelivered.</li>
 * </ul>
 *
 * <h3>NACK-by-Omission Strategy</h3>
 * <p>FireFly has <strong>no explicit NACK frame</strong>. If the client
 * does not send an {@code ack} within the server-side timeout
 * ({@code eventPollTimeout}, default 120 s), FireFly automatically
 * redelivers the event. We exploit this:</p>
 * <ol>
 *   <li>Processing fails → we <em>skip</em> the ACK (implicit NACK).</li>
 *   <li>FireFly redelivers after its timeout.</li>
 *   <li>After 3 local failures we persist to the DLQ and <em>then</em>
 *       ACK, unblocking the stream for subsequent events.</li>
 * </ol>
 */
@Component
public class FireFlyProtocol {

    private final FireFlyProperties properties;
    private final ObjectMapper objectMapper;

    public FireFlyProtocol(FireFlyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Builds the {@code {"type":"start", ...}} subscription frame. */
    public String startMessage() {
        return objectMapper.writeValueAsString(Map.of(
                "type", "start",
                "namespace", properties.getNamespace(),
                "name", properties.getSubscriptionName()
        ));
    }

    /**
     * Builds the {@code {"type":"ack", "id":"..."}} confirmation frame.
     * Call this ONLY after successful processing <em>or</em> after the
     * event has been safely parked in the local DLQ.
     */
    public String ackMessage(String eventId) {
        return objectMapper.writeValueAsString(Map.of(
                "type", "ack",
                "id", eventId
        ));
    }
}