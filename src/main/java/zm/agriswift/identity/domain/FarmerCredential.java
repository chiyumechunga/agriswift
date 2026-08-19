package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import zm.agriswift.common.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmer_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FarmerCredential extends BaseEntity {

    @Id
    @Column(name = "farmer_id")
    private UUID farmerId;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;          // stored as Argon2idyj hash

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Business methods
    public void recordFailedAttempt(short maxAttempts, long lockDurationMinutes) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockDurationMinutes * 60);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
}