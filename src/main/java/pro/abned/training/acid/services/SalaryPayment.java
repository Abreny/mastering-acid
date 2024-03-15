package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.abned.training.acid.BulkOperation;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

import java.util.Map;

@Service
public class SalaryPayment implements BulkOperation {
    private final AccountRepository accountRepository;
    private final Credit credit;
    private final Debit debit;

    public SalaryPayment(AccountRepository accountRepository, Credit credit, Debit debit) {
        this.accountRepository = accountRepository;
        this.credit = credit;
        this.debit = debit;
    }

    @Override
    @Transactional
    public void execute(Account companyAccount, Map<Account, Double> amounts) {
        final Account dbAccount = accountRepository.findById(companyAccount.getId()).orElseThrow(() -> new IllegalArgumentException("salary.payment.account.not_found"));
        double amountTotal = 0.0;
        for (var entry : amounts.entrySet()) {
            if (dbAccount.getBalance() < entry.getValue()) {
                throw new IllegalArgumentException("salary.payment.balance.insufficient");
            }
            credit.credit(entry.getKey(), entry.getValue());
            dbAccount.setBalance(dbAccount.getBalance() - entry.getValue());
            amountTotal += entry.getValue();
        }
        debit.debit(companyAccount, amountTotal);
    }
}
