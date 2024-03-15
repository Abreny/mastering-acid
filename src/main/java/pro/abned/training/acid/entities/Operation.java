package pro.abned.training.acid.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table
public class Operation {
    @Id
    private Long id;
    private String motif;
    private OperationType type;
    private Double amount;
    private LocalDateTime valuedAt;

    /**
     * An operation can have an account source
     */
    private Long accountSourceId;
    private Long accountId;

    private Long relatedOperationId;
}
