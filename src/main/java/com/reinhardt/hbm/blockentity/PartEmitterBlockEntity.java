package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Direct port of the four PartEmitter effect modes and their legacy parameters. */
public final class PartEmitterBlockEntity extends BlockEntity {
    public static final int RANGE = 150;
    public static final int EFFECT_COUNT = 4;

    private int effect;

    public PartEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PART_EMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PartEmitterBlockEntity emitter) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            emitter.tickServer(serverLevel);
        }
    }

    public void cycleEffect() {
        effect = (effect + 1) % EFFECT_COUNT;
        sync();
    }

    private void tickServer(ServerLevel level) {
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;

        if (effect == 1) {
            level.sendParticles(HbmParticleTypes.GEYSER_FIRE.get(),
                    worldPosition.getX() + level.random.nextDouble(),
                    worldPosition.getY() + 4.5D + level.random.nextDouble(),
                    worldPosition.getZ() + level.random.nextDouble(),
                    0, level.random.nextGaussian() * 0.2D, 0.1D,
                    level.random.nextGaussian() * 0.2D, 1.0D);
        } else if (effect == 2) {
            level.sendParticles(HbmParticleTypes.PART_EMITTER_TOWER_SMALL.get(), x, y, z,
                    0, 0.0D, 0.0D, 0.0D, 1.0D);
        } else if (effect == 3) {
            level.sendParticles(HbmParticleTypes.PART_EMITTER_TOWER_LARGE.get(),
                    x + level.random.nextDouble() * 3.0D - 1.5D,
                    worldPosition.getY() + 1.0D,
                    z + level.random.nextDouble() * 3.0D - 1.5D,
                    0, 0.0D, 0.0D, 0.0D, 1.0D);
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        effect = Math.floorMod(tag.getInt("effect"), EFFECT_COUNT);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
