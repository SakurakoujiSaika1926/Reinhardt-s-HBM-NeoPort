package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
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

public class SoyuzLauncherMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = SoyuzLauncherBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final boolean clientFallback;

    public SoyuzLauncherMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, menuContext(playerInventory, buffer));
    }

    private SoyuzLauncherMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(SoyuzLauncherBlockEntity.DATA_COUNT),
                context.blockPos(), context.clientFallback());
    }

    public SoyuzLauncherMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, blockPosFromContainer(container));
    }

    private SoyuzLauncherMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, BlockPos blockPos) {
        this(containerId, playerInventory, container, data, blockPos, false);
    }

    private SoyuzLauncherMenu(int containerId, Inventory playerInventory, Container container, ContainerData data,
                              BlockPos blockPos, boolean clientFallback) {
        super(HbmMenus.SOYUZ_LAUNCHER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, SoyuzLauncherBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = blockPos;
        this.clientFallback = clientFallback;

        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_ROCKET, 62, 18));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_DESIGNATOR, 62, 36));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_SATELLITE, 116, 18));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_ORBITAL_MODULE, 116, 36));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_KEROSENE_IN, 8, 90));
        addSlot(new TakeOnlySlot(container, SoyuzLauncherBlockEntity.SLOT_KEROSENE_OUT, 8, 108));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_OXYGEN_IN, 26, 90));
        addSlot(new TakeOnlySlot(container, SoyuzLauncherBlockEntity.SLOT_OXYGEN_OUT, 26, 108));
        addSlot(new ValidatedSlot(container, SoyuzLauncherBlockEntity.SLOT_BATTERY, 44, 108));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 6; column++) {
                addSlot(new Slot(container, SoyuzLauncherBlockEntity.SLOT_CARGO_START + column + row * 6, 62 + column * 18, 72 + row * 18));
            }
        }

        addPlayerInventory(playerInventory, 8, 140);
        addDataSlots(data);
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(SoyuzLauncherBlockEntity.SLOT_ROCKET, stack)) {
            if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_ROCKET, SoyuzLauncherBlockEntity.SLOT_ROCKET + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(SoyuzLauncherBlockEntity.SLOT_KEROSENE_IN, stack)) {
            if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_KEROSENE_IN, SoyuzLauncherBlockEntity.SLOT_KEROSENE_IN + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(SoyuzLauncherBlockEntity.SLOT_OXYGEN_IN, stack)) {
            if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_OXYGEN_IN, SoyuzLauncherBlockEntity.SLOT_OXYGEN_IN + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, SoyuzLauncherBlockEntity.SLOT_BATTERY, SoyuzLauncherBlockEntity.SLOT_BATTERY + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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
        if (this.clientFallback) {
            return player.canInteractWithBlock(this.blockPos, 8.0D);
        }
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int energy() {
        return this.data.get(0);
    }

    public int mode() {
        return this.data.get(1);
    }

    public boolean starting() {
        return this.data.get(2) != 0;
    }

    public int countdown() {
        return this.data.get(3);
    }

    public int rocketType() {
        return this.data.get(4);
    }

    public int keroseneAmount() {
        return this.data.get(5);
    }

    public int keroseneCapacity() {
        return Math.max(1, this.data.get(6));
    }

    public int oxygenAmount() {
        return this.data.get(7);
    }

    public int oxygenCapacity() {
        return Math.max(1, this.data.get(8));
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (energy() * (long) pixels / SoyuzLauncherBlockEntity.MAX_POWER));
    }

    public int keroseneScaled(int pixels) {
        return Math.min(pixels, keroseneAmount() * pixels / keroseneCapacity());
    }

    public int oxygenScaled(int pixels) {
        return Math.min(pixels, oxygenAmount() * pixels / oxygenCapacity());
    }

    public boolean hasFuel() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz && soyuz.hasFuel();
    }

    public boolean hasOxygen() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz && soyuz.hasOxygen();
    }

    public boolean hasPower() {
        return energy() >= SoyuzLauncherBlockEntity.MAX_POWER * 3 / 4;
    }

    public boolean hasRocket() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz && soyuz.hasRocket();
    }

    public int designatorState() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz ? soyuz.designatorState() : 0;
    }

    public int orbitalState() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz ? soyuz.orbitalState() : 0;
    }

    public int satelliteState() {
        return this.container instanceof SoyuzLauncherBlockEntity soyuz ? soyuz.satelliteState() : 0;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static MenuContext menuContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SoyuzLauncherBlockEntity soyuz) {
            return new MenuContext(soyuz, pos, false);
        }
        return new MenuContext(new SimpleContainer(MACHINE_SLOT_COUNT), pos, true);
    }

    private static BlockPos blockPosFromContainer(Container container) {
        if (container instanceof SoyuzLauncherBlockEntity soyuz) {
            return soyuz.getBlockPos();
        }
        return BlockPos.ZERO;
    }

    private record MenuContext(Container container, BlockPos blockPos, boolean clientFallback) {
    }

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
