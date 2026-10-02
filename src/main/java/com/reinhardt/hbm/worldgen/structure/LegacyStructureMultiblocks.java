package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.block.VendingMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

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
        HOWARD_DAMAGED("reinhardtshbm:turret_howard_damaged"),
        RADIO_TELEX("reinhardtshbm:radio_telex"),
        VENDING_MACHINE("reinhardtshbm:vending_machine");

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

    record Part(Kind kind, BlockPos corePos, int variant) {
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
            BlockPos corePos = resolveCore(entry.getKey(), kind, sources);
            int variant = kind == Kind.VENDING_MACHINE ? vendingMachineVariant(corePos, sources) : 0;
            result.put(entry.getKey(), new Part(kind, corePos, variant));
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
            case RADIO_TELEX -> migrateRadioTelex(legacy);
            case VENDING_MACHINE -> new CompoundTag();
            case NONE -> throw new IllegalArgumentException("Cannot migrate a non-multiblock structure entry");
        };
    }

    static BlockState migrateCoreState(Part part, BlockState state) {
        if (part.kind() != Kind.VENDING_MACHINE) {
            return state;
        }
        if (!(state.getBlock() instanceof VendingMachineBlock)
                || !state.hasProperty(VendingMachineBlock.VARIANT)) {
            throw new IllegalStateException("Legacy vending-machine core did not resolve to a vending-machine blockstate");
        }
        return state.setValue(VendingMachineBlock.VARIANT, part.variant());
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
        copyLegacyItemsToSlots(legacy, result, 6);
        result.put("Tank", migrateTank(legacy, "tank"));
        result.putByte("Mode", (byte) requiredInt(legacy, "mode"));
        result.putBoolean("Exploded", requiredBoolean(legacy, "exploded"));
        result.putBoolean("OnFire", requiredBoolean(legacy, "onFire"));
        return result;
    }

    private static CompoundTag migrateRotaryFurnace(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        copyLegacyItemsToSlots(legacy, result, 5);
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
        copyLegacyItemsToVanillaList(legacy, result, Integer.MAX_VALUE);
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

    private static CompoundTag migrateRadioTelex(CompoundTag legacy) {
        CompoundTag result = new CompoundTag();
        for (int index = 0; index < 5; index++) {
            result.putString("tx" + index, legacy.getString("tx" + index));
            result.putString("rx" + index, legacy.getString("rx" + index));
        }
        result.putString("txChan", legacy.getString("txChan"));
        result.putString("rxChan", legacy.getString("rxChan"));
        return result;
    }

    private static int vendingMachineVariant(BlockPos corePos, Map<BlockPos, Source> sources) {
        Source upper = sources.get(corePos.above());
        if (upper == null || Kind.fromBlockId(upper.blockId()) != Kind.VENDING_MACHINE) {
            throw new IllegalStateException("Legacy vending-machine core at " + corePos
                    + " is missing its upper saved block");
        }

        int metadata = upper.metadata();
        int directionMetadata = Math.floorMod(metadata, 6);
        if (directionMetadata != Direction.UP.get3DDataValue() || metadata < 0 || metadata > 11) {
            throw new IllegalStateException("Legacy vending-machine upper block at " + corePos.above()
                    + " has invalid metadata " + metadata);
        }
        // BlockVendingMachine marked the upper proxy with BlockDummyable.extra
        // (metadata + 6) for the snacks variant.  The core metadata itself
        // contains only its horizontal facing and cannot recover this bit.
        return metadata >= 6 ? 1 : 0;
    }

    static CompoundTag migrateTank(CompoundTag legacy, String prefix) {
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

    private static void copyLegacyItemsToSlots(CompoundTag legacy, CompoundTag result, int slotCount) {
        ListTag items = requireLegacyItems(legacy);
        for (int index = 0; index < items.size(); index++) {
            LegacyItem item = readLegacyItem(items.getCompound(index), slotCount);
            if (item != null) {
                result.put("Slot" + item.slot(), item.stack());
            }
        }
    }

    private static void copyLegacyItemsToVanillaList(CompoundTag legacy, CompoundTag result, int slotCount) {
        ListTag items = requireLegacyItems(legacy);
        ListTag migrated = new ListTag();
        for (int index = 0; index < items.size(); index++) {
            LegacyItem item = readLegacyItem(items.getCompound(index), slotCount);
            if (item == null) {
                continue;
            }
            CompoundTag stack = item.stack();
            stack.putByte("Slot", (byte) item.slot());
            migrated.add(stack);
        }
        result.put("Items", migrated);
    }

    private static ListTag requireLegacyItems(CompoundTag legacy) {
        if (!legacy.contains("items", Tag.TAG_LIST)) {
            throw new IllegalStateException("Legacy structure multiblock NBT has no items list");
        }
        return legacy.getList("items", Tag.TAG_COMPOUND);
    }

    private static LegacyItem readLegacyItem(CompoundTag source, int slotCount) {
        CompoundTag item = source.copy();
        if (item.isEmpty() || item.getString("id").equals("minecraft:air")) {
            return null;
        }
        int slot = requiredInt(item, "slot");
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalStateException("Legacy structure item slot " + slot + " is outside its machine inventory");
        }
        if (!item.contains("id", Tag.TAG_STRING) || !item.contains("count", Tag.TAG_ANY_NUMERIC)) {
            throw new IllegalStateException("Legacy structure item at slot " + slot + " was not resolved through itemPalette");
        }
        item.remove("slot");
        item.remove("Slot");
        return new LegacyItem(slot, item);
    }

    private record LegacyItem(int slot, CompoundTag stack) {
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
