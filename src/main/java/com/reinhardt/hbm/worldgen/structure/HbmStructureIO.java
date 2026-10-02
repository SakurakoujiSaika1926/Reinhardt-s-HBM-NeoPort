package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.block.MetalFenceBlock;
import com.reinhardt.hbm.block.SteelWallBlock;
import com.reinhardt.hbm.block.SteelPolesBlock;
import com.reinhardt.hbm.block.SteelBeamBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HbmStructureIO {
    private static final Map<String, String> LEGACY_ID_ALIASES = Map.of(
            "reinhardtshbm:brick_concrete_slab", "reinhardtshbm:concrete_brick_slab",
            "reinhardtshbm:ore_coal_oil", "minecraft:coal_ore",
            "reinhardtshbm:ore_coal_oil_burning", "minecraft:coal_ore",
            "reinhardtshbm:#undef", "reinhardtshbm:statue_elb_f",
            "reinhardtshbm:hev_battery", "reinhardtshbm:hev_battery_block",
            "reinhardtshbm:machine_battery", "reinhardtshbm:capacitor_copper",
            "reinhardtshbm:block_copper", "minecraft:copper_block",
            "reinhardtshbm:machine_weapon_table", "minecraft:air",
            "reinhardtshbm:barrel_iron", "minecraft:air",
            "reinhardtshbm:ladder_tungsten", "minecraft:air"
    );

    private HbmStructureIO() {
    }

    public record LegacyBlockKey(String id, int meta) {
        public LegacyBlockKey {
            id = normalizeId(id);
        }
    }

    public record StructureLoadResult(boolean loaded, int sizeX, int sizeY, int sizeZ) {
        private static StructureLoadResult failed() {
            return new StructureLoadResult(false, 0, 0, 0);
        }
    }

    public static Path structureDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("structures");
    }

    public static Path structureDirectory(ServerLevel level) {
        return structureDirectory();
    }

    public static Path structureFile(ServerLevel level, String name) {
        String cleanName = name == null ? "" : name.replace('\\', '/');
        while (cleanName.startsWith("/")) {
            cleanName = cleanName.substring(1);
        }
        if (!cleanName.endsWith(".nbt")) {
            cleanName += ".nbt";
        }
        return structureDirectory(level).resolve(cleanName).normalize();
    }

    public static boolean saveArea(ServerLevel level, String name, BlockPos from, BlockPos to, Set<LegacyBlockKey> excluded) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            Files.createDirectories(structureDirectory(level));
            CompoundTag root = buildStructureNbt(level, from, to, excluded);
            try (OutputStream stream = Files.newOutputStream(structureFile(level, name))) {
                NbtIo.writeCompressed(root, stream);
            }
            return true;
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to save HBM structure {}", name, exception);
            return false;
        }
    }

    public static StructureLoadResult loadArea(ServerLevel level, String name, BlockPos origin, int rotation, boolean debug) {
        Path file = structureFile(level, name);
        try (InputStream stream = Files.newInputStream(file)) {
            CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            int[] size = readIntList(root.getList("size", Tag.TAG_INT), 3);
            placeStructure(level, root, origin, rotation, debug, name);
            return new StructureLoadResult(true, size[0], size[1], size[2]);
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to load HBM structure {}", name, exception);
            return StructureLoadResult.failed();
        } catch (RuntimeException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to place HBM structure {}", name, exception);
            return StructureLoadResult.failed();
        }
    }

    public static CompoundTag loadResourceStructure(String name) throws IOException {
        String path = "data/" + ReinhardtsHBM.MOD_ID + "/structure/" + name + ".nbt";
        try (InputStream stream = HbmStructureIO.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing resource structure " + path);
            }
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        }
    }

    public static boolean placeResourceStructure(ServerLevel level, String name, BlockPos origin, int rotation, boolean debug) {
        try {
            placeStructure(level, loadResourceStructure(name), origin, rotation, debug, name);
            return true;
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to place resource HBM structure {}", name, exception);
            return false;
        } catch (RuntimeException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to place resource HBM structure {}", name, exception);
            return false;
        }
    }

    public static void placeStructure(ServerLevel level, CompoundTag root, BlockPos origin, int rotation, boolean debug, String structureName) {
        int[] size = readIntList(root.getList("size", Tag.TAG_INT), 3);
        PaletteEntry[] palette = readPalette(root.getList("palette", Tag.TAG_COMPOUND), debug);
        Map<Short, String> itemPalette = readItemPalette(root);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        HolderLookup.Provider registries = level.registryAccess();
        List<PreparedPlacement> placements = new ArrayList<>(blocks.size());

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.length) {
                continue;
            }

            int[] local = readIntList(blockTag.getList("pos", Tag.TAG_INT), 3);
            PaletteEntry entry = palette[stateIndex];
            CompoundTag nbt = blockTag.contains("nbt", Tag.TAG_COMPOUND)
                    ? cleanBlockEntityNbt(entry.id(), blockTag.getCompound("nbt"), itemPalette)
                    : null;
            Placement placement = transformPlacement(entry, nbt, rotation, debug, structureName, level.getRandom());
            BlockPos localPos = new BlockPos(local[0], local[1], local[2]);
            BlockPos pos = origin.offset(
                    HbmLegacyNbtTemplate.rotateX(local[0], local[2], rotation, size[0], size[2]),
                    local[1],
                    HbmLegacyNbtTemplate.rotateZ(local[0], local[2], rotation, size[0], size[2])
            );
            placements.add(new PreparedPlacement(localPos, pos, placement.entry(), placement.nbt()));
        }

        Map<BlockPos, PreparedPlacement> placementsByPos = new HashMap<>();
        Map<BlockPos, PreparedPlacement> placementsByLocalPos = new HashMap<>();
        Map<BlockPos, LegacyStructureMultiblocks.Source> multiblockSources = new HashMap<>();
        Map<BlockPos, LegacyStructureHeavyDoors.Source> heavyDoorSources = new HashMap<>();
        List<PreparedPlacement> uniquePlacements = new ArrayList<>(placements.size());
        for (PreparedPlacement placement : placements) {
            if (placementsByPos.put(placement.pos(), placement) != null) {
                ReinhardtsHBM.LOGGER.error(
                        "Skipping duplicate block while placing legacy HBM structure {} at {}",
                        structureName,
                        placement.pos()
                );
                continue;
            }
            if (placementsByLocalPos.put(placement.localPos(), placement) != null) {
                ReinhardtsHBM.LOGGER.error(
                        "Skipping duplicate local block while placing legacy HBM structure {} at {}",
                        structureName,
                        placement.localPos()
                );
                placementsByPos.remove(placement.pos());
                continue;
            }
            uniquePlacements.add(placement);
            multiblockSources.put(
                    placement.localPos(),
                    new LegacyStructureMultiblocks.Source(placement.entry().id(), placement.entry().meta())
            );
            heavyDoorSources.put(
                    placement.localPos(),
                    new LegacyStructureHeavyDoors.Source(placement.entry().id(), placement.entry().meta())
            );
        }
        Map<BlockPos, LegacyStructureMultiblocks.Part> multiblockParts;
        Map<BlockPos, LegacyStructureHeavyDoors.Part> heavyDoorParts;
        try {
            multiblockParts = LegacyStructureMultiblocks.index(multiblockSources);
            heavyDoorParts = LegacyStructureHeavyDoors.index(heavyDoorSources);
        } catch (RuntimeException exception) {
            ReinhardtsHBM.LOGGER.error(
                    "Skipping legacy HBM structure {} because its multiblock/heavy-door topology is invalid",
                    structureName,
                    exception
            );
            return;
        }

        for (PreparedPlacement placement : uniquePlacements) {
            LegacyStructureMultiblocks.Part multiblockPart = multiblockParts.get(placement.localPos());
            LegacyStructureHeavyDoors.Part heavyDoorPart = heavyDoorParts.get(placement.localPos());
            boolean isLegacyDummy = multiblockPart != null
                    && !multiblockPart.isCoreAt(placement.localPos());
            boolean isLegacyHeavyDoorPart = heavyDoorPart != null
                    && !heavyDoorPart.isCoreAt(placement.localPos());
            BlockState state;
            if (isLegacyHeavyDoorPart) {
                state = HbmBlocks.HEAVY_DOOR_PART.get().defaultBlockState()
                        .setValue(HbmHeavyDoorPartBlock.EXTRA, false);
            } else if (isLegacyDummy) {
                state = HbmBlocks.MACHINE_DUMMY.get().defaultBlockState();
            } else {
                state = HbmLegacyNbtTemplate.stateFromLegacyId(
                        placement.entry().id(), placement.entry().meta(), placement.nbt()
                );
                if (multiblockPart != null) {
                    state = LegacyStructureMultiblocks.migrateCoreState(multiblockPart, state);
                }
                if (state.getBlock() instanceof DoorBlock) {
                    PreparedPlacement lower = (placement.entry().meta() & 8) != 0
                            ? placementsByPos.get(placement.pos().below())
                            : placement;
                    PreparedPlacement upper = lower == null ? null : placementsByPos.get(lower.pos().above());
                    if (lower != null
                            && upper != null
                            && lower.entry().id().equals(upper.entry().id())
                            && (lower.entry().meta() & 8) == 0
                            && (upper.entry().meta() & 8) != 0) {
                        DoubleBlockHalf half = placement == upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
                        state = HbmLegacyNbtTemplate.pairedLegacyDoorState(
                                state, lower.entry().meta(), upper.entry().meta(), half
                        );
                    }
                }
                // Legacy NBTStructure transforms rail coordinates but leaves
                // RailGeneric metadata unchanged. Levers use the old complete
                // EnumOrientation metadata transform, including face flips.
                if (state.getBlock() == Blocks.LEVER) {
                    state = HbmLegacyNbtTemplate.stateFromLegacyId(
                            placement.entry().id(),
                            HbmLegacyNbtTemplate.transformLegacyLeverMeta(placement.entry().meta(), rotation)
                    );
                } else if (!(state.getBlock() instanceof RailBlock)) {
                    state = state.rotate(toMcRotation(rotation));
                }
            }

            // Legacy NBTStructure always placed blocks with flag 2. UPDATE_ALL
            // causes neighbor updates during a bulk structure pass and can
            // make falling/attached blocks turn into drops immediately.
            setStructureBlock(level, placement.pos(), state, Block.UPDATE_CLIENTS);
            if (isLegacyHeavyDoorPart) {
                PreparedPlacement core = placementsByLocalPos.get(heavyDoorPart.corePos());
                if (core == null) {
                    ReinhardtsHBM.LOGGER.error(
                            "Skipping legacy heavy-door part link in {} at {} because local core {} is absent",
                            structureName, placement.pos(), heavyDoorPart.corePos()
                    );
                } else {
                    Direction worldFacing = toMcRotation(rotation).rotate(heavyDoorPart.coreFacing());
                    BlockPos localOffset = HbmHeavyDoorBlock.worldOffsets(
                            heavyDoorPart.decl(), worldFacing, core.pos()
                    ).get(placement.pos());
                    if (localOffset == null) {
                        ReinhardtsHBM.LOGGER.error(
                                "Skipping legacy heavy-door part link in {} at {} because it is outside the rotated footprint of {}",
                                structureName, placement.pos(), core.pos()
                        );
                    } else {
                        linkStructureHeavyDoorPart(level, placement.pos(), core.pos(), localOffset);
                    }
                }
            } else if (isLegacyDummy) {
                PreparedPlacement core = placementsByLocalPos.get(multiblockPart.corePos());
                if (core == null) {
                    ReinhardtsHBM.LOGGER.error(
                            "Skipping legacy structure dummy link in {} at {} because local core {} is absent",
                            structureName, placement.pos(), multiblockPart.corePos()
                    );
                } else {
                    linkStructureDummy(level, placement.pos(), core.pos());
                }
            } else {
                CompoundTag nbt = placement.nbt();
                if (multiblockPart != null) {
                    if (nbt == null) {
                        ReinhardtsHBM.LOGGER.error(
                                "Skipping legacy {} core NBT load in structure {} at {} because the saved core has no block entity NBT",
                                multiblockPart.kind(),
                                structureName,
                                placement.pos()
                        );
                        refreshPaneConnections(level, placement.pos());
                        continue;
                    }
                    try {
                        nbt = LegacyStructureMultiblocks.migrateCoreNbt(multiblockPart.kind(), nbt);
                    } catch (RuntimeException exception) {
                        ReinhardtsHBM.LOGGER.error(
                                "Skipping legacy {} core NBT load in structure {} at {} because migration failed",
                                multiblockPart.kind(),
                                structureName,
                                placement.pos(),
                                exception
                        );
                        refreshPaneConnections(level, placement.pos());
                        continue;
                    }
                }
                if (heavyDoorPart != null) {
                    if (nbt == null) {
                        ReinhardtsHBM.LOGGER.error(
                                "Skipping legacy {} core NBT load in structure {} at {} because the saved core has no block entity NBT",
                                heavyDoorPart.decl(), structureName, placement.pos()
                        );
                        refreshPaneConnections(level, placement.pos());
                        continue;
                    }
                    nbt = LegacyStructureHeavyDoors.migrateCoreNbt(nbt);
                }
                if (nbt != null) {
                    if (!state.hasBlockEntity()) {
                        if (multiblockPart != null || heavyDoorPart != null) {
                            ReinhardtsHBM.LOGGER.error(
                                    "Skipping strict legacy core NBT load in structure {} at {} because {} has no block entity state",
                                    structureName,
                                    placement.pos(),
                                    state
                            );
                        }
                    } else {
                        loadBlockEntity(level, placement.pos(), state, nbt, registries);
                    }
                }
            }
            refreshPaneConnections(level, placement.pos());
        }
    }

    private static void refreshPaneConnections(ServerLevel level, BlockPos changedPos) {
        HbmLegacyNbtTemplate.refreshPaneConnections(level, changedPos);
    }

    public static CompoundTag buildStructureNbt(ServerLevel level, BlockPos from, BlockPos to, Set<LegacyBlockKey> excluded) {
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );

        CompoundTag root = new CompoundTag();
        root.putInt("version", 1);
        ListTag blocks = new ListTag();
        ListTag paletteTag = new ListTag();
        ListTag itemPaletteTag = new ListTag();
        Map<LegacyBlockKey, Integer> palette = new HashMap<>();
        Map<String, Short> itemPalette = new HashMap<>();
        HolderLookup.Provider registries = level.registryAccess();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    LegacyBlockKey key = keyFromState(state);
                    if (excluded.contains(key)) {
                        continue;
                    }
                    if (key.id().equals(ReinhardtsHBM.id("wand_air").toString())) {
                        key = new LegacyBlockKey("minecraft:air", 0);
                    }
                    int paletteId = palette.computeIfAbsent(key, k -> {
                        int id = palette.size();
                        paletteTag.add(writePaletteEntry(k));
                        return id;
                    });
                    CompoundTag blockTag = new CompoundTag();
                    blockTag.putInt("state", paletteId);
                    blockTag.put("pos", intList(x - min.getX(), y - min.getY(), z - min.getZ()));

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        CompoundTag beTag = blockEntity.saveWithoutMetadata(registries);
                        beTag.remove("x");
                        beTag.remove("y");
                        beTag.remove("z");
                        collectItemPalette(beTag, itemPalette, itemPaletteTag);
                        blockTag.put("nbt", beTag);
                    }
                    blocks.add(blockTag);
                }
            }
        }

        root.put("blocks", blocks);
        root.put("palette", paletteTag);
        root.put("itemPalette", itemPaletteTag);
        root.put("size", intList(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
        root.put("entities", new ListTag());
        return root;
    }

    public static LegacyBlockKey keyFromState(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        int meta = legacyMeta(state);
        return new LegacyBlockKey(id.toString(), meta);
    }

    public static LegacyBlockKey keyFromBlock(Block block, int meta) {
        return new LegacyBlockKey(BuiltInRegistries.BLOCK.getKey(block).toString(), meta);
    }

    public static boolean isStructureBlock(Block block) {
        return block == HbmBlocks.WAND_AIR.get()
                || block == HbmBlocks.WAND_STRUCTURE.get()
                || block == HbmBlocks.WAND_JIGSAW.get()
                || block == HbmBlocks.WAND_LOGIC.get()
                || block == HbmBlocks.WAND_LOOT.get()
                || block == HbmBlocks.WAND_TANDEM.get();
    }

    private static PaletteEntry[] readPalette(ListTag paletteTag, boolean debug) {
        PaletteEntry[] palette = new PaletteEntry[paletteTag.size()];
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag tag = paletteTag.getCompound(i);
            String id = normalizeId(tag.getString("Name"));
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
            if (debug && id.equals("minecraft:air")) {
                id = ReinhardtsHBM.id("wand_air").toString();
            }
            palette[i] = new PaletteEntry(id, meta);
        }
        return palette;
    }

    private static Placement transformPlacement(
            PaletteEntry entry,
            CompoundTag nbt,
            int rotation,
            boolean debug,
            String structureName,
            RandomSource random
    ) {
        if (entry.id().equals(ReinhardtsHBM.id("floodlight").toString()) && nbt != null) {
            return new Placement(entry, HbmLegacyNbtTemplate.migrateLegacyFloodlightNbt(nbt));
        }
        if (entry.id().equals(ReinhardtsHBM.id("bobblehead").toString()) && nbt != null) {
            CompoundTag copy = nbt.copy();
            copy.putByte("type", (byte) (random.nextInt(BobbleheadType.values().length - 1) + 1));
            return new Placement(entry, copy);
        }
        if (entry.id().equals(ReinhardtsHBM.id("wand_jigsaw").toString()) && nbt != null && !debug) {
            return new Placement(new PaletteEntry(normalizeId(nbt.getString("block")), nbt.getInt("meta")), null);
        }
        if (entry.id().equals(ReinhardtsHBM.id("wand_loot").toString()) && nbt != null) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("trigger", !debug);
            copy.putFloat("rot", Mth.wrapDegrees(copy.getFloat("rot") + (rotation & 3) * 90.0F));
            return new Placement(entry, copy);
        }
        if (entry.id().equals(ReinhardtsHBM.id("wand_logic").toString()) && nbt != null) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("trigger", !debug);
            return new Placement(entry, copy);
        }
        if (entry.id().equals(ReinhardtsHBM.id("wand_tandem").toString()) && nbt != null) {
            CompoundTag copy = nbt.copy();
            copy.putBoolean("isArmed", !debug);
            if (structureName != null && !structureName.isBlank()) {
                copy.putString("structure", structureName);
            }
            return new Placement(entry, copy);
        }
        if (entry.id().equals(ReinhardtsHBM.id("wand_air").toString()) && !debug) {
            return new Placement(new PaletteEntry("minecraft:air", 0), null);
        }
        return new Placement(entry, nbt);
    }

    private static void loadBlockEntity(ServerLevel level, BlockPos pos, BlockState state, CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag copy = nbt.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            ReinhardtsHBM.LOGGER.error(
                    "Skipping structure block entity NBT for {} at {} because no block entity was created",
                    state,
                    pos
            );
            return;
        }
        try {
            blockEntity.loadWithComponents(copy, registries);
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        } catch (RuntimeException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to load structure block entity NBT for {} at {}", state, pos, exception);
        }
    }

    private static void setStructureBlock(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        if (state.getBlock() instanceof LegacyTurretBlock) {
            LegacyTurretBlock.runWithoutAutomaticDummies(() -> level.setBlock(pos, state, flags));
            return;
        }
        level.setBlock(pos, state, flags);
    }

    private static void linkStructureDummy(ServerLevel level, BlockPos pos, BlockPos corePos) {
        if (!(level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy)) {
            ReinhardtsHBM.LOGGER.error(
                    "Skipping legacy structure dummy link at {} -> {} because no MachineDummyBlockEntity was created",
                    pos,
                    corePos
            );
            return;
        }
        dummy.setCorePos(corePos);
    }

    private static void linkStructureHeavyDoorPart(
            ServerLevel level,
            BlockPos pos,
            BlockPos corePos,
            BlockPos localOffset
    ) {
        if (!(level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part)) {
            ReinhardtsHBM.LOGGER.error(
                    "Skipping legacy heavy-door part link at {} -> {} because no HbmHeavyDoorPartBlockEntity was created",
                    pos,
                    corePos
            );
            return;
        }
        part.configure(corePos, localOffset);
    }

    private static CompoundTag cleanBlockEntityNbt(
            String blockName,
            CompoundTag tag,
            Map<Short, String> itemPalette
    ) {
        CompoundTag copy = tag.copy();
        cleanLegacyNbt(copy, itemPalette);
        return HbmLegacyNbtTemplate.migrateOrdinaryBlockEntityNbt(blockName, copy);
    }

    private static void cleanLegacyNbt(Tag tag, Map<Short, String> itemPalette) {
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("block", Tag.TAG_STRING)) {
                compound.putString("block", normalizeId(compound.getString("block")));
            }
            if (compound.contains("disguise", Tag.TAG_STRING)) {
                compound.putString("disguise", normalizeId(compound.getString("disguise")));
            }
            if (looksLikeItemStack(compound)
                    && !HbmLegacyNbtTemplate.migrateLegacyItemStack(compound, itemPalette)) {
                clearCompound(compound);
                return;
            }
            for (String key : Set.copyOf(compound.getAllKeys())) {
                cleanLegacyNbt(compound.get(key), itemPalette);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Tag child = list.get(i);
                if (child instanceof CompoundTag compound && looksLikeItemStack(compound)
                        && !HbmLegacyNbtTemplate.migrateLegacyItemStack(compound, itemPalette)) {
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

    private static void clearCompound(CompoundTag tag) {
        for (String key : Set.copyOf(tag.getAllKeys())) {
            tag.remove(key);
        }
    }

    private static Map<Short, String> readItemPalette(CompoundTag root) {
        if (!root.contains("itemPalette", Tag.TAG_LIST)) {
            return Map.of();
        }
        Map<Short, String> palette = new HashMap<>();
        ListTag list = root.getList("itemPalette", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            palette.put(tag.getShort("ID"), normalizeId(tag.getString("Name")));
        }
        return palette;
    }

    private static void collectItemPalette(Tag tag, Map<String, Short> palette, ListTag output) {
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("id", Tag.TAG_STRING) && looksLikeItemStack(compound)) {
                String itemId = normalizeId(compound.getString("id"));
                if (!palette.containsKey(itemId)) {
                    short id = (short) palette.size();
                    palette.put(itemId, id);
                    CompoundTag entry = new CompoundTag();
                    entry.putShort("ID", id);
                    entry.putString("Name", itemId);
                    output.add(entry);
                }
            }
            for (String key : compound.getAllKeys()) {
                collectItemPalette(compound.get(key), palette, output);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                collectItemPalette(list.get(i), palette, output);
            }
        }
    }

    private static CompoundTag writePaletteEntry(LegacyBlockKey key) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", key.id());
        CompoundTag properties = new CompoundTag();
        properties.putString("meta", Integer.toString(key.meta()));
        tag.put("Properties", properties);
        return tag;
    }

    private static ListTag intList(int... values) {
        ListTag tag = new ListTag();
        for (int value : values) {
            tag.add(net.minecraft.nbt.IntTag.valueOf(value));
        }
        return tag;
    }

    private static int[] readIntList(ListTag tag, int expectedSize) {
        int[] result = new int[expectedSize];
        for (int i = 0; i < expectedSize && i < tag.size(); i++) {
            result[i] = tag.getInt(i);
        }
        return result;
    }

    private static int legacyMeta(BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                return 8 | (state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? 1 : 0);
            }
            int facing = switch (state.getValue(DoorBlock.FACING)) {
                case EAST -> 0;
                case SOUTH -> 1;
                case WEST -> 2;
                case NORTH -> 3;
                default -> 0;
            };
            return facing | (state.getValue(DoorBlock.OPEN) ? 4 : 0);
        }
        if (state.getBlock() instanceof com.reinhardt.hbm.block.FilingCabinetBlock && state.hasProperty(com.reinhardt.hbm.block.FilingCabinetBlock.FACING)) {
            return (com.reinhardt.hbm.block.FilingCabinetBlock.legacyRotation(state.getValue(com.reinhardt.hbm.block.FilingCabinetBlock.FACING)) << 2)
                    | state.getValue(com.reinhardt.hbm.block.FilingCabinetBlock.MATERIAL);
        }
        if (state.getBlock() instanceof com.reinhardt.hbm.block.TapeRecorderBlock && state.hasProperty(com.reinhardt.hbm.block.TapeRecorderBlock.FACING)) {
            return switch (state.getValue(com.reinhardt.hbm.block.TapeRecorderBlock.FACING)) {
                case NORTH -> 3;
                case EAST -> 4;
                case WEST -> 5;
                default -> 2;
            };
        }
        if (state.getBlock() instanceof SteelWallBlock && state.hasProperty(SteelWallBlock.FACING)) {
            return SteelWallBlock.toLegacyMeta(state.getValue(SteelWallBlock.FACING));
        }
        if (state.getBlock() instanceof SteelPolesBlock && state.hasProperty(SteelPolesBlock.FACING)) {
            return SteelPolesBlock.toLegacyMeta(state.getValue(SteelPolesBlock.FACING));
        }
        if (state.getBlock() instanceof SteelBeamBlock && state.hasProperty(SteelBeamBlock.FACING)) {
            return SteelBeamBlock.toLegacyMeta(state.getValue(SteelBeamBlock.FACING));
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING).get3DDataValue();
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return switch (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                case SOUTH -> 0;
                case WEST -> 1;
                case NORTH -> 2;
                case EAST -> 3;
                default -> 0;
            };
        }
        var meta = state.getBlock().getStateDefinition().getProperty("meta");
        if (meta != null) {
            try {
                return Integer.parseInt(state.getValue(meta).toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    public static String normalizeId(String name) {
        if (name == null || name.isBlank()) {
            return "minecraft:air";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        String normalized;
        if (lower.startsWith("hbm:tile.")) {
            normalized = ReinhardtsHBM.MOD_ID + ":" + lower.substring("hbm:tile.".length());
        } else if (lower.startsWith("hbm:item.")) {
            normalized = ReinhardtsHBM.MOD_ID + ":" + lower.substring("hbm:item.".length());
        } else if (lower.startsWith("hbm:")) {
            normalized = ReinhardtsHBM.MOD_ID + ":" + lower.substring("hbm:".length());
        } else {
            normalized = lower;
        }
        return LEGACY_ID_ALIASES.getOrDefault(normalized, normalized);
    }

    private static net.minecraft.world.level.block.Rotation toMcRotation(int rotation) {
        return switch (rotation & 3) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
    }

    private record PaletteEntry(String id, int meta) {
    }

    private record Placement(PaletteEntry entry, CompoundTag nbt) {
    }

    private record PreparedPlacement(BlockPos localPos, BlockPos pos, PaletteEntry entry, CompoundTag nbt) {
    }
}
