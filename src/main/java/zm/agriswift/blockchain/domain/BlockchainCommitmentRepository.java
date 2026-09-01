package zm.agriswift.blockchain.domain;

import java.util.Optional;

public interface BlockchainCommitmentRepository {
    Optional<BlockchainCommitment> findByAnchorId(String anchorId);
    BlockchainCommitment save(BlockchainCommitment commitment);
}