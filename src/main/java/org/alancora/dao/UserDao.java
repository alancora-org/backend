package org.alancora.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.alancora.config.Database;
import org.alancora.model.Need;
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
        try (final Connection connection = Database.connect()) {
            return findById(connection, id);
        }
    }

    public List<Need> listNeeds() throws SQLException {
        final String sql = "SELECT codigo, nome FROM necessidade ORDER BY nome, codigo";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql);
                final ResultSet result = statement.executeQuery()) {
            final List<Need> needs = new ArrayList<>();

            while (result.next()) {
                needs.add(new Need(result.getString("codigo"), result.getString("nome")));
            }

            return List.copyOf(needs);
        }
    }

    public User saveDetails(final long id, final String housing, final String location,
            final List<String> needs) throws SQLException {
        final String sql = "UPDATE usuario SET moradia = ?, localizacao_aproximada = ? WHERE id = ?";

        try (final Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, housing);
                statement.setString(2, location);
                statement.setLong(3, id);

                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return null;
                }

                replaceNeeds(connection, id, needs);
                final User user = findById(connection, id);
                connection.commit();
                return user;
            } catch (final SQLException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private static void replaceNeeds(final Connection connection, final long id, final List<String> codes)
            throws SQLException {
        if (codes == null) {
            return;
        }

        final String delete = "DELETE FROM usuario_necessidade WHERE usuario_id = ?";
        final String insert = "INSERT INTO usuario_necessidade (usuario_id, necessidade_codigo) "
                + "VALUES (?, ?)";

        try (final PreparedStatement statement = connection.prepareStatement(delete)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }

        try (final PreparedStatement statement = connection.prepareStatement(insert)) {
            for (final String code : codes) {
                statement.setLong(1, id);
                statement.setString(2, code);
                statement.executeUpdate();
            }
        }
    }

    private static User findById(final Connection connection, final long id) throws SQLException {
        final String sql = "SELECT u.nome, u.email, u.senha_hash, u.moradia, u.localizacao_aproximada, n.necessidade_codigo "
                + "FROM usuario u LEFT JOIN usuario_necessidade n ON n.usuario_id = u.id "
                + "WHERE u.id = ? ORDER BY n.necessidade_codigo";

        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (final ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                final String name = result.getString("nome");
                final String email = result.getString("email");
                final String hash = result.getString("senha_hash");
                final String housing = result.getString("moradia");
                final String location = result.getString("localizacao_aproximada");
                final List<String> needs = new ArrayList<>();

                do {
                    final String code = result.getString("necessidade_codigo");
                    if (code != null) {
                        needs.add(code);
                    }
                } while (result.next());

                return new User(id, name, email, hash, housing, location, needs);
            }
        }
    }
}
