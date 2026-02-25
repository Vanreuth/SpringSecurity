package com.example.springsecurity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {

        private Long   id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String status;

        @JsonProperty("profile_picture")
        private String profilePicture;

        @JsonProperty("roles")
        private List<String> roles;

        @JsonProperty("login_attempt")
        private int loginAttempt;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
}
