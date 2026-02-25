package com.example.springsecurity.service.impl;

import com.example.springsecurity.exception.CustomMessageException;
import com.example.springsecurity.mapper.UserMapper;
import com.example.springsecurity.modal.Role;
import com.example.springsecurity.modal.User;
import com.example.springsecurity.request.UserRequest;
import com.example.springsecurity.response.ApiResponse;
import com.example.springsecurity.response.PageResponse;
import com.example.springsecurity.response.UserResponse;
import com.example.springsecurity.repository.RoleRepository;
import com.example.springsecurity.repository.UserRepository;
import com.example.springsecurity.service.R2StorageService;
import com.example.springsecurity.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final R2StorageService r2StorageService;

    @Override
    public ApiResponse<UserResponse> createUser(UserRequest userRequest, MultipartFile profilePicture) {

        //Validation
        this.userRequestValidation(userRequest);

        User user = userMapper.toEntity(userRequest);
        user.setPassword(passwordEncoder.encode(userRequest.password()));
        user.setStatus("ACTIVE");

        user.setRoles(resolveRoles(userRequest.roles()));

        // Upload profile picture if provided
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String url = r2StorageService.uploadFile(profilePicture, "profile");
                user.setProfilePicture(url);
            } catch (IOException e) {
                throw new CustomMessageException("Failed to upload profile picture: " + e.getMessage());
            }
        }

        User savedUser = userRepository.save(user);


        return ApiResponse.success(userMapper.toResponse(savedUser), "User created successfully");
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        Page<UserResponse> userResponsePage = userPage.map(userMapper::toResponse);
        return PageResponse.of(userResponsePage);
    }

    @Override
    public ApiResponse<UserResponse> getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomMessageException("User not found with id: " + id));
        return ApiResponse.success(userMapper.toResponse(user),"User successfully retrieved");
    }

    @Override
    public ApiResponse<UserResponse> findById(String id) {
        return null;
    }

    @Override
    public ApiResponse<UserResponse> updateUser(UserRequest userRequest) {
        return null;
    }



    private Set<Role> resolveRoles(Set<String> requestedRoles) {
        if (ObjectUtils.isEmpty(requestedRoles)) {
            // Default → ROLE_USER
            return Set.of(findOrCreateRole("USER"));
        }
        return requestedRoles.stream()
                .map(this::findOrCreateRole)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Role findOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name(name).build()));
    }

    private void userRequestValidation(UserRequest userRequest) {

        if(ObjectUtils.isEmpty(userRequest.password())) {
            throw new CustomMessageException("Password is empty");
        }

        if(userRepository.existsByEmail(userRequest.email())) {
            throw  new CustomMessageException("Email already exists", String.valueOf(HttpStatus.NOT_FOUND.value()));
        }

    }


}
