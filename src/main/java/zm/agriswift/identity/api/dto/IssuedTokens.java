package zm.agriswift.identity.api.dto;

public record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds) {}