package org.alancora.model;

public final class User {
    private final long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String housing;
    private final String location;

    public User(final long id, final String name, final String email, final String passwordHash) {
        this(id, name, email, passwordHash, "", "");
    }

    public User(final long id, final String name, final String email, final String passwordHash,
            final String housing, final String location) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.housing = housing;
        this.location = location;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getHousing() {
        return housing;
    }

    public String getLocation() {
        return location;
    }
}
