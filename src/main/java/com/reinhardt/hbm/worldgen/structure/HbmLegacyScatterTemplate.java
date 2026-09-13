package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.HbmLegacyDoorBlock;
import com.reinhardt.hbm.blockentity.HbmStructureLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime template for 1.7.10 WorldGenerator classes that were generated as
 * Java source rather than saved NBT.  The TSV data is mechanically extracted
 * from the old setBlock/placeDoorBlock calls; this class only applies the old
 * metadata and special random selectors while structure pieces write one chunk
 * slice at a time.
 */
final class HbmLegacyScatterTemplate {
    private static final Map<String, HbmLegacyScatterTemplate> CACHE = new ConcurrentHashMap<>();
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final String name;
    private final List<Action> actions;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    private HbmLegacyScatterTemplate(String name, List<Action> actions) {
        this.name = name;
        this.actions = List.copyOf(actions);
        int minX = 0;
        int minY = 0;
        int minZ = 0;
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        boolean first = true;
        for (Action action : actions) {
            if (!action.affectsBlocks()) {
                continue;
            }
            int actionMaxY = action instanceof DoorAction ? action.localY + 1 : action.localY;
            if (first) {
                minX = maxX = action.localX;
                minY = action.localY;
                minZ = maxZ = action.localZ;
                maxY = actionMaxY;
                first = false;
            } else {
                minX = Math.min(minX, action.localX);
                minY = Math.min(minY, action.localY);
                minZ = Math.min(minZ, action.localZ);
                maxX = Math.max(maxX, action.localX);
                maxY = Math.max(maxY, actionMaxY);
                maxZ = Math.max(maxZ, action.localZ);
            }
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    static HbmLegacyScatterTemplate get(String name) {
        return CACHE.computeIfAbsent(name, HbmLegacyScatterTemplate::load);
    }

    int minX() {
        return minX;
    }

    int minY() {
        return minY;
    }

    int minZ() {
        return minZ;
    }

    int maxX() {
        return maxX;
    }

    int maxY() {
        return maxY;
    }

    int maxZ() {
        return maxZ;
    }

    void place(WorldGenLevel level, BlockPos origin, BoundingBox chunkBox, long seed) {
        RandomSource selectorRandom = RandomSource.create(seed);
        RandomSource lootRandom = RandomSource.create(seed ^ 0x5DEECE66DL);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Action action : actions) {
            if (action instanceof BlockAction blockAction) {
                BlockState state = blockAction.state(selectorRandom);
                setBlockIfInside(level, chunkBox, cursor, origin, blockAction.localX, blockAction.localY, blockAction.localZ, state);
            } else if (action instanceof DoorAction doorAction) {
                placeDoor(level, chunkBox, cursor, origin, doorAction);
            }
        }

        for (Action action : actions) {
            if (action instanceof LootAction lootAction) {
                fillHbmLootIfInside(level, chunkBox, cursor, origin, lootAction, lootRandom);
            } else if (action instanceof VanillaDungeonLootAction dungeonLootAction) {
                setVanillaDungeonLootIfInside(level, chunkBox, cursor, origin, dungeonLootAction, lootRandom);
            } else if (action instanceof SpawnerAction spawnerAction) {
                setSpawnerIfInside(level, chunkBox, cursor, origin, spawnerAction, lootRandom);
            }
        }
    }

    private static HbmLegacyScatterTemplate load(String name) {
        String path = "data/" + ReinhardtsHBM.MOD_ID + "/legacy_worldgen/" + name + ".tsv";
        try (InputStream stream = HbmLegacyScatterTemplate.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing legacy HBM scatter template: " + path);
            }
            List<Action> actions = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    actions.add(parseLine(name, lineNumber, line));
                }
            }
            return new HbmLegacyScatterTemplate(name, actions);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read legacy HBM scatter template " + name, exception);
        }
    }

    private static Action parseLine(String name, int lineNumber, String line) {
        String[] fields = line.split("\\t");
        if (fields.length != 6) {
            throw new IllegalStateException("Bad legacy scatter row in " + name + ":" + lineNumber + " -> " + line);
        }
        String kind = fields[0];
        int x = parseInt(name, lineNumber, fields[1]);
        int y = parseInt(name, lineNumber, fields[2]);
        int z = parseInt(name, lineNumber, fields[3]);
        String value = fields[4].toLowerCase(Locale.ROOT);
        int meta = parseInt(name, lineNumber, fields[5]);
        return switch (kind) {
            case "B" -> new FixedBlockAction(x, y, z, value, meta, fixedState(value, meta));
            case "S" -> new SelectorBlockAction(x, y, z, value);
            case "D" -> new DoorAction(x, y, z, value, meta, fixedState(value, meta));
            case "L" -> new LootAction(x, y, z, value, meta);
            case "V" -> new VanillaDungeonLootAction(x, y, z);
            case "M" -> new SpawnerAction(x, y, z);
            default -> throw new IllegalStateException("Unknown legacy scatter row kind in " + name + ":" + lineNumber + " -> " + kind);
        };
    }

    private static int parseInt(String name, int lineNumber, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Bad integer in " + name + ":" + lineNumber + " -> " + value, exception);
        }
    }

    private static BlockState fixedState(String blockId, int meta) {
        BlockState state = specialState(blockId, meta);
        if (state == null) {
            state = HbmLegacyNbtTemplate.stateFromLegacyId(blockId, meta);
        }
        validateResolvedBlock(blockId, state);
        return state;
    }

    private static BlockState specialState(String blockId, int meta) {
        if (blockId.equals("minecraft:redstone_lamp")) {
            return Blocks.REDSTONE_LAMP.defaultBlockState()
                    .setValue(RedstoneLampBlock.LIT, true);
        }
        if (blockId.equals("minecraft:redstone_wire")) {
            return Blocks.REDSTONE_WIRE.defaultBlockState()
                    .setValue(RedStoneWireBlock.POWER, Math.max(0, Math.min(15, meta)));
        }
        return null;
    }

    private static void validateResolvedBlock(String blockId, BlockState state) {
        if (blockId.equals("minecraft:air")) {
            return;
        }
        if (state.isAir()) {
            throw new IllegalStateException("Legacy scatter template resolved non-air block to air: " + blockId);
        }
        if (state.is(Blocks.BARRIER)) {
            throw new IllegalStateException("Legacy scatter template resolved missing HBM block to barrier: " + blockId);
        }
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id != null && !id.getNamespace().equals("minecraft") && !BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalStateException("Legacy scatter template references unregistered block: " + blockId);
        }
    }

    private static void setBlockIfInside(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BlockPos.MutableBlockPos cursor,
            BlockPos origin,
            int localX,
            int localY,
            int localZ,
            BlockState state
    ) {
        cursor.set(origin.getX() + localX, origin.getY() + localY, origin.getZ() + localZ);
        if (cursor.getY() < level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight() || !chunkBox.isInside(cursor)) {
            return;
        }
        level.setBlock(cursor, state, FLAGS);
        HbmLegacyNbtTemplate.refreshPaneConnections(level, cursor);
    }

    private static void placeDoor(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BlockPos.MutableBlockPos cursor,
            BlockPos origin,
            DoorAction action
    ) {
        BlockPos lowerPos = new BlockPos(origin.getX() + action.localX, origin.getY() + action.localY, origin.getZ() + action.localZ);
        if (lowerPos.getY() < level.getMinBuildHeight() || lowerPos.getY() + 1 >= level.getMaxBuildHeight()) {
            return;
        }
        boolean hingeRight = legacyDoorHingeRight(level, lowerPos, action.legacyFacing, action.doorState.getBlock());
        int upperMeta = 8 | (hingeRight ? 1 : 0);
        BlockState lower = HbmLegacyNbtTemplate.pairedLegacyDoorState(action.doorState, action.legacyFacing, upperMeta, DoubleBlockHalf.LOWER);
        BlockState upper = HbmLegacyNbtTemplate.pairedLegacyDoorState(action.doorState, action.legacyFacing, upperMeta, DoubleBlockHalf.UPPER);

        cursor.set(lowerPos);
        if (chunkBox.isInside(cursor)) {
            level.setBlock(cursor, lower, FLAGS);
        }
        cursor.set(lowerPos.above());
        if (chunkBox.isInside(cursor)) {
            level.setBlock(cursor, upper, FLAGS);
        }
    }

    /**
     * Direct equivalent of the 1.7.10 ItemDoor.placeDoorBlock hinge side
     * calculation.  The old facing integer is kept as-is; only the resulting
     * lower/upper states are translated to modern door blockstates.
     */
    private static boolean legacyDoorHingeRight(WorldGenLevel level, BlockPos pos, int side, Block door) {
        int offsetX = 0;
        int offsetZ = 0;
        if (side == 0) {
            offsetZ = 1;
        } else if (side == 1) {
            offsetX = -1;
        } else if (side == 2) {
            offsetZ = -1;
        } else if (side == 3) {
            offsetX = 1;
        }

        int leftSolid = normalCube(level, pos.offset(-offsetX, 0, -offsetZ)) ? 1 : 0;
        leftSolid += normalCube(level, pos.offset(-offsetX, 1, -offsetZ)) ? 1 : 0;
        int rightSolid = normalCube(level, pos.offset(offsetX, 0, offsetZ)) ? 1 : 0;
        rightSolid += normalCube(level, pos.offset(offsetX, 1, offsetZ)) ? 1 : 0;
        boolean leftDoor = sameDoor(level, pos.offset(-offsetX, 0, -offsetZ), door)
                || sameDoor(level, pos.offset(-offsetX, 1, -offsetZ), door);
        boolean rightDoor = sameDoor(level, pos.offset(offsetX, 0, offsetZ), door)
                || sameDoor(level, pos.offset(offsetX, 1, offsetZ), door);

        if (leftDoor && !rightDoor) {
            return true;
        }
        return rightSolid > leftSolid;
    }

    private static boolean normalCube(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean sameDoor(WorldGenLevel level, BlockPos pos, Block door) {
        return level.getBlockState(pos).is(door);
    }

    private static void fillHbmLootIfInside(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BlockPos.MutableBlockPos cursor,
            BlockPos origin,
            LootAction action,
            RandomSource random
    ) {
        cursor.set(origin.getX() + action.localX, origin.getY() + action.localY, origin.getZ() + action.localZ);
        if (!chunkBox.isInside(cursor)) {
            return;
        }
        if (level.getBlockEntity(cursor) instanceof Container container) {
            HbmStructureLoot.fillContainer(container, action.pool, action.count, action.count, random);
        }
    }

    private static void setVanillaDungeonLootIfInside(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BlockPos.MutableBlockPos cursor,
            BlockPos origin,
            VanillaDungeonLootAction action,
            RandomSource random
    ) {
        cursor.set(origin.getX() + action.localX, origin.getY() + action.localY, origin.getZ() + action.localZ);
        if (!chunkBox.isInside(cursor)) {
            return;
        }
        if (level.getBlockEntity(cursor) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
            container.setLootTableSeed(random.nextLong());
            container.setChanged();
        }
    }

    private static void setSpawnerIfInside(
            WorldGenLevel level,
            BoundingBox chunkBox,
            BlockPos.MutableBlockPos cursor,
            BlockPos origin,
            SpawnerAction action,
            RandomSource random
    ) {
        cursor.set(origin.getX() + action.localX, origin.getY() + action.localY, origin.getZ() + action.localZ);
        if (!chunkBox.isInside(cursor)) {
            return;
        }
        if (level.getBlockEntity(cursor) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(randomDungeonMob(random), random);
            spawner.setChanged();
        }
    }

    private static EntityType<?> randomDungeonMob(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> EntityType.SKELETON;
            case 1, 2 -> EntityType.ZOMBIE;
            default -> EntityType.SPIDER;
        };
    }

    private static BlockState randomConcrete(RandomSource random) {
        int roll = random.nextInt(20);
        if (roll <= 1) {
            return fixedState("reinhardtshbm:brick_concrete_broken", 0);
        }
        if (roll <= 4) {
            return fixedState("reinhardtshbm:brick_concrete_cracked", 0);
        }
        if (roll <= 10) {
            return fixedState("reinhardtshbm:brick_concrete_mossy", 0);
        }
        return fixedState("reinhardtshbm:brick_concrete", 0);
    }

    private static BlockState selectorState(String selector, RandomSource random) {
        return switch (selector) {
            case "random_concrete" -> randomConcrete(random);
            case "library_brick" -> fixedState("minecraft:stonebrick", random.nextInt(3));
            case "library_shelf" -> random.nextInt(2) == 0
                    ? fixedState("minecraft:oak_planks", 0)
                    : fixedState("minecraft:bookshelf", 0);
            default -> throw new IllegalStateException("Unknown legacy scatter selector: " + selector);
        };
    }

    private abstract static sealed class Action permits BlockAction, DoorAction, LootAction, VanillaDungeonLootAction, SpawnerAction {
        final int localX;
        final int localY;
        final int localZ;

        Action(int localX, int localY, int localZ) {
            this.localX = localX;
            this.localY = localY;
            this.localZ = localZ;
        }

        boolean affectsBlocks() {
            return false;
        }
    }

    private abstract static sealed class BlockAction extends Action permits FixedBlockAction, SelectorBlockAction {
        BlockAction(int localX, int localY, int localZ) {
            super(localX, localY, localZ);
        }

        @Override
        boolean affectsBlocks() {
            return true;
        }

        abstract BlockState state(RandomSource random);
    }

    private static final class FixedBlockAction extends BlockAction {
        private final BlockState state;

        FixedBlockAction(int localX, int localY, int localZ, String blockId, int meta, BlockState state) {
            super(localX, localY, localZ);
            this.state = state;
        }

        @Override
        BlockState state(RandomSource random) {
            return this.state;
        }
    }

    private static final class SelectorBlockAction extends BlockAction {
        private final String selector;

        SelectorBlockAction(int localX, int localY, int localZ, String selector) {
            super(localX, localY, localZ);
            this.selector = selector;
        }

        @Override
        BlockState state(RandomSource random) {
            return selectorState(this.selector, random);
        }
    }

    private static final class DoorAction extends Action {
        private final String blockId;
        private final int legacyFacing;
        private final BlockState doorState;

        DoorAction(int localX, int localY, int localZ, String blockId, int legacyFacing, BlockState doorState) {
            super(localX, localY, localZ);
            this.blockId = blockId;
            this.legacyFacing = legacyFacing;
            this.doorState = doorState;
            if (!(doorState.getBlock() instanceof DoorBlock) || !doorState.hasProperty(DoorBlock.HALF)) {
                throw new IllegalStateException("Legacy scatter door row does not resolve to a door: " + blockId);
            }
            // Verify that HBM's old-facing helper recognises this metadata now,
            // before a worldgen thread tries to place the structure.
            HbmLegacyDoorBlock.facingFromOldMeta(legacyFacing);
        }

        @Override
        boolean affectsBlocks() {
            return true;
        }
    }

    private static final class LootAction extends Action {
        private final String pool;
        private final int count;

        LootAction(int localX, int localY, int localZ, String pool, int count) {
            super(localX, localY, localZ);
            this.pool = pool;
            this.count = count;
        }
    }

    private static final class VanillaDungeonLootAction extends Action {
        VanillaDungeonLootAction(int localX, int localY, int localZ) {
            super(localX, localY, localZ);
        }
    }

    private static final class SpawnerAction extends Action {
        SpawnerAction(int localX, int localY, int localZ) {
            super(localX, localY, localZ);
        }
    }
}
