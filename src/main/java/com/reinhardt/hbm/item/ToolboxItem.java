package com.reinhardt.hbm.item;

import com.reinhardt.hbm.menu.ToolboxMenu;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Complete 1.7.10 toolbox: three eight-slot rows plus hotbar row swapping. */
public final class ToolboxItem extends Item {
    public static final int ROWS = 3;
    public static final int COLUMNS = 8;
    public static final int SLOT_COUNT = ROWS * COLUMNS;
    private static final String CONTENTS = "toolbox_contents";
    private static final String OPEN = "toolbox_open";

    public ToolboxItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                setOpen(box, true);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(
                            new SimpleMenuProvider(
                                    (containerId, inventory, ignored) -> new ToolboxMenu(containerId, inventory, hand),
                                    Component.translatable("container.reinhardtshbm.toolbox")
                            ),
                            buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND)
                    );
                }
            } else {
                swapHotbarRows(box, player, hand);
            }
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.toolbox.swap").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.toolbox.open").withStyle(ChatFormatting.GRAY));
    }

    public static List<ItemStack> loadContents(ItemStack stack, net.minecraft.core.RegistryAccess registries) {
        List<ItemStack> contents = new ArrayList<>(SLOT_COUNT);
        CompoundTag root = data(stack);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            contents.add(root.contains(CONTENTS + slot)
                    ? ItemStack.parseOptional(registries, root.getCompound(CONTENTS + slot))
                    : ItemStack.EMPTY);
        }
        return contents;
    }

    public static void saveContents(ItemStack stack, List<ItemStack> contents, net.minecraft.core.RegistryAccess registries) {
        CompoundTag root = data(stack);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack content = slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
            if (content.isEmpty()) {
                root.remove(CONTENTS + slot);
            } else {
                root.put(CONTENTS + slot, content.saveOptional(registries));
            }
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    static void setOpen(ItemStack stack, boolean open) {
        CompoundTag root = data(stack);
        if (open) {
            root.putBoolean(OPEN, true);
        } else {
            root.remove(OPEN);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static void swapHotbarRows(ItemStack box, Player player, InteractionHand hand) {
        List<ItemStack> contents = loadContents(box, player.registryAccess());
        int selected = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        List<ItemStack> hotbar = new ArrayList<>(COLUMNS);

        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(HbmItems.TOOLBOX.get())) {
                player.drop(stack, true);
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("tooltip.reinhardtshbm.toolbox.no_nested"), true);
            } else {
                hotbar.add(stack.copy());
            }
        }

        int sourceRow = firstActiveRow(contents);
        List<ItemStack> next = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            next.add(ItemStack.EMPTY);
        }

        if (sourceRow >= 0) {
            for (int row = sourceRow + 1; row < ROWS; row++) {
                copyRow(contents, row, next, row - 1);
            }
        }

        int targetRow = sourceRow < 0 ? 0 : ROWS - 1;
        for (int column = 0; column < COLUMNS; column++) {
            next.set(targetRow * COLUMNS + column, hotbar.get(column));
        }

        int hotbarIndex = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) {
                continue;
            }
            player.getInventory().setItem(slot, sourceRow < 0
                    ? ItemStack.EMPTY
                    : contents.get(sourceRow * COLUMNS + hotbarIndex).copy());
            hotbarIndex++;
        }
        saveContents(box, next, player.registryAccess());
        player.getInventory().setChanged();
    }

    private static int firstActiveRow(List<ItemStack> contents) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (!contents.get(row * COLUMNS + column).isEmpty()) {
                    return row;
                }
            }
        }
        return -1;
    }

    private static void copyRow(List<ItemStack> source, int sourceRow, List<ItemStack> target, int targetRow) {
        for (int column = 0; column < COLUMNS; column++) {
            target.set(targetRow * COLUMNS + column, source.get(sourceRow * COLUMNS + column).copy());
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
