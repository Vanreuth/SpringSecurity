package com.example.springsecurity.service;

import com.example.springsecurity.request.LoginRequest;
import com.example.springsecurity.request.RegisterRequest;
import com.example.springsecurity.response.ApiResponse;
import com.example.springsecurity.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    ApiResponse<Void>         register(RegisterRequest request , MultipartFile profilePicture);
    ApiResponse<AuthResponse> login(LoginRequest request, HttpServletResponse response);
    ApiResponse<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response);
    ApiResponse<Void>         logout(HttpServletRequest request, HttpServletResponse response);
    ApiResponse<AuthResponse> me(HttpServletRequest request);
}
