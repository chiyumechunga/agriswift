package zm.agriswift.blockchain.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import zm.agriswift.blockchain.domain.BlockchainCommitment;

interface BlockchainCommitmentJpaRepository
        extends JpaRepository<BlockchainCommitment, String> {
}