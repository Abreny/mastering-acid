package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

@Service
public class Retrait implements Debit {
    private final AccountRepository accountRepository;

    public Retrait(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void debit(Account account, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("retrait.amount.negative");
        }
        final Account dbAccount = accountRepository.findById(account.getId()).orElseThrow(() -> new IllegalArgumentException("retrait.account.not_found"));

        if (amount > dbAccount.getBalance()) {
            throw new IllegalArgumentException("retrait.amount.insufficient");
        }
        dbAccount.setBalance(dbAccount.getBalance() - amount);
        accountRepository.save(dbAccount);
    }
}
