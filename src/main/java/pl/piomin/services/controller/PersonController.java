package pl.piomin.services.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.dto.PersonRequest;
import pl.piomin.services.dto.PersonResponse;
import pl.piomin.services.service.PersonService;

import java.util.List;

@Tag(name = "Persons", description = "CRUD operations for person resources")
@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @Operation(summary = "List all persons", description = "Returns all persons, optionally filtered by last name")
    @ApiResponse(responseCode = "200", description = "Persons retrieved successfully")
    @GetMapping
    public List<PersonResponse> findAll(@Parameter(description = "Filter by last name (optional)") @RequestParam(required = false) String lastName) {
        if (lastName != null && !lastName.isBlank()) {
            return personService.findByLastName(lastName);
        }
        return personService.findAll();
    }

    @Operation(summary = "Get a person by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Person found"),
        @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @GetMapping("/{id}")
    public PersonResponse findById(@Parameter(description = "Person ID") @PathVariable Long id) {
        return personService.findById(id);
    }

    @Operation(summary = "Create a new person")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Person created"),
        @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse create(@Valid @RequestBody PersonRequest request) {
        return personService.create(request);
    }

    @Operation(summary = "Update an existing person")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Person updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @PutMapping("/{id}")
    public PersonResponse update(@Parameter(description = "Person ID") @PathVariable Long id, @Valid @RequestBody PersonRequest request) {
        return personService.update(id, request);
    }

    @Operation(summary = "Delete a person by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Person deleted"),
        @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Person ID") @PathVariable Long id) {
        personService.delete(id);
    }
}
