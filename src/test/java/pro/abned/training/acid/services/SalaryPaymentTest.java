package pro.abned.training.acid.services;

import lombok.Getter;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pro.abned.training.acid.BulkOperation;
import pro.abned.training.acid.configs.DatabaseConfig;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.repositories.AccountRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseConfig.class)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Sql({"classpath:db/drop-database.sql", "classpath:db/schema.sql", "classpath:db/data.sql"})
class SalaryPaymentTest {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private BulkOperation salaryPayment;

    @Test
    @Order(1)
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
    @Order(2)
    @DirtiesContext
    void testExecute_consistency() throws InterruptedException {
        Account aCompany = Account.builder().id(101L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        SalaryPaymentRunnable request1 = new SalaryPaymentRunnable(salaryPayment, aCompany, startSignal, doneSignal);
        SalaryPaymentRunnable request2 = new SalaryPaymentRunnable(salaryPayment, aCompany, startSignal, doneSignal);

        new Thread(request1, "T1").start();
        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(650, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(110, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 103", Double.class));
        assertEquals(200, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 104", Double.class));
        assertEquals(50, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 105", Double.class));

        assertEquals(1, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    @Test
    @Order(3)
    @DirtiesContext
    void testExecute_consistency_with_other_account() throws InterruptedException {
        Account aCompany = Account.builder().id(101L).build();
        Account anotherCompany = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        SalaryPaymentRunnable request1 = new SalaryPaymentRunnable(salaryPayment, aCompany, startSignal, doneSignal);
        SalaryPaymentRunnable request2 = new SalaryPaymentRunnable(salaryPayment, anotherCompany, startSignal, doneSignal);
        request2.setDestinations(106L, 107L, 108L);

        new Thread(request1, "T1").start();
        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(650, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(2650, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
        assertEquals(110, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 103", Double.class));
        assertEquals(200, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 104", Double.class));
        assertEquals(50, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 105", Double.class));
        assertEquals(100, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 106", Double.class));
        assertEquals(200, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 107", Double.class));
        assertEquals(50, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 108", Double.class));

        assertEquals(0, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    private static final class SalaryPaymentRunnable implements Runnable {
        private final BulkOperation salaryPayment;
        private final Account source;
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        @Getter
        private int lockCount;

        private Long[] destinations;

        private SalaryPaymentRunnable(BulkOperation salaryPayment, Account source, CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.salaryPayment = salaryPayment;
            this.source = source;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void setDestinations(Long ...destinations) {
            this.destinations = destinations;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                Map<Account, Double> paymentAttempts = new HashMap<>();
                Account anEmployee = Account.builder().id(destinations != null && destinations.length > 0 ? destinations[0] : 103L).build();
                paymentAttempts.put(anEmployee, 100.0);

                anEmployee = Account.builder().id(destinations != null && destinations.length > 1 ? destinations[1] : 104L).build();
                paymentAttempts.put(anEmployee, 200.0);

                anEmployee = Account.builder().id(destinations != null && destinations.length > 2 ? destinations[2] : 105L).build();
                paymentAttempts.put(anEmployee, 50.0);

                try {
                    salaryPayment.execute(source, paymentAttempts);
                } catch (DbActionExecutionException e) {
                    lockCount++;
                }

                doneSignal.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}