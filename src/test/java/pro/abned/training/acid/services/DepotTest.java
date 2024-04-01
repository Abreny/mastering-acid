package pro.abned.training.acid.services;

import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException;
import lombok.Getter;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.configs.DatabaseConfig;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseConfig.class)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Sql({"classpath:db/drop-database.sql", "classpath:db/schema.sql", "classpath:db/data.sql"})
class DepotTest {
    @Autowired
    private Credit credit;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DirtiesContext
    @Order(1)
    void testCredit() {
        Account account = Account.builder().id(102L).build();
        Operation operation = Operation.builder().motif("TEST CREDIT").amount(1000.0).build();

        credit.credit(account, operation);

        assertEquals(4000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
    }

    @Test
    @DirtiesContext
    @Order(2)
    void testCredit_consistency() throws InterruptedException {
        Account account = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        CreditRunnable request1 = new CreditRunnable(credit, account, startSignal, doneSignal);
        CreditRunnable request2 = new CreditRunnable(credit, account, startSignal, doneSignal);

        new Thread(request1, "T1").start();

        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(4000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
        assertEquals(1, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    @Test
    @DirtiesContext
    @Order(3)
    void testCredit_consistency_with_other_account() throws InterruptedException {
        Account account = Account.builder().id(101L).build();
        Account otherAccount = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        CreditRunnable request1 = new CreditRunnable(credit, account, startSignal, doneSignal);
        CreditRunnable request2 = new CreditRunnable(credit, otherAccount, startSignal, doneSignal);

        new Thread(request1, "T1").start();

        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(2000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(4000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
        assertEquals(0, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    private final static class CreditRunnable implements Runnable {
        private final Credit credit;
        private final Account account;
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        @Getter
        private int lockCount;

        private CreditRunnable(Credit credit, Account account, CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.credit = credit;
            this.account = account;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                try {
                    final var operation = Operation.builder().motif("CREDIT TEST - " + Thread.currentThread().getName())
                                    .amount(1000.0).build();
                    credit.credit(account, operation);
                } catch (DbActionExecutionException el) {
                    lockCount++;
                }
                doneSignal.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}