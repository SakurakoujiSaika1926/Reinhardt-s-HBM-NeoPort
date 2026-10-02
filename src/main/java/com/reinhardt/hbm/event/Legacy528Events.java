package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Runtime rules controlled by the old 528 master switch and its sub-switches. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class Legacy528Events {
    private Legacy528Events() { }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide
                || !HbmConfig.netherBurn()
                || player.fireImmune()
                || !player.level().dimension().equals(Level.NETHER)) {
            return;
        }
        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !HbmConfig.explosiveEnergistics()) return;
        String namespace = BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock()).getNamespace();
        if (!namespace.equals("ae2")) return;

        level.destroyBlock(event.getPos(), false);
        LegacyProjectileUtil.explosiveEnergisticsBlast(level, event.getPos().getCenter());
        event.setCanceled(true);
    }
}
