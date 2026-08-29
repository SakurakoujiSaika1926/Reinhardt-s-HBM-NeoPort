package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Direct 1.7.10 BlockDetonatable port for the four wired explosives. */
public class DetonatableBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private final Kind kind;

    public DetonatableBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return withNeighborConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), canConnectToDetCord(level, pos, direction));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock()) && !level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate(level, pos, null);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && (level.hasNeighborSignal(pos) || isAdjacentToFire(level, pos))) {
            detonate(level, pos, null);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.Explosion explosion) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Entity owner = explosion == null ? null : explosion.getIndirectSourceEntity();
        level.removeBlock(pos, false);
        prime(server, pos, owner, 0, this.kind);
    }

    public boolean canDropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return false;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 15;
    }

    /** Remote detonators use the same entry point as redstone and chain reactions. */
    public void detonate(Level level, BlockPos pos, @Nullable Entity owner) {
        if (!(level instanceof ServerLevel server) || !level.getBlockState(pos).is(this)) {
            return;
        }
        level.removeBlock(pos, false);
        detonatePayload(server, pos, this.kind, owner);
    }

    /** Detonates a primed chain entity after its source block has been removed. */
    public static void detonatePrimed(ServerLevel level, BlockPos pos,
                                      TimedExplosiveEntity.Kind kind, @Nullable Entity owner) {
        switch (kind) {
            case DET_CORD -> detonatePayload(level, pos, Kind.CORD, owner);
            case DET_CHARGE -> detonatePayload(level, pos, Kind.CHARGE, owner);
            case DET_NUKE -> detonatePayload(level, pos, Kind.NUKE, owner);
            case DET_MINER -> detonatePayload(level, pos, Kind.MINER, owner);
            default -> {
            }
        }
    }

    private static void detonatePayload(ServerLevel level, BlockPos pos, Kind kind, @Nullable Entity owner) {
        Vec3 center = Vec3.atCenterOf(pos);
        switch (kind) {
            case CORD -> level.explode(null, center.x, center.y, center.z,
                    1.5F, true, Level.ExplosionInteraction.TNT);
            case CHARGE -> LegacyProjectileUtil.detonateExplosiveCharge(level, center, owner);
            case NUKE -> NukeExplosionManager.scheduleMissileNuke(level, center.x, center.y, center.z);
            case MINER -> LegacyProjectileUtil.detonateMiningCharge(level, center);
        }
    }

    private static void prime(ServerLevel level, BlockPos pos, @Nullable Entity owner, int fuse, Kind kind) {
        level.addFreshEntity(new TimedExplosiveEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, owner, fuse, kind.timedKind()));
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static boolean isAdjacentToFire(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState withNeighborConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(propertyFor(direction), canConnectToDetCord(level, pos, direction));
        }
        return updated;
    }

    public static boolean canConnectToDetCord(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.getBlock() instanceof DetonatableBlock detonatable && detonatable.kind.connectible;
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    public enum Kind {
        CORD(true, TimedExplosiveEntity.Kind.DET_CORD),
        CHARGE(true, TimedExplosiveEntity.Kind.DET_CHARGE),
        NUKE(true, TimedExplosiveEntity.Kind.DET_NUKE),
        MINER(false, TimedExplosiveEntity.Kind.DET_MINER);

        private final boolean connectible;
        private final TimedExplosiveEntity.Kind timedKind;

        Kind(boolean connectible, TimedExplosiveEntity.Kind timedKind) {
            this.connectible = connectible;
            this.timedKind = timedKind;
        }

        public TimedExplosiveEntity.Kind timedKind() {
            return this.timedKind;
        }
    }
}
