package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PwrBlockEntity extends BlockEntity {
    private BlockState originalState = Blocks.AIR.defaultBlockState();
    private BlockPos corePos = BlockPos.ZERO;
    private boolean assembledPart;
    private PwrBlock.Kind originalKind = PwrBlock.Kind.BLOCK;

    public PwrBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.PWR_PART.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PwrBlockEntity part) {
        if (level.isClientSide || !part.assembledPart || level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!(level.getBlockEntity(part.corePos) instanceof PwrControllerBlockEntity controller) || !controller.assembled()) {
            part.restoreOriginal();
        }
    }

    public void bind(BlockState originalState, BlockPos corePos, PwrBlock.Kind originalKind) {
        this.originalState = originalState;
        this.corePos = corePos.immutable();
        this.originalKind = originalKind;
        this.assembledPart = true;
        setChanged();
    }

    public boolean isPort() {
        return assembledPart && originalKind == PwrBlock.Kind.PORT;
    }

    public PwrBlock.Kind originalKind() {
        return originalKind;
    }

    public PwrControllerBlockEntity controller() {
        if (level != null && level.getBlockEntity(corePos) instanceof PwrControllerBlockEntity controller) {
            return controller;
        }
        return null;
    }

    public void restoreOriginalAndInvalidateCore() {
        if (level != null && level.getBlockEntity(corePos) instanceof PwrControllerBlockEntity controller) {
            controller.invalidateAssemblyOnly();
        }
        restoreOriginal();
    }

    public void restoreOriginal() {
        if (level == null || level.isClientSide || !assembledPart) {
            return;
        }
        BlockState restore = originalState == null || originalState.isAir() ? Blocks.AIR.defaultBlockState() : originalState;
        assembledPart = false;
        level.setBlock(worldPosition, restore, 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("assembledPart", assembledPart);
        tag.putLong("corePos", corePos.asLong());
        tag.put("originalState", NbtUtils.writeBlockState(originalState));
        tag.putString("originalKind", originalKind.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        assembledPart = tag.getBoolean("assembledPart");
        corePos = BlockPos.of(tag.getLong("corePos"));
        originalState = tag.contains("originalState")
                ? NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("originalState"))
                : Blocks.AIR.defaultBlockState();
        try {
            originalKind = PwrBlock.Kind.valueOf(tag.getString("originalKind"));
        } catch (IllegalArgumentException ignored) {
            originalKind = PwrBlock.Kind.BLOCK;
        }
    }
}
