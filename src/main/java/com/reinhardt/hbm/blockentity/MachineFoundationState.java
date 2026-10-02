package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntFunction;

/**
 * Exact modern counterpart of {@code TileEntityLoadedBase.checkTilt}.
 *
 * <p>This class only owns the old incremental foundation test and persisted
 * tilted flag.  Render transforms remain deliberately machine-specific: the
 * five old renderers did not share an authored rotation order.</p>
 */
final class MachineFoundationState {
    private boolean tilted;
    private int blocksChecked;
    private int blocksValid;

    boolean tilted() {
        return this.tilted;
    }

    boolean tick(Level level, BlockPos core, int floorCount, IntFunction<BlockPos> floorPosition,
                 boolean extraHeavy, boolean enabled) {
        if (!enabled || floorCount <= 0) {
            this.blocksChecked = 0;
            this.blocksValid = 0;
            if (!this.tilted) {
                return false;
            }
            this.tilted = false;
            return true;
        }
        if ((level.getGameTime() + legacyPositionIdentity(core)) % 20L != 0L) {
            return false;
        }

        boolean changed = false;
        if (this.blocksChecked >= floorCount) {
            boolean nextTilted = this.blocksValid < this.blocksChecked * 0.95D;
            if (nextTilted && !this.tilted) {
                level.playSound(null, core, HbmSoundEvents.METAL_IMPACT.get(),
                        SoundSource.BLOCKS, 3.0F, 1.0F);
            }
            changed = nextTilted != this.tilted;
            this.tilted = nextTilted;
            this.blocksChecked = 0;
            this.blocksValid = 0;
        }

        BlockPos floorPos = floorPosition.apply(this.blocksChecked);
        if (floorPos == null) {
            return changed;
        }
        this.blocksChecked++;
        if (extraHeavy ? isValidHeavyFoundation(level, floorPos) : isValidStandardFoundation(level, floorPos)) {
            this.blocksValid++;
        }
        return changed;
    }

    void save(CompoundTag tag) {
        tag.putBoolean("tilted", this.tilted);
    }

    void load(CompoundTag tag) {
        this.tilted = tag.getBoolean("tilted");
        this.blocksChecked = 0;
        this.blocksValid = 0;
    }

    private static boolean isValidStandardFoundation(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isFaceSturdy(level, pos, Direction.UP) || state.is(BlockTags.SAND)) {
            return false;
        }
        return !state.is(HbmBlocks.DIRT_DEAD.get())
                && !state.is(HbmBlocks.DIRT_OILY.get())
                && !state.is(HbmBlocks.STONE_CRACKED.get());
    }

    /** HBM 1.7.10's faux-1.12 BlockPos#getIdentity, including int overflow. */
    private static int legacyPositionIdentity(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 27_644_437) * 27_644_437 + pos.getX();
    }

    private static boolean isValidHeavyFoundation(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isFaceSturdy(level, pos, Direction.UP) || !state.isSolidRender(level, pos)) {
            return false;
        }
        if (state.is(BlockTags.SAND) || state.is(BlockTags.WOOL) || state.is(BlockTags.DIRT)) {
            return false;
        }
        return state.getBlock().getExplosionResistance() >= Blocks.STONE.getExplosionResistance();
    }
}
