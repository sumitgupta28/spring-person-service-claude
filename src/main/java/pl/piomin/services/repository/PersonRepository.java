package pl.piomin.services.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.piomin.services.model.Person;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByLastNameContainingIgnoreCase(String lastName);

    boolean existsByEmail(String email);

    Optional<Person> findByEmail(String email);
}
