package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Direct 1.7.10 ItemModDefuser behavior: permanently removes nearby swell AI. */
public final class LegacyDefuserArmorModItem extends ArmorModItem {
    private static final String DEFUSED = "hbm_defused";
    private static final String LEGACY_DEFUSED = "hfr_defused";

    public LegacyDefuserArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        for (Creeper creeper : player.level().getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(5.0D))) {
            defuse(creeper, player, true);
        }
    }

    public static boolean defuse(Creeper creeper, LivingEntity entity, boolean dropItem) {
        if (creeper.level().isClientSide
                || creeper.getPersistentData().getBoolean(DEFUSED)
                || creeper.getPersistentData().getBoolean(LEGACY_DEFUSED)) {
            return false;
        }
        creeper.setSwellDir(-1);
        creeper.setTarget(null);
        java.util.List<net.minecraft.world.entity.ai.goal.Goal> swellGoals = creeper.goalSelector.getAvailableGoals().stream()
                .filter(goal -> goal.getGoal() instanceof SwellGoal)
                .map(goal -> goal.getGoal())
                .toList();
        if (swellGoals.isEmpty()) {
            return false;
        }
        swellGoals.forEach(creeper.goalSelector::removeGoal);
        creeper.getPersistentData().putBoolean(DEFUSED, true);
        creeper.getPersistentData().putBoolean(LEGACY_DEFUSED, true);
        if (dropItem) {
            Item fuse = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("safety_fuse"));
            if (fuse != net.minecraft.world.item.Items.AIR) {
                creeper.spawnAtLocation(new ItemStack(fuse));
            }
            creeper.hurt(entity.damageSources().mobAttack(entity), 1.0F);
            entity.level().playSound(null, creeper.blockPosition(), SoundEvents.TRIPWIRE_DETACH,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.defuser").withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
