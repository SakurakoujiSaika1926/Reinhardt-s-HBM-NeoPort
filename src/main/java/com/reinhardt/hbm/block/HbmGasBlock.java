package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public abstract class HbmGasBlock extends Block {
    protected HbmGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!tryMove(level, pos, firstDirection(level, pos, random))
                && !tryMove(level, pos, secondDirection(level, pos, random))) {
            level.scheduleTick(pos, this, delay(level, random));
        }
    }

    protected abstract Direction firstDirection(Level level, BlockPos pos, RandomSource random);

    protected Direction secondDirection(Level level, BlockPos pos, RandomSource random) {
        return firstDirection(level, pos, random);
    }

    protected int delay(Level level, RandomSource random) {
        return 2;
    }

    protected Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (level.isEmptyBlock(target)) {
            level.removeBlock(pos, false);
            level.setBlock(target, defaultBlockState(), 3);
            level.scheduleTick(target, this, delay(level, level.random));
            return true;
        }
        return false;
    }

    /**
     * 1.7.10 BlockGasBase only rendered a cloud when the client wore
     * ashglasses.  Keep the same opt-in visibility for otherwise invisible
     * gas blocks; the gameplay collision logic remains server authoritative.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide) {
            return;
        }
        Player viewer = level.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D,
                entity -> entity instanceof net.minecraft.world.entity.LivingEntity living
                        && living.getItemBySlot(EquipmentSlot.HEAD).is(HbmItems.ASHGLASSES.get())
        );
        if (viewer != null) {
            level.addParticle(HbmParticleTypes.LEGACY_CLOUD.get(),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 10);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
