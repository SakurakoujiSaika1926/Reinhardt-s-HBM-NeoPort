package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SatelliteGroundStationBlockEntity;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/** Exact 2x2x7 structure and satellite-chip interaction of 1.7.10 MachineSatLink. */
public final class SatelliteGroundStationBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.box(-1, 0, 0, 6, -1, 0);

    public SatelliteGroundStationBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), SatelliteGroundStationBlock::mapAuthoredOffset);
    }

    /** MachineSatLink was authored facing south; each mapping is kept local to this block. */
    private static BlockPos mapAuthoredOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException("Satellite ground station requires a horizontal facing: " + facing);
        };
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
                case UP, DOWN -> throw new IllegalStateException("Ground-station facing became vertical: " + facing);
            };
            case CLOCKWISE_180 -> facing.getOpposite();
            case COUNTERCLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException("Ground-station facing became vertical: " + facing);
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
                case UP, DOWN -> throw new IllegalStateException("Ground-station facing became vertical: " + facing);
            };
            case FRONT_BACK -> switch (facing) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH, SOUTH -> facing;
                case UP, DOWN -> throw new IllegalStateException("Ground-station facing became vertical: " + facing);
            };
        };
        return state.setValue(FACING, mirrored);
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SatelliteGroundStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.SATELLITE_GROUND_STATION.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SatelliteGroundStationBlockEntity.tick(
                tickerLevel, pos, tickerState, (SatelliteGroundStationBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof SatelliteChipItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return setFrequencyFromChip(level, pos, player, stack);
    }

    public static ItemInteractionResult setFrequencyFromChip(
            Level level, BlockPos corePos, Player player, ItemStack chip) {
        if (!(chip.getItem() instanceof SatelliteChipItem)
                || !(level.getBlockEntity(corePos) instanceof SatelliteGroundStationBlockEntity station)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            int frequency = SatelliteChipItem.frequency(chip);
            station.setFrequency(frequency);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.reinhardtshbm.satellite_ground_station.frequency_set", frequency),
                    false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
