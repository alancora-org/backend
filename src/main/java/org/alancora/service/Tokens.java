package org.alancora.service;

import java.time.Duration;
import java.time.Instant;

import org.alancora.model.User;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public final class Tokens {
    public static final Duration DURATION = Duration.ofDays(7);

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public Tokens(final String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Defina JWT_SECRET com pelo menos 32 caracteres.");
        }

        algorithm = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithm).withIssuer("ancora")
                .withClaimPresence("sub").withClaimPresence("exp")
                .withClaimPresence("name").withClaimPresence("email").build();
    }

    public String create(final User user) {
        return JWT.create().withIssuer("ancora")
                .withSubject(String.valueOf(user.getId()))
                .withClaim("name", user.getName()).withClaim("email", user.getEmail())
                .withExpiresAt(Instant.now().plus(DURATION)).sign(algorithm);
    }

    public User read(final String token) {
        try {
            final DecodedJWT jwt = verifier.verify(token);

            return new User(Long.parseLong(jwt.getSubject()), jwt.getClaim("name").asString(),
                    jwt.getClaim("email").asString(), "");
        } catch (final JWTVerificationException | IllegalArgumentException error) {
            return null;
        }
    }
}
