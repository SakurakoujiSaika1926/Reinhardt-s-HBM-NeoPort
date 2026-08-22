package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.List;

/** Chunk-safe procedural translation of AncientTomb.build from HBM 1.7.10. */
public final class AncientTombPiece extends StructurePiece {
    private static final int PYRAMID_SIZE = 15;
    private static final int WORLD_RADIUS = 32;

    private final int originX;
    private final int originZ;
    private final int pyramidBase;
    private final long seed;

    public AncientTombPiece(BlockPos origin, long seed, int minBuildHeight, int maxBuildHeight) {
        super(
                HbmWorldgenStructures.ANCIENT_TOMB_PIECE.get(),
                0,
                new BoundingBox(
                        origin.getX() - WORLD_RADIUS,
                        minBuildHeight,
                        origin.getZ() - WORLD_RADIUS,
                        origin.getX() + WORLD_RADIUS,
                        maxBuildHeight - 1,
                        origin.getZ() + WORLD_RADIUS
                )
        );
        this.originX = origin.getX();
        this.originZ = origin.getZ();
        this.pyramidBase = origin.getY();
        this.seed = seed;
        this.setOrientation(null);
    }

    public AncientTombPiece(CompoundTag tag) {
        super(HbmWorldgenStructures.ANCIENT_TOMB_PIECE.get(), tag);
        this.originX = tag.getInt("OriginX");
        this.originZ = tag.getInt("OriginZ");
        this.pyramidBase = tag.getInt("PyramidBase");
        this.seed = tag.getLong("Seed");
        this.setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OriginX", this.originX);
        tag.putInt("OriginZ", this.originZ);
        tag.putInt("PyramidBase", this.pyramidBase);
        tag.putLong("Seed", this.seed);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource ignored,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pos
    ) {
        generate(level, chunkBox, RandomSource.create(this.seed), this.originX, this.pyramidBase, this.originZ);
    }

