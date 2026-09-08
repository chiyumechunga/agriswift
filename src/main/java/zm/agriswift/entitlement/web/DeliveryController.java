package zm.agriswift.entitlement.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.exception.NotFoundException;
import zm.agriswift.entitlement.application.BuyoutService;
import zm.agriswift.entitlement.domain.Entitlement;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.referencedata.CropPrice;
import zm.agriswift.referencedata.CropPriceRepository;
import zm.agriswift.referencedata.CropType;
import zm.agriswift.referencedata.CropTypeRepository;
import zm.agriswift.referencedata.Depot;
import zm.agriswift.referencedata.DepotRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entitlements")
public class DeliveryController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);

    private final BuyoutService buyoutService;
    private final FarmerDirectory farmerDirectory;
    private final DepotRepository depotRepository;
    private final CropTypeRepository cropTypeRepository;
    private final CropPriceRepository cropPriceRepository;

    public DeliveryController(BuyoutService buyoutService,
                              FarmerDirectory farmerDirectory,
                              DepotRepository depotRepository,
                              CropTypeRepository cropTypeRepository,
                              CropPriceRepository cropPriceRepository) {
        this.buyoutService = buyoutService;
        this.farmerDirectory = farmerDirectory;
        this.depotRepository = depotRepository;
        this.cropTypeRepository = cropTypeRepository;
        this.cropPriceRepository = cropPriceRepository;
    }

    public record RecordDeliveryRequest(
            @NotNull UUID farmerId,
            @NotNull Integer depotId,      // depots.depot_id = IDENTITY Integer
            @NotNull Short cropTypeId,     // crop_types.crop_type_id = IDENTITY Short
            @NotNull Long cropPriceId,     // crop_prices.crop_price_id = IDENTITY Long
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
            @RequestBody @Valid RecordDeliveryRequest req) {

        // Validate farmer exists and is active
        FarmerSummary farmer = farmerDirectory.findById(req.farmerId())
                .orElseThrow(() -> new NotFoundException("Farmer not found: " + req.farmerId()));
        if (!farmer.isActive()) {
            throw new DomainException(
                    "Farmer " + farmer.farmerCode() + " is deactivated; deliveries cannot be recorded.");
        }
        if (!farmer.kycVerified()) {
            // V1 policy: e-KYC is a placeholder — warn, do not block. Tighten in V2.
            log.warn("Farmer {} recorded a delivery without verified KYC (V1 placeholder policy).",
                    farmer.farmerCode());
        }

        // Validate referenced data exists
        Depot depot = depotRepository.findById(req.depotId())
                .orElseThrow(() -> new NotFoundException("Depot not found: " + req.depotId()));
        CropType cropType = cropTypeRepository.findById(req.cropTypeId())
                .orElseThrow(() -> new NotFoundException("Crop type not found: " + req.cropTypeId()));
        CropPrice cropPrice = cropPriceRepository.findById(req.cropPriceId())
                .orElseThrow(() -> new NotFoundException("Crop price not found: " + req.cropPriceId()));

        Entitlement entitlement = new Entitlement(
                UUID.randomUUID(),
                farmer.farmerId(),   // ID-only reference — no cross-module entity import
                depot,
                cropType,
                cropPrice,
                req.cropWeightKg(),
                req.moistureContentPct(),
                req.deliveryDate()
        );

        BuyoutService.BuyoutOutcome outcome =
                buyoutService.recordAndValidate(entitlement, req.nationalIdHash());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DeliveryRecordedResponse(
                        outcome.entitlementId(),
                        outcome.validationStatus(),
                        outcome.receiptSerialNumber()));
    }
}