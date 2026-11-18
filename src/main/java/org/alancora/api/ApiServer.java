package org.alancora.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.alancora.dao.PlanDao;
import org.alancora.dao.UserDao;
import org.alancora.service.AuthService;
import org.alancora.service.PlanService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class ApiServer {
    private final HttpServer server;
    private final AuthHandler auth = new AuthHandler(new AuthService(new UserDao()));
    private final PlanHandler plans = new PlanHandler(new PlanService(new UserDao(), new PlanDao()));

    public ApiServer(final int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    public void start() {
        server.start();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!cors(exchange)) {
                return;
            }

            final String path = exchange.getRequestURI().getPath();

            if (path.startsWith("/api/auth/")) {
                auth.handle(exchange);
                return;
            }

            if (path.equals("/api/plans") || path.startsWith("/api/plans/")) {
                plans.handle(exchange);
                return;
            }

            respond(exchange, 404, "{\"error\":\"Rota não encontrada.\"}");
        }
    }

    private static boolean cors(final HttpExchange exchange) throws IOException {
        final String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Vary", "Origin");

        if (origin != null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        }

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return false;
        }

        return true;
    }

    public static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
