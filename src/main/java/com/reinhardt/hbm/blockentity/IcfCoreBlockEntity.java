package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.IcfCoreBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.IcfPelletItem;
import com.reinhardt.hbm.menu.IcfCoreMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Exact inventory and heat loop port of TileEntityICF. */
public final class IcfCoreBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, MachineInventory {
    public static final int FRESH_START = 0;
    public static final int FRESH_END = 5;
    public static final int ACTIVE_SLOT = 5;
    public static final int DEPLETED_START = 6;
    public static final int DEPLETED_END = 11;
    public static final int IDENTIFIER_SLOT = 11;
    public static final int SLOT_COUNT = 12;
    public static final int SODIUM_CAPACITY = 512_000;
    public static final int FLUX_CAPACITY = 24_000;
    public static final long MAX_HEAT = 1_000_000_000_000L;
    public static final int DATA_COUNT = 15;

    private static final int[] AUTOMATION_SLOTS = {0, 1, 2, 3, 4, 6, 7, 8, 9, 10};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank sodium = new HbmFluidTank(fluid("sodium"), SODIUM_CAPACITY);
    private final HbmFluidTank hotSodium = new HbmFluidTank(fluid("sodium_hot"), SODIUM_CAPACITY);
    private final HbmFluidTank stellarFlux = new HbmFluidTank(fluid("stellar_flux"), FLUX_CAPACITY);
    private final IFluidHandler fluidHandler = new IcfFluidHandler();
    private long laser;
    private long maxLaser;
    private long heat;
    private long heatup;
    private int consumption;
    private int output;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> sodium.type().oldId();
                case 1 -> sodium.amount();
                case 2 -> hotSodium.type().oldId();
                case 3 -> hotSodium.amount();
                case 4 -> stellarFlux.type().oldId();
                case 5 -> stellarFlux.amount();
                case 6 -> low(laser);
                case 7 -> high(laser);
                case 8 -> low(maxLaser);
                case 9 -> high(maxLaser);
                case 10 -> low(heat);
                case 11 -> high(heat);
                case 12 -> low(heatup);
                case 13 -> high(heatup);
                case 14 -> consumption;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> sodium.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> sodium.setAmount(value);
                case 2 -> hotSodium.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 3 -> hotSodium.setAmount(value);
                case 4 -> stellarFlux.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 5 -> stellarFlux.setAmount(value);
                case 6 -> laser = combine(value, high(laser));
                case 7 -> laser = combine(low(laser), value);
                case 8 -> maxLaser = combine(value, high(maxLaser));
                case 9 -> maxLaser = combine(low(maxLaser), value);
                case 10 -> heat = combine(value, high(heat));
                case 11 -> heat = combine(low(heat), value);
                case 12 -> heatup = combine(value, high(heatup));
                case 13 -> heatup = combine(low(heatup), value);
                case 14 -> consumption = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IcfCoreBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_CORE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfCoreBlockEntity core) {
        if (!level.isClientSide) {
            core.tickServer(level);
        }
    }

    public static List<Port> portsFor(BlockPos corePos, Direction facing) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                new Port(corePos.above(5), Direction.UP),
                new Port(corePos, Direction.DOWN),
                new Port(corePos.relative(facing, 3).relative(rot, 6).above(3), facing),
                new Port(corePos.relative(facing, 3).relative(rot, -6).above(3), facing),
                new Port(corePos.relative(facing, -3).relative(rot, 6).above(3), facing.getOpposite()),
                new Port(corePos.relative(facing, -3).relative(rot, -6).above(3), facing.getOpposite())
        );
    }

    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        return portsFor(this.worldPosition, facing);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return null;
        }
        for (Port port : ports(this.level)) {
            if (port.proxyPos().equals(queriedPos) && (side == null || side == port.exposedFace())) {
                return this.fluidHandler;
            }
        }
        return null;
    }

    public HbmFluidTank sodiumTank() {
        return sodium;
    }

    public HbmFluidTank hotSodiumTank() {
        return hotSodium;
    }

    public HbmFluidTank stellarFluxTank() {
        return stellarFlux;
    }

    public long laser() {
        return laser;
    }

    public long maxLaser() {
        return maxLaser;
    }

    public long heat() {
        return heat;
    }

    public long heatup() {
        return heatup;
    }

    public int consumption() {
        return consumption;
    }

    public int output() {
        return output;
    }

    public void receiveLaser(long power, long available) {
        this.laser = Math.max(0L, this.laser + Math.max(0L, power));
        this.maxLaser = Math.max(this.maxLaser, Math.max(0L, available));
        markDirtyAndSync();
    }

    public ContainerData menuData() {
        return menuData;
    }

    private void tickServer(Level level) {
        applyFluidIdentifier();
        boolean changed = movePellets();
        this.heatup = 0L;

        ItemStack active = this.items.get(ACTIVE_SLOT);
        if (active.is(HbmItems.ICF_PELLET.get()) && IcfPelletItem.getFusingDifficulty(active) <= this.laser) {
            this.heatup = IcfPelletItem.react(active, this.laser);
            this.heat = Math.min(MAX_HEAT, this.heat + this.heatup);
            if (IcfPelletItem.getDepletion(active) >= IcfPelletItem.getMaxDepletion(active)) {
                this.items.set(ACTIVE_SLOT, new ItemStack(HbmItems.ICF_PELLET_DEPLETED.get()));
                changed = true;
            }
            stellarFlux.fill(stellarFlux.type(), (int) Math.ceil(this.heat * 10.0D / MAX_HEAT), false);
        }
        if (this.heatup == 0L) {
            this.heat = Math.min(MAX_HEAT, this.heat + (long) (this.laser * 0.25D));
        }

        this.consumption = 0;
        this.output = 0;
        convertSodium();
        sendOutputs(level);
        this.heat = (long) (this.heat * 0.999D);
        this.laser = 0L;
        this.maxLaser = 0L;
        if (changed || level.getGameTime() % 5L == 0L) {
            markDirtyAndSync();
        } else {
            setChanged();
        }
    }

    private void applyFluidIdentifier() {
        ItemStack identifier = this.items.get(IDENTIFIER_SLOT);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition type = FluidIdentifierItem.primary(identifier);
        HbmThermalConversions.firstIcfStep(type).ifPresent(step -> {
            if (sodium.amount() == 0 && sodium.type() != step.input()) {
                sodium.setType(step.input());
            }
            if (hotSodium.amount() == 0 && hotSodium.type() != step.output()) {
                hotSodium.setType(step.output());
            }
        });
    }

    private boolean movePellets() {
        boolean changed = false;
        if (this.items.get(ACTIVE_SLOT).is(HbmItems.ICF_PELLET_DEPLETED.get())) {
            for (int slot = DEPLETED_START; slot < DEPLETED_END; slot++) {
                if (this.items.get(slot).isEmpty()) {
                    this.items.set(slot, this.items.get(ACTIVE_SLOT).copy());
                    this.items.set(ACTIVE_SLOT, ItemStack.EMPTY);
                    changed = true;
                    break;
                }
            }
        }
        if (this.items.get(ACTIVE_SLOT).isEmpty()) {
            for (int slot = FRESH_START; slot < FRESH_END; slot++) {
                if (this.items.get(slot).is(HbmItems.ICF_PELLET.get())) {
                    this.items.set(ACTIVE_SLOT, this.items.get(slot).copy());
                    this.items.set(slot, ItemStack.EMPTY);
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private void convertSodium() {
        HbmThermalConversions.firstIcfStep(sodium.type()).ifPresent(step -> {
            if (hotSodium.amount() == 0 && hotSodium.type() != step.output()) {
                hotSodium.setType(step.output());
            }
            int sourceCycles = sodium.amount() / step.amountReq();
            int targetCycles = (hotSodium.capacity() - hotSodium.amount()) / step.amountProduced();
            long heatCycles = Math.min((long) (heat / 4.0D / step.heatReq()), heat / step.heatReq());
            int cycles = (int) Math.min(Math.min(sourceCycles, targetCycles), heatCycles);
            if (cycles <= 0) {
                return;
            }
            sodium.drain(step.input(), step.amountReq() * cycles, false);
            hotSodium.fill(step.output(), step.amountProduced() * cycles, false);
            heat -= (long) step.heatReq() * cycles;
            consumption = step.amountReq() * cycles;
            output = step.amountProduced() * cycles;
        });
    }

    private void sendOutputs(Level level) {
        for (Port port : ports(level)) {
            BlockPos target = port.proxyPos().relative(port.exposedFace());
            sendTank(level, hotSodium, target, port.exposedFace());
            sendTank(level, stellarFlux, target, port.exposedFace());
        }
    }

    private void sendTank(Level level, HbmFluidTank tank, BlockPos target, Direction face) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        FluidStack offered = HbmFluids.toNeoStack(tank.type(), tank.amount());
        int accepted = HbmFluidNetworks.fillInto(level, target, face.getOpposite(), offered, this.worldPosition, true);
        if (accepted > 0) {
            tank.drain(tank.type(), accepted, false);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            markDirtyAndSync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT || !canPlaceItem(slot, stack)) {
            return;
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        markDirtyAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return (slot >= FRESH_START && slot < FRESH_END && stack.is(HbmItems.ICF_PELLET.get()))
                || (slot == IDENTIFIER_SLOT && stack.getItem() instanceof FluidIdentifierItem);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= DEPLETED_START && slot < DEPLETED_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < SLOT_COUNT; index++) {
            items.set(index, ItemStack.EMPTY);
        }
        markDirtyAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.icf");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new IcfCoreMenu(containerId, inventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("Sodium", sodium.save());
        tag.put("HotSodium", hotSodium.save());
        tag.put("StellarFlux", stellarFlux.save());
        tag.putLong("Heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        sodium.load(tag.getCompound("Sodium"));
        hotSodium.load(tag.getCompound("HotSodium"));
        stellarFlux.load(tag.getCompound("StellarFlux"));
        heat = Math.clamp(tag.getLong("Heat"), 0L, MAX_HEAT);
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

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(level)) {
                level.invalidateCapabilities(port.proxyPos());
            }
        }
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private static int low(long value) {
        return (int) value;
    }

    private static int high(long value) {
        return (int) (value >>> 32);
    }

    private static long combine(int low, int high) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }

    public record Port(BlockPos proxyPos, Direction exposedFace) {
    }

    private final class IcfFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> sodium.getFluidInTank(0);
                case 1 -> hotSodium.getFluidInTank(0);
                case 2 -> stellarFlux.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> sodium.capacity();
                case 1 -> hotSodium.capacity();
                case 2 -> stellarFlux.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid())
                    .flatMap(HbmThermalConversions::firstIcfStep)
                    .map(step -> step.input() == sodium.type())
                    .orElse(false);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition supplied = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (HbmThermalConversions.firstIcfStep(supplied).isEmpty() || supplied != sodium.type()) {
                return 0;
            }
            int filled = sodium.fill(supplied, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                markDirtyAndSync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidStack drained = hotSodium.drain(requested, resource.getAmount(), action.simulate());
            if (drained.isEmpty()) {
                drained = stellarFlux.drain(requested, resource.getAmount(), action.simulate());
            }
            if (!drained.isEmpty() && action.execute()) {
                markDirtyAndSync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = hotSodium.drain(null, maxDrain, action.simulate());
            if (drained.isEmpty()) {
                drained = stellarFlux.drain(null, maxDrain, action.simulate());
            }
            if (!drained.isEmpty() && action.execute()) {
                markDirtyAndSync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }

}
