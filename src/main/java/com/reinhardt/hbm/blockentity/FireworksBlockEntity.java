package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.FireworksEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent state and redstone sequencer for the 1.7.10 fireworks battery. */
public final class FireworksBlockEntity extends BlockEntity {
    private static final int DEFAULT_COLOR = 0xff0000;
    private static final String DEFAULT_MESSAGE = "NUCLEAR TECH";

    private int color = DEFAULT_COLOR;
    private String message = DEFAULT_MESSAGE;
    private int charges;
    private int index;
    private int delay;

    public FireworksBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FIREWORKS.get(), pos, state);
    }

    public int color() {
        return color;
    }

    public String message() {
        return message;
    }

    public int charges() {
        return charges;
    }

    public void setColor(int color) {
        this.color = color & 0xffffff;
        setChanged();
    }

    public void setMessage(String message) {
        this.message = message;
        setChanged();
    }

    public void addCharges(int amount) {
        this.charges = Math.max(0, this.charges + amount);
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FireworksBlockEntity fireworks) {
        if (level.isClientSide) {
            return;
        }

        if (level.hasNeighborSignal(pos) && !fireworks.message.isEmpty() && fireworks.charges > 0) {
            fireworks.delay--;
            if (fireworks.delay <= 0) {
                fireworks.delay = 30;
                int mod = fireworks.index % 9;
                double offX = (mod / 3 - 1) * 0.3125D;
                double offZ = (mod % 3 - 1) * 0.3125D;
                FireworksEntity entity = new FireworksEntity(
                        HbmEntityTypes.FIREWORKS.get(),
                        level,
                        pos.getX() + 0.5D + offX,
                        pos.getY() + 1.5D,
                        pos.getZ() + 0.5D + offZ,
                        fireworks.color,
                        fireworks.message.charAt(fireworks.index));
                level.addFreshEntity(entity);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        HbmSoundEvents.WEAPON_ROCKET_FLAME.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 3.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5D + offX,
                            pos.getY() + 1.125D,
                            pos.getZ() + 0.5D + offZ,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                fireworks.charges--;
                fireworks.index++;
                fireworks.setChanged();
                if (fireworks.index >= fireworks.message.length()) {
                    fireworks.index = 0;
                    fireworks.delay = 100;
                }
            }
        } else {
            fireworks.delay = 0;
            fireworks.index = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Color", color);
        tag.putString("Message", message);
        tag.putInt("Charges", charges);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.contains("Color") ? tag.getInt("Color") & 0xffffff : DEFAULT_COLOR;
        message = tag.contains("Message") ? tag.getString("Message") : DEFAULT_MESSAGE;
        charges = Math.max(0, tag.getInt("Charges"));
        index = 0;
        delay = 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        index = 0;
        delay = 0;
    }
}
