package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Direct-use medical consumables from ItemSyringe and ItemSimpleConsumable.
 * The old implementation applied these effects immediately on right click and
 * allowed the four metal syringes to be used on another living entity.
 */
public class LegacySyringeItem extends Item {
    private final String id;

    public LegacySyringeItem(Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (requiresPotionSicknessCheck() && player.hasEffect(HbmMobEffects.POTION_SICKNESS)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            applyTo(level, player, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide && supportsTargetUse()) {
            if (!(target.hasEffect(HbmMobEffects.POTION_SICKNESS) && requiresPotionSicknessCheck())) {
                applyTo(target.level(), target, attacker, stack);
            }
        }
        return false;
    }

    private void applyTo(Level level, LivingEntity target, LivingEntity source, ItemStack stack) {
        switch (id) {
            case "syringe_metal_stimpak" -> {
                target.heal(5.0F);
                applySickness(target, 5);
                consume(source, stack, "syringe_metal_empty");
            }
            case "syringe_metal_medx" -> {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 60 * 20, 2));
                applySickness(target, 5);
                consume(source, stack, "syringe_metal_empty");
            }
            case "syringe_metal_psycho" -> {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2 * 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2 * 60 * 20, 0));
                applySickness(target, 5);
                consume(source, stack, "syringe_metal_empty");
            }
            case "syringe_metal_super" -> {
                target.heal(25.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0));
                applySickness(target, 15);
                consume(source, stack, "syringe_metal_empty");
            }
            case "syringe_taint" -> {
                target.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 60 * 20, 0));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
                consume(source, stack, "syringe_metal_empty", "bottle2_empty");
            }
            case "syringe_mkunicorn" -> {
                // The legacy contagion capability has no modern equivalent yet;
                // preserve its only gameplay-visible action, the one-shot dose.
                target.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 3 * 60 * 60 * 20, 2));
                consume(source, stack);
            }
            case "med_bag" -> {
                if (target instanceof Player player) {
                    player.setHealth(player.getMaxHealth());
                    clearMedicalDebuffs(player);
                    applySickness(player, 15);
                    consume(player, stack);
                }
            }
            case "syringe_antidote" -> {
                target.removeAllEffects();
                applySickness(target, 5);
                consume(source, stack, "syringe_empty");
            }
            case "syringe_poison" -> {
                target.hurt(target.damageSources().magic(), 30.0F);
                consume(source, stack, "syringe_empty");
            }
            case "syringe_awesome" -> {
                applyAwesome(target);
                applySickness(target, 5);
                consume(source, stack, "syringe_empty");
            }
            case "radaway" -> consumeRadaway(source, stack, 140);
            case "radaway_strong" -> consumeRadaway(source, stack, 350);
            case "radaway_flush" -> consumeRadaway(source, stack, 500);
            case "iv_empty" -> {
                if (source instanceof Player player) {
                    player.setHealth(Math.max(player.getHealth() - 5.0F, 0.0F));
                    consume(player, stack, "iv_blood");
                }
            }
            case "iv_blood" -> {
                if (source instanceof Player player) {
                    player.heal(5.0F);
                    consume(player, stack, "iv_empty");
                }
            }
            case "iv_xp_empty" -> {
                if (source instanceof Player player && player.totalExperience >= 100) {
                    player.giveExperiencePoints(-100);
                    consume(player, stack, "iv_xp");
                }
            }
            case "iv_xp" -> {
                if (source instanceof Player player) {
                    player.giveExperiencePoints(100);
                    consume(player, stack, "iv_xp_empty");
                }
            }
            default -> {
            }
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private boolean supportsTargetUse() {
        return switch (id) {
            case "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super",
                    "syringe_taint", "syringe_mkunicorn", "syringe_antidote", "syringe_poison", "syringe_awesome" -> true;
            default -> false;
        };
    }

    private boolean requiresPotionSicknessCheck() {
        return switch (id) {
            case "syringe_metal_stimpak", "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super",
                    "med_bag", "syringe_antidote", "syringe_awesome" -> true;
            default -> false;
        };
    }

    private static void applySickness(LivingEntity target, int seconds) {
        target.addEffect(new MobEffectInstance(HbmMobEffects.POTION_SICKNESS, seconds * 20));
    }

    private static void applyAwesome(LivingEntity target) {
        effect(target, MobEffects.REGENERATION, 50 * 20, 9);
        effect(target, MobEffects.DAMAGE_RESISTANCE, 50 * 20, 9);
        effect(target, MobEffects.FIRE_RESISTANCE, 50 * 20, 0);
        effect(target, MobEffects.DAMAGE_BOOST, 50 * 20, 24);
        effect(target, MobEffects.DIG_SPEED, 50 * 20, 9);
        effect(target, MobEffects.MOVEMENT_SPEED, 50 * 20, 6);
        effect(target, MobEffects.JUMP, 50 * 20, 9);
        effect(target, MobEffects.HEALTH_BOOST, 50 * 20, 9);
        effect(target, MobEffects.ABSORPTION, 50 * 20, 4);
        effect(target, MobEffects.CONFUSION, 5 * 20, 4);
        target.addEffect(new MobEffectInstance(HbmMobEffects.RADX, 50 * 20, 9));
    }

    private static void effect(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    private static void clearMedicalDebuffs(Player player) {
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

    private static void consumeRadaway(LivingEntity source, ItemStack stack, int duration) {
        if (source instanceof Player player) {
            player.addEffect(new MobEffectInstance(HbmMobEffects.RADAWAY, duration, 0));
            consume(player, stack, "iv_empty");
        }
    }

    private static void consume(LivingEntity source, ItemStack stack, String... containerIds) {
        if (source instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        stack.shrink(1);
        if (!(source instanceof Player player)) {
            return;
        }
        for (String id : containerIds) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, id));
            if (!item.equals(net.minecraft.world.item.Items.AIR)) {
                ItemStack container = new ItemStack(item);
                if (!player.getInventory().add(container)) {
                    player.drop(container, false);
                }
            }
        }
    }
}
