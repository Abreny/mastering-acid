package pro.abned.training.acid.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pro.abned.training.acid.BulkOperation;
import pro.abned.training.acid.configs.TestDatabaseConfig;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDatabaseConfig.class)
class SalaryPaymentTest {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private BulkOperation salaryPayment;

    @Test
    void testExecute() {
        Account aCompany = Account.builder().id(101L).build();

        Map<Account, Double> paymentAttempts = new HashMap<>();
        Account anEmployee = Account.builder().id(103L).build();
        paymentAttempts.put(anEmployee, 500.0);

        anEmployee = Account.builder().id(104L).build();
        paymentAttempts.put(anEmployee, 400.0);

        anEmployee = Account.builder().id(105L).build();
        paymentAttempts.put(anEmployee, 400.0);

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            salaryPayment.execute(aCompany, paymentAttempts);
        });

        assertEquals("salary.payment.balance.insufficient", e.getMessage());

        assertEquals(1000.0, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(10, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 103", Double.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 104", Double.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 105", Double.class));
    }

    @Test
    @DirtiesContext
    void testExecute_consistency() throws InterruptedException {
        Account aCompany = Account.builder().id(101L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        Runnable r = () -> {
            try {
                startSignal.await();
                Map<Account, Double> paymentAttempts = new HashMap<>();
                Account anEmployee = Account.builder().id(103L).build();
                paymentAttempts.put(anEmployee, 100.0);

                anEmployee = Account.builder().id(104L).build();
                paymentAttempts.put(anEmployee, 200.0);

                anEmployee = Account.builder().id(105L).build();
                paymentAttempts.put(anEmployee, 50.0);

                salaryPayment.execute(aCompany, paymentAttempts);
                doneSignal.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        new Thread(r, "T1").start();
        new Thread(r, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(300, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(210, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 103", Double.class));
        assertEquals(400, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 104", Double.class));
        assertEquals(100, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 105", Double.class));
    }
}