package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** Direct 1.7.10 ItemReactorSensor coordinate binding for research reactors. */
public final class ReactorSensorItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    public ReactorSensorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos reactorPos = resolveCore(level, context.getClickedPos());
        if (!(level.getBlockEntity(reactorPos) instanceof ResearchReactorBlockEntity)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            CompoundTag tag = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(X, reactorPos.getX());
            tag.putInt(Y, reactorPos.getY());
            tag.putInt(Z, reactorPos.getZ());
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            level.playSound(null, reactorPos, HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.reactor_sensor.linked")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos target = target(stack);
        if (target == null) {
            tooltip.add(Component.translatable("item.reinhardtshbm.reactor_sensor.no_target").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("x: " + target.getX()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("y: " + target.getY()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("z: " + target.getZ()).withStyle(ChatFormatting.GRAY));
    }

    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(X) || !tag.contains(Y) || !tag.contains(Z)) {
            return null;
        }
        return new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z));
    }

    private static BlockPos resolveCore(Level level, BlockPos clicked) {
        BlockEntity blockEntity = level.getBlockEntity(clicked);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return clicked;
    }
}
