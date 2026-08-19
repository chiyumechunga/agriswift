package zm.agriswift.audit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, AuditLog.AuditId> {
}
