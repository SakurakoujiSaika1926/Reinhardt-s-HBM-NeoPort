package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class CatalyticCrackerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public CatalyticCrackerBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return LargeMachineBlock.canPlaceLegacyFootprint(context, facing, FOOTPRINT)
                ? this.defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CatalyticCrackerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPorts(level, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.CATALYTIC_CRACKER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> CatalyticCrackerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (CatalyticCrackerBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CatalyticCrackerBlockEntity cracker) {
            printInfo(player, cracker);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CatalyticCrackerBlockEntity cracker) {
            applyIdentifier(player, stack, cracker);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            refreshPorts(level, pos, state);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshPorts(level, pos, state);
        }
    }

    public static void printInfo(Player player, CatalyticCrackerBlockEntity cracker) {
        for (int index = 0; index < CatalyticCrackerBlockEntity.TANK_COUNT; index++) {
            player.displayClientMessage(cracker.tankLine(index), false);
        }
    }

    public static void applyIdentifier(Player player, ItemStack stack, CatalyticCrackerBlockEntity cracker) {
        cracker.setInputType(FluidIdentifierItem.primary(stack));
        player.displayClientMessage(Component.translatable(
                "chat.reinhardtshbm.cracking.changedto",
                Component.translatable(FluidIdentifierItem.primary(stack).translationKey())
        ), false);
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        for (CatalyticCrackerBlockEntity.Port port : CatalyticCrackerBlockEntity.portsFor(corePos, facing)) {
            BlockPos connectorPos = port.connectorPos();
            refreshDuctsAtPort(level, port.pos(), connectorPos);
        }
    }

    static void refreshDuctsAround(LevelAccessor level, BlockPos connectorPos) {
        refreshDuct(level, connectorPos);
        for (Direction direction : Direction.values()) {
            refreshDuct(level, connectorPos.relative(direction));
        }
    }

    public static void refreshDuctsAtPort(LevelAccessor level, BlockPos proxyPos, BlockPos connectorPos) {
        refreshDuct(level, proxyPos);
        refreshDuctsAround(level, connectorPos);
    }

    private static void refreshDuct(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluidDuctBlock duct) {
            duct.refreshConnections(level, pos);
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{0, 0, 3, 3, 2, 3});
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{8, -1, 3, -1, 2, 0});
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{13, 0, 0, 3, 2, 1});
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{14, -13, -1, 2, 1, 0});
        addLegacyFill(offsets, BlockPos.ZERO, new int[]{3, -1, 2, 3, -1, 3});
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }

    private static void addLegacyFill(Set<BlockPos> offsets, BlockPos anchor, int[] dim) {
        for (int x = anchor.getX() - dim[4]; x <= anchor.getX() + dim[5]; x++) {
            for (int y = anchor.getY() - dim[1]; y <= anchor.getY() + dim[0]; y++) {
                for (int z = anchor.getZ() - dim[2]; z <= anchor.getZ() + dim[3]; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}

