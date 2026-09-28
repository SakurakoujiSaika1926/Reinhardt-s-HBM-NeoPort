package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PneumaticStorageBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.PneumaticStorageMenu;
import com.reinhardt.hbm.pneumatic.PneumaticNetworkManager;
import com.reinhardt.hbm.pneumatic.PneumaticStorageNode;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import com.reinhardt.hbm.rtty.RttyInfo;
import com.reinhardt.hbm.rtty.RttyInteractive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** Complete five-endpoint pneumatic storage implementation from latest 1.7.10. */
public final class PneumaticStorageBlockEntity extends BlockEntity
        implements MenuProvider, WorldlyContainer, MachineInventory, PneumaticStorageNode, RttyInteractive {
    public static final int AIR_CAPACITY = 4_000;
    public static final int MONO_CAPACITY = 100_000;
    public static final int CLUTTER_SLOTS = 54;
    public static final int MONO_SLOTS = 3;
    public static final int IMPORTER_SLOTS = 9;
    public static final int EXPORTER_FILTER_SLOTS = 9;
    public static final int EXPORTER_OUTPUT_START = 9;
    public static final int EXPORTER_TOTAL_SLOTS = 18;
    public static final int EXPORTER_RETRY_TICKS = 10;
    public static final int IMPORTER_RETRY_TICKS = 100;

    public static final int REQUEST_AS_MUCH_AS_POSSIBLE = 0;
    public static final int REQUEST_FULL_STACK = 1;
    public static final int REQUEST_FULL_REQUEST = 2;

    private final PneumaticStorageBlock.Kind kind;
    private final NonNullList<ItemStack> items;
    private final int[] allSlots;
    private final int[] exporterOutputSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final HbmFluidTank air = new HbmFluidTank(HbmFluids.byName("air").orElse(HbmFluids.none()), AIR_CAPACITY);
    private final int[] monoAmounts = new int[MONO_SLOTS];
    private final int[] delays = new int[IMPORTER_SLOTS];
    private final NonNullList<ItemStack> rttyFilters = NonNullList.withSize(EXPORTER_FILTER_SLOTS, ItemStack.EMPTY);
    private boolean continuousRequest;
    private boolean rttyConfiguredMode;
    private int requestMode;
    private boolean lastRedstone;

    public PneumaticStorageBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PNEUMATIC_STORAGE.get(), pos, state);
        this.kind = state.getBlock() instanceof PneumaticStorageBlock block
                ? block.kind()
                : PneumaticStorageBlock.Kind.ACCESS;
        int slots = switch (kind) {
            case ACCESS -> 0;
            case CLUTTER -> CLUTTER_SLOTS;
            case MONO -> MONO_SLOTS;
            case IMPORTER -> IMPORTER_SLOTS;
            case EXPORTER -> EXPORTER_TOTAL_SLOTS;
        };
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.allSlots = new int[slots];
        for (int index = 0; index < slots; index++) {
            allSlots[index] = index;
        }
        this.air.setPressure(1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumaticStorageBlockEntity storage) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (storage.kind) {
            case CLUTTER, MONO -> storage.tickAir(serverLevel);
            case IMPORTER -> storage.tickImporter(serverLevel);
            case EXPORTER -> storage.tickExporter(serverLevel);
            case ACCESS -> {
            }
        }
    }

    public PneumaticStorageBlock.Kind kind() {
        return kind;
    }

    public HbmFluidTank airTank() {
        return air;
    }

    public int pressure() {
        return Math.max(1, Math.min(5, air.pressure()));
    }

    public int maximumRange() {
        return rangeForPressure(pressure());
    }

    public static int rangeForPressure(int pressure) {
        return switch (pressure) {
            case 1 -> 10;
            case 2 -> 25;
            case 3 -> 100;
            case 4 -> 250;
            case 5 -> 1_000;
            default -> 0;
        };
    }

    public void cyclePressure() {
        if (!hasAirTank()) {
            return;
        }
        int next = pressure() + 1;
        if (next > 5) {
            next = 1;
        }
        air.setType(HbmFluids.byName("air").orElse(HbmFluids.none()));
        air.setPressure(next);
        sync();
    }

    public boolean continuousRequest() {
        return continuousRequest;
    }

    public int requestMode() {
        return requestMode;
    }

    public boolean rttyConfiguredMode() {
        return rttyConfiguredMode;
    }

    public int monoAmount(int slot) {
        return slot >= 0 && slot < monoAmounts.length ? monoAmounts[slot] : 0;
    }

    public void setMonoFilter(int slot, ItemStack stack) {
        if (kind != PneumaticStorageBlock.Kind.MONO || slot < 0 || slot >= MONO_SLOTS || monoAmounts[slot] > 0) {
            return;
        }
        items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        contentsChanged();
    }

    public void setExporterFilter(int slot, ItemStack stack) {
        if (kind != PneumaticStorageBlock.Kind.EXPORTER || slot < 0 || slot >= EXPORTER_FILTER_SLOTS) {
            return;
        }
        ItemStack filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!filter.isEmpty()) {
            filter.setCount(Math.max(1, Math.min(filter.getMaxStackSize(), filter.getCount())));
        }
        items.set(slot, filter);
        delays[slot] = 0;
        sync();
    }

    public void toggleContinuousRequest() {
        if (kind == PneumaticStorageBlock.Kind.EXPORTER) {
            continuousRequest = !continuousRequest;
            sync();
        }
    }

    public void cycleRequestMode() {
        if (kind == PneumaticStorageBlock.Kind.EXPORTER) {
            requestMode = (requestMode + 1) % 3;
            Arrays.fill(delays, 0);
            sync();
        }
    }

    public void toggleRttyConfiguredMode() {
        if (kind == PneumaticStorageBlock.Kind.EXPORTER) {
            rttyConfiguredMode = !rttyConfiguredMode;
            Arrays.fill(delays, 0);
            sync();
        }
    }

    public void requestNow() {
        if (kind == PneumaticStorageBlock.Kind.EXPORTER && level instanceof ServerLevel serverLevel) {
            request(serverLevel, true);
        }
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (!hasAirTank()) {
            return null;
        }
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? air.getFluidInTank(0) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? AIR_CAPACITY : 0;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && isAir(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!isAir(resource)) {
                    return 0;
                }
                HbmFluidDefinition airType = HbmFluids.byName("air").orElse(HbmFluids.none());
                int filled = air.fill(airType, resource.getAmount(), pressure(), action.simulate());
                if (filled > 0 && action.execute()) {
                    sync();
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (!isAir(resource)) {
                    return FluidStack.EMPTY;
                }
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                HbmFluidStack drained = air.drain(HbmFluids.byName("air").orElse(HbmFluids.none()), maxDrain, action.simulate());
                if (!drained.isEmpty() && action.execute()) {
                    sync();
                }
                return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
            }
        };
    }

    @Override
    public BlockPos pneumaticPosition() {
        return worldPosition;
    }

    @Override
    public boolean pneumaticAvailableFrom(BlockPos accessor) {
        if (!hasAirTank() || air.amount() <= 0) {
            return false;
        }
        int range = maximumRange();
        return worldPosition.distSqr(accessor) <= (double) range * range;
    }

    @Override
    public int pneumaticSlotCount() {
        return switch (kind) {
            case CLUTTER -> CLUTTER_SLOTS;
            case MONO -> MONO_SLOTS;
            default -> 0;
        };
    }

    @Override
    public ItemStack pneumaticPrototype(int slot) {
        if (slot < 0 || slot >= pneumaticSlotCount()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot).isEmpty() ? ItemStack.EMPTY : items.get(slot).copyWithCount(1);
    }

    @Override
    public long pneumaticAmount(int slot) {
        if (slot < 0 || slot >= pneumaticSlotCount()) {
            return 0L;
        }
        return kind == PneumaticStorageBlock.Kind.MONO ? monoAmounts[slot] : items.get(slot).getCount();
    }

    /** Returns the quantity that remains uninserted. */
    @Override
    public long pneumaticInsert(int slot, ItemStack stack, long amount, boolean simulate) {
        if (slot < 0 || slot >= pneumaticSlotCount() || stack.isEmpty() || amount <= 0L) {
            return Math.max(0L, amount);
        }
        if (kind == PneumaticStorageBlock.Kind.MONO) {
            ItemStack type = items.get(slot);
            if (type.isEmpty() || !ItemStack.isSameItemSameComponents(type, stack)) {
                return amount;
            }
            int accepted = (int) Math.min(amount, MONO_CAPACITY - monoAmounts[slot]);
            if (!simulate && accepted > 0) {
                monoAmounts[slot] += accepted;
                contentsChanged();
            }
            return amount - accepted;
        }

        ItemStack existing = items.get(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return amount;
        }
        int capacity = existing.isEmpty() ? stack.getMaxStackSize() : existing.getMaxStackSize();
        int room = existing.isEmpty() ? capacity : capacity - existing.getCount();
        int accepted = (int) Math.min(amount, Math.max(0, room));
        if (!simulate && accepted > 0) {
            items.set(slot, existing.isEmpty()
                    ? stack.copyWithCount(accepted)
                    : existing.copyWithCount(existing.getCount() + accepted));
            contentsChanged();
        }
        return amount - accepted;
    }

    /** Returns the quantity actually extracted. */
    @Override
    public long pneumaticExtract(int slot, ItemStack stack, long amount, boolean simulate) {
        if (slot < 0 || slot >= pneumaticSlotCount() || stack.isEmpty() || amount <= 0L) {
            return 0L;
        }
        ItemStack existing = items.get(slot);
        if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
            return 0L;
        }
        int available = kind == PneumaticStorageBlock.Kind.MONO ? monoAmounts[slot] : existing.getCount();
        int extracted = (int) Math.min(amount, available);
        if (!simulate && extracted > 0) {
            if (kind == PneumaticStorageBlock.Kind.MONO) {
                monoAmounts[slot] -= extracted;
            } else if (extracted == existing.getCount()) {
                items.set(slot, ItemStack.EMPTY);
            } else {
                items.set(slot, existing.copyWithCount(existing.getCount() - extracted));
            }
            contentsChanged();
        }
        return extracted;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        if (kind == PneumaticStorageBlock.Kind.MONO) {
            for (int amount : monoAmounts) {
                if (amount > 0) {
                    return false;
                }
            }
        }
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0 || isGhostSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            contentsChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot) || isGhostSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        if (isGhostSlot(slot)) {
            if (kind == PneumaticStorageBlock.Kind.MONO) {
                setMonoFilter(slot, stack);
            } else if (kind == PneumaticStorageBlock.Kind.EXPORTER) {
                setExporterFilter(slot, stack);
            }
            return;
        }
        ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > stored.getMaxStackSize()) {
            stored.setCount(stored.getMaxStackSize());
        }
        items.set(slot, stored);
        if (kind == PneumaticStorageBlock.Kind.IMPORTER && !stored.isEmpty()) {
            delays[slot] = Math.max(delays[slot], 1);
        }
        contentsChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return validSlot(slot) && !isGhostSlot(slot) && kind != PneumaticStorageBlock.Kind.EXPORTER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return kind == PneumaticStorageBlock.Kind.EXPORTER ? exporterOutputSlots : allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return kind == PneumaticStorageBlock.Kind.CLUTTER || kind == PneumaticStorageBlock.Kind.IMPORTER;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return kind == PneumaticStorageBlock.Kind.CLUTTER
                || kind == PneumaticStorageBlock.Kind.EXPORTER && slot >= EXPORTER_OUTPUT_START;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 128.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        Arrays.fill(monoAmounts, 0);
        contentsChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.pneumatic_storage_" + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PneumaticStorageMenu(id, inventory, this);
    }

    public void dropRealContents(Level level, BlockPos pos) {
        if (kind == PneumaticStorageBlock.Kind.CLUTTER || kind == PneumaticStorageBlock.Kind.IMPORTER) {
            dropRange(level, pos, 0, items.size());
        } else if (kind == PneumaticStorageBlock.Kind.EXPORTER) {
            dropRange(level, pos, EXPORTER_OUTPUT_START, EXPORTER_TOTAL_SLOTS);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        dropRealContents(level, pos);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            PneumaticNetworkManager.invalidate(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            PneumaticNetworkManager.invalidate(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("air", air.save());
        tag.putIntArray("mono_amounts", monoAmounts);
        tag.putIntArray("delays", delays);
        tag.putBoolean("continuous_request", continuousRequest);
        tag.putBoolean("rtty_configured_mode", rttyConfiguredMode);
        tag.putInt("request_mode", requestMode);
        tag.putBoolean("last_redstone", lastRedstone);
        CompoundTag rttyTag = new CompoundTag();
        ContainerHelper.saveAllItems(rttyTag, rttyFilters, registries);
        tag.put("rtty_filters", rttyTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains("air")) {
            air.load(tag.getCompound("air"));
        }
        copyArray(tag.getIntArray("mono_amounts"), monoAmounts);
        copyArray(tag.getIntArray("delays"), delays);
        continuousRequest = tag.getBoolean("continuous_request");
        rttyConfiguredMode = tag.getBoolean("rtty_configured_mode");
        requestMode = Math.max(0, Math.min(2, tag.getInt("request_mode")));
        lastRedstone = tag.getBoolean("last_redstone");
        rttyFilters.clear();
        if (tag.contains("rtty_filters")) {
            ContainerHelper.loadAllItems(tag.getCompound("rtty_filters"), rttyFilters, registries);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickAir(ServerLevel level) {
        if (air.amount() <= 0) {
            return;
        }
        int consumption = air.amount() * 9 / AIR_CAPACITY + 1;
        air.setAmount(air.amount() - consumption);
        if (air.amount() == 0 || level.getGameTime() % 20L == 0L) {
            sync();
        } else {
            setChanged();
        }
    }

    private void tickImporter(ServerLevel level) {
        boolean changed = false;
        for (int slot = 0; slot < IMPORTER_SLOTS; slot++) {
            if (delays[slot] > 0) {
                delays[slot]--;
                continue;
            }
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            long remainder = PneumaticNetworkManager.insert(level, worldPosition, stack, stack.getCount(), false);
            int moved = stack.getCount() - (int) remainder;
            if (moved <= 0) {
                delays[slot] = IMPORTER_RETRY_TICKS;
            } else {
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                changed = true;
            }
        }
        if (changed) {
            sync();
        } else {
            setChanged();
        }
    }

    private void tickExporter(ServerLevel level) {
        for (int slot = 0; slot < delays.length; slot++) {
            if (delays[slot] > 0) {
                delays[slot]--;
            }
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (continuousRequest) {
            request(level, false);
        } else if (powered && !lastRedstone) {
            request(level, true);
        }
        if (lastRedstone != powered) {
            lastRedstone = powered;
            setChanged();
        }
    }

    private void request(ServerLevel level, boolean force) {
        if (requestMode == REQUEST_FULL_REQUEST) {
            requestAllOrNothing(level, force);
            return;
        }
        for (int slot = 0; slot < EXPORTER_FILTER_SLOTS; slot++) {
            if (!requestSlot(level, slot, force)) {
                delays[slot] = EXPORTER_RETRY_TICKS;
            }
        }
    }

    private boolean requestSlot(ServerLevel level, int slot, boolean force) {
        if (!force && delays[slot] > 0) {
            return true;
        }
        ItemStack filter = activeExporterFilter(slot);
        if (filter.isEmpty()) {
            return false;
        }
        ItemStack output = items.get(slot + EXPORTER_OUTPUT_START);
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, filter)) {
            return false;
        }
        int existing = output.getCount();
        int room = filter.getMaxStackSize() - existing;
        int requested = filter.getCount();
        if (requestMode != REQUEST_AS_MUCH_AS_POSSIBLE && room < requested) {
            return false;
        }
        long available = PneumaticNetworkManager.available(level, worldPosition, filter);
        if (requestMode != REQUEST_AS_MUCH_AS_POSSIBLE && available < requested) {
            return false;
        }
        int pull = (int) Math.min(Math.min((long) requested, available), room);
        if (pull <= 0) {
            return false;
        }
        long extracted = PneumaticNetworkManager.extract(level, worldPosition, filter, pull, false);
        if (extracted <= 0L) {
            return false;
        }
        items.set(slot + EXPORTER_OUTPUT_START, filter.copyWithCount(existing + (int) extracted));
        sync();
        return true;
    }

    private void requestAllOrNothing(ServerLevel level, boolean force) {
        for (int slot = 0; slot < EXPORTER_FILTER_SLOTS; slot++) {
            ItemStack filter = activeExporterFilter(slot);
            if (filter.isEmpty()) {
                continue;
            }
            if (!force && delays[slot] > 0) {
                return;
            }
            ItemStack output = items.get(slot + EXPORTER_OUTPUT_START);
            if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, filter)) {
                delays[slot] = EXPORTER_RETRY_TICKS;
                return;
            }
            int room = filter.getMaxStackSize() - output.getCount();
            if (room < filter.getCount()
                    || PneumaticNetworkManager.available(level, worldPosition, filter) < filter.getCount()) {
                delays[slot] = EXPORTER_RETRY_TICKS;
                return;
            }
        }

        for (int slot = 0; slot < EXPORTER_FILTER_SLOTS; slot++) {
            ItemStack filter = activeExporterFilter(slot);
            if (filter.isEmpty()) {
                continue;
            }
            ItemStack output = items.get(slot + EXPORTER_OUTPUT_START);
            int existing = output.getCount();
            long extracted = PneumaticNetworkManager.extract(level, worldPosition, filter, filter.getCount(), false);
            items.set(slot + EXPORTER_OUTPUT_START, filter.copyWithCount(existing + (int) extracted));
        }
        sync();
    }

    private ItemStack activeExporterFilter(int slot) {
        if (slot < 0 || slot >= EXPORTER_FILTER_SLOTS) return ItemStack.EMPTY;
        return rttyConfiguredMode ? rttyFilters.get(slot) : items.get(slot);
    }

    @Override
    public String[] rttyFunctionInfo() {
        return new String[] {
                RttyInfo.PREFIX_FUNCTION + "setfilter" + RttyInteractive.NAME_SEPARATOR
                        + "slot" + RttyInteractive.PARAM_SEPARATOR + "itemid"
                        + RttyInteractive.PARAM_SEPARATOR + "itemmeta"
                        + RttyInteractive.PARAM_SEPARATOR + "amount",
                RttyInfo.PREFIX_FUNCTION + "setcontinuous" + RttyInteractive.NAME_SEPARATOR + "on/off",
                RttyInfo.PREFIX_FUNCTION + "request",
                RttyInfo.PREFIX_FUNCTION + "requestslot" + RttyInteractive.NAME_SEPARATOR + "slot",
                RttyInfo.PREFIX_FUNCTION + "checkavailability" + RttyInteractive.NAME_SEPARATOR
                        + "itemid" + RttyInteractive.PARAM_SEPARATOR + "itemmeta"
                        + RttyInteractive.PARAM_SEPARATOR + "returnchannel"
        };
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if (kind != PneumaticStorageBlock.Kind.EXPORTER || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (name.equals(RttyInfo.PREFIX_FUNCTION + "setfilter") && params.length == 4) {
            int slot = RttyInteractive.parseInt(params[0], 1, EXPORTER_FILTER_SLOTS) - 1;
            int itemId = RttyInteractive.parseInt(params[1], 0, Short.MAX_VALUE);
            // Metadata was removed by item flattening, but remains a validated
            // field in the stable on-air command format.
            RttyInteractive.parseInt(params[2], 0, Short.MAX_VALUE);
            int amount = RttyInteractive.parseInt(params[3], 1, 64);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.byId(itemId);
            rttyFilters.set(slot, item == null || item == Items.AIR
                    ? ItemStack.EMPTY : new ItemStack(item, amount));
            delays[slot] = 0;
            sync();
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "setcontinuous") && params.length == 1) {
            if (params[0].equals("on")) continuousRequest = true;
            if (params[0].equals("off")) continuousRequest = false;
            sync();
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "request")) {
            request(serverLevel, true);
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "requestslot") && params.length == 1) {
            int slot = RttyInteractive.parseInt(params[0], 1, EXPORTER_FILTER_SLOTS) - 1;
            if (!requestSlot(serverLevel, slot, true)) delays[slot] = EXPORTER_RETRY_TICKS;
        } else if (name.equals(RttyInfo.PREFIX_FUNCTION + "checkavailability") && params.length == 3) {
            int itemId = RttyInteractive.parseInt(params[0], 0, Short.MAX_VALUE);
            RttyInteractive.parseInt(params[1], 0, Short.MAX_VALUE);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.byId(itemId);
            long available = item == null || item == Items.AIR ? 0L
                    : PneumaticNetworkManager.available(serverLevel, worldPosition, new ItemStack(item));
            HbmRttySystem.broadcast(serverLevel, params[2], Long.toString(available));
        }
        return null;
    }

    private boolean hasAirTank() {
        return kind == PneumaticStorageBlock.Kind.CLUTTER || kind == PneumaticStorageBlock.Kind.MONO;
    }

    private boolean isGhostSlot(int slot) {
        return kind == PneumaticStorageBlock.Kind.MONO
                || kind == PneumaticStorageBlock.Kind.EXPORTER && slot < EXPORTER_FILTER_SLOTS;
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < items.size();
    }

    private static boolean isAir(FluidStack stack) {
        return !stack.isEmpty() && HbmFluids.fromNeoFluid(stack.getFluid())
                .map(fluid -> fluid.name().equals("air"))
                .orElse(false);
    }

    private void contentsChanged() {
        setChanged();
        if (level != null) {
            PneumaticNetworkManager.markContentsChanged(level, worldPosition);
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void sync() {
        contentsChanged();
    }

    private void dropRange(Level level, BlockPos pos, int from, int to) {
        for (int slot = from; slot < to; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
                items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void copyArray(int[] source, int[] target) {
        Arrays.fill(target, 0);
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }
}
