package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

/** Direct port of ItemFlask's shield-infusion variant. */
public final class LegacyFlaskItem extends Item {
    public LegacyFlaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.flask_infusion.shield");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (HbmPlayerShield.get(player).maxShield() >= HbmPlayerShield.CAP) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (living instanceof Player player && !level.isClientSide) {
            HbmPlayerShield.addInfusion(player, 5.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    public static ItemStack shieldStack(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
        return stack;
    }
}
