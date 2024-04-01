package pro.abned.training.acid;

import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;

public interface Debit {
    void debit(Account account, Operation operation);
}
