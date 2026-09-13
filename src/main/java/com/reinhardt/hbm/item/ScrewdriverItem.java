package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.WandLogicBlockEntity;
import com.reinhardt.hbm.blockentity.WandLootBlockEntity;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.block.ConveyorBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isScrewdriver(ItemStack stack) {
        return stack.getItem() instanceof ScrewdriverItem;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (ConveyorBlock.configureWithScrewdriver(context.getLevel(), context.getClickedPos(), player, stack, context.getHand())) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        InteractionResult rbmkResult = useOnRbmkLid(stack, context.getLevel(), player, context.getHand(), context.getClickedPos());
        if (rbmkResult.consumesAction()) {
            return rbmkResult;
        }
        InteractionResult wandResult = useOnWand(stack, context.getLevel(), player, context.getHand(), context.getClickedPos());
        if (wandResult.consumesAction()) {
            return wandResult;
        }
        return useOnHeater(stack, context.getLevel(), player, context.getHand(), context.getClickedPos());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (ConveyorBlock.configureWithScrewdriver(context.getLevel(), context.getClickedPos(), player, context.getItemInHand(), context.getHand())) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        InteractionResult rbmkResult = useOnRbmkLid(context.getItemInHand(), context.getLevel(), player, context.getHand(), context.getClickedPos());
        if (rbmkResult.consumesAction()) {
            return rbmkResult;
        }
        InteractionResult wandResult = useOnWand(context.getItemInHand(), context.getLevel(), player, context.getHand(), context.getClickedPos());
        if (wandResult.consumesAction()) {
            return wandResult;
        }
        return useOnHeater(context.getItemInHand(), context.getLevel(), player, context.getHand(), context.getClickedPos());
    }

    public static ItemInteractionResult useItemOnHeater(ItemStack stack, Level level, Player player, InteractionHand hand, BlockPos clickedPos) {
        if (ConveyorBlock.configureWithScrewdriver(level, clickedPos, player, stack, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult rbmkResult = useOnRbmkLid(stack, level, player, hand, clickedPos);
        if (rbmkResult.consumesAction()) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult wandResult = useOnWand(stack, level, player, hand, clickedPos);
        if (wandResult.consumesAction()) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult result = useOnHeater(stack, level, player, hand, clickedPos);
        if (result.consumesAction()) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static InteractionResult useOnHeater(ItemStack stack, Level level, Player player, InteractionHand hand, BlockPos clickedPos) {
        BlockPos corePos = resolveCore(level, clickedPos);
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof HeaterBlockEntity heater) || !canConfigure(heater)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            heater.cycleSetting(false);
            damageTool(stack, level, player, hand);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult useOnWand(ItemStack stack, Level level, Player player, InteractionHand hand, BlockPos clickedPos) {
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity instanceof WandLootBlockEntity loot) {
            if (!level.isClientSide) {
                loot.adjustMin(player.isShiftKeyDown());
                damageTool(stack, level, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (blockEntity instanceof WandLogicBlockEntity logic) {
            if (!level.isClientSide) {
                logic.cycleAction(player.isShiftKeyDown());
                damageTool(stack, level, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public static boolean isHandDrill(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("hand_drill") || id.equals("hand_drill_desh");
    }

    public static boolean isDefuser(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("defuser") || id.equals("defuser_desh") || id.equals("defuser_gold");
    }

    private static InteractionResult useOnRbmkLid(ItemStack stack, Level level, Player player, InteractionHand hand, BlockPos clickedPos) {
        BlockPos corePos = resolveCore(level, clickedPos);
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof RbmkComponentBlockEntity rbmk)) {
            return InteractionResult.PASS;
        }
        if (rbmk.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            if (!level.isClientSide) {
                rbmk.rotateConsoleScan();
                damageTool(stack, level, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (rbmk.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            if (!level.isClientSide) {
                rbmk.cycleCraneRotation();
                damageTool(stack, level, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (rbmk.kind() == RbmkComponentBlock.Kind.DISPLAY) {
            if (!level.isClientSide) {
                rbmk.rotateDisplayScan();
                damageTool(stack, level, player, hand);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // TileEntityRBMKBase.hasLid() is also true for the fixed-lid control
        // rods, but TileEntityRBMKBase.onScrew() only accepted removable
        // lids.  Keep a click on a control rod unhandled, just like 1.7.10,
        // instead of consuming the screwdriver action without changing any
        // state.
        if (!rbmk.canUseLid() || !rbmk.hasLid()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            ItemStack lid = rbmk.removeLidStack();
            if (!lid.isEmpty()) {
                player.getInventory().placeItemBackInInventory(lid);
                damageTool(stack, level, player, hand);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.screwdriver.1").withStyle(ChatFormatting.GRAY));
    }

    private static boolean canConfigure(HeaterBlockEntity heater) {
        return heater.kind() == HeaterBlockEntity.Kind.ELECTRIC || heater.kind() == HeaterBlockEntity.Kind.OILBURNER;
    }

    private static BlockPos resolveCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return pos;
    }

    public static void damageTool(ItemStack stack, Level level, Player player, InteractionHand hand) {
        if (!stack.isDamageableItem() || player.getAbilities().instabuild || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, serverLevel, serverPlayer, item -> serverPlayer.onEquippedItemBroken(item, slot));
    }
}
