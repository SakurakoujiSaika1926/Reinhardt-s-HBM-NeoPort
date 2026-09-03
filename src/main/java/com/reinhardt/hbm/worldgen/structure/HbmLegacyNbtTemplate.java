package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.MetalFenceBlock;
import com.reinhardt.hbm.block.SteelWallBlock;
import com.reinhardt.hbm.block.SteelPolesBlock;
import com.reinhardt.hbm.block.DecoModelBlock;
import com.reinhardt.hbm.block.DecoCrtBlock;
import com.reinhardt.hbm.block.HbmLegacyDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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

    private final String name;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<PlacedBlock> blocks;
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
        for (PlacedBlock block : blocks) {
            int x = rotateX(block.x, block.z, rotation) + pieceBox.minX();
            int z = rotateZ(block.x, block.z, rotation) + pieceBox.minZ();
            int yBase = conformToTerrain ? level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + heightOffset : pieceBox.minY();
            int y = yBase + block.y;
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            if (!chunkBox.isInside(pos)) {
                continue;
            }

            BlockState state = block.state;
            if (replacement != null && random != null) {
                state = replacement.replace(block.name, block.meta, state, random);
            }
            // Legacy NBTStructure transforms rail coordinates but leaves RailGeneric metadata unchanged.
            if (!(state.getBlock() instanceof RailBlock)) {
                state = state.rotate(toMcRotation(rotation));
            }
            level.setBlock(pos, state, 2);
            if (block.nbt != null) {
                loadBlockEntity(level, pos, state, block.nbt, rotation);
            }
            refreshPaneConnections(level, pos);
        }
    }

    private static void refreshPaneConnections(WorldGenLevel level, BlockPos changedPos) {
        refreshPane(level, changedPos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            refreshPane(level, changedPos.relative(direction));
        }
    }

    private static void refreshPane(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IronBarsBlock)) {
            return;
        }
        BlockState connected = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            connected = connected.updateShape(direction, level.getBlockState(neighborPos), level, pos, neighborPos);
        }
        if (connected != state) {
            level.setBlock(pos, connected, 2);
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
                CompoundTag nbt = block.contains("nbt", Tag.TAG_COMPOUND) ? cleanBlockEntityNbt(block.getCompound("nbt"), itemPalette) : null;
                if (isWandJigsaw(entry, nbt)) {
                    registerJigsawConnection(nbt, pos, connections, topConnections, bottomConnections, horizontalConnections);
                    entry = jigsawReplacement(nbt);
                    nbt = null;
                } else if (entry.name.equals("reinhardtshbm:wand_air")) {
                    entry = new PaletteEntry("minecraft:air", 0);
                    nbt = null;
                }

                BlockState state = stateFromLegacy(entry.name, entry.meta);
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

    private static CompoundTag cleanBlockEntityNbt(CompoundTag tag, Map<Short, String> itemPalette) {
        CompoundTag copy = tag.copy();
        cleanLegacyNbt(copy, itemPalette);
        return copy;
    }

    private static void cleanLegacyNbt(Tag tag, Map<Short, String> itemPalette) {
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("block", Tag.TAG_STRING)) {
                compound.putString("block", normalizeId(compound.getString("block")));
            }

            if (looksLikeItemStack(compound)) {
                fixItemStack(compound, itemPalette);
            }

            for (String key : List.copyOf(compound.getAllKeys())) {
                cleanLegacyNbt(compound.get(key), itemPalette);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                cleanLegacyNbt(list.get(i), itemPalette);
            }
        }
    }

    private static boolean looksLikeItemStack(CompoundTag tag) {
        return tag.contains("id")
                && (tag.contains("Count") || tag.contains("count") || tag.contains("Slot") || tag.contains("slot") || tag.contains("Damage"));
    }

    private static void fixItemStack(CompoundTag tag, Map<Short, String> itemPalette) {
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
                }
            }
        }

        if (tag.contains("Count", Tag.TAG_BYTE) && !tag.contains("count")) {
            tag.putInt("count", Math.max(1, tag.getByte("Count") & 255));
        }
    }

    private static void loadBlockEntity(WorldGenLevel level, BlockPos pos, BlockState state, CompoundTag nbt, int rotation) {
        if (!state.hasBlockEntity()) {
            return;
        }

        CompoundTag copy = nbt.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            try {
                blockEntity.loadWithComponents(copy, level.registryAccess());
                blockEntity.setChanged();
            } catch (RuntimeException exception) {
                ReinhardtsHBM.LOGGER.debug("Failed to load block entity NBT for structure block {} at {}", state, pos, exception);
            }
        }
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

    public static BlockState stateFromLegacyId(String name, int meta) {
        return stateFromLegacy(normalizeId(name), meta);
    }

    static BlockState stateFromLegacy(String name, int meta) {
        BlockState legacyVanillaState = legacyVanillaState(name, meta);
        if (legacyVanillaState != null) {
            return legacyVanillaState;
        }

        Block block = blockByName(name);
        BlockState state = block.defaultBlockState();
        if (block == Blocks.AIR) {
            return state;
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

        if (block instanceof DecoCrtBlock) {
            int normalized = Math.abs(meta) % 16;
            return state.setValue(DecoCrtBlock.FACING, DecoCrtBlock.fromLegacyFacing(normalized & 3))
                    .setValue(DecoCrtBlock.VARIANT, normalized / 4)
                    .setValue(DecoCrtBlock.LIT, meta >= 8);
        }

        if (name.startsWith("reinhardtshbm:barbed_wire") && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction direction = switch (meta & 7) {
                case 3 -> Direction.SOUTH;
                case 4 -> Direction.WEST;
                case 5 -> Direction.EAST;
                default -> Direction.NORTH;
            };
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
            case "minecraft:brick_block" -> Blocks.BRICKS.defaultBlockState();
            case "minecraft:stonebrick" -> legacyStoneBrick(meta).defaultBlockState();
            case "minecraft:stone_slab" -> slabState(legacyStoneSlab(meta), (meta & 8) != 0);
            case "minecraft:double_stone_slab" -> legacyDoubleStoneSlab(meta).defaultBlockState();
            case "minecraft:wooden_slab" -> slabState(legacyWoodSlab(meta), (meta & 8) != 0);
            case "minecraft:double_wooden_slab" -> legacyWoodPlanks(meta).defaultBlockState();
            case "minecraft:stained_glass_pane" -> legacyStainedGlassPane(meta).defaultBlockState();
            case "minecraft:web" -> Blocks.COBWEB.defaultBlockState();
            case "minecraft:skull" -> legacySkull(meta);
            default -> null;
        };
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
        if (lower.startsWith("hbm:tile.")) {
            return "reinhardtshbm:" + lower.substring("hbm:tile.".length());
        }
        if (lower.startsWith("hbm:")) {
            return "reinhardtshbm:" + lower.substring("hbm:".length());
        }
        return lower;
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
