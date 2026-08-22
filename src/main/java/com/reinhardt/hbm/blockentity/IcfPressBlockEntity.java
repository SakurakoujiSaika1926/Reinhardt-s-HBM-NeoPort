package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.IcfPelletItem;
import com.reinhardt.hbm.menu.IcfPressMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public final class IcfPressBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, MachineInventory, FluidCopiable {
    public static final int EMPTY_PELLET_SLOT = 0;
    public static final int FILLED_PELLET_SLOT = 1;
    public static final int MUON_INPUT_SLOT = 2;
    public static final int MUON_OUTPUT_SLOT = 3;
    public static final int LEFT_SOLID_FUEL_SLOT = 4;
    public static final int RIGHT_SOLID_FUEL_SLOT = 5;
    public static final int LEFT_IDENTIFIER_SLOT = 6;
    public static final int RIGHT_IDENTIFIER_SLOT = 7;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 7;
    public static final int TANK_CAPACITY = 16_000;
    public static final int MAX_MUON = 16;

    private static final int[] TOP_BOTTOM_SLOTS = {0, 1, 2, 3, 4};
    private static final int[] SIDE_SLOTS = {0, 1, 2, 3, 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidDefinition[] configuredFuel = {
            fluid("deuterium"),
            fluid("tritium")
    };
    private final HbmFluidTank[] tanks = {
            new HbmFluidTank(this.configuredFuel[0], TANK_CAPACITY),
            new HbmFluidTank(this.configuredFuel[1], TANK_CAPACITY)
    };
    private int muon;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> IcfPressBlockEntity.this.muon;
                case 1 -> IcfPressBlockEntity.this.configuredFuel[0].oldId();
                case 2 -> IcfPressBlockEntity.this.tanks[0].amount();
                case 3 -> IcfPressBlockEntity.this.tanks[0].pressure();
                case 4 -> IcfPressBlockEntity.this.configuredFuel[1].oldId();
                case 5 -> IcfPressBlockEntity.this.tanks[1].amount();
                case 6 -> IcfPressBlockEntity.this.tanks[1].pressure();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> IcfPressBlockEntity.this.muon = value;
                case 1 -> IcfPressBlockEntity.this.setFuelType(0, HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 2 -> IcfPressBlockEntity.this.tanks[0].setAmount(value);
                case 3 -> IcfPressBlockEntity.this.tanks[0].setPressure(value);
                case 4 -> IcfPressBlockEntity.this.setFuelType(1, HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 5 -> IcfPressBlockEntity.this.tanks[1].setAmount(value);
                case 6 -> IcfPressBlockEntity.this.tanks[1].setPressure(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IcfPressBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_PRESS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfPressBlockEntity press) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = press.applyFluidIdentifiers();
        changed |= press.captureMuonCapsule();
        changed |= press.pressPellet();
        if (changed) {
            press.setChangedAndSync();
        }
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank tank(int index) {
        return this.tanks[index];
    }

    public HbmFluidDefinition configuredFuel(int index) {
        return this.configuredFuel[index];
    }

    public int muon() {
        return this.muon;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new IcfPressFluidHandler();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChangedAndSync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case EMPTY_PELLET_SLOT -> stack.is(HbmItems.ICF_PELLET_EMPTY.get());
            case MUON_INPUT_SLOT -> stack.is(HbmItems.PARTICLE_MUON.get());
            case LEFT_SOLID_FUEL_SLOT, RIGHT_SOLID_FUEL_SLOT -> !stack.isEmpty();
            case LEFT_IDENTIFIER_SLOT, RIGHT_IDENTIFIER_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? TOP_BOTTOM_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return contains(getSlotsForFace(side == null ? Direction.NORTH : side), slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == FILLED_PELLET_SLOT || slot == MUON_OUTPUT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_icf_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new IcfPressMenu(containerId, inventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stack.copy()
                ));
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.configuredFuel[0].oldId(), this.configuredFuel[1].oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        setFuelType(0, fluid);
        setChangedAndSync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.put("Tank0", this.tanks[0].save());
        tag.put("Tank1", this.tanks[1].save());
        tag.putString("FuelType0", this.configuredFuel[0].name());
        tag.putString("FuelType1", this.configuredFuel[1].name());
        tag.putByte("Muon", (byte) this.muon);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.configuredFuel[0] = HbmFluids.byName(tag.getString("FuelType0")).orElse(fluid("deuterium"));
        this.configuredFuel[1] = HbmFluids.byName(tag.getString("FuelType1")).orElse(fluid("tritium"));
        this.tanks[0].load(tag.getCompound("Tank0"));
        this.tanks[1].load(tag.getCompound("Tank1"));
        normalizeTank(0);
        normalizeTank(1);
        this.muon = Math.max(0, Math.min(MAX_MUON, tag.getByte("Muon")));
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

    private boolean applyFluidIdentifiers() {
        boolean changed = false;
        changed |= applyIdentifier(LEFT_IDENTIFIER_SLOT, 0);
        changed |= applyIdentifier(RIGHT_IDENTIFIER_SLOT, 1);
        return changed;
    }

    private boolean applyIdentifier(int slot, int tankIndex) {
        ItemStack identifier = this.items.get(slot);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (selected.isNone() || selected == this.configuredFuel[tankIndex]) {
            return false;
        }
        setFuelType(tankIndex, selected);
        return true;
    }

    private boolean captureMuonCapsule() {
        if (this.muon > 0) {
            return false;
        }
        ItemStack input = this.items.get(MUON_INPUT_SLOT);
        if (!input.is(HbmItems.PARTICLE_MUON.get())) {
            return false;
        }

        ItemStack container = input.getItem().hasCraftingRemainingItem(input)
                ? input.getItem().getCraftingRemainingItem(input)
                : ItemStack.EMPTY;
        if (!canStore(MUON_OUTPUT_SLOT, container)) {
            return false;
        }
        store(MUON_OUTPUT_SLOT, container);
        input.shrink(1);
        if (input.isEmpty()) {
            this.items.set(MUON_INPUT_SLOT, ItemStack.EMPTY);
        }
        this.muon = MAX_MUON;
        return true;
    }

    private boolean pressPellet() {
        ItemStack emptyPellet = this.items.get(EMPTY_PELLET_SLOT);
        if (!emptyPellet.is(HbmItems.ICF_PELLET_EMPTY.get()) || !this.items.get(FILLED_PELLET_SLOT).isEmpty()) {
            return false;
        }

        FuelUse first = getFuel(0, LEFT_SOLID_FUEL_SLOT);
        FuelUse second = getFuel(1, RIGHT_SOLID_FUEL_SLOT);
        if (first == null || second == null || first.fuel() == second.fuel()) {
            return false;
        }

        this.items.set(FILLED_PELLET_SLOT, IcfPelletItem.setup(first.fuel(), second.fuel(), this.muon > 0));
        if (this.muon > 0) {
            this.muon--;
        }
        emptyPellet.shrink(1);
        if (emptyPellet.isEmpty()) {
            this.items.set(EMPTY_PELLET_SLOT, ItemStack.EMPTY);
        }
        consumeFuel(first, 0, LEFT_SOLID_FUEL_SLOT);
        consumeFuel(second, 1, RIGHT_SOLID_FUEL_SLOT);
        return true;
    }

    @Nullable
    private FuelUse getFuel(int tankIndex, int solidSlot) {
        HbmFluidTank tank = this.tanks[tankIndex];
        IcfPelletItem.Fuel fluidFuel = fuelForFluid(this.configuredFuel[tankIndex]);
        if (tank.amount() >= 1000 && tank.type() == this.configuredFuel[tankIndex] && fluidFuel != null) {
            return new FuelUse(fluidFuel, true);
        }

        ItemStack stack = this.items.get(solidSlot);
        Optional<FoundryMaterialStack> materialStack = FoundryMaterial.materialFromItem(stack);
        if (materialStack.isEmpty() || materialStack.get().amount() != FoundryShape.INGOT.q(1)) {
            return null;
        }
        IcfPelletItem.Fuel solidFuel = fuelForMaterial(materialStack.get().material().name());
        return solidFuel == null ? null : new FuelUse(solidFuel, false);
    }

    private void consumeFuel(FuelUse use, int tankIndex, int solidSlot) {
        if (use.fluid()) {
            this.tanks[tankIndex].drain(this.configuredFuel[tankIndex], 1000, false);
            normalizeTank(tankIndex);
            return;
        }
        ItemStack stack = this.items.get(solidSlot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.items.set(solidSlot, ItemStack.EMPTY);
        }
    }

    @Nullable
    private static IcfPelletItem.Fuel fuelForFluid(HbmFluidDefinition fluid) {
        return switch (fluid.name()) {
            case "hydrogen" -> IcfPelletItem.Fuel.HYDROGEN;
            case "deuterium" -> IcfPelletItem.Fuel.DEUTERIUM;
            case "tritium" -> IcfPelletItem.Fuel.TRITIUM;
            case "helium3" -> IcfPelletItem.Fuel.HELIUM3;
            case "helium4" -> IcfPelletItem.Fuel.HELIUM4;
            case "oxygen" -> IcfPelletItem.Fuel.OXYGEN;
            case "chlorine" -> IcfPelletItem.Fuel.CHLORINE;
            default -> null;
        };
    }

    @Nullable
    private static IcfPelletItem.Fuel fuelForMaterial(String material) {
        return switch (material) {
            case "lithium" -> IcfPelletItem.Fuel.LITHIUM;
            case "beryllium" -> IcfPelletItem.Fuel.BERYLLIUM;
            case "boron" -> IcfPelletItem.Fuel.BORON;
            case "carbon", "graphite" -> IcfPelletItem.Fuel.CARBON;
            case "sodium" -> IcfPelletItem.Fuel.SODIUM;
            case "calcium" -> IcfPelletItem.Fuel.CALCIUM;
            default -> null;
        };
    }

    private void setFuelType(int index, HbmFluidDefinition fluid) {
        HbmFluidDefinition next = fluid == null ? HbmFluids.none() : fluid;
        this.configuredFuel[index] = next;
        this.tanks[index].conform(next, 0);
    }

    private void normalizeTank(int index) {
        if (this.tanks[index].amount() <= 0) {
            this.tanks[index].conform(this.configuredFuel[index], 0);
        }
    }

    private boolean canStore(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    private void store(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static boolean contains(int[] slots, int slot) {
        for (int candidate : slots) {
            if (candidate == slot) {
                return true;
            }
        }
        return false;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private record FuelUse(IcfPelletItem.Fuel fuel, boolean fluid) {
    }

    private final class IcfPressFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < tanks.length ? tanks[tank].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < tanks.length ? tanks[tank].capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank < 0 || tank >= tanks.length || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && fluid == configuredFuel[tank];
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return 0;
            }

            int remaining = resource.getAmount();
            int accepted = 0;
            for (int index = 0; index < tanks.length && remaining > 0; index++) {
                if (configuredFuel[index] != fluid) {
                    continue;
                }
                normalizeTank(index);
                int filled = tanks[index].fill(fluid, remaining, action.simulate());
                accepted += filled;
                remaining -= filled;
            }
            if (accepted > 0 && action.execute()) {
                setChangedAndSync();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
