package com.creaite.wardrobe_api.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.creaite.wardrobe_api.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.refresh.secret:${api.security.token.secret}_refresh}")
    private String refreshSecret;

    public String generateAccessToken(User user) { // cria o token
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("wardrobe-api")
                    .withSubject(user.getEmail())
                    .withClaim("type", "access")
                    .withClaim("userId", user.getId())
                    .withExpiresAt(Date.from(generateAccessTokenExpiration()))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while creating access token");
        }
    }

    public String generateRefreshToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(refreshSecret);

            return JWT.create()
                    .withIssuer("wardrobe-api")
                    .withSubject(user.getEmail())
                    .withClaim("type", "refresh")
                    .withClaim("userId", user.getId())
                    .withExpiresAt(Date.from(generateRefreshTokenExpiration()))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while creating refresh token");
        }
    }

    public String validateAccessToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("wardrobe-api")
                    .withClaim("type", "access")
                    .build()
                    .verify(token)
                    .getSubject(); // o email
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public String validateRefreshToken(String refreshToken) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(refreshSecret);
            return JWT.require(algorithm)
                    .withIssuer("wardrobe-api")
                    .withClaim("type", "refresh")
                    .build()
                    .verify(refreshToken)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    @Deprecated
    public String validateToken(String token) {
        return validateAccessToken(token);
    }

    private Instant generateAccessTokenExpiration() {
        return Instant.now().plusSeconds(2 * 60 * 60); // 2 horas
    }

    private Instant generateRefreshTokenExpiration() {
        return Instant.now().plusSeconds(60 * 24 * 60 * 60); // 60 dias
    }
}