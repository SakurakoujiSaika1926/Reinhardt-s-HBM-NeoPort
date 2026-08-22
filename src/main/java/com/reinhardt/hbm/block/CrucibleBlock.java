package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CrucibleBlock extends LargeMachineBlock implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(-1.0D, 0.0D, -1.0D, 2.0D, 0.5D, 2.0D),
            Shapes.box(-0.75D, 0.5D, -0.75D, 1.75D, 1.5D, -0.5D),
            Shapes.box(-0.75D, 0.5D, 1.5D, 1.75D, 1.5D, 1.75D),
            Shapes.box(-0.75D, 0.5D, -0.75D, -0.5D, 1.5D, 1.75D),
            Shapes.box(1.5D, 0.5D, -0.75D, 1.75D, 1.5D, 1.75D)
    );

    public CrucibleBlock(Properties properties) {
        super(properties, Footprint.centered(1, 1, 1), SHAPE, RotationBasis.MODERN_NORTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(blockEntityType, HbmBlockEntities.CRUCIBLE.get(), CrucibleBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(crucible, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ItemTags.SHOVELS) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            if (!level.isClientSide) {
                crucible.clearMoltenToScraps(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }
}

