package zm.agriswift.identity.internal.security;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public  class RefreshTokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private RefreshTokenGenerator() {}

    /** 512 bits of CSPRNG entropy — this is what goes to the client. */
   public static String generateRaw() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of the raw token — this is what goes in the DB (64 hex chars, fits the existing length=128 column). */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}