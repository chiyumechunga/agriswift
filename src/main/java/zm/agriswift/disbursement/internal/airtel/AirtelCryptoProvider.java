package zm.agriswift.disbursement.internal.airtel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "agriswift.disbursement.airtel.enabled", havingValue = "true")
public class AirtelCryptoProvider {

    private static final String RSA_TRANSFORM = "RSA/ECB/PKCS1Padding";

    private final AirtelProperties properties;

    public AirtelCryptoProvider(AirtelProperties properties) {
        this.properties = properties;
    }

    /**
     * x-signature header: RSA operation over the exact request body
     * using OUR private key. Verify the padding scheme against the
     * onboarding docs once keys are provisioned.
     */
    public String sign(String requestBody) {
        try {
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(properties.rsaPrivateKeyBase64())));

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Airtel x-signature", e);
        }
    }

    /**
     * Encrypts the merchant wallet PIN with AIRTEL's public key
     * before placing it in the "pin" field.
     */
    public String encryptPin(String pin) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(properties.airtelRsaPublicKeyBase64())));

            Cipher cipher = Cipher.getInstance(RSA_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt PIN for Airtel", e);
        }
    }
}