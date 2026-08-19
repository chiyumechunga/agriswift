package zm.agriswift.aml.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import zm.agriswift.aml.AmlScreeningResult;
import zm.agriswift.farmer.api.FarmerDirectory;

import java.util.UUID;

@Component
class WatchlistProviderImpl implements WatchlistProvider {

    private static final Logger log = LoggerFactory.getLogger(WatchlistProviderImpl.class);

    private FarmerDirectory farmerDirectory;



    WatchlistProviderImpl(FarmerDirectory farmerDirectory) {
        this.farmerDirectory = farmerDirectory;
    }

    @Override
    public AmlScreeningResult.MatchStatus screen(UUID farmerId) {
        return farmerDirectory.findById(farmerId)
                .map(farmerSummary -> {
                    log.info("Screening farmer [{}] against AML sanctions watchlists", farmerSummary.farmerCode());

                    // TODO: Replace with real REST/gRPC sanctions API client (e.g., Zambia FIC)
                    // Example screening attributes: farmerSummary.fullName(), farmerSummary.farmerCode()

                    return AmlScreeningResult.MatchStatus.CLEAR;
                })
                .orElseGet(() -> {
                    log.warn("Cannot perform AML screening: Farmer [{}] not found or anonymized", farmerId);
                    return AmlScreeningResult.MatchStatus.REJECTED;
                });
    }
}