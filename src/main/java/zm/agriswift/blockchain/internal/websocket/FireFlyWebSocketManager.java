package zm.agriswift.blockchain.internal.websocket;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.application.AnchorEventHandlerService;
import zm.agriswift.blockchain.internal.FireFlyProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class FireFlyWebSocketManager extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FireFlyWebSocketManager.class);

    private final FireFlyProperties properties;
    private final FireFlyProtocol protocol;
    private final ObjectMapper objectMapper;
    private final AnchorEventHandlerService eventHandler;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile WebSocketSession session;
    private volatile boolean shuttingDown = false;

    public FireFlyWebSocketManager(FireFlyProperties properties,
                                   FireFlyProtocol protocol,
                                   ObjectMapper objectMapper,
                                   AnchorEventHandlerService eventHandler) {
        this.properties = properties;
        this.protocol = protocol;
        this.objectMapper = objectMapper;
        this.eventHandler = eventHandler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        connect();
    }

    private synchronized void connect() {
        if (shuttingDown) return;
        try {
            log.info("Connecting to FireFly WebSocket: {}", properties.getWebSocketUrl());
            new StandardWebSocketClient()
                    .execute(this, properties.getWebSocketUrl())
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("FireFly WebSocket connect failed; retrying in 5s", e);
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        log.info("Connected to FireFly WebSocket");
        session.sendMessage(new TextMessage(protocol.startMessage()));
        log.info("Subscription started: namespace={} name={}",
                properties.getNamespace(), properties.getSubscriptionName());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        JsonNode root;
        try {
            root = objectMapper.readTree(payload); // Jackson 3: unchecked JacksonException
        } catch (Exception e) {
            log.error("Unparseable FireFly frame (cannot ack): {}", payload, e);
            return;
        }

        String type = root.path("type").asString("");
        String eventId = root.path("id").asString(null);

        switch (type) {
            case "blockchain_event_received" -> {
                JsonNode blockchainEvent = root.path("blockchainEvent");
                String eventName = blockchainEvent.path("name").asString("");
                if ("AnchorRecorded".equals(eventName)) {
                    log.info("AnchorRecorded received (eventId={})", eventId);
                    eventHandler.handleAnchorRecorded(blockchainEvent);
                } else {
                    log.debug("Ignoring blockchain event name={}", eventName);
                }
                ack(session, eventId); // processed -> ack (at-least-once)
            }
            case "protocol_error", "protocolError" -> log.error("FireFly protocol error: {}", payload);
            default -> {
                log.debug("Ignoring FireFly frame type={}", type);
                ack(session, eventId); // keep the readAhead pipeline flowing
            }
        }
    }

    private void ack(WebSocketSession session, String eventId) {
        if (eventId == null || eventId.isBlank()) return;
        try {
            session.sendMessage(new TextMessage(protocol.ackMessage(eventId)));
            log.info("Acked FireFly event id={}", eventId);
        } catch (Exception e) {
            log.error("Failed to ack event id={}", eventId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.session = null;
        if (!shuttingDown) {
            log.warn("FireFly WebSocket closed ({}); reconnecting in 5s", status);
            scheduleReconnect();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("FireFly WebSocket transport error", exception);
        try {
            if (session.isOpen()) session.close();
        } catch (Exception ignored) {
        }
    }

    private void scheduleReconnect() {
        if (!shuttingDown) reconnectExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        reconnectExecutor.shutdownNow();
        WebSocketSession current = session;
        if (current != null && current.isOpen()) {
            try {
                current.close();
            } catch (Exception ignored) {
            }
        }
    }
}