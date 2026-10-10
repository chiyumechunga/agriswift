package zm.agriswift.entitlement.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Application-layer command representing the intent to record a crop delivery.
 * Decoupled from HTTP-specific validation annotations, which belong in the web layer.
 */
public record RecordDeliveryCommand(
        UUID farmerId,
        Integer depotId,
        Short cropTypeId,
        Long cropPriceId,
        BigDecimal cropWeightKg,
        BigDecimal moistureContentPct,
        LocalDate deliveryDate,
        String nationalIdHash
) {}