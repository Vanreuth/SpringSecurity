package com.example.springsecurity.service.impl;

import com.example.springsecurity.config.JwtProperties;
import com.example.springsecurity.exception.CustomMessageException;
import com.example.springsecurity.modal.RefreshToken;
import com.example.springsecurity.modal.User;
import com.example.springsecurity.repository.RefreshTokenRepository;
import com.example.springsecurity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken =  RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(jwtProperties.getRefreshExpiration() / 1000))
                .build();

        log.info("Created refresh token for: {}", user.getUsername());
        return refreshTokenRepository.save(refreshToken);
    }

    // ── Validate — checks exists + not revoked + not expired ──────────────────
    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new CustomMessageException(
                        "Refresh token not found — please login again",
                        String.valueOf(HttpStatus.UNAUTHORIZED.value())));

        if (token.isRevoked())
            throw new CustomMessageException("Refresh token revoked — please login again",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));

        if (token.isExpired())
            throw new CustomMessageException("Refresh token expired — please login again",
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()));

        return token;
    }

    // ── Revoke single token ───────────────────────────────────────────────────
    @Override
    @Transactional
    public void revokeRefreshToken(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Revoked refresh token for: {}", token.getUser().getUsername());
        });
    }

    // ── Revoke all tokens for a user (password change, force logout) ──────────
    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("Revoked all tokens for: {}", user.getUsername());
    }
}
