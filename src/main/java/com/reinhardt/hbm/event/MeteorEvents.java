package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class MeteorEvents {
    private static int meteorShower;

    private MeteorEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !HbmConfig.ENABLE_METEOR_STRIKES.get()) {
            return;
        }
        meteorUpdate(level);
    }

    private static void meteorUpdate(ServerLevel level) {
        RandomSource random = level.random;
        int strikeChance = Math.max(1, meteorShower > 0 ? HbmConfig.METEOR_SHOWER_CHANCE.get() : HbmConfig.METEOR_STRIKE_CHANCE.get());
        if (random.nextInt(strikeChance) == 0 && !level.players().isEmpty()) {
            ServerPlayer player = level.players().get(random.nextInt(level.players().size()));
            boolean repel = hasCharm(player, HbmItems.PROTECTION_CHARM.get());
            boolean strike = !hasCharm(player, HbmItems.METEOR_CHARM.get());
            if (strike) {
                spawnMeteorAtPlayer(player, repel);
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
        }

        int showerRoll = Math.max(1, HbmConfig.METEOR_STRIKE_CHANCE.get()) * 100;
        if (HbmConfig.ENABLE_METEOR_SHOWERS.get() && random.nextInt(showerRoll) == 0) {
            int duration = Math.max(1, HbmConfig.METEOR_SHOWER_DURATION.get());
            meteorShower = (int) (duration * 0.75D + duration * 0.25D * random.nextDouble());
        }
    }

    public static void spawnMeteorAtPlayer(ServerPlayer player, boolean repel) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.random;
        MeteorEntity meteor = new MeteorEntity(HbmEntityTypes.METEOR.get(), level);
        meteor.setPos(player.getX() + random.nextInt(201) - 100, 384.0D, player.getZ() + random.nextInt(201) - 100);

        Vec3 vec;
        if (repel) {
            vec = new Vec3(meteor.getX() - player.getX(), 0.0D, meteor.getZ() - player.getZ()).normalize();
            double velocity = random.nextDouble();
            vec = new Vec3(vec.x * velocity, 0.0D, vec.z * velocity);
            meteor.setSafe(true);
        } else {
            double angle = Math.PI * random.nextDouble();
            double x = random.nextDouble() - 0.5D;
            vec = new Vec3(x * Math.cos(angle), 0.0D, x * -Math.sin(angle));
        }

        meteor.setDeltaMovement(vec.x, -2.5D, vec.z);
        level.addFreshEntity(meteor);
    }

    private static boolean hasCharm(ServerPlayer player, net.minecraft.world.item.Item charm) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.armor) {
            if (stack.getItem() == charm) {
                return true;
            }
        }
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() == charm) {
                return true;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack.getItem() == charm) {
                return true;
            }
        }
        return false;
    }
}
