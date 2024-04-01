package pro.abned.training.acid.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;
import pro.abned.training.acid.repositories.AccountRepository;
import pro.abned.training.acid.repositories.OperationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DepotUnitTest {
    private Depot depot;

    @BeforeEach
    void setUp() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        OperationRepository operationRepository = mock(OperationRepository.class);

        when(accountRepository.findById(eq(101L))).thenReturn(Optional.of(Account.builder().id(1L).balance(1000.0).build()));

        depot = new Depot(accountRepository, operationRepository);
    }

    @Test
    void testCredit_negative_value() {
        Account account = new Account();
        account.setId(1L);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            depot.credit(account, Operation.builder().amount(-100.0).build());
        });
        assertEquals("depot.amount.negative", e.getMessage());
    }

    @Test
    void testCredit_not_defined_account() {
        Account account = new Account();
        account.setId(99L);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            depot.credit(account, Operation.builder().amount(100.0).build());
        });
        assertEquals("depot.account.not_found", e.getMessage());
    }
}
