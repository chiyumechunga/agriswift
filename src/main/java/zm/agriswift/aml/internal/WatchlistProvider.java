package zm.agriswift.aml.internal;

import zm.agriswift.aml.AmlScreeningResult;

import java.util.UUID;

/** Placeholder for the real sanctions/watchlist screening provider integration. */
public interface WatchlistProvider {
    AmlScreeningResult.MatchStatus screen(UUID farmerId);
}
