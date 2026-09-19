package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasTurbineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // HBM 1.7.10 MachineTurbineGas#getDimensions(): {2, 0, 1, 1, 4, 5}
    // Order is UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT with SOUTH as the unrotated basis.
    public static final LargeMachineBlock.Footprint FOOTPRINT = LargeMachineBlock.Footprint.legacySouthBox(2, 0, 1, 1, 4, 5);
    private final VoxelShape shape;

    public GasTurbineBlock(Properties properties, VoxelShape shape) {
        super(LargeMachineBlock.nonOccludingMachineProperties(properties));
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceOldTurbine(context, context.getClickedPos(), facing)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeOldTurbineDummies(level, pos, state.getValue(FACING));
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), oldTurbinePositions(pos, state.getValue(FACING)), placer);
            refreshPorts(level, pos, state);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.GAS_TURBINE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> GasTurbineBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (GasTurbineBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof GasTurbineBlockEntity turbine) {
            if (!level.isClientSide) {
                turbine.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        if (removed && !level.isClientSide) {
            removeOldTurbineDummies(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
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
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static boolean canPlaceOldTurbine(BlockPlaceContext context, BlockPos corePos, Direction facing) {
        for (BlockPos pos : oldTurbinePositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!context.getLevel().getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos, Direction facing) {
        return clickedPos.relative(facing, -1);
    }

    public static boolean canPlaceLegacyAt(Level level, BlockPos clickedPos, Direction facing, BlockPlaceContext context) {
        BlockPos corePos = legacyCorePos(clickedPos, facing);
        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return false;
        }
        if (!level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        for (BlockPos pos : oldTurbinePositions(corePos, facing)) {
            if (pos.equals(clickedPos) || pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public void placeLegacyDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        placeOldTurbineDummies(level, corePos, facing);
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, oldTurbinePositions(corePos, facing), placer);
        refreshPorts(level, corePos, defaultBlockState().setValue(FACING, facing));
    }

    private static void placeOldTurbineDummies(Level level, BlockPos corePos, Direction facing) {
        for (BlockPos pos : oldTurbinePositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeOldTurbineDummies(Level level, BlockPos corePos, Direction facing) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : oldTurbinePositions(corePos, facing)) {
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static List<BlockPos> oldTurbinePositions(BlockPos corePos, Direction facing) {
        return LegacyMachineGeometry.positionsForLegacyFootprint(corePos, facing, FOOTPRINT);
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        for (GasTurbineBlockEntity.Port port : GasTurbineBlockEntity.portsFor(corePos, facing)) {
            CatalyticCrackerBlock.refreshDuctsAtPort(level, port.pos(), port.connectorPos());
        }
        for (BlockPos connectorPos : GasTurbineBlockEntity.powerConnectors(corePos, facing)) {
            EnergyCableBlock.refreshConnections(level, connectorPos);
        }
    }
}

