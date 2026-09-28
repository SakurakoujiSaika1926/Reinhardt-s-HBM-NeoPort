package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Exact 27-slot layout of the latest ContainerLaunchpadSoyuz. */
public final class SoyuzLaunchpadMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = SoyuzLaunchpadBlockEntity.SLOT_COUNT;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public SoyuzLaunchpadMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, context(inventory, buffer));
    }

    private SoyuzLaunchpadMenu(int id, Inventory inventory, MenuContext context) {
        this(id, inventory, context.container(), context.container().menuData(), context.pos());
    }

    public SoyuzLaunchpadMenu(int id, Inventory inventory,
                              Container container, ContainerData data) {
        this(id, inventory, container, data, pos(container));
    }

    private SoyuzLaunchpadMenu(int id, Inventory inventory,
                               Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.SOYUZ_LAUNCHPAD.get(), id);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, SoyuzLaunchpadBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos;

        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_ROCKET, 98, 80));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_DESIGNATOR, 80, 80));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_SATELLITE, 98, 26));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_ORBITAL_MODULE, 80, 26));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_KEROSENE_IN, 152, 98));
        addSlot(new TakeOnlySlot(container, SoyuzLaunchpadBlockEntity.SLOT_KEROSENE_OUT, 152, 116));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_OXYGEN_IN, 170, 98));
        addSlot(new TakeOnlySlot(container, SoyuzLaunchpadBlockEntity.SLOT_OXYGEN_OUT, 170, 116));
        addSlot(new ValidatedSlot(container, SoyuzLaunchpadBlockEntity.SLOT_BATTERY, 134, 98));

        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 6; row++) {
                int slot = row + column * 6 + SoyuzLaunchpadBlockEntity.SLOT_CARGO_START;
                addSlot(new ValidatedSlot(container, slot, 44 - column * 18, 26 + row * 18));
            }
        }

        addPlayerInventory(inventory, 17, 162);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int machineSlot = 0; machineSlot < MACHINE_SLOTS && !moved; machineSlot++) {
                Slot target = this.slots.get(machineSlot);
                if (target.mayPlace(stack)) {
                    moved = moveItemStackTo(stack, machineSlot, machineSlot + 1, false);
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() { return this.blockPos; }
    public long energy() { return Integer.toUnsignedLong(this.data.get(0)); }
    public int loadedType() { return this.data.get(1); }
    public boolean cargoMode() { return this.data.get(2) != 0; }
    public int countdown() { return this.data.get(3); }
    public SoyuzLaunchpadBlockEntity.SoyuzStatus status() {
        int ordinal = this.data.get(4);
        var values = SoyuzLaunchpadBlockEntity.SoyuzStatus.values();
        return ordinal >= 0 && ordinal < values.length
                ? values[ordinal] : SoyuzLaunchpadBlockEntity.SoyuzStatus.ABSENT;
    }
    public int keroseneAmount() { return this.data.get(5); }
    public int keroseneCapacity() { return Math.max(1, this.data.get(6)); }
    public int oxygenAmount() { return this.data.get(7); }
    public int oxygenCapacity() { return Math.max(1, this.data.get(8)); }
    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (energy() * pixels / SoyuzLaunchpadBlockEntity.MAX_POWER));
    }
    public int keroseneScaled(int pixels) {
        return Math.min(pixels, keroseneAmount() * pixels / keroseneCapacity());
    }
    public int oxygenScaled(int pixels) {
        return Math.min(pixels, oxygenAmount() * pixels / oxygenCapacity());
    }
    public boolean hasJetFuel() { return keroseneAmount() >= SoyuzLaunchpadBlockEntity.FUEL_REQUIRED; }
    public boolean hasOxidizer() { return oxygenAmount() >= SoyuzLaunchpadBlockEntity.FUEL_REQUIRED; }
    public boolean hasPower() { return energy() >= SoyuzLaunchpadBlockEntity.CONSUMPTION; }
    public int orbitalState() {
        return this.container instanceof SoyuzLaunchpadBlockEntity launchpad
                ? launchpad.orbitalState() : 0;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static MenuContext context(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof SoyuzLaunchpadBlockEntity launchpad) {
            return new MenuContext(launchpad, pos);
        }
        throw new IllegalStateException("Soyuz launchpad menu has no launchpad at " + pos);
    }

    private static BlockPos pos(Container container) {
        if (container instanceof SoyuzLaunchpadBlockEntity launchpad) {
            return launchpad.getBlockPos();
        }
        throw new IllegalArgumentException("Soyuz launchpad menu requires its block entity");
    }

    private record MenuContext(SoyuzLaunchpadBlockEntity container, BlockPos pos) {}

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
