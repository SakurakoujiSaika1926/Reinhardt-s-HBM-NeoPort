package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HbmHeavyDoorPartBlock extends Block implements EntityBlock {
    public static final BooleanProperty EXTRA = BooleanProperty.create("extra");
    private static final ThreadLocal<Boolean> SUPPRESS_CORE_DESTROY = ThreadLocal.withInitial(() -> false);

    public HbmHeavyDoorPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EXTRA, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HbmHeavyDoorPartBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state, level, pos, true);
    }

    private static VoxelShape shapeFor(BlockState state, BlockGetter level, BlockPos pos, boolean collision) {
        if (!(level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part)) {
            return Shapes.block();
        }
        BlockState coreState = level.getBlockState(part.corePos());
        if (!(coreState.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return Shapes.block();
        }
        boolean open = state.getValue(EXTRA);
        if (level.getBlockEntity(part.corePos()) instanceof HbmHeavyDoorBlockEntity door) {
            open = open || door.isOpenForCollision();
        }
        BlockPos local = part.localOffset();
        return doorBlock.decl().localShape(local.getX(), local.getY(), local.getZ(), open, collision);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            BlockState coreState = level.getBlockState(part.corePos());
            if (coreState.getBlock() instanceof HbmHeavyDoorBlock coreBlock) {
                return coreBlock.useWithoutItem(coreState, level, part.corePos(), player, hitResult.withPosition(part.corePos()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            BlockState coreState = level.getBlockState(part.corePos());
            if (coreState.getBlock() instanceof HbmHeavyDoorBlock coreBlock) {
                return coreBlock.useItemOn(stack, coreState, level, part.corePos(), player, hand, hitResult.withPosition(part.corePos()));
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            BlockState coreState = level.getBlockState(part.corePos());
            return coreState.getBlock().getCloneItemStack(level, part.corePos(), coreState);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_DESTROY.get()
                && !state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            level.destroyBlock(part.corePos(), true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    static void runWithoutCoreDestroy(Runnable action) {
        boolean previous = SUPPRESS_CORE_DESTROY.get();
        SUPPRESS_CORE_DESTROY.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_CORE_DESTROY.set(previous);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EXTRA);
    }
}
