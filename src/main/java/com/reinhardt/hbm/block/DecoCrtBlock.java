package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DecoDisplayBlockEntity;
import com.reinhardt.hbm.item.DecoCrtBlockItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 BlockDecoCRT, including its four metadata variants and lit bit. */
public final class DecoCrtBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape FULL_BLOCK = Shapes.block();

    public DecoCrtBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VARIANT, 0)
                .setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int variant = context.getItemInHand().getItem() instanceof DecoCrtBlockItem item
                ? item.variantIndex(context.getItemInHand()) : 0;
        return defaultBlockState()
                .setValue(FACING, facingForLegacyPlacement(context.getHorizontalDirection()))
                .setValue(VARIANT, variant);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT, LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecoDisplayBlockEntity(pos, state);
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (asItem() instanceof DecoCrtBlockItem item) {
            return java.util.List.of(DecoCrtBlockItem.stackFor(item, state.getValue(VARIANT)));
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (asItem() instanceof DecoCrtBlockItem item) {
            return DecoCrtBlockItem.stackFor(item, state.getValue(VARIANT));
        }
        return super.getCloneItemStack(level, pos, state);
    }

    public static Direction fromLegacyFacing(int facing) {
        return switch (facing & 3) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.WEST;
            case 3 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    private static Direction facingForLegacyPlacement(Direction look) {
        return switch (look) {
            case SOUTH -> Direction.NORTH;
            case WEST -> Direction.EAST;
            case NORTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }
}
