package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Exact two-tick linear falloff from TileEntityCrashedBomb. */
public final class CrashedBombBlockEntity extends BlockEntity {
    public CrashedBombBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CRASHED_BOMB.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrashedBombBlockEntity ignored) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 2L != 0L) {
            return;
        }
        CrashedBombBlock.Type type = CrashedBombBlock.type(state);
        double range = switch (type) {
            case BALEFIRE -> 15.0D;
            case NUKE, SALTED -> 10.0D;
            default -> 0.0D;
        };
        float multiplier = switch (type) {
            case BALEFIRE -> 1.0F;
            case NUKE -> 0.25F;
            case SALTED -> 0.5F;
            default -> 0.0F;
        };
        if (range <= 0.0D) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range))) {
            Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
            double distance = center.distanceTo(target);
            if (distance > range) {
                continue;
            }
            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addRadiationWithReadout((float) ((1.0D - distance / range) * multiplier));
            HbmLivingRadiation.set(entity, radiation);
        }
    }
}
