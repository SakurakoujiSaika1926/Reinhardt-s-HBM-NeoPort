package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DecorationEmitterBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Server state and ray length calculation from 1.7.10 BlockEmitter.TileEntityEmitter. */
public final class DecorationEmitterBlockEntity extends BlockEntity {
    public static final int RANGE = 100;
    public static final int EFFECT_COUNT = 5;

    private int color;
    private int beam;
    private float girth = 0.5F;
    private int effect;

    public DecorationEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECO_EMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DecorationEmitterBlockEntity emitter) {
        if (!level.isClientSide && level.getGameTime() % 20L == 0L) {
            emitter.updateBeam(level, state);
        }
    }

    public int color() {
        return color;
    }

    public int beam() {
        return beam;
    }

    public float girth() {
        return girth;
    }

    public int effect() {
        return effect;
    }

    public void setColor(int color) {
        this.color = color;
        sync();
    }

    public void increaseGirth() {
        // The original only limits the lower bound while narrowing.
        this.girth += 0.125F;
        sync();
    }

    public void decreaseGirth() {
        this.girth = Math.max(0.125F, this.girth - 0.125F);
        sync();
    }

    public void cycleEffect() {
        this.effect = (this.effect + 1) % EFFECT_COUNT;
        sync();
    }

    private void updateBeam(Level level, BlockState state) {
        Direction direction = state.getValue(DecorationEmitterBlock.FACING);
        int previousBeam = this.beam;
        for (int distance = 1; distance <= RANGE; distance++) {
            this.beam = distance;
            BlockPos target = worldPosition.relative(direction, distance);
            BlockState targetState = level.getBlockState(target);
            if (targetState.isFaceSturdy(level, target, direction.getOpposite())) {
                break;
            }
        }
        if (this.beam != previousBeam) {
            sync();
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
        tag.putInt("color", color);
        tag.putInt("beam", beam);
        tag.putFloat("girth", girth);
        tag.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.getInt("color");
        beam = tag.getInt("beam");
        girth = Math.max(0.125F, tag.contains("girth") ? tag.getFloat("girth") : 0.5F);
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
