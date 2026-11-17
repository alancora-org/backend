package org.alancora.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import org.alancora.config.Database;

public final class ApiServer {
    private final HttpServer server;

    public ApiServer(final int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    public void start() {
        server.start();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        try (exchange) {
            final String path = exchange.getRequestURI().getPath();

            if (!path.equals("/health") && !path.equals("/ready")) {
                respond(exchange, 404, "{\"error\":\"Rota não encontrada.\"}");
                return;
            }

            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                respond(exchange, 405, "{\"error\":\"Método não permitido.\"}");
                return;
            }

            if (path.equals("/ready") && !isDatabaseAvailable()) {
                respond(exchange, 503, "{\"error\":\"Banco de dados indisponível.\"}");
                return;
            }

            respond(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    private static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static boolean isDatabaseAvailable() {
        try (final Connection connection = Database.connect()) {
            return connection.isValid(2);
        } catch (final SQLException error) {
            return false;
        }
    }
}
