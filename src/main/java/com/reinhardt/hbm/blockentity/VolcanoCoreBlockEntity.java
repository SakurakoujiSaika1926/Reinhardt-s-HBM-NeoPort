package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.VolcanoCoreBlock;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Server-side behavior copied from the old TileEntityVolcanoCore. */
public final class VolcanoCoreBlockEntity extends BlockEntity {
    private int volcanoTimer;

    public VolcanoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.VOLCANO_CORE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcanoCoreBlockEntity core) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof VolcanoCoreBlock volcano)) {
            return;
        }

        core.volcanoTimer++;
        int mode = state.getValue(VolcanoCoreBlock.MODE);
        if (core.volcanoTimer % 10 == 0) {
            if (mode != 4) {
                core.blastMagmaChannel(serverLevel, pos, volcano.radioactive());
                core.raiseMagma(serverLevel, pos, volcano.radioactive());
            }
            if (mode == 4) {
                core.blastMagmaChamber(serverLevel, pos, 15.0D, volcano.radioactive());
                core.meltSurface(serverLevel, pos, volcano.radioactive());
            }
            if (mode != 4) {
                core.spawnBlobs(serverLevel, pos, volcano.radioactive());
                core.spawnSmoke(serverLevel, pos);
            }
            core.surroundLava(serverLevel, pos, volcano.radioactive());
        }

        if (core.volcanoTimer < updateRate(mode)) {
            return;
        }
        core.volcanoTimer = 0;
        if (grows(mode) && pos.getY() < 200) {
            serverLevel.setBlock(pos.above(), state, Block.UPDATE_ALL);
            serverLevel.setBlock(pos, lava(volcano.radioactive()), Block.UPDATE_ALL);
        } else if (extinguishes(mode)) {
            serverLevel.setBlock(pos, lava(volcano.radioactive()), Block.UPDATE_ALL);
        }
    }

    private void blastMagmaChannel(ServerLevel level, BlockPos pos, boolean radioactive) {
        Vec3 first = new Vec3(pos.getX() + 0.5D,
                pos.getY() + level.random.nextInt(15) + 1.5D,
                pos.getZ() + 0.5D);
        LegacyProjectileUtil.volcanicTerrainExplosion(level, first, 7.0F, lava(radioactive));

        Vec3 second = new Vec3(pos.getX() + 0.5D + level.random.nextGaussian() * 3.0D,
                level.random.nextInt(pos.getY() + 1),
                pos.getZ() + 0.5D + level.random.nextGaussian() * 3.0D);
        LegacyProjectileUtil.volcanicTerrainExplosion(level, second, 10.0F, lava(radioactive));
    }

    private void blastMagmaChamber(ServerLevel level, BlockPos pos, double size, boolean radioactive) {
        for (int i = 0; i < 2; i++) {
            double distance = size / (i + 1.0D);
            Vec3 center = new Vec3(
                    pos.getX() + 0.5D + level.random.nextGaussian() * distance,
                    pos.getY() + 0.5D + level.random.nextGaussian() * distance,
                    pos.getZ() + 0.5D + level.random.nextGaussian() * distance);
            LegacyProjectileUtil.volcanicTerrainExplosion(level, center, 7.0F, lava(radioactive));
        }
    }

    private void meltSurface(ServerLevel level, BlockPos pos, boolean radioactive) {
        for (int i = 0; i < 50; i++) {
            int x = (int) Math.floor(pos.getX() + level.random.nextGaussian() * 50.0D);
            int z = (int) Math.floor(pos.getZ() + level.random.nextGaussian() * 50.0D);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                    - (int) Math.floor(Math.abs(level.random.nextGaussian() * 10.0D));
            BlockPos target = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(target);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < Blocks.OBSIDIAN.getExplosionResistance()) {
                level.setBlock(target, state.isSolidRender(level, target)
                        ? lava(radioactive) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private void raiseMagma(ServerLevel level, BlockPos pos, boolean radioactive) {
        BlockPos target = new BlockPos(
                pos.getX() - 10 + level.random.nextInt(21),
                pos.getY() + level.random.nextInt(11),
                pos.getZ() - 10 + level.random.nextInt(21));
        if (level.isEmptyBlock(target) && level.getBlockState(target.below()).is(lava(radioactive).getBlock())) {
            level.setBlock(target, lava(radioactive), Block.UPDATE_ALL);
        }
    }

    private void surroundLava(ServerLevel level, BlockPos pos, boolean radioactive) {
        BlockState lava = lava(radioactive);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        level.setBlock(pos.offset(x, y, z), lava, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private void spawnBlobs(ServerLevel level, BlockPos pos, boolean radioactive) {
        for (int i = 0; i < 3; i++) {
            LegacyShrapnelEntity fragment = new LegacyShrapnelEntity(
                    level, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                    new Vec3(level.random.nextGaussian() * 0.2D,
                            1.0D + level.random.nextDouble(),
                            level.random.nextGaussian() * 0.2D), false);
            if (radioactive) {
                fragment.setRadVolcano(true);
            } else {
                fragment.setVolcano(true);
            }
            level.addFreshEntity(fragment);
        }
    }

    private void spawnSmoke(ServerLevel level, BlockPos pos) {
        level.sendParticles(HbmParticleTypes.VOLCANO_SMOKE.get(),
                pos.getX() + 0.5D, pos.getY() + 10.0D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static BlockState lava(boolean radioactive) {
        return (radioactive ? HbmBlocks.RAD_LAVA_BLOCK : HbmBlocks.VOLCANIC_LAVA_BLOCK).get().defaultBlockState();
    }

    private static boolean grows(int mode) {
        return mode == 2 || mode == 3;
    }

    private static boolean extinguishes(int mode) {
        return mode == 1 || mode == 3;
    }

    private static int updateRate(int mode) {
        return switch (mode) {
            case 1 -> 60 * 60 * 20;
            case 2, 3 -> 60 * 60 * 20 / 250;
            default -> 10;
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("timer", this.volcanoTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.volcanoTimer = Math.max(0, tag.getInt("timer"));
    }
}
