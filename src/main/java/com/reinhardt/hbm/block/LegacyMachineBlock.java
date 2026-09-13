package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * Shared structural block for legacy 1.7.10 machines whose complete geometry
 * is supplied by an OBJ model. The footprint is the old BlockDummyable volume,
 * not an approximation of the rendered mesh.
 */
public class LegacyMachineBlock extends LargeMachineBlock implements EntityBlock {
    private static final DustParticleOptions FORCEFIELD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F);

    public LegacyMachineBlock(BlockBehaviour.Properties properties, Footprint footprint, VoxelShape coreShape) {
        super(properties, footprint, coreShape, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LEGACY_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) -> {
            if (entity instanceof LegacyMachineBlockEntity machine) {
                LegacyMachineBlockEntity.tick(tickerLevel, tickerPos, tickerState, machine);
            }
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!BuiltInRegistries.BLOCK.getKey(this).getPath().equals("machine_forcefield")
                || !(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine)) {
            return;
        }
        if (machine.forcefieldCooldown() > 0) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.SMOKE,
                        pos.getX() + random.nextFloat(), pos.getY() + 2.0D,
                        pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        } else if (machine.forcefieldRenderable()) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(machine.forcefieldColor() == 0xFF0000 ? ParticleTypes.LAVA : FORCEFIELD_DUST,
                        pos.getX() + random.nextFloat(), pos.getY() + 2.0D,
                        pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        markFbiTarget(level, player, machine);
        if ((machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                && pos.getY() < HbmConfig.RADAR_ALTITUDE.get()) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.radar.altitude")
                        .withStyle(ChatFormatting.RED), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // MachineOrbus uses crouching right-click as a non-GUI interaction.
        if (player.isCrouching()) {
            return machine.machineId().equals("machine_orbus")
                    ? InteractionResult.sidedSuccess(level.isClientSide)
                    : InteractionResult.PASS;
        }
        if (machine.handleEmptyHandInteraction(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!machine.hasMenu()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menu, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                ? machine.radarRedPower() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && machine.handleItemInteraction(player, stack)) {
            markFbiTarget(level, player, machine);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine) {
            markFbiTarget(level, player, machine);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void markFbiTarget(Level level, Player player, LegacyMachineBlockEntity machine) {
        if (!level.isClientSide && !player.isCrouching() && player instanceof ServerPlayer serverPlayer
                && (machine.machineId().equals("machine_missile_assembly")
                || machine.machineId().equals("machine_radiolysis"))) {
            LegacyMobSpawnEvents.markFbi(serverPlayer);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
