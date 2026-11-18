package org.alancora.model;

public enum Emergency {
    ENCHENTE("Enchente"),
    INCENDIO("Incêndio"),
    FALTA_ENERGIA("Falta de energia");

    private final String name;

    private Emergency(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Emergency parse(final String code) {
        for (final Emergency emergency : values()) {
            if (emergency.name().equals(code)) {
                return emergency;
            }
        }

        throw new IllegalArgumentException("Selecione um tipo de emergência disponível.");
    }
}
