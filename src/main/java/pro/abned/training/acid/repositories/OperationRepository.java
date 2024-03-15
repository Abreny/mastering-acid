package pro.abned.training.acid.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.abned.training.acid.entities.Operation;

@Repository
public interface OperationRepository extends CrudRepository<Operation, Long> {
}
