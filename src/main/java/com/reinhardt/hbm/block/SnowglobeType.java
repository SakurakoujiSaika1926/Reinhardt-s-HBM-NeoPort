package com.reinhardt.hbm.block;

public enum SnowglobeType {
    NONE("none", "NONE", null, null),
    RIVET_CITY("rivetcity", "Rivet City", "Welcome to Rivet City. Please wait while the bridge extends.", "RivetCity"),
    TENPENNY_TOWER("tenpennytower", "Tenpenny Tower", "Tenpenny Tower is the brainchild of Allistair Tenpenny, a British refugee who came to the Capital Wasteland seeking his fortune.", "TenpennyTower"),
    LUCKY_38("lucky38", "Lucky 38", "My guess? Leads to a big cashout at some casino - and if the \"38\" on it is any indication... well... Lucky 38 it is.", "Lucky38"),
    SIERRA_MADRE("sierramadre", "Sierra Madre", "It's the moment you've been waiting for, the reason we're all here - the Gala Event, the Grand Opening of the Sierra Madre Casino.", "SierraMadre"),
    PRYDWEN("prydwen", "The Prydwen", "People of the Commonwealth. Do not interfere. Our intentions are peaceful. We are the Brotherhood of Steel.", "Prydwen");

    private final String id;
    private final String label;
    private final String inscription;
    private final String modelGroup;

    SnowglobeType(String id, String label, String inscription, String modelGroup) {
        this.id = id;
        this.label = label;
        this.inscription = inscription;
        this.modelGroup = modelGroup;
    }

    public String id() { return id; }
    public String label() { return label; }
    public String inscription() { return inscription; }
    public String modelGroup() { return modelGroup; }

    public static SnowglobeType byId(String id) {
        for (SnowglobeType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return NONE;
    }

    public static SnowglobeType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
