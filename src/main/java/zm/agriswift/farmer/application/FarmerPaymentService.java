package zm.agriswift.farmer.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zm.agriswift.common.exception.DomainException;
import zm.agriswift.common.exception.NotFoundException;
import zm.agriswift.farmer.domain.FarmerPaymentAccount;
import zm.agriswift.farmer.internal.FarmerPaymentAccountRepository;

import java.util.UUID;

@Service
@Transactional
public class FarmerPaymentService {

    private final FarmerPaymentAccountRepository accountRepository;

    public FarmerPaymentService(FarmerPaymentAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void setPreferredPayoutAccount(UUID farmerId, UUID accountId) {
        // 1. Demote any existing preferred account (business rule)
        accountRepository.demotePreferredAccounts(farmerId);

        // 2. Promote the chosen account
        FarmerPaymentAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Payment account not found"));

        if (!account.getFarmer().getFarmerId().equals(farmerId)) {
            throw new DomainException("Account does not belong to this farmer.");
        }
        if (!account.isActive()) {
            throw new DomainException("Cannot set an inactive account as preferred.");
        }

        account.markPreferred();
        accountRepository.save(account);
    }
}