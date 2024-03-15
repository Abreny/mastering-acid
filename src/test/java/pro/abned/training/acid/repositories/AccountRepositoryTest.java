package pro.abned.training.acid.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import pro.abned.training.acid.configs.TestDatabaseConfig;
import pro.abned.training.acid.entities.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDatabaseConfig.class)
class AccountRepositoryTest {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testSave() {
        Account account = Account.builder()
                .name("CLIENT TEST 1")
                .balance(100.0)
                .build();

        accountRepository.save(account);

        assertNotNull(account.getId());
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "ACCOUNT", "name = 'CLIENT TEST 1'"));
    }
}