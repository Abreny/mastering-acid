package pro.abned.training.acid.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import pro.abned.training.acid.configs.DatabaseConfig;
import pro.abned.training.acid.entities.Operation;
import pro.abned.training.acid.entities.OperationType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DatabaseConfig.class)
class OperationRepositoryTest {
    @Autowired
    OperationRepository operationRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testSave() {
        Operation operation = Operation.builder()
                .accountId(2L)
                .type(OperationType.CREDIT)
                .motif("OPERATION 1")
                .valuedAt(LocalDateTime.now())
                .amount(100.0)
                .build();
        operationRepository.save(operation);
        assertNotNull(operation.getId());
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "OPERATION", "MOTIF = 'OPERATION 1'"));
    }
}