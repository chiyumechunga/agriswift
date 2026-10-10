package zm.agriswift.common.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
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
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int BLIND_INDEX_LENGTH_BYTES = 32; // 256-bit output (matches HMAC-SHA256 length)

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey encryptionKey;
    private final Argon2Parameters argon2Parameters;

    public PiiCipher(
            @Value("${agriswift.security.pii.aes-key-base64}") String aesKeyBase64,
            @Value("${agriswift.security.pii.argon2-salt-base64}") String argon2SaltBase64) {

        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        byte[] argon2SaltBytes = Base64.getDecoder().decode(argon2SaltBase64);

        if (aesKeyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 32 bytes (256-bit)");
        }
        if (argon2SaltBytes.length < 16) {
            throw new IllegalArgumentException("Argon2 salt (pepper) must be at least 16 bytes");
        }

        this.encryptionKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Argon2id configuration for deterministic blind indexing.
        // Memory-hard to prevent GPU/ASIC brute-forcing if the DB is leaked.
        this.argon2Parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(16384) // 16 MB memory cost (takes ~15ms on modern CPUs)
                .withIterations(3)     // 3 iterations
                .withParallelism(1)    // 1 thread (safe for web request threads)
                .withSalt(argon2SaltBytes)
                .build();
    }

    /**
     * Generates a deterministic Argon2id blind index for secure, exact-match database lookups.
     * Note: BouncyCastle does not throw checked exceptions, so the signature is cleaner.
     */
    public byte[] generateBlindIndex(String rawString) {
        if (rawString == null) {
            throw new IllegalArgumentException("Raw string cannot be null");
        }

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(argon2Parameters);

        byte[] hash = new byte[BLIND_INDEX_LENGTH_BYTES];
        // Argon2 expects char[] for the secret/input
        generator.generateBytes(rawString.toCharArray(), hash);

        return hash;
    }

    /**
     * Encrypts plaintext using AES-256-GCM and prepends the 12-byte IV.
     */
    public byte[] encrypt(String rawString) throws GeneralSecurityException {
        if (rawString == null) {
            throw new IllegalArgumentException("Raw string cannot be null");
        }
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

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
     * Decrypts the combined payload (IV plus ciphertext) using AES-256-GCM.
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