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
}
