package org.alancora.api;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.alancora.model.User;
import org.alancora.config.Env;
import org.alancora.service.AuthService;
import org.alancora.service.Tokens;

import com.sun.net.httpserver.HttpExchange;

public final class AuthHandler {
    private static final int MAX_BODY_SIZE = 8192;
    private static final String COOKIE_NAME = "ancora_token";

    private final AuthService auth;
    private final Tokens tokens;
    private final boolean secure;

    public AuthHandler(AuthService auth) {
        this.auth = auth;

        this.tokens = new Tokens(Env.get("JWT_SECRET"));
        this.secure = Boolean.parseBoolean(
                Env.get("COOKIE_SECURE", "false"));
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        String method = switch (path) {
            case "/api/auth/me" -> "GET";
            case "/api/auth/login", "/api/auth/register", "/api/auth/logout" -> "POST";
            default -> null;
        };

        if (method == null) {
            error(exchange, 404, "Rota não encontrada.");
            return;
        }

        if (!exchange.getRequestMethod().equals(method)) {
            exchange.getResponseHeaders().set("Allow", method);
            error(exchange, 405, "Método não permitido.");
            return;
        }

        switch (path) {
            case "/api/auth/me" -> me(exchange);
            case "/api/auth/logout" -> logout(exchange);
            case "/api/auth/login" -> authenticate(exchange, false);
            case "/api/auth/register" -> authenticate(exchange, true);
        }
    }

    private void me(HttpExchange exchange) throws IOException {
        User user = tokens.read(token(exchange));

        if (user == null) {
            error(exchange, 401, "Entre na sua conta para continuar.");
            return;
        }

        String json = "{\"id\":" + user.getId()
                + ",\"name\":" + quote(user.getName())
                + ",\"email\":" + quote(user.getEmail()) + "}";

        ApiServer.respond(exchange, 200, json);
    }

    private void logout(HttpExchange exchange) throws IOException {
        setCookie(exchange, "", Duration.ZERO);
        ApiServer.respond(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void authenticate(HttpExchange exchange, boolean register) throws IOException {
        try {
            Map<String, String> form = readForm(exchange);

            String email = form.getOrDefault("email", "");
            String password = form.getOrDefault("password", "");

            User user = register
                    ? auth.register(
                            form.getOrDefault("name", ""),
                            email,
                            password,
                            form.getOrDefault("confirmation", ""))
                    : auth.login(email, password);

            setCookie(exchange, tokens.create(user), Tokens.DURATION);
            ApiServer.respond(exchange, register ? 201 : 200, "{\"status\":\"ok\"}");
        } catch (RequestException e) {
            error(exchange, e.status, e.getMessage());
        } catch (IllegalArgumentException e) {
            error(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            error(exchange, 401, "E-mail ou senha incorretos.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                error(exchange, 409, "Já existe uma conta com esse e-mail.");
            } else {
                error(exchange, 503, "Não foi possível acessar o banco. Tente novamente.");
            }
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");

        if (type == null) {
            throw new RequestException(415, "Formato de formulário inválido.");
        }

        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);

        if (bytes.length > MAX_BODY_SIZE) {
            throw new RequestException(413, "Formulário muito grande.");
        }

        Map<String, String> form = new HashMap<>();
        String body = new String(bytes, StandardCharsets.UTF_8);

        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);

            if (pair.length != 2) {
                continue;
            }

            try {
                form.put(
                        URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw new RequestException(400, "Formulário inválido.");
            }
        }

        return form;
    }

    private void setCookie(HttpExchange exchange, String token, Duration age) {
        String cookie = COOKIE_NAME + "=" + token
                + "; Path=/; HttpOnly; SameSite=Lax"
                + "; Max-Age=" + age.toSeconds()
                + (secure ? "; Secure" : "");

        exchange.getResponseHeaders().set("Set-Cookie", cookie);
    }

    private static String token(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");

        if (header == null) {
            return "";
        }

        for (String part : header.split(";")) {
            String cookie = part.strip();

            if (cookie.startsWith(COOKIE_NAME + "=")) {
                return cookie.substring(COOKIE_NAME.length() + 1);
            }
        }

        return "";
    }

    private static void error(HttpExchange exchange, int status, String message)
            throws IOException {
        ApiServer.respond(exchange, status, "{\"error\":" + quote(message) + "}");
    }

    private static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");

        for (char c : text.toCharArray()) {
            if (c == '"' || c == '\\') {
                result.append('\\').append(c);
            } else if (c < 32) {
                result.append(String.format("\\u%04x", (int) c));
            } else {
                result.append(c);
            }
        }

        return result.append('"').toString();
    }

    private static final class RequestException extends RuntimeException {
        final int status;

        RequestException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
