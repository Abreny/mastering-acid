package pro.abned.training.acid.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RetraitUnitTest {
    private Retrait retrait;

    @BeforeEach
    void setUp() {
        AccountRepository accountRepository = mock(AccountRepository.class);

        when(accountRepository.findById(eq(101L))).thenReturn(Optional.of(Account.builder().id(1L).balance(1000.0).build()));

        retrait = new Retrait(accountRepository);
    }

    @Test
    void testRetrait_negative_value() {
        Account account = Account.builder().id(1L).build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            retrait.debit(account, -100.0);
        });
        assertEquals("retrait.amount.negative", e.getMessage());
    }

    @Test
    void testRetrait_not_defined_account() {
        Account account = Account.builder().id(100L).build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            retrait.debit(account, 100.0);
        });
        assertEquals("retrait.account.not_found", e.getMessage());
    }

    @Test
    void testRetrait_insufficient_balance() {
        Account account = Account.builder().id(101L).build();

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            retrait.debit(account, 10000.0);
        });
        assertEquals("retrait.amount.insufficient", e.getMessage());
    }
}
