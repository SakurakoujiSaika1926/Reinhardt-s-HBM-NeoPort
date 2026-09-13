package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exact legacy structure support for the only 1.7.10 BlockDummyable families
 * that are actually present in shipped HBM structure NBT files.
 *
 * <p>The old template format stores every occupied position as the original
 * block, with metadata 0-11 forming a chain back to a 12-15 core.  It does
 * not call {@code onBlockPlacedBy}; therefore a modern structure loader must
 * reconstruct that saved topology itself rather than place every entry as a
 * new modern core.</p>
 */
final class LegacyStructureMultiblocks {
    private LegacyStructureMultiblocks() {
    }

    enum Kind {
        NONE(""),
        FLUID_TANK("reinhardtshbm:machine_fluidtank"),
        ROTARY_FURNACE("reinhardtshbm:machine_rotary_furnace"),
        HOWARD_DAMAGED("reinhardtshbm:turret_howard_damaged");

        private final String blockId;

        Kind(String blockId) {
            this.blockId = blockId;
        }

        static Kind fromBlockId(String blockId) {
            for (Kind kind : values()) {
                if (kind.blockId.equals(blockId)) {
                    return kind;
                }
            }
            return NONE;
        }
    }

    record Source(String blockId, int metadata) {
    }

    record Part(Kind kind, BlockPos corePos) {
        boolean isCoreAt(BlockPos pos) {
            return this.corePos.equals(pos);
        }
    }

    static Map<BlockPos, Part> index(Map<BlockPos, Source> sources) {
        Map<BlockPos, Part> result = new HashMap<>();
        for (Map.Entry<BlockPos, Source> entry : sources.entrySet()) {
            Kind kind = Kind.fromBlockId(entry.getValue().blockId());
            if (kind == Kind.NONE) {
                continue;
            }
            result.put(entry.getKey(), new Part(kind, resolveCore(entry.getKey(), kind, sources)));
        }
        return Map.copyOf(result);
    }

