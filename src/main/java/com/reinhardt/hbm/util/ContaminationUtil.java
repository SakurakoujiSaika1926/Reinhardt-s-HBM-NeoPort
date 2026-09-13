package com.reinhardt.hbm.util;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Small public compatibility surface for the legacy contamination helpers.
 *
 * <p>The 1.7.10 API exposed {@code ContaminationUtil.applyAsbestos(Entity,
 * int)} and several projectiles/compatibility modules used it directly.  The
 * modern attachment is intentionally kept behind this method so those
 * callers do not need to know about NeoForge data attachments.</p>
 */
public final class ContaminationUtil {
    private ContaminationUtil() {
    }

    /**
     * Applies asbestos exposure using the exact 1.7.10 guards: non-living
     * entities, creative players, and the first 200 player ticks are immune;
     * a protected head damages its installed filter instead of accumulating
     * asbestos.
     */
    public static void applyAsbestos(Entity entity, int amount) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player
                && (player.isCreative() || living.tickCount < 200)) {
            return;
        }

        if (HbmArmorProtection.hasHeadProtection(
                living, HbmArmorProtection.HazardClass.PARTICLE_FINE)) {
            HbmArmorProtection.damageInstalledFilter(living, amount);
            return;
        }

        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        hazards.addAsbestos(living, amount);
        HbmLivingHazards.set(living, hazards);
    }
}
