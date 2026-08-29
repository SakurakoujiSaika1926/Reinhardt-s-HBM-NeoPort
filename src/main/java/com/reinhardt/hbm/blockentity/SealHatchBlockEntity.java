package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.block.SealControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

public final class SealHatchBlockEntity extends BlockEntity {
    private BlockPos controllerPos;

    public SealHatchBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SEAL_HATCH.get(), pos, state);
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos.immutable();
        setChanged();
    }

    public BlockPos controllerPos() {
        return this.controllerPos;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SealHatchBlockEntity hatch) {
        if (hatch.controllerPos == null || !SealControllerBlock.hasValidFrame(level, hatch.controllerPos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.controllerPos != null) {
            tag.putLong("controller", this.controllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.controllerPos = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
    }
}
