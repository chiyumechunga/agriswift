package zm.agriswift.disbursement.internal.airtel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agriswift.disbursement.airtel")
public record AirtelProperties(
        boolean enabled,
        String baseUrl,                    // https://openapiuat.airtel.africa
        String clientId,                   // OAuth2 client_id
        String clientSecret,               // OAuth2 client_secret
        String apiKey,                     // x-key header
        String merchantCode,               // SMC548XW-style code
        String merchantPin,                // business wallet PIN (encrypted before send)
        String rsaPrivateKeyBase64,        // YOUR private key -> x-signature
        String airtelRsaPublicKeyBase64,   // AIRTEL's public key -> PIN encryption
        String country,                    // ZM
        String currency                    // ZMW
) {}