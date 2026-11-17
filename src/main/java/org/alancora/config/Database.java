package org.alancora.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {
    }

    public static Connection connect() throws SQLException {
        final String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/ancora");
        final String user = System.getenv().getOrDefault("DB_USER", "ancora");
        final String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException("Defina DB_PASSWORD antes de conectar ao banco.");
        }

        DriverManager.setLoginTimeout(5);

        return DriverManager.getConnection(url, user, password);
    }
}
