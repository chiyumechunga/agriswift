package zm.agriswift.referencedata.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.agriswift.referencedata.CropPriceRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/crops")
public class CropPriceController {

    private final CropPriceRepository cropPriceRepository;

    public CropPriceController(CropPriceRepository cropPriceRepository) {
        this.cropPriceRepository = cropPriceRepository;
    }

    public record CropPriceResponse(Long cropPriceId, Short cropTypeId, String name,
                                    BigDecimal pricePerKg, BigDecimal maxMoisturePct) {}

    @GetMapping("/prices/current")
    public List<CropPriceResponse> currentPrices() {
        return cropPriceRepository.findAllActive(Instant.now()).stream()
                .map(p -> new CropPriceResponse(p.getCropPriceId(), p.getCropTypeId(), p.getName(),
                        p.getPricePerKg(), p.getMaxMoisturePct()))
                .toList();
    }
}