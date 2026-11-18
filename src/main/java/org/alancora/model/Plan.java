package org.alancora.model;

import java.time.Instant;
import java.util.List;

public record Plan(long id, long userId, Instant createdAt, int version, Emergency emergency,
        String name, String housing, String location, String housingType, List<String> needs,
        List<Orientation> orientations) {
    public Plan {
        needs = List.copyOf(needs);
        orientations = List.copyOf(orientations);
    }
}
