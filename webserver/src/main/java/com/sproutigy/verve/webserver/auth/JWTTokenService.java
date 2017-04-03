package com.sproutigy.verve.webserver.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Date;

public class JWTTokenService implements TokenService {

    @Getter
    protected String issuerName;

    private JWTVerifier verifier;
    private Algorithm algorithm;

    public JWTTokenService(String issuerName, Algorithm algorithm) {
        this.issuerName = issuerName;
        this.algorithm = algorithm;
        this.verifier = JWT.require(algorithm).withIssuer(issuerName).build();
    }

    public JWTTokenService(String issuerName, byte[] secret) {
        this(issuerName, Algorithm.HMAC256(secret));
    }

    @Override
    public String generateToken(String id) {
        if (id == null) {
            throw new NullPointerException("id == null");
        }

        return JWT.create()
                .withIssuer(getIssuerName())
                .withIssuedAt(new Date())
                .withSubject(id)
                .sign(algorithm);
    }

    @Override
    public boolean verifyToken(String token) {
        if (token == null) {
            return false;
        }

        try {
            verifier.verify(token);
            return true;
        } catch(JWTVerificationException e) {
            return false;
        }
    }

    @Nullable
    public JWT parseToken(String token) {
        JWT jwt;
        try {
            jwt = JWT.decode(token);
        } catch(JWTDecodeException e) {
            return null;
        }
        return jwt;
    }

    @Nullable
    @Override
    public String getIdFromToken(String token) {
        JWT jwt = parseToken(token);
        if (jwt != null) {
            return jwt.getSubject();
        }
        return null;
    }

}
