package org.alancora.api;

import static org.alancora.api.Http.array;
import static org.alancora.api.Http.error;
import static org.alancora.api.Http.quote;
import static org.alancora.api.Http.readForm;
import static org.alancora.api.Http.token;
import static org.alancora.api.Http.value;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.alancora.api.Http.RequestException;
import org.alancora.config.Env;
import org.alancora.model.Emergency;
import org.alancora.model.Plan;
import org.alancora.model.User;
import org.alancora.service.PlanService;
import org.alancora.service.Tokens;

import com.sun.net.httpserver.HttpExchange;

public final class PlanHandler {
    private final PlanService plans;
    private final Tokens tokens;

    public PlanHandler(final PlanService plans) {
        this(plans, new Tokens(Env.get("JWT_SECRET")));
    }

    public PlanHandler(final PlanService plans, final Tokens tokens) {
        this.plans = plans;
        this.tokens = tokens;
    }

    public void handle(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();
        final boolean collection = path.equals("/api/plans");

        if (!collection && !path.matches("/api/plans/[1-9][0-9]*")) {
            error(exchange, 404, "Rota não encontrada.");
            return;
        }

        final String method = exchange.getRequestMethod();
        final boolean allowed = collection
                ? method.equals("GET") || method.equals("POST")
                : method.equals("GET") || method.equals("PUT") || method.equals("DELETE");

        if (!allowed) {
            exchange.getResponseHeaders().set("Allow", collection ? "GET, POST" : "GET, PUT, DELETE");
            error(exchange, 405, "Método não permitido.");
            return;
        }

        final User session = tokens.read(token(exchange));
        if (session == null) {
            error(exchange, 401, "Entre na sua conta para continuar.");
            return;
        }

        try {
            if (method.equals("POST")) {
                final Plan plan = plans.create(session.getId(), value(readForm(exchange), "emergency"));

                exchange.getResponseHeaders().set("Location", "/api/plans/" + plan.id());
                ApiServer.respond(exchange, 201, json(plan));
            } else if (collection) {
                final String options = Arrays.stream(Emergency.values())
                        .map(item -> "{\"code\":" + quote(item.name()) + ",\"name\":" + quote(item.getName()) + "}")
                        .collect(Collectors.joining(",", "[", "]"));
                final String items = plans.list(session.getId()).stream().map(PlanHandler::summary)
                        .collect(Collectors.joining(",", "[", "]"));

                ApiServer.respond(exchange, 200, "{\"emergencies\":" + options + ",\"plans\":" + items + "}");
            } else {
                final long id = Long.parseLong(path.substring("/api/plans/".length()));

                if (method.equals("DELETE")) {
                    if (plans.delete(session.getId(), id)) {
                        ApiServer.respond(exchange, 200, "{\"status\":\"ok\"}");
                    } else {
                        error(exchange, 404, "Plano não encontrado.");
                    }

                    return;
                }

                final Plan plan = method.equals("PUT")
                        ? plans.update(session.getId(), id, value(readForm(exchange), "emergency"))
                        : plans.find(session.getId(), id);

                if (plan == null) {
                    error(exchange, 404, "Plano não encontrado.");
                } else {
                    ApiServer.respond(exchange, 200, json(plan));
                }
            }
        } catch (final RequestException error) {
            error(exchange, error.status, error.getMessage());
        } catch (final NumberFormatException error) {
            error(exchange, 404, "Plano não encontrado.");
        } catch (final IllegalArgumentException error) {
            error(exchange, 400, error.getMessage());
        } catch (final SecurityException error) {
            error(exchange, 401, error.getMessage());
        } catch (final SQLException error) {
            error(exchange, 503, "Não foi possível acessar o banco. Tente novamente.");
        }
    }

    private static String summary(final Plan plan) {
        return "{\"id\":" + plan.id() + ",\"emergency\":" + quote(plan.emergency().name())
                + ",\"title\":" + quote(plan.emergency().getName())
                + ",\"createdAt\":" + quote(plan.createdAt().toString()) + "}";
    }

    private static String json(final Plan plan) {
        final String items = plan.orientations().stream().map(item -> "{\"rule\":" + quote(item.rule())
                + ",\"reason\":" + quote(item.reason()) + ",\"text\":" + quote(item.text())
                + ",\"source\":" + quote(item.source()) + "}")
                .collect(Collectors.joining(",", "[", "]"));
        final String header = summary(plan);
        return header.substring(0, header.length() - 1) + ",\"version\":" + plan.version()
                + ",\"profile\":{\"name\":" + quote(plan.name()) + ",\"housing\":" + quote(plan.housing())
                + ",\"housingType\":" + quote(plan.housingType())
                + ",\"location\":" + quote(plan.location()) + ",\"needs\":" + array(plan.needs())
                + "},\"orientations\":" + items + "}";
    }
}
