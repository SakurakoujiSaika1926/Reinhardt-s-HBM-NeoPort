package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.SpotlightBeamBlock;
import com.reinhardt.hbm.block.SpotlightBlock;
import com.reinhardt.hbm.block.TritiumLampBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the six incoming light directions for a spotlight beam block.
 *
 * The old implementation stored the same bit mask in TileEntityData. Keeping
 * it in a block entity is intentional: a beam can be illuminated by several
 * lamps and must remain until every source has been removed.
 */
public final class SpotlightBeamBlockEntity extends BlockEntity {
    private int incomingMask;

    public SpotlightBeamBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SPOTLIGHT_BEAM.get(), pos, state);
    }

    public static List<Direction> directions(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SpotlightBeamBlockEntity beam) {
            return directions(beam.incomingMask);
        }
        return List.of();
    }

    private static List<Direction> directions(int mask) {
        List<Direction> result = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            if ((mask & bit(direction)) != 0) {
                result.add(direction);
            }
        }
        return result;
    }

    public static int setDirection(Level level, BlockPos pos, Direction direction, boolean enabled) {
        if (!(level.getBlockEntity(pos) instanceof SpotlightBeamBlockEntity beam)) {
            return 0;
        }
        int oldMask = beam.incomingMask;
        if (enabled) {
            beam.incomingMask |= bit(direction);
        } else {
            beam.incomingMask &= ~bit(direction);
        }
        if (oldMask != beam.incomingMask) {
            beam.setChanged();
            level.sendBlockUpdated(pos, beam.getBlockState(), beam.getBlockState(), Block.UPDATE_CLIENTS);
        }
        return beam.incomingMask;
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    /** Add a beam segment exactly as Spotlight.propagateBeam did in 1.7.10. */
    public static void propagate(Level level, BlockPos source, Direction direction, int distance) {
        distance--;
        if (distance <= 0) {
            return;
        }

        BlockPos next = source.relative(direction);
        BlockState state = level.getBlockState(next);
        if (!state.isAir() && !(state.getBlock() instanceof SpotlightBeamBlock)) {
            return;
        }

        if (!(state.getBlock() instanceof SpotlightBeamBlock)) {
            level.setBlock(next, com.reinhardt.hbm.registry.HbmBlocks.SPOTLIGHT_BEAM.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (setDirection(level, next, direction, true) == 0) {
            return;
        }
        propagate(level, next, direction, distance);
    }

    /** Remove one source direction while preserving other incoming sources. */
    public static void unpropagate(Level level, BlockPos source, Direction direction) {
        BlockPos next = source.relative(direction);
        if (!(level.getBlockState(next).getBlock() instanceof SpotlightBeamBlock)) {
            return;
        }
        if (setDirection(level, next, direction, false) == 0) {
            level.removeBlock(next, false);
        }
        unpropagate(level, next, direction);
    }

    /** Rebuild a beam after a blocking neighbor has been removed. */
    public static void repropagateFrom(Level level, BlockPos beamPos) {
        for (Direction direction : directions(level, beamPos)) {
            BlockPos previous = beamPos.relative(direction.getOpposite());
            BlockState previousState = level.getBlockState(previous);
            if (previousState.getBlock() instanceof SpotlightBlock lamp && lamp.isLit()) {
                propagate(level, previous, direction, lamp.beamLength());
            } else if (previousState.getBlock() instanceof TritiumLampBlock lamp && lamp.isLit()) {
                propagate(level, previous, direction, lamp.beamLength());
            } else if (previousState.getBlock() instanceof SpotlightBeamBlock) {
                repropagateFrom(level, previous);
            }
        }
    }

    /** Remove every segment that was supplied by this beam's incoming rays. */
    public static void removeFromAllSources(Level level, BlockPos pos) {
        for (Direction direction : directions(level, pos)) {
            unpropagate(level, pos, direction);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IncomingMask", incomingMask);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        incomingMask = tag.getInt("IncomingMask") & 0x3F;
    }
}
