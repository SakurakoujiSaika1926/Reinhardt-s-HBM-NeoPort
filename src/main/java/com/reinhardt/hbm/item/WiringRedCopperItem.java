package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class WiringRedCopperItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    public WiringRedCopperItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        return useOnBlock(stack, level, player, context.getClickedPos());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        return useOnBlock(context.getItemInHand(), level, player, context.getClickedPos());
    }

    public static ItemInteractionResult useItemOnBlock(ItemStack stack, Level level, Player player, BlockPos clickedPos) {
        InteractionResult result = useOnBlock(stack, level, player, clickedPos);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static InteractionResult useOnBlock(ItemStack stack, Level level, Player player, BlockPos clickedPos) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        PowerPylonBlockEntity thisPylon = resolvePylon(level, clickedPos);
        if (thisPylon != null) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockPos start = start(stack);
            if (start == null) {
                BlockPos corePos = thisPylon.getBlockPos();
                setStart(stack, corePos);
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.start", corePos.getX(), corePos.getY(), corePos.getZ()), false);
                player.swing(player.getUsedItemHand(), true);
                return InteractionResult.CONSUME;
            }

            connect(level, player, thisPylon, start);
            clearStart(stack);
            player.swing(player.getUsedItemHand(), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && level.getGameTime() % 200L == 0L && start(stack) != null && entity instanceof Player player && isSelected) {
            BlockPos start = start(stack);
            int distance = (int) Math.sqrt(start.distSqr(player.blockPosition()));
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.measure", distance), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        BlockPos start = start(stack);
        if (start != null) {
            tooltipComponents.add(Component.translatable("desc.reinhardtshbm.wiring.start", start.getX(), start.getY(), start.getZ()).withStyle(ChatFormatting.GOLD));
        } else {
            tooltipComponents.add(Component.translatable("desc.reinhardtshbm.wiring.1").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void connect(Level level, Player player, PowerPylonBlockEntity thisPylon, BlockPos start) {
        PowerPylonBlockEntity targetPylon = resolvePylon(level, start);
        if (targetPylon == null) {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.notcompatible"), false);
            return;
        }

        switch (PowerPylonBlockEntity.canConnect(thisPylon, targetPylon)) {
            case 0 -> {
                thisPylon.addConnection(targetPylon.getBlockPos());
                targetPylon.addConnection(thisPylon.getBlockPos());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.connected"), false);
            }
            case 1 -> player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.notcompatible"), false);
            case 2 -> player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.noself"), false);
            case 3 -> {
                double maxLength = Math.min(thisPylon.kind().maxWireLength(), targetPylon.kind().maxWireLength());
                int distance = (int) thisPylon.connectionPoint().distanceTo(targetPylon.connectionPoint());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.wiring.tofar", distance, (int) maxLength), false);
            }
            default -> {
            }
        }
    }

    private static PowerPylonBlockEntity resolvePylon(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PowerPylonBlockEntity pylon) {
            return pylon;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof PowerPylonBlockEntity pylon) {
            return pylon;
        }
        return null;
    }

    private static void setStart(ItemStack stack, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(X, pos.getX());
        tag.putInt(Y, pos.getY());
        tag.putInt(Z, pos.getZ());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearStart(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private static BlockPos start(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(X) || !tag.contains(Y) || !tag.contains(Z)) {
            return null;
        }
        return new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z));
    }
}
