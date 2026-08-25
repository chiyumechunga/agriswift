package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import zm.agriswift.common.CreationAuditedEntity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmer_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // Enables @CreatedDate
public class FarmerCredential extends CreationAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "farmer_id", nullable = false, unique = true)
    private UUID farmerId;

    @Column(name = "pin_hash", nullable = false)
    private String  pinHash;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    // --- AUDITING FIELDS ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    // --- FACTORY METHOD ---

    public static FarmerCredential issue(UUID farmerId, String encodedPin) {
        FarmerCredential c = new FarmerCredential();
        c.farmerId = farmerId;
        c.pinHash = encodedPin;
        c.lastUpdated = Instant.now(); // Set initial last_updated manually
        return c;
    }

    public void updatePin(String encodedPin) {
        this.pinHash = encodedPin;
        resetFailedAttempts();
    }

    public void recordFailedAttempt(short maxAttempts, long lockDurationMinutes) {
        this.failedAttempts++;
        this.lastUpdated = Instant.now(); // Audit the failed attempt
        if (this.failedAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockDurationMinutes * 60);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        // Note: We don't update lastUpdated here because this is usually
        // called as part of updatePin() or a successful login.
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
}