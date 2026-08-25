package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The small, behavior-only armor modifications from 1.7.10. They share the
 * old armor-mod storage but keep their individual trigger conditions here.
 */
public final class LegacyArmorUtilityModItem extends ArmorModItem {
    public enum Kind {
        POLISH,
        BANDAID,
        SERUM,
        QUARTZ,
        MORNING_GLORY,
        SPIDER_MILK,
        INK,
        AUTO_INJECTOR
    }

    private final Kind kind;

    public LegacyArmorUtilityModItem(Item.Properties properties, Kind kind) {
        super(properties, slot(kind), helmet(kind), chest(kind), legs(kind), boots(kind));
        this.kind = kind;
    }

    @Override
    public float modifyArmorDamage(Player player, ItemStack armor, DamageSource source, float amount) {
        return switch (this.kind) {
            case POLISH -> player.getRandom().nextInt(20) == 0 ? 0.0F : amount;
            case BANDAID -> {
                if (player.getRandom().nextInt(100) < 3) {
                    player.setHealth(player.getMaxHealth());
                    yield 0.0F;
                }
                yield amount;
            }
            case QUARTZ -> {
                HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
                radiation.setRadiation(Math.max(0.0F, radiation.getRadiation() - 10.0F));
                yield amount;
            }
            case MORNING_GLORY -> {
                if (player.getRandom().nextInt(20) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
                }
                yield amount;
            }
            case INK -> {
                if (player.getRandom().nextInt(10) == 0) {
                    if (player.getRandom().nextInt(10) == 0) {
                        player.spawnAtLocation(new ItemStack(Items.DANDELION));
                    }
                    player.spawnAtLocation(new ItemStack(Items.POPPY));
                    yield 0.0F;
                }
                yield amount;
            }
            default -> amount;
        };
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        switch (this.kind) {
            case SERUM -> {
                if (player.hasEffect(MobEffects.POISON)) {
                    player.removeEffect(MobEffects.POISON);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 4));
                }
            }
            case MORNING_GLORY -> player.removeEffect(MobEffects.WITHER);
            case SPIDER_MILK -> player.getActiveEffectsMap().keySet().stream()
                    .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                    .toList()
                    .forEach(player::removeEffect);
            case AUTO_INJECTOR -> {
                HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
                if (radiation.getDigamma() >= 5.0F) {
                    ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
                    radiation.setDigamma(radiation.getDigamma() - 5.0F);
                    player.addEffect(new MobEffectInstance(HbmMobEffects.STABILITY, 60 * 20, 0));
                    player.heal(20.0F);
                    player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod." + this.kind.name().toLowerCase())
                .withStyle(color(this.kind)));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static int slot(Kind kind) {
        return kind == Kind.AUTO_INJECTOR ? ArmorModHandler.EXTRA : ArmorModHandler.EXTRA;
    }

    private static boolean helmet(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static boolean chest(Kind kind) {
        return true;
    }

    private static boolean legs(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static boolean boots(Kind kind) {
        return kind != Kind.AUTO_INJECTOR;
    }

    private static ChatFormatting color(Kind kind) {
        return switch (kind) {
            case POLISH -> ChatFormatting.BLUE;
            case BANDAID -> ChatFormatting.RED;
            case SERUM -> ChatFormatting.GREEN;
            case QUARTZ -> ChatFormatting.DARK_GRAY;
            case MORNING_GLORY, INK -> ChatFormatting.LIGHT_PURPLE;
            case SPIDER_MILK -> ChatFormatting.WHITE;
            case AUTO_INJECTOR -> ChatFormatting.BLUE;
        };
    }
}
