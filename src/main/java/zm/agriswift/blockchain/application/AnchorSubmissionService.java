package zm.agriswift.blockchain.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.blockchain.domain.BlockchainCommitment;
import zm.agriswift.blockchain.domain.BlockchainCommitmentRepository;
import zm.agriswift.blockchain.internal.firefly.FireFlyNodeRouter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AnchorSubmissionService {

    private final BlockchainCommitmentRepository repository;
    private final FireFlyNodeRouter fireFlyRouter;

    @Transactional
    public void submitAnchor(String anchorId, String anchorType, String entityId, String rawDomainEventJson) {
        // 1. Compute SHA-256 locally
        String hash = computeSha256(rawDomainEventJson);

        // 2. Save local commitment (State: PENDING)
        BlockchainCommitment commitment = BlockchainCommitment.create(
                anchorId, anchorType, entityId, rawDomainEventJson, hash
        );
        repository.save(commitment);

        // 3. Delegate to infrastructure (FireFly REST)
        // Note: This might throw an exception if FireFly is down.
        // The caller (e.g., Event Listener) should handle this or rely on a scheduler.
        fireFlyRouter.invokeRecordAnchor(commitment);

        commitment.markSubmitted();
        repository.save(commitment);
    }

    private String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }
}