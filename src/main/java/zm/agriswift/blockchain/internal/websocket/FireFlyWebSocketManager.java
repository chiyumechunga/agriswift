package zm.agriswift.blockchain.internal.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import zm.agriswift.blockchain.application.AnchorEventHandlerService;
import zm.agriswift.blockchain.internal.FireFlyProperties;
import zm.agriswift.blockchain.internal.dlq.FailedEvent;
import zm.agriswift.blockchain.internal.dlq.FailedEventRepository;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the persistent WebSocket connection to the Hyperledger FireFly
 * supernode and routes inbound {@code AnchorRecorded} chaincode events to
 * the {@link AnchorEventHandlerService}.
 *
 * <h3>Resilience Contract (NACK → 3 retries → DLQ)</h3>
 * <pre>
 *   Attempt 1  ──fail──►  skip ACK  ──►  FireFly redelivers (implicit NACK)
 *   Attempt 2  ──fail──►  skip ACK  ──►  FireFly redelivers
 *   Attempt 3  ──fail──►  persist to DLQ  ──►  ACK (unblock stream)
 * </pre>
 *
 * <p>The in-memory {@link #retryTracker} is intentionally ephemeral: if the
 * JVM restarts, FireFly will redeliver any un-ACKed events and the counter
 * resets. This is safe because {@code AnchorEventHandlerService} is
 * idempotent (duplicate COMMITTED events are no-ops).</p>
 */
@Slf4j
@Component
public class FireFlyWebSocketManager extends TextWebSocketHandler {

    /** Maximum local processing attempts before parking in the DLQ. */
    private static final int MAX_FIREFLY_RETRIES = 3;

    /** Backoff base for WebSocket reconnection (exponential). */
    private static final long RECONNECT_BASE_MS = 1_000;
    private static final long RECONNECT_MAX_MS  = 60_000;

    private final FireFlyProperties properties;
    private final FireFlyProtocol protocol;
    private final ObjectMapper objectMapper;
    private final AnchorEventHandlerService eventHandler;
    private final FailedEventRepository failedEventRepository;

    /** eventId → number of local processing failures. */
    private final Map<String, AtomicInteger> retryTracker = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile WebSocketSession session;
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                var t = new Thread(r, "firefly-reconnect");
                t.setDaemon(true);
                return t;
            });

    public FireFlyWebSocketManager(FireFlyProperties properties,
                                   FireFlyProtocol protocol,
                                   ObjectMapper objectMapper,
                                   AnchorEventHandlerService eventHandler,
                                   FailedEventRepository failedEventRepository) {
        this.properties = properties;
        this.protocol = protocol;
        this.objectMapper = objectMapper;
        this.eventHandler = eventHandler;
        this.failedEventRepository = failedEventRepository;
    }

    // ──────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void connectOnStartup() {
        if (running.compareAndSet(false, true)) {
            doConnect(0);
        }
    }

    /** Graceful shutdown – called by Spring on context close. */
    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void disconnectOnShutdown() {
        running.set(false);
        reconnectExecutor.shutdownNow();
        closeSessionQuietly();
        log.info("FireFly WebSocket manager shut down.");
    }

    private void doConnect(int attempt) {
        if (!running.get()) return;
        try {
            var client = new StandardWebSocketClient();
            var uri = URI.create(properties.getWebSocketUrl());
            log.info("Connecting to FireFly WebSocket: {}", uri);
            session = client.execute(this, new WebSocketHttpHeaders(), uri)
                    .get(10, TimeUnit.SECONDS);
            log.info("Connected to FireFly WebSocket");
        } catch (Exception ex) {
            long delay = Math.min(
                    RECONNECT_BASE_MS * (1L << Math.min(attempt, 6)),
                    RECONNECT_MAX_MS);
            log.warn("FireFly WS connect failed (attempt {}). Retrying in {} ms.",
                    attempt + 1, delay);
            reconnectExecutor.schedule(() -> doConnect(attempt + 1),
                    delay, TimeUnit.MILLISECONDS);
        }
    }

    // ──────────────────────────────────────────────────────────
    //  Inbound message routing
    // ──────────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage message) {
        String payload = message.getPayload();
        try {
            var dto = objectMapper.readValue(payload, FireFlyEventDto.class);

            // Ignore non-event control frames (e.g. "subscribed", "pong")
            if (dto.id() == null || dto.data() == null) {
                log.debug("Control frame received: {}", payload);
                return;
            }

            log.info("FireFly event received: id={} type={}", dto.id(), dto.type());

            // Delegate to the application service (idempotent)
            eventHandler.handleAnchorRecorded(dto);

            // ✅ Success → ACK and clear retry state
            sendAck(ws, dto.id());
            retryTracker.remove(dto.id());

        } catch (Exception ex) {
            handleProcessingFailure(ws, payload, ex);
        }
    }

    /**
     * NACK-by-omission with 3-strike DLQ escalation.
     *
     * <ol>
     *   <li>Increment the in-memory failure counter for this event.</li>
     *   <li>If failures &lt; 3: skip the ACK. FireFly will redeliver
     *       after its server-side timeout (implicit NACK).</li>
     *   <li>If failures &ge; 3: persist to the local DLQ, ACK the event
     *       (unblocking the stream), and remove the tracker entry.</li>
     * </ol>
     */
    private void handleProcessingFailure(WebSocketSession ws,
                                         String rawPayload,
                                         Exception ex) {
        String eventId = extractEventId(rawPayload);
        int failures = retryTracker
                .computeIfAbsent(eventId, k -> new AtomicInteger(0))
                .incrementAndGet();

        log.warn("Processing attempt {}/{} failed for event {}: {}",
                failures, MAX_FIREFLY_RETRIES, eventId, ex.getMessage());

        if (failures >= MAX_FIREFLY_RETRIES) {
            log.error("Max retries ({}) exhausted for event {}. Routing to DLQ.",
                    MAX_FIREFLY_RETRIES, eventId);
            parkInDlq(eventId, rawPayload, ex);
            sendAck(ws, eventId);          // unblock the FireFly stream
            retryTracker.remove(eventId);
        }
        // else: no ACK sent → FireFly will redeliver (implicit NACK)
    }

    private void parkInDlq(String eventId, String rawPayload, Exception cause) {
        try {
            var failed = new FailedEvent();
            failed.setAnchorId(eventId);
            failed.setEventType("ANCHOR_RECORDED");
            failed.setRawPayload(rawPayload);
            failed.setErrorMessage(cause.getMessage());
            failed.setRetryCount(0);
            failed.setNextRetryAt(Instant.now().plusSeconds(60));
            failed.setCreatedAt(Instant.now());
            failed.setUpdatedAt(Instant.now());
            failedEventRepository.save(failed);
            log.info("Event {} persisted to DLQ.", eventId);
        } catch (Exception dlqEx) {
            log.error("CRITICAL: Failed to persist event {} to DLQ. "
                    + "Event may be lost.", eventId, dlqEx);
        }
    }

    private void sendAck(WebSocketSession ws, String eventId) {
        try {
            if (ws.isOpen()) {
                ws.sendMessage(new TextMessage(protocol.ackMessage(eventId)));
                log.debug("ACK sent for event {}", eventId);
            }
        } catch (Exception ex) {
            log.warn("Failed to send ACK for event {}: {}",
                    eventId, ex.getMessage());
        }
    }

    private String extractEventId(String rawPayload) {
        try {
            var node = objectMapper.readTree(rawPayload);
            var idNode = node.get("id");
            return (idNode != null && !idNode.isNull())
                    ? idNode.asString()
                    : "unknown-" + System.nanoTime();
        } catch (Exception ex) {
            return "unparseable-" + System.nanoTime();
        }
    }

    // ──────────────────────────────────────────────────────────
    //  Connection lifecycle callbacks
    // ──────────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) throws Exception {
        log.info("Subscription started: namespace={} name={}",
                properties.getNamespace(), properties.getSubscriptionName());
        ws.sendMessage(new TextMessage(protocol.startMessage()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        log.warn("FireFly WS closed: {}. Scheduling reconnect.", status);
        if (running.get()) {
            reconnectExecutor.schedule(
                    () -> doConnect(0), RECONNECT_BASE_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession ws, Throwable exception) {
        log.error("FireFly WS transport error", exception);
        closeSessionQuietly();
    }

    private void closeSessionQuietly() {
        try {
            if (session != null && session.isOpen()) session.close();
        } catch (Exception ignored) { /* best-effort */ }
    }
}