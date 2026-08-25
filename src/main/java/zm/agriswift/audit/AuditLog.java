package zm.agriswift.audit;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@IdClass(AuditLog.AuditId.class)      // partitioned table => composite PK via @IdClass
@Getter
public class AuditLog {

    @Id
    @Column(name = "audit_id")
    private UUID auditId;

    @Id                                 // partition key = identity; assigned in factory
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "actor_type", length = 10)
    private String actorType;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "resource", nullable = false, length = 80)
    private String resource;

    @Column(name = "resource_id", length = 80)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)        // jsonb column needs an explicit JSON jdbc type
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    protected AuditLog() { /* JPA requires a no-arg constructor */ }

    /**
     * Factory method ensures the composite PK (auditId, occurredAt) is fully
     * populated before the entity is passed to the repository.
     */
    public static AuditLog record(UUID userId, String actorType, String action,
                                  String resource, String resourceId,
                                  String details, String ipAddress) {
        AuditLog log = new AuditLog();
        log.auditId = UUID.randomUUID();
        log.occurredAt = Instant.now();
        log.userId = userId;
        log.actorType = actorType;
        log.action = action;
        log.resource = resource;
        log.resourceId = resourceId;
        log.details = details;
        log.ipAddress = ipAddress;
        return log;
    }

    public static AuditLog of(UUID userId, String action, String resource, String resourceId) {
        AuditLog log = new AuditLog();
        log.auditId   = UUID.randomUUID();   // PK part 1 (assigned — no @GeneratedValue with @IdClass)
        log.occurredAt = Instant.now();      // PK part 2 = partition key; MUST be set pre-insert
        log.userId    = userId;
        log.action    = action;
        log.resource  = resource;
        log.resourceId = resourceId;
        // actorType left null: the CHECK ('STAFF','FARMER') passes for NULL;
        // add an overload of(userId, actorType, ...) later if callers need it.
        return log;
    }

    /**
     * Component names MUST exactly mirror the @Id field names in the entity
     * for the @IdClass binding to work.
     */
    public record AuditId(UUID auditId, Instant occurredAt)
            implements java.io.Serializable { }
}