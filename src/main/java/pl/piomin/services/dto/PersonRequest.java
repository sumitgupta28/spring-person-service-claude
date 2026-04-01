package pl.piomin.services.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@Schema(description = "Request payload for creating or updating a person")
public record PersonRequest(
        @Schema(description = "First name", example = "John")
        @NotBlank(message = "First name must not be blank")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        @NotBlank(message = "Last name must not be blank")
        String lastName,

        @Schema(description = "Email address", example = "john.doe@example.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Phone number", example = "+1-555-0100")
        String phone,

        @Schema(description = "Date of birth", example = "1990-06-15")
        LocalDate birthDate,

        @Schema(description = "Home address", example = "123 Main St, Springfield")
        String address
) {
}
