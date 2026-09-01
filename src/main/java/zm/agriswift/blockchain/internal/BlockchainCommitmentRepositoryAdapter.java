package zm.agriswift.blockchain.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import zm.agriswift.blockchain.domain.BlockchainCommitment;
import zm.agriswift.blockchain.domain.BlockchainCommitmentRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class BlockchainCommitmentRepositoryAdapter implements BlockchainCommitmentRepository {

    private final BlockchainCommitmentJpaRepository jpa;

    @Override
    public Optional<BlockchainCommitment> findByAnchorId(String anchorId) {
        return jpa.findById(anchorId);
    }

    @Override
    public BlockchainCommitment save(BlockchainCommitment commitment) {
        return jpa.save(commitment);
    }
}