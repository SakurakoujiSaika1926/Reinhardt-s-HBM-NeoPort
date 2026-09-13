package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Collision-only part of the three-block floodlight footprint.
 *
 * This deliberately does not implement PowerEndpoint (or any other
 * capability-owning interface): only the center block is an electrical
 * endpoint.
 */
public final class FloodlightDummyBlockEntity extends BlockEntity {
    private static final String CORE_OFFSET_X = "HbmCoreOffsetX";
    private static final String CORE_OFFSET_Y = "HbmCoreOffsetY";
    private static final String CORE_OFFSET_Z = "HbmCoreOffsetZ";
    private BlockPos corePos = BlockPos.ZERO;
    private boolean dropCore;

    public FloodlightDummyBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FLOODLIGHT_DUMMY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FloodlightDummyBlockEntity dummy) {
        if (!level.isClientSide
                && !(level.getBlockEntity(dummy.corePos) instanceof FloodlightBlockEntity)) {
            level.removeBlock(pos, false);
        }
    }

    public BlockPos corePos() {
        return corePos;
    }

    public BlockPos coreOffset() {
        return corePos.subtract(worldPosition);
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
    }

    public void setCoreOffset(BlockPos offset) {
        setCorePos(worldPosition.offset(offset));
    }

    public void setDropCore(boolean dropCore) {
        this.dropCore = dropCore;
    }

    public boolean consumeDropCore() {
        boolean result = dropCore;
        dropCore = false;
        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", corePos.getX());
        tag.putInt("CoreY", corePos.getY());
        tag.putInt("CoreZ", corePos.getZ());
        BlockPos offset = corePos.subtract(worldPosition);
        tag.putInt(CORE_OFFSET_X, offset.getX());
        tag.putInt(CORE_OFFSET_Y, offset.getY());
        tag.putInt(CORE_OFFSET_Z, offset.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(CORE_OFFSET_X) && tag.contains(CORE_OFFSET_Y) && tag.contains(CORE_OFFSET_Z)) {
            corePos = worldPosition.offset(
                    tag.getInt(CORE_OFFSET_X), tag.getInt(CORE_OFFSET_Y), tag.getInt(CORE_OFFSET_Z));
        } else {
            corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        }
    }
}
