package com.example.springsecurity.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.function.Function;

public interface JwtService {

    String extractUsername(String token);
    String generateAccessToken(Authentication authentication);
    String generateRefreshToken(String username);
    boolean isValidateToken(String token, UserDetails userDetails) ;
    boolean isTokenExpired(String token);
}
