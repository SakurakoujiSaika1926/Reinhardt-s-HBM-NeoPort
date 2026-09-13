package com.reinhardt.hbm.item;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** 1.7.10 cigarette and crack pipe consumption, including persistent hazards. */
public class LegacyCigaretteItem extends Item {
    private final boolean crackPipe;

    public LegacyCigaretteItem(Properties properties, boolean crackPipe) {
        super(properties);
        this.crackPipe = crackPipe;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (living instanceof Player player && !level.isClientSide) {
            if (crackPipe) {
                HbmLivingHazards hazards = HbmLivingHazards.get(player);
                hazards.addBlackLung(player, 500);
                HbmLivingHazards.set(player, hazards);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                player.heal(10.0F);
            } else {
                HbmLivingHazards hazards = HbmLivingHazards.get(player);
                hazards.addBlackLung(player, 2_000);
                hazards.addAsbestos(player, 2_000);
                HbmLivingHazards.set(player, hazards);
                HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
                radiation.addRadiation(100.0F);
                HbmLivingRadiation.set(player, radiation);
                if (player.getItemBySlot(EquipmentSlot.HEAD).is(HbmItems.NO9.get())) {
                    HbmAdvancements.award(player, "no9");
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.PLAYER_COUGH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (!(living instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (crackPipe) {
            tooltip.add(Component.literal("This can't be good for me, but I feel GREAT").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.literal("Asbestos filter").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("High in tar").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Tobacco contains 100% Polonium-210").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Yum").withStyle(ChatFormatting.RED));
        }
    }
}
