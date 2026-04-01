package pl.piomin.services.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.piomin.services.dto.PersonRequest;
import pl.piomin.services.dto.PersonResponse;
import pl.piomin.services.service.PersonService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonService personService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void findAll_authenticated_returns200() throws Exception {
        when(personService.findAll()).thenReturn(List.of(
                new PersonResponse(1L, "John", "Doe", "john@example.com", null, null, null, null, null)
        ));

        mockMvc.perform(get("/api/v1/persons").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));
    }

    @Test
    void findAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/persons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        var request = new PersonRequest("John", "Doe", "john@example.com", null, null, null);
        var response = new PersonResponse(1L, "John", "Doe", "john@example.com", null, null, null, null, null);
        when(personService.create(any(PersonRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/persons")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/persons")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Doe","email":"john@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_authenticated_returns200() throws Exception {
        var response = new PersonResponse(1L, "John", "Doe", "john@example.com", null, null, null, null, null);
        when(personService.findById(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/persons/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
