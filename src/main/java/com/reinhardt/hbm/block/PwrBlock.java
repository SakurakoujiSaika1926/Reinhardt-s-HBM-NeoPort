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
        if (kind == Kind.CONTROLLER) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.title").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.shell").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.core").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.required").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.facing").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_controller.guide.assemble").withStyle(ChatFormatting.DARK_GRAY));
        }
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
        return switch (this.kind) {
            case BLOCK -> blockStateForPlacement(context);
            case CASING -> casingStateForPlacement(context);
            case CHANNEL -> channelStateForPlacement(context);
            case CONTROL -> controlStateForPlacement(context);
            case CONTROLLER -> controllerStateForPlacement(context);
            case FUEL -> fuelStateForPlacement(context);
            case HEATEX -> heatExchangerStateForPlacement(context);
            case HEATSINK -> heatSinkStateForPlacement(context);
            case NEUTRON_SOURCE -> neutronSourceStateForPlacement(context);
            case PORT -> portStateForPlacement(context);
            case REFLECTOR -> reflectorStateForPlacement(context);
        };
    }

    private BlockState blockStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState casingStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState channelStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState controlStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState controllerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState fuelStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState heatExchangerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState heatSinkStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState neutronSourceStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState portStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    private BlockState reflectorStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PORT);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case BLOCK -> rotateBlock(state, rotation);
            case CASING -> rotateCasing(state, rotation);
            case CHANNEL -> rotateChannel(state, rotation);
            case CONTROL -> rotateControl(state, rotation);
            case CONTROLLER -> rotateController(state, rotation);
            case FUEL -> rotateFuel(state, rotation);
            case HEATEX -> rotateHeatExchanger(state, rotation);
            case HEATSINK -> rotateHeatSink(state, rotation);
            case NEUTRON_SOURCE -> rotateNeutronSource(state, rotation);
            case PORT -> rotatePort(state, rotation);
            case REFLECTOR -> rotateReflector(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case BLOCK -> mirrorBlock(state, mirror);
            case CASING -> mirrorCasing(state, mirror);
            case CHANNEL -> mirrorChannel(state, mirror);
            case CONTROL -> mirrorControl(state, mirror);
            case CONTROLLER -> mirrorController(state, mirror);
            case FUEL -> mirrorFuel(state, mirror);
            case HEATEX -> mirrorHeatExchanger(state, mirror);
            case HEATSINK -> mirrorHeatSink(state, mirror);
            case NEUTRON_SOURCE -> mirrorNeutronSource(state, mirror);
            case PORT -> mirrorPort(state, mirror);
            case REFLECTOR -> mirrorReflector(state, mirror);
        };
    }

    private static BlockState rotateBlock(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateCasing(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateChannel(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateControl(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateController(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFuel(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateHeatExchanger(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateHeatSink(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateNeutronSource(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotatePort(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState rotateReflector(BlockState state, Rotation rotation) {
        return state;
    }

    private static BlockState mirrorBlock(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorCasing(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorChannel(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorControl(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorController(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorFuel(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorHeatExchanger(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorHeatSink(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorNeutronSource(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorPort(BlockState state, Mirror mirror) {
        return state;
    }

    private static BlockState mirrorReflector(BlockState state, Mirror mirror) {
        return state;
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
                PwrControllerBlockEntity.AssemblyResult result = controller.assemble();
                Component message = switch (result.status()) {
                    case SUCCESS -> Component.translatable("chat.reinhardtshbm.pwr.assembly.success");
                    case NON_REACTOR_BLOCK -> Component.translatable(
                            "chat.reinhardtshbm.pwr.assembly.non_reactor_block",
                            result.problemPos().getX(), result.problemPos().getY(), result.problemPos().getZ()
                    );
                    case MAX_SIZE_EXCEEDED -> Component.translatable("chat.reinhardtshbm.pwr.assembly.max_size", 4096);
                    case FUEL_RODS_REQUIRED -> Component.translatable("chat.reinhardtshbm.pwr.assembly.fuel_rods_required");
                    case NEUTRON_SOURCES_REQUIRED -> Component.translatable("chat.reinhardtshbm.pwr.assembly.neutron_sources_required");
                    case UNAVAILABLE -> Component.translatable("chat.reinhardtshbm.pwr.assembly.unavailable");
                };
                player.displayClientMessage(
                        message.copy().withStyle(result.status() == PwrControllerBlockEntity.AssemblyStatus.SUCCESS
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED),
                        true
                );
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
        /*
         * Create and Create Aeronautics capture a contraption with the
         * vanilla moving flag.  That is a relocation, not a player breaking
         * one reactor cell.  Disassembling here would restore every raw PWR
         * block in the old world while the contraption keeps a second copy.
         */
        if (removed && !movedByPiston && !level.isClientSide) {
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
                || block == HbmBlocks.PWR_FUEL.get()
                || block == HbmBlocks.PWR_HEATEX.get()
                || block == HbmBlocks.PWR_HEATSINK.get()
                || block == HbmBlocks.PWR_NEUTRON_SOURCE.get()
                || block == HbmBlocks.PWR_PORT.get()
                || block == HbmBlocks.PWR_REFLECTOR.get();
    }
}
