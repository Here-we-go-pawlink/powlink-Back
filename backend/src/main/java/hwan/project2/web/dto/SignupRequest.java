package hwan.project2.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 20) String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 64) String password
) {}
