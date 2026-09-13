package com.reinhardt.hbm.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Direct 1.7.10 EntityGhost behaviour, including its deliberately short lifetime. */
public final class LegacyGhostEntity extends PathfinderMob {
    private static final double DESPAWN_RANGE = 50.0D;

    public LegacyGhostEntity(EntityType<? extends LegacyGhostEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel
                && !serverLevel.getEntitiesOfClass(Player.class, getBoundingBox().inflate(DESPAWN_RANGE)).isEmpty()) {
            discard();
        }
    }

    /** EntityGhost#setHealth: every direct health assignment restores full health. */
    @Override
    public void setHealth(float health) {
        super.setHealth(getMaxHealth());
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        // EntityGhost#isEntityInvulnerable returned true for every damage
        // source in 1.7.10.
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // EntityGhost multiplies the legacy render-distance weight by 10.
        // EntityGhost uses the default 0.6 x 1.8 box and multiplies the
        // default render-distance weight by ten.  The old renderer uses the
        // average edge length (1.2) before applying 64 * weight.
        return distance < 589824.0D;
    }
}
