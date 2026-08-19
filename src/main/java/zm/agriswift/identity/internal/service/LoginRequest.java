package zm.agriswift.identity.internal.service;

public record LoginRequest(
        // Staff login fields
        String username,
        String password,
        // Farmer login fields
        String identifier,   // can be mobile, email, or NRC
        String pin
) {}