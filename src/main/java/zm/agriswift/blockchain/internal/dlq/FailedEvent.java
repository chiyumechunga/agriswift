package zm.agriswift.blockchain.internal.dlq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blockchain_failed_events")
@Getter @Setter
public class FailedEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String txId;

    @Lob
    private String rawPayload;

    @Lob
    private String errorMessage;

    private int retryCount;
    private Instant createdAt = Instant.now();
}