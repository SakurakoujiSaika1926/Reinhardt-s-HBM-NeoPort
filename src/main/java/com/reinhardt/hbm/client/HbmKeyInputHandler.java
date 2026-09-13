package com.reinhardt.hbm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.LegacyDuckPayload;
import com.reinhardt.hbm.network.JetpackControlPayload;
import com.reinhardt.hbm.network.ArmorDashPayload;
import com.reinhardt.hbm.network.ToggleMagnetPayload;
import com.reinhardt.hbm.item.ArmorFSBItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class HbmKeyInputHandler {
    private static boolean ducked;
    private static boolean jetpackInput;

    private HbmKeyInputHandler() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ducked
                && minecraft.screen == null
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_O)) {
            ducked = true;
            PacketDistributor.sendToServer(new LegacyDuckPayload());
        }
        while (HbmKeyMappings.TOGGLE_MAGNET.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleMagnetPayload());
        }
        while (HbmKeyMappings.ARMOR_DASH.consumeClick()) {
            if (minecraft.player != null
                    && ArmorFSBItem.hasFSBArmor(minecraft.player)
                    && ArmorFSBItem.features(minecraft.player).dashCount() > 0) {
                PacketDistributor.sendToServer(new ArmorDashPayload());
            }
        }
        boolean jump = minecraft.screen == null && minecraft.options.keyJump.isDown();
        if (jump != jetpackInput) {
            jetpackInput = jump;
            PacketDistributor.sendToServer(new JetpackControlPayload(jump));
        }
        if (minecraft.player != null
                && ArmorFSBItem.hasFSBArmor(minecraft.player)
                && ArmorFSBItem.hasFeature(minecraft.player, ArmorFSBItem.Feature.THERMAL)) {
            // The old thermal sight was a helmet shader. Night vision is the
            // vanilla-safe equivalent and is refreshed only while the powered
            // full set is active.
            minecraft.player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 5, 0, false, false
            ));
        }
    }
}
