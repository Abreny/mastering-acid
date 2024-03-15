package pro.abned.training.acid.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalaryPaymentUnitTest {
    private SalaryPayment salaryPayment;
    private Credit credit;
    private Debit debit;

    @BeforeEach
    void setUp() {
        AccountRepository accountRepository = mock(AccountRepository.class);

        when(accountRepository.findById(eq(101L))).thenReturn(Optional.of(Account.builder().id(101L).balance(1000.0).build()));

        credit = mock(Credit.class);
        debit = mock(Debit.class);
        salaryPayment = new SalaryPayment(accountRepository, credit, debit);
    }

    @Test
    void execute() {
        Account aCompany = Account.builder().id(101L).balance(1000.0).build();
        Map<Account, Double> attempts = new HashMap<>();
        attempts.put(Account.builder().id(102L).build(), 500.0);
        attempts.put(Account.builder().id(103L).build(), 200.0);

        salaryPayment.execute(aCompany, attempts);

        ArgumentCaptor<Account> destinations = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<Double> amounts = ArgumentCaptor.forClass(Double.class);

        verify(credit, times(2)).credit(destinations.capture(), amounts.capture());

        List<Account> destArgs = destinations.getAllValues();
        List<Double> amountArgs = amounts.getAllValues();

        assertEquals(102L, destArgs.getFirst().getId());
        assertEquals(103L, destArgs.get(1).getId());

        assertEquals(500, amountArgs.getFirst());
        assertEquals(200, amountArgs.get(1));

        verify(debit, times(1)).debit(eq(aCompany), eq(700.0));
    }

    @Test
    void execute_insufficient_amount() {
        Account aCompany = Account.builder().id(101L).balance(1000.0).build();
        Map<Account, Double> attempts = new HashMap<>();
        attempts.put(Account.builder().id(102L).build(), 500.0);
        attempts.put(Account.builder().id(103L).build(), 200.0);
        attempts.put(Account.builder().id(104L).build(), 400.0);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            salaryPayment.execute(aCompany, attempts);
        });
        assertEquals("salary.payment.balance.insufficient", e.getMessage());

        ArgumentCaptor<Account> destinations = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<Double> amounts = ArgumentCaptor.forClass(Double.class);

        verify(credit, times(2)).credit(destinations.capture(), amounts.capture());

        List<Account> destArgs = destinations.getAllValues();
        List<Double> amountArgs = amounts.getAllValues();

        assertEquals(102L, destArgs.getFirst().getId());
        assertEquals(103L, destArgs.get(1).getId());

        assertEquals(500, amountArgs.getFirst());
        assertEquals(200, amountArgs.get(1));

        verify(debit, never()).debit(eq(aCompany), eq(700.0));
    }
}