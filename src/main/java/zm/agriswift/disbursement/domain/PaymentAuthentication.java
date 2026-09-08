package zm.agriswift.disbursement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_authentications")
public class PaymentAuthentication extends CreationAuditedEntity {

    public enum Method { OTP, PIN, BIOMETRIC }

    @Getter
    @Id
    @Column(name = "auth_id")
    private UUID authId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private Method method;

    @Column(name = "otp_code_hash")
    private byte[] otpCodeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_successful")
    private Boolean successful;

    protected PaymentAuthentication() {
        // JPA
    }

}
