package org.alancora;

import java.io.IOException;
import org.alancora.api.ApiServer;

public final class Main {
    private Main() {}

    public static void main(final String[] args) throws IOException {
        final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("PORT deve estar entre 1 e 65535.");
        }

        final ApiServer server = new ApiServer(port);
        server.start();

        System.out.println("API iniciada na porta " + port + ".");
    }
}
