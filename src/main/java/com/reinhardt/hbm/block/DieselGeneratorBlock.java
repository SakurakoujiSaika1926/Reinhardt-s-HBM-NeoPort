package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
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

public class DieselGeneratorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public DieselGeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DieselGeneratorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DieselGeneratorBlockEntity diesel)
                || !diesel.hasAcceptableFuel()
                || diesel.fuelTank().amount() <= 0) {
            return;
        }
        Direction facing = state.getValue(FACING);
        Direction side = facing.getClockWise();
        level.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5D - facing.getStepX() * 0.6D + side.getStepX() * 0.1875D,
                pos.getY() + 0.3125D,
                pos.getZ() + 0.5D - facing.getStepZ() * 0.6D + side.getStepZ() * 0.1875D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.diesel.fuel_efficiency").withStyle(ChatFormatting.YELLOW));
        for (CombustibleFuelGrade grade : List.of(CombustibleFuelGrade.MEDIUM, CombustibleFuelGrade.HIGH, CombustibleFuelGrade.AERO)) {
            int efficiency = (int) Math.round(DieselGeneratorBlockEntity.fuelEfficiency(grade) * 100.0D);
            tooltip.add(Component.translatable(
                    "tooltip.reinhardtshbm.diesel.grade",
                    Component.translatable(grade.translationKey()).withStyle(ChatFormatting.YELLOW),
                    Component.literal(efficiency + "%").withStyle(ChatFormatting.RED)
            ));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.DIESEL_GENERATOR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> DieselGeneratorBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (DieselGeneratorBlockEntity) blockEntity
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        builder.add(FACING, LIT);
    }
}
