package pro.abned.training.acid;

import pro.abned.training.acid.entities.Account;

public interface Credit {
    void credit(Account account, double amount);
}
