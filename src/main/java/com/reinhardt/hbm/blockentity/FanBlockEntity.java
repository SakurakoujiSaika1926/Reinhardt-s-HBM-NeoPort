package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FanBlock;
import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public final class FanBlockEntity extends BlockEntity {
    private boolean falloff = true;
    private boolean suck;
    private float spin;
    private float previousSpin;

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FAN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FanBlockEntity fan) {
        if (level.isClientSide) {
            fan.previousSpin = fan.spin;
            if (level.hasNeighborSignal(pos)) {
                fan.spin += 30.0F;
                fan.wrapSpin();
                if (level.random.nextInt(30) == 0) {
                    Direction direction = state.getValue(FanBlock.FACING);
                    double velocity = fan.suck ? -0.2D : 0.2D;
                    level.addParticle(ParticleTypes.CLOUD,
                            pos.getX() + 0.5D + direction.getStepX() * 0.5D,
                            pos.getY() + 0.5D + direction.getStepY() * 0.5D,
                            pos.getZ() + 0.5D + direction.getStepZ() * 0.5D,
                            direction.getStepX() * velocity,
                             direction.getStepY() * velocity,
                             direction.getStepZ() * velocity);
                }
            }
            return;
        }

        if (level.hasNeighborSignal(pos)) {
            fan.applyForce(level, pos, state);
        }
        fan.setChanged();
    }

    private void applyForce(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FanBlock.FACING);
        int effectiveRange = 0;
        for (int distance = 1; distance <= 10; distance++) {
            BlockPos target = pos.relative(direction, distance);
            BlockState targetState = level.getBlockState(target);
            boolean blowable = targetState.getBlock() instanceof PileGraphiteBlock graphite
                    && graphite.kind() == PileGraphiteBlock.Kind.FUEL;
            if (targetState.isCollisionShapeFullBlock(level, target) || blowable) {
                if (blowable) {
                    PileGraphiteBlock.applyFan(level, target, direction, distance);
                }
                break;
            }
            effectiveRange = distance;
        }

        if (effectiveRange <= 0) {
            return;
        }

        int dx = direction.getStepX() * effectiveRange;
        int dy = direction.getStepY() * effectiveRange;
        int dz = direction.getStepZ() * effectiveRange;
        AABB affectedBox = new AABB(
                pos.getX() + 0.5D + Math.min(dx, 0),
                pos.getY() + 0.5D + Math.min(dy, 0),
                pos.getZ() + 0.5D + Math.min(dz, 0),
                pos.getX() + 0.5D + Math.max(dx, 0),
                pos.getY() + 0.5D + Math.max(dy, 0),
                pos.getZ() + 0.5D + Math.max(dz, 0)
        ).inflate(0.5D);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, affectedBox)) {
            double coefficient = 0.1D;
            if (falloff) {
                double distance = entity.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                coefficient *= 1.5D * (1.0D - Math.sqrt(distance) / 10.0D / 2.0D);
            }
            if (suck) {
                coefficient *= -1.0D;
            }
            entity.push(direction.getStepX() * coefficient, direction.getStepY() * coefficient,
                    direction.getStepZ() * coefficient);
        }
    }

    private void wrapSpin() {
        if (spin >= 360.0F) {
            spin -= 360.0F;
            previousSpin -= 360.0F;
        }
    }

    public float spin(float partialTick) {
        return previousSpin + (spin - previousSpin) * partialTick;
    }

    public boolean falloff() {
        return falloff;
    }

    public boolean suck() {
        return suck;
    }

    public void setFalloff(boolean value) {
        falloff = value;
        sync();
    }

    public void setSuck(boolean value) {
        suck = value;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Falloff", falloff);
        tag.putBoolean("Suck", suck);
        tag.putFloat("Spin", spin);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        falloff = tag.getBoolean("Falloff");
        suck = tag.getBoolean("Suck");
        spin = tag.getFloat("Spin");
        previousSpin = spin;
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
