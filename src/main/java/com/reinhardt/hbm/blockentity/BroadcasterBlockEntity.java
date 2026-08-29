package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server behavior of the 1.7.10 TileEntityBroadcaster. */
public final class BroadcasterBlockEntity extends BlockEntity {
    private static final double CONFUSION_RADIUS = 25.0D;
    private static final double DAMAGE_RADIUS = 15.0D;

    public BroadcasterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.BROADCASTER.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (!(blockEntity instanceof BroadcasterBlockEntity broadcaster) || level.isClientSide) {
            return;
        }
        broadcaster.tickServer(level);
    }

    private void tickServer(Level level) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        AABB range = new AABB(center.x - CONFUSION_RADIUS, center.y - CONFUSION_RADIUS, center.z - CONFUSION_RADIUS,
                center.x + CONFUSION_RADIUS, center.y + CONFUSION_RADIUS, center.z + CONFUSION_RADIUS);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, range)) {
            double distance = entity.position().distanceTo(center);
            if (distance <= CONFUSION_RADIUS) {
                MobEffectInstance effect = entity.getEffect(MobEffects.CONFUSION);
                if (effect == null || effect.getDuration() < 100) {
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
                }
            }
            if (distance <= DAMAGE_RADIUS) {
                entity.hurt(entity.damageSources().source(HbmDamageTypes.BROADCAST),
                        (float) ((DAMAGE_RADIUS - distance) / DAMAGE_RADIUS * 10.0D));
            }
        }
    }
}
