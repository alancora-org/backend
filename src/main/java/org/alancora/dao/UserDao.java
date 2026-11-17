package org.alancora.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.alancora.config.Database;
import org.alancora.model.User;

public class UserDao {
    public User create(final String name, final String email, final String hash) throws SQLException {
        final String sql = "INSERT INTO usuario (nome, email, senha_hash) VALUES (?, ?, ?) RETURNING id";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, hash);

            try (final ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Não foi possível criar a conta.");
                }

                return new User(result.getLong("id"), name, email, hash);
            }
        }
    }

    public User findByEmail(final String email) throws SQLException {
        final String sql = "SELECT id, nome, email, senha_hash FROM usuario WHERE email = ?";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);

            try (final ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new User(result.getLong("id"), result.getString("nome"),
                        result.getString("email"), result.getString("senha_hash"));
            }
        }
    }

    public User findById(final long id) throws SQLException {
        final String sql = "SELECT id, nome, email, senha_hash, moradia, localizacao_aproximada FROM usuario WHERE id = ?";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (final ResultSet result = statement.executeQuery()) {
                return read(result);
            }
        }
    }

    public User saveDetails(final long id, final String housing, final String location) throws SQLException {
        final String sql = "UPDATE usuario SET moradia = ?, localizacao_aproximada = ? WHERE id = ? "
                + "RETURNING id, nome, email, senha_hash, moradia, localizacao_aproximada";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, housing);
            statement.setString(2, location);
            statement.setLong(3, id);

            try (final ResultSet result = statement.executeQuery()) {
                return read(result);
            }
        }
    }

    private static User read(final ResultSet result) throws SQLException {
        if (!result.next()) {
            return null;
        }

        return new User(result.getLong("id"), result.getString("nome"),
                result.getString("email"), result.getString("senha_hash"),
                result.getString("moradia"), result.getString("localizacao_aproximada"));
    }
}
