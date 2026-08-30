package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RadioboxBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** 1.7.10 TileEntityRadiobox: its energy sink and hostile-mob pulse are deliberately separate. */
public final class RadioboxBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final long MAX_POWER = 500_000L;
    private static final long ENERGY_PER_TICK = 25_000L;
    private static final double RANGE = 15.0D;
    private long power;
    private boolean infinite;

    public RadioboxBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADIOBOX.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioboxBlockEntity radiobox) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, radiobox);
        if (!radiobox.isActive() || (!radiobox.infinite && radiobox.power < ENERGY_PER_TICK)) {
            return;
        }
        if (!radiobox.infinite) {
            radiobox.power -= ENERGY_PER_TICK;
            radiobox.setChanged();
        }
        AABB area = new AABB(pos).inflate(RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory().isFriendly() == false)) {
            mob.hurt(level.damageSources().source(HbmDamageTypes.BROADCAST), 20.0F);
        }
    }

    public boolean isInfinite() {
        return infinite;
    }

    public boolean isActive() {
        return getBlockState().getValue(RadioboxBlock.ACTIVE);
    }

    public void enableInfinite() {
        this.infinite = true;
        sync();
    }

    public void toggleActive() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        level.setBlock(worldPosition, state.setValue(RadioboxBlock.ACTIVE, !state.getValue(RadioboxBlock.ACTIVE)), Block.UPDATE_ALL);
        setChanged();
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                worldPosition.above(), worldPosition.below(), worldPosition.north(),
                worldPosition.south(), worldPosition.west(), worldPosition.east());
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        power = Math.min(MAX_POWER, power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("tooltip.reinhardtshbm.radiobox.energy", power, MAX_POWER);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putBoolean("infinite", infinite);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.clamp(tag.getLong("power"), 0L, MAX_POWER);
        infinite = tag.getBoolean("infinite");
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
