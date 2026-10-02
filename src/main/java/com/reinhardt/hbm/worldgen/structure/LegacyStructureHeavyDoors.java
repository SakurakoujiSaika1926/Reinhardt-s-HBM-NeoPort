package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.door.HbmDoorDecl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reconstructs the saved 1.7.10 {@code BlockDoorGeneric} dummy topology.
 *
 * <p>Legacy structure files store every occupied position with the door's
 * block id. Metadata 0-11 forms a direction chain back to one 12-15 core.
 * Modern heavy doors instead use one typed core plus configured
 * {@code heavy_door_part} block entities, so copying palette states verbatim
 * would create nine independent cargo-door cores in the military base.</p>
 */
final class LegacyStructureHeavyDoors {
    private static final String CARGO_DOOR_ID = "reinhardtshbm:cargo_door";

    private LegacyStructureHeavyDoors() {
    }

    record Source(String blockId, int metadata) {
    }

    record Part(BlockPos corePos, BlockPos localOffset, Direction coreFacing, HbmDoorDecl decl) {
        boolean isCoreAt(BlockPos pos) {
            return this.corePos.equals(pos);
        }
    }

    static Map<BlockPos, Part> index(Map<BlockPos, Source> sources) {
        Map<BlockPos, Part> result = new HashMap<>();
        Map<BlockPos, Direction> validatedCores = new HashMap<>();

        for (Map.Entry<BlockPos, Source> entry : sources.entrySet()) {
            if (!CARGO_DOOR_ID.equals(entry.getValue().blockId())) {
                continue;
            }

            BlockPos corePos = resolveCore(entry.getKey(), sources);
            Direction facing = validatedCores.computeIfAbsent(corePos, pos -> {
                Direction resolved = coreFacing(requiredSource(sources, pos).metadata());
                validateCompleteCargoDoor(pos, resolved, sources);
                return resolved;
            });

            BlockPos localOffset = HbmHeavyDoorBlock.worldOffsets(HbmDoorDecl.CARGO_DOOR, facing, corePos)
                    .get(entry.getKey());
            if (localOffset == null) {
                throw new IllegalStateException("Legacy cargo-door block at " + entry.getKey()
                        + " is outside the exact 1.7.10 footprint around " + corePos + " facing " + facing);
            }
            result.put(entry.getKey(), new Part(corePos, localOffset, facing, HbmDoorDecl.CARGO_DOOR));
        }

        return Map.copyOf(result);
    }

    static Direction coreFacing(int legacyMetadata) {
        if (legacyMetadata < 12 || legacyMetadata > 15) {
            throw new IllegalStateException("Legacy cargo-door core metadata must be 12-15, got " + legacyMetadata);
        }
        Direction facing = Direction.from3DDataValue(legacyMetadata - 10);
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalStateException("Legacy cargo-door core metadata is not horizontal: " + legacyMetadata);
        }
        return facing;
    }

    static CompoundTag migrateCoreNbt(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        result.putByte("State", numericByte(legacy, "state"));
        result.putInt("OpenTicks", numericInt(legacy, "openTicks"));
        result.putByte("Skin", numericByte(legacy, "skin"));
        result.putBoolean("RedstonePowered", numericBoolean(legacy, "redstoned"));
        result.putBoolean("RedstoneReleasePending", false);
        return result;
    }

    private static BlockPos resolveCore(BlockPos start, Map<BlockPos, Source> sources) {
        Set<BlockPos> visited = new HashSet<>();
        BlockPos current = start;
        while (true) {
            if (!visited.add(current)) {
                throw new IllegalStateException("Legacy cargo-door dummy chain loops at " + current);
            }

            Source source = requiredSource(sources, current);
            int effectiveMetadata = source.metadata();
            if (effectiveMetadata >= 6) {
                effectiveMetadata -= 6;
            }
            if (effectiveMetadata >= 6) {
                coreFacing(source.metadata());
                return current;
            }
            if (effectiveMetadata < 0 || effectiveMetadata > 5) {
                throw new IllegalStateException("Invalid legacy cargo-door dummy metadata "
                        + source.metadata() + " at " + current);
            }

            current = current.relative(Direction.from3DDataValue(effectiveMetadata).getOpposite());
        }
    }

    private static void validateCompleteCargoDoor(BlockPos corePos, Direction facing, Map<BlockPos, Source> sources) {
        Map<BlockPos, BlockPos> expected = HbmHeavyDoorBlock.worldOffsets(HbmDoorDecl.CARGO_DOOR, facing, corePos);
        for (BlockPos expectedPos : expected.keySet()) {
            Source source = sources.get(expectedPos);
            if (source == null || !CARGO_DOOR_ID.equals(source.blockId())) {
                throw new IllegalStateException("Legacy cargo door at " + corePos + " facing " + facing
                        + " is missing its saved footprint block at " + expectedPos);
            }
        }
    }

    private static Source requiredSource(Map<BlockPos, Source> sources, BlockPos pos) {
        Source source = sources.get(pos);
        if (source == null || !CARGO_DOOR_ID.equals(source.blockId())) {
            throw new IllegalStateException("Legacy cargo-door dummy at " + pos + " has no matching core chain");
        }
        return source;
    }

    private static int numericInt(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : 0;
    }

    private static byte numericByte(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getByte(key) : 0;
    }

    private static boolean numericBoolean(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) && tag.getBoolean(key);
    }
}
