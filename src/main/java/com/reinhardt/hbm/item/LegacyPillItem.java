package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.concurrent.ThreadLocalRandom;

/** Exact effect branch for the remaining 1.7.10 ItemPill variants. */
public class LegacyPillItem extends Item {
    public enum Kind {
        RADX,
        SIOX,
        HERBAL,
        XANAX,
        FMN,
        FIVE_HTP,
        IODINE,
        PLAN_C,
        RED,
        CHOCOLATE
    }

    private final Kind kind;

    public LegacyPillItem(Properties properties, Kind kind) {
        super(properties.stacksTo(64));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.hasEffect(HbmMobEffects.POTION_SICKNESS)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (living instanceof Player player && !level.isClientSide) {
            HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
            HbmLivingHazards hazards = HbmLivingHazards.get(player);
            player.addEffect(new MobEffectInstance(HbmMobEffects.POTION_SICKNESS, 5 * 20));
            switch (this.kind) {
                case RADX -> player.addEffect(new MobEffectInstance(HbmMobEffects.RADX, 3 * 60 * 20, 0));
                case SIOX -> {
                    hazards.setAsbestos(player, 0);
                    hazards.setBlackLung(player, Math.min(hazards.getBlackLung(), HbmLivingHazards.MAX_BLACK_LUNG / 5));
                }
                case HERBAL -> {
                    hazards.setAsbestos(player, 0);
                    hazards.setBlackLung(player, Math.min(hazards.getBlackLung(), HbmLivingHazards.MAX_BLACK_LUNG / 5));
                    radiation.addRadiation(-100.0F);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 60 * 20, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10 * 60 * 20, 2));
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 2));
                    player.addEffect(new MobEffectInstance(HbmMobEffects.POTION_SICKNESS, 10 * 60 * 20));
                }
                case XANAX -> radiation.setDigamma(Math.max(0.0F, radiation.getDigamma() - 0.5F));
                case FMN -> {
                    radiation.setDigamma(Math.min(2.0F, radiation.getDigamma()));
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
                case FIVE_HTP -> {
                    radiation.setDigamma(0.0F);
                    player.addEffect(new MobEffectInstance(HbmMobEffects.STABILITY, 10 * 60 * 20));
                }
                case IODINE -> clearIodineDebuffs(player);
                case PLAN_C -> player.hurt(player.damageSources().source(HbmDamageTypes.DEATH), 1000.0F);
                case RED -> player.addEffect(new MobEffectInstance(HbmMobEffects.DEATH, 60 * 60 * 20));
                case CHOCOLATE -> {
                    if (ThreadLocalRandom.current().nextInt(25) == 0) {
                        player.hurt(player.damageSources().source(HbmDamageTypes.DEATH), 1000.0F);
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60 * 20, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60 * 20, 3));
                }
            }
            HbmLivingRadiation.set(player, radiation);
            HbmLivingHazards.set(player, hazards);
        }
        if (!(living instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    private static void clearIodineDebuffs(Player player) {
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(HbmMobEffects.TAINT);
    }
}
