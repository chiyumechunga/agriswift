package zm.agriswift.farmer.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PiiCipher {

    private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    // Thread-safe and non-blocking entropy source
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey encryptionKey;
    private final SecretKey hmacKey;

    public PiiCipher(
            @Value("${agriswift.security.pii.aes-key-base64}") String aesKeyBase64,
            @Value("${agriswift.security.pii.hmac-key-base64}") String hmacKeyBase64) {

        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        byte[] hmacKeyBytes = Base64.getDecoder().decode(hmacKeyBase64);

        if (aesKeyBytes.length != 32) { // 256-bit AES
            throw new IllegalArgumentException("AES key must be 32 bytes (256-bit)");
        }

        this.encryptionKey = new SecretKeySpec(aesKeyBytes, "AES");
        this.hmacKey = new SecretKeySpec(hmacKeyBytes, HMAC_ALGO);
    }

    /**
     * Generates a deterministic HMAC-SHA256 blind index for database lookups.
     */
    public byte[] generateBlindIndex(String rawString) throws GeneralSecurityException {
        if (rawString == null) {
            throw new IllegalArgumentException("Raw string cannot be null");
        }
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(hmacKey);
        return mac.doFinal(rawString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypts plaintext using AES-GCM and prepends the 12-byte IV.
     */
    public byte[] encrypt(String rawString) throws GeneralSecurityException {
        if (rawString == null) {
            throw new IllegalArgumentException("Raw string cannot be null");
        }
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv); // Non-blocking secure random

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);

        byte[] encryptedBytes = cipher.doFinal(rawString.getBytes(StandardCharsets.UTF_8));

        byte[] combinedPayload = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combinedPayload, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length);

        return combinedPayload;
    }

    /**
     * Decrypts the combined payload (IV plus ciphertext) using AES-GCM.
     */
    public String decrypt(byte[] combinedPayload) throws GeneralSecurityException {
        if (combinedPayload == null || combinedPayload.length < GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid ciphertext payload");
        }

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        System.arraycopy(combinedPayload, 0, iv, 0, iv.length);

        int ciphertextLength = combinedPayload.length - GCM_IV_LENGTH_BYTES;
        byte[] encryptedBytes = new byte[ciphertextLength];
        System.arraycopy(combinedPayload, GCM_IV_LENGTH_BYTES, encryptedBytes, 0, ciphertextLength);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);

        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }
}