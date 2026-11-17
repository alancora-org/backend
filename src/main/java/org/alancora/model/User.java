package org.alancora.model;

public final class User {
    private final long id;
    private final String name;
    private final String email;
    private final String passwordHash;

    public User(final long id, final String name, final String email, final String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
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
}
