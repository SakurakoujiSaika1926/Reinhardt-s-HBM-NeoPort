package com.reinhardt.hbm.door;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public enum HbmDoorDecl {
    FIRE_DOOR("fire_door", 160, dims(2, 0, 0, 0, 2, 1), new int[][]{{-1, 0, 0, 3, 4, 1}}, 5, "block/doors/fire_door", "block/doors/fire_door", Animation.VERTICAL),
    TRANSITION_SEAL("transition_seal", 480, dims(23, 0, 0, 0, 13, 12), new int[][]{{-9, 2, 0, 20, 20, 1}}, 0, "block/doors/transition_seal", "block/doors/transition_seal", Animation.VERTICAL),
    SLIDING_BLAST_DOOR("sliding_blast_door", 24, dims(3, 0, 0, 0, 3, 3), new int[][]{{-2, 0, 0, 4, 5, 1}}, 3, "block/doors/sliding_blast_door", "block/doors/sliding_blast_door", Animation.VERTICAL),
    SLIDING_BLAST_DOOR_2("sliding_blast_door_2", 24, dims(3, 0, 0, 0, 3, 3), new int[][]{{-2, 0, 0, 4, 5, 1}}, 3, "block/doors/sliding_blast_door", "block/doors/sliding_blast_door", Animation.VERTICAL),
    SLIDING_GATE_DOOR("sliding_gate_door", 10, dims(1, 0, 0, 0, 1, 0), new int[][]{{0, 0, 0, 2, 2, 2}}, 0, "block/doors/qe_sliding_door", "block/doors/qe_sliding_door", Animation.VERTICAL),
    QE_SLIDING("qe_sliding", 10, dims(1, 0, 0, 0, 1, 0), new int[][]{{0, 0, 0, 2, 2, 2}}, 0, "block/doors/qe_sliding_door", "block/doors/qe_sliding_door", Animation.VERTICAL),
    QE_SLIDING_DOOR("qe_sliding_door", 10, dims(1, 0, 0, 0, 1, 0), new int[][]{{0, 0, 0, 2, 2, 2}}, 0, "block/doors/qe_sliding_door", "block/doors/qe_sliding_door", Animation.VERTICAL),
    QE_CONTAINMENT("qe_containment", 160, dims(2, 0, 0, 0, 1, 1), new int[][]{{-1, 0, 0, 3, 3, 1}}, 3, "block/doors/qe_containment_door", "block/doors/qe_containment_door", Animation.VERTICAL),
    SLIDING_SEAL_DOOR("sliding_seal_door", 20, dims(1, 0, 0, 0, 0, 0), new int[][]{{0, 0, 0, 1, 2, 2}}, 0, "block/doors/sliding_seal_door", "block/doors/sliding_seal_door", Animation.VERTICAL),
    SECURE_ACCESS_DOOR("secure_access_door", 120, dims(4, 0, 0, 0, 2, 2), new int[][]{{-2, 1, 0, 4, 5, 1}}, 4, "block/doors/secure_access_door", "block/doors/secure_access_door", Animation.VERTICAL),
    ROUND_AIRLOCK_DOOR("round_airlock_door", 60, dims(3, 0, 0, 0, 2, 1), new int[][]{{0, 0, 0, -2, 4, 2}, {0, 0, 0, 3, 4, 2}}, 3, "block/doors/round_airlock_door", "block/doors/round_airlock_door", Animation.VERTICAL),
    LARGE_VEHICLE_DOOR("large_vehicle_door", 60, dims(5, 0, 0, 0, 3, 3), new int[][]{{0, 0, 0, -4, 6, 2}, {0, 0, 0, 4, 6, 2}}, 0, "block/doors/large_vehicle_door", "block/doors/large_vehicle_door", Animation.VERTICAL),
    VAULT_DOOR("vault_door", 120, dims(4, 0, 0, 0, 2, 2), new int[][]{{-1, 1, 0, 3, 3, 2}}, 7, "block/doors/vault_door", "block/doors/vault_door", Animation.VAULT, new int[][]{{0, 0, 1, -1, 2, 2}}),
    WATER_DOOR("water_door", 60, dims(2, 0, 0, 0, 1, 1), new int[][]{{1, 0, 0, -3, 3, 2}}, 2, "block/doors/water_door", "block/doors/water_door", Animation.WATER),
    SILO_HATCH("silo_hatch", 60, dims(0, 0, 2, 2, 2, 2), new int[][]{{1, 0, 1, -3, 3, 0}, {0, 0, 1, -3, 3, 0}, {-1, 0, 1, -3, 3, 0}}, 0, "block/doors/silo_hatch", "block/doors/silo_hatch", Animation.HATCH, null, 2),
    SILO_HATCH_LARGE("silo_hatch_large", 60, dims(0, 0, 3, 3, 3, 3), new int[][]{{2, 0, 1, -3, 3, 0}, {1, 0, 2, -5, 3, 0}, {0, 0, 2, -5, 3, 0}, {-1, 0, 2, -5, 3, 0}, {-2, 0, 1, -3, 3, 0}}, 0, "block/doors/silo_hatch_large", "block/doors/silo_hatch_large", Animation.HATCH, null, 3);

    private static final Map<String, HbmDoorDecl> BY_ID = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(HbmDoorDecl::id, door -> door));
    public static final List<HbmDoorDecl> CREATIVE_ORDER = List.of(
            FIRE_DOOR,
            TRANSITION_SEAL,
            SLIDING_BLAST_DOOR,
            SLIDING_BLAST_DOOR_2,
            SLIDING_GATE_DOOR,
            QE_SLIDING,
            QE_CONTAINMENT,
            SLIDING_SEAL_DOOR,
            SECURE_ACCESS_DOOR,
            ROUND_AIRLOCK_DOOR,
            LARGE_VEHICLE_DOOR,
            VAULT_DOOR,
            WATER_DOOR,
            SILO_HATCH,
            SILO_HATCH_LARGE
    );

    private final String id;
    private final int timeToOpen;
    private final Dimensions dimensions;
    private final int[][] openRanges;
    private final int skinCount;
    private final String worldModel;
    private final String itemModel;
    private final Animation animation;
    private final int[][] extraDimensions;
    private final int blockOffset;

    HbmDoorDecl(String id, int timeToOpen, Dimensions dimensions, int[][] openRanges, int skinCount, String worldModel, String itemModel, Animation animation) {
        this(id, timeToOpen, dimensions, openRanges, skinCount, worldModel, itemModel, animation, null, 0);
    }

    HbmDoorDecl(String id, int timeToOpen, Dimensions dimensions, int[][] openRanges, int skinCount, String worldModel, String itemModel, Animation animation, int[][] extraDimensions) {
        this(id, timeToOpen, dimensions, openRanges, skinCount, worldModel, itemModel, animation, extraDimensions, 0);
    }

    HbmDoorDecl(String id, int timeToOpen, Dimensions dimensions, int[][] openRanges, int skinCount, String worldModel, String itemModel, Animation animation, int[][] extraDimensions, int blockOffset) {
        this.id = id;
        this.timeToOpen = timeToOpen;
        this.dimensions = dimensions;
        this.openRanges = openRanges;
        this.skinCount = skinCount;
        this.worldModel = worldModel;
        this.itemModel = itemModel;
        this.animation = animation;
        this.extraDimensions = extraDimensions;
        this.blockOffset = blockOffset;
    }

    public String id() {
        return this.id;
    }

    public int timeToOpen() {
        return this.timeToOpen;
    }

    public Dimensions dimensions() {
        return this.dimensions;
    }

    public int[][] openRanges() {
        return this.openRanges;
    }

    public int skinCount() {
        return this.skinCount;
    }

    public String worldModel() {
        return this.worldModel;
    }

    public String itemModel() {
        return this.itemModel;
    }

    public Animation animation() {
        return this.animation;
    }

    public int[][] extraDimensions() {
        return this.extraDimensions;
    }

    public int blockOffset() {
        return this.blockOffset;
    }

    public float openProgress(int openTicks) {
        return norm(openTicks, 0.0F, this.timeToOpen);
    }

    public float openProgress(float openTicks) {
        return norm(openTicks, 0.0F, this.timeToOpen);
    }

    public float rangeOpenProgress(int openTicks, int rangeIndex) {
        return rangeOpenProgress((float) openTicks, rangeIndex);
    }

    public float rangeOpenProgress(float openTicks, int rangeIndex) {
        if ((this == WATER_DOOR) || (this == SILO_HATCH) || (this == SILO_HATCH_LARGE)) {
            return norm(openTicks, this == WATER_DOOR ? 35.0F : 20.0F, this == WATER_DOOR ? 40.0F : 20.0F);
        }
        return openProgress(openTicks);
    }

    public VoxelShape localShape(int x, int y, int z, boolean open, boolean collision) {
        if (!open) {
            return closedShape(x, y, z, collision);
        }
        return openShape(x, y, z, collision);
    }

    private VoxelShape closedShape(int x, int y, int z, boolean collision) {
        return switch (this) {
            case SLIDING_SEAL_DOOR -> Shapes.box(0.0D, 0.0D, 0.75D, 1.0D, 1.0D, 1.0D);
            case SECURE_ACCESS_DOOR -> y > 0
                    ? Shapes.box(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D)
                    : Shapes.block();
            case QE_CONTAINMENT -> Shapes.box(0.0D, 0.0D, 0.5D, 1.0D, 1.0D, 1.0D);
            case WATER_DOOR -> Shapes.box(0.0D, 0.0D, 0.75D, 1.0D, 1.0D, 1.0D);
            default -> Shapes.block();
        };
    }

    private VoxelShape openShape(int x, int y, int z, boolean collision) {
        return switch (this) {
            case FIRE_DOOR -> {
                if (z == 1) yield Shapes.box(0.5D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (z == -2) yield Shapes.box(0.0D, 0.0D, 0.0D, 0.5D, 1.0D, 1.0D);
                if (y > 1) yield Shapes.box(0.0D, 0.75D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, collision ? 0.0D : 0.1D, 1.0D);
                yield Shapes.empty();
            }
            case SLIDING_BLAST_DOOR, SLIDING_BLAST_DOOR_2 -> {
                if (y == 3) yield Shapes.box(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, collision ? 0.0D : 0.08D, 1.0D);
                yield Shapes.empty();
            }
            case SLIDING_SEAL_DOOR -> collision ? Shapes.empty() : Shapes.box(0.0D, 0.0D, 0.75D, 1.0D, 1.0D, 1.0D);
            case SECURE_ACCESS_DOOR -> {
                if (y == 1) yield Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, collision ? 0.0D : 0.0625D, 1.0D);
                if (y == 4) yield Shapes.box(0.0D, 0.5D, 0.15D, 1.0D, 1.0D, 0.85D);
                if (y == 0) yield Shapes.block();
                yield Shapes.empty();
            }
            case ROUND_AIRLOCK_DOOR -> {
                if (z == 1) yield Shapes.box(0.4D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (z == -2) yield Shapes.box(0.0D, 0.0D, 0.0D, 0.6D, 1.0D, 1.0D);
                if (y == 3) yield Shapes.box(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, collision ? 0.0D : 0.0625D, 1.0D);
                yield Shapes.empty();
            }
            case SLIDING_GATE_DOOR -> collision
                    ? (z == 0 ? Shapes.box(0.875D, 0.0D, 0.8125D, 1.0D, 1.0D, 1.0D) : Shapes.box(0.0D, 0.0D, 0.8125D, 0.125D, 1.0D, 1.0D))
                    : Shapes.box(0.0D, 0.0D, 0.8125D, 1.0D, 1.0D, 1.0D);
            case QE_CONTAINMENT -> {
                if (y > 1) yield Shapes.box(0.0D, 0.25D, 0.5D, 1.0D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.5D, 1.0D, collision ? 0.0D : 0.125D, 1.0D);
                yield Shapes.empty();
            }
            case WATER_DOOR -> {
                if (y > 1) yield Shapes.box(0.0D, 0.85D, 0.75D, 1.0D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.75D, 1.0D, collision ? 0.0D : 0.15D, 1.0D);
                yield Shapes.empty();
            }
            case LARGE_VEHICLE_DOOR -> {
                if (z == 3) yield Shapes.box(0.4D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                if (z == -3) yield Shapes.box(0.0D, 0.0D, 0.0D, 0.6D, 1.0D, 1.0D);
                if (y == 0) yield Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, collision ? 0.0D : 0.0625D, 1.0D);
                yield Shapes.empty();
            }
            case VAULT_DOOR -> y == 0 ? Shapes.block() : Shapes.empty();
            default -> Shapes.empty();
        };
    }

    private static float norm(float value, float min, float max) {
        if (max <= min) {
            return value >= min ? 1.0F : 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (value - min) / (max - min)));
    }

    private static Dimensions dims(int up, int down, int north, int south, int west, int east) {
        return new Dimensions(up, down, north, south, west, east);
    }

    public static HbmDoorDecl byId(String id) {
        HbmDoorDecl decl = BY_ID.get(id);
        if (decl == null) {
            throw new IllegalArgumentException("Unknown HBM door: " + id);
        }
        return decl;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public enum Animation {
        VERTICAL,
        VAULT,
        WATER,
        HATCH
    }

    public record Dimensions(int up, int down, int north, int south, int west, int east) {
    }
}
