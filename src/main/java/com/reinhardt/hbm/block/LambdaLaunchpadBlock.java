package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Exact five-volume footprint of the latest 1.7.10 Lambda launch pad. */
public final class LambdaLaunchpadBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = exactLegacyFootprint();

    public LambdaLaunchpadBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), LambdaLaunchpadBlock::mapAuthoredOffset);
    }

    /** The Lambda OBJ and BlockDummyable dimensions are authored facing south. */
    public static BlockPos mapAuthoredOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Lambda launch pad requires a horizontal facing: " + facing);
        };
    }

    /** Five TilePort positions produced by LaunchpadLambda#fillSpace. */
    public static List<BlockPos> portHosts(BlockPos corePos, Direction facing) {
        Direction rotated = facing.getClockWise();
        ArrayList<BlockPos> positions = new ArrayList<>(5);
        for (int i = 2; i <= 6; i++) {
            positions.add(corePos.relative(facing, 7).relative(rotated, -i).above().immutable());
        }
        return List.copyOf(positions);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(FACING);
        Direction rotated = switch (rotation) {
            case NONE -> facing;
            case CLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException(
                        "Lambda launch pad facing became vertical: " + facing);
            };
            case CLOCKWISE_180 -> facing.getOpposite();
            case COUNTERCLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException(
                        "Lambda launch pad facing became vertical: " + facing);
            };
        };
        return state.setValue(FACING, rotated);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        Direction mirrored = switch (mirror) {
            case NONE -> facing;
            case LEFT_RIGHT -> switch (facing) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST, WEST -> facing;
                case UP, DOWN -> throw new IllegalStateException(
                        "Lambda launch pad facing became vertical: " + facing);
            };
            case FRONT_BACK -> switch (facing) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH, SOUTH -> facing;
                case UP, DOWN -> throw new IllegalStateException(
                        "Lambda launch pad facing became vertical: " + facing);
            };
        };
        return state.setValue(FACING, mirrored);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LambdaLaunchpadBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LAMBDA_LAUNCHPAD.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> LambdaLaunchpadBlockEntity.tick(
                tickerLevel, pos, tickerState, (LambdaLaunchpadBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static float legacyRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case UP, DOWN -> throw new IllegalStateException(
                    "Lambda launch pad facing is vertical: " + facing);
        };
    }

    private static Footprint exactLegacyFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        addBox(offsets, -7, 7, 0, 1, -7, 7);
        addBox(offsets, -7, 7, 2, 2, -7, -6);
        addBox(offsets, -7, 7, 2, 2, 6, 7);
        addBox(offsets, -7, -6, 2, 2, -7, 7);
        addBox(offsets, 6, 7, 2, 2, -7, 7);
        return new Footprint(List.copyOf(new ArrayList<>(offsets)));
    }

    private static void addBox(LinkedHashSet<BlockPos> offsets,
                               int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}
