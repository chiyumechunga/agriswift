package zm.agriswift.entitlement.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.common.security.CurrentUser;
import zm.agriswift.entitlement.application.BuyoutService;
import zm.agriswift.entitlement.application.DeliveryApplicationService;
import zm.agriswift.entitlement.application.RecordDeliveryCommand;
import zm.agriswift.entitlement.domain.ReceiptIssuanceContext;
import zm.agriswift.identity.api.dto.UserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * HTTP adapter for the Entitlement module.
 *
 * <p>Adheres to SRP by strictly handling HTTP protocol concerns:
 * request validation, header extraction, authentication context mapping,
 * and response serialization. All business orchestration is delegated
 * to {@link DeliveryApplicationService}.
 */
@RestController
@RequestMapping("/api/v1/entitlements")
public class DeliveryController {

    private final DeliveryApplicationService deliveryUseCase;

    public DeliveryController(DeliveryApplicationService deliveryUseCase) {
        this.deliveryUseCase = deliveryUseCase;
    }

    public record RecordDeliveryRequest(
            @NotNull UUID farmerId,
            @NotNull Integer depotId,
            @NotNull Short cropTypeId,
            @NotNull Long cropPriceId,
            @NotNull @Positive
            @DecimalMax(value = "50000.00", message = "a single delivery cannot exceed 50,000 kg")
            BigDecimal cropWeightKg,
            @DecimalMin(value = "0.00", message = "moisture content cannot be negative")
            @DecimalMax(value = "100.00", message = "moisture content cannot exceed 100%")
            BigDecimal moistureContentPct,
            @NotNull @PastOrPresent(message = "delivery date cannot be in the future")
            LocalDate deliveryDate,
            String nationalIdHash
    ) {}

    public record DeliveryRecordedResponse(
            UUID entitlementId,
            String validationStatus,
            String receiptSerialNumber
    ) {}

    @PostMapping("/deliveries")
    public ResponseEntity<DeliveryRecordedResponse> recordDelivery(
            @RequestBody @Valid RecordDeliveryRequest req,
            @CurrentUser UserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", defaultValue = "WEB-CONSOLE") String deviceHwId,
            @RequestHeader(value = "X-Network",   defaultValue = "ONLINE")      String networkUsed) {

        // 1. Map HTTP DTO to Application Command
        RecordDeliveryCommand command = new RecordDeliveryCommand(
                req.farmerId(), req.depotId(), req.cropTypeId(), req.cropPriceId(),
                req.cropWeightKg(), req.moistureContentPct(), req.deliveryDate(), req.nationalIdHash()
        );

        // 2. Map HTTP context to Domain Context
        ReceiptIssuanceContext issuanceCtx = new ReceiptIssuanceContext(
                principal.id(), deviceHwId, networkUsed
        );

        // 3. Delegate to Use Case
        BuyoutService.BuyoutOutcome outcome = deliveryUseCase.recordDelivery(command, issuanceCtx);

        // 4. Map Domain Outcome to HTTP Response
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DeliveryRecordedResponse(
                        outcome.entitlementId(),
                        outcome.validationStatus(),
                        outcome.receiptSerialNumber()));
    }
}