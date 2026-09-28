package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
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

/** Exact latest-1.7.10 LaunchpadSoyuz footprint and dedicated orientation rules. */
public final class SoyuzLaunchpadBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = exactLegacyFootprint();

    public SoyuzLaunchpadBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), SoyuzLaunchpadBlock::mapAuthoredOffset);
    }

    /** The source OBJ and fillSpace calls are authored for ForgeDirection.EAST. */
    public static BlockPos mapAuthoredOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case EAST -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case WEST -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case SOUTH -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case NORTH -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Soyuz launch pad requires a horizontal facing: " + facing);
        };
    }

    public static float legacyRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case UP, DOWN -> throw new IllegalStateException(
                    "Soyuz launch pad facing is vertical: " + facing);
        };
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
                case UP, DOWN -> throw new IllegalStateException("Soyuz launch pad facing became vertical");
            };
            case CLOCKWISE_180 -> facing.getOpposite();
            case COUNTERCLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException("Soyuz launch pad facing became vertical");
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
                case UP, DOWN -> throw new IllegalStateException("Soyuz launch pad facing became vertical");
            };
            case FRONT_BACK -> switch (facing) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH, SOUTH -> facing;
                case UP, DOWN -> throw new IllegalStateException("Soyuz launch pad facing became vertical");
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
        return new SoyuzLaunchpadBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.SOYUZ_LAUNCHPAD.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SoyuzLaunchpadBlockEntity.tick(
                tickerLevel, pos, tickerState, (SoyuzLaunchpadBlockEntity) blockEntity);
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

    private static Footprint exactLegacyFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        offsets.add(BlockPos.ZERO);

        // Literal LaunchpadSoyuz#fillSpace calls, already resolved for EAST.
        addAuthoredEastVolume(offsets, 0, 0, 0, 2, 0, 2, 2, 2, 2);
        addAuthoredEastVolume(offsets, 0, 0, 0, 3, 0, 2, 1, 1, 2);
        addAuthoredEastVolume(offsets, 0, 2, 0, 0, 0, 2, 2, -2, 10);
        addAuthoredEastVolume(offsets, 0, 2, 0, 0, 0, 10, -2, 2, 10);
        addAuthoredEastVolume(offsets, 0, 0, 0, 3, -3, 9, 1, 1, 9);
        addAuthoredEastVolume(offsets, 0, 2, 0, -1, 2, 2, 2, -6, 10);
        addAuthoredEastVolume(offsets, 0, 2, 0, -1, 2, 10, -6, 2, 2);
        addAuthoredEastVolume(offsets, 0, 2, 0, -1, 2, 10, -6, -6, 10);

        addAuthoredEastVolume(offsets, 2, 0, 0, 0, 0, 0, 0, -10, 58);
        addAuthoredEastVolume(offsets, 2, 0, 0, 1, -1, 0, 0, -56, 58);
        addAuthoredEastVolume(offsets, -10, 0, 0, 0, 0, 0, 0, -10, 58);
        addAuthoredEastVolume(offsets, -10, 0, 0, 1, -1, 0, 0, -56, 58);

        addAuthoredEastVolume(offsets, 0, 0, 0, 2, 0, 2, 1, 7, -3);
        addAuthoredEastVolume(offsets, -7, 0, 0, 2, 0, 2, 1, 7, -3);
        addAuthoredEastVolume(offsets, -4, 2, 0, 0, 0, 1, 1, 7, -3);
        addAuthoredEastVolume(offsets, -4, 2, 0, 1, -1, 5, 5, 7, -3);
        addAuthoredEastVolume(offsets, -4, 2, 0, 3, -2, 4, 4, 7, -3);
        addAuthoredEastVolume(offsets, -4, 2, 0, 6, -4, 3, 3, 7, -3);
        addAuthoredEastVolume(offsets, -4, 2, 0, 51, -7, 2, 2, 7, -3);
        addAuthoredEastVolume(offsets, 0, 0, 5, 7, 0, -2, 3, 2, 2);

        return new Footprint(List.copyOf(new ArrayList<>(offsets)));
    }

    /** Exact MultiblockHandlerXR EAST bounds; the volume center is never a dummy. */
    private static void addAuthoredEastVolume(LinkedHashSet<BlockPos> offsets,
                                              int centerX, int centerY, int centerZ,
                                              int up, int down, int north, int south,
                                              int west, int east) {
        int minX = centerX - north;
        int maxX = centerX + south;
        int minY = centerY - down;
        int maxY = centerY + up;
        int minZ = centerZ - east;
        int maxZ = centerZ + west;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == centerX && y == centerY && z == centerZ) {
                        continue;
                    }
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}
