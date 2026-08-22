package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineBlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

import javax.annotation.Nullable;

public class MachineBlastFurnaceBlock extends LargeMachineBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int FOOTPRINT_HEIGHT = 7;
    public static final Footprint FOOTPRINT = Footprint.centered(1, FOOTPRINT_HEIGHT, 1);

    public MachineBlastFurnaceBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), RotationBasis.MODERN_NORTH);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlastFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.MACHINE_BLAST_FURNACE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> MachineBlastFurnaceBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (MachineBlastFurnaceBlockEntity) blockEntity
        );
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmBlocks.MACHINE_BLAST_FURNACE.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        level.addParticle(
                ParticleTypes.SMOKE,
                centerX + random.nextDouble() * 0.5D - 0.25D,
                pos.getY() + 7.0D,
                centerZ + random.nextDouble() * 0.5D - 0.25D,
                0.0D,
                0.02D,
                0.0D
        );
        if (random.nextInt(5) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    centerX + random.nextDouble() * 0.5D - 0.25D,
                    pos.getY() + 7.25D,
                    centerZ + random.nextDouble() * 0.5D - 0.25D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
        if (random.nextDouble() < 0.1D) {
            level.playLocalSound(centerX, pos.getY(), centerZ, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.25F, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}

