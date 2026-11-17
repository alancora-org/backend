package org.alancora.config;

import io.github.cdimascio.dotenv.Dotenv;

public final class Env {
    private static final Dotenv VALUES = Dotenv.configure().ignoreIfMissing().load();

    public static String get(final String name) {
        return VALUES.get(name);
    }

    public static String get(final String name, final String fallback) {
        return VALUES.get(name, fallback);
    }
}
