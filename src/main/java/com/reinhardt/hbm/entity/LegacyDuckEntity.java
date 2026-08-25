package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** Direct modern equivalent of the 1.7.10 EntityDuck. */
public final class LegacyDuckEntity extends Chicken {
    public LegacyDuckEntity(EntityType<? extends LegacyDuckEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.DUCC.get();
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return HbmEntityTypes.DUCK.get().create(level);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(getCombatTracker().getDeathMessage(), false);
        }
        super.die(source);
    }
}
