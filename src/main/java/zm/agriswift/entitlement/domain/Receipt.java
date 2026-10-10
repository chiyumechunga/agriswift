package zm.agriswift.entitlement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import zm.agriswift.common.CreationAuditedEntity;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * Mirrors FRA's paper "Produce Receipt and Certification Note" process.
 *
 * <p>The issuing agent, capturing device, and network state are part of the
 * receipt's identity because they are the offline-sync audit trail: when a
 * receipt is later reconciled, these columns prove <em>who</em> issued it,
 * on <em>which device</em>, over <em>what network</em>. They are NOT NULL
 * with no DB defaults, so the domain factory requires them as arguments.
 */
@Entity
@Table(name = "receipts")
public class Receipt extends CreationAuditedEntity {

    public enum Status { ISSUED, ACTIVE, VOIDED, RECONCILED }

    @Getter
    @Id
    @Column(name = "receipt_id")
    private UUID receiptId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_id", nullable = false, unique = true)
    private Entitlement entitlement;

    @Getter
    @Column(name = "system_serial_number", unique = true, length = 40, nullable = false)
    private String systemSerialNumber;

    @Column(name = "qr_code_token", nullable = false, unique = true)
    private String qrCodeToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ISSUED;

    /** Staff user who issued this receipt (FK → users). */
    @Column(name = "issued_by_agent_id", nullable = false)
    private UUID issuedByAgentId;

    /** Capturing device identifier (offline-sync audit). */
    @Column(name = "device_hw_id", nullable = false, length = 100)
    private String deviceHwId;

    /** Network state at capture time (e.g. ONLINE / OFFLINE / CELLULAR). */
    @Column(name = "network_used", nullable = false, length = 40)
    private String networkUsed;

    @Column(name = "is_printed", nullable = false)
    private boolean isPrinted = false;

    @Column(name = "is_synced_delayed", nullable = false)
    private boolean isSyncedDelayed = false;

    /** Transcription-safe alphabet: no I, L, O, 0, 1 for depot staff reading aloud. */
    private static final String SERIAL_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    protected Receipt() {
        // JPA
    }

    /**
     * Domain factory: the receipt is the farmer's paper trail and may only
     * exist for a VALIDATED entitlement. Serial format: FRA-<year>-<8 chars>.
     *
     * @param entitlement     the validated entitlement this receipt covers

     */
    public static Receipt issue(Entitlement entitlement, ReceiptIssuanceContext ctx) {
        Objects.requireNonNull(entitlement, "entitlement");
        Objects.requireNonNull(ctx, "issuanceCtx");

        if (entitlement.getValidationStatus() != Entitlement.ValidationStatus.VALIDATED) {
            throw new IllegalStateException(
                    "Receipt may only be issued for a VALIDATED entitlement, but was: "
                            + entitlement.getValidationStatus());
        }

        Receipt receipt = new Receipt();
        receipt.receiptId          = UUID.randomUUID();
        receipt.entitlement        = entitlement;
        receipt.systemSerialNumber =
                "FRA-" + entitlement.getDeliveryDate().getYear() + "-" + randomSuffix(8);
        receipt.qrCodeToken        = UUID.randomUUID().toString();
        receipt.issuedByAgentId    = ctx.issuedByAgentId();
        receipt.deviceHwId         = ctx.deviceHwId();
        receipt.networkUsed        = ctx.networkUsed();
        receipt.status             = Status.ISSUED;
        return receipt;
    }

    public void markActive() {
        if (status == Status.ISSUED) {
            status = Status.ACTIVE;
        }
    }

    public void markReconciled() {
        if (status == Status.VOIDED) {
            throw new IllegalStateException("Cannot reconcile a VOIDED receipt.");
        }
        status = Status.RECONCILED;
    }

    public void voidReceipt() {
        if (status == Status.RECONCILED) {
            throw new IllegalStateException("Cannot void a RECONCILED receipt.");
        }
        status = Status.VOIDED;
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SERIAL_ALPHABET.charAt(RANDOM.nextInt(SERIAL_ALPHABET.length())));
        }
        return sb.toString();
    }
}