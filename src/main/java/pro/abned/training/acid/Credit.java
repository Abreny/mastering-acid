package pro.abned.training.acid;

import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;

public interface Credit {
    void credit(Account account, Operation operation);
}
