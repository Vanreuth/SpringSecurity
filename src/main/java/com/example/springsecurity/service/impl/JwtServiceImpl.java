package com.example.springsecurity.service.impl;

import com.example.springsecurity.config.JwtProperties;
import com.example.springsecurity.exception.CustomMessageException;
import com.example.springsecurity.modal.RefreshToken;
import com.example.springsecurity.modal.User;
import com.example.springsecurity.repository.RefreshTokenRepository;
import com.example.springsecurity.service.JwtService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    @Override
    public String generateAccessToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // collect role names from authorities
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());


        return Jwts.builder()
                .setSubject(user.getUsername())                 // sub: "johndoe"
                .claim("id", user.getId())                   // custom claim: user id
                .claim("email", user.getEmail())             // custom claim: email
                .claim("roles", roles)                       // custom claim: ["ROLE_USER"]
                .setIssuedAt(new Date())                        // iat: now
                .setExpiration(new Date(
                        System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public boolean isValidateToken(String token , UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())
                && !isTokenExpired(token));
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    @Override
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()
                        + jwtProperties.getRefreshExpiration()))
                .signWith(signingKey())
                .compact();
    }
}
