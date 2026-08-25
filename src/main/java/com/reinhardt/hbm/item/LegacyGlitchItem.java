package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyVortexEntity;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The one-use randomized ItemGlitch effect table. */
public final class LegacyGlitchItem extends Item {
    public LegacyGlitchItem(Properties properties) {
        super(properties.stacksTo(1).durability(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int roll = level.random.nextInt(31);
            switch (roll) {
                case 2 -> player.hurt(level.damageSources().source(HbmDamageTypes.RADIATION), 1000.0F);
                case 4 -> LegacyVortexEntity.spawn(level, player.position(), 1.5F, 0.0025F, false);
                case 5, 6, 7 -> player.addItem(new ItemStack(HbmBlocks.BLOCK_METEOR_TREASURE.get(), roll == 5 ? 1 : roll == 6 ? 3 : 10));
                case 9 -> player.displayClientMessage(Component.translatable("message.reinhardtshbm.glitch.advanced_kit"), true);
                case 10 -> player.displayClientMessage(Component.translatable("message.reinhardtshbm.glitch.starter_kit"), true);
                case 14 -> {
                    player.getInventory().dropAll();
                    level.explode(player, player.getX(), player.getY(), player.getZ(), 0.0F, Level.ExplosionInteraction.NONE);
                }
                case 15 -> player.addItem(new ItemStack(net.minecraft.world.item.Items.DIRT, 36 * 64));
                case 21 -> player.displayClientMessage(Component.translatable("message.reinhardtshbm.glitch.missile"), true);
                case 24 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 9));
                case 25 -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 9));
                case 26 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * 20, 9));
                case 27 -> LegacyVortexEntity.spawn(level, player.position().add(0, -15, 0), 2.5F, 0.0025F, false);
                case 28 -> {
                    MeteorEntity meteor = new MeteorEntity(HbmEntityTypes.METEOR.get(), level);
                    meteor.setPos(player.getX(), player.getY() + 100.0D, player.getZ());
                    level.addFreshEntity(meteor);
                }
                case 29 -> NukeExplosionManager.scheduleLegacyNuke((net.minecraft.server.level.ServerLevel) level, player.getX(), player.getY(), player.getZ(), 27);
                default -> player.displayClientMessage(Component.translatable("message.reinhardtshbm.glitch.nothing"), true);
            }
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.glitch").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
