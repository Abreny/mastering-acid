package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;
import pro.abned.training.acid.entities.OperationType;
import pro.abned.training.acid.repositories.AccountRepository;
import pro.abned.training.acid.repositories.OperationRepository;

import java.time.LocalDateTime;

@Service
public class Retrait implements Debit {
    private final AccountRepository accountRepository;
    private final OperationRepository operationRepository;

    public Retrait(AccountRepository accountRepository, OperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void debit(Account account, Operation operation) {
        assert operation.getAmount() != null;
        if (operation.getAmount() <= 0) {
            throw new IllegalArgumentException("retrait.amount.negative");
        }
        final Account dbAccount = accountRepository.findById(account.getId()).orElseThrow(() -> new IllegalArgumentException("retrait.account.not_found"));

        if (operation.getAmount() > dbAccount.getBalance()) {
            throw new IllegalArgumentException("retrait.amount.insufficient");
        }
        dbAccount.setBalance(dbAccount.getBalance() - operation.getAmount());
        accountRepository.save(dbAccount);

        operation.setType(OperationType.DEBIT);
        operation.setAccountId(dbAccount.getId());
        operation.setValuedAt(LocalDateTime.now());
        operation.setMotif(operation.getMotif() == null ? "RETRAIT" : operation.getMotif());
        operationRepository.save(operation);
    }
}
