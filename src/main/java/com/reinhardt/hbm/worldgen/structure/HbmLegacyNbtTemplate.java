package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.MetalFenceBlock;
import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.block.ChargerBlock;
import com.reinhardt.hbm.block.DungeonChainBlock;
import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.block.MicrowaveBlock;
import com.reinhardt.hbm.block.SmallBoilerBlock;
import com.reinhardt.hbm.block.SteelGrateBlock;
import com.reinhardt.hbm.block.SteelScaffoldBlock;
import com.reinhardt.hbm.block.SuperConcreteBlock;
import com.reinhardt.hbm.block.SteelWallBlock;
import com.reinhardt.hbm.block.SteelPolesBlock;
import com.reinhardt.hbm.block.SteelBeamBlock;
import com.reinhardt.hbm.block.WoodStructureBlock;
import com.reinhardt.hbm.block.WoodBarrierBlock;
import com.reinhardt.hbm.block.CaveSpikeBlock;
import com.reinhardt.hbm.block.DecoModelBlock;
import com.reinhardt.hbm.block.DecoCrtBlock;
import com.reinhardt.hbm.block.DeadPlantBlock;
import com.reinhardt.hbm.block.HbmAnvilBlock;
import com.reinhardt.hbm.block.HbmLegacyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.block.SpotlightBlock;
import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HbmLegacyNbtTemplate {
    private static final Map<String, HbmLegacyNbtTemplate> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED_MISSING_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> LEGACY_ID_ALIASES = Map.of(
            "reinhardtshbm:brick_concrete_slab", "reinhardtshbm:concrete_brick_slab",
            "reinhardtshbm:ore_coal_oil", "minecraft:coal_ore",
            "reinhardtshbm:ore_coal_oil_burning", "minecraft:coal_ore",
            // Latest 1.7.10 still serializes the statue under the old undefined registry name.
            "reinhardtshbm:#undef", "reinhardtshbm:statue_elb_f",
            "reinhardtshbm:hev_battery", "reinhardtshbm:hev_battery_block",
            "reinhardtshbm:machine_battery", "reinhardtshbm:capacitor_copper",
            "reinhardtshbm:block_copper", "minecraft:copper_block",
            "reinhardtshbm:machine_weapon_table", "minecraft:air",
            // These two blocks were deliberately removed upstream while their old structures
            // were left untouched. Legacy NBTStructure resolves the now-missing ids to air.
            "reinhardtshbm:barrel_iron", "minecraft:air",
            "reinhardtshbm:ladder_tungsten", "minecraft:air"
    );

    private final String name;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<PlacedBlock> blocks;
    private final Map<BlockPos, LegacyStructureMultiblocks.Part> multiblockParts;
    private final Map<BlockPos, LegacyStructureHeavyDoors.Part> heavyDoorParts;
    private final List<List<JigsawConnection>> fromConnections;
    private final Map<String, List<JigsawConnection>> toTopConnections;
    private final Map<String, List<JigsawConnection>> toBottomConnections;
    private final Map<String, List<JigsawConnection>> toHorizontalConnections;

    private HbmLegacyNbtTemplate(
            String name,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<PlacedBlock> blocks,
            List<List<JigsawConnection>> fromConnections,
            Map<String, List<JigsawConnection>> toTopConnections,
            Map<String, List<JigsawConnection>> toBottomConnections,
            Map<String, List<JigsawConnection>> toHorizontalConnections
    ) {
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = List.copyOf(blocks);
        this.multiblockParts = indexLegacyMultiblocks(this.blocks);
        this.heavyDoorParts = indexLegacyHeavyDoors(this.blocks);
        this.fromConnections = copyConnectionGroups(fromConnections);
        this.toTopConnections = copyConnectionMap(toTopConnections);
        this.toBottomConnections = copyConnectionMap(toBottomConnections);
        this.toHorizontalConnections = copyConnectionMap(toHorizontalConnections);
    }

    public static HbmLegacyNbtTemplate get(String name) {
        return CACHE.computeIfAbsent(name, HbmLegacyNbtTemplate::load);
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    int rotatedSizeX(int rotation) {
        return swapsAxes(rotation) ? sizeZ : sizeX;
    }

    int rotatedSizeZ(int rotation) {
        return swapsAxes(rotation) ? sizeX : sizeZ;
    }

    void place(WorldGenLevel level, BoundingBox pieceBox, BoundingBox chunkBox, int rotation, boolean conformToTerrain, int heightOffset) {
        place(level, pieceBox, chunkBox, rotation, conformToTerrain, heightOffset, null, null);
    }

    void place(
            WorldGenLevel level,
            BoundingBox pieceBox,
            BoundingBox chunkBox,
            int rotation,
            boolean conformToTerrain,
            int heightOffset,
            @Nullable RandomSource random,
            @Nullable BlockReplacement replacement
    ) {
        place(level, pieceBox, chunkBox, rotation, conformToTerrain, heightOffset, random, replacement, null);
    }

    void place(
            WorldGenLevel level,
            BoundingBox pieceBox,
            BoundingBox chunkBox,
            int rotation,
            boolean conformToTerrain,
            int heightOffset,
            @Nullable RandomSource random,
            @Nullable BlockReplacement replacement,
            @Nullable String tandemStructureName
    ) {
        RandomSource placementRandom = random != null ? random : level.getRandom();
        for (PlacedBlock block : blocks) {
            BlockPos localPos = new BlockPos(block.x, block.y, block.z);
            BlockPos pos = worldPosFor(level, pieceBox, rotation, conformToTerrain, heightOffset, block);
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                continue;
            }
            if (!chunkBox.isInside(pos)) {
                continue;
            }

            try {
                LegacyStructureMultiblocks.Part multiblockPart = this.multiblockParts.get(localPos);
                LegacyStructureHeavyDoors.Part heavyDoorPart = this.heavyDoorParts.get(localPos);
                boolean isLegacyDummy = multiblockPart != null && !multiblockPart.isCoreAt(localPos);
                boolean isLegacyHeavyDoorPart = heavyDoorPart != null && !heavyDoorPart.isCoreAt(localPos);
                BlockState state;
                if (isLegacyHeavyDoorPart) {
                    state = HbmBlocks.HEAVY_DOOR_PART.get().defaultBlockState()
                            .setValue(HbmHeavyDoorPartBlock.EXTRA, false);
                } else if (isLegacyDummy) {
                    // 1.7.10 saved this exact occupied block as a BlockDummyable
                    // proxy.  It is not another core instance of the machine.
                    state = HbmBlocks.MACHINE_DUMMY.get().defaultBlockState();
                } else {
                    state = block.state;
                    if (multiblockPart != null) {
                        state = LegacyStructureMultiblocks.migrateCoreState(multiblockPart, state);
                    }
                    if (replacement != null && random != null) {
                        state = replacement.replace(block.name, block.meta, state, random);
                    }
                    if (this.name.equals("factory") && block.name.equals("reinhardtshbm:rail_narrow")) {
                        state = factoryNarrowRailState(block.meta, rotation, state);
                    }
                    // Legacy NBTStructure transforms rail coordinates but leaves
                    // RailGeneric metadata unchanged. Levers are the exception:
                    // 1.7.10 transforms their complete EnumOrientation metadata
                    // (including the intentionally face-flipping 0/7 and 5/6
                    // pairs) before writing the block state.
                    if (state.getBlock() == Blocks.LEVER) {
                        state = stateFromLegacyId(block.name, transformLegacyLeverMeta(block.meta, rotation));
                    } else if (!(state.getBlock() instanceof RailBlock)) {
                        state = state.rotate(toMcRotation(rotation));
                    }
                }

                setStructureBlock(level, pos, state);
                if (isLegacyHeavyDoorPart) {
                    BlockPos corePos = worldPosFor(
                            level,
                            pieceBox,
                            rotation,
                            conformToTerrain,
                            heightOffset,
                            blockAt(heavyDoorPart.corePos())
                    );
                    Direction worldFacing = toMcRotation(rotation).rotate(heavyDoorPart.coreFacing());
                    BlockPos localOffset = HbmHeavyDoorBlock.worldOffsets(
                            heavyDoorPart.decl(), worldFacing, corePos
                    ).get(pos);
                    if (localOffset == null) {
                        throw new IllegalStateException("Rotated legacy " + heavyDoorPart.decl()
                                + " part at " + pos + " is outside the footprint around " + corePos
                                + " facing " + worldFacing);
                    }
                    linkStructureHeavyDoorPart(level, pos, state, corePos, localOffset);
                } else if (isLegacyDummy) {
                    BlockPos corePos = worldPosFor(
                            level,
                            pieceBox,
                            rotation,
                            conformToTerrain,
                            heightOffset,
                            blockAt(multiblockPart.corePos())
                    );
                    linkStructureDummy(level, pos, state, corePos);
                } else {
                    CompoundTag blockEntityNbt = transformLegacyBlockEntityNbt(
                            block.name,
                            block.nbt,
                            rotation,
                            tandemStructureName,
                            placementRandom
                    );
                    if (multiblockPart != null) {
                        if (blockEntityNbt == null) {
                            throw new IllegalStateException("Legacy " + multiblockPart.kind() + " core at " + localPos + " has no block entity NBT");
                        }
                        blockEntityNbt = LegacyStructureMultiblocks.migrateCoreNbt(multiblockPart.kind(), blockEntityNbt);
                    }
                    if (heavyDoorPart != null) {
                        if (blockEntityNbt == null) {
                            throw new IllegalStateException("Legacy " + heavyDoorPart.decl()
                                    + " core at " + localPos + " has no block entity NBT");
                        }
                        blockEntityNbt = LegacyStructureHeavyDoors.migrateCoreNbt(blockEntityNbt);
                    }
                    if (blockEntityNbt != null) {
                        loadBlockEntity(level, pos, state, blockEntityNbt,
                                multiblockPart != null || heavyDoorPart != null);
                    }
                }
                refreshPaneConnections(level, pos);
            } catch (RuntimeException exception) {
                ReinhardtsHBM.LOGGER.error(
                        "Skipping legacy HBM structure block {} meta {} from template {} at local {} / world {}",
                        block.name,
                        block.meta,
                        this.name,
                        localPos,
                        pos,
                        exception
                );
            }
        }
    }

    private BlockPos worldPosFor(
            WorldGenLevel level,
            BoundingBox pieceBox,
            int rotation,
            boolean conformToTerrain,
            int heightOffset,
            PlacedBlock block
    ) {
        int x = rotateX(block.x, block.z, rotation) + pieceBox.minX();
        int z = rotateZ(block.x, block.z, rotation) + pieceBox.minZ();
        int yBase = conformToTerrain
                ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + heightOffset
                : pieceBox.minY();
        return new BlockPos(x, yBase + block.y, z);
    }

    private PlacedBlock blockAt(BlockPos localPos) {
        for (PlacedBlock block : this.blocks) {
            if (block.x == localPos.getX() && block.y == localPos.getY() && block.z == localPos.getZ()) {
                return block;
            }
        }
        throw new IllegalStateException("Legacy multiblock core is absent from template " + this.name + " at " + localPos);
    }

    /**
     * The factory template contains thirteen legacy {@code rail_narrow} states,
     * all with metadata {@code 1} (east-west).  In 1.7.10 NBTStructure wrote
     * that metadata unchanged, then BlockRailBase recalculated its final straight
     * shape when the neighbouring rails were placed.  Worldgen now places the
     * template one chunk at a time, so retain that final old-world result here
     * rather than relying on an update that may occur before its neighbours exist.
     */
    private static BlockState factoryNarrowRailState(int legacyMeta, int rotation, BlockState state) {
        if (legacyMeta != 1) {
            throw new IllegalStateException("Factory narrow rail must use legacy metadata 1, got " + legacyMeta);
        }
        if (!(state.getBlock() instanceof RailBlock) || !state.hasProperty(BlockStateProperties.RAIL_SHAPE)) {
            throw new IllegalStateException("Factory narrow rail did not resolve to a rail blockstate");
        }

        RailShape finalShape = switch (rotation & 3) {
            case 0, 2 -> RailShape.EAST_WEST;
            case 1, 3 -> RailShape.NORTH_SOUTH;
            default -> throw new IllegalStateException("Unreachable factory rotation: " + rotation);
        };
        return state.setValue(BlockStateProperties.RAIL_SHAPE, finalShape);
    }

    static void refreshPaneConnections(LevelAccessor level, BlockPos changedPos) {
        // Legacy NBTStructure wrote with flag 2. Modern panes, fences, walls
        // and stair corners persist their neighbour-derived shape in the
        // block state, so refresh only those exact block families after the
        // bulk write instead of issuing broad neighbour updates that could
        // detach or drop other structure blocks.
        refreshConnectedShape(level, changedPos);
        for (Direction direction : Direction.values()) {
            refreshConnectedShape(level, changedPos.relative(direction));
        }
    }

    private static void refreshConnectedShape(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DungeonChainBlock) {
            refreshDungeonChainEnd(level, pos, state);
            return;
        }
        if (state.getBlock() instanceof WoodStructureBlock) {
            BlockState connected = state;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                connected = connected.updateShape(direction, level.getBlockState(neighborPos), level, pos, neighborPos);
            }
            if (connected != state) {
                level.setBlock(pos, connected, 2);
            }
            return;
        }
        if (state.getBlock() instanceof WoodBarrierBlock) {
            BlockState connected = WoodBarrierBlock.updateConnections(state, level, pos);
            if (connected != state) {
                level.setBlock(pos, connected, 2);
            }
            return;
        }
        if (state.getBlock() instanceof FluidDuctBlock duct) {
            duct.refreshConnections(level, pos);
            return;
        }
        if (state.getBlock() instanceof EnergyCableBlock) {
            EnergyCableBlock.refreshConnections(level, pos);
            return;
        }
        if (state.getBlock() instanceof MetalFenceBlock) {
            MetalFenceBlock.refreshConnections(level, pos);
            return;
        }
        if (!(state.getBlock() instanceof IronBarsBlock)
                && !(state.getBlock() instanceof FenceBlock)
                && !(state.getBlock() instanceof WallBlock)
                && !(state.getBlock() instanceof StairBlock)) {
            return;
        }
        BlockState connected = state;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            connected = connected.updateShape(direction, level.getBlockState(neighborPos), level, pos, neighborPos);
        }
        if (connected != state) {
            level.setBlock(pos, connected, 2);
        }
    }

    private static void refreshDungeonChainEnd(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        boolean continues = below.is(state.getBlock())
                && below.getValue(DungeonChainBlock.WALL) == state.getValue(DungeonChainBlock.WALL);
        boolean end = !continues && !below.isFaceSturdy(level, belowPos, Direction.UP);
        if (state.getValue(DungeonChainBlock.END) != end) {
            level.setBlock(pos, state.setValue(DungeonChainBlock.END, end), 2);
        }
    }

    List<List<JigsawConnection>> fromConnections() {
        return fromConnections;
    }

    public List<JigsawConnection> getConnectionPool(Direction fromDirection, String target) {
        if (fromDirection == Direction.DOWN) {
            return toTopConnections.getOrDefault(target, List.of());
        }
        if (fromDirection == Direction.UP) {
            return toBottomConnections.getOrDefault(target, List.of());
        }
        return toHorizontalConnections.getOrDefault(target, List.of());
    }

    private static HbmLegacyNbtTemplate load(String name) {
        String path = "data/" + ReinhardtsHBM.MOD_ID + "/structure/" + name + ".nbt";
        try (InputStream stream = HbmLegacyNbtTemplate.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                ReinhardtsHBM.LOGGER.error("Missing legacy HBM structure NBT: {}", path);
                return empty(name);
            }

            CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            int[] size = readIntList(root.getList("size", Tag.TAG_INT), 3);
            ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
            PaletteEntry[] palette = new PaletteEntry[paletteTag.size()];
            for (int i = 0; i < paletteTag.size(); i++) {
                palette[i] = readPaletteEntry(paletteTag.getCompound(i));
            }

            Map<Short, String> itemPalette = readItemPalette(root);
            List<PlacedBlock> placedBlocks = new ArrayList<>();
            List<JigsawConnection> connections = new ArrayList<>();
            Map<String, List<JigsawConnection>> topConnections = new HashMap<>();
            Map<String, List<JigsawConnection>> bottomConnections = new HashMap<>();
            Map<String, List<JigsawConnection>> horizontalConnections = new HashMap<>();
            ListTag blockTag = root.getList("blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blockTag.size(); i++) {
                CompoundTag block = blockTag.getCompound(i);
                int stateIndex = block.getInt("state");
                if (stateIndex < 0 || stateIndex >= palette.length) {
                    continue;
                }

                int[] pos = readIntList(block.getList("pos", Tag.TAG_INT), 3);
                PaletteEntry entry = palette[stateIndex];
                CompoundTag nbt = block.contains("nbt", Tag.TAG_COMPOUND)
                        ? cleanBlockEntityNbt(entry.name, block.getCompound("nbt"), itemPalette)
                        : null;
                if (isWandJigsaw(entry, nbt)) {
                    registerJigsawConnection(nbt, pos, connections, topConnections, bottomConnections, horizontalConnections);
                    entry = jigsawReplacement(nbt);
                    nbt = null;
                } else if (entry.name.equals("reinhardtshbm:wand_air")) {
                    entry = new PaletteEntry("minecraft:air", 0);
                    nbt = null;
                }

                BlockState state = stateFromLegacy(entry.name, entry.meta, nbt);
                placedBlocks.add(new PlacedBlock(pos[0], pos[1], pos[2], entry.name, entry.meta, state, nbt));
            }
            linkLegacyDoorStates(placedBlocks);

            return new HbmLegacyNbtTemplate(
                    name,
                    size[0],
                    size[1],
                    size[2],
                    placedBlocks,
                    groupFromConnections(connections),
                    topConnections,
                    bottomConnections,
                    horizontalConnections
            );
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to read legacy HBM structure {}", name, exception);
            return empty(name);
        } catch (RuntimeException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to parse legacy HBM structure {}", name, exception);
            return empty(name);
        }
    }

    private static HbmLegacyNbtTemplate empty(String name) {
        return new HbmLegacyNbtTemplate(name, 1, 1, 1, List.of(), List.of(), Map.of(), Map.of(), Map.of());
    }

    private static PaletteEntry readPaletteEntry(CompoundTag tag) {
        String name = tag.getString("Name");
        int meta = 0;
        if (tag.contains("Properties", Tag.TAG_COMPOUND)) {
            CompoundTag properties = tag.getCompound("Properties");
            if (properties.contains("meta", Tag.TAG_STRING)) {
                try {
                    meta = Integer.parseInt(properties.getString("meta"));
                } catch (NumberFormatException ignored) {
                    meta = 0;
                }
            }
        }

        return new PaletteEntry(normalizeId(name), meta);
    }

    private static Map<Short, String> readItemPalette(CompoundTag root) {
        if (!root.contains("itemPalette", Tag.TAG_LIST)) {
            return Map.of();
        }

        Map<Short, String> palette = new HashMap<>();
        ListTag itemPalette = root.getList("itemPalette", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemPalette.size(); i++) {
            CompoundTag entry = itemPalette.getCompound(i);
            palette.put(entry.getShort("ID"), normalizeItemId(entry.getString("Name")));
        }
        return palette;
    }

    private static CompoundTag cleanBlockEntityNbt(
            String blockName,
            CompoundTag tag,
            Map<Short, String> itemPalette
    ) {
        CompoundTag copy = tag.copy();
        cleanLegacyNbt(copy, itemPalette);
        return migrateOrdinaryBlockEntityNbt(blockName, copy);
    }

    static CompoundTag migrateOrdinaryBlockEntityNbt(String blockName, CompoundTag legacy) {
        CompoundTag result = legacy.copy();

        if (blockName.equals("reinhardtshbm:capacitor_copper")
                && result.contains("power", Tag.TAG_ANY_NUMERIC)
                && !result.contains("Power", Tag.TAG_ANY_NUMERIC)) {
            result.putLong("Power", Mth.clamp(result.getLong("power"), 0L, 1_000_000L));
        }

        boolean legacyDiesel = result.contains("items", Tag.TAG_LIST)
                || result.contains("powerTime", Tag.TAG_ANY_NUMERIC)
                || result.contains("fuel_type", Tag.TAG_STRING)
                || result.contains("smoke0_type", Tag.TAG_STRING);
        boolean legacyBarrel = result.contains("items", Tag.TAG_LIST)
                || result.contains("tank_type", Tag.TAG_STRING)
                || result.contains("mode", Tag.TAG_ANY_NUMERIC);
        boolean legacyRedConnector = result.contains("color", Tag.TAG_ANY_NUMERIC)
                || result.contains("conCount", Tag.TAG_ANY_NUMERIC);
        boolean legacyDamagedSentry = result.contains("items", Tag.TAG_LIST)
                || result.contains("isOn", Tag.TAG_BYTE)
                || result.contains("targetPlayers", Tag.TAG_BYTE)
                || result.contains("targetAnimals", Tag.TAG_BYTE)
                || result.contains("targetMobs", Tag.TAG_BYTE)
                || result.contains("targetMachines", Tag.TAG_BYTE);

        if (blockName.equals("reinhardtshbm:machine_diesel") && legacyDiesel) {
            migrateLegacyItemsToSlots(result, 5);
            result.putLong("Power", result.getLong("powerTime"));
            result.putLong("PowerCap", result.getLong("powerCap"));
            result.putBoolean("Running", result.getBoolean("isOn"));
            result.put("FuelTank", LegacyStructureMultiblocks.migrateTank(result, "fuel"));
            result.put("Smoke", LegacyStructureMultiblocks.migrateTank(result, "smoke0"));
            result.put("SmokeLeaded", LegacyStructureMultiblocks.migrateTank(result, "smoke1"));
            result.put("SmokePoison", LegacyStructureMultiblocks.migrateTank(result, "smoke2"));
        } else if ((blockName.equals("reinhardtshbm:barrel_corroded")
                || blockName.equals("reinhardtshbm:barrel_plastic")) && legacyBarrel) {
            migrateLegacyItemsToSlots(result, 6);
            result.put("Tank", LegacyStructureMultiblocks.migrateTank(result, "tank"));
            result.putByte("Mode", result.getByte("mode"));
            result.putBoolean("Exploded", false);
            result.putBoolean("OnFire", false);
        } else if (blockName.equals("reinhardtshbm:red_connector") && legacyRedConnector) {
            result.putInt("Color", result.getInt("color"));
            int connectionCount = result.getInt("conCount");
            if (connectionCount != 0) {
                throw new IllegalStateException("Legacy structure red connector has "
                        + connectionCount + " absolute connections; generated structures must save none");
            }
            result.put("Connections", new ListTag());
        } else if (blockName.equals("reinhardtshbm:turret_sentry_damaged") && legacyDamagedSentry) {
            migrateLegacyItemsToVanillaList(result, 11);
            result.putBoolean("on", result.getBoolean("isOn"));
            result.putBoolean("target_players", result.getBoolean("targetPlayers"));
            result.putBoolean("target_animals", result.getBoolean("targetAnimals"));
            result.putBoolean("target_mobs", result.getBoolean("targetMobs"));
            result.putBoolean("target_machines", result.getBoolean("targetMachines"));
        } else {
            int crateSlots = switch (blockName) {
                case "reinhardtshbm:crate_iron" -> 36;
                case "reinhardtshbm:crate_steel" -> 54;
                case "reinhardtshbm:crate_desh" -> 104;
                case "reinhardtshbm:crate_tungsten", "reinhardtshbm:crate_template" -> 27;
                default -> -1;
            };
            if (crateSlots >= 0) {
                migrateLegacyItemsToCrate(result, crateSlots);
            }
        }
        return result;
    }

    private static void migrateLegacyItemsToSlots(CompoundTag tag, int slotCount) {
        if (!tag.contains("items", Tag.TAG_LIST)) {
            return;
        }
        ListTag items = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag stack = items.getCompound(index);
            if (stack.isEmpty() || stack.getString("id").equals("minecraft:air")) {
                continue;
            }
            int slot = legacyItemSlot(stack);
            if (slot < 0 || slot >= slotCount) {
                throw new IllegalStateException("Legacy item slot " + slot + " is outside inventory size " + slotCount);
            }
            CompoundTag migrated = stack.copy();
            migrated.remove("slot");
            migrated.remove("Slot");
            tag.put("Slot" + slot, migrated);
        }
        tag.remove("items");
    }

    private static void migrateLegacyItemsToCrate(CompoundTag tag, int slotCount) {
        if (!tag.contains("items", Tag.TAG_LIST)) {
            return;
        }
        CompoundTag migratedItems = tag.contains("Items", Tag.TAG_COMPOUND)
                ? tag.getCompound("Items").copy()
                : new CompoundTag();
        ListTag items = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag stack = items.getCompound(index);
            if (stack.isEmpty() || stack.getString("id").equals("minecraft:air")) {
                continue;
            }
            int slot = legacyItemSlot(stack);
            if (slot < 0 || slot >= slotCount) {
                throw new IllegalStateException("Legacy crate item slot " + slot + " is outside inventory size " + slotCount);
            }
            CompoundTag migrated = stack.copy();
            migrated.remove("slot");
            migrated.remove("Slot");
            migratedItems.put("slot" + slot, migrated);
        }
        copyIfPresent(tag, migratedItems, "lock");
        copyIfPresent(tag, migratedItems, "isLocked");
        copyIfPresent(tag, migratedItems, "lockMod");
        copyIfPresent(tag, migratedItems, "cheesable");
        copyIfPresent(tag, migratedItems, "spiders");
        tag.put("Items", migratedItems);
        tag.remove("items");
    }

    private static void migrateLegacyItemsToVanillaList(CompoundTag tag, int slotCount) {
        if (!tag.contains("items", Tag.TAG_LIST)) {
            return;
        }
        ListTag migratedItems = new ListTag();
        ListTag items = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag stack = items.getCompound(index);
            if (stack.isEmpty() || stack.getString("id").equals("minecraft:air")) {
                continue;
            }
            int slot = legacyItemSlot(stack);
            if (slot < 0 || slot >= slotCount) {
                throw new IllegalStateException("Legacy item slot " + slot + " is outside inventory size " + slotCount);
            }
            CompoundTag migrated = stack.copy();
            migrated.remove("slot");
            migrated.putByte("Slot", (byte) slot);
            migratedItems.add(migrated);
        }
        tag.put("Items", migratedItems);
        tag.remove("items");
    }

    private static int legacyItemSlot(CompoundTag stack) {
        if (stack.contains("slot", Tag.TAG_ANY_NUMERIC)) {
            return stack.getInt("slot");
        }
        if (stack.contains("Slot", Tag.TAG_ANY_NUMERIC)) {
            return stack.getByte("Slot") & 255;
        }
        throw new IllegalStateException("Legacy structure item has no slot field");
    }

    private static void copyIfPresent(CompoundTag source, CompoundTag target, String key) {
        Tag value = source.get(key);
        if (value != null) {
            target.put(key, value.copy());
        }
    }

    private static void cleanLegacyNbt(Tag tag, Map<Short, String> itemPalette) {
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("block", Tag.TAG_STRING)) {
                compound.putString("block", normalizeId(compound.getString("block")));
            }

            if (looksLikeItemStack(compound) && !migrateLegacyItemStack(compound, itemPalette)) {
                clearCompound(compound);
                return;
            }

            for (String key : List.copyOf(compound.getAllKeys())) {
                cleanLegacyNbt(compound.get(key), itemPalette);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Tag child = list.get(i);
                if (child instanceof CompoundTag compound && looksLikeItemStack(compound)
                        && !migrateLegacyItemStack(compound, itemPalette)) {
                    list.remove(i);
                    continue;
                }
                cleanLegacyNbt(child, itemPalette);
            }
        }
    }

    private static boolean looksLikeItemStack(CompoundTag tag) {
        return tag.contains("id")
                && (tag.contains("Count") || tag.contains("count") || tag.contains("Slot") || tag.contains("slot") || tag.contains("Damage"));
    }

    static boolean migrateLegacyItemStack(CompoundTag tag, Map<Short, String> itemPalette) {
        if (tag.contains("id", Tag.TAG_STRING)) {
            tag.putString("id", normalizeItemId(tag.getString("id")));
        } else if (tag.contains("id", Tag.TAG_SHORT)) {
            String mapped = itemPalette.get(tag.getShort("id"));
            if (mapped != null) {
                tag.putString("id", normalizeItemId(mapped));
            }
        } else if (tag.contains("id", Tag.TAG_INT)) {
            int id = tag.getInt("id");
            if (id >= Short.MIN_VALUE && id <= Short.MAX_VALUE) {
                String mapped = itemPalette.get((short) id);
                if (mapped != null) {
                    tag.putString("id", normalizeItemId(mapped));
                } else if (id == 386) {
                    // The latest lighthouse template contains one vanilla
                    // 1.7.10 written book without an item-palette entry.
                    tag.putString("id", "minecraft:written_book");
                }
            }
        }

        if (tag.contains("Count", Tag.TAG_BYTE) && !tag.contains("count")) {
            tag.putInt("count", Math.max(1, tag.getByte("Count") & 255));
        }
        if (tag.getString("id").equals("minecraft:written_book")
                && tag.contains("tag", Tag.TAG_COMPOUND)
                && !tag.contains("components", Tag.TAG_COMPOUND)) {
            migrateLegacyWrittenBook(tag);
        }
        return !tag.contains("id", Tag.TAG_STRING) || !tag.getString("id").equals("minecraft:air");
    }

    private static void migrateLegacyWrittenBook(CompoundTag stack) {
        CompoundTag legacyData = stack.getCompound("tag");
        ListTag pages = legacyData.getList("pages", Tag.TAG_STRING).copy();

        CompoundTag content = new CompoundTag();
        content.putString("title", legacyData.getString("title"));
        content.putString("author", legacyData.getString("author"));
        content.putInt("generation", Mth.clamp(legacyData.getInt("generation"), 0, 3));
        content.put("pages", pages);
        content.putBoolean("resolved", legacyData.getBoolean("resolved"));

        CompoundTag components = new CompoundTag();
        components.put("minecraft:written_book_content", content);
        stack.put("components", components);
        stack.remove("tag");
        stack.remove("Count");
        stack.remove("Damage");
    }

    private static void clearCompound(CompoundTag tag) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            tag.remove(key);
        }
    }

    @Nullable
    private static CompoundTag transformLegacyBlockEntityNbt(
            String blockName,
            @Nullable CompoundTag nbt,
            int rotation,
            @Nullable String tandemStructureName,
            RandomSource random
    ) {
        if (nbt == null) {
            return null;
        }

        // The 1.7.10 floodlight tile entity saved lower-case fields.  The
        // modern block entity uses the normalised names below; without this
        // bridge a structure loses its stored aim and power whenever it is
        // generated from the legacy NBT template.
        if (blockName.equals("reinhardtshbm:floodlight")) {
            return migrateLegacyFloodlightNbt(nbt);
        }

        // NBTStructure always calls TileEntityBobble#transformTE while
        // building a template.  The saved subtype is deliberately ignored;
        // every generated structure receives one random non-empty bobblehead.
        if (blockName.equals("reinhardtshbm:bobblehead")) {
            CompoundTag copy = nbt.copy();
            copy.putByte("type", (byte) (random.nextInt(BobbleheadType.values().length - 1) + 1));
            return copy;
        }

        // 1.7.10 BlockWandLoot#transformTE: trigger on first tick and rotate only its stored placement yaw.
        if (blockName.equals("reinhardtshbm:wand_loot")) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("trigger", true);
            copy.putFloat("rot", Mth.wrapDegrees(copy.getFloat("rot") + (rotation & 3) * 90.0F));
            return copy;
        }

        // 1.7.10 BlockWandLogic#transformTE: the logic direction is not transformed here.
        if (blockName.equals("reinhardtshbm:wand_logic")) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("trigger", true);
            return copy;
        }

        // 1.7.10 NBTStructure#buildTileEntity arms a tandem only while building a named jigsaw structure.
        if (blockName.equals("reinhardtshbm:wand_tandem") && tandemStructureName != null && !tandemStructureName.isBlank()) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("isArmed", true);
            copy.putString("structure", tandemStructureName);
            return copy;
        }

        return nbt;
    }

    static CompoundTag migrateLegacyFloodlightNbt(CompoundTag nbt) {
        CompoundTag copy = nbt.copy();
        if (copy.contains("rotation", Tag.TAG_ANY_NUMERIC) && !copy.contains("Rotation", Tag.TAG_ANY_NUMERIC)) {
            copy.putFloat("Rotation", copy.getFloat("rotation"));
        }
        if (copy.contains("power", Tag.TAG_ANY_NUMERIC) && !copy.contains("Power", Tag.TAG_ANY_NUMERIC)) {
            long legacyPower = copy.getLong("power");
            copy.putInt("Power", (int) Math.max(0L, Math.min((long) FloodlightBlockEntity.MAX_POWER, legacyPower)));
        }
        if (copy.contains("isOn", Tag.TAG_ANY_NUMERIC) && !copy.contains("On", Tag.TAG_ANY_NUMERIC)) {
            copy.putBoolean("On", copy.getBoolean("isOn"));
        }
        return copy;
    }

    private static void setStructureBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        // LegacyTurretBlock normally expands itself from onPlace.  NBTStructure
        // did not do that: it wrote the saved 2x2 dummy layout verbatim.
        if (state.getBlock() instanceof LegacyTurretBlock) {
            LegacyTurretBlock.runWithoutAutomaticDummies(() -> level.setBlock(pos, state, 2));
            return;
        }
        level.setBlock(pos, state, 2);
    }

    private static void linkStructureDummy(WorldGenLevel level, BlockPos pos, BlockState state, BlockPos corePos) {
        BlockEntity blockEntity = getOrCreateBlockEntity(level, pos, state);
        if (!(blockEntity instanceof MachineDummyBlockEntity dummy)) {
            throw new IllegalStateException("Legacy structure dummy at " + pos + " did not create MachineDummyBlockEntity");
        }
        dummy.setCorePos(corePos);
    }

    private static void linkStructureHeavyDoorPart(
            WorldGenLevel level,
            BlockPos pos,
            BlockState state,
            BlockPos corePos,
            BlockPos localOffset
    ) {
        BlockEntity blockEntity = getOrCreateBlockEntity(level, pos, state);
        if (!(blockEntity instanceof HbmHeavyDoorPartBlockEntity part)) {
            throw new IllegalStateException("Legacy heavy-door part at " + pos
                    + " did not create HbmHeavyDoorPartBlockEntity");
        }
        part.configure(corePos, localOffset);
    }

    private static void loadBlockEntity(WorldGenLevel level, BlockPos pos, BlockState state, CompoundTag nbt, boolean strictLegacyMultiblock) {
        if (!state.hasBlockEntity()) {
            if (strictLegacyMultiblock) {
                throw new IllegalStateException("Legacy multiblock core " + state + " at " + pos + " has no block entity state");
            }
            return;
        }

        CompoundTag copy = nbt.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());

        BlockEntity blockEntity = getOrCreateBlockEntity(level, pos, state);
        if (blockEntity == null) {
            if (strictLegacyMultiblock) {
                throw new IllegalStateException("Legacy multiblock core " + state + " at " + pos + " did not create a block entity");
            }
            ReinhardtsHBM.LOGGER.error("Skipping structure block entity NBT for {} at {} because no block entity was created", state, pos);
            return;
        }
        try {
            blockEntity.loadWithComponents(copy, level.registryAccess());
            blockEntity.setChanged();
        } catch (RuntimeException exception) {
            if (strictLegacyMultiblock) {
                throw new IllegalStateException("Failed to load strict legacy multiblock NBT for " + state + " at " + pos, exception);
            }
            // A malformed legacy tag must not abort the complete feature-placement
            // task.  Keep the placed block and leave the entity at its defaults.
            ReinhardtsHBM.LOGGER.error("Failed to load block entity NBT for structure block {} at {}", state, pos, exception);
        }
    }

    @Nullable
    private static BlockEntity getOrCreateBlockEntity(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // WorldGenRegion deliberately defers block-entity creation while a
        // chunk is still being generated.  Create and attach the entity to
        // the backing ChunkAccess before loading its legacy NBT; otherwise a
        // perfectly valid structure block can abort feature placement (and
        // crash the integrated server) with a null block entity.
        if (blockEntity == null && level instanceof WorldGenRegion region
                && state.getBlock() instanceof EntityBlock entityBlock) {
            try {
                blockEntity = entityBlock.newBlockEntity(pos, state);
                if (blockEntity != null) {
                    region.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockEntity(blockEntity);
                }
            } catch (RuntimeException exception) {
                ReinhardtsHBM.LOGGER.error("Failed to create block entity for structure block {} at {}", state, pos, exception);
                return null;
            }
        }
        return blockEntity;
    }

    private static boolean isWandJigsaw(PaletteEntry entry, @Nullable CompoundTag nbt) {
        return entry.name.equals("reinhardtshbm:wand_jigsaw") && nbt != null;
    }

    private static void registerJigsawConnection(
            CompoundTag nbt,
            int[] pos,
            List<JigsawConnection> connections,
            Map<String, List<JigsawConnection>> topConnections,
            Map<String, List<JigsawConnection>> bottomConnections,
            Map<String, List<JigsawConnection>> horizontalConnections
    ) {
        Direction direction = Direction.from3DDataValue(nbt.getInt("direction"));
        JigsawConnection connection = new JigsawConnection(
                pos[0],
                pos[1],
                pos[2],
                direction,
                nbt.getString("pool"),
                nbt.getString("target"),
                nbt.getBoolean("roll"),
                nbt.getInt("selection"),
                nbt.getInt("placement")
        );
        connections.add(connection);

        String name = nbt.getString("name");
        Map<String, List<JigsawConnection>> map;
        if (direction == Direction.UP) {
            map = topConnections;
        } else if (direction == Direction.DOWN) {
            map = bottomConnections;
        } else {
            map = horizontalConnections;
        }
        map.computeIfAbsent(name, ignored -> new ArrayList<>()).add(connection);
    }

    private static PaletteEntry jigsawReplacement(CompoundTag nbt) {
        String block = nbt.getString("block");
        int meta = nbt.contains("meta", Tag.TAG_ANY_NUMERIC) ? nbt.getInt("meta") : 0;
        return new PaletteEntry(normalizeId(block), meta);
    }

    private static List<List<JigsawConnection>> groupFromConnections(List<JigsawConnection> connections) {
        if (connections.isEmpty()) {
            return List.of();
        }

        connections.sort((a, b) -> Integer.compare(b.selectionPriority, a.selectionPriority));
        List<List<JigsawConnection>> groups = new ArrayList<>();
        List<JigsawConnection> current = null;
        int currentPriority = Integer.MIN_VALUE;
        for (JigsawConnection connection : connections) {
            if (current == null || currentPriority != connection.selectionPriority) {
                current = new ArrayList<>();
                groups.add(current);
                currentPriority = connection.selectionPriority;
            }
            current.add(connection);
        }
        return groups;
    }

    private static List<List<JigsawConnection>> copyConnectionGroups(List<List<JigsawConnection>> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }

        List<List<JigsawConnection>> copy = new ArrayList<>(groups.size());
        for (List<JigsawConnection> group : groups) {
            copy.add(List.copyOf(group));
        }
        return List.copyOf(copy);
    }

    private static Map<String, List<JigsawConnection>> copyConnectionMap(Map<String, List<JigsawConnection>> map) {
        if (map.isEmpty()) {
            return Map.of();
        }

        Map<String, List<JigsawConnection>> copy = new HashMap<>();
        for (Map.Entry<String, List<JigsawConnection>> entry : map.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<BlockPos, LegacyStructureMultiblocks.Part> indexLegacyMultiblocks(List<PlacedBlock> blocks) {
        Map<BlockPos, LegacyStructureMultiblocks.Source> sources = new HashMap<>();
        for (PlacedBlock block : blocks) {
            BlockPos pos = new BlockPos(block.x, block.y, block.z);
            LegacyStructureMultiblocks.Source previous = sources.put(
                    pos,
                    new LegacyStructureMultiblocks.Source(block.name, block.meta)
            );
            if (previous != null) {
                throw new IllegalStateException("Legacy template has more than one block at " + pos);
            }
        }
        return LegacyStructureMultiblocks.index(sources);
    }

    private static Map<BlockPos, LegacyStructureHeavyDoors.Part> indexLegacyHeavyDoors(List<PlacedBlock> blocks) {
        Map<BlockPos, LegacyStructureHeavyDoors.Source> sources = new HashMap<>();
        for (PlacedBlock block : blocks) {
            BlockPos pos = new BlockPos(block.x, block.y, block.z);
            LegacyStructureHeavyDoors.Source previous = sources.put(
                    pos,
                    new LegacyStructureHeavyDoors.Source(block.name, block.meta)
            );
            if (previous != null) {
                throw new IllegalStateException("Legacy template has more than one block at " + pos);
            }
        }
        return LegacyStructureHeavyDoors.index(sources);
    }

    public static BlockState stateFromLegacyId(String name, int meta) {
        return stateFromLegacy(normalizeId(name), meta);
    }

    static BlockState stateFromLegacyId(String name, int meta, @Nullable CompoundTag nbt) {
        return stateFromLegacy(normalizeId(name), meta, nbt);
    }

    private static BlockState stateFromLegacy(String name, int meta, @Nullable CompoundTag nbt) {
        BlockState state = stateFromLegacy(name, meta);
        if (name.equals("minecraft:skull") && (meta & 7) == 1
                && nbt != null && nbt.contains("Rot", Tag.TAG_ANY_NUMERIC)
                && state.hasProperty(SkullBlock.ROTATION)) {
            int rotation = nbt.getInt("Rot");
            if (rotation < 0 || rotation > 15) {
                throw new IllegalStateException("Invalid legacy floor-skull rotation " + rotation);
            }
            return state.setValue(SkullBlock.ROTATION, rotation);
        }
        if (!name.equals("minecraft:flower_pot") || nbt == null) {
            return state;
        }

        // The only populated legacy flower pot shipped in the structures is
        // numeric item id 39, the 1.7.10 brown mushroom. Empty pots store 0.
        int item = nbt.getInt("Item");
        int data = nbt.getInt("Data");
        if (item == 0) {
            return state;
        }
        if (item == 39 && data == 0) {
            return Blocks.POTTED_BROWN_MUSHROOM.defaultBlockState();
        }
        throw new IllegalStateException("Unsupported legacy flower-pot item " + item + ":" + data);
    }

    static BlockState stateFromLegacy(String name, int meta) {
        BlockState legacyVanillaState = legacyVanillaState(name, meta);
        if (legacyVanillaState != null) {
            return legacyVanillaState;
        }

        BlockState flattenedState = legacyFlattenedHbmState(name, meta);
        if (flattenedState != null) {
            return flattenedState;
        }

        Block block = blockByName(name);
        BlockState state = block.defaultBlockState();
        if (block == Blocks.AIR) {
            return state;
        }

        // Legacy liquid metadata is the actual fluid level (0 = source,
        // 1..7 = flowing, 8..15 = falling). Preserve it verbatim instead of
        // letting the generic property conversion collapse every liquid to
        // its default source state.
        if (block instanceof LiquidBlock && state.hasProperty(LiquidBlock.LEVEL)) {
            return state.setValue(LiquidBlock.LEVEL, Mth.clamp(meta, 0, 15));
        }

        LegacyStructureMultiblocks.Kind multiblockKind = LegacyStructureMultiblocks.Kind.fromBlockId(name);
        if (multiblockKind != LegacyStructureMultiblocks.Kind.NONE && meta >= 12) {
            Direction facing = LegacyStructureMultiblocks.coreFacing(multiblockKind, meta);
            if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                throw new IllegalStateException("Modern " + multiblockKind + " structure core has no horizontal facing state");
            }
            return state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }

        if (block instanceof HbmHeavyDoorBlock && meta >= 12) {
            Direction facing = LegacyStructureHeavyDoors.coreFacing(meta);
            return state.setValue(HbmHeavyDoorBlock.FACING, facing);
        }

        if ((block instanceof com.reinhardt.hbm.block.DieselGeneratorBlock
                || name.equals("reinhardtshbm:machine_electric_furnace_off")
                || name.equals("reinhardtshbm:machine_electric_furnace_on"))
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = Direction.from3DDataValue(meta & 7);
            if (!facing.getAxis().isHorizontal()) {
                throw new IllegalStateException("Legacy machine " + name
                        + " has non-horizontal facing metadata " + meta);
            }
            return state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }

        if ((block instanceof ChargerBlock
                || block instanceof MicrowaveBlock
                || block instanceof SmallBoilerBlock)
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, requireLegacyHorizontal(meta));
        }

        if (block instanceof TrapDoorBlock) {
            return legacyTrapDoorState(state, meta);
        }

        if (block instanceof DeadPlantBlock) {
            DeadPlantBlock.Kind kind = switch (meta) {
                case 0 -> DeadPlantBlock.Kind.GENERIC;
                case 1 -> DeadPlantBlock.Kind.GRASS;
                case 2 -> DeadPlantBlock.Kind.FLOWER;
                case 3 -> DeadPlantBlock.Kind.BIG_FLOWER;
                case 4 -> DeadPlantBlock.Kind.FERN;
                default -> throw new IllegalStateException("Invalid legacy dead-plant metadata " + meta);
            };
            return state.setValue(DeadPlantBlock.KIND, kind);
        }

        if (block instanceof HbmAnvilBlock) {
            Direction facing = switch (meta) {
                case 2 -> Direction.NORTH;
                case 3 -> Direction.SOUTH;
                case 4 -> Direction.EAST;
                case 5 -> Direction.WEST;
                default -> throw new IllegalStateException("Invalid legacy HBM-anvil metadata " + meta);
            };
            return state.setValue(HbmAnvilBlock.FACING, facing);
        }

        if (block instanceof StorageCrateBlock) {
            // BlockStorageCrate accepted metadata 0 as its untouched worldgen
            // state. The old steel-crate texture had no horizontal front, and
            // both DesertAtom002 and Barrel deliberately placed crate_steel:0.
            // Player placement was the only path that rewrote it to 2-5.
            Direction facing = meta == 0 ? Direction.NORTH : requireLegacyHorizontal(meta);
            return state.setValue(StorageCrateBlock.FACING, facing);
        }

        if (block instanceof DungeonChainBlock) {
            boolean wall = meta >= 2 && meta <= 5;
            Direction facing = wall ? requireLegacyHorizontal(meta) : Direction.NORTH;
            return state
                    .setValue(DungeonChainBlock.WALL, wall)
                    .setValue(DungeonChainBlock.END, false)
                    .setValue(DungeonChainBlock.FACING, facing);
        }

        if (block instanceof WoodStructureBlock) {
            WoodStructureBlock.Type type = switch (meta) {
                case 0 -> WoodStructureBlock.Type.ROOF;
                case 1 -> WoodStructureBlock.Type.SCAFFOLD;
                case 2 -> WoodStructureBlock.Type.CEILING;
                default -> throw new IllegalStateException("Invalid legacy wood-structure metadata " + meta);
            };
            return state.setValue(WoodStructureBlock.TYPE, type);
        }

        if (block instanceof SteelGrateBlock) {
            if (meta < 0 || meta > 9) {
                throw new IllegalStateException("Invalid legacy steel-grate height metadata " + meta);
            }
            return state.setValue(SteelGrateBlock.HEIGHT, meta);
        }

        if (block instanceof CaveSpikeBlock) {
            if (meta < 0 || meta > 6) {
                throw new IllegalStateException("Invalid legacy cave-spike material metadata " + meta);
            }
            return state.setValue(CaveSpikeBlock.MATERIAL, meta);
        }

        if (block instanceof SuperConcreteBlock) {
            if (meta < 0 || meta > 15) {
                throw new IllegalStateException("Invalid legacy super-concrete damage metadata " + meta);
            }
            return state.setValue(SuperConcreteBlock.DAMAGE, meta);
        }

        if (block instanceof WoodBarrierBlock) {
            Direction facing = requireLegacyHorizontal(meta);
            return state
                    .setValue(WoodBarrierBlock.FACING, facing)
                    .setValue(WoodBarrierBlock.NORTH, facing == Direction.SOUTH)
                    .setValue(WoodBarrierBlock.EAST, facing == Direction.WEST)
                    .setValue(WoodBarrierBlock.SOUTH, facing == Direction.NORTH)
                    .setValue(WoodBarrierBlock.WEST, facing == Direction.EAST)
                    .setValue(WoodBarrierBlock.UP, false);
        }

        if (block instanceof com.reinhardt.hbm.block.PoleSatelliteReceiverBlock
                && state.hasProperty(com.reinhardt.hbm.block.PoleSatelliteReceiverBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.PoleSatelliteReceiverBlock.FACING,
                    requireLegacyHorizontal(meta).getOpposite());
        }

        if (block instanceof com.reinhardt.hbm.block.RadioRecBlock
                && state.hasProperty(com.reinhardt.hbm.block.RadioRecBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.RadioRecBlock.FACING,
                    requireLegacyHorizontal(meta).getOpposite());
        }

        if (block instanceof com.reinhardt.hbm.block.GeigerBlock
                && state.hasProperty(com.reinhardt.hbm.block.GeigerBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.GeigerBlock.FACING,
                    requireLegacyHorizontal(meta));
        }

        if (block instanceof com.reinhardt.hbm.block.StatueElbFBlock
                && state.hasProperty(com.reinhardt.hbm.block.StatueElbFBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.StatueElbFBlock.FACING,
                    requireLegacyHorizontal(meta));
        }

        if (block instanceof com.reinhardt.hbm.block.LegacyDisplayStandBlock stand
                && stand.kind() == com.reinhardt.hbm.block.LegacyDisplayStandBlock.Kind.SKELETON_HOLDER) {
            Direction facing = switch (meta & 7) {
                case 2 -> Direction.WEST;
                case 3 -> Direction.EAST;
                case 4 -> Direction.SOUTH;
                case 5 -> Direction.NORTH;
                default -> throw new IllegalStateException("Invalid legacy skeleton-holder metadata " + meta);
            };
            return state.setValue(com.reinhardt.hbm.block.LegacyDisplayStandBlock.FACING, facing);
        }

        if (block instanceof com.reinhardt.hbm.block.ToasterBlock) {
            int normalized = Math.floorMod(meta, 12);
            Direction facing = switch (normalized & 3) {
                case 0 -> Direction.SOUTH;
                case 1 -> Direction.WEST;
                case 2 -> Direction.NORTH;
                default -> Direction.EAST;
            };
            return state
                    .setValue(com.reinhardt.hbm.block.ToasterBlock.FACING, facing)
                    .setValue(com.reinhardt.hbm.block.ToasterBlock.VARIANT, normalized / 4);
        }

        if (block instanceof BobbleheadBlock && state.hasProperty(BobbleheadBlock.ROTATION)) {
            // BlockBobble stores its complete 16-step floor rotation directly
            // in metadata.  The block's rotate() method then applies the
            // structure's quarter turn, exactly matching transformMeta().
            return state.setValue(BobbleheadBlock.ROTATION, meta & 15);
        }

        if (block instanceof RailBlock && state.hasProperty(BlockStateProperties.RAIL_SHAPE)) {
            return state.setValue(BlockStateProperties.RAIL_SHAPE, legacyRailShape(meta));
        }

        if (block instanceof DoorBlock) {
            if ((meta & 8) != 0) {
                return state
                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.HINGE, (meta & 1) != 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            }
            return state
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(DoorBlock.FACING, HbmLegacyDoorBlock.facingFromOldMeta(meta))
                    .setValue(DoorBlock.OPEN, (meta & 4) != 0)
                    .setValue(DoorBlock.POWERED, false);
        }

        if (block == Blocks.LEVER && state.hasProperty(BlockStateProperties.ATTACH_FACE)
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return legacyLeverState(state, meta);
        }

        if (block instanceof RepeaterBlock && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, horizontalFromLegacy(meta & 3));
            if (state.hasProperty(RepeaterBlock.DELAY)) {
                state = state.setValue(RepeaterBlock.DELAY, ((meta >> 2) & 3) + 1);
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                state = state.setValue(BlockStateProperties.POWERED, false);
            }
            return state;
        }

        if (block instanceof DecoModelBlock && state.hasProperty(DecoModelBlock.FACING)) {
            return state.setValue(DecoModelBlock.FACING, DecoModelBlock.fromLegacyRotation(meta >> 2));
        }

        if (block instanceof com.reinhardt.hbm.block.FilingCabinetBlock && state.hasProperty(com.reinhardt.hbm.block.FilingCabinetBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.FilingCabinetBlock.FACING,
                            com.reinhardt.hbm.block.FilingCabinetBlock.fromLegacyRotation(meta >> 2))
                    .setValue(com.reinhardt.hbm.block.FilingCabinetBlock.MATERIAL, meta & 3);
        }

        if (block instanceof com.reinhardt.hbm.block.TapeRecorderBlock && state.hasProperty(com.reinhardt.hbm.block.TapeRecorderBlock.FACING)) {
            return state.setValue(com.reinhardt.hbm.block.TapeRecorderBlock.FACING,
                    com.reinhardt.hbm.block.TapeRecorderBlock.fromLegacyMeta(meta));
        }

        // 1.7.10 spotlights store the mounted face as side << 1 (with bit 0
        // reserved for the broken-light flag).  Do not feed that value into
        // the generic 0..5 facing conversion: metadata 8/10 are WEST/EAST,
        // not DOWN/NORTH.  This is especially visible in the oil-rig
        // template, whose elevated cage lamps use metadata 8 and 10 and
        // otherwise appear to float away from their supports.
        if (block instanceof SpotlightBlock && state.hasProperty(SpotlightBlock.FACING)) {
            Direction direction = Direction.from3DDataValue((meta >> 1) & 7);
            return state.setValue(SpotlightBlock.FACING, direction);
        }

        // Floodlights use the original 0..5 mount-face metadata, while
        // values 6..11 carry the same face plus the upper-state bit used by
        // the legacy renderer.  Preserve both parts explicitly so structure
        // placement matches the saved 1.7.10 orientation and flip state.
        if (block instanceof FloodlightBlock && state.hasProperty(FloodlightBlock.FACING)) {
            Direction direction = Direction.from3DDataValue(Math.floorMod(meta, 6));
            state = state.setValue(FloodlightBlock.FACING, direction);
            if (state.hasProperty(FloodlightBlock.FLIPPED)) {
                state = state.setValue(FloodlightBlock.FLIPPED, meta >= 6);
            }
            return state;
        }

        if (block instanceof DecoCrtBlock) {
            int normalized = Math.abs(meta) % 16;
            return state.setValue(DecoCrtBlock.FACING, DecoCrtBlock.fromLegacyFacing(normalized & 3))
                    .setValue(DecoCrtBlock.VARIANT, normalized / 4)
                    .setValue(DecoCrtBlock.LIT, meta >= 8);
        }

        if (name.startsWith("reinhardtshbm:barbed_wire") && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // RenderBarbedWire uses exactly two legacy metadata groups, not
            // four facings: meta < 4 rotates the OBJ by -90° around Y, while
            // meta >= 4 rotates it by -180°. The blockstate model maps WEST
            // to -90° and SOUTH to -180°, respectively.
            Direction direction = (meta & 7) < 4 ? Direction.WEST : Direction.SOUTH;
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        }

        if (name.equals("reinhardtshbm:fence_metal") && state.hasProperty(MetalFenceBlock.FORCE_POST)) {
            return state.setValue(MetalFenceBlock.FORCE_POST, (meta & 1) == 1)
                    .setValue(MetalFenceBlock.PILLAR, true);
        }

        if (block instanceof SteelWallBlock && state.hasProperty(SteelWallBlock.FACING)) {
            return state.setValue(SteelWallBlock.FACING, SteelWallBlock.fromLegacyMeta(meta));
        }

        if (block instanceof SteelPolesBlock && state.hasProperty(SteelPolesBlock.FACING)) {
            return state.setValue(SteelPolesBlock.FACING, SteelPolesBlock.fromLegacyMeta(meta));
        }

        if (block instanceof SteelBeamBlock && state.hasProperty(SteelBeamBlock.FACING)) {
            return state.setValue(SteelBeamBlock.FACING, SteelBeamBlock.fromLegacyMeta(meta));
        }

        if (block instanceof SlabBlock && state.hasProperty(SlabBlock.TYPE)) {
            SlabType type = (meta & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM;
            return applyMatchingNumericProperties(state.setValue(SlabBlock.TYPE, type), meta);
        }

        if (block instanceof StairBlock && state.hasProperty(StairBlock.FACING)) {
            Direction direction = switch (meta & 3) {
                case 0 -> Direction.EAST;
                case 1 -> Direction.WEST;
                case 2 -> Direction.SOUTH;
                default -> Direction.NORTH;
            };
            state = state.setValue(StairBlock.FACING, direction);
            if (state.hasProperty(StairBlock.HALF)) {
                state = state.setValue(StairBlock.HALF, (meta & 4) != 0
                        ? net.minecraft.world.level.block.state.properties.Half.TOP
                        : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
            }
            return state;
        }

        if (block instanceof RotatedPillarBlock && state.hasProperty(RotatedPillarBlock.AXIS)) {
            Direction.Axis axis = switch (meta & 12) {
                case 4 -> Direction.Axis.X;
                case 8 -> Direction.Axis.Z;
                default -> Direction.Axis.Y;
            };
            return state.setValue(RotatedPillarBlock.AXIS, axis);
        }

        if (block instanceof LadderBlock && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction direction = switch (meta & 7) {
                case 3 -> Direction.SOUTH;
                case 4 -> Direction.WEST;
                case 5 -> Direction.EAST;
                default -> Direction.NORTH;
            };
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction direction = switch (meta & 7) {
                case 0 -> Direction.DOWN;
                case 1 -> Direction.UP;
                case 2 -> Direction.NORTH;
                case 3 -> Direction.SOUTH;
                case 4 -> Direction.WEST;
                default -> Direction.EAST;
            };
            return state.setValue(BlockStateProperties.FACING, direction);
        }

        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, horizontalFromLegacy(meta & 3));
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, horizontalFromLegacy(meta & 3));
        }

        return applyMatchingNumericProperties(state, meta);
    }

    @Nullable
    private static BlockState legacyFlattenedHbmState(String name, int meta) {
        if (name.equals("reinhardtshbm:platemetal")) {
            Block block = switch (meta) {
                case 0 -> HbmBlocks.PLATEMETAL.get();
                case 1 -> HbmBlocks.PLATEMETAL_BLACK.get();
                case 2 -> HbmBlocks.PLATEMETAL_WHITE.get();
                case 3 -> HbmBlocks.PLATEMETAL_RED.get();
                case 4 -> HbmBlocks.PLATEMETAL_GREEN.get();
                case 5 -> HbmBlocks.PLATEMETAL_LIGHT_GRAY.get();
                case 6 -> HbmBlocks.PLATEMETAL_BLUE.get();
                case 7 -> HbmBlocks.PLATEMETAL_PURPLE.get();
                case 8 -> HbmBlocks.PLATEMETAL_CYAN.get();
                case 9 -> HbmBlocks.PLATEMETAL_PINK.get();
                case 10 -> HbmBlocks.PLATEMETAL_LIME.get();
                case 11 -> HbmBlocks.PLATEMETAL_YELLOW.get();
                case 12 -> HbmBlocks.PLATEMETAL_LIGHT_BLUE.get();
                case 13 -> HbmBlocks.PLATEMETAL_MAGENTA.get();
                case 14 -> HbmBlocks.PLATEMETAL_ORANGE.get();
                default -> throw new IllegalStateException("Invalid legacy platemetal metadata " + meta);
            };
            return block.defaultBlockState();
        }

        if (name.equals("reinhardtshbm:lightstone")) {
            Block block = switch (meta) {
                case 0 -> HbmBlocks.LIGHTSTONE.get();
                case 1 -> HbmBlocks.LIGHTSTONE_TILE.get();
                case 2 -> HbmBlocks.LIGHTSTONE_BRICKS.get();
                case 3 -> HbmBlocks.LIGHTSTONE_BRICKS_CHISELED.get();
                case 4 -> HbmBlocks.LIGHTSTONE_CHISELED.get();
                default -> throw new IllegalStateException("Invalid legacy lightstone metadata " + meta);
            };
            return block.defaultBlockState();
        }

        if (name.equals("reinhardtshbm:steel_scaffold")) {
            Block block = switch (meta & 3) {
                case 0 -> HbmBlocks.STEEL_SCAFFOLD.get();
                case 1 -> HbmBlocks.STEEL_SCAFFOLD_RED.get();
                case 2 -> HbmBlocks.STEEL_SCAFFOLD_WHITE.get();
                default -> HbmBlocks.STEEL_SCAFFOLD_YELLOW.get();
            };
            SteelScaffoldBlock.Orientation orientation = switch (meta & 12) {
                case 0 -> SteelScaffoldBlock.Orientation.HORIZONTAL_NS;
                case 4 -> SteelScaffoldBlock.Orientation.VERTICAL_NS;
                case 8 -> SteelScaffoldBlock.Orientation.HORIZONTAL_EW;
                case 12 -> SteelScaffoldBlock.Orientation.VERTICAL_EW;
                default -> throw new IllegalStateException("Invalid legacy steel-scaffold metadata " + meta);
            };
            return block.defaultBlockState().setValue(SteelScaffoldBlock.ORIENT, orientation);
        }
        return null;
    }

    private static RailShape legacyRailShape(int meta) {
        return switch (meta & 15) {
            case 1 -> RailShape.EAST_WEST;
            case 2 -> RailShape.ASCENDING_EAST;
            case 3 -> RailShape.ASCENDING_WEST;
            case 4 -> RailShape.ASCENDING_NORTH;
            case 5 -> RailShape.ASCENDING_SOUTH;
            case 6 -> RailShape.SOUTH_EAST;
            case 7 -> RailShape.SOUTH_WEST;
            case 8 -> RailShape.NORTH_WEST;
            case 9 -> RailShape.NORTH_EAST;
            default -> RailShape.NORTH_SOUTH;
        };
    }

    static BlockState pairedLegacyDoorState(BlockState state, int lowerMeta, int upperMeta, DoubleBlockHalf half) {
        return state
                .setValue(DoorBlock.HALF, half)
                .setValue(DoorBlock.FACING, HbmLegacyDoorBlock.facingFromOldMeta(lowerMeta))
                .setValue(DoorBlock.HINGE, (upperMeta & 1) != 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, (lowerMeta & 4) != 0)
                .setValue(DoorBlock.POWERED, false);
    }

    private static void linkLegacyDoorStates(List<PlacedBlock> blocks) {
        Map<BlockPos, Integer> indexes = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).state().getBlock() instanceof DoorBlock) {
                PlacedBlock block = blocks.get(i);
                indexes.put(new BlockPos(block.x(), block.y(), block.z()), i);
            }
        }

        for (int lowerIndex = 0; lowerIndex < blocks.size(); lowerIndex++) {
            PlacedBlock lower = blocks.get(lowerIndex);
            if (!(lower.state().getBlock() instanceof DoorBlock) || (lower.meta() & 8) != 0) {
                continue;
            }
            Integer upperIndex = indexes.get(new BlockPos(lower.x(), lower.y() + 1, lower.z()));
            if (upperIndex == null) {
                continue;
            }
            PlacedBlock upper = blocks.get(upperIndex);
            if (!upper.name().equals(lower.name()) || (upper.meta() & 8) == 0) {
                continue;
            }
            blocks.set(lowerIndex, new PlacedBlock(
                    lower.x(), lower.y(), lower.z(), lower.name(), lower.meta(),
                    pairedLegacyDoorState(lower.state(), lower.meta(), upper.meta(), DoubleBlockHalf.LOWER), lower.nbt()
            ));
            blocks.set(upperIndex, new PlacedBlock(
                    upper.x(), upper.y(), upper.z(), upper.name(), upper.meta(),
                    pairedLegacyDoorState(upper.state(), lower.meta(), upper.meta(), DoubleBlockHalf.UPPER), upper.nbt()
            ));
        }
    }

    @Nullable
    private static BlockState legacyVanillaState(String name, int meta) {
        return switch (name) {
            case "minecraft:bed" -> legacyBedState(meta);
            case "minecraft:brick_block" -> Blocks.BRICKS.defaultBlockState();
            case "minecraft:carpet" -> legacyCarpet(meta).defaultBlockState();
            case "minecraft:double_plant" -> legacyDoublePlantState(meta);
            case "minecraft:fence" -> Blocks.OAK_FENCE.defaultBlockState();
            case "minecraft:grass" -> Blocks.GRASS_BLOCK.defaultBlockState();
            case "minecraft:leaves" -> legacyLeavesState(meta);
            case "minecraft:log" -> legacyLogState(meta);
            case "minecraft:planks" -> legacyWoodPlanks(meta).defaultBlockState();
            case "minecraft:redstone_torch" -> legacyTorchState(meta, true);
            case "minecraft:unlit_redstone_torch" -> legacyTorchState(meta, false);
            case "minecraft:torch" -> legacyTorchState(meta, null);
            case "minecraft:sand" -> (meta & 1) == 1
                    ? Blocks.RED_SAND.defaultBlockState()
                    : Blocks.SAND.defaultBlockState();
            case "minecraft:snow_layer" -> Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, Mth.clamp((meta & 7) + 1, 1, 8));
            case "minecraft:stained_glass" -> legacyStainedGlass(meta).defaultBlockState();
            case "minecraft:stained_hardened_clay" -> legacyTerracotta(meta).defaultBlockState();
            case "minecraft:stonebrick" -> legacyStoneBrick(meta).defaultBlockState();
            case "minecraft:stone_stairs" -> legacyStairState(Blocks.COBBLESTONE_STAIRS, meta);
            case "minecraft:stone_slab" -> slabState(legacyStoneSlab(meta), (meta & 8) != 0);
            case "minecraft:double_stone_slab" -> legacyDoubleStoneSlab(meta).defaultBlockState();
            case "minecraft:wooden_slab" -> slabState(legacyWoodSlab(meta), (meta & 8) != 0);
            case "minecraft:double_wooden_slab" -> legacyWoodPlanks(meta).defaultBlockState();
            case "minecraft:stained_glass_pane" -> legacyStainedGlassPane(meta).defaultBlockState();
            case "minecraft:stone_button" -> legacyButtonState(meta);
            case "minecraft:tallgrass" -> legacyTallGrass(meta).defaultBlockState();
            case "minecraft:trapdoor" -> legacyTrapDoorState(Blocks.OAK_TRAPDOOR.defaultBlockState(), meta);
            case "minecraft:trapped_chest" -> legacyTrappedChestState(meta);
            case "minecraft:unpowered_comparator" -> legacyComparatorState(meta);
            case "minecraft:unpowered_repeater" -> legacyRepeaterState(meta);
            case "minecraft:vine" -> legacyVineState(meta);
            case "minecraft:wall_sign" -> legacyWallSignState(meta);
            case "minecraft:waterlily" -> Blocks.LILY_PAD.defaultBlockState();
            case "minecraft:wooden_door" -> Blocks.OAK_DOOR.defaultBlockState();
            case "minecraft:wooden_pressure_plate" -> Blocks.OAK_PRESSURE_PLATE.defaultBlockState()
                    .setValue(BlockStateProperties.POWERED, (meta & 1) != 0);
            case "minecraft:web" -> Blocks.COBWEB.defaultBlockState();
            case "minecraft:wool" -> legacyWool(meta).defaultBlockState();
            case "minecraft:skull" -> legacySkull(meta);
            default -> null;
        };
    }

    private static Direction requireLegacyHorizontal(int meta) {
        Direction direction = Direction.from3DDataValue(meta & 7);
        if (!direction.getAxis().isHorizontal()) {
            throw new IllegalStateException("Legacy metadata does not encode a horizontal direction: " + meta);
        }
        return direction;
    }

    private static BlockState legacyStairState(Block stairs, int meta) {
        BlockState state = stairs.defaultBlockState()
                .setValue(StairBlock.FACING, switch (meta & 3) {
                    case 0 -> Direction.EAST;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.SOUTH;
                    default -> Direction.NORTH;
                })
                .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }

    private static BlockState legacyTrappedChestState(int meta) {
        Direction facing = requireLegacyHorizontal(meta);
        return Blocks.TRAPPED_CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, facing)
                .setValue(ChestBlock.TYPE, ChestType.SINGLE)
                .setValue(ChestBlock.WATERLOGGED, false);
    }

    private static BlockState legacyDoublePlantState(int meta) {
        // Every double plant in the shipped templates is the old variant 2
        // (double tall grass). Upper metadata carries only the upper bit and
        // render-orientation bits; its species comes from the lower block.
        return Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF,
                        (meta & 8) != 0 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    private static BlockState legacyLeavesState(int meta) {
        Block leaves = switch (meta & 3) {
            case 1 -> Blocks.SPRUCE_LEAVES;
            case 2 -> Blocks.BIRCH_LEAVES;
            case 3 -> Blocks.JUNGLE_LEAVES;
            default -> Blocks.OAK_LEAVES;
        };
        return leaves.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, (meta & 4) != 0)
                .setValue(LeavesBlock.DISTANCE, 7);
    }

    /**
     * 1.7.10 torch metadata uses 1/2/3/4 for EAST/WEST/SOUTH/NORTH wall
     * attachment and 5 for a standing torch. A null lit value denotes the
     * ordinary non-redstone torch.
     */
    private static BlockState legacyTorchState(int meta, @Nullable Boolean lit) {
        int facing = meta & 7;
        boolean wall = facing >= 1 && facing <= 4;
        Block block;
        if (lit == null) {
            block = wall ? Blocks.WALL_TORCH : Blocks.TORCH;
        } else {
            block = wall ? Blocks.REDSTONE_WALL_TORCH : Blocks.REDSTONE_TORCH;
        }
        BlockState state = block.defaultBlockState();
        if (wall) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, switch (facing) {
                case 1 -> Direction.EAST;
                case 2 -> Direction.WEST;
                case 3 -> Direction.SOUTH;
                default -> Direction.NORTH;
            });
        }
        if (lit != null && state.hasProperty(BlockStateProperties.LIT)) {
            state = state.setValue(BlockStateProperties.LIT, lit);
        }
        return state;
    }

    private static BlockState legacyButtonState(int meta) {
        int orientation = meta & 7;
        BlockState state = Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.POWERED, (meta & 8) != 0);
        return switch (orientation) {
            case 0 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.CEILING)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            case 1 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
            case 2 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            case 3 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            case 4 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            default -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        };
    }

    private static BlockState legacyComparatorState(int meta) {
        return Blocks.COMPARATOR.defaultBlockState()
                .setValue(ComparatorBlock.FACING, horizontalFromLegacy(meta))
                .setValue(ComparatorBlock.MODE, (meta & 4) != 0 ? ComparatorMode.SUBTRACT : ComparatorMode.COMPARE)
                .setValue(ComparatorBlock.POWERED, false);
    }

    private static BlockState legacyRepeaterState(int meta) {
        return Blocks.REPEATER.defaultBlockState()
                .setValue(RepeaterBlock.FACING, horizontalFromLegacy(meta))
                .setValue(RepeaterBlock.DELAY, ((meta >> 2) & 3) + 1)
                .setValue(RepeaterBlock.POWERED, false);
    }

    private static BlockState legacyVineState(int meta) {
        return Blocks.VINE.defaultBlockState()
                .setValue(VineBlock.SOUTH, (meta & 1) != 0)
                .setValue(VineBlock.WEST, (meta & 2) != 0)
                .setValue(VineBlock.NORTH, (meta & 4) != 0)
                .setValue(VineBlock.EAST, (meta & 8) != 0)
                .setValue(VineBlock.UP, false);
    }

    private static BlockState legacyWallSignState(int meta) {
        Direction facing = switch (meta & 7) {
            case 2 -> Direction.NORTH;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> throw new IllegalStateException("Invalid legacy wall-sign metadata " + meta);
        };
        return Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.WATERLOGGED, false);
    }

    private static BlockState legacyBedState(int meta) {
        return Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, horizontalFromLegacy(meta))
                .setValue(BedBlock.PART, (meta & 8) != 0 ? BedPart.HEAD : BedPart.FOOT)
                .setValue(BedBlock.OCCUPIED, (meta & 4) != 0);
    }

    private static BlockState legacyTrapDoorState(BlockState state, int meta) {
        Direction facing = switch (meta & 3) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.NORTH;
            case 2 -> Direction.EAST;
            default -> Direction.WEST;
        };
        return state
                .setValue(TrapDoorBlock.FACING, facing)
                .setValue(TrapDoorBlock.HALF, (meta & 8) != 0 ? Half.TOP : Half.BOTTOM)
                .setValue(TrapDoorBlock.OPEN, (meta & 4) != 0)
                .setValue(TrapDoorBlock.POWERED, false)
                .setValue(TrapDoorBlock.WATERLOGGED, false);
    }

    private static BlockState legacyLogState(int meta) {
        Block block = switch (meta & 3) {
            case 1 -> (meta & 12) == 12 ? Blocks.SPRUCE_WOOD : Blocks.SPRUCE_LOG;
            case 2 -> (meta & 12) == 12 ? Blocks.BIRCH_WOOD : Blocks.BIRCH_LOG;
            case 3 -> (meta & 12) == 12 ? Blocks.JUNGLE_WOOD : Blocks.JUNGLE_LOG;
            default -> (meta & 12) == 12 ? Blocks.OAK_WOOD : Blocks.OAK_LOG;
        };
        BlockState state = block.defaultBlockState();
        if ((meta & 12) == 12 || !state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state;
        }
        Direction.Axis axis = switch (meta & 12) {
            case 4 -> Direction.Axis.X;
            case 8 -> Direction.Axis.Z;
            default -> Direction.Axis.Y;
        };
        return state.setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static Block legacyTallGrass(int meta) {
        return switch (meta & 3) {
            case 0 -> Blocks.DEAD_BUSH;
            case 2 -> Blocks.FERN;
            default -> Blocks.SHORT_GRASS;
        };
    }

    private static Block legacyCarpet(int meta) {
        return legacyColoredBlock(meta,
                Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET,
                Blocks.YELLOW_CARPET, Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET,
                Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
                Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET);
    }

    private static Block legacyStainedGlass(int meta) {
        return legacyColoredBlock(meta,
                Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
                Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
                Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
                Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS);
    }

    private static Block legacyTerracotta(int meta) {
        return legacyColoredBlock(meta,
                Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
                Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
                Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
                Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA);
    }

    private static Block legacyWool(int meta) {
        return legacyColoredBlock(meta,
                Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL,
                Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL, Blocks.GRAY_WOOL,
                Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
                Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL);
    }

    private static Block legacyColoredBlock(int meta, Block... colors) {
        return colors[meta & 15];
    }

    private static Block legacyStoneBrick(int meta) {
        return switch (meta & 3) {
            case 1 -> Blocks.MOSSY_STONE_BRICKS;
            case 2 -> Blocks.CRACKED_STONE_BRICKS;
            case 3 -> Blocks.CHISELED_STONE_BRICKS;
            default -> Blocks.STONE_BRICKS;
        };
    }

    private static Block legacyStoneSlab(int meta) {
        return switch (meta & 7) {
            case 1 -> Blocks.SANDSTONE_SLAB;
            case 2 -> Blocks.OAK_SLAB;
            case 3 -> Blocks.COBBLESTONE_SLAB;
            case 4 -> Blocks.BRICK_SLAB;
            case 5 -> Blocks.STONE_BRICK_SLAB;
            case 6 -> Blocks.NETHER_BRICK_SLAB;
            case 7 -> Blocks.QUARTZ_SLAB;
            default -> Blocks.SMOOTH_STONE_SLAB;
        };
    }

    private static Block legacyDoubleStoneSlab(int meta) {
        return switch (meta & 7) {
            case 1 -> Blocks.SANDSTONE;
            case 2 -> Blocks.OAK_PLANKS;
            case 3 -> Blocks.COBBLESTONE;
            case 4 -> Blocks.BRICKS;
            case 5 -> Blocks.STONE_BRICKS;
            case 6 -> Blocks.NETHER_BRICKS;
            case 7 -> Blocks.QUARTZ_BLOCK;
            default -> Blocks.SMOOTH_STONE;
        };
    }

    private static Block legacyWoodSlab(int meta) {
        return switch (meta & 7) {
            case 1 -> Blocks.SPRUCE_SLAB;
            case 2 -> Blocks.BIRCH_SLAB;
            case 3 -> Blocks.JUNGLE_SLAB;
            case 4 -> Blocks.ACACIA_SLAB;
            case 5 -> Blocks.DARK_OAK_SLAB;
            default -> Blocks.OAK_SLAB;
        };
    }

    private static Block legacyWoodPlanks(int meta) {
        return switch (meta & 7) {
            case 1 -> Blocks.SPRUCE_PLANKS;
            case 2 -> Blocks.BIRCH_PLANKS;
            case 3 -> Blocks.JUNGLE_PLANKS;
            case 4 -> Blocks.ACACIA_PLANKS;
            case 5 -> Blocks.DARK_OAK_PLANKS;
            default -> Blocks.OAK_PLANKS;
        };
    }

    private static Block legacyStainedGlassPane(int meta) {
        return switch (meta & 15) {
            case 1 -> Blocks.ORANGE_STAINED_GLASS_PANE;
            case 2 -> Blocks.MAGENTA_STAINED_GLASS_PANE;
            case 3 -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 4 -> Blocks.YELLOW_STAINED_GLASS_PANE;
            case 5 -> Blocks.LIME_STAINED_GLASS_PANE;
            case 6 -> Blocks.PINK_STAINED_GLASS_PANE;
            case 7 -> Blocks.GRAY_STAINED_GLASS_PANE;
            case 8 -> Blocks.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 9 -> Blocks.CYAN_STAINED_GLASS_PANE;
            case 10 -> Blocks.PURPLE_STAINED_GLASS_PANE;
            case 11 -> Blocks.BLUE_STAINED_GLASS_PANE;
            case 12 -> Blocks.BROWN_STAINED_GLASS_PANE;
            case 13 -> Blocks.GREEN_STAINED_GLASS_PANE;
            case 14 -> Blocks.RED_STAINED_GLASS_PANE;
            case 15 -> Blocks.BLACK_STAINED_GLASS_PANE;
            default -> Blocks.WHITE_STAINED_GLASS_PANE;
        };
    }

    private static BlockState slabState(Block block, boolean top) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(SlabBlock.TYPE)
                ? state.setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM)
                : state;
    }

    private static BlockState legacySkull(int meta) {
        if ((meta & 7) == 1) {
            return Blocks.SKELETON_SKULL.defaultBlockState();
        }
        BlockState state = Blocks.SKELETON_WALL_SKULL.defaultBlockState();
        Direction direction = switch (meta & 7) {
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> Direction.NORTH;
        };
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction)
                : state;
    }

    private static Direction horizontalFromLegacy(int meta) {
        return switch (meta & 3) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    /**
     * Maps the 1.7.10/1.12 BlockLever.EnumOrientation values directly to the
     * modern lever's face, horizontal-facing, and powered properties.
     */
    private static BlockState legacyLeverState(BlockState state, int meta) {
        BlockState oriented = switch (meta & 7) {
            case 0 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.CEILING)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            case 1 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
            case 2 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            case 3 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
            case 4 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            case 5 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            case 6 -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.CEILING)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            default -> state.setValue(BlockStateProperties.ATTACH_FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        };
        return oriented.hasProperty(BlockStateProperties.POWERED)
                ? oriented.setValue(BlockStateProperties.POWERED, (meta & 8) != 0)
                : oriented;
    }

    static int transformLegacyLeverMeta(int meta, int coordBaseMode) {
        if ((coordBaseMode & 3) == 0) {
            return meta;
        }
        if (meta <= 0 || meta >= 7) {
            if ((coordBaseMode & 3) == 1 || (coordBaseMode & 3) == 3) {
                return meta ^ 0b111;
            }
            return meta;
        }
        if (meta >= 5) {
            return ((coordBaseMode & 3) == 1 || (coordBaseMode & 3) == 3)
                    ? (meta + 1) % 2 + 5
                    : meta;
        }
        return switch (coordBaseMode & 3) {
            case 1 -> switch (meta) {
                case 1 -> 3;
                case 2 -> 4;
                case 3 -> 2;
                default -> 1;
            };
            case 2 -> switch (meta) {
                case 1 -> 2;
                case 2 -> 1;
                case 3 -> 4;
                default -> 3;
            };
            case 3 -> switch (meta) {
                case 1 -> 4;
                case 2 -> 3;
                case 3 -> 1;
                default -> 2;
            };
            default -> meta;
        };
    }

    private static BlockState applyMatchingNumericProperties(BlockState state, int meta) {
        StateDefinition<Block, BlockState> definition = state.getBlock().getStateDefinition();
        Property<?> metaProperty = definition.getProperty("meta");
        if (metaProperty != null) {
            return trySetProperty(state, metaProperty, Integer.toString(meta));
        }

        Property<?> variantProperty = definition.getProperty("variant");
        if (variantProperty != null) {
            return trySetProperty(state, variantProperty, Integer.toString(meta & 7));
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState trySetProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    private static Block blockByName(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null) {
            ReinhardtsHBM.LOGGER.error("Invalid block id in legacy HBM structure: {}", name);
            return Blocks.AIR;
        }
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.get(id);
        }
        if (ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            if (LOGGED_MISSING_BLOCKS.add(name)) {
                ReinhardtsHBM.LOGGER.error("Missing HBM block id while placing legacy structure: {}", name);
            }
            return Blocks.BARRIER;
        }
        return Blocks.AIR;
    }

    private static String normalizeId(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        String normalized;
        if (lower.startsWith("hbm:tile.")) {
            normalized = "reinhardtshbm:" + lower.substring("hbm:tile.".length());
        } else if (lower.startsWith("hbm:item.")) {
            normalized = "reinhardtshbm:" + lower.substring("hbm:item.".length());
        } else if (lower.startsWith("hbm:")) {
            normalized = "reinhardtshbm:" + lower.substring("hbm:".length());
        } else {
            normalized = lower;
        }
        return LEGACY_ID_ALIASES.getOrDefault(normalized, normalized);
    }

    private static String normalizeItemId(String name) {
        String normalized = normalizeId(name);
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) {
            return normalized;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static int[] readIntList(ListTag tag, int expectedSize) {
        int[] result = new int[expectedSize];
        for (int i = 0; i < expectedSize && i < tag.size(); i++) {
            result[i] = tag.getInt(i);
        }
        return result;
    }

    private int rotateX(int x, int z, int rotation) {
        return rotateX(x, z, rotation, sizeX, sizeZ);
    }

    private int rotateZ(int x, int z, int rotation) {
        return rotateZ(x, z, rotation, sizeX, sizeZ);
    }

    public static int rotateX(int x, int z, int rotation, int sizeX, int sizeZ) {
        return switch (rotation & 3) {
            case 1 -> sizeZ - 1 - z;
            case 2 -> sizeX - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    public static int rotateZ(int x, int z, int rotation, int sizeX, int sizeZ) {
        return switch (rotation & 3) {
            case 1 -> x;
            case 2 -> sizeZ - 1 - z;
            case 3 -> sizeX - 1 - x;
            default -> z;
        };
    }

    private static boolean swapsAxes(int rotation) {
        return (rotation & 3) == 1 || (rotation & 3) == 3;
    }

    private static net.minecraft.world.level.block.Rotation toMcRotation(int rotation) {
        return switch (rotation & 3) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    private record PaletteEntry(String name, int meta) {
    }

    public record JigsawConnection(
            int x,
            int y,
            int z,
            Direction direction,
            String poolName,
            String targetName,
            boolean rollable,
            int selectionPriority,
            int placementPriority
    ) {
    }

    @FunctionalInterface
    interface BlockReplacement {
        BlockState replace(String blockName, int meta, BlockState state, RandomSource random);
    }

    private record PlacedBlock(int x, int y, int z, String name, int meta, BlockState state, @Nullable CompoundTag nbt) {
    }
}
