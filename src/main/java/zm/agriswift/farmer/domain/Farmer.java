package zm.agriswift.farmer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import zm.agriswift.common.BaseEntity;
import zm.agriswift.referencedata.Depot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "farmers")
@Getter
@Setter (AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Farmer extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "farmer_id")
    private UUID farmerId;

    @Column(name = "farmer_code", nullable = false, unique = true, length = 20)
    private String farmerCode;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "national_id_ciphertext", nullable = false)
    private byte[] nationalIdCiphertext;

    @Column(name = "national_id_hash", nullable = false, unique = true)
    private byte[] nationalIdHash;

    @Column(name = "mobile_number_ciphertext")
    private byte[] mobileNumberCiphertext;

    @Column(name = "mobile_number_hash", unique = true)
    private byte[] mobileNumberHash;

    @Column(name = "email")
    private String email;

    @Column(name = "preferred_language", nullable = false, length = 20)
    private String preferredLanguage = "ENGLISH";

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "biometric_hash")
    private byte[] biometricHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_channel", nullable = false)
    private OnboardingChannel onboardingChannel;

    @Column(name = "registering_agent_id")
    private UUID registeringAgentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registering_depot_id")
    private Depot registeringDepot;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "encryption_key_version", nullable = false)
    private short encryptionKeyVersion = 1;

    @OneToMany(mappedBy = "farmer")
    private List<FarmerPaymentAccount> paymentAccounts = new ArrayList<>();

    // ----------- State-changing behaviour -----------

    public void assignFraOfficerRegistration(UUID agentId, Depot depot, OnboardingChannel channel) {
        if (channel == OnboardingChannel.SELF_REGISTRATION) {
            throw new IllegalArgumentException("SELF_REGISTRATION not allowed for FRA officer.");
        }
        this.registeringAgentId = Objects.requireNonNull(agentId);
        this.registeringDepot = Objects.requireNonNull(depot);
        this.onboardingChannel = channel;
    }

    public void assignSelfRegistration() {
        this.registeringAgentId = null;
        this.registeringDepot = null;
        this.onboardingChannel = OnboardingChannel.SELF_REGISTRATION;
    }

    public void markKycVerified(Instant verifiedAt) {
        if (this.kycStatus == KycStatus.VERIFIED) {
            throw new IllegalStateException("KYC already verified.");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerifiedAt = verifiedAt;
    }

    public void markKycRejected() {
        this.kycStatus = KycStatus.REJECTED;
        this.kycVerifiedAt = Instant.now(); // optional: keep timestamp for audit
    }

    public void deactivate() { this.active = false; }

    public void reactivate() { this.active = true; }

    // Helper for display (no PII leak)
    public String getFullName() {
        return (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
    }
}