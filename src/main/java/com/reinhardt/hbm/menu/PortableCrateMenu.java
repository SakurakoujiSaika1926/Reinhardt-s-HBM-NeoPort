package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.integration.curios.PortableCrateStorage;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PortableCrateMenu extends AbstractContainerMenu {
    private final Container container;
    private final StorageCrateBlockEntity.Kind kind;
    private final int crateSlotCount;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public PortableCrateMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, readKind(buffer), null);
    }

    public PortableCrateMenu(int containerId, Inventory inventory, StorageCrateBlockEntity.Kind kind,
                             Container container) {
        super(HbmMenus.PORTABLE_CRATE.get(), containerId);
        this.kind = kind;
        this.container = container == null ? new SimpleContainer(kind.slots()) : container;
        this.crateSlotCount = kind.slots();
        this.playerInventoryStart = this.crateSlotCount;
        this.playerInventoryEnd = this.playerInventoryStart + 27;
        this.hotbarStart = this.playerInventoryEnd;
        this.hotbarEnd = this.hotbarStart + 9;
        checkContainerSize(this.container, this.crateSlotCount);

        for (int row = 0; row < kind.rows(); row++) {
            for (int column = 0; column < kind.columns(); column++) {
                int slot = column + row * kind.columns();
                addSlot(new PortableCrateSlot(this.container, slot,
                        kind.crateX() + column * 18, kind.crateY() + row * 18));
            }
        }
        addPlayerInventory(inventory);
    }

    public StorageCrateBlockEntity.Kind kind() {
        return this.kind;
    }

    public int insertOverflow(ItemStack source) {
        if (!(this.container instanceof OverflowContainer overflow)) {
            return 0;
        }
        return overflow.insert(source);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = sourceSlot.getItem();
        ItemStack result = source.copy();
        if (index < this.crateSlotCount) {
            if (!moveItemStackTo(source, this.playerInventoryStart, this.hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, this.crateSlotCount, false)) {
            if (index < this.playerInventoryEnd) {
                if (!moveItemStackTo(source, this.hotbarStart, this.hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(source, this.playerInventoryStart, this.playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.setChanged();
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        kind.playerInventoryX() + column * 18, kind.playerInventoryY() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    kind.playerInventoryX() + column * 18, kind.hotbarY()));
        }
    }

    private static StorageCrateBlockEntity.Kind readKind(RegistryFriendlyByteBuf buffer) {
        int ordinal = Byte.toUnsignedInt(buffer.readByte());
        StorageCrateBlockEntity.Kind[] kinds = StorageCrateBlockEntity.Kind.values();
        return ordinal < kinds.length ? kinds[ordinal] : StorageCrateBlockEntity.Kind.IRON;
    }

    private static final class PortableCrateSlot extends Slot {
        private PortableCrateSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !PortableCrateStorage.isCrate(stack) && super.mayPlace(stack);
        }
    }

    public interface OverflowContainer extends Container {
        int insert(ItemStack source);
    }
}
