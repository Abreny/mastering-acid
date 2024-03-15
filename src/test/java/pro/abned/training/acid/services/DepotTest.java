package pro.abned.training.acid.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pro.abned.training.acid.Credit;
import pro.abned.training.acid.configs.TestDatabaseConfig;
import pro.abned.training.acid.entities.Account;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDatabaseConfig.class)
class DepotTest {
    @Autowired
    private Credit credit;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DirtiesContext
    void testCredit() {
        Account account = Account.builder().id(102L).build();

        credit.credit(account, 1000.0);

        assertEquals(4000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
    }

    @Test
    @DirtiesContext
    void testCredit_consistency() throws InterruptedException {
        Account account = Account.builder().id(102L).build();

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(2);

        Runnable request = () -> {
            try {
                startSignal.await();
                credit.credit(account, 1000.0);
                doneSignal.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        new Thread(request, "T1").start();

        new Thread(request, "T2").start();

        startSignal.countDown();
        doneSignal.await();

        assertEquals(5000, jdbcTemplate.queryForObject("SELECT BALANCE FROM ACCOUNT WHERE ID = 102", Double.class));
    }
}