package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SoyuzCapsuleMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = SoyuzCapsuleBlockEntity.SLOT_COUNT;
    private final Container container;

    public SoyuzCapsuleMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public SoyuzCapsuleMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.SOYUZ_CAPSULE.get(), containerId);
        this.container = container;
        checkContainerSize(container, MACHINE_SLOTS);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 6; column++) {
                addSlot(new Slot(container, column + row * 6, 44 + column * 18, 17 + row * 18));
            }
        }
        addSlot(new Slot(container, SoyuzCapsuleBlockEntity.SLOT_ROCKET, 8, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SoyuzCapsuleBlockEntity capsule) {
            return capsule;
        }
        return new SimpleContainer(MACHINE_SLOTS);
    }
}
