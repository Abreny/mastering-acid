package pro.abned.training.acid.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import pro.abned.training.acid.entities.Account;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {
}
