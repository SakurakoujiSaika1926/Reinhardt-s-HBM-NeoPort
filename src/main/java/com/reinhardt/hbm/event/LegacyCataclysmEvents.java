package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.LegacyBurningFoeqEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** GeneralConfig.enableCataclysm death hook from the latest 1.7.10 source. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class LegacyCataclysmEvents {
    private LegacyCataclysmEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!HbmConfig.ENABLE_CATACLYSM.get()
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LegacyBurningFoeqEntity foeq = new LegacyBurningFoeqEntity(
                HbmEntityTypes.LEGACY_BURNING_FOEQ.get(), level);
        foeq.setPos(event.getEntity().getX(), 500.0D, event.getEntity().getZ());
        foeq.setYRot(0.0F);
        foeq.setXRot(0.0F);
        level.addFreshEntity(foeq);
    }
}
