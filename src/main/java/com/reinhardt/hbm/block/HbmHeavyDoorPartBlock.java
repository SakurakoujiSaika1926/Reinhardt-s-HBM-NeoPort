package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
            // Parts are created before their BE data is guaranteed to arrive
            // on the client.  An orphaned/transient part must not become a
            // solid camera-blocking cube.
            return Shapes.empty();
        }
        BlockState coreState = level.getBlockState(part.corePos());
        if (!(coreState.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return Shapes.empty();
        }
        if (!(level.getBlockEntity(part.corePos()) instanceof HbmHeavyDoorBlockEntity door)) {
            return Shapes.empty();
        }
        boolean open = state.getValue(EXTRA) || door.isOpenForCollision();
        BlockPos corePos = part.corePos();
        BlockPos worldRelative = new BlockPos(
                pos.getX() - corePos.getX(),
                pos.getY() - corePos.getY(),
                pos.getZ() - corePos.getZ());
        Direction facing = coreState.getValue(HbmHeavyDoorBlock.FACING);
        // Match BlockDoorGeneric#getBoundingBox: the stored footprint
        // coordinate is not the DoorDecl coordinate basis.  The old code
        // rotated the world-relative dummy position by the door direction
        // plus a counter-clockwise quarter turn before asking the declaration
        // for its open/closed AABB.
        BlockPos local = HbmHeavyDoorBlock.legacyCollisionLocalOffset(worldRelative, facing);
        VoxelShape localShape = doorBlock.decl().localShape(local.getX(), local.getY(), local.getZ(), open, collision);
        return HbmHeavyDoorBlock.orientLegacyShape(localShape, facing);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            BlockPos corePos = part.corePos();
            if (!corePos.equals(pos)) {
                BlockState coreState = level.getBlockState(corePos);
                if (!coreState.isAir()) {
                    return coreState.getDestroyProgress(player, level, corePos);
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
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
            if (coreState.getBlock() instanceof HbmHeavyDoorBlock) {
                return coreState.getBlock().getCloneItemStack(level, part.corePos(), coreState);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            part.setDropCoreWhenRemoved(!player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_DESTROY.get()
                && !state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof HbmHeavyDoorPartBlockEntity part) {
            BlockPos corePos = part.corePos();
            if (!corePos.equals(pos)
                    && level.getBlockState(corePos).getBlock() instanceof HbmHeavyDoorBlock) {
                level.destroyBlock(corePos, part.consumeDropCoreWhenRemoved());
            }
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
