package zm.agriswift.blockchain.internal.websocket;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import zm.agriswift.blockchain.application.AnchorEventHandlerService;
import zm.agriswift.blockchain.internal.FireFlyProperties;
import zm.agriswift.blockchain.internal.dlq.FailedEvent;
import zm.agriswift.blockchain.internal.dlq.FailedEventRepository;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class FireFlyWebSocketManager extends TextWebSocketHandler {

    private final FireFlyProperties properties;
    private final AnchorEventHandlerService eventService;
    private final FailedEventRepository failedEventRepo;
    private final ObjectMapper mapper;

    private WebSocketSession session;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    @PostConstruct
    public void init() { connect(); }

    private void connect() {
        if (isConnected.get()) return;
        try {
            log.info("🔌 Connecting to FireFly WebSocket: {}", properties.webSocketUrl());
            StandardWebSocketClient client = new StandardWebSocketClient();
            this.session = client.execute(this, properties.webSocketUrl()).get();
            isConnected.set(true);
            sendSubscriptionStart();
        } catch (Exception e) {
            log.error("❌ Failed to connect to FireFly WebSocket. Will retry...", e);
            isConnected.set(false);
        }
    }

    private void sendSubscriptionStart() throws Exception {
        FireFlyProtocol.StartCommand startCmd = FireFlyProtocol.StartCommand.create(
                properties.namespace(), properties.subscriptionName()
        );
        session.sendMessage(new TextMessage(mapper.writeValueAsString(startCmd)));
        log.info("📡 Subscribed to FireFly namespace: {}", properties.namespace());
    }

    @Scheduled(fixedDelay = 30000)
    public void reconnectIfNecessary() {
        if (!isConnected.get()) connect();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("⚠️ FireFly WebSocket closed: {}. Will attempt reconnect.", status);
        isConnected.set(false);
    }

    // Inside FireFlyWebSocketManager.java

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Map directly to the DTO
        FireFlyEventDto eventDto = mapper.readValue(message.getPayload(), FireFlyEventDto.class);

        // We only care about contract events (or specifically AnchorRecorded)
        if ("contract_event".equals(eventDto.type()) || "AnchorRecorded".equals(eventDto.name())) {

            try {
                // 1. Process business logic
                eventService.handleAnchorRecorded(eventDto);

                // 2. SUCCESS: Send ACK to advance FireFly cursor
                sendAck(session, eventDto.id());

            } catch (Exception e) {
                // 3. POISON PILL: Save to DLQ and ACK to prevent stream freezing
                log.error("❌ Business processing failed for event {}. Routing to DLQ.", eventDto.id(), e);
                saveToDeadLetterQueue(eventDto, e.getMessage());
                sendAck(session, eventDto.id());
            }
        }
    }

    private void saveToDeadLetterQueue(FireFlyEventDto eventDto, String errorMessage) {
        try {
            FailedEvent failedEvent = new FailedEvent();
            failedEvent.setTxId(eventDto.id() != null ? eventDto.id() : "UNKNOWN");
            failedEvent.setRawPayload(mapper.writeValueAsString(eventDto)); // Serialize DTO back to JSON for DLQ
            failedEvent.setErrorMessage(errorMessage);
            failedEvent.setRetryCount(0);
            failedEventRepo.save(failedEvent);
        } catch (Exception ex) {
            log.error("Critical: Failed to save poison pill to DLQ.", ex);
        }
    }

    private void sendAck(WebSocketSession session, String eventId) throws Exception {
        if (session != null && session.isOpen()) {
            FireFlyProtocol.AckCommand ack = FireFlyProtocol.AckCommand.forEvent(eventId);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(ack)));
        }
    }

    private void saveToDeadLetterQueue(JsonNode eventNode, String errorMessage) {
        try {
            FailedEvent failedEvent = new FailedEvent();
            failedEvent.setTxId(eventNode.path("id").asText("UNKNOWN"));
            failedEvent.setRawPayload(eventNode.toString());
            failedEvent.setErrorMessage(errorMessage);
            failedEvent.setRetryCount(0);
            failedEventRepo.save(failedEvent);
        } catch (Exception ex) {
            log.error("Critical: Failed to save poison pill to DLQ.", ex);
        }
    }

    @PreDestroy
    public void disconnect() throws Exception {
        if (session != null && session.isOpen()) session.close();
    }
}