package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Small global hooks which existed in ModEventHandler rather than an item. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class LegacyGameplayParityEvents {
    private LegacyGameplayParityEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide
                || !victim.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || !(event.getSource().getEntity() instanceof Player player)
                || player instanceof FakePlayer) {
            return;
        }

        if (victim instanceof Spider && victim.getRandom().nextInt(500) == 0) {
            addDrop(event, victim, "spider_milk");
        }
        if (victim instanceof CaveSpider && victim.getRandom().nextInt(100) == 0) {
            addDrop(event, victim, "serum");
        }
        if (victim instanceof Animal && victim.getRandom().nextInt(500) == 0) {
            addDrop(event, victim, "bandaid");
        }
        if (victim instanceof Enemy) {
            if (victim.getRandom().nextInt(1000) == 0) addDrop(event, victim, "heart_piece");
            if (victim.getRandom().nextInt(250) == 0) addDrop(event, victim, "key_red_cracked");
            if (victim.getRandom().nextInt(250) == 0) addDrop(event, victim, "launch_code_piece");
        }
        if (victim instanceof LegacyCyberCrabEntity && victim.getRandom().nextInt(500) == 0) {
            addDrop(event, victim, "wd40");
        }
        if (victim instanceof Zombie) {
            if (victim.getRandom().nextInt(200) == 0) addDrop(event, victim, "ingot_copper");
            if (victim.getRandom().nextInt(200) == 0) addDrop(event, victim, "ingot_aluminium");
            if (victim.getRandom().nextInt(200) == 0) addDrop(event, victim, "ingot_titanium");
        }
    }

    @SubscribeEvent
    public static void onFoodFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !event.getItem().has(DataComponents.FOOD)) {
            return;
        }
        CompoundTagView data = new CompoundTagView(event.getItem());
        if (data.flag("ntmCyanide")) {
            for (int index = 0; index < 10; index++) {
                player.hurt(player.damageSources().source(HbmDamageTypes.DEATH), 1000.0F);
            }
        }
        if (data.flag("ntmRedPill")) {
            for (int index = 0; index < 10; index++) {
                player.addEffect(new MobEffectInstance(HbmMobEffects.DEATH, 60 * 60 * 20));
            }
        }
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity victim, String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, itemId));
        ItemEntity drop = new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), new ItemStack(item));
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private record CompoundTagView(ItemStack stack) {
        private boolean flag(String key) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(key);
        }
    }
}
