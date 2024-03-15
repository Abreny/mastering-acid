package pro.abned.training.acid;

import pro.abned.training.acid.entities.Account;

import java.util.Map;

public interface BulkOperation {
    void execute(Account companyAccount, Map<Account, Double> amounts);
}
