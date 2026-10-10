package zm.agriswift.entitlement.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.entitlement.api.CropBuyoutRecorded;
import zm.agriswift.entitlement.domain.Entitlement;
import zm.agriswift.entitlement.domain.EntitlementRepository;
import zm.agriswift.entitlement.domain.Receipt;
import zm.agriswift.entitlement.domain.ReceiptIssuanceContext;
import zm.agriswift.entitlement.domain.ReceiptRepository;
import zm.agriswift.entitlement.domain.ZiamisRegistryPort;

import java.util.UUID;

/**
 * Validates a recorded delivery, issues the paper-trail receipt and, on success,
 * publishes {@link CropBuyoutRecorded} — this is the "Crop buyout recorded" box at
 * the top of disbursement_flow.png.
 */
@Service
public class BuyoutService {

    private static final Logger log = LoggerFactory.getLogger(BuyoutService.class);

    private final EntitlementRepository entitlementRepository;
    private final ReceiptRepository receiptRepository;
    private final ApplicationEventPublisher events;
    private final ZiamisRegistryPort ziamisRegistryPort;

    public BuyoutService(EntitlementRepository entitlementRepository,
                         ReceiptRepository receiptRepository,
                         ApplicationEventPublisher events,
                         ZiamisRegistryPort ziamisRegistryPort) {
        this.entitlementRepository = entitlementRepository;
        this.receiptRepository = receiptRepository;
        this.events = events;
        this.ziamisRegistryPort = ziamisRegistryPort;
    }

    /** Use-case outcome, safe to expose to the web layer (no entities leak). */
    public record BuyoutOutcome(UUID entitlementId, String validationStatus, String receiptSerialNumber) {}

    /**
     * @param entitlement   the aggregate to validate and persist
     * @param nationalIdHash blind-index source for the ZIAMIS registry check
     * @param issuanceCtx   immutable audit context for the receipt
     *                      (agent, device, network) — satisfies the receipt
     *                      table NOT NULL columns without primitive sprawl
     */
    @Transactional
    public BuyoutOutcome recordAndValidate(Entitlement entitlement,
                                           String nationalIdHash,
                                           ReceiptIssuanceContext issuanceCtx) {
        entitlement.applyMoistureRuleAndPrice();

        boolean ziamisMatch = ziamisRegistryPort.isFarmerRegistered(
                entitlement.getFarmerId(), nationalIdHash);
        entitlement.validate(ziamisMatch);

        entitlementRepository.save(entitlement);

        String receiptSerial = null;
        if (entitlement.getValidationStatus() == Entitlement.ValidationStatus.VALIDATED) {
            // Single context object → adding a future audit field (e.g. GPS)
            // changes only ReceiptIssuanceContext, never this signature.
            Receipt receipt = receiptRepository.save(Receipt.issue(entitlement, issuanceCtx));
            receiptSerial = receipt.getSystemSerialNumber();

            events.publishEvent(new CropBuyoutRecorded(
                    entitlement.getEntitlementId(),
                    entitlement.getFarmerId(),
                    entitlement.getPaymentAmount(),
                    "ZMW"));
            log.info("Entitlement {} validated; receipt {} issued; buyout event published",
                    entitlement.getEntitlementId(), receiptSerial);
        } else {
            log.warn("Entitlement {} REJECTED (moistureOK={}, ziamisMatch={})",
                    entitlement.getEntitlementId(), entitlement.getMoistureCheckPassed(), ziamisMatch);
        }

        return new BuyoutOutcome(
                entitlement.getEntitlementId(),
                entitlement.getValidationStatus().name(),
                receiptSerial);
    }
}