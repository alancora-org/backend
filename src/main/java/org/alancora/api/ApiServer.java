package org.alancora.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.alancora.dao.UserDao;
import org.alancora.service.AuthService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class ApiServer {
    private final HttpServer server;
    private final AuthHandler auth = new AuthHandler(new AuthService(new UserDao()));

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

            if (path.startsWith("/api/auth/")) {
                auth.handle(exchange);
                return;
            }

            respond(exchange, 404, "{\"error\":\"Rota não encontrada.\"}");
        }
    }

    public static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
