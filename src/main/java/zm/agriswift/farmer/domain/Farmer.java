package zm.agriswift.farmer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA only
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

    // ---------- Factory Method ----------
    public static Farmer register(
            String farmerCode,
            String firstName,
            String middleName,
            String lastName,
            LocalDate dateOfBirth,
            byte[] nationalIdCiphertext,
            byte[] nationalIdHash,
            byte[] mobileNumberCiphertext,
            byte[] mobileNumberHash,
            String email,
            String preferredLanguage,
            OnboardingChannel onboardingChannel,
            UUID registeringAgentId,
            Depot registeringDepot
    ) {
        Farmer farmer = new Farmer();
        farmer.farmerCode = Objects.requireNonNull(farmerCode, "farmerCode");
        farmer.firstName = Objects.requireNonNull(firstName, "firstName");
        farmer.middleName = middleName;
        farmer.lastName = Objects.requireNonNull(lastName, "lastName");
        farmer.dateOfBirth = dateOfBirth;
        farmer.nationalIdCiphertext = Objects.requireNonNull(nationalIdCiphertext, "nationalIdCiphertext");
        farmer.nationalIdHash = Objects.requireNonNull(nationalIdHash, "nationalIdHash");
        farmer.mobileNumberCiphertext = mobileNumberCiphertext;
        farmer.mobileNumberHash = mobileNumberHash;
        farmer.email = email;
        farmer.preferredLanguage = preferredLanguage != null ? preferredLanguage : "ENGLISH";
        farmer.applyOnboarding(onboardingChannel, registeringAgentId, registeringDepot);
        return farmer;
    }

    public void assignSelfRegistration() {
        this.registeringAgentId = null;
        this.registeringDepot = null;
        this.onboardingChannel = OnboardingChannel.SELF_REGISTRATION;
    }
    // ---------- Private Onboarding Logic (encapsulated) ----------
    private void applyOnboarding(OnboardingChannel channel, UUID agentId, Depot depot) {
        if (channel == OnboardingChannel.SELF_REGISTRATION) {
            this.registeringAgentId = null;
            this.registeringDepot = null;
            this.onboardingChannel = OnboardingChannel.SELF_REGISTRATION;
        } else {
            // FRA_DEPOT or FRA_FIELD_OFFICER
            if (agentId == null || depot == null) {
                throw new IllegalArgumentException(
                        "Agent and depot are required for FRA onboarding channels."
                );
            }
            this.registeringAgentId = agentId;
            this.registeringDepot = depot;
            this.onboardingChannel = channel;
        }
    }

    // ---------- State-changing behaviour (already present) ----------
    public void markKycVerified(Instant verifiedAt) {
        if (this.kycStatus == KycStatus.VERIFIED) {
            throw new IllegalStateException("KYC already verified.");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerifiedAt = verifiedAt;
    }

    public void markKycRejected() {
        this.kycStatus = KycStatus.REJECTED;
        this.kycVerifiedAt = Instant.now();
    }

    public void deactivate() { this.active = false; }

    public void reactivate() { this.active = true; }

    public String getFullName() {
        return (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
    }
}