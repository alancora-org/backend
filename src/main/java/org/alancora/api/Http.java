package org.alancora.api;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

public final class Http {
    public static final String COOKIE_NAME = "ancora_token";
    private static final int MAX_BODY_SIZE = 8192;

    public static Map<String, List<String>> readForm(final HttpExchange exchange) throws IOException {
        final String type = exchange.getRequestHeaders().getFirst("Content-Type");

        if (type == null || !type.split(";", 2)[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded")) {
            throw new RequestException(415, "Formato de formulário inválido.");
        }

        final byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_SIZE + 1);

        if (bytes.length > MAX_BODY_SIZE) {
            throw new RequestException(413, "Formulário muito grande.");
        }

        final Map<String, List<String>> form = new HashMap<>();
        final String body = new String(bytes, StandardCharsets.UTF_8);

        for (final String part : body.split("&")) {
            final String[] pair = part.split("=", 2);

            if (pair.length != 2) {
                continue;
            }

            try {
                final String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                final String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                form.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            } catch (final IllegalArgumentException e) {
                throw new RequestException(400, "Formulário inválido.");
            }
        }

        return form;
    }

    public static String value(final Map<String, List<String>> form, final String name) {
        return form.getOrDefault(name, List.of("")).getLast();
    }

    public static String array(final List<String> values) {
        return values.stream().map(Http::quote).collect(Collectors.joining(",", "[", "]"));
    }

    public static String token(final HttpExchange exchange) {
        final String header = exchange.getRequestHeaders().getFirst("Cookie");

        if (header == null) {
            return "";
        }

        for (final String part : header.split(";")) {
            final String cookie = part.strip();

            if (cookie.startsWith(COOKIE_NAME + "=")) {
                return cookie.substring(COOKIE_NAME.length() + 1);
            }
        }

        return "";
    }

    public static void error(final HttpExchange exchange, final int status, final String message)
            throws IOException {
        ApiServer.respond(exchange, status, "{\"error\":" + quote(message) + "}");
    }

    public static String quote(final String text) {
        final StringBuilder result = new StringBuilder("\"");

        for (final char c : text.toCharArray()) {
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

    public static final class RequestException extends RuntimeException {
        public final int status;

        public RequestException(final int status, final String message) {
            super(message);
            this.status = status;
        }
    }
}
