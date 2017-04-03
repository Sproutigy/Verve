package com.sproutigy.verve.webserver.auth;

import javax.annotation.Nullable;

public interface TokenService {
    String generateToken(String id);
    boolean verifyToken(String token);

    @Nullable
    String getIdFromToken(String token);
}
