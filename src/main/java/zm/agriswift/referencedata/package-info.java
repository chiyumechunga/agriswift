/**
 * Reference / lookup data: depots, crop types, seasonal crop pricing,
 * payment providers (mobile money or bank), and the chart of accounts.
 * Low write-rate, high read-rate — mostly maintained by FRA_ADMIN users.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"common"}
)
package zm.agriswift.referencedata;