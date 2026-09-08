package zm.agriswift.entitlement.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.entitlement.domain.Entitlement;
import zm.agriswift.entitlement.application.BuyoutService;
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

    private final BuyoutService buyoutService;
    private final DepotRepository depotRepository;
    private final CropTypeRepository cropTypeRepository;
    private final CropPriceRepository cropPriceRepository;

    public DeliveryController(BuyoutService buyoutService,
                              DepotRepository depotRepository,
                              CropTypeRepository cropTypeRepository,
                              CropPriceRepository cropPriceRepository) {
        this.buyoutService = buyoutService;
        this.depotRepository = depotRepository;
        this.cropTypeRepository = cropTypeRepository;
        this.cropPriceRepository = cropPriceRepository;
    }

    public record RecordDeliveryRequest(
            @NotNull UUID farmerId,
            @NotNull Integer depotId,     // depots.depot_id = IDENTITY Integer
            @NotNull Short cropTypeId,    // crop_types.crop_type_id = IDENTITY Short
            @NotNull Long cropPriceId,    // crop_prices.crop_price_id = IDENTITY Long
            @NotNull @Positive BigDecimal cropWeightKg,
            BigDecimal moistureContentPct,
            @NotNull LocalDate deliveryDate,
            String nationalIdHash
    ) {}

    @PostMapping("/deliveries")
    public ResponseEntity<UUID> recordDelivery(@RequestBody @Valid RecordDeliveryRequest req) {
        Depot depot = depotRepository.findById(req.depotId())
                .orElseThrow(() -> new IllegalArgumentException("Depot not found: " + req.depotId()));
        CropType cropType = cropTypeRepository.findById(req.cropTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Crop type not found: " + req.cropTypeId()));
        CropPrice cropPrice = cropPriceRepository.findById(req.cropPriceId())
                .orElseThrow(() -> new IllegalArgumentException("Crop price not found: " + req.cropPriceId()));

        Entitlement entitlement = new Entitlement(
                UUID.randomUUID(),
                req.farmerId(),   // ID-only reference — no cross-module entity import
                depot,
                cropType,
                cropPrice,
                req.cropWeightKg(),
                req.moistureContentPct(),
                req.deliveryDate()
        );

        buyoutService.recordAndValidate(entitlement, req.nationalIdHash());

        return ResponseEntity.status(HttpStatus.CREATED).body(entitlement.getEntitlementId());
    }
}