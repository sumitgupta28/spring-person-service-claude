package pl.piomin.services.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.dto.PersonRequest;
import pl.piomin.services.dto.PersonResponse;
import pl.piomin.services.model.Person;
import pl.piomin.services.repository.PersonRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<PersonResponse> findAll() {
        return personRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return personRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));
    }

    public List<PersonResponse> findByLastName(String lastName) {
        return personRepository.findByLastNameContainingIgnoreCase(lastName)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PersonResponse create(PersonRequest request) {
        Person person = toPerson(request);
        Person saved = personRepository.save(person);
        return toResponse(saved);
    }

    @Transactional
    public PersonResponse update(Long id, PersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));

        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setEmail(request.email());
        person.setPhone(request.phone());
        person.setBirthDate(request.birthDate());
        person.setAddress(request.address());

        Person saved = personRepository.save(person);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!personRepository.existsById(id)) {
            throw new EntityNotFoundException("Person not found with id: " + id);
        }
        personRepository.deleteById(id);
    }

    private Person toPerson(PersonRequest request) {
        Person person = new Person();
        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setEmail(request.email());
        person.setPhone(request.phone());
        person.setBirthDate(request.birthDate());
        person.setAddress(request.address());
        return person;
    }

    private PersonResponse toResponse(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getEmail(),
                person.getPhone(),
                person.getBirthDate(),
                person.getAddress(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }
}