    static Direction coreFacing(Kind kind, int legacyMetadata) {
        if (legacyMetadata < 12 || legacyMetadata > 15) {
            throw new IllegalStateException("Legacy " + kind + " core metadata must be 12-15, got " + legacyMetadata);
        }
        Direction facing = Direction.from3DDataValue(legacyMetadata - 10);
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalStateException("Legacy " + kind + " core metadata is not horizontal: " + legacyMetadata);
        }
        return facing;
    }

    static CompoundTag migrateCoreNbt(Kind kind, CompoundTag legacy) {
        return switch (kind) {
            case FLUID_TANK -> migrateFluidTank(legacy);
            case ROTARY_FURNACE -> migrateRotaryFurnace(legacy);
            case HOWARD_DAMAGED -> migrateHowardDamaged(legacy);
            case NONE -> throw new IllegalArgumentException("Cannot migrate a non-multiblock structure entry");
        };
    }

    private static BlockPos resolveCore(BlockPos start, Kind kind, Map<BlockPos, Source> sources) {
        Set<BlockPos> visited = new HashSet<>();
        BlockPos current = start;
        while (true) {
            if (!visited.add(current)) {
                throw new IllegalStateException("Legacy " + kind + " structure dummy chain loops at " + current);
            }

            Source source = sources.get(current);
            if (source == null || Kind.fromBlockId(source.blockId()) != kind) {
                throw new IllegalStateException("Legacy " + kind + " structure dummy at " + current + " has no matching core chain");
            }

            int effectiveMetadata = source.metadata();
            if (effectiveMetadata >= 6) {
                effectiveMetadata -= 6;
            }
            if (effectiveMetadata >= 6) {
                coreFacing(kind, source.metadata());
                return current;
            }
            if (effectiveMetadata < 0 || effectiveMetadata > 5) {
                throw new IllegalStateException("Invalid legacy " + kind + " dummy metadata " + source.metadata() + " at " + current);
            }

            // BlockDummyable#findCoreRec follows the opposite of the stored
            // dummy direction.  This is the saved 1.7.10 topology, not a
            // synthesized modern footprint.
            current = current.relative(Direction.from3DDataValue(effectiveMetadata).getOpposite());
        }
    }

    private static CompoundTag migrateFluidTank(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        copyLegacyItems(legacy, result, 6);
        result.put("Tank", migrateTank(legacy, "tank"));
        result.putByte("Mode", (byte) requiredInt(legacy, "mode"));
        result.putBoolean("Exploded", requiredBoolean(legacy, "exploded"));
        result.putBoolean("OnFire", requiredBoolean(legacy, "onFire"));
        return result;
    }

    private static CompoundTag migrateRotaryFurnace(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        copyLegacyItems(legacy, result, 5);
        result.put("AdditiveTank", migrateTank(legacy, "t0"));
        result.put("SteamTank", migrateTank(legacy, "t1"));
        result.put("SpentSteamTank", migrateTank(legacy, "t2"));
        result.put("Smoke", migrateTank(legacy, "smoke0"));
        result.put("SmokeLeaded", migrateTank(legacy, "smoke1"));
        result.put("SmokePoison", migrateTank(legacy, "smoke2"));
        result.putFloat("Progress", requiredFloat(legacy, "prog"));
        result.putInt("BurnTime", requiredInt(legacy, "burn"));
        // TileEntityMachineRotaryFurnace#getDouble("heat") is intentionally
        // zero when an older structure tag did not persist this field.
        result.putDouble("BurnHeat", legacy.getDouble("heat"));
        result.putInt("MaxBurnTime", requiredInt(legacy, "maxBurn"));
        if (legacy.contains("outType", Tag.TAG_ANY_NUMERIC)) {
            int materialId = legacy.getInt("outType");
            FoundryMaterial material = FoundryMaterial.byId(materialId)
                    .orElseThrow(() -> new IllegalStateException("Unknown legacy rotary-furnace material " + materialId));
            CompoundTag output = new CompoundTag();
            output.putInt("material_id", material.id());
            output.putString("material", material.name());
            output.putInt("amount", requiredInt(legacy, "outAmount"));
            result.put("Output", output);
        }
        return result;
    }

    private static CompoundTag migrateHowardDamaged(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        copyLegacyItems(legacy, result, Integer.MAX_VALUE);
        result.putLong("power", requiredLong(legacy, "power"));
        result.putBoolean("on", requiredBoolean(legacy, "isOn"));
        result.putBoolean("target_players", requiredBoolean(legacy, "targetPlayers"));
        result.putBoolean("target_animals", requiredBoolean(legacy, "targetAnimals"));
        result.putBoolean("target_mobs", requiredBoolean(legacy, "targetMobs"));
        result.putBoolean("target_machines", requiredBoolean(legacy, "targetMachines"));
        result.putInt("loaded", requiredInt(legacy, "loaded"));
        result.putInt("stattrak", requiredInt(legacy, "stattrak"));
        return result;
    }

    private static CompoundTag migrateTank(CompoundTag legacy, String prefix) {
        int typeId = requiredInt(legacy, prefix + "_type");
        HbmFluidDefinition type = HbmFluids.byOldId(typeId)
                .orElseThrow(() -> new IllegalStateException("Unknown legacy HBM fluid id " + typeId + " in " + prefix));
        CompoundTag result = new CompoundTag();
        result.putString("type", type.name());
        result.putInt("amount", requiredInt(legacy, prefix));
        result.putInt("capacity", requiredInt(legacy, prefix + "_max"));
        result.putInt("pressure", requiredInt(legacy, prefix + "_p"));
        return result;
    }

    private static void copyLegacyItems(CompoundTag legacy, CompoundTag result, int slotCount) {
        if (!legacy.contains("items", Tag.TAG_LIST)) {
            throw new IllegalStateException("Legacy structure multiblock NBT has no items list");
        }
        ListTag items = legacy.getList("items", Tag.TAG_COMPOUND);
        ListTag migrated = new ListTag();
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index).copy();
            if (item.isEmpty() || item.getString("id").equals("minecraft:air")) {
                continue;
            }
            int slot = requiredInt(item, "slot");
            if (slot < 0 || slot >= slotCount) {
                throw new IllegalStateException("Legacy structure item slot " + slot + " is outside its machine inventory");
            }
            if (!item.contains("id", Tag.TAG_STRING) || !item.contains("count", Tag.TAG_ANY_NUMERIC)) {
                throw new IllegalStateException("Legacy structure item at slot " + slot + " was not resolved through itemPalette");
            }
            item.remove("slot");
            item.putByte("Slot", (byte) slot);
            migrated.add(item);
        }
        result.put("Items", migrated);
    }

    private static int requiredInt(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalStateException("Missing legacy structure NBT numeric field " + key);
        }
        return tag.getInt(key);
    }

    private static long requiredLong(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalStateException("Missing legacy structure NBT numeric field " + key);
        }
        return tag.getLong(key);
    }

    private static float requiredFloat(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalStateException("Missing legacy structure NBT numeric field " + key);
        }
        return tag.getFloat(key);
    }

    private static boolean requiredBoolean(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_BYTE)) {
            throw new IllegalStateException("Missing legacy structure NBT boolean field " + key);
        }
        return tag.getBoolean(key);
    }
}
