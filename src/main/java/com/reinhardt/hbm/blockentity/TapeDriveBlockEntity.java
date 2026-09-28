package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.TapeDriveBlock;
import com.reinhardt.hbm.menu.TapeDriveMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Exact twelve-drive inventory and ten-tick satellite data exchange. */
public final class TapeDriveBlockEntity extends BlockEntity
        implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 12;
    public static final byte SLOT_EMPTY = 0;
    public static final byte SLOT_ANY = 1;
    public static final byte SLOT_EMPTY_DRIVE = 2;
    public static final byte SLOT_FILLED_DRIVE = 3;

    private static final int[] NO_AUTOMATION_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final byte[] visualSlots = new byte[SLOT_COUNT];

    public TapeDriveBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.TAPE_DRIVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TapeDriveBlockEntity drive) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || level.getGameTime() % 10L != 0L
                || !state.hasProperty(TapeDriveBlock.FACING)) {
            return;
        }

        BlockPos stationPos = pos.relative(state.getValue(TapeDriveBlock.FACING).getOpposite());
        BlockEntity adjacent = level.getBlockEntity(stationPos);
        SatelliteGroundStationBlockEntity station = adjacent instanceof SatelliteGroundStationBlockEntity direct
                ? direct
                : adjacent instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof SatelliteGroundStationBlockEntity core
                ? core
                : null;
        if (station == null || !station.connected()) {
            return;
        }

        SatelliteSavedData.SatelliteRecord satellite = SatelliteSavedData.get(serverLevel)
                .satellite(station.frequency()).orElse(null);
        if (satellite == null) {
            return;
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack input = drive.items.get(slot);
            if (!isDrive(input)) {
                continue;
            }
            ItemStack output = satellite.exchangeDrive(serverLevel, input);
            if (!output.isEmpty()) {
                drive.items.set(slot, output);
                drive.inventoryChanged();
                break;
            }
        }
    }

    public byte visualSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? this.visualSlots[slot] : SLOT_EMPTY;
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY; }
    @Override public int getMaxStackSize() { return 1; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            inventoryChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(this.items, slot);
        if (!removed.isEmpty()) {
            inventoryChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        ItemStack stored = stack;
        if (!stored.isEmpty() && stored.getCount() > 1) {
            stored = stored.copyWithCount(1);
        }
        this.items.set(slot, stored);
        inventoryChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT && isDrive(stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return NO_AUTOMATION_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        this.items.clear();
        inventoryChanged();
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("container.reinhardtshbm.tape_drive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TapeDriveMenu(containerId, inventory, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        refreshVisualSlots();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putByteArray("visualSlots", this.visualSlots);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        byte[] incoming = tag.getByteArray("visualSlots");
        System.arraycopy(incoming, 0, this.visualSlots, 0, Math.min(incoming.length, SLOT_COUNT));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void inventoryChanged() {
        refreshVisualSlots();
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void refreshVisualSlots() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.visualSlots[slot] = visualType(this.items.get(slot));
        }
    }

    private static byte visualType(ItemStack stack) {
        if (stack.isEmpty()) return SLOT_EMPTY;
        if (!isDrive(stack) || stack.is(HbmItems.DRIVE_FLASH_BROKEN.get())
                || stack.is(HbmItems.DRIVE_DISK_BROKEN.get())) return SLOT_ANY;
        if (stack.is(HbmItems.DRIVE_FLASH_EMPTY.get()) || stack.is(HbmItems.DRIVE_DISK_EMPTY.get())) {
            return SLOT_EMPTY_DRIVE;
        }
        return SLOT_FILLED_DRIVE;
    }

    public static boolean isDrive(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(HbmItems.DRIVE_FLASH_EMPTY.get())
                || stack.is(HbmItems.DRIVE_DISK_EMPTY.get())
                || stack.is(HbmItems.DRIVE_FLASH_BROKEN.get())
                || stack.is(HbmItems.DRIVE_DISK_BROKEN.get())
                || stack.is(HbmItems.DRIVE_FLASH_FLIGHTSIM.get())
                || stack.is(HbmItems.DRIVE_FLASH_PARTICLESIM.get())
                || stack.is(HbmItems.DRIVE_DISK_FLIGHTDATA.get())
                || stack.is(HbmItems.DRIVE_DISK_FLIGHTDATA_PROCESSED.get())
                || stack.is(HbmItems.DRIVE_DISK_ORBITDATA.get())
                || stack.is(HbmItems.DRIVE_DISK_ORBITDATA_PROCESSED.get())
                || stack.is(HbmItems.DRIVE_KLAUS.get()));
    }
}
