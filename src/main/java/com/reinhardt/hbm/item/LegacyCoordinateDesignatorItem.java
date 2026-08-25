package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** The 1.7.10 ItemDesingator block-click coordinate writer for launch pads. */
public final class LegacyCoordinateDesignatorItem extends Item {
    private static final String X = "xCoord";
    private static final String Z = "zCoord";

    public LegacyCoordinateDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SoyuzLauncherBlockEntity) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        if (!context.getLevel().isClientSide) {
            BlockPos target = context.getClickedPos();
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(X, target.getX());
            tag.putInt(Z, target.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            context.getPlayer().displayClientMessage(Component.translatable("message.reinhardtshbm.designator_range.set", target.getX(), target.getZ()), true);
            context.getLevel().playSound(null, context.getPlayer().blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(X) || !tag.contains(Z)) {
            tooltip.add(Component.translatable("item.reinhardtshbm.designator_range.no_target").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("item.reinhardtshbm.designator_range.target", tag.getInt(X), tag.getInt(Z))
                .withStyle(ChatFormatting.YELLOW));
    }
}
