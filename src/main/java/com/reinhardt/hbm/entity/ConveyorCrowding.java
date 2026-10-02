package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Shared modern equivalent of EntityMovingConveyorObject's cram protection. */
final class ConveyorCrowding {
    private ConveyorCrowding() {
    }

    static boolean check(Entity self) {
        if ((self.tickCount + self.getId()) % 400 != 0) {
            return false;
        }

        List<Entity> crowded = self.level().getEntities(
                self,
                self.getBoundingBox().inflate(0.125D),
                ConveyorCrowding::isMovingConveyorObject
        );
        if (crowded.size() + 1 < HbmConfig.CONVEYOR_CRAM_MAX.get()) {
            return false;
        }

        for (Entity entity : crowded) {
            entity.discard();
        }
        self.discard();
        self.level().explode(
                self,
                self.getX(), self.getY() + 0.125D, self.getZ(),
                1.0F,
                Level.ExplosionInteraction.NONE
        );

        if (self.tickCount > 400
                && HbmConfig.CONVEYOR_CRAM_EXPLODE.get()
                && self.level().getBlockState(self.blockPosition()).getBlock() instanceof ConveyorBlock) {
            self.level().destroyBlock(self.blockPosition(), false);
        }
        return true;
    }

    private static boolean isMovingConveyorObject(Entity entity) {
        return entity instanceof ConveyorMovingItem || entity instanceof ConveyorMovingPackage;
    }
}
