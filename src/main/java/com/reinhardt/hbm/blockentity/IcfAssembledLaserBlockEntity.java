package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.IcfAssembledLaserBlock;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stores the exact raw ICF part and controller position while it is assembled. */
public final class IcfAssembledLaserBlockEntity extends BlockEntity implements PowerGraphNode {
    private Block originalBlock = HbmBlocks.ICF_LASER_COMPONENT.get();
    private int originalVariant;
    private BlockPos controllerPos = BlockPos.ZERO;

    public IcfAssembledLaserBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_ASSEMBLED_LASER.get(), pos, state);
    }

    public void configure(Block originalBlock, int originalVariant, BlockPos controllerPos) {
        this.originalBlock = originalBlock;
        this.originalVariant = originalVariant;
        this.controllerPos = controllerPos.immutable();
        setChanged();
    }

    public void restoreOriginal() {
        if (this.level == null || this.level.isClientSide || this.originalBlock == null) {
            return;
        }
        BlockState restored = this.originalBlock.defaultBlockState();
        if (restored.hasProperty(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT)) {
            restored = restored.setValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT, this.originalVariant);
        }
        this.level.setBlock(this.worldPosition, restored, Block.UPDATE_ALL);
        if (this.level.getBlockEntity(this.controllerPos) instanceof IcfControllerBlockEntity controller) {
            controller.setAssembled(false);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfAssembledLaserBlockEntity entity) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!(level.getBlockEntity(entity.controllerPos) instanceof IcfControllerBlockEntity controller)
                || !controller.assembled()) {
            entity.restoreOriginal();
        }
    }

    @Override
    public BlockPos getGraphPos() {
        return this.worldPosition;
    }

    @Override
    public boolean isPowerGraphEnabled(LevelAccessor level) {
        return getBlockState().getValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT) == 1
                && level.getBlockEntity(controllerPos) instanceof IcfControllerBlockEntity controller
                && controller.assembled();
    }

    @Override
    public java.util.List<BlockPos> getRemotePowerLinks(Level level) {
        return isPowerGraphEnabled(level) ? java.util.List.of(controllerPos) : java.util.List.of();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("OriginalBlock", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(originalBlock).toString());
        tag.putInt("OriginalVariant", originalVariant);
        tag.putInt("ControllerX", controllerPos.getX());
        tag.putInt("ControllerY", controllerPos.getY());
        tag.putInt("ControllerZ", controllerPos.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        originalBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                net.minecraft.resources.ResourceLocation.parse(tag.getString("OriginalBlock"))).orElse(HbmBlocks.ICF_LASER_COMPONENT.get());
        originalVariant = tag.getInt("OriginalVariant");
        controllerPos = new BlockPos(tag.getInt("ControllerX"), tag.getInt("ControllerY"), tag.getInt("ControllerZ"));
    }
}
