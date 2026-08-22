package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.network.BatteryReddSyncPayload;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigInteger;

public class BatteryReddMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = BatteryReddBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final BlockPos blockPos;
    private final Player player;
    private BigInteger syncedPower = BigInteger.ZERO;
    private BigInteger syncedDelta = BigInteger.ZERO;
    private int syncedRedLow;
    private int syncedRedHigh = 2;
    private int syncedPriority = PowerEndpoint.ConnectionPriority.LOW.ordinal();
    private int syncTicker;

    public BatteryReddMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()));
    }

    public BatteryReddMenu(int containerId, Inventory playerInventory, Container container) {
        super(HbmMenus.BATTERY_REDD.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        this.container = container;
        this.blockPos = blockPosFromContainer(container);
        this.player = playerInventory.player;
        if (container instanceof BatteryReddBlockEntity battery) {
            setSyncedState(battery.power(), battery.delta(), battery.redLow(), battery.redHigh(), battery.priority().ordinal());
        }
        this.addSlot(new BatterySlot(container, BatteryReddBlockEntity.SLOT_INPUT, 26, 53));
        this.addSlot(new BatterySlot(container, BatteryReddBlockEntity.SLOT_OUTPUT, 80, 53));
        addPlayerInventory(playerInventory, 8, 99);
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
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, BatteryReddBlockEntity.SLOT_INPUT, BatteryReddBlockEntity.SLOT_OUTPUT + 1, false)) {
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
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    @org.jetbrains.annotations.Nullable
    public BatteryReddBlockEntity battery() {
        return this.container instanceof BatteryReddBlockEntity battery ? battery : null;
    }

    public BigInteger power() {
        return this.syncedPower;
    }

    public BigInteger delta() {
        return this.syncedDelta;
    }

    public int redLow() {
        return this.syncedRedLow;
    }

    public int redHigh() {
        return this.syncedRedHigh;
    }

    public int priorityOrdinal() {
        return this.syncedPriority;
    }

    public void acceptSync(BatteryReddSyncPayload payload) {
        if (!this.blockPos.equals(payload.pos())) {
            return;
        }
        setSyncedState(readBigInteger(payload.power()), readBigInteger(payload.delta()), payload.redLow(), payload.redHigh(), payload.priority());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (++this.syncTicker % 5 == 0) {
            sendSync();
        }
    }

    public void sendSync() {
        if (!(this.player instanceof ServerPlayer serverPlayer) || !(this.container instanceof BatteryReddBlockEntity battery)) {
            return;
        }
        PacketDistributor.sendToPlayer(serverPlayer, BatteryReddSyncPayload.of(
                this.blockPos,
                battery.power(),
                battery.delta(),
                battery.redLow(),
                battery.redHigh(),
                battery.priority()
        ));
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BatteryReddBlockEntity battery) {
            return battery;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static BlockPos blockPosFromContainer(Container container) {
        return container instanceof BatteryReddBlockEntity battery ? battery.getBlockPos() : BlockPos.ZERO;
    }

    private void setSyncedState(BigInteger power, BigInteger delta, int redLow, int redHigh, int priority) {
        this.syncedPower = power;
        this.syncedDelta = delta;
        this.syncedRedLow = redLow & 3;
        this.syncedRedHigh = redHigh & 3;
        this.syncedPriority = Math.max(0, Math.min(priority, PowerEndpoint.ConnectionPriority.VALUES.length - 1));
    }

    private static BigInteger readBigInteger(String value) {
        try {
            return new BigInteger(value);
        } catch (NumberFormatException ignored) {
            return BigInteger.ZERO;
        }
    }

    private static final class BatterySlot extends Slot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BatteryPackItem.isBattery(stack);
        }
    }
}
