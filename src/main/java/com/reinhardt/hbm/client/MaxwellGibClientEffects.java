package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.MaxwellGibEffectPayload;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class MaxwellGibClientEffects {
    private static final Map<Integer, Long> HIDDEN_ENTITIES = new HashMap<>();
    private static ClientLevel trackedLevel;

    private MaxwellGibClientEffects() {
    }

    public static void accept(MaxwellGibEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ensureLevel(level);
        HIDDEN_ENTITIES.put(payload.entityId(), System.currentTimeMillis() + 2_000L);

        int gibWidth = (int) (payload.width() / 0.25F);
        int gibHeight = (int) (payload.height() / 0.25F);
        int count = (int) (gibWidth * 1.5D * gibHeight);
        RandomSource random = level.random;
        double multiplier = random.nextInt(15) == 0 ? 10.0D : 1.0D;
        SimpleParticleType particle = switch (payload.gibType()) {
            case 1 -> HbmParticleTypes.GIBLET_SLIME.get();
            case 2 -> HbmParticleTypes.GIBLET_METAL.get();
            default -> HbmParticleTypes.GIBLET_MEAT.get();
        };
        for (int index = 0; index < count; index++) {
            minecraft.particleEngine.createParticle(
                    particle,
                    payload.x(), payload.y(), payload.z(),
                    random.nextGaussian() * 0.25D * multiplier,
                    random.nextDouble() * multiplier,
                    random.nextGaussian() * 0.25D * multiplier
            );
        }
    }

    @SubscribeEvent
    public static void hideVictim(RenderLivingEvent.Pre<?, ?> event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        ensureLevel(level);
        Long deadline = HIDDEN_ENTITIES.get(event.getEntity().getId());
        if (deadline != null && deadline > System.currentTimeMillis()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            trackedLevel = null;
            HIDDEN_ENTITIES.clear();
            return;
        }
        ensureLevel(level);
        long now = System.currentTimeMillis();
        HIDDEN_ENTITIES.values().removeIf(deadline -> deadline <= now);
    }

    private static void ensureLevel(ClientLevel level) {
        if (trackedLevel != level) {
            trackedLevel = level;
            HIDDEN_ENTITIES.clear();
        }
    }
}
