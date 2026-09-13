package com.reinhardt.hbm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** EntityItemBuoyant: legacy plastic-bag drops rise in water. */
public final class LegacyBuoyantItemEntity extends ItemEntity {
    public LegacyBuoyantItemEntity(EntityType<? extends LegacyBuoyantItemEntity> type, Level level) {
        super(type, level);
    }

    public LegacyBuoyantItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(com.reinhardt.hbm.registry.HbmEntityTypes.BUOYANT_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }

    @Override
    public void tick() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.0625D, getZ());
        // 1.7.10 checked water metadata < 8: source and flowing water
        // floated the item, while falling-water metadata 8..15 did not.
        var fluidState = level().getFluidState(below);
        // The 1.7.10 implementation accepted water metadata 0..7 and
        // rejected falling-water metadata 8..15.  Modern FluidState does
        // not expose a falling flag, but its legacy block representation
        // retains the same LEVEL value.
        if (fluidState.is(FluidTags.WATER)
                && fluidState.createLegacyBlock().hasProperty(LiquidBlock.LEVEL)
                && fluidState.createLegacyBlock().getValue(LiquidBlock.LEVEL) < 8) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.045D, 0.0D));
        }
        super.tick();
    }
}
