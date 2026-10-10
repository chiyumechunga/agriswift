package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import zm.agriswift.common.CreationAuditedEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmer_credentials")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)   // required by @Builder
@EntityListeners(AuditingEntityListener.class)
public class FarmerCredential extends CreationAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id")
    private UUID credentialId;

    @Column(name = "farmer_id", nullable = false, unique = true)
    private UUID farmerId;

    @Column(name = "national_id_hash", nullable = false, unique = true)
    private byte[] nationalIdHash;

    @Column(name = "mobile_number_hash", unique = true)
    private byte[] mobileNumberHash;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    // ---------- Factory ----------
    public static FarmerCredential issue(UUID farmerId, byte[] nationalIdHash, byte[] mobileNumberHash,
                                         String email, String encodedPin) {
        FarmerCredential c = new FarmerCredential();
        c.farmerId = farmerId;
        c.nationalIdHash = nationalIdHash;
        c.mobileNumberHash = mobileNumberHash;
        c.email = email;
        c.pinHash = encodedPin;
        c.active = true;
        c.failedAttempts = 0;
        c.lastUpdated = Instant.now();
        return c;
    }

    // ---------- Behaviour ----------
    public void updatePin(String encodedPin) {
        this.pinHash = encodedPin;
        resetFailedAttempts();
        this.lastUpdated = Instant.now();
    }

    public void recordFailedAttempt(short maxAttempts, long lockDurationMinutes) {
        this.failedAttempts++;
        this.lastUpdated = Instant.now();
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
    public void reactivate() { this.active = true; }
}