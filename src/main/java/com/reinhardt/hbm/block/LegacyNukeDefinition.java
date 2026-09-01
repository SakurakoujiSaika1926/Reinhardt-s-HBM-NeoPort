package com.reinhardt.hbm.block;

import com.reinhardt.hbm.config.HbmConfig;

import java.util.Arrays;

public enum LegacyNukeDefinition {
    GADGET("nuke_gadget", "gadget", 6, new int[]{26, 8, 44, 8, 44, 98}, new int[]{35, 17, 17, 53, 53, 35},
            new String[]{"gadget_wireing", "early_explosive_lenses", "early_explosive_lenses", "early_explosive_lenses", "early_explosive_lenses", "gadget_core"}, 150, 166, 8, 84, 0),
    MAN("nuke_man", "man", 6, new int[]{26, 8, 44, 8, 44, 98}, new int[]{35, 17, 17, 53, 53, 35},
            new String[]{"man_igniter", "early_explosive_lenses", "early_explosive_lenses", "early_explosive_lenses", "early_explosive_lenses", "man_core"}, 175, 166, 8, 84, 0),
    MIKE("nuke_mike", "mike", 8, new int[]{26, 26, 44, 44, 39, 98, 116, 134}, new int[]{83, 101, 83, 101, 35, 91, 91, 91},
            new String[]{"explosive_lenses", "explosive_lenses", "explosive_lenses", "explosive_lenses", "man_core", "mike_core", "mike_deut", "mike_cooling_unit"}, 250, 217, 8, 135, 0),
    TSAR("nuke_tsar", "tsar", 6, new int[]{48, 66, 84, 102, 55, 138}, new int[]{101, 101, 101, 101, 51, 101},
            new String[]{"explosive_lenses", "explosive_lenses", "explosive_lenses", "explosive_lenses", "man_core", "tsar_core"}, 500, 233, 48, 151, 0),
    FLEIJA("nuke_fleija", "fleija", 11, new int[]{8, 152, 44, 44, 44, 80, 98, 80, 98, 80, 98}, new int[]{36, 36, 18, 36, 54, 18, 18, 36, 36, 54, 54},
            new String[]{"fleija_igniter", "fleija_igniter", "fleija_propellant", "fleija_propellant", "fleija_propellant", "fleija_core", "fleija_core", "fleija_core", "fleija_core", "fleija_core", "fleija_core"}, 50, 222, 8, 140, 0),
    PROTOTYPE("nuke_prototype", "prototype", 14, new int[]{8, 26, 44, 44, 62, 62, 80, 80, 98, 98, 116, 116, 134, 152}, new int[]{35, 35, 26, 44, 26, 44, 26, 44, 26, 44, 26, 44, 35, 35},
            new String[]{"cell_sas3", "cell_sas3", "rod_quad:uranium", "rod_quad:uranium", "rod_quad:lead", "rod_quad:lead", "rod_quad:np237", "rod_quad:np237", "rod_quad:lead", "rod_quad:lead", "rod_quad:uranium", "rod_quad:uranium", "cell_sas3", "cell_sas3"}, 150, 166, 8, 84, 0),
    SOLINIUM("nuke_solinium", "solinium", 9, new int[]{26, 53, 107, 134, 80, 26, 53, 107, 134}, new int[]{18, 18, 18, 18, 36, 54, 54, 54, 54},
            new String[]{"solinium_igniter", "solinium_propellant", "solinium_propellant", "solinium_igniter", "solinium_core", "solinium_igniter", "solinium_propellant", "solinium_propellant", "solinium_igniter"}, 150, 222, 8, 140, 0),
    N2("nuke_n2", "n2", 12, new int[]{98, 116, 134, 98, 116, 134, 98, 116, 134, 98, 116, 134}, new int[]{36, 36, 36, 54, 54, 54, 72, 72, 72, 90, 90, 90},
            new String[]{"n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge", "n2_charge"}, 200, 222, 8, 140, 0),
    CUSTOM("nuke_custom", "custom", 27, customX(), customY(), new String[27], 150, 222, 8, 140, 0),
    BALEFIRE("nuke_fstbmb", "fstbmb", 2, new int[]{17, 53}, new int[]{36, 36}, new String[]{"egg_balefire", "battery_spark|battery_trixite"}, 250, 222, 8, 140, 0);

    private final String blockId;
    private final String modelId;
    private final int slotCount;
    private final int[] slotX;
    private final int[] slotY;
    private final String[] expected;
    private final int radius;
    private final int guiHeight;
    private final int playerLeft;
    private final int playerTop;
    private final int inventoryOffset;

    LegacyNukeDefinition(String blockId, String modelId, int slotCount, int[] slotX, int[] slotY, String[] expected,
                         int radius, int guiHeight, int playerLeft, int playerTop, int inventoryOffset) {
        this.blockId = blockId;
        this.modelId = modelId;
        this.slotCount = slotCount;
        this.slotX = slotX;
        this.slotY = slotY;
        this.expected = expected;
        this.radius = radius;
        this.guiHeight = guiHeight;
        this.playerLeft = playerLeft;
        this.playerTop = playerTop;
        this.inventoryOffset = inventoryOffset;
    }

    public String blockId() { return blockId; }
    public String modelId() { return modelId; }
    public int slotCount() { return slotCount; }
    public int slotX(int slot) { return slotX[slot]; }
    public int slotY(int slot) { return slotY[slot]; }
    public String expected(int slot) { return slot < expected.length ? expected[slot] : ""; }
    public int radius() { return radius; }
    public int guiHeight() { return guiHeight; }
    public int playerLeft() { return playerLeft; }
    public int playerTop() { return playerTop; }
    public int inventoryOffset() { return inventoryOffset; }

    public boolean isCustom() { return this == CUSTOM; }

    public float worldYaw(net.minecraft.core.Direction facing) {
        return switch (this) {
            case GADGET -> switch (facing) { case WEST -> 0F; case SOUTH -> 90F; case EAST -> 180F; default -> 270F; };
            case MAN -> switch (facing) { case EAST -> 90F; case NORTH -> 180F; case WEST -> 270F; default -> 0F; };
            case MIKE -> switch (facing) { case EAST -> 0F; case NORTH -> 90F; case WEST -> 180F; default -> 270F; };
            case TSAR, BALEFIRE -> switch (facing) { case WEST -> 90F; case SOUTH -> 180F; case EAST -> 270F; default -> 0F; };
            case FLEIJA, PROTOTYPE -> switch (facing) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; };
            case SOLINIUM, N2 -> switch (facing) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; };
            case CUSTOM -> switch (facing) { case WEST -> 90F; case SOUTH -> 180F; case EAST -> 270F; default -> 0F; };
        };
    }

    public static LegacyNukeDefinition fromBlockId(String id) {
        return Arrays.stream(values()).filter(definition -> definition.blockId.equals(id)).findFirst().orElse(CUSTOM);
    }

    private static int[] customX() {
        int[] values = new int[27];
        for (int i = 0; i < values.length; i++) values[i] = 8 + (i % 9) * 18;
        return values;
    }

    private static int[] customY() {
        int[] values = new int[27];
        for (int i = 0; i < values.length; i++) values[i] = 18 + (i / 9) * 18;
        return values;
    }
}
