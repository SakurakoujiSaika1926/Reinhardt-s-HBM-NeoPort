package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WallChargeBlockEntity extends BlockEntity {
    private int timer;
    private boolean started;

    public WallChargeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WALL_CHARGE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WallChargeBlockEntity charge) {
        if (level.isClientSide || !charge.started) {
            return;
        }
        charge.timer--;
        if (charge.timer > 0 && charge.timer % 20 == 0) {
            level.playSound(null, pos, HbmSoundEvents.CHARGE_BEEP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        charge.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        if (charge.timer <= 0 && level instanceof ServerLevel server) {
            charge.started = false;
            WallChargeExplosions.detonate(server, pos, charge.kind());
        }
    }

    public WallChargeBlock.Kind kind() {
        return ((WallChargeBlock) getBlockState().getBlock()).kind();
    }

    public int timer() { return timer; }
    public boolean started() { return started; }

    public void cycleTimer() {
        timer = switch (timer) {
            case 0 -> 100;
            case 100 -> 200;
            case 200 -> 300;
            case 300 -> 600;
            case 600 -> 1200;
            case 1200 -> 3600;
            case 3600 -> 6000;
            default -> 0;
        };
        sync();
    }

    public void arm(int fuse) {
        if (fuse > 0) {
            timer = fuse;
            started = true;
            sync();
        }
    }

    public void disarm() {
        started = false;
        sync();
        if (level != null) {
            level.playSound(null, worldPosition, HbmSoundEvents.CHARGE_START.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    public String minutes() {
        return String.format("%02d", timer / 1200);
    }

    public String seconds() {
        return String.format("%02d", (timer / 20) % 60);
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Timer", timer);
        tag.putBoolean("Started", started);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        timer = tag.getInt("Timer");
        started = tag.getBoolean("Started");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
