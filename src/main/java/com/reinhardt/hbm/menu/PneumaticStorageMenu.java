package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.PneumaticStorageBlock;
import com.reinhardt.hbm.blockentity.PneumaticStorageBlockEntity;
import com.reinhardt.hbm.pneumatic.PneumaticNetworkManager;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Shared menu retaining the exact slot layouts of the five legacy endpoints. */
public final class PneumaticStorageMenu extends AbstractContainerMenu {
    public static final int ACCESS_SLOTS = 48;

    private final PneumaticStorageBlockEntity storage;
    private final PneumaticStorageBlock.Kind kind;
    private final Container machineContainer;
    private final SimpleContainer accessDisplay = new SimpleContainer(ACCESS_SLOTS);
    private final int[] accessAmounts = new int[ACCESS_SLOTS];
    private final ContainerData accessData = new ContainerData() {
        @Override
        public int get(int index) {
            return index >= 0 && index < accessAmounts.length ? accessAmounts[index] : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < accessAmounts.length) {
                accessAmounts[index] = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return accessAmounts.length;
        }
    };
    private List<PneumaticNetworkManager.Entry> accessEntries = List.of();
    private long accessRevision = Long.MIN_VALUE;
    private final int machineSlotCount;

    public PneumaticStorageMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()), readKind(buffer));
    }

    public PneumaticStorageMenu(int id, Inventory inventory, PneumaticStorageBlockEntity storage) {
        this(id, inventory, storage, storage.kind());
    }

    private PneumaticStorageMenu(int id, Inventory inventory, PneumaticStorageBlockEntity storage,
                                 PneumaticStorageBlock.Kind fallbackKind) {
        super(HbmMenus.PNEUMATIC_STORAGE.get(), id);
        this.storage = storage;
        this.kind = storage == null ? fallbackKind : storage.kind();
        this.machineContainer = storage == null ? new SimpleContainer(slotCount(this.kind)) : storage;

        switch (this.kind) {
            case ACCESS -> {
                machineSlotCount = ACCESS_SLOTS;
                for (int row = 0; row < 6; row++) {
                    for (int column = 0; column < 8; column++) {
                        addSlot(new ReadOnlySlot(accessDisplay, column + row * 8, 42 + column * 18, 17 + row * 18));
                    }
                }
                addDataSlots(accessData);
                addPlayerInventory(inventory, 42, 169, 227);
                refreshAccess();
            }
            case CLUTTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.CLUTTER_SLOTS;
                for (int row = 0; row < 6; row++) {
                    for (int column = 0; column < 9; column++) {
                        addSlot(new Slot(machineContainer, column + row * 9, 8 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 8, 153, 211);
            }
            case MONO -> {
                machineSlotCount = PneumaticStorageBlockEntity.MONO_SLOTS;
                for (int row = 0; row < 3; row++) {
                    addSlot(new ReadOnlySlot(machineContainer, row, 8, 17 + row * 18));
                }
                addPlayerInventory(inventory, 8, 99, 157);
            }
            case IMPORTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.IMPORTER_SLOTS;
                for (int row = 0; row < 3; row++) {
                    for (int column = 0; column < 3; column++) {
                        addSlot(new Slot(machineContainer, column + row * 3, 62 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 8, 103, 161);
            }
            case EXPORTER -> {
                machineSlotCount = PneumaticStorageBlockEntity.EXPORTER_TOTAL_SLOTS;
                for (int row = 0; row < 3; row++) {
                    for (int column = 0; column < 3; column++) {
                        int index = column + row * 3;
                        addSlot(new ReadOnlySlot(machineContainer, index, 17 + column * 18, 17 + row * 18));
                        addSlot(new TakeOnlySlot(machineContainer, index + PneumaticStorageBlockEntity.EXPORTER_OUTPUT_START,
                                80 + column * 18, 17 + row * 18));
                    }
                }
                addPlayerInventory(inventory, 8, 103, 161);
            }
            default -> throw new IllegalStateException("Unhandled pneumatic storage kind " + this.kind);
        }
    }

    public PneumaticStorageBlock.Kind kind() {
        return kind;
    }

    public PneumaticStorageBlockEntity storage() {
        return storage;
    }

    public int accessAmount(int slot) {
        return slot >= 0 && slot < accessAmounts.length ? accessAmounts[slot] : 0;
    }

    @Override
    public void broadcastChanges() {
        refreshAccess();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (storage != null && !player.level().isClientSide) {
            if (kind == PneumaticStorageBlock.Kind.ACCESS && slotId >= 0 && slotId < ACCESS_SLOTS) {
                clickAccess(slotId, button, clickType, player);
                return;
            }
            if (kind == PneumaticStorageBlock.Kind.MONO && slotId >= 0
                    && slotId < PneumaticStorageBlockEntity.MONO_SLOTS) {
                storage.setMonoFilter(slotId, getCarried());
                return;
            }
            if (kind == PneumaticStorageBlock.Kind.EXPORTER && slotId >= 0
                    && slotId < PneumaticStorageBlockEntity.EXPORTER_FILTER_SLOTS * 2 && slotId % 2 == 0) {
                storage.setExporterFilter(slotId / 2, getCarried());
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (storage == null) {
            return false;
        }
        return switch (id) {
            case 0 -> {
                storage.cyclePressure();
                yield kind == PneumaticStorageBlock.Kind.CLUTTER || kind == PneumaticStorageBlock.Kind.MONO;
            }
            case 1 -> {
                storage.toggleContinuousRequest();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            case 2 -> {
                storage.cycleRequestMode();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            case 3 -> {
                storage.toggleRttyConfiguredMode();
                yield kind == PneumaticStorageBlock.Kind.EXPORTER;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        if (kind == PneumaticStorageBlock.Kind.ACCESS) {
            return quickMoveAccess(player, index);
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlotCount) {
            if (!slot.mayPickup(player) || !moveItemStackTo(stack, machineSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int targetStart = switch (kind) {
                case CLUTTER, IMPORTER -> 0;
                default -> machineSlotCount;
            };
            if (targetStart == machineSlotCount || !moveItemStackTo(stack, targetStart, machineSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage == null || storage.stillValid(player);
    }

    private ItemStack quickMoveAccess(Player player, int index) {
        if (storage == null || !(player.level() instanceof ServerLevel level)) {
            return ItemStack.EMPTY;
        }
        if (index < ACCESS_SLOTS) {
            if (index >= accessEntries.size()) {
                return ItemStack.EMPTY;
            }
            PneumaticNetworkManager.Entry entry = accessEntries.get(index);
            int desired = (int) Math.min(entry.amount(), entry.stack().getMaxStackSize());
            long extracted = PneumaticNetworkManager.extract(level, storage.getBlockPos(), entry.stack(), desired, false);
            if (extracted <= 0L) {
                return ItemStack.EMPTY;
            }
            ItemStack moved = entry.stack().copyWithCount((int) extracted);
            ItemStack result = moved.copy();
            if (!player.getInventory().add(moved) && !moved.isEmpty()) {
                PneumaticNetworkManager.insert(level, storage.getBlockPos(), moved, moved.getCount(), false);
            }
            refreshAccess();
            return result;
        }
        Slot playerSlot = slots.get(index);
        if (!playerSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = playerSlot.getItem().copy();
        long remainder = PneumaticNetworkManager.insert(level, storage.getBlockPos(), original, original.getCount(), false);
        int moved = original.getCount() - (int) remainder;
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        playerSlot.getItem().shrink(moved);
        if (playerSlot.getItem().isEmpty()) {
            playerSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            playerSlot.setChanged();
        }
        refreshAccess();
        return original.copyWithCount(moved);
    }

    private void clickAccess(int slot, int button, ClickType clickType, Player player) {
        if (!(player.level() instanceof ServerLevel level) || slot >= accessEntries.size()) {
            return;
        }
        PneumaticNetworkManager.Entry entry = accessEntries.get(slot);
        if (clickType == ClickType.QUICK_MOVE) {
            quickMoveAccess(player, slot);
            return;
        }
        if (clickType != ClickType.PICKUP) {
            return;
        }
        ItemStack carried = getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, entry.stack())) {
            int offered = button == 1 ? 1 : carried.getCount();
            long remainder = PneumaticNetworkManager.insert(level, storage.getBlockPos(), carried, offered, false);
            carried.shrink(offered - (int) remainder);
            setCarried(carried);
            refreshAccess();
            return;
        }
        int room = carried.isEmpty() ? entry.stack().getMaxStackSize() : carried.getMaxStackSize() - carried.getCount();
        int desired = button == 1 ? Math.min(1, room) : room;
        long extracted = PneumaticNetworkManager.extract(level, storage.getBlockPos(), entry.stack(), desired, false);
        if (extracted > 0L) {
            setCarried(carried.isEmpty()
                    ? entry.stack().copyWithCount((int) extracted)
                    : carried.copyWithCount(carried.getCount() + (int) extracted));
            refreshAccess();
        }
    }

    private void refreshAccess() {
        if (kind != PneumaticStorageBlock.Kind.ACCESS || storage == null
                || !(storage.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PneumaticNetworkManager.Snapshot snapshot = PneumaticNetworkManager.snapshot(level, storage.getBlockPos());
        if (snapshot.revision() == accessRevision) {
            return;
        }
        accessRevision = snapshot.revision();
        accessEntries = new ArrayList<>(snapshot.entries().subList(0, Math.min(ACCESS_SLOTS, snapshot.entries().size())));
        for (int slot = 0; slot < ACCESS_SLOTS; slot++) {
            if (slot < accessEntries.size()) {
                PneumaticNetworkManager.Entry entry = accessEntries.get(slot);
                accessDisplay.setItem(slot, entry.stack().copyWithCount(1));
                accessAmounts[slot] = (int) Math.min(Integer.MAX_VALUE, entry.amount());
            } else {
                accessDisplay.setItem(slot, ItemStack.EMPTY);
                accessAmounts[slot] = 0;
            }
        }
    }

    private void addPlayerInventory(Inventory inventory, int x, int inventoryY, int hotbarY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, hotbarY));
        }
    }

    private static int slotCount(PneumaticStorageBlock.Kind kind) {
        return switch (kind) {
            case ACCESS -> 0;
            case CLUTTER -> PneumaticStorageBlockEntity.CLUTTER_SLOTS;
            case MONO -> PneumaticStorageBlockEntity.MONO_SLOTS;
            case IMPORTER -> PneumaticStorageBlockEntity.IMPORTER_SLOTS;
            case EXPORTER -> PneumaticStorageBlockEntity.EXPORTER_TOTAL_SLOTS;
        };
    }

    private static PneumaticStorageBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof PneumaticStorageBlockEntity storage ? storage : null;
    }

    private static PneumaticStorageBlock.Kind readKind(RegistryFriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        PneumaticStorageBlock.Kind[] values = PneumaticStorageBlock.Kind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PneumaticStorageBlock.Kind.ACCESS;
    }

    private static class ReadOnlySlot extends Slot {
        private ReadOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
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
