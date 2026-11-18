package org.alancora.model;

public enum HousingType {
    CASA_TERREA("Casa térrea"),
    SOBRADO("Sobrado"),
    APARTAMENTO("Apartamento"),
    OUTRO("Outro tipo de moradia");

    private final String name;

    private HousingType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static HousingType parse(final String code) {
        for (final HousingType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Selecione e salve seu tipo de moradia para continuar.");
    }
}
