package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.ElectrolyzerBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.foundry.CrucibleUtil;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ElectrolyzerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ElectrolyzerFluidRecipe;
import com.reinhardt.hbm.recipe.ElectrolyzerMetalRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElectrolyzerBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_END = 3;
    public static final int FLUID_IDENTIFIER_START = 3;
    public static final int FLUID_INPUT_ITEM_START = 5;
    public static final int FLUID_BYPRODUCT_START = 11;
    public static final int FLUID_BYPRODUCT_END = 14;
    public static final int METAL_INPUT_SLOT = 14;
    public static final int METAL_OUTPUT_START = 15;
    public static final int METAL_OUTPUT_END = 21;
    public static final int SLOT_COUNT = 21;
    public static final int DATA_COUNT = 20;
    public static final int GUI_FLUID = 0;
    public static final int GUI_METAL = 1;
    public static final int TANK_CAPACITY = 16_000;
    public static final long MAX_POWER = 20_000_000L;
    public static final int BASE_USAGE_ORE = 10_000;
    public static final int BASE_USAGE_FLUID = 10_000;
    public static final int MAX_MATERIAL = FoundryShape.BLOCK.q(16);

    private static final int[] AUTOMATION_SLOTS = {5, 6, 7, 8, 9, 10, FLUID_BYPRODUCT_START, FLUID_BYPRODUCT_START + 1, FLUID_BYPRODUCT_START + 2, METAL_INPUT_SLOT, 15, 16, 17, 18, 19, 20};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank inputTank = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank outputTank1 = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank outputTank2 = new HbmFluidTank(TANK_CAPACITY);
    private final HbmFluidTank acidTank = new HbmFluidTank(TANK_CAPACITY);
    private FoundryMaterialStack leftStack;
    private FoundryMaterialStack rightStack;
    private HbmFluidDefinition configuredInput = HbmFluids.byName("water").orElse(HbmFluids.none());
    private long power;
    private long lastInput;
    private int progressFluid;
    private int progressOre;
    private int processFluidTime = 100;
    private int processOreTime = 600;
    private int usageFluid = BASE_USAGE_FLUID;
    private int usageOre = BASE_USAGE_ORE;
    private boolean workingFluid;
    private boolean workingOre;
    private int selectedGui = GUI_FLUID;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ElectrolyzerBlockEntity.this.power;
                case 1 -> (int) ElectrolyzerBlockEntity.this.lastInput;
                case 2 -> ElectrolyzerBlockEntity.this.progressFluid;
                case 3 -> ElectrolyzerBlockEntity.this.processFluidTime;
                case 4 -> ElectrolyzerBlockEntity.this.progressOre;
                case 5 -> ElectrolyzerBlockEntity.this.processOreTime;
                case 6 -> ElectrolyzerBlockEntity.this.usageFluid;
                case 7 -> ElectrolyzerBlockEntity.this.usageOre;
                case 8 -> ElectrolyzerBlockEntity.this.inputTank.type().oldId();
                case 9 -> ElectrolyzerBlockEntity.this.inputTank.amount();
                case 10 -> ElectrolyzerBlockEntity.this.outputTank1.type().oldId();
                case 11 -> ElectrolyzerBlockEntity.this.outputTank1.amount();
                case 12 -> ElectrolyzerBlockEntity.this.outputTank2.type().oldId();
                case 13 -> ElectrolyzerBlockEntity.this.outputTank2.amount();
                case 14 -> ElectrolyzerBlockEntity.this.acidTank.type().oldId();
                case 15 -> ElectrolyzerBlockEntity.this.acidTank.amount();
                case 16 -> ElectrolyzerBlockEntity.this.leftStack == null ? -1 : ElectrolyzerBlockEntity.this.leftStack.material().id();
                case 17 -> ElectrolyzerBlockEntity.this.leftStack == null ? 0 : ElectrolyzerBlockEntity.this.leftStack.amount();
                case 18 -> ElectrolyzerBlockEntity.this.rightStack == null ? -1 : ElectrolyzerBlockEntity.this.rightStack.material().id();
                case 19 -> ElectrolyzerBlockEntity.this.rightStack == null ? 0 : ElectrolyzerBlockEntity.this.rightStack.amount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ElectrolyzerBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 1 -> ElectrolyzerBlockEntity.this.lastInput = Integer.toUnsignedLong(value);
                case 2 -> ElectrolyzerBlockEntity.this.progressFluid = value;
                case 3 -> ElectrolyzerBlockEntity.this.processFluidTime = Math.max(1, value);
                case 4 -> ElectrolyzerBlockEntity.this.progressOre = value;
                case 5 -> ElectrolyzerBlockEntity.this.processOreTime = Math.max(1, value);
                case 6 -> ElectrolyzerBlockEntity.this.usageFluid = Math.max(1, value);
                case 7 -> ElectrolyzerBlockEntity.this.usageOre = Math.max(1, value);
                case 8 -> ElectrolyzerBlockEntity.this.inputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 9 -> ElectrolyzerBlockEntity.this.inputTank.setAmount(value);
                case 10 -> ElectrolyzerBlockEntity.this.outputTank1.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 11 -> ElectrolyzerBlockEntity.this.outputTank1.setAmount(value);
                case 12 -> ElectrolyzerBlockEntity.this.outputTank2.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 13 -> ElectrolyzerBlockEntity.this.outputTank2.setAmount(value);
                case 14 -> ElectrolyzerBlockEntity.this.acidTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 15 -> ElectrolyzerBlockEntity.this.acidTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ELECTROLYZER.get(), pos, blockState);
        this.inputTank.setType(this.configuredInput);
        this.acidTank.setType(HbmFluids.byName("nitric_acid").orElse(HbmFluids.none()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectrolyzerBlockEntity electrolyzer) {
        if (level.isClientSide) {
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, electrolyzer);
        electrolyzer.tickServer(level, state);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank1() {
        return this.outputTank1;
    }

    public HbmFluidTank outputTank2() {
        return this.outputTank2;
    }

    public HbmFluidTank acidTank() {
        return this.acidTank;
    }

    public int selectedGui() {
        return this.selectedGui;
    }

    public void setSelectedGui(int selectedGui) {
        this.selectedGui = selectedGui == GUI_METAL ? GUI_METAL : GUI_FLUID;
        setChangedAndSync(true);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return portsFor(this.worldPosition, getBlockState()).stream().map(Port::connectorPos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : portsFor(this.worldPosition, getBlockState())) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return 0;
    }

    @Override
    public long getRequestedInput() {
        if (this.power >= MAX_POWER) {
            return 0;
        }
        long requested = Math.max(this.usageFluid, this.usageOre);
        return Math.min(requested, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        int fluid = this.processFluidTime <= 0 ? 0 : this.progressFluid * 100 / this.processFluidTime;
        int ore = this.processOreTime <= 0 ? 0 : this.progressOre * 100 / this.processOreTime;
        return Component.translatable("message.reinhardtshbm.power.electrolyzer", this.lastInput, this.power, MAX_POWER, fluid, ore);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new ElectrolyzerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public static List<Port> portsFor(BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(ElectrolyzerBlock.FACING) ? state.getValue(ElectrolyzerBlock.FACING) : Direction.NORTH;
        Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
        return List.of(
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, -6, rot, 0, 0)).immutable(), facing.getOpposite()),
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, -6, rot, 1, 0)).immutable(), facing.getOpposite()),
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, -6, rot, -1, 0)).immutable(), facing.getOpposite()),
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, 6, rot, 0, 0)).immutable(), facing),
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, 6, rot, 1, 0)).immutable(), facing),
                Port.fromConnector(pos.offset(LegacyMachineGeometry.legacyOffset(facing, 6, rot, -1, 0)).immutable(), facing)
        );
    }

    @Override
    public int[] getFluidIdsToCopy() {
        HbmFluidDefinition copied = this.inputTank.amount() > 0 ? this.inputTank.type() : this.configuredInput;
        return copied.isNone() ? new int[0] : new int[]{copied.oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (fluid != null && acceptsInputFluid(fluid)) {
            this.configuredInput = fluid;
            if (this.inputTank.amount() == 0 || this.inputTank.type() == fluid) {
                this.inputTank.setType(fluid);
            }
            setChangedAndSync(true);
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
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
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
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return isSupportedUpgrade(stack);
        }
        if (slot >= FLUID_IDENTIFIER_START && slot < FLUID_INPUT_ITEM_START) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot == 5 || slot == 7 || slot == 9) {
            return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
        }
        if (slot == METAL_INPUT_SLOT) {
            return isMetalInputCandidate(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == METAL_INPUT_SLOT || slot == 5 || slot == 7 || slot == 9) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 6 || slot == 8 || slot == 10 || slot != METAL_INPUT_SLOT && slot >= FLUID_BYPRODUCT_START;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_electrolyser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectrolyzerMenu(containerId, playerInventory, this, this.menuData, this.selectedGui);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
        drop(level, pos, this.leftStack);
        drop(level, pos, this.rightStack);
        this.leftStack = null;
        this.rightStack = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("InputTank", this.inputTank.save());
        tag.put("OutputTank1", this.outputTank1.save());
        tag.put("OutputTank2", this.outputTank2.save());
        tag.put("AcidTank", this.acidTank.save());
        tag.putString("ConfiguredInput", this.configuredInput.name());
        if (this.leftStack != null) tag.put("LeftStack", this.leftStack.save());
        if (this.rightStack != null) tag.put("RightStack", this.rightStack.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("ProgressFluid", this.progressFluid);
        tag.putInt("ProgressOre", this.progressOre);
        tag.putInt("SelectedGui", this.selectedGui);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.inputTank.load(tag.getCompound("InputTank"));
        this.outputTank1.load(tag.getCompound("OutputTank1"));
        this.outputTank2.load(tag.getCompound("OutputTank2"));
        this.acidTank.load(tag.getCompound("AcidTank"));
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput")).orElse(HbmFluids.byName("water").orElse(HbmFluids.none()));
        this.leftStack = tag.contains("LeftStack") ? FoundryMaterialStack.load(tag.getCompound("LeftStack")) : null;
        this.rightStack = tag.contains("RightStack") ? FoundryMaterialStack.load(tag.getCompound("RightStack")) : null;
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = tag.getLong("LastInput");
        this.progressFluid = tag.getInt("ProgressFluid");
        this.progressOre = tag.getInt("ProgressOre");
        this.selectedGui = tag.getInt("SelectedGui") == GUI_METAL ? GUI_METAL : GUI_FLUID;
        updateUpgrades();
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

    private void tickServer(Level level, BlockState state) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyIdentifierSlots();
        transferFluidContainers();
        updateUpgrades();
        boolean wasWorking = this.workingFluid || this.workingOre;
        this.workingFluid = false;
        this.workingOre = false;

        for (int i = 0; i < cycleCount(); i++) {
            processFluidCycle();
            processMetalCycle();
        }
        pourMoltenOutputs(level, state);
        if (wasWorking != (this.workingFluid || this.workingOre)) {
            setChangedAndSync(true);
        }
    }

    private void processFluidCycle() {
        Optional<RecipeHolder<ElectrolyzerFluidRecipe>> holder = fluidRecipe();
        if (holder.isEmpty() || !canProcessFluid(holder.get().value())) {
            this.progressFluid = 0;
            return;
        }
        if (this.power < this.usageFluid) {
            return;
        }
        ElectrolyzerFluidRecipe recipe = holder.get().value();
        this.processFluidTime = durationFluid(recipe);
        this.power -= this.usageFluid;
        this.progressFluid++;
        this.workingFluid = true;
        if (this.progressFluid >= this.processFluidTime) {
            finishFluid(recipe);
            this.progressFluid = 0;
            setChangedAndSync(true);
        } else {
            setChanged();
        }
    }

    private void processMetalCycle() {
        Optional<RecipeHolder<ElectrolyzerMetalRecipe>> holder = metalRecipe(this.items.get(METAL_INPUT_SLOT));
        if (holder.isEmpty() || !canProcessMetal(holder.get().value())) {
            this.progressOre = 0;
            return;
        }
        if (this.power < this.usageOre) {
            return;
        }
        ElectrolyzerMetalRecipe recipe = holder.get().value();
        this.processOreTime = durationMetal(recipe);
        this.power -= this.usageOre;
        this.progressOre++;
        this.workingOre = true;
        if (this.progressOre >= this.processOreTime) {
            finishMetal(recipe);
            this.progressOre = 0;
            setChangedAndSync(true);
        } else {
            setChanged();
        }
    }

    private void applyIdentifierSlots() {
        for (int slot = FLUID_IDENTIFIER_START; slot < FLUID_INPUT_ITEM_START; slot++) {
            ItemStack identifier = this.items.get(slot);
            if (identifier.getItem() instanceof FluidIdentifierItem) {
                HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
                if (acceptsInputFluid(fluid)) {
                    this.configuredInput = fluid;
                    if (this.inputTank.amount() == 0 || this.inputTank.type() == fluid) {
                        this.inputTank.setType(fluid);
                    }
                }
            }
        }
    }

    private void transferFluidContainers() {
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(5),
                this.inputTank,
                this::acceptsInputFluid,
                stack -> canInsertItem(6, stack),
                stack -> insertItem(6, stack)
        );
        changed |= HbmFluidContainerTransfer.fillFromTank(
                this.items.get(7),
                this.outputTank1,
                stack -> canInsertItem(8, stack),
                stack -> insertItem(8, stack)
        );
        changed |= HbmFluidContainerTransfer.fillFromTank(
                this.items.get(9),
                this.outputTank2,
                stack -> canInsertItem(10, stack),
                stack -> insertItem(10, stack)
        );
        if (changed) {
            setChangedAndSync(true);
        }
    }

    private void updateUpgrades() {
        int speed = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
        int power = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3);
        this.usageOre = Math.max(1, BASE_USAGE_ORE - BASE_USAGE_ORE * power / 4 + BASE_USAGE_ORE * speed);
        this.usageFluid = Math.max(1, BASE_USAGE_FLUID - BASE_USAGE_FLUID * power / 4 + BASE_USAGE_FLUID * speed);
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private int cycleCount() {
        int speed = upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
        return Math.min(1 + speed * 2, 7);
    }

    private Optional<RecipeHolder<ElectrolyzerFluidRecipe>> fluidRecipe() {
        if (this.level == null || this.inputTank.type().isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_FLUID.get()).stream()
                .filter(holder -> holder.value().input().fluid() == this.inputTank.type())
                .findFirst();
    }

    private Optional<RecipeHolder<ElectrolyzerMetalRecipe>> metalRecipe(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_METAL.get()).stream()
                .filter(holder -> holder.value().matches(input, this.level))
                .findFirst();
    }

    public static boolean isMetalInputCandidate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(HbmItems.BEDROCK_ORE_NEW.get())
                || HbmItems.BEDROCK_ORE_FRAGMENTS.stream().anyMatch(item -> stack.is(item.get()))) {
            return true;
        }
        return HbmItems.MINERAL_CRYSTALS.stream().anyMatch(item -> stack.is(item.get()));
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        return this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_FLUID.get()).stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
    }

    private boolean canProcessFluid(ElectrolyzerFluidRecipe recipe) {
        if (this.inputTank.type() != recipe.input().fluid() || this.inputTank.amount() < recipe.input().amount()) {
            return false;
        }
        if (!canFitFluid(this.outputTank1, recipe.output1()) || !canFitFluid(this.outputTank2, recipe.output2())) {
            return false;
        }
        return canFitItems(FLUID_BYPRODUCT_START, FLUID_BYPRODUCT_END, recipe.byproducts());
    }

    private void finishFluid(ElectrolyzerFluidRecipe recipe) {
        this.inputTank.drain(recipe.input().fluid(), recipe.input().amount(), false);
        fillIfPresent(this.outputTank1, recipe.output1());
        fillIfPresent(this.outputTank2, recipe.output2());
        addOutputs(FLUID_BYPRODUCT_START, FLUID_BYPRODUCT_END, recipe.byproducts());
        if (this.inputTank.amount() == 0 && !this.configuredInput.isNone()) {
            this.inputTank.setType(this.configuredInput);
        }
    }

    private boolean canProcessMetal(ElectrolyzerMetalRecipe recipe) {
        HbmFluidDefinition nitric = HbmFluids.byName("nitric_acid").orElse(HbmFluids.none());
        if (this.items.get(METAL_INPUT_SLOT).isEmpty() || this.acidTank.type() != nitric || this.acidTank.amount() < 100) {
            return false;
        }
        if (!canFitMaterial(this.leftStack, recipe.output1Stack()) || !canFitMaterial(this.rightStack, recipe.output2Stack())) {
            return false;
        }
        return canFitItems(METAL_OUTPUT_START, METAL_OUTPUT_END, recipe.byproducts());
    }

    private void finishMetal(ElectrolyzerMetalRecipe recipe) {
        this.leftStack = mergeMaterial(this.leftStack, recipe.output1Stack());
        this.rightStack = mergeMaterial(this.rightStack, recipe.output2Stack());
        addOutputs(METAL_OUTPUT_START, METAL_OUTPUT_END, recipe.byproducts());
        this.acidTank.drain(HbmFluids.byName("nitric_acid").orElse(HbmFluids.none()), 100, false);
        this.items.get(METAL_INPUT_SLOT).shrink(1);
        if (this.items.get(METAL_INPUT_SLOT).isEmpty()) {
            this.items.set(METAL_INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void pourMoltenOutputs(Level level, BlockState state) {
        Direction facing = state.hasProperty(ElectrolyzerBlock.FACING) ? state.getValue(ElectrolyzerBlock.FACING) : Direction.NORTH;
        this.leftStack = pourStack(level, this.leftStack, facing.getOpposite());
        this.rightStack = pourStack(level, this.rightStack, facing);
    }

    @Nullable
    private FoundryMaterialStack pourStack(Level level, @Nullable FoundryMaterialStack stack, Direction direction) {
        if (stack == null || stack.amount() <= 0) {
            return null;
        }
        ArrayList<FoundryMaterialStack> stacks = new ArrayList<>();
        stacks.add(stack);
        CrucibleUtil.pourFullStack(
                level,
                this.worldPosition.getX() + 0.5D + direction.getStepX() * 5.875D,
                this.worldPosition.getY() + 2.0D,
                this.worldPosition.getZ() + 0.5D + direction.getStepZ() * 5.875D,
                6.0D,
                true,
                stacks,
                FoundryShape.NUGGET.q(3) * Math.max(cycleCount(), 1),
                material -> true
        );
        return stacks.isEmpty() ? null : stacks.get(0);
    }

    private int durationFluid(ElectrolyzerFluidRecipe recipe) {
        int speed = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED) - Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 1);
        return (int) Math.ceil(recipe.duration() * Math.max(1F - 0.25F * speed, 0.2F));
    }

    private int durationMetal(ElectrolyzerMetalRecipe recipe) {
        int speed = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED) - Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 1);
        return (int) Math.ceil(recipe.duration() * Math.max(1F - 0.25F * speed, 0.2F));
    }

    private static boolean canFitFluid(HbmFluidTank tank, ElectrolyzerFluidRecipe.FluidAmount fluid) {
        return fluid.isEmpty() || ((tank.amount() == 0 || tank.type() == fluid.fluid()) && tank.amount() + fluid.amount() <= tank.capacity());
    }

    private static void fillIfPresent(HbmFluidTank tank, ElectrolyzerFluidRecipe.FluidAmount fluid) {
        if (!fluid.isEmpty()) {
            tank.fill(fluid.fluid(), fluid.amount(), false);
        }
    }

    private static boolean canFitMaterial(@Nullable FoundryMaterialStack current, Optional<FoundryMaterialStack> output) {
        if (output.isEmpty()) {
            return true;
        }
        FoundryMaterialStack next = output.get();
        return current == null || (current.material() == next.material() && current.amount() + next.amount() <= MAX_MATERIAL);
    }

    @Nullable
    private static FoundryMaterialStack mergeMaterial(@Nullable FoundryMaterialStack current, Optional<FoundryMaterialStack> output) {
        if (output.isEmpty()) {
            return current;
        }
        FoundryMaterialStack next = output.get();
        if (current == null) {
            return next.copy();
        }
        return new FoundryMaterialStack(current.material(), current.amount() + next.amount());
    }

    private boolean canFitItems(int start, int end, List<ItemStack> outputs) {
        for (int index = 0; index < outputs.size(); index++) {
            if (start + index >= end) {
                return false;
            }
            ItemStack output = outputs.get(index);
            ItemStack slot = this.items.get(start + index);
            if (!slot.isEmpty() && (!ItemStack.isSameItemSameComponents(slot, output) || slot.getCount() + output.getCount() > slot.getMaxStackSize())) {
                return false;
            }
        }
        return true;
    }

    private boolean canInsertItem(int slot, ItemStack stack) {
        ItemStack current = this.items.get(slot);
        return stack.isEmpty()
                || current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void insertItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
        setChangedAndSync(true);
    }

    private void addOutputs(int start, int end, List<ItemStack> outputs) {
        for (int index = 0; index < outputs.size() && start + index < end; index++) {
            ItemStack output = outputs.get(index).copy();
            ItemStack slot = this.items.get(start + index);
            if (slot.isEmpty()) {
                this.items.set(start + index, output);
            } else {
                slot.grow(output.getCount());
            }
        }
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(this.worldPosition, getBlockState())) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            for (Port port : portsFor(this.worldPosition, getBlockState())) {
                this.level.invalidateCapabilities(port.pos());
                this.level.invalidateCapabilities(port.connectorPos());
            }
            if (sync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isSupportedUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED || type == MachineUpgradeItem.UpgradeType.POWER || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    private static void drop(Level level, BlockPos pos, @Nullable FoundryMaterialStack stack) {
        if (stack != null && stack.amount() > 0) {
            drop(level, pos, com.reinhardt.hbm.item.ScrapsItem.create(stack, true));
        }
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class ElectrolyzerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private ElectrolyzerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 4;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tankFor(tank).getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tankFor(tank).capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return tank == 0 ? acceptsInputFluid(fluid) : tank == 3 && fluid == HbmFluids.byName("nitric_acid").orElse(HbmFluids.none());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank tank = fluid == HbmFluids.byName("nitric_acid").orElse(HbmFluids.none()) ? acidTank : inputTank;
            if (tank == inputTank && !acceptsInputFluid(fluid)) {
                return 0;
            }
            int filled = tank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                if (tank == inputTank) configuredInput = fluid;
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidStack drained = outputTank1.drain(fluid, resource.getAmount(), action.simulate());
            if (drained.isEmpty()) {
                drained = outputTank2.drain(fluid, resource.getAmount(), action.simulate());
            }
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = outputTank1.drain(null, maxDrain, action.simulate());
            if (drained.isEmpty()) {
                drained = outputTank2.drain(null, maxDrain, action.simulate());
            }
            if (!drained.isEmpty() && action.execute()) {
                setChangedAndSync(true);
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        private HbmFluidTank tankFor(int index) {
            return switch (index) {
                case 1 -> outputTank1;
                case 2 -> outputTank2;
                case 3 -> acidTank;
                default -> inputTank;
            };
        }
    }
}
