package com.example.springsecurity.mapper;

import com.example.springsecurity.modal.Role;
import com.example.springsecurity.modal.User;
import com.example.springsecurity.request.UserRequest;
import com.example.springsecurity.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    // ─── Entity → Response (Builder) ─────────────────────────────────────────

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .profilePicture(user.getProfilePicture())
                .roles(rolesToList(user))
                .loginAttempt(user.getLoginAttempt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // ─── Request → Entity ─────────────────────────────────────────────────────

    public User toEntity(UserRequest request) {
        return User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                // password → encoded in service before save
                // roles    → resolved in service
                // status   → set in service
                .build();
    }

    // ─── Apply update to existing entity ─────────────────────────────────────

    public void updateEntity(UserRequest request, User existing) {
        existing.setFirstName(request.firstName());
        existing.setLastName(request.lastName());
        existing.setUsername(request.username());
        existing.setEmail(request.email());
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    private List<String> rolesToList(User user) {
        if (user.getRoles() == null) return List.of();
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }
}