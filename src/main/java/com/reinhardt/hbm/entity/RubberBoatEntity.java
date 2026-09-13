package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10's rubber boat, using the native boat physics and passenger protocol. */
public final class RubberBoatEntity extends Boat {
    public RubberBoatEntity(EntityType<? extends RubberBoatEntity> type, Level level) {
        super(type, level);
        setVariant(Type.OAK);
    }

    public RubberBoatEntity(Level level, double x, double y, double z) {
        this(HbmEntityTypes.RUBBER_BOAT.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public Item getDropItem() {
        return HbmItems.BOAT_RUBBER.get();
    }

    /**
     * ItemBoatRubber's entity survived falls up to five blocks and dropped
     * itself when destroyed. Vanilla boats use a three-block threshold and
     * split into planks, so keep the legacy behavior here.
     */
    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (isPassenger()) {
            return;
        }

        if (onGround) {
            if (fallDistance > 5.0F) {
                causeFallDamage(fallDistance, 1.0F, damageSources().fall());
                if (!level().isClientSide && !isRemoved()) {
                    kill();
                    spawnAtLocation(getDropItem());
                }
            }
            resetFallDistance();
        } else if (!level().getFluidState(blockPosition().below()).is(FluidTags.WATER) && y < 0.0D) {
            fallDistance -= (float) y;
        }
    }

    /** EntityBoatRubber#canTriggerWalking returned false in 1.7.10. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
