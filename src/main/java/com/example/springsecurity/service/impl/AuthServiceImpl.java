package com.example.springsecurity.service.impl;

import com.example.springsecurity.exception.CustomMessageException;
import com.example.springsecurity.modal.RefreshToken;
import com.example.springsecurity.modal.Role;
import com.example.springsecurity.modal.User;
import com.example.springsecurity.repository.RoleRepository;
import com.example.springsecurity.repository.UserRepository;
import com.example.springsecurity.request.LoginRequest;
import com.example.springsecurity.request.RegisterRequest;
import com.example.springsecurity.response.ApiResponse;
import com.example.springsecurity.response.AuthResponse;
import com.example.springsecurity.service.AuthService;
import com.example.springsecurity.service.JwtService;
import com.example.springsecurity.service.R2StorageService;
import com.example.springsecurity.service.RefreshTokenService;
import com.example.springsecurity.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final RoleRepository        roleRepository;
    private final R2StorageService      r2StorageService;
    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final RefreshTokenService   refreshTokenService;

    // ─── REGISTER ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> register(RegisterRequest request, MultipartFile profilePicture) {
        if (userRepository.existsByEmail(request.email()))
            throw new CustomMessageException("Email already exists",
                    String.valueOf(HttpStatus.CONFLICT.value()));

        if (userRepository.existsByUsername(request.username()))
            throw new CustomMessageException("Username already exists",
                    String.valueOf(HttpStatus.CONFLICT.value()));

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .status("ACTIVE")
                .roles(resolveRoles(request.roles()))
                .build();

        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                user.setProfilePicture(r2StorageService.uploadFile(profilePicture, "profile"));
            } catch (IOException e) {
                throw new CustomMessageException("Failed to upload profile picture: " + e.getMessage());
            }
        }

        userRepository.save(user);
        log.info("Registered: {}", user.getUsername());
        return ApiResponse.success("Registration successful — you can now login");
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        userRepository.resetLoginAttempt(user.getUsername());

        String accessToken = jwtService.generateAccessToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN,
                accessToken, 15 * 60 * 1000L);                  // 15 min

        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN,
                refreshToken.getToken(), 24 * 60 * 60 * 1000L); // 24 hours

        log.info("Logged in: {}", user.getUsername());
        return ApiResponse.success(buildAuthResponse(user), "Login successful");
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> refresh(HttpServletRequest request,
                                             HttpServletResponse response) {
        // 1. Read refresh token from cookie via CookieUtil ←────────────────────
        String refreshTokenValue = CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN);

        if (refreshTokenValue == null)
            throw new CustomMessageException(
                    "Refresh token cookie missing — please login again",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));

        // 2. Validate refresh token against DB
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        // 3. Generate new access token
        String newAccessToken = jwtService.generateRefreshToken(user.getUsername());

        // 4. Set new access token cookie via CookieUtil ←───────────────────────
        CookieUtil.addCookie(response,
                CookieUtil.ACCESS_TOKEN, newAccessToken,
                15 * 60 * 1000L);                               // 15 min

        log.info("Refreshed token for: {}", user.getUsername());
        return ApiResponse.success(buildAuthResponse(user), "Token refreshed successfully");
    }

    // ─── LOGOUT — revoke DB token + clear cookies ─────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        // 1. Read and revoke refresh token from DB
        String refreshTokenValue = CookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN);
        if (refreshTokenValue != null) {
            try {
                refreshTokenService.revokeRefreshToken(refreshTokenValue);
            } catch (Exception e) {
                log.warn("Refresh token not found on logout: {}", e.getMessage());
            }
        }

        // 2. Clear both cookies (Max-Age=0 → browser deletes immediately)
        CookieUtil.clearCookie(response, CookieUtil.ACCESS_TOKEN);
        CookieUtil.clearCookie(response, CookieUtil.REFRESH_TOKEN);

        // 3. Clear Spring Security context
        SecurityContextHolder.clearContext();

        log.info("User logged out");
        return ApiResponse.success("Logged out successfully");
    }

    // ─── ME ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<AuthResponse> me(HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomMessageException("User not found",
                        String.valueOf(HttpStatus.NOT_FOUND.value())));
        return ApiResponse.success(buildAuthResponse(user), "Profile fetched successfully");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))   // ← strip prefix for display only
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)   // ← ["USER"] instead of ["ROLE_USER"]
                .build();
    }


    private Set<Role> resolveRoles(Set<String> requested) {
        if (ObjectUtils.isEmpty(requested))
            return Set.of(findOrCreateRole("USER"));  // ← "USER" not "ROLE_USER"
        return requested.stream()
                .map(this::findOrCreateRole)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Role findOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name(name).build()));
    }
}