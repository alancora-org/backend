package org.alancora.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.alancora.dao.UserDao;
import org.alancora.model.HousingType;
import org.alancora.model.Need;
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

    public User findById(final long id) throws SQLException {
        return users.findById(id);
    }

    public boolean delete(final long id) throws SQLException {
        return users.delete(id);
    }

    public User saveDetails(final long id, final String housing, final String location,
            final List<String> needs) throws SQLException {
        return saveDetails(id, housing, location, needs, null);
    }

    public User saveDetails(final long id, final String housing, final String location,
            final List<String> needs, final String housingType) throws SQLException {
        final String cleanHousing = cleanDetail(housing, "Moradia");
        final String cleanLocation = cleanDetail(location, "Localização aproximada");
        final String type = housingType == null ? null : HousingType.parse(housingType).name();
        final List<String> cleanNeeds = cleanNeeds(needs);

        return users.saveDetails(id, cleanHousing, cleanLocation, cleanNeeds, type);
    }

    public List<Need> listNeeds() throws SQLException {
        return users.listNeeds();
    }

    private static String cleanDetail(final String value, final String label) {
        final String text = value.strip();

        if (text.length() > 120 || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " deve ter até 120 caracteres, sem quebras de linha.");
        }

        return text;
    }

    private List<String> cleanNeeds(final List<String> values) throws SQLException {
        if (values == null) {
            return null;
        }

        final List<String> codes = listNeeds().stream().map(Need::getCode).toList();

        if (values.stream().anyMatch(code -> !codes.contains(code))) {
            throw new IllegalArgumentException("Selecione apenas as necessidades de assistência disponíveis.");
        }

        return values.stream().distinct().sorted().toList();
    }

    private static String normalizeEmail(final String email) {
        final String cleanEmail = email.strip().toLowerCase(Locale.ROOT);

        if (cleanEmail.length() > 254 || !cleanEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }

        return cleanEmail;
    }
}
