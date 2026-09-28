package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileMk2Block;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Current-upstream Chicago Pile MK2 control-rod actuator. */
public final class PileControlBlockEntity extends PileMk2DeviceBlockEntity {
    private static final double SPEED = 1.0D / 60.0D;

    private double insertionLevel;
    private double previousLevel;
    private double synchronizedLevel;
    private int interpolationTicks;
    private double targetLevel;
    private boolean wasRedstone;

    public PileControlBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PILE_MK2_CONTROL.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileControlBlockEntity control) {
        if (level.isClientSide) {
            control.previousLevel = control.insertionLevel;
            if (control.interpolationTicks > 0) {
                control.insertionLevel += (control.synchronizedLevel - control.insertionLevel) / control.interpolationTicks;
                control.interpolationTicks--;
            } else {
                control.insertionLevel = control.synchronizedLevel;
            }
            return;
        }
        control.tickServer();
    }

    private void tickServer() {
        if (level == null) return;
        boolean canMove = false;
        BlockPos entry = worldPosition.below();
        BlockState entryState = level.getBlockState(entry);
        if (entryState.getBlock() instanceof PileMk2Block
                && entryState.getValue(PileMk2Block.KIND) == PileMk2Block.Kind.CONTROL) {
            PileMk2CoreBlockEntity core = resolveCore(entry);
            if (core != null) {
                PileMk2CoreBlockEntity.PileChannel channel = core.getControlChannel(entry);
                if (channel != null) {
                    canMove = true;
                    channelNumber = core.getControlChannelNumber(channel);
                    channel.setControl(insertionLevel);
                }
            }
        }
        if (!canMove) channelNumber = -1;

        boolean moved = false;
        if (canMove && insertionLevel != targetLevel) {
            if (Math.abs(insertionLevel - targetLevel) <= SPEED) insertionLevel = targetLevel;
            else if (insertionLevel < targetLevel) insertionLevel += SPEED;
            else insertionLevel -= SPEED;
            moved = true;
        }

        Direction outward = facing();
        BlockPos signalPos = worldPosition.relative(outward);
        boolean redstone = this.level.getSignal(signalPos, outward.getOpposite()) > 0;
        if (redstone && !wasRedstone) targetLevel = 1.0D;
        if (!redstone && wasRedstone) targetLevel = 0.0D;
        wasRedstone = redstone;

        if (moved || this.level.getGameTime() % 100L == 0L) sync();
    }

    public void setTargetLevel(double target) {
        targetLevel = Mth.clamp(target, 0.0D, 1.0D);
        sync();
    }

    public double level() {
        return insertionLevel;
    }

    public double targetLevel() {
        return targetLevel;
    }

    public double renderedLevel(float partialTick) {
        return Mth.lerp(partialTick, previousLevel, insertionLevel);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Level", insertionLevel);
        tag.putDouble("TargetLevel", targetLevel);
        tag.putBoolean("Redstone", wasRedstone);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        double loadedLevel = Mth.clamp(tag.getDouble("Level"), 0.0D, 1.0D);
        if (this.insertionLevel != loadedLevel) {
            synchronizedLevel = loadedLevel;
            interpolationTicks = 2;
            if (this.level == null || !this.level.isClientSide) this.insertionLevel = loadedLevel;
        }
        targetLevel = Mth.clamp(tag.getDouble("TargetLevel"), 0.0D, 1.0D);
        wasRedstone = tag.getBoolean("Redstone");
    }
}
