package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PwrBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PORT = BooleanProperty.create("port");

    private final Kind kind;

    public PwrBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PORT, false));
        if (kind == Kind.CONTROLLER) {
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "desc.reinhardtshbm." + descriptionPath();
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private String descriptionPath() {
        return switch (kind) {
            case BLOCK -> "pwr_block";
            case CASING -> "pwr_casing";
            case CHANNEL -> "pwr_channel";
            case CONTROL -> "pwr_control";
            case CONTROLLER -> "pwr_controller";
            case FUEL -> "pwr_fuelrod";
            case HEATEX -> "pwr_heatex";
            case HEATSINK -> "pwr_heatsink";
            case NEUTRON_SOURCE -> "pwr_neutron_source";
            case PORT -> "pwr_port";
            case REFLECTOR -> "pwr_reflector";
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return kind == Kind.CONTROLLER ? state.setValue(FACING, context.getHorizontalDirection().getOpposite()) : state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PORT);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return kind == Kind.CONTROLLER ? state.setValue(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return kind == Kind.CONTROLLER ? state.rotate(mirror.getRotation(state.getValue(FACING))) : state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.CONTROLLER ? new PwrControllerBlockEntity(pos, state) : new PwrBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (kind == Kind.CONTROLLER && type == HbmBlockEntities.PWR_CONTROLLER.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) -> PwrControllerBlockEntity.tick(tickerLevel, pos, tickerState, (PwrControllerBlockEntity) blockEntity);
        }
        if (kind != Kind.CONTROLLER && type == HbmBlockEntities.PWR_PART.get()) {
            return (tickerLevel, pos, tickerState, blockEntity) -> PwrBlockEntity.tick(tickerLevel, pos, tickerState, (PwrBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (kind != Kind.CONTROLLER) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PwrControllerBlockEntity controller) {
            if (!controller.assembled()) {
                controller.assemble();
                return InteractionResult.CONSUME;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu((MenuProvider) controller, buffer -> buffer.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Held items use a separate interaction path in 1.21.  Let that path fall
     * through to {@link #useWithoutItem} so the controller keeps the 1.7.10
     * behavior of opening after assembly regardless of the player's hand.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof PwrControllerBlockEntity controller) {
                controller.disassemble();
            } else if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
                part.restoreOriginalAndInvalidateCore();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public enum Kind {
        BLOCK,
        CASING,
        CHANNEL,
        CONTROL,
        CONTROLLER,
        FUEL,
        HEATEX,
        HEATSINK,
        NEUTRON_SOURCE,
        PORT,
        REFLECTOR
    }

    public static boolean isPwrBlock(Block block) {
        return block == HbmBlocks.PWR_BLOCK.get()
                || block == HbmBlocks.PWR_CASING.get()
                || block == HbmBlocks.PWR_CHANNEL.get()
                || block == HbmBlocks.PWR_CONTROL.get()
                || block == HbmBlocks.PWR_CONTROLLER.get()
                || block == HbmBlocks.PWR_FUELROD.get()
                || block == HbmBlocks.PWR_HEATEX.get()
                || block == HbmBlocks.PWR_HEATSINK.get()
                || block == HbmBlocks.PWR_NEUTRON_SOURCE.get()
                || block == HbmBlocks.PWR_PORT.get()
                || block == HbmBlocks.PWR_REFLECTOR.get();
    }
}
