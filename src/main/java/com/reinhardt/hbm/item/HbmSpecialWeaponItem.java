package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Direct port of the per-item branches in 1.7.10 WeaponSpecial. */
public final class HbmSpecialWeaponItem extends SwordItem {
    public enum Kind {
        BOTTLE_OPENER,
        CHERNOBYL_SIGN,
        DIAMOND_GAVEL,
        LEAD_GAVEL,
        MEME_SPOON,
        SCHRABIDIUM_HAMMER,
        SHIMMER_SLEDGE,
        SOP_SIGN,
        STOP_SIGN,
        ULLAPOOL_CABER,
        WOOD_GAVEL,
        WRENCH_FLIPPED
    }

    private final Kind kind;

    public HbmSpecialWeaponItem(HbmToolProfile profile, Kind kind) {
        super(profile.tier(), properties(profile));
        this.kind = kind;
    }

    private static Item.Properties properties(HbmToolProfile profile) {
        Item.Properties properties = HbmToolBehavior.properties(profile);
        if (profile.tier().getUses() > 0) {
            properties.durability(profile.tier().getUses());
        }
        return properties;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            applyHit(stack, target, attacker);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    private void applyHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        switch (this.kind) {
            case SCHRABIDIUM_HAMMER -> {
                target.setHealth(0.0F);
                target.playSound(SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            case BOTTLE_OPENER -> {
                switch (level.random.nextInt(7)) {
                    case 0 -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 60 * 20, 0));
                    case 1 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 60 * 20, 2));
                    case 2 -> target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 60 * 20, 2));
                    case 3 -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60 * 20, 0));
                    default -> {
                    }
                }
                target.playSound(SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            case ULLAPOOL_CABER -> {
                level.explode(null, target.getX(), target.getY(), target.getZ(), 7.5F, true, Level.ExplosionInteraction.BLOCK);
                if (attacker instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
                    stack.hurtAndBreak(505, serverLevel, player, item -> player.onEquippedItemBroken(item, net.minecraft.world.entity.EquipmentSlot.MAINHAND));
                }
            }
            case SHIMMER_SLEDGE -> {
                target.setDeltaMovement(target.getDeltaMovement().add(attacker.getLookAngle().scale(5.0D)));
                target.hurtMarked = true;
                target.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 1.0F);
            }
            case DIAMOND_GAVEL -> {
                target.setHealth(Math.max(0.0F, target.getHealth() - target.getMaxHealth() / 3.0F));
                target.playSound(SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            case LEAD_GAVEL -> {
                target.addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 15 * 20, 4));
                target.playSound(SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            case WOOD_GAVEL -> target.playSound(SoundEvents.WOOD_HIT, 3.0F, 1.0F);
            case STOP_SIGN, SOP_SIGN -> target.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.0F, 1.0F);
            case MEME_SPOON -> applyMemeSpoon(target, attacker);
            case CHERNOBYL_SIGN, WRENCH_FLIPPED -> {
            }
        }
    }

    private static void applyMemeSpoon(LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player)) {
            return;
        }
        if (player.fallDistance >= 2.0F) {
            target.hurt(attacker.damageSources().playerAttack(player), 50.0F);
        }
        if (player.fallDistance >= 20.0F && !player.getAbilities().instabuild) {
            attacker.level().explode(player, target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ(), 15.0F, false, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (this.kind != Kind.SHIMMER_SLEDGE) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (state.isAir() || state.getBlock().getExplosionResistance() >= 6000.0F) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel && context.getPlayer() != null) {
            FallingBlockEntity rubble = FallingBlockEntity.fall(serverLevel, context.getClickedPos(), state);
            Vec3 velocity = context.getPlayer().getLookAngle().scale(5.0D);
            rubble.setDeltaMovement(velocity);
            rubble.hasImpulse = true;
            rubble.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
