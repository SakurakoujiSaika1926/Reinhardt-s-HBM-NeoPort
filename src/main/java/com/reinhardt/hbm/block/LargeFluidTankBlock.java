package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LargeFluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LargeFluidTankBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int LEGACY_CORE_OFFSET = 6;
    private static final ThreadLocal<Boolean> RELOCATING = ThreadLocal.withInitial(() -> false);

    public LargeFluidTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = context.getClickedPos().relative(facing, -LEGACY_CORE_OFFSET);
        for (BlockPos occupied : occupiedPositions(corePos, facing)) {
            if (!occupied.equals(context.getClickedPos())
                    && !context.getLevel().getBlockState(occupied).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing, -LEGACY_CORE_OFFSET);
        RELOCATING.set(true);
        try {
            level.removeBlock(pos, false);
            level.setBlock(corePos, state, Block.UPDATE_ALL);
        } finally {
            RELOCATING.set(false);
        }
        if (level.getBlockEntity(corePos) instanceof LargeFluidTankBlockEntity tank) {
            tank.loadFromItem(stack);
            tank.refreshConnectionsAfterPlacement();
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> placeDummies(level, corePos, facing));
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing,
                occupiedPositions(corePos, facing), placer);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeFluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.LARGE_FLUID_TANK.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                LargeFluidTankBlockEntity.tick(tickerLevel, pos, tickerState,
                        (LargeFluidTankBlockEntity) blockEntity);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof LargeFluidTankBlockEntity tank) {
            ItemStack stack = new ItemStack(this);
            tank.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LargeFluidTankBlockEntity tank) {
            tank.setType(FluidIdentifierItem.primary(stack));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                    0.25F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !RELOCATING.get()) {
            if (level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            Direction facing = state.getValue(FACING);
            MachineDummyBlock.runWithoutCoreDestroy(() -> removeDummies(level, pos, facing));
            level.invalidateCapabilities(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof LargeFluidTankBlockEntity tank)
                || tank.tank().amount() <= 0) {
            return 0;
        }
        return Math.min(15, (int) Math.floor((double) tank.tank().amount()
                / Math.max(1, tank.tank().capacity()) * 15.0D) + 1);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.block();
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
        builder.add(FACING);
    }

    public static Set<BlockPos> occupiedPositions(BlockPos corePos, Direction facing) {
        Set<BlockPos> positions = LegacyMachineGeometry.newOrderedPosSet();
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 5, 0, 4, 4, 4, 4);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 4, 0, 5, -4, 2, 2);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 4, 0, -4, 5, 2, 2);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 4, 0, 2, 2, 5, -4);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 4, 0, 2, 2, -4, 5);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 3, 0, 6, -5, 0, 0);
        LegacyMachineGeometry.addLegacyBox(positions, corePos, facing, 3, 0, -5, 6, 0, 0);
        return positions;
    }

    private static void placeDummies(Level level, BlockPos corePos, Direction facing) {
        for (BlockPos occupied : occupiedPositions(corePos, facing)) {
            if (occupied.equals(corePos)) {
                continue;
            }
            level.setBlock(occupied, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(occupied) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeDummies(Level level, BlockPos corePos, Direction facing) {
        for (BlockPos occupied : occupiedPositions(corePos, facing)) {
            if (level.getBlockState(occupied).is(HbmBlocks.MACHINE_DUMMY.get())
                    && level.getBlockEntity(occupied) instanceof MachineDummyBlockEntity dummy
                    && dummy.getCorePos().equals(corePos)) {
                level.removeBlock(occupied, false);
            }
        }
    }
}