    private static void generate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int yOff, int z) {
        List<Block> concrete = List.of(
                HbmBlocks.BRICK_CONCRETE.get(),
                HbmBlocks.BRICK_CONCRETE_BROKEN.get(),
                HbmBlocks.BRICK_CONCRETE_CRACKED.get()
        );

        // The underground chamber is always centered at Y=20 in the 1.7.10 implementation.
        int y = 20;

        for (int iy = PYRAMID_SIZE; iy > 0; iy--) {
            int range = PYRAMID_SIZE - iy;
            for (int ix = -range; ix <= range; ix++) {
                for (int iz = -range; iz <= range; iz++) {
                    if ((ix <= -range + 1 || ix >= range - 1) && (iz <= -range + 1 || iz >= range - 1)) {
                        set(level, chunkBox, x + ix, yOff + iy, z + iz, HbmBlocks.REINFORCED_STONE.get().defaultBlockState());
                    } else if (iy == 1) {
                        set(level, chunkBox, x + ix, yOff + iy, z + iz, HbmBlocks.CONCRETE_SMOOTH.get().defaultBlockState());
                    } else if (ix <= -range + 1 || ix >= range - 1 || iz <= -range + 1 || iz >= range - 1) {
                        set(level, chunkBox, x + ix, yOff + iy, z + iz, HbmBlocks.CONCRETE_SMOOTH.get().defaultBlockState());
                    } else {
                        set(level, chunkBox, x + ix, yOff + iy, z + iz, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        fillRandom(level, chunkBox, random, x - 2, yOff + 2, z - 2, 5, 4, 5, concrete);
        set(level, chunkBox, x + 2, yOff + 3, z, HbmBlocks.BRICK_CONCRETE_MARKED.get().defaultBlockState());
        set(level, chunkBox, x - 2, yOff + 3, z, HbmBlocks.BRICK_CONCRETE_MARKED.get().defaultBlockState());
        set(level, chunkBox, x, yOff + 3, z + 2, HbmBlocks.BRICK_CONCRETE_MARKED.get().defaultBlockState());
        set(level, chunkBox, x, yOff + 3, z - 2, HbmBlocks.BRICK_CONCRETE_MARKED.get().defaultBlockState());

        fill(level, chunkBox, x + 5, yOff + 2, z + 5, x + 5, yOff + 8, z + 5, HbmBlocks.CONCRETE_PILLAR.get().defaultBlockState());
        fill(level, chunkBox, x + 5, yOff + 2, z - 5, x + 5, yOff + 8, z - 5, HbmBlocks.CONCRETE_PILLAR.get().defaultBlockState());
        fill(level, chunkBox, x - 5, yOff + 2, z - 5, x - 5, yOff + 8, z - 5, HbmBlocks.CONCRETE_PILLAR.get().defaultBlockState());
        fill(level, chunkBox, x - 5, yOff + 2, z + 5, x - 5, yOff + 8, z + 5, HbmBlocks.CONCRETE_PILLAR.get().defaultBlockState());

        int spikeCount = 36 + random.nextInt(15);
        double vecX = 20.0D;
        double vecZ = 0.0D;
        double angle = Math.toRadians(360.0D / spikeCount);
        for (int i = 0; i < spikeCount; i++) {
            double rotatedX = vecX * Math.cos(angle) - vecZ * Math.sin(angle);
            double rotatedZ = vecX * Math.sin(angle) + vecZ * Math.cos(angle);
            vecX = rotatedX;
            vecZ = rotatedZ;
            double variance = 1.0D + random.nextDouble() * 0.4D;
            int ix = (int) (x + vecX * variance);
            int iz = (int) (z + vecZ * variance);
            int iy = level.getHeight(Heightmap.Types.WORLD_SURFACE, ix, iz) - 3;
            fill(level, chunkBox, ix, iy, iz, ix, iy + 6, iz, HbmBlocks.DECO_STEEL.get().defaultBlockState());
        }

        double spiralX = 10.0D;
        double spiralZ = 0.0D;
        double spiralAngle = Math.toRadians(360.0D / 32.0D);
        for (int i = y - 1; i < yOff + 2; i++) {
            int ix = (int) Math.floor(spiralX);
            int iz = (int) Math.floor(spiralZ);
            int height = i < yOff ? 3 : 2;
            BlockState tunnelState = i > 40
                    ? Blocks.AIR.defaultBlockState()
                    : HbmBlocks.GAS_RADON_TOMB.get().defaultBlockState();
            fill(level, chunkBox, x + ix - 1, i, z + iz - 1, x + ix + 1, i + height - 1, z + iz + 1, tunnelState);

            for (int dx = x + ix - 2; dx <= x + ix + 2; dx++) {
                for (int dy = i - 1; dy <= i + 3; dy++) {
                    for (int dz = z + iz - 2; dz <= z + iz + 2; dz++) {
                        BlockState existing = level.getBlockState(new BlockPos(dx, dy, dz));
                        if (!isTunnelMaterial(existing)) {
                            setRandom(level, chunkBox, dx, dy, dz, concrete, random);
                        }
                    }
                }
            }

            double nextX = spiralX * Math.cos(spiralAngle) - spiralZ * Math.sin(spiralAngle);
            double nextZ = spiralX * Math.sin(spiralAngle) + spiralZ * Math.cos(spiralAngle);
            spiralX = nextX;
            spiralZ = nextZ;
        }

        for (int dx = x + 4; dx < x + 8; dx++) {
            for (int dy = y - 1; dy < y + 4; dy++) {
                for (int dz = z - 2; dz < z + 3; dz++) {
                    BlockState existing = level.getBlockState(new BlockPos(dx, dy, dz));
                    if (!isTunnelMaterial(existing)) {
                        setRandom(level, chunkBox, dx, dy, dz, concrete, random);
                    }
                }
            }
        }

        int size = 5;
        int cladding = size - 1;
        int core = size - 2;
        int dimOuter = size * 2 + 1;
        int dimInner = cladding * 2 + 1;
        int dimCore = core * 2 + 1;

        fill(level, chunkBox, x - size, y - size, z - size, x - size, y + size, z + size, concrete, random);
        fill(level, chunkBox, x - size, y - size, z - size, x + size, y - size, z + size, concrete, random);
        fill(level, chunkBox, x - size, y - size, z - size, x + size, y + size, z - size, concrete, random);
        fill(level, chunkBox, x + size, y - size, z - size, x + size, y + size, z + size, concrete, random);
        fill(level, chunkBox, x - size, y + size, z - size, x + size, y + size, z + size, concrete, random);
        fill(level, chunkBox, x - size, y - size, z + size, x + size, y + size, z + size, concrete, random);

        BlockState obsidian = HbmBlocks.BRICK_OBSIDIAN.get().defaultBlockState();
        fill(level, chunkBox, x - cladding, y - cladding, z - cladding, x - cladding, y + cladding, z + cladding, obsidian);
        fill(level, chunkBox, x - cladding, y - cladding, z - cladding, x + cladding, y - cladding, z + cladding, obsidian);
        fill(level, chunkBox, x - cladding, y - cladding, z - cladding, x + cladding, y + cladding, z - cladding, obsidian);
        fill(level, chunkBox, x + cladding, y - cladding, z - cladding, x + cladding, y + cladding, z + cladding, obsidian);
        fill(level, chunkBox, x - cladding, y + cladding, z - cladding, x + cladding, y + cladding, z + cladding, obsidian);
        fill(level, chunkBox, x - cladding, y - cladding, z + cladding, x + cladding, y + cladding, z + cladding, obsidian);

        fill(level, chunkBox, x - core, y - core, z - core, x + core, y + core, z + core, HbmBlocks.ANCIENT_SCRAP.get().defaultBlockState());

        fill(level, chunkBox, x + 6, y - 2, z - 1, x + 7, y - 2, z + 1, concrete, random);
        fill(level, chunkBox, x + 4, y - 1, z - 1, x + 8, y + 1, z + 1, HbmBlocks.GAS_RADON_TOMB.get().defaultBlockState());
        fill(level, chunkBox, x + 6, y + 2, z - 1, x + 7, y + 2, z + 1, concrete, random);
    }

    private static boolean isTunnelMaterial(BlockState state) {
        Block block = state.getBlock();
        return state.isAir()
                || block == HbmBlocks.GAS_RADON_TOMB.get()
                || block == HbmBlocks.CONCRETE.get()
                || block == HbmBlocks.CONCRETE_SMOOTH.get()
                || block == HbmBlocks.BRICK_CONCRETE.get()
                || block == HbmBlocks.BRICK_CONCRETE_CRACKED.get()
                || block == HbmBlocks.BRICK_CONCRETE_BROKEN.get();
    }

    private static void setRandom(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, List<Block> blocks, RandomSource random) {
        set(level, chunkBox, x, y, z, blocks.get(random.nextInt(blocks.size())).defaultBlockState());
    }

    private static void fillRandom(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z, int sx, int sy, int sz, List<Block> blocks) {
        for (int ix = x; ix < x + sx; ix++) {
            for (int iy = y; iy < y + sy; iy++) {
                for (int iz = z; iz < z + sz; iz++) {
                    setRandom(level, chunkBox, ix, iy, iz, blocks, random);
                }
            }
        }
    }

    private static void fill(WorldGenLevel level, BoundingBox chunkBox, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    set(level, chunkBox, x, y, z, state);
                }
            }
        }
    }

    private static void fill(WorldGenLevel level, BoundingBox chunkBox, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, List<Block> blocks, RandomSource random) {
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    setRandom(level, chunkBox, x, y, z, blocks, random);
                }
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState state) {
        BlockPos pos = new BlockPos(x, y, z);
        if (chunkBox.isInside(pos) && y >= level.getMinBuildHeight() && y < level.getMaxBuildHeight()) {
            level.setBlock(pos, state, 2);
        }
    }
}
