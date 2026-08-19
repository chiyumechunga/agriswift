package zm.agriswift.identity.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_token", columnList = "token"),
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token", length = 128)
    private String replacedByToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RefreshToken(String token, UUID userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /**
     * Marks this token as revoked and links it to the replacement token
     * for audit trails and reuse detection.
     */
    public void revoke(String replacementToken, Instant now) {
        this.revoked = true;
        this.revokedAt = now;
        this.replacedByToken = replacementToken;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isActive(Instant now) {
        return !revoked && !isExpired(now);
    }
}