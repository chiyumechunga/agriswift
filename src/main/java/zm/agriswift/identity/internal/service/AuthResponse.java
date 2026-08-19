package zm.agriswift.identity.internal.service;

import zm.agriswift.identity.api.dto.UserPrincipal;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, UserPrincipal user) {}