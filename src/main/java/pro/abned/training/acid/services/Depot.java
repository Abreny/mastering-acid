package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;
import pro.abned.training.acid.entities.OperationType;
import pro.abned.training.acid.repositories.AccountRepository;
import pro.abned.training.acid.repositories.OperationRepository;

import java.time.LocalDateTime;

@Service
public class Depot implements Credit {
    private final AccountRepository accountRepository;
    private final OperationRepository operationRepository;

    public Depot(AccountRepository accountRepository, OperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void credit(Account account, Operation operation) {
        assert operation.getAmount() != null;
        if (operation.getAmount() <= 0) {
            throw new IllegalArgumentException("depot.amount.negative");
        }
        final Account dbAccount = accountRepository.findById(account.getId()).orElseThrow(() -> new IllegalArgumentException("depot.account.not_found"));
        dbAccount.setBalance(dbAccount.getBalance() + operation.getAmount());
        accountRepository.save(dbAccount);

        operation.setType(OperationType.CREDIT);
        operation.setAccountId(dbAccount.getId());
        operation.setValuedAt(LocalDateTime.now());
        operation.setMotif(operation.getMotif() == null ? "DEPOT" : operation.getMotif());
        operationRepository.save(operation);
    }
}
