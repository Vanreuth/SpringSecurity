package com.example.springsecurity.controller;

import com.example.springsecurity.request.LoginRequest;
import com.example.springsecurity.request.RegisterRequest;
import com.example.springsecurity.request.UserRequest;
import com.example.springsecurity.response.ApiResponse;
import com.example.springsecurity.response.AuthResponse;
import com.example.springsecurity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid  RegisterRequest request,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request, profilePicture));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    /**
     * POST /api/v1/auth/refresh
     * Reads refresh_token cookie automatically (browser sends it).
     * Validates against DB → sets a new access_token cookie.
     * No request body needed.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    /**
     * POST /api/v1/auth/logout
     * Reads refresh_token cookie → revokes it in DB.
     * Clears both cookies (Max-Age=0) so browser deletes them.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.logout(request, response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me(HttpServletRequest request) {
        return ResponseEntity.ok(authService.me(request));
    }

}
