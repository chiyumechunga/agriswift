/**
 * AML module: watchlist screening of farmers around payment events.
 * Kept independent of disbursement's transaction so a slow/unavailable
 * watchlist provider never blocks a payout in flight.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common", "farmer", "disbursement", "farmer :: farmer"}
)

package zm.agriswift.aml;