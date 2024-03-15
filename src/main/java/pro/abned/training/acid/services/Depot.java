package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

@Service
public class Depot implements Credit {
    private final AccountRepository accountRepository;

    public Depot(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void credit(Account account, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("depot.amount.negative");
        }
        final Account dbAccount = accountRepository.findById(account.getId()).orElseThrow(() -> new IllegalArgumentException("depot.account.not_found"));
        dbAccount.setBalance(dbAccount.getBalance() + amount);
        accountRepository.save(dbAccount);
    }
}
