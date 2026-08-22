package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BigAssTankBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BigAssTankBlock extends LargeMachineBlock implements EntityBlock {
    private static final int CORE_OFFSET = 2;
    private static final ThreadLocal<Boolean> RELOCATING = ThreadLocal.withInitial(() -> false);

    public BigAssTankBlock(Properties properties, Footprint footprint, VoxelShape shape) {
        super(properties, footprint, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Exact 1.7.10 BAT9000 footprint; the four corners are intentionally empty. */
    public static Footprint legacyFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
            for (int z = -1; z <= 1; z++) {
                offsets.add(new BlockPos(-2, y, z));
                offsets.add(new BlockPos(2, y, z));
            }
        }
        return new Footprint(List.copyOf(offsets));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = context.getClickedPos().relative(facing, -CORE_OFFSET);
        for (BlockPos occupied : footprintPositions(corePos, facing)) {
            if (!occupied.equals(context.getClickedPos())
                    && !context.getLevel().getBlockState(occupied).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }

        net.minecraft.core.Direction facing = state.getValue(FACING);
        BlockPos corePos = pos.relative(facing, -CORE_OFFSET);
        RELOCATING.set(true);
        try {
            level.removeBlock(pos, false);
            level.setBlock(corePos, state, Block.UPDATE_ALL);
        } finally {
            RELOCATING.set(false);
        }

        if (level.getBlockEntity(corePos) instanceof BigAssTankBlockEntity tank) {
            tank.loadFromItem(stack);
            tank.refreshConnectionsAfterPlacement();
        }
        LargeMachineBlock.placeDummies(level, corePos, facing, machineFootprint(), RotationBasis.HBM_LEGACY_SOUTH);
        LargeMachineBlock.pushEntitiesOutOfFootprint(level, corePos, facing, machineFootprint(), placer, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BigAssTankBlockEntity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof BigAssTankBlockEntity tank) {
            ItemStack stack = new ItemStack(this);
            tank.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.BIG_ASS_TANK.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof BigAssTankBlockEntity tank) {
                BigAssTankBlockEntity.tick(tickerLevel, pos, tickerState, tank);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BigAssTankBlockEntity tank) {
            tank.setType(FluidIdentifierItem.primary(stack));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !RELOCATING.get()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            level.invalidateCapabilities(pos);
            LargeMachineBlock.removeDummies(level, pos, state.getValue(FACING), machineFootprint(), RotationBasis.HBM_LEGACY_SOUTH);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private Set<BlockPos> footprintPositions(BlockPos corePos, net.minecraft.core.Direction facing) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (BlockPos offset : machineFootprint().offsets()) {
            positions.add(corePos.offset(com.reinhardt.hbm.util.LegacyMachineGeometry.rotate(
                    offset, facing, RotationBasis.HBM_LEGACY_SOUTH)));
        }
        return positions;
    }
}

