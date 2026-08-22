package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerDetectorBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class PowerDetectorBlockEntity extends BlockEntity implements PowerEndpoint {
    private long power;

    public PowerDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.POWER_DETECTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerDetectorBlockEntity detector) {
        if (level.isClientSide) return;
        PowerNetworkManager.tickFromEndpoint(level, detector);
        boolean lit = detector.power > 0;
        if (lit) detector.power--;
        if (state.getValue(PowerDetectorBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PowerDetectorBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override public BlockPos getPowerPos() { return worldPosition; }
    @Override public long getAvailableOutput() { return 0; }
    @Override public long getRequestedInput() { return power < 5 ? 5 - power : 0; }
    @Override public void applyPower(long usedOutput, long receivedInput) { power = Math.min(5, power + receivedInput); setChanged(); }
    @Override public PowerEndpoint.ConnectionPriority getPowerPriority() { return PowerEndpoint.ConnectionPriority.HIGH; }
    @Override public Component getPowerStatus() { return Component.literal(power + " / 5 HE"); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putLong("Power", power); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); power = Math.max(0, Math.min(5, tag.getLong("Power"))); }
}
