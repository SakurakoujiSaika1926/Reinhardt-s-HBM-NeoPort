package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.RadiationEvents;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps nuclear blast damage as real health damage against generic and
 * third-party defenses. HBM's own shield, armor-insert, armor-mod and FSB
 * protection chain is deliberately applied once before the final value is
 * locked, while the normal death pipeline and creative/spectator exemption
 * remain intact.
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class NuclearTrueDamageEvents {
    private static final Map<DamageContainer, Float> DAMAGE_AFTER_HBM_PROTECTION =
            Collections.synchronizedMap(new WeakHashMap<>());

    private NuclearTrueDamageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        Entity entity = event.getEntity();
        if (!event.getSource().is(HbmDamageTypes.NUCLEAR_BLAST)
                || entity.isRemoved()
                || isCreativeOrSpectator(entity)) {
            return;
        }
        event.setInvulnerable(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(HbmDamageTypes.NUCLEAR_BLAST)
                || isCreativeOrSpectator(event.getEntity())) {
            return;
        }

        event.setCanceled(false);
        event.setAmount(event.getOriginalAmount());
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player) {
            RadiationEvents.applyHbmDamageProtection(player, event);
        }
        float damageAfterHbmProtection = Math.max(0.0F, event.getAmount());
        event.setAmount(damageAfterHbmProtection);
        DAMAGE_AFTER_HBM_PROTECTION.put(event.getContainer(), damageAfterHbmProtection);
        event.setInvulnerabilityTicks(0);
        for (DamageContainer.Reduction reduction : DamageContainer.Reduction.values()) {
            event.addReductionModifier(reduction, (container, amount) -> 0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getDamageSource().is(HbmDamageTypes.NUCLEAR_BLAST)
                || isCreativeOrSpectator(event.getEntity())) {
            return;
        }
        event.setBlocked(false);
        event.setBlockedDamage(0.0F);
        event.setShieldDamage(0.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!event.getSource().is(HbmDamageTypes.NUCLEAR_BLAST)
                || isCreativeOrSpectator(event.getEntity())) {
            return;
        }
        Float protectedDamage = DAMAGE_AFTER_HBM_PROTECTION.remove(event.getContainer());
        float finalDamage = protectedDamage == null ? event.getOriginalDamage() : protectedDamage;
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player) {
            finalDamage = RadiationEvents.applyHbmArmorProtection(player, event.getSource(), finalDamage);
        }
        event.setNewDamage(finalDamage);
    }

    private static boolean isCreativeOrSpectator(Entity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }
}
