package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CraneMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Directional conveyor crane head.  The old 1.7.10 versions stored the input
 * and output sides in tile entities; the modern port keeps the orientation and
 * enabled state in block state so the blocks remain useful in structures and
 * redstone contraptions even when no menu is installed.
 */
public final class CraneMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public enum Kind {
        BOXER, EXTRACTOR, GRABBER, INSERTER, PARTITIONER, ROUTER, SPLITTER, UNBOXER
    }

    private final Kind kind;
    private static final VoxelShape PARTITIONER_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public CraneMachineBlock(Properties properties, Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, true));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.PARTITIONER ? PARTITIONER_SHAPE : super.getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case BOXER -> boxerStateForPlacement(context);
            case EXTRACTOR -> extractorStateForPlacement(context);
            case GRABBER -> grabberStateForPlacement(context);
            case INSERTER -> inserterStateForPlacement(context);
            case PARTITIONER -> partitionerStateForPlacement(context);
            case ROUTER -> routerStateForPlacement(context);
            case SPLITTER -> splitterStateForPlacement(context);
            case UNBOXER -> unboxerStateForPlacement(context);
        };
    }

    private BlockState boxerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState extractorStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState grabberStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState inserterStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState partitionerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState routerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState splitterStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        BlockPos partner = splitterPartner(context.getClickedPos(), state.getValue(FACING));
        return context.getLevel().getBlockState(partner).canBeReplaced(context) ? state : null;
    }

    private BlockState unboxerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (kind != Kind.SPLITTER || level.isClientSide) {
            return;
        }
        Direction input = state.getValue(FACING);
        BlockPos partner = splitterPartner(pos, input);
        if (!level.getBlockState(partner).canBeReplaced()) {
            return;
        }
        level.setBlock(partner, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CraneMachineBlockEntity core) {
            core.configureSplitterPart(false, pos, input);
        }
        if (level.getBlockEntity(partner) instanceof CraneMachineBlockEntity secondary) {
            secondary.configureSplitterPart(true, pos, input);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || kind == Kind.PARTITIONER || kind == Kind.SPLITTER) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CraneMachineBlockEntity crane
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(crane, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ScrewdriverItem.isScrewdriver(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraneMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type != HbmBlockEntities.CRANE_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> CraneMachineBlockEntity.tick(
                tickerLevel, tickerPos, tickerState, (CraneMachineBlockEntity) blockEntity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (!level.isClientSide && entity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            if (!level.isClientSide && kind == Kind.SPLITTER && entity instanceof CraneMachineBlockEntity splitter) {
                BlockPos counterpart = splitter.splitterCounterpart();
                if (!counterpart.equals(pos) && level.getBlockState(counterpart).is(this)) {
                    if (splitter.splitterSecondary()) {
                        level.destroyBlock(counterpart, true);
                    } else {
                        level.removeBlock(counterpart, false);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (kind == Kind.SPLITTER && entity instanceof CraneMachineBlockEntity splitter && splitter.splitterSecondary()) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind == Kind.BOXER || kind == Kind.EXTRACTOR || kind == Kind.INSERTER
                || kind == Kind.PARTITIONER || kind == Kind.UNBOXER;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CraneMachineBlockEntity crane ? crane.comparatorSignal() : 0;
    }

    public static boolean configureWithScrewdriver(Level level, BlockPos pos, Player player, ItemStack stack,
                                                    InteractionHand hand, Direction clickedFace) {
        if (!(level.getBlockState(pos).getBlock() instanceof CraneMachineBlock block)
                || !(level.getBlockEntity(pos) instanceof CraneMachineBlockEntity crane)) {
            return false;
        }
        if (!level.isClientSide) {
            switch (block.kind) {
                case BOXER -> configureBoxer(crane, player, clickedFace);
                case EXTRACTOR -> configureExtractor(crane, player, clickedFace);
                case GRABBER -> configureGrabber(crane, player, clickedFace);
                case INSERTER -> configureInserter(crane, player, clickedFace);
                case PARTITIONER -> configurePartitioner(crane, player, clickedFace);
                case ROUTER -> configureRouter(crane, player, clickedFace);
                case SPLITTER -> configureSplitter(crane, player);
                case UNBOXER -> configureUnboxer(crane, player, clickedFace);
            }
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return true;
    }

    private static void configureBoxer(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configureExtractor(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configureGrabber(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configureInserter(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configurePartitioner(CraneMachineBlockEntity crane, Player player, Direction side) {
        player.displayClientMessage(Component.translatable("message.reinhardtshbm.crane.fixed_direction"), true);
    }

    private static void configureRouter(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configureSplitter(CraneMachineBlockEntity crane, Player player) {
        crane.adjustSplitterRatio(crane.splitterSecondary(), player.isShiftKeyDown() ? -1 : 1);
        player.displayClientMessage(Component.translatable("message.reinhardtshbm.crane.splitter_ratio",
                crane.leftRatio(), crane.rightRatio()), true);
    }

    private static void configureUnboxer(CraneMachineBlockEntity crane, Player player, Direction side) {
        configureDirectional(crane, player, side);
    }

    private static void configureDirectional(CraneMachineBlockEntity crane, Player player, Direction side) {
        if (player.isShiftKeyDown()) crane.setOutputSide(side);
        else crane.setInputSide(side);
        player.displayClientMessage(Component.translatable("message.reinhardtshbm.crane.directions",
                crane.inputSide().getName(), crane.outputSide().getName()), true);
    }

    private static BlockPos splitterPartner(BlockPos core, Direction input) {
        Direction output = input.getOpposite();
        return core.relative(output.getClockWise());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case BOXER -> rotateBoxer(state, rotation);
            case EXTRACTOR -> rotateExtractor(state, rotation);
            case GRABBER -> rotateGrabber(state, rotation);
            case INSERTER -> rotateInserter(state, rotation);
            case PARTITIONER -> rotatePartitioner(state, rotation);
            case ROUTER -> rotateRouter(state, rotation);
            case SPLITTER -> rotateSplitter(state, rotation);
            case UNBOXER -> rotateUnboxer(state, rotation);
        };
    }

    private static BlockState rotateBoxer(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateExtractor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateGrabber(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateInserter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotatePartitioner(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRouter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSplitter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateUnboxer(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case BOXER -> mirrorBoxer(state, mirror);
            case EXTRACTOR -> mirrorExtractor(state, mirror);
            case GRABBER -> mirrorGrabber(state, mirror);
            case INSERTER -> mirrorInserter(state, mirror);
            case PARTITIONER -> mirrorPartitioner(state, mirror);
            case ROUTER -> mirrorRouter(state, mirror);
            case SPLITTER -> mirrorSplitter(state, mirror);
            case UNBOXER -> mirrorUnboxer(state, mirror);
        };
    }

    private static BlockState mirrorBoxer(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorExtractor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorGrabber(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorInserter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorPartitioner(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRouter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSplitter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorUnboxer(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }
}
