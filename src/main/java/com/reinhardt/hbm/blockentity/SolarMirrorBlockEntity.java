package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SolarMirrorBlockEntity extends BlockEntity {
    private BlockPos target = BlockPos.ZERO;
    private boolean on;

    public SolarMirrorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOLAR_MIRROR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SolarMirrorBlockEntity mirror) {
        if (level.isClientSide) {
            return;
        }
        mirror.tickServer(level);
    }

    public BlockPos target() {
        return this.target;
    }

    public boolean isOn() {
        return this.on;
    }

    public void setTarget(BlockPos target) {
        this.target = target.immutable();
        setChanged();
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("targetX", this.target.getX());
        tag.putInt("targetY", this.target.getY());
        tag.putInt("targetZ", this.target.getZ());
        tag.putBoolean("On", this.on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.target = new BlockPos(tag.getInt("targetX"), tag.getInt("targetY"), tag.getInt("targetZ"));
        this.on = tag.getBoolean("On");
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

    private void tickServer(Level level) {
        boolean wasOn = this.on;
        this.on = false;

        if (this.target.getY() >= this.worldPosition.getY()) {
            int sun = level.getBrightness(LightLayer.SKY, this.worldPosition) - level.getSkyDarken() - 11;
            if (sun > 0 && level.canSeeSky(this.worldPosition.above())) {
                this.on = true;
                BlockEntity targetEntity = level.getBlockEntity(this.target.below());
                if (targetEntity instanceof SolarBoilerBlockEntity boiler) {
                    boiler.addHeatInput(sun);
                }
            }
        }

        if (wasOn != this.on || level.getGameTime() % 20L == 0L) {
            sync();
        }
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
