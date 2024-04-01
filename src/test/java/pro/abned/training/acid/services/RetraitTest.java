package pro.abned.training.acid.services;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pro.abned.training.acid.Debit;
import pro.abned.training.acid.configs.DatabaseConfig;
import pro.abned.training.acid.entities.Account;
import pro.abned.training.acid.entities.Operation;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseConfig.class)
@Sql({"classpath:db/drop-database.sql", "classpath:db/schema.sql", "classpath:db/data.sql"})
class RetraitTest {

    @Autowired
    private Debit retrait;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DirtiesContext
    void testDebit() {
        Account account = Account.builder().id(102L).build();

        retrait.debit(account, Operation.builder().amount(1000.0).build());

        assertEquals(2000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
    }

    @Test
    @DirtiesContext
    void testRetrait_consistency() throws InterruptedException {
        Account account = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        DebitRunnable request1 = new DebitRunnable(retrait, account, startSignal, doneSignal);
        DebitRunnable request2 = new DebitRunnable(retrait, account, startSignal, doneSignal);

        new Thread(request1, "T1").start();

        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(2000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
        assertEquals(1, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    @Test
    @DirtiesContext
    void testRetrait_consistency_with_other_account() throws InterruptedException {
        Account account = Account.builder().id(101L).build();
        Account otherAccount = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        DebitRunnable request1 = new DebitRunnable(retrait, account, startSignal, doneSignal);
        DebitRunnable request2 = new DebitRunnable(retrait, otherAccount, startSignal, doneSignal);

        new Thread(request1, "T1").start();

        new Thread(request2, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(0, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 101", Double.class));
        assertEquals(2000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
        assertEquals(0, Math.max(request1.getLockCount(), request2.getLockCount()));
        assertEquals(0, Math.min(request1.getLockCount(), request2.getLockCount()));
    }

    private static final class DebitRunnable implements Runnable {
        private final Debit debit;
        private final Account account;
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        @Getter
        private int lockCount;

        private DebitRunnable(Debit debit, Account account, CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.debit = debit;
            this.account = account;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                try {
                    final var motif = "TEST RETRAIT " + Thread.currentThread().getName();
                    debit.debit(account, Operation.builder().amount(1000.0).motif(motif).build());
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