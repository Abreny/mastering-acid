package pro.abned.training.acid;

import pro.abned.training.acid.entities.Account;

public interface Debit {
    void debit(Account account, double amount);
}
