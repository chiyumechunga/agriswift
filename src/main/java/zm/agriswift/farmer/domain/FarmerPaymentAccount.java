package zm.agriswift.farmer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import zm.agriswift.common.CreationAuditedEntity;
import zm.agriswift.referencedata.PaymentProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "farmer_payment_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Satisfies JPA protected constructor
public class FarmerPaymentAccount extends CreationAuditedEntity {

    public enum AccountType { MOBILE_MONEY, BANK_TRANSFER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "account_number_ciphertext", columnDefinition = "BYTEA", nullable = false)
    private byte[] accountNumberCiphertext;

    @Column(name = "account_number_hash", columnDefinition = "BYTEA", nullable = false)
    private byte[] accountNumberHash;

    @Column(name = "holders_name", length = 150)
    private String holdersName;

    @Column(name = "sort_code", length = 6)
    private String sortCode;

    @Column(name = "is_verified_by_zechl", nullable = false)
    private boolean verifiedByZechl = false;

    @Column(name = "is_preferred", nullable = false)
    private boolean preferred = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private FarmerPaymentAccount(Farmer farmer, PaymentProvider provider, AccountType accountType,
                                 byte[] accountNumberCiphertext, byte[] accountNumberHash,
                                 String holdersName, String sortCode) {
        this.farmer = Objects.requireNonNull(farmer, "farmer");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.accountNumberCiphertext = Objects.requireNonNull(accountNumberCiphertext, "accountNumberCiphertext");
        this.accountNumberHash = Objects.requireNonNull(accountNumberHash, "accountNumberHash");
        this.holdersName = holdersName;
        this.sortCode = sortCode;
    }

    public static FarmerPaymentAccount bankAccount(Farmer farmer, PaymentProvider provider,
                                                   byte[] accountNumberCiphertext, byte[] accountNumberHash,
                                                   String holdersName, String sortCode) {
        Objects.requireNonNull(holdersName, "holdersName is required for BANK_TRANSFER");
        Objects.requireNonNull(sortCode, "sortCode is required for BANK_TRANSFER");
        return new FarmerPaymentAccount(farmer, provider, AccountType.BANK_TRANSFER,
                accountNumberCiphertext, accountNumberHash, holdersName, sortCode);
    }

    public static FarmerPaymentAccount mobileMoneyAccount(Farmer farmer, PaymentProvider provider,
                                                          byte[] accountNumberCiphertext, byte[] accountNumberHash) {
        return new FarmerPaymentAccount(farmer, provider, AccountType.MOBILE_MONEY,
                accountNumberCiphertext, accountNumberHash, null, null);
    }

    @PrePersist
    void onPersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void applyZechlVerification(String confirmedHoldersName) {
        this.holdersName = Objects.requireNonNull(confirmedHoldersName, "confirmedHoldersName");
        this.verifiedByZechl = true;
    }

    public void markPreferred() {
        this.preferred = true;
    }

    public void deactivate() {
        this.active = false;
        this.preferred = false;
    }
}