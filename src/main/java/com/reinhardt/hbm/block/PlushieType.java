package com.reinhardt.hbm.block;

public enum PlushieType {
    NONE("none", "NONE", null),
    YOMI("yomi", "Yomi", "Hi! Can I be your rabbit friend?"),
    NUMBER_NINE("numbernine", "Number Nine", "None of y'all deserve coal."),
    HUNDUN("hundun", "Hundun", "混沌"),
    DERG("derg", "Dragon", "Squeeze him.");

    private final String id;
    private final String label;
    private final String inscription;

    PlushieType(String id, String label, String inscription) {
        this.id = id;
        this.label = label;
        this.inscription = inscription;
    }

    public String id() { return id; }
    public String label() { return label; }
    public String inscription() { return inscription; }

    public static PlushieType byId(String id) {
        for (PlushieType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return NONE;
    }

    public static PlushieType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
