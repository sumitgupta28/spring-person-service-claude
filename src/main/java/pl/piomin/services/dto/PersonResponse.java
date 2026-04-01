package pl.piomin.services.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Person resource representation")
public record PersonResponse(
        @Schema(description = "Unique identifier") Long id,
        @Schema(description = "First name") String firstName,
        @Schema(description = "Last name") String lastName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Phone number") String phone,
        @Schema(description = "Date of birth") LocalDate birthDate,
        @Schema(description = "Home address") String address,
        @Schema(description = "Record creation timestamp") LocalDateTime createdAt,
        @Schema(description = "Record last update timestamp") LocalDateTime updatedAt
) {
}
