package com.applidium.qlrequest.Query;

public enum QLType {
    ID("ID"), STRING("String"), INT("Int"), FLOAT("Float"), BOOLEAN("Boolean"), ENUM("Enum");
    // TODO (kelianclerc) 23/5/17 how to add enums

    private final String id;

    private QLType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
