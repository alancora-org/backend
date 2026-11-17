package org.alancora.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// Password hashing and verification using PBKDF2
public final class Passwords {
    private static final int ITERATIONS = 600000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(final String password) {
        final byte[] salt = new byte[16];

        RANDOM.nextBytes(salt);

        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt));
    }

    public static boolean matches(final String password, final String hash) {
        try {
            final String[] parts = hash.split(":");

            if (parts.length != 3 || !parts[0].equals(String.valueOf(ITERATIONS))) {
                return false;
            }

            final byte[] salt = Base64.getDecoder().decode(parts[1]);
            final byte[] expected = Base64.getDecoder().decode(parts[2]);

            return salt.length == 16 && expected.length == 32
                    && MessageDigest.isEqual(expected, derive(password, salt));
        } catch (final IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(final String password, final byte[] salt) {
        final PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);

        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (final GeneralSecurityException error) {
            throw new IllegalStateException("Não foi possível proteger a senha.", error);
        } finally {
            spec.clearPassword();
        }
    }
}
