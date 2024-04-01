package pro.abned.training.acid.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pro.abned.training.acid.BulkOperation;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;
import pro.abned.training.acid.repositories.AccountRepository;

import java.time.LocalDate;
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
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(Account companyAccount, Map<Account, Double> amounts) {
        final Account dbAccount = accountRepository.findById(companyAccount.getId()).orElseThrow(() -> new IllegalArgumentException("salary.payment.account.not_found"));
        double amountTotal = 0.0;
        for (var entry : amounts.entrySet()) {
            if (dbAccount.getBalance() < entry.getValue()) {
                throw new IllegalArgumentException("salary.payment.balance.insufficient");
            }
            var operation = Operation.builder()
                    .amount(entry.getValue())
                    .accountSourceId(companyAccount.getId())
                    .motif(String.format("SALARY FOR MONTH %s %d", LocalDate.now().getMonth().name(), LocalDate.now().getYear()))
                    .build();
            credit.credit(entry.getKey(), operation);
            dbAccount.setBalance(dbAccount.getBalance() - entry.getValue());
            amountTotal += entry.getValue();
        }
        var debitOperation = Operation.builder()
                .amount(amountTotal)
                .motif(String.format("PAYMENT SALARY FOR MONTH %s %d", LocalDate.now().getMonth().name(), LocalDate.now().getYear()))
                .build();
        debit.debit(companyAccount, debitOperation);
    }
}
