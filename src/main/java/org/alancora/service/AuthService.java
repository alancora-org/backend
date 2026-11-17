package org.alancora.service;

import java.sql.SQLException;
import java.util.Locale;

import org.alancora.dao.UserDao;
import org.alancora.model.User;

public final class AuthService {
    private final UserDao users;
    private final String dummyHash = Passwords.hash("invalid-account-password");

    public AuthService(final UserDao users) {
        this.users = users;
    }

    public User register(final String name, final String email, final String password, final String confirmation)
            throws SQLException {
        final String cleanName = name.strip();
        final String cleanEmail = normalizeEmail(email);

        if (cleanName.length() < 2 || cleanName.length() > 100 || cleanName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Informe um nome com 2 a 100 caracteres.");
        }

        if (password.length() < 8 || password.length() > 128 || password.isBlank()) {
            throw new IllegalArgumentException("A senha deve ter de 8 a 128 caracteres.");
        }

        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        return users.create(cleanName, cleanEmail, Passwords.hash(password));
    }

    public User login(final String email, final String password) throws SQLException {
        final String cleanEmail = normalizeEmail(email);

        if (password.isEmpty() || password.length() > 128) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }

        final User user = users.findByEmail(cleanEmail);
        final boolean valid = Passwords.matches(password, user == null ? dummyHash : user.getPasswordHash());

        if (user == null || !valid) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }

        return user;
    }

    private static String normalizeEmail(final String email) {
        final String cleanEmail = email.strip().toLowerCase(Locale.ROOT);

        if (cleanEmail.length() > 254 || !cleanEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }

        return cleanEmail;
    }
}
