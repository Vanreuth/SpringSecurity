package com.example.springsecurity.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50) String firstName,

        @Size(max = 50) String lastName,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                message = "Username: letters, numbers, underscores only")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Minimum 8 characters")
//        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
//                message = "Must have uppercase, lowercase, number and special character")
        String password,

        Set<String> roles
) {}
