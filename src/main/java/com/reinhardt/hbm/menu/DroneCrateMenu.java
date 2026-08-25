package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DroneCrateBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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

/** Slot layout of ContainerDroneCrate: 18 cargo, one fluid identifier, then player inventory. */
public final class DroneCrateMenu extends AbstractContainerMenu {
    private static final int CRATE_SLOTS = 19;
    private final Container container;
    private final DroneCrateBlockEntity crate;

    public DroneCrateMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolve(playerInventory, buffer.readBlockPos()));
    }

    public DroneCrateMenu(int id, Inventory playerInventory, DroneCrateBlockEntity crate) {
        this(id, playerInventory, new Resolved(crate, crate));
    }

    private DroneCrateMenu(int id, Inventory playerInventory, Resolved resolved) {
        super(HbmMenus.DRONE_CRATE.get(), id);
        this.container = resolved.container();
        this.crate = resolved.crate();
        checkContainerSize(container, CRATE_SLOTS);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 6; column++) {
                addSlot(new Slot(container, column + row * 6, 8 + column * 18, 17 + row * 18));
            }
        }
        addSlot(new IdentifierSlot(container, DroneCrateBlockEntity.IDENTIFIER_SLOT, 125, 53));
        addPlayerInventory(playerInventory);
    }

    public boolean itemType() {
        return crate == null || crate.itemType();
    }

    public boolean sendingMode() {
        return crate != null && crate.sendingMode();
    }

    public HbmFluidDefinition tankFluid() {
        return crate == null ? HbmFluids.none() : crate.tank().type();
    }

    public int tankAmount() {
        return crate == null ? 0 : crate.tank().amount();
    }

    public int tankCapacity() {
        return crate == null ? DroneCrateBlockEntity.FLUID_CAPACITY : crate.tank().capacity();
    }

    public int tankScaled(int pixels) {
        return tankCapacity() <= 0 ? 0 : Math.min(pixels, tankAmount() * pixels / tankCapacity());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crate == null) {
            return false;
        }
        if (id == 0) {
            crate.togglePayloadType();
            return true;
        }
        if (id == 1) {
            crate.toggleSendingMode();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < CRATE_SLOTS) {
            if (!moveItemStackTo(stack, CRATE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, DroneCrateBlockEntity.IDENTIFIER_SLOT, DroneCrateBlockEntity.IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, DroneCrateBlockEntity.CARGO_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 161));
        }
    }

    private static Resolved resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof DroneCrateBlockEntity crate
                ? new Resolved(crate, crate)
                : new Resolved(new SimpleContainer(CRATE_SLOTS), null);
    }

    private record Resolved(Container container, DroneCrateBlockEntity crate) {
    }

    private static final class IdentifierSlot extends Slot {
        private IdentifierSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
    }
}
