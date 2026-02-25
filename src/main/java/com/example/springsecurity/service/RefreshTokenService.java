package com.example.springsecurity.service;

import com.example.springsecurity.modal.RefreshToken;
import com.example.springsecurity.modal.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken validateRefreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    void revokeAllUserTokens(User user);


}
