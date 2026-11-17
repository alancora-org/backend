package org.alancora.model;

public final class Need {
    private final String code;
    private final String name;

    public Need(final String code, final String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
