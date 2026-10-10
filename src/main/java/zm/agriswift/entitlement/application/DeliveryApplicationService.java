package zm.agriswift.entitlement.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.exception.NotFoundException;
import zm.agriswift.entitlement.domain.Entitlement;
import zm.agriswift.entitlement.domain.ReceiptIssuanceContext;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.referencedata.CropPrice;
import zm.agriswift.referencedata.CropPriceRepository;
import zm.agriswift.referencedata.CropType;
import zm.agriswift.referencedata.CropTypeRepository;
import zm.agriswift.referencedata.Depot;
import zm.agriswift.referencedata.DepotRepository;

import java.util.UUID;

/**
 * Orchestrates the "Record Delivery" use case.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch and validate cross-module reference data (Farmer, Depot, CropType, CropPrice).</li>
 *   <li>Construct the {@link Entitlement} aggregate root.</li>
 *   <li>Delegate domain validation and receipt issuance to {@link BuyoutService}.</li>
 * </ul>
 */
@Service
public class DeliveryApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryApplicationService.class);

    private final FarmerDirectory farmerDirectory;
    private final DepotRepository depotRepository;
    private final CropTypeRepository cropTypeRepository;
    private final CropPriceRepository cropPriceRepository;
    private final BuyoutService buyoutService;

    public DeliveryApplicationService(FarmerDirectory farmerDirectory,
                                      DepotRepository depotRepository,
                                      CropTypeRepository cropTypeRepository,
                                      CropPriceRepository cropPriceRepository,
                                      BuyoutService buyoutService) {
        this.farmerDirectory = farmerDirectory;
        this.depotRepository = depotRepository;
        this.cropTypeRepository = cropTypeRepository;
        this.cropPriceRepository = cropPriceRepository;
        this.buyoutService = buyoutService;
    }

    @Transactional
    public BuyoutService.BuyoutOutcome recordDelivery(RecordDeliveryCommand command,
                                                      ReceiptIssuanceContext issuanceCtx) {
        // 1. Validate farmer exists and is active
        FarmerSummary farmer = farmerDirectory.findById(command.farmerId())
                .orElseThrow(() -> new NotFoundException("Farmer not found: " + command.farmerId()));

        if (!farmer.isActive()) {
            throw new DomainException(
                    "Farmer " + farmer.farmerCode() + " is deactivated; deliveries cannot be recorded.");
        }
        if (!farmer.kycVerified()) {
            log.warn("Farmer {} recorded a delivery without verified KYC (V1 placeholder policy).",
                    farmer.farmerCode());
        }

        // 2. Validate referenced data exists
        Depot depot = depotRepository.findById(command.depotId())
                .orElseThrow(() -> new NotFoundException("Depot not found: " + command.depotId()));
        CropType cropType = cropTypeRepository.findById(command.cropTypeId())
                .orElseThrow(() -> new NotFoundException("Crop type not found: " + command.cropTypeId()));
        CropPrice cropPrice = cropPriceRepository.findById(command.cropPriceId())
                .orElseThrow(() -> new NotFoundException("Crop price not found: " + command.cropPriceId()));

        // 3. Construct the aggregate
        Entitlement entitlement = new Entitlement(
                UUID.randomUUID(),
                farmer.farmerId(),
                depot,
                cropType,
                cropPrice,
                command.cropWeightKg(),
                command.moistureContentPct(),
                command.deliveryDate()
        );

        // 4. Delegate to domain service
        return buyoutService.recordAndValidate(entitlement, command.nationalIdHash(), issuanceCtx);
    }
}