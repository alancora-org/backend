package org.alancora.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    public static void migrate() throws IOException, SQLException {
        final String sql;

        try (final InputStream file = Database.class.getResourceAsStream("/database/schema.sql")) {
            if (file == null) {
                throw new IOException("Arquivo database/schema.sql não encontrado.");
            }

            sql = new String(file.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (final Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (final Statement statement = connection.createStatement()) {
                statement.execute(sql);
                connection.commit();
            } catch (final SQLException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public static Connection connect() throws SQLException {
        final String url = Env.get("DB_URL");
        if (url == null || url.isBlank()) {
            throw new SQLException("Defina DB_URL para conectar ao banco.");
        }

        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(url);
    }
}
