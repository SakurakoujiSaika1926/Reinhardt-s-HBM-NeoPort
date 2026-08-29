package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.IcfCoreBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot coordinates directly ported from ContainerICF. */
public final class IcfCoreMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = IcfCoreBlockEntity.SLOT_COUNT;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 27;
    private static final int HOTBAR_END = PLAYER_END + 9;

    private final Container container;
    private final ContainerData data;

    public IcfCoreMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, containerAt(inventory, buffer.readBlockPos()), new SimpleContainerData(IcfCoreBlockEntity.DATA_COUNT));
    }

    public IcfCoreMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.ICF.get(), id);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, IcfCoreBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        for (int slot = 0; slot < 5; slot++) {
            addSlot(new ValidatedSlot(container, slot, 80 + slot * 18, 18));
        }
        addSlot(new ActiveSlot(container, IcfCoreBlockEntity.ACTIVE_SLOT, 116, 54));
        for (int slot = 0; slot < 5; slot++) {
            addSlot(new TakeOnlySlot(container, IcfCoreBlockEntity.DEPLETED_START + slot, 80 + slot * 18, 90));
        }
        addSlot(new IdentifierSlot(container, IcfCoreBlockEntity.IDENTIFIER_SLOT, 44, 90));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, IcfCoreBlockEntity.IDENTIFIER_SLOT, IcfCoreBlockEntity.IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(HbmItems.ICF_PELLET.get())) {
            if (!moveItemStackTo(stack, IcfCoreBlockEntity.FRESH_START, IcfCoreBlockEntity.FRESH_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_END) {
            if (!moveItemStackTo(stack, PLAYER_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, false)) {
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
        return container.stillValid(player);
    }

    public HbmFluidDefinition tankFluid(int tank) {
        return HbmFluids.byOldId(data.get(switch (tank) { case 0 -> 0; case 1 -> 2; default -> 4; })).orElse(HbmFluids.none());
    }

    public int tankAmount(int tank) {
        return data.get(switch (tank) { case 0 -> 1; case 1 -> 3; default -> 5; });
    }

    public int tankCapacity(int tank) {
        return tank == 2 ? IcfCoreBlockEntity.FLUX_CAPACITY : IcfCoreBlockEntity.SODIUM_CAPACITY;
    }

    public int tankScaled(int tank, int pixels) {
        return Math.min(pixels, tankAmount(tank) * pixels / tankCapacity(tank));
    }

    public long laser() { return value(6); }
    public long maxLaser() { return value(8); }
    public long heat() { return value(10); }
    public long heatup() { return value(12); }
    public int consumption() { return data.get(14); }

    private long value(int lowIndex) {
        return ((long) data.get(lowIndex + 1) << 32) | (data.get(lowIndex) & 0xFFFFFFFFL);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 44 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 44 + column * 18, 198));
        }
    }

    private static Container containerAt(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof IcfCoreBlockEntity icf ? icf : new SimpleContainer(MACHINE_SLOTS);
    }

    private static class ValidatedSlot extends Slot {
        ValidatedSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(index, stack); }
    }

    private static final class ActiveSlot extends Slot {
        ActiveSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    private static final class TakeOnlySlot extends Slot {
        TakeOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    private static final class IdentifierSlot extends ValidatedSlot {
        IdentifierSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
    }
}
