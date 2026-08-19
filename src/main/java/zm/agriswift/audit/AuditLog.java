package zm.agriswift.audit;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@IdClass(AuditLog.AuditId.class)
public class AuditLog {

    @Id
    @Column(name = "audit_id")
    private UUID auditId;

    @Id
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "resource", nullable = false, length = 80)
    private String resource;

    @Column(name = "resource_id", length = 80)
    private String resourceId;

    protected AuditLog() {
        // JPA
    }

    public static AuditLog of(UUID userId, String action, String resource, String resourceId) {
        AuditLog log = new AuditLog();
        log.auditId = UUID.randomUUID();
        log.occurredAt = Instant.now();
        log.userId = userId;
        log.action = action;
        log.resource = resource;
        log.resourceId = resourceId;
        return log;
    }

    public record AuditId(UUID auditId, Instant occurredAt) implements java.io.Serializable {
    }
}

