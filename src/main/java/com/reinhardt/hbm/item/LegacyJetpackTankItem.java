package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Exact 1.7.10 jetpack_tank behavior: consume one tank to insert up to 1,000 mB of kerosene. */
public final class LegacyJetpackTankItem extends Item {
    private static final int FILL_AMOUNT = 1_000;

    public LegacyJetpackTankItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tank = player.getItemInHand(hand);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack installed = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
        if (!(installed.getItem() instanceof LegacyJetpackItem jetpack)) {
            return InteractionResultHolder.pass(tank);
        }

        int filled = jetpack.fill(installed, "kerosene", FILL_AMOUNT);
        if (filled <= 0) {
            return InteractionResultHolder.pass(tank);
        }
        if (!level.isClientSide) {
            ArmorModHandler.applyMod(chestplate, installed, player.registryAccess());
            if (!player.getAbilities().instabuild) {
                tank.shrink(1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(tank, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.jetpack_tank")
                .withStyle(ChatFormatting.GRAY));
    }
}
