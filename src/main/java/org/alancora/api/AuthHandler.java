package org.alancora.api;

import static org.alancora.api.Http.COOKIE_NAME;
import static org.alancora.api.Http.array;
import static org.alancora.api.Http.error;
import static org.alancora.api.Http.quote;
import static org.alancora.api.Http.readForm;
import static org.alancora.api.Http.token;
import static org.alancora.api.Http.value;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alancora.api.Http.RequestException;
import org.alancora.config.Env;
import org.alancora.model.HousingType;
import org.alancora.model.Need;
import org.alancora.model.User;
import org.alancora.service.AuthService;
import org.alancora.service.Tokens;

import com.sun.net.httpserver.HttpExchange;

public final class AuthHandler {
    private final AuthService auth;
    private final Tokens tokens;
    private final boolean secure;

    public AuthHandler(final AuthService auth) {
        this(auth, new Tokens(Env.get("JWT_SECRET")), Boolean.parseBoolean(Env.get("COOKIE_SECURE", "false")));
    }

    public AuthHandler(final AuthService auth, final Tokens tokens, final boolean secure) {
        this.auth = auth;
        this.tokens = tokens;
        this.secure = secure;
    }

    public void handle(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();

        final List<String> methods = switch (path) {
            case "/api/auth/me" -> List.of("GET", "DELETE");
            case "/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/details" -> List.of("POST");
            default -> null;
        };

        if (methods == null) {
            error(exchange, 404, "Rota não encontrada.");
            return;
        }

        if (!methods.contains(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", String.join(", ", methods));
            error(exchange, 405, "Método não permitido.");
            return;
        }

        switch (path) {
            case "/api/auth/me" -> {
                if (exchange.getRequestMethod().equals("DELETE")) {
                    delete(exchange);
                } else {
                    profile(exchange, false);
                }
            }
            case "/api/auth/details" -> profile(exchange, true);
            case "/api/auth/logout" -> logout(exchange);
            case "/api/auth/login" -> authenticate(exchange, false);
            case "/api/auth/register" -> authenticate(exchange, true);
        }
    }

    private void profile(final HttpExchange exchange, final boolean save) throws IOException {
        final User session = tokens.read(token(exchange));

        if (session == null) {
            error(exchange, 401, "Entre na sua conta para continuar.");
            return;
        }

        try {
            final User user;

            if (save) {
                final Map<String, List<String>> form = readForm(exchange);
                user = auth.saveDetails(session.getId(), value(form, "housing"), value(form, "location"),
                        choices(form, "needs"), form.containsKey("housingType") ? value(form, "housingType") : null);
            } else {
                user = auth.findById(session.getId());
            }

            if (user == null) {
                error(exchange, 401, "Entre na sua conta para continuar.");
                return;
            }

            final String json = "{\"id\":" + user.getId()
                    + ",\"name\":" + quote(user.getName())
                    + ",\"email\":" + quote(user.getEmail())
                    + ",\"housing\":" + quote(user.getHousing())
                    + ",\"housingType\":" + quote(user.getHousingType())
                    + ",\"location\":" + quote(user.getLocation())
                    + ",\"needs\":" + array(user.getNeeds())
                    + (save ? ""
                            : ",\"options\":" + options(auth.listNeeds())
                                    + ",\"housingTypes\":" + housingTypes())
                    + "}";

            ApiServer.respond(exchange, 200, json);
        } catch (final RequestException error) {
            error(exchange, error.status, error.getMessage());
        } catch (final IllegalArgumentException error) {
            error(exchange, 400, error.getMessage());
        } catch (final SQLException error) {
            error(exchange, 503, "Não foi possível acessar o banco. Tente novamente.");
        }
    }

    private void delete(final HttpExchange exchange) throws IOException {
        final User session = tokens.read(token(exchange));

        if (session == null) {
            error(exchange, 401, "Entre na sua conta para continuar.");
            return;
        }

        try {
            if (!auth.delete(session.getId())) {
                error(exchange, 401, "Entre na sua conta para continuar.");
                return;
            }

            logout(exchange);
        } catch (final SQLException error) {
            error(exchange, 503, "Não foi possível excluir sua conta. Tente novamente.");
        }
    }

    private void logout(HttpExchange exchange) throws IOException {
        setCookie(exchange, "", Duration.ZERO);
        ApiServer.respond(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void authenticate(HttpExchange exchange, boolean register) throws IOException {
        try {
            final Map<String, List<String>> form = readForm(exchange);

            final String email = value(form, "email");
            final String password = value(form, "password");

            User user = register
                    ? auth.register(
                            value(form, "name"),
                            email,
                            password,
                            value(form, "confirmation"))
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

    private static List<String> choices(final Map<String, List<String>> form, final String name) {
        final List<String> values = form.get(name);
        return values == null ? null : values.stream().filter(value -> !value.isEmpty()).toList();
    }

    private static String options(final List<Need> needs) {
        return needs.stream().map(need -> "{\"code\":" + quote(need.getCode())
                + ",\"name\":" + quote(need.getName()) + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String housingTypes() {
        return Arrays.stream(HousingType.values()).map(type -> "{\"code\":" + quote(type.name())
                + ",\"name\":" + quote(type.getName()) + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private void setCookie(HttpExchange exchange, String token, Duration age) {
        String cookie = COOKIE_NAME + "=" + token
                + "; Path=/; HttpOnly; SameSite=Lax"
                + "; Max-Age=" + age.toSeconds()
                + (secure ? "; Secure" : "");

        exchange.getResponseHeaders().set("Set-Cookie", cookie);
    }
}
