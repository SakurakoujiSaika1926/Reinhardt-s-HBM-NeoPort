package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
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

/** Exact seven-slot layout of ContainerLaunchpadLambda. */
public final class LambdaLaunchpadMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = LambdaLaunchpadBlockEntity.SLOT_COUNT;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public LambdaLaunchpadMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, context(inventory, buffer));
    }

    private LambdaLaunchpadMenu(int containerId, Inventory inventory, MenuContext context) {
        this(containerId, inventory, context.container(), context.container().menuData(), context.pos());
    }

    public LambdaLaunchpadMenu(int containerId, Inventory inventory,
                               Container container, ContainerData data) {
        this(containerId, inventory, container, data, pos(container));
    }

    private LambdaLaunchpadMenu(int containerId, Inventory inventory,
                                Container container, ContainerData data, BlockPos blockPos) {
        super(HbmMenus.LAMBDA_LAUNCHPAD.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LambdaLaunchpadBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos;

        addSlot(new ValidatedSlot(container, LambdaLaunchpadBlockEntity.SLOT_ROCKET, 35, 17));
        addSlot(new ValidatedSlot(container, LambdaLaunchpadBlockEntity.SLOT_SATELLITE, 53, 17));
        addSlot(new ValidatedSlot(container, LambdaLaunchpadBlockEntity.SLOT_GASOLINE_IN, 107, 80));
        addSlot(new TakeOnlySlot(container, LambdaLaunchpadBlockEntity.SLOT_GASOLINE_OUT, 107, 98));
        addSlot(new ValidatedSlot(container, LambdaLaunchpadBlockEntity.SLOT_PEROXIDE_IN, 125, 80));
        addSlot(new TakeOnlySlot(container, LambdaLaunchpadBlockEntity.SLOT_PEROXIDE_OUT, 125, 98));
        addSlot(new ValidatedSlot(container, LambdaLaunchpadBlockEntity.SLOT_BATTERY, 89, 80));
        addPlayerInventory(inventory, 8, 144);
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
    public boolean erected() { return this.data.get(1) != 0; }
    public boolean erecting() { return this.data.get(2) != 0; }
    public boolean autolaunch() { return this.data.get(3) != 0; }
    public int countdown() { return this.data.get(4); }
    public int gasolineAmount() { return this.data.get(5); }
    public int gasolineCapacity() { return Math.max(1, this.data.get(6)); }
    public int peroxideAmount() { return this.data.get(7); }
    public int peroxideCapacity() { return Math.max(1, this.data.get(8)); }
    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (energy() * pixels / LambdaLaunchpadBlockEntity.MAX_POWER));
    }
    public int gasolineScaled(int pixels) {
        return Math.min(pixels, gasolineAmount() * pixels / gasolineCapacity());
    }
    public int peroxideScaled(int pixels) {
        return Math.min(pixels, peroxideAmount() * pixels / peroxideCapacity());
    }
    public boolean hasRocket() {
        return this.container instanceof LambdaLaunchpadBlockEntity launchpad && launchpad.hasRocketLoaded();
    }
    public boolean hasJetFuel() {
        return gasolineAmount() >= LambdaLaunchpadBlockEntity.FUEL_REQUIRED;
    }
    public boolean hasOxidizer() {
        return peroxideAmount() >= LambdaLaunchpadBlockEntity.FUEL_REQUIRED;
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
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof LambdaLaunchpadBlockEntity launchpad) {
            return new MenuContext(launchpad, pos);
        }
        throw new IllegalStateException("Lambda launchpad menu has no launchpad at " + pos);
    }

    private static BlockPos pos(Container container) {
        if (container instanceof LambdaLaunchpadBlockEntity launchpad) {
            return launchpad.getBlockPos();
        }
        throw new IllegalArgumentException("Lambda launchpad menu requires its block entity");
    }

    private record MenuContext(LambdaLaunchpadBlockEntity container, BlockPos pos) {}

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
