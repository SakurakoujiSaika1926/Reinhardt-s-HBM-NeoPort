package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class RbmkConsoleLinkerItem extends Item {
    private static final String POS_X = "posX";
    private static final String POS_Y = "posY";
    private static final String POS_Z = "posZ";

    public RbmkConsoleLinkerItem(Properties properties) {
        super(properties.stacksTo(1).attributes(createAttributes()));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnTarget(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnTarget(context.getItemInHand(), context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.rbmk_tool.desc.1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.reinhardtshbm.rbmk_tool.desc.2").withStyle(ChatFormatting.YELLOW));
        BlockPos target = target(stack);
        if (target != null) {
            tooltip.add(Component.translatable("item.reinhardtshbm.rbmk_tool.target", target.getX(), target.getY(), target.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static InteractionResult useOnTarget(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos corePos = resolveCore(level, context.getClickedPos());
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof RbmkComponentBlockEntity rbmk)) {
            return InteractionResult.PASS;
        }
        RbmkComponentBlock.Kind kind = rbmk.kind();
        if (kind.isColumn()) {
            if (!level.isClientSide) {
                rememberTarget(stack, corePos);
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.rbmk_tool.linked")
                        .withStyle(ChatFormatting.YELLOW), false);
                level.playSound(null, context.getClickedPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if ((kind == RbmkComponentBlock.Kind.CONSOLE
                || kind == RbmkComponentBlock.Kind.CRANE_CONSOLE
                || kind == RbmkComponentBlock.Kind.DISPLAY)
                && target(stack) != null) {
            if (!level.isClientSide) {
                rbmk.setConsoleTarget(target(stack));
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.rbmk_tool.set")
                        .withStyle(ChatFormatting.YELLOW), false);
                level.playSound(null, context.getClickedPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static void rememberTarget(ItemStack stack, BlockPos target) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(POS_X, target.getX());
        tag.putInt(POS_Y, target.getY());
        tag.putInt(POS_Z, target.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(POS_X) || !tag.contains(POS_Y) || !tag.contains(POS_Z)) {
            return null;
        }
        return new BlockPos(tag.getInt(POS_X), tag.getInt(POS_Y), tag.getInt(POS_Z));
    }

    private static BlockPos resolveCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return pos;
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2.0D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
