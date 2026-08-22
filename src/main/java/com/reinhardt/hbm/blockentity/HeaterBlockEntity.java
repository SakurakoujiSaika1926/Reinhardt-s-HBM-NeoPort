package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.HeaterMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class HeaterBlockEntity extends BlockEntity implements HeatSourceBlockEntity, PowerEndpoint, WorldlyContainer, MenuProvider, MachineInventory {
    public static final int OILBURNER_INPUT_SLOT = 0;
    public static final int OILBURNER_OUTPUT_SLOT = 1;
    public static final int OILBURNER_IDENTIFIER_SLOT = 2;
    public static final int OILBURNER_SLOT_COUNT = 3;
    public static final int HEATEX_IDENTIFIER_SLOT = 0;
    public static final int HEATEX_SLOT_COUNT = 1;
    public static final int MAX_HEAT = 100_000;
    public static final int OVEN_MAX_HEAT = 500_000;
    public static final int OILBURNER_TANK_CAPACITY = 16_000;
    public static final int HEATEX_TANK_CAPACITY = 24_000;
    public static final int DATA_COUNT = 21;
    private static final double OVEN_HEAT_PULL_EFFICIENCY = 0.5D;
    private static final double ELECTRIC_HEAT_PULL_EFFICIENCY = 0.85D;
    private static final int SMOKE_BUFFER_CAPACITY = 50;
    private static final int[] FUEL_SLOTS = {0, 1};
    private static final int[] OILBURNER_SLOTS = {0, 1, 2};
    private static final int[] NO_SLOTS = {};

    private Kind kind;
    private ItemStack fuelA = ItemStack.EMPTY;
    private ItemStack fuelB = ItemStack.EMPTY;
    private ItemStack fluidIdentifier = ItemStack.EMPTY;
    private final HbmFluidTank oilTank = new HbmFluidTank(defaultOilburnerFluid(), OILBURNER_TANK_CAPACITY);
    private final HbmFluidTank smokeTank = new HbmFluidTank(smokeFluid(), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokeLeadedTank = new HbmFluidTank(smokeLeadedFluid(), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank smokePoisonTank = new HbmFluidTank(smokePoisonFluid(), SMOKE_BUFFER_CAPACITY);
    private final HbmFluidTank heatExInputTank = new HbmFluidTank(defaultHeatExchangerFluid(), HEATEX_TANK_CAPACITY);
    private final HbmFluidTank heatExOutputTank = new HbmFluidTank(defaultHeatExchangerOutput(), HEATEX_TANK_CAPACITY);
    private HbmFluidDefinition oilburnerFluid = defaultOilburnerFluid();
    private HbmFluidDefinition heatExchangerFluid = defaultHeatExchangerFluid();
    private int heatEnergy;
    private int setting = 1;
    private boolean enabled;
    private long power;
    private long lastInput;
    private int animationTicks;
    private int burnTime;
    private int maxBurnTime;
    private int burnHeat;
    private int heatExchangerAmountToCool = HEATEX_TANK_CAPACITY;
    private int heatExchangerTickDelay = 1;
    private int playersUsing;
    private float doorAngle;
    private float prevDoorAngle;
    private boolean clientActive;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> HeaterBlockEntity.this.kind.ordinal();
                case 1 -> HeaterBlockEntity.this.heatEnergy;
                case 2 -> HeaterBlockEntity.this.setting;
                case 3 -> HeaterBlockEntity.this.enabled ? 1 : 0;
                case 4 -> (int) HeaterBlockEntity.this.power;
                case 5 -> (int) HeaterBlockEntity.this.lastInput;
                case 6 -> HeaterBlockEntity.this.burnTime;
                case 7 -> HeaterBlockEntity.this.maxBurnTime;
                case 8 -> HeaterBlockEntity.this.burnHeat;
                case 9 -> HeaterBlockEntity.this.playersUsing;
                case 10 -> HeaterBlockEntity.this.oilburnerFluid.oldId();
                case 11 -> HeaterBlockEntity.this.oilTank.amount();
                case 12 -> HeaterBlockEntity.this.oilTank.capacity();
                case 13 -> HeaterBlockEntity.this.heatExchangerFluid.oldId();
                case 14 -> HeaterBlockEntity.this.heatExInputTank.amount();
                case 15 -> HeaterBlockEntity.this.heatExInputTank.capacity();
                case 16 -> HeaterBlockEntity.this.heatExOutputTank.type().oldId();
                case 17 -> HeaterBlockEntity.this.heatExOutputTank.amount();
                case 18 -> HeaterBlockEntity.this.heatExOutputTank.capacity();
                case 19 -> HeaterBlockEntity.this.heatExchangerAmountToCool;
                case 20 -> HeaterBlockEntity.this.heatExchangerTickDelay;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> HeaterBlockEntity.this.kind = Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, value))];
                case 1 -> HeaterBlockEntity.this.heatEnergy = value;
                case 2 -> HeaterBlockEntity.this.setting = value;
                case 3 -> HeaterBlockEntity.this.enabled = value != 0;
                case 4 -> HeaterBlockEntity.this.power = value;
                case 5 -> HeaterBlockEntity.this.lastInput = value;
                case 6 -> HeaterBlockEntity.this.burnTime = value;
                case 7 -> HeaterBlockEntity.this.maxBurnTime = value;
                case 8 -> HeaterBlockEntity.this.burnHeat = value;
                case 9 -> HeaterBlockEntity.this.playersUsing = value;
                case 10 -> HeaterBlockEntity.this.setConfiguredOilburnerFluid(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 11 -> HeaterBlockEntity.this.oilTank.setAmount(value);
                case 13 -> HeaterBlockEntity.this.setConfiguredHeatExchangerFluid(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 14 -> HeaterBlockEntity.this.heatExInputTank.setAmount(value);
                case 16 -> HeaterBlockEntity.this.heatExOutputTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 17 -> HeaterBlockEntity.this.heatExOutputTank.setAmount(value);
                case 19 -> HeaterBlockEntity.this.heatExchangerAmountToCool = Math.max(1, Math.min(HEATEX_TANK_CAPACITY, value));
                case 20 -> HeaterBlockEntity.this.heatExchangerTickDelay = Math.max(1, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, Kind.FIREBOX);
    }

    public HeaterBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(HbmBlockEntities.HEATER.get(), pos, state);
        this.kind = kind;
        this.enabled = kind == Kind.FIREBOX || kind == Kind.OVEN || kind == Kind.HEATEX;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HeaterBlockEntity heater) {
        if (level.isClientSide) {
            heater.tickClientAnimation(state);
            if (heater.isActive()) {
                heater.animationTicks++;
            }
            return;
        }

        if (heater.kind == Kind.FIREBOX || heater.kind == Kind.OVEN) {
            heater.sendSmoke();
        }
        switch (heater.kind) {
            case ELECTRIC -> {
                heater.decayWithLegacyMultiplier();
                PowerNetworkManager.tickFromEndpoint(level, heater);
                heater.pullHeatFromBelow(level, ELECTRIC_HEAT_PULL_EFFICIENCY, false);
                long cost = heater.getConsumption();
                heater.enabled = heater.setting > 0 && heater.power >= cost;
                if (heater.enabled) {
                    heater.power -= cost;
                    heater.addHeat(heater.setting * 100);
                }
            }
            case OILBURNER -> {
                heater.sendOilburnerSmoke();
                heater.tickOilburner();
            }
            case HEATEX -> {
                heater.tickHeatExchanger(level);
            }
            case FIREBOX, OVEN -> {
                if (heater.kind == Kind.OVEN) {
                    heater.pullHeatFromBelow(level, OVEN_HEAT_PULL_EFFICIENCY, true);
                }
                heater.tickSolidFuel();
            }
        }
        heater.setChanged();
        if (level.getGameTime() % 10L == 0L) {
            heater.syncToClient();
        }
    }

    public void toggleOrCycle(boolean reverse) {
        cycleSetting(reverse);
    }

    public void cycleSetting(boolean reverse) {
        if (this.kind == Kind.OILBURNER || this.kind == Kind.ELECTRIC) {
            this.setting += reverse ? -1 : 1;
            int max = this.kind == Kind.ELECTRIC ? 10 : 100;
            if (this.setting > max) {
                this.setting = this.kind == Kind.ELECTRIC ? 0 : 1;
            }
            if (this.setting < (this.kind == Kind.ELECTRIC ? 0 : 1)) {
                this.setting = max;
            }
            if (this.kind == Kind.ELECTRIC) {
                this.enabled = this.setting > 0;
            }
        } else {
            this.enabled = !this.enabled;
        }
        setChanged();
        syncToClient();
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
        setChanged();
        syncToClient();
    }

    public Component status() {
        return Component.translatable(
                "message.reinhardtshbm.heater.status",
                this.kind.id,
                this.heatEnergy,
                this.setting,
                this.enabled ? "on" : "off"
        );
    }

    public boolean isActive() {
        return this.enabled && this.heatEnergy > 0;
    }

    public boolean isVisuallyActive() {
        return this.clientActive || this.burnTime > 0 || isActive();
    }

    public int getAnimationTicks() {
        return this.animationTicks;
    }

    public Kind kind() {
        return this.kind;
    }

    public int setting() {
        return this.setting;
    }

    public int burnHeat() {
        return this.burnHeat;
    }

    public HbmFluidTank oilTank() {
        return this.oilTank;
    }

    public HbmFluidTank heatExchangerInputTank() {
        return this.heatExInputTank;
    }

    public HbmFluidTank heatExchangerOutputTank() {
        return this.heatExOutputTank;
    }

    public HbmFluidDefinition heatExchangerFluid() {
        return this.heatExchangerFluid;
    }

    public int heatExchangerAmountToCool() {
        return this.heatExchangerAmountToCool;
    }

    public int heatExchangerTickDelay() {
        return this.heatExchangerTickDelay;
    }

    public void setHeatExchangerSettings(int amountToCool, int tickDelay) {
        this.heatExchangerAmountToCool = Math.max(1, Math.min(HEATEX_TANK_CAPACITY, amountToCool));
        this.heatExchangerTickDelay = Math.max(1, tickDelay);
        setChanged();
        syncToClient();
    }

    public long consumption() {
        return getConsumption();
    }

    public int heatGeneration() {
        if (this.kind == Kind.OILBURNER) {
            return this.setting * flammableHeatPerMillibucket(this.oilburnerFluid);
        }
        return this.setting * 100;
    }

    public int maxHeat() {
        return maxHeat(this.kind);
    }

    public static int maxHeatForKind(int kindOrdinal) {
        Kind[] kinds = Kind.values();
        int clamped = Math.max(0, Math.min(kinds.length - 1, kindOrdinal));
        return maxHeat(kinds[clamped]);
    }

    public boolean hasMenu() {
        return this.kind != Kind.ELECTRIC;
    }

    public boolean acceptsHopperFuelInput() {
        return this.kind == Kind.FIREBOX || this.kind == Kind.OVEN;
    }

    public float doorAngle(float partialTick) {
        return this.prevDoorAngle + (this.doorAngle - this.prevDoorAngle) * partialTick;
    }

    @Override
    public int getHeatStored() {
        return this.heatEnergy;
    }

    @Override
    public void useHeat(int amount) {
        this.heatEnergy = Math.max(0, this.heatEnergy - amount);
        setChanged();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        if (this.kind != Kind.ELECTRIC) {
            return List.of();
        }
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        return List.of(this.worldPosition.relative(facing, 3).immutable());
    }

    @Override
    public long getAvailableOutput() {
        return 0;
    }

    @Override
    public long getRequestedInput() {
        return this.kind == Kind.ELECTRIC ? Math.max(0L, getMaxPower() - this.power) : 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(getMaxPower(), this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal("Heater input " + this.lastInput + " HE/t, stored " + this.power + " HE");
    }

    @Override
    public int getContainerSize() {
        return switch (this.kind) {
            case OILBURNER -> OILBURNER_SLOT_COUNT;
            case HEATEX -> HEATEX_SLOT_COUNT;
            default -> 2;
        };
    }

    @Override
    public boolean isEmpty() {
        if (this.kind == Kind.HEATEX) {
            return this.fluidIdentifier.isEmpty();
        }
        return this.fuelA.isEmpty() && this.fuelB.isEmpty() && (this.kind != Kind.OILBURNER || this.fluidIdentifier.isEmpty());
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0 -> this.kind == Kind.HEATEX ? this.fluidIdentifier : this.fuelA;
            case 1 -> this.fuelB;
            case 2 -> this.kind == Kind.OILBURNER ? this.fluidIdentifier : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = getItem(slot);
        if (slot == 0 && this.kind == Kind.HEATEX) {
            this.fluidIdentifier = ItemStack.EMPTY;
        } else if (slot == 0) {
            this.fuelA = ItemStack.EMPTY;
        } else if (slot == 1) {
            this.fuelB = ItemStack.EMPTY;
        } else if (slot == 2 && this.kind == Kind.OILBURNER) {
            this.fluidIdentifier = ItemStack.EMPTY;
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && this.kind == Kind.HEATEX) {
            this.fluidIdentifier = stack;
            if (applyHeatExchangerIdentifier()) {
                syncToClient();
            }
        } else if (slot == 0) {
            this.fuelA = stack;
        } else if (slot == 1) {
            this.fuelB = stack;
        } else if (slot == 2 && this.kind == Kind.OILBURNER) {
            this.fluidIdentifier = stack;
        }
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (this.kind == Kind.HEATEX) {
            return slot == HEATEX_IDENTIFIER_SLOT
                    && stack.getItem() instanceof FluidIdentifierItem
                    && HbmThermalConversions.heatExchangerCoolingStep(FluidIdentifierItem.primary(stack)).isPresent();
        }
        if (this.kind == Kind.OILBURNER) {
            return switch (slot) {
                case OILBURNER_INPUT_SLOT -> HbmFluidContainerTransfer.canDrainIntoTank(
                        stack,
                        this.oilTank,
                        this::acceptsConfiguredOilburnerFluid,
                        output -> canPlaceOutput(OILBURNER_OUTPUT_SLOT, output)
                );
                case OILBURNER_IDENTIFIER_SLOT -> stack.getItem() instanceof FluidIdentifierItem
                        && isOilburnerFluid(FluidIdentifierItem.primary(stack));
                default -> false;
            };
        }
        return acceptsHopperFuelInput() && slot >= 0 && slot < getContainerSize() && fuelDuration(stack) > 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (this.kind == Kind.HEATEX) {
            return new int[]{HEATEX_IDENTIFIER_SLOT};
        }
        if (this.kind == Kind.OILBURNER) {
            return OILBURNER_SLOTS;
        }
        return acceptsHopperFuelInput() ? FUEL_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (this.kind == Kind.OILBURNER && slot == OILBURNER_OUTPUT_SLOT)
                || (this.kind == Kind.HEATEX && slot == HEATEX_IDENTIFIER_SLOT);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.fuelA = ItemStack.EMPTY;
        this.fuelB = ItemStack.EMPTY;
        this.fluidIdentifier = ItemStack.EMPTY;
        if (this.kind == Kind.HEATEX) {
            setConfiguredHeatExchangerFluid(HbmFluids.none());
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.heater." + this.kind.id);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!hasMenu()) {
            return null;
        }
        this.playersUsing++;
        setChanged();
        syncToClient();
        return new HeaterMenu(containerId, playerInventory, this, this.menuData);
    }

    public void menuClosed() {
        this.playersUsing = Math.max(0, this.playersUsing - 1);
        setChanged();
        syncToClient();
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (this.kind != Kind.HEATEX) {
            drop(level, pos, this.fuelA);
            drop(level, pos, this.fuelB);
        }
        drop(level, pos, this.fluidIdentifier);
        this.fuelA = ItemStack.EMPTY;
        this.fuelB = ItemStack.EMPTY;
        this.fluidIdentifier = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Kind", this.kind.id);
        tag.put("FuelA", this.fuelA.saveOptional(registries));
        tag.put("FuelB", this.fuelB.saveOptional(registries));
        tag.put("FluidIdentifier", this.fluidIdentifier.saveOptional(registries));
        tag.putString("OilburnerFluid", this.oilburnerFluid.name());
        tag.put("OilTank", this.oilTank.save());
        tag.putString("HeatExchangerFluid", this.heatExchangerFluid.name());
        tag.put("HeatExchangerInput", this.heatExInputTank.save());
        tag.put("HeatExchangerOutput", this.heatExOutputTank.save());
        tag.putInt("HeatExchangerAmountToCool", this.heatExchangerAmountToCool);
        tag.putInt("HeatExchangerTickDelay", this.heatExchangerTickDelay);
        tag.put("Smoke", this.smokeTank.save());
        tag.put("SmokeLeaded", this.smokeLeadedTank.save());
        tag.put("SmokePoison", this.smokePoisonTank.save());
        tag.putInt("HeatEnergy", this.heatEnergy);
        tag.putInt("Setting", this.setting);
        tag.putBoolean("Enabled", this.enabled);
        tag.putLong("Power", this.power);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("MaxBurnTime", this.maxBurnTime);
        tag.putInt("BurnHeat", this.burnHeat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.kind = Kind.byId(tag.getString("Kind"));
        this.fuelA = ItemStack.parseOptional(registries, tag.getCompound("FuelA"));
        this.fuelB = ItemStack.parseOptional(registries, tag.getCompound("FuelB"));
        this.fluidIdentifier = ItemStack.parseOptional(registries, tag.getCompound("FluidIdentifier"));
        HbmFluidDefinition savedOilburnerFluid = HbmFluids.byName(tag.getString("OilburnerFluid"))
                .filter(HeaterBlockEntity::isOilburnerFluid)
                .orElse(HbmFluids.none());
        HbmFluidDefinition savedTankFluid = HbmFluids.none();
        if (tag.contains("OilTank")) {
            CompoundTag oilTankTag = tag.getCompound("OilTank");
            savedTankFluid = HbmFluids.byName(oilTankTag.getString("type")).orElse(HbmFluids.none());
            this.oilTank.load(oilTankTag);
        }
        if (savedOilburnerFluid.isNone() && isOilburnerFluid(savedTankFluid)) {
            savedOilburnerFluid = savedTankFluid;
        }
        this.oilburnerFluid = savedOilburnerFluid.isNone() ? defaultOilburnerFluid() : savedOilburnerFluid;
        if (this.oilTank.amount() > 0) {
            if (isOilburnerFluid(this.oilTank.type())) {
                this.oilburnerFluid = this.oilTank.type();
            } else {
                this.oilTank.clear();
            }
        }
        if (this.oilTank.amount() == 0) {
            this.oilTank.setType(this.oilburnerFluid);
        }
        this.heatExchangerFluid = HbmFluids.byName(tag.getString("HeatExchangerFluid"))
                .filter(fluid -> HbmThermalConversions.heatExchangerCoolingStep(fluid).isPresent())
                .orElse(defaultHeatExchangerFluid());
        this.heatExInputTank.load(tag.getCompound("HeatExchangerInput"));
        this.heatExOutputTank.load(tag.getCompound("HeatExchangerOutput"));
        if (this.heatExInputTank.amount() > 0
                && HbmThermalConversions.heatExchangerCoolingStep(this.heatExInputTank.type()).isPresent()) {
            this.heatExchangerFluid = this.heatExInputTank.type();
        } else if (this.heatExInputTank.amount() > 0) {
            this.heatExInputTank.clear();
        }
        setupHeatExchangerTanks();
        this.heatExchangerAmountToCool = Math.max(1, Math.min(HEATEX_TANK_CAPACITY, tag.getInt("HeatExchangerAmountToCool")));
        this.heatExchangerTickDelay = Math.max(1, tag.getInt("HeatExchangerTickDelay"));
        this.smokeTank.load(tag.getCompound("Smoke"));
        this.smokeLeadedTank.load(tag.getCompound("SmokeLeaded"));
        this.smokePoisonTank.load(tag.getCompound("SmokePoison"));
        this.heatEnergy = tag.getInt("HeatEnergy");
        this.setting = Math.max(0, tag.getInt("Setting"));
        this.enabled = tag.getBoolean("Enabled");
        this.power = tag.getLong("Power");
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.burnHeat = tag.getInt("BurnHeat");
        this.playersUsing = tag.getInt("PlayersUsing");
        this.clientActive = tag.getBoolean("ClientActive");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putInt("PlayersUsing", this.playersUsing);
        tag.putBoolean("ClientActive", isVisuallyActive());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (this.kind == Kind.OILBURNER) {
            return allowsOilburnerPort(queriedPos, side) ? new OilburnerFluidHandler(queriedPos.immutable(), side) : null;
        }
        if (this.kind == Kind.HEATEX) {
            return allowsHeatExchangerPort(queriedPos, side) ? new HeatExchangerFluidHandler(queriedPos.immutable(), side) : null;
        }
        if ((this.kind == Kind.FIREBOX || this.kind == Kind.OVEN) && allowsSmokePort(queriedPos, side)) {
            return new SmokeOutputFluidHandler(queriedPos.immutable(), side);
        }
        return null;
    }

    public List<Port> oilburnerPorts(LevelAccessor level) {
        return List.of(
                new Port(this.worldPosition.relative(Direction.EAST), Direction.EAST),
                new Port(this.worldPosition.relative(Direction.WEST), Direction.WEST),
                new Port(this.worldPosition.relative(Direction.SOUTH), Direction.SOUTH),
                new Port(this.worldPosition.relative(Direction.NORTH), Direction.NORTH)
        );
    }

    private boolean allowsOilburnerPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : oilburnerPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    public List<Port> heatExchangerPorts(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction sideways = facing.getClockWise();
        Direction back = facing.getOpposite();
        return List.of(
                new Port(this.worldPosition.relative(facing).relative(sideways), facing),
                new Port(this.worldPosition.relative(facing).relative(sideways.getOpposite()), facing),
                new Port(this.worldPosition.relative(back).relative(sideways), back),
                new Port(this.worldPosition.relative(back).relative(sideways.getOpposite()), back)
        );
    }

    private boolean allowsHeatExchangerPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : heatExchangerPorts(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private boolean allowsSmokePort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null || side == Direction.DOWN) {
            return false;
        }
        BlockPos diff = queriedPos.subtract(this.worldPosition);
        return diff.getY() == 0
                && Math.abs(diff.getX()) <= 1
                && Math.abs(diff.getZ()) <= 1;
    }

    private void tickOilburner() {
        boolean changed = tickOilburnerContainers();
        int heatPerMillibucket = flammableHeatPerMillibucket(this.oilburnerFluid);
        int burnRate = this.oilTank.type() == this.oilburnerFluid ? Math.min(this.setting, this.oilTank.amount()) : 0;
        boolean burned = false;
        this.burnHeat = 0;

        if (this.enabled && this.heatEnergy < maxHeat() && heatPerMillibucket > 0 && burnRate > 0) {
            this.oilTank.drain(this.oilTank.type(), burnRate, false);
            if (this.level != null && this.level.getGameTime() % 5L == 0L) {
                HbmPollution.bufferedLegacyPolluteFluid(this.level, this.worldPosition, this.oilburnerFluid, HbmPollution.ReleaseType.BURN, burnRate, this::smokeTank);
            }
            this.burnHeat = heatPerMillibucket * burnRate;
            addHeat(this.burnHeat);
            burned = true;
            changed = true;
        }

        if (!burned && this.heatEnergy < maxHeat()) {
            int previousHeat = this.heatEnergy;
            coolPassively();
            changed |= previousHeat != this.heatEnergy;
        }

        if (changed) {
            setChanged();
        }
    }

    private boolean tickOilburnerContainers() {
        boolean changed = applyOilburnerIdentifier();
        ItemStack input = this.fuelA;
        changed |= HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.oilTank,
                this::acceptsConfiguredOilburnerFluid,
                output -> canPlaceOutput(OILBURNER_OUTPUT_SLOT, output),
                output -> placeOutput(OILBURNER_OUTPUT_SLOT, output)
        );
        if (input.isEmpty()) {
            this.fuelA = ItemStack.EMPTY;
        }
        return changed;
    }

    private boolean applyOilburnerIdentifier() {
        if (!(this.fluidIdentifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition next = FluidIdentifierItem.primary(this.fluidIdentifier);
        if (!isOilburnerFluid(next)) {
            return false;
        }
        return setConfiguredOilburnerFluid(next);
    }

    private boolean applyHeatExchangerIdentifier() {
        if (!(this.fluidIdentifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        return setConfiguredHeatExchangerFluid(FluidIdentifierItem.primary(this.fluidIdentifier));
    }

    private boolean setConfiguredHeatExchangerFluid(HbmFluidDefinition fluid) {
        HbmFluidDefinition next = HbmThermalConversions.heatExchangerCoolingStep(fluid).isPresent()
                ? fluid
                : HbmFluids.none();
        if (next == this.heatExchangerFluid) {
            setupHeatExchangerTanks();
            return false;
        }
        this.heatExchangerFluid = next;
        this.heatExInputTank.clear();
        this.heatExOutputTank.clear();
        setupHeatExchangerTanks();
        return true;
    }

    private void setupHeatExchangerTanks() {
        HbmThermalConversions.heatExchangerCoolingStep(this.heatExchangerFluid).ifPresentOrElse(step -> {
            if (this.heatExInputTank.amount() == 0 && this.heatExInputTank.type() != step.input()) {
                this.heatExInputTank.setType(step.input());
            }
            if (this.heatExOutputTank.amount() == 0 && this.heatExOutputTank.type() != step.output()) {
                this.heatExOutputTank.setType(step.output());
            }
        }, () -> {
            this.heatExInputTank.clear();
            this.heatExOutputTank.clear();
        });
    }

    private void tickHeatExchanger(Level level) {
        boolean changed = applyHeatExchangerIdentifier();
        setupHeatExchangerTanks();
        decayWithLegacyMultiplier();
        if (level.getGameTime() % this.heatExchangerTickDelay == 0L) {
            HbmThermalConversions.CoolingStep step = HbmThermalConversions.heatExchangerCoolingStep(this.heatExchangerFluid).orElse(null);
            if (step != null) {
                int inputOps = this.heatExInputTank.amount() / step.amountReq();
                int outputOps = (this.heatExOutputTank.capacity() - this.heatExOutputTank.amount()) / step.amountProduced();
                int ops = Math.min(this.heatExchangerAmountToCool, Math.min(inputOps, outputOps));
                if (ops > 0) {
                    this.heatExInputTank.drain(step.input(), step.amountReq() * ops, false);
                    this.heatExOutputTank.fill(step.output(), step.amountProduced() * ops, false);
                    addHeatUncapped((int) (step.heatEnergy() * ops * step.turbineEfficiency()));
                    changed = true;
                }
            }
        }
        if (this.heatExOutputTank.amount() > 0 && !this.heatExOutputTank.type().isNone()) {
            for (Port port : heatExchangerPorts(level)) {
                if (this.heatExOutputTank.amount() <= 0) {
                    break;
                }
                FluidStack stack = HbmFluids.toNeoStack(this.heatExOutputTank.type(), this.heatExOutputTank.amount());
                BlockPos target = port.pos().relative(port.face());
                int accepted = HbmFluidNetworks.fillInto(level, target, port.face().getOpposite(), stack, this.worldPosition, true);
                if (accepted > 0) {
                    this.heatExOutputTank.drain(this.heatExOutputTank.type(), accepted, false);
                    changed = true;
                }
            }
        }
        if (changed) {
            syncToClient();
        }
    }

    private boolean setConfiguredOilburnerFluid(HbmFluidDefinition fluid) {
        HbmFluidDefinition next = normalizeOilburnerFluid(fluid);
        if (next == this.oilburnerFluid) {
            if (this.oilTank.amount() == 0 && this.oilTank.type().isNone()) {
                this.oilTank.setType(next);
            }
            return false;
        }
        this.oilburnerFluid = next;
        this.oilTank.setType(next);
        return true;
    }

    private boolean acceptsConfiguredOilburnerFluid(HbmFluidDefinition fluid) {
        return isOilburnerFluid(fluid) && fluid == this.oilburnerFluid;
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = getItem(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            setItem(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
            setChanged();
        }
    }

    private void addHeat(int amount) {
        if (amount <= 0 || this.heatEnergy >= maxHeat()) {
            return;
        }
        this.heatEnergy = (int) Math.min(maxHeat(), (long) this.heatEnergy + amount);
    }

    private void addHeatUncapped(int amount) {
        if (amount <= 0) {
            return;
        }
        this.heatEnergy = (int) Math.min(Integer.MAX_VALUE, (long) this.heatEnergy + amount);
    }

    private void coolPassively() {
        if (this.heatEnergy > 0) {
            this.heatEnergy = Math.max(0, this.heatEnergy - Math.max(this.heatEnergy / 1000, 1));
        }
    }

    private void decayWithLegacyMultiplier() {
        this.heatEnergy = (int) (this.heatEnergy * 0.999D);
    }

    private long getConsumption() {
        return (long) (Math.pow(this.setting, 1.4D) * 200D);
    }

    private long getMaxPower() {
        return getConsumption() * 20L;
    }

    private void pullHeatFromBelow(Level level, double efficiency, boolean respectCapacity) {
        HeatSourceBlockEntity source = heatSourceBelow(level);
        if (source == null) {
            return;
        }

        int sourceHeat = source.getHeatStored();
        if (sourceHeat <= 0) {
            return;
        }

        int toPull = sourceHeat;
        if (respectCapacity) {
            toPull = Math.min(sourceHeat, Math.max(0, maxHeat() - this.heatEnergy));
        }
        if (toPull <= 0) {
            return;
        }

        int gained = (int) (toPull * efficiency);
        source.useHeat(toPull);
        if (respectCapacity) {
            addHeat(gained);
        } else {
            addHeatUncapped(gained);
        }
    }

    @Nullable
    private HeatSourceBlockEntity heatSourceBelow(Level level) {
        BlockPos below = this.worldPosition.below();
        BlockEntity blockEntity = level.getBlockEntity(below);
        if (blockEntity instanceof HeatSourceBlockEntity source && blockEntity != this) {
            return source;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            if (!corePos.equals(this.worldPosition) && level.getBlockEntity(corePos) instanceof HeatSourceBlockEntity source) {
                return source;
            }
        }
        return null;
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            if (this.kind == Kind.OILBURNER) {
                for (Port port : oilburnerPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                }
            }
            if (this.kind == Kind.HEATEX) {
                for (Port port : heatExchangerPorts(this.level)) {
                    this.level.invalidateCapabilities(port.pos());
                }
            }
            if (this.kind == Kind.FIREBOX || this.kind == Kind.OVEN) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        this.level.invalidateCapabilities(this.worldPosition.offset(x, 0, z));
                    }
                }
            }
        }
    }

    public record Port(BlockPos pos, Direction face) {
    }

    private void tickSolidFuel() {
        if (!this.enabled) {
            this.burnHeat = 0;
            coolPassively();
            return;
        }

        if (this.burnTime <= 0) {
            startBurningFuel();
        }

        if (this.burnTime > 0 && this.heatEnergy < maxHeat()) {
            this.burnTime--;
            addHeat(this.burnHeat);
            if ((this.kind == Kind.FIREBOX || this.kind == Kind.OVEN) && this.level != null && this.level.getGameTime() % 20L == 0L) {
                bufferedPollute(HbmPollutionType.SOOT, HbmPollutionConstants.SOOT_PER_SECOND * 3.0D);
            }
        } else if (this.burnTime <= 0) {
            this.burnHeat = 0;
            coolPassively();
        }
    }

    private void bufferedPollute(HbmPollutionType type, double amount) {
        if (this.level == null || amount <= 0.0D) {
            return;
        }
        HbmFluidTank tank = smokeTank(type);
        int fluidAmount = (int) Math.ceil(amount * 100.0D);
        int accepted = tank.fill(smokeFluid(type), fluidAmount, false);
        int overflow = fluidAmount - accepted;
        if (overflow > 0) {
            HbmPollution.increment(this.level, this.worldPosition, type, overflow / 100.0D);
        }
    }

    private void sendSmoke() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Direction sideways = direction.getClockWise();
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos target = this.worldPosition
                        .relative(direction, 2)
                        .relative(sideways, offset);
                sendSmokeTo(target, direction.getOpposite(), this.smokeTank);
                sendSmokeTo(target, direction.getOpposite(), this.smokeLeadedTank);
                sendSmokeTo(target, direction.getOpposite(), this.smokePoisonTank);
            }
        }
    }

    private void sendSmokeTo(BlockPos target, Direction side, HbmFluidTank tank) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        FluidStack stack = HbmFluids.toNeoStack(tank.type(), tank.amount());
        int accepted = HbmFluidNetworks.fillInto(this.level, target, side, stack, this.worldPosition, true);
        if (accepted > 0) {
            tank.drain(tank.type(), accepted, false);
            setChanged();
        }
    }

    private void sendOilburnerSmoke() {
        if (this.level == null || this.level.isClientSide || this.kind != Kind.OILBURNER) {
            return;
        }
        boolean changed = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = this.worldPosition.relative(direction, 2);
            changed |= HbmPollution.sendSmoke(this.level, this.worldPosition, target, direction.getOpposite(), this.smokeTank, this.smokeLeadedTank, this.smokePoisonTank);
        }
        if (changed) {
            setChanged();
        }
    }

    private void startBurningFuel() {
        for (int slot = 0; slot < 2; slot++) {
            ItemStack stack = getItem(slot);
            int duration = legacyFuelDuration(stack);
            if (duration <= 0) {
                continue;
            }
            int heat = legacyFuelHeat(stack);
            Item consumed = stack.getItem();
            stack.shrink(1);
            if (stack.isEmpty()) {
                setItem(slot, consumed == Items.LAVA_BUCKET ? new ItemStack(Items.BUCKET) : ItemStack.EMPTY);
            }
            this.maxBurnTime = this.burnTime = duration;
            this.burnHeat = heat;
            break;
        }
    }

    /** Mirrors the 1.7.10 ModuleBurnTime profiles used by fireboxes and heating ovens. */
    private int legacyFuelDuration(ItemStack stack) {
        int baseDuration = fuelDuration(stack);
        if (baseDuration <= 0) {
            return 0;
        }
        double multiplier = this.kind == Kind.OVEN ? 0.125D : 1.0D;
        String path = fuelPath(stack);
        if (isBalefireFuel(path)) {
            multiplier *= 0.5D;
        } else if (path.startsWith("solid_fuel") || path.equals("rocket_fuel")) {
            multiplier *= 1.5D;
        } else if (isCoalFuel(path)) {
            multiplier *= 1.25D;
        }
        return (int) (baseDuration * multiplier);
    }

    private int legacyFuelHeat(ItemStack stack) {
        int baseHeat = this.kind == Kind.OVEN ? 500 : 100;
        String path = fuelPath(stack);
        if (isBalefireFuel(path)) {
            return baseHeat * 15;
        }
        if (path.startsWith("solid_fuel")) {
            return baseHeat * 3;
        }
        if (path.equals("rocket_fuel")) {
            return baseHeat * 5;
        }
        if (isCoalFuel(path)) {
            return baseHeat * 2;
        }
        return baseHeat;
    }

    private static boolean isCoalFuel(String path) {
        return path.contains("coke") || path.contains("coal") || path.contains("lignite");
    }

    private static boolean isBalefireFuel(String path) {
        return path.startsWith("solid_fuel") && path.contains("_bf");
    }

    private static String fuelPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
    }

    private static int maxHeat(Kind kind) {
        return kind == Kind.OVEN ? OVEN_MAX_HEAT : MAX_HEAT;
    }

    private static HbmFluidDefinition defaultOilburnerFluid() {
        return HbmFluids.byName("heatingoil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition defaultHeatExchangerFluid() {
        return HbmFluids.byName("coolant_hot").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition defaultHeatExchangerOutput() {
        return HbmFluids.byName("coolant").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokeFluid() {
        return HbmFluids.byName("smoke").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokeLeadedFluid() {
        return HbmFluids.byName("smoke_leaded").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokePoisonFluid() {
        return HbmFluids.byName("smoke_poison").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition smokeFluid(HbmPollutionType type) {
        return switch (type) {
            case SOOT -> smokeFluid();
            case HEAVYMETAL -> smokeLeadedFluid();
            case POISON -> smokePoisonFluid();
            case FALLOUT -> HbmFluids.none();
        };
    }

    private HbmFluidTank smokeTank(HbmPollutionType type) {
        return switch (type) {
            case SOOT -> this.smokeTank;
            case HEAVYMETAL -> this.smokeLeadedTank;
            case POISON -> this.smokePoisonTank;
            case FALLOUT -> this.smokeTank;
        };
    }

    private static HbmFluidDefinition normalizeOilburnerFluid(HbmFluidDefinition fluid) {
        return isOilburnerFluid(fluid) ? fluid : defaultOilburnerFluid();
    }

    private static boolean isOilburnerFluid(HbmFluidDefinition fluid) {
        return flammableHeatPerMillibucket(fluid) > 0;
    }

    public static int flammableHeatPerMillibucket(HbmFluidDefinition fluid) {
        return fluid == null ? 0 : fluid.flammableHeatPerMillibucket();
    }

    private final class OilburnerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private OilburnerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? oilTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? oilTank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsOilburnerPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return acceptsConfiguredOilburnerFluid(fluid) && (oilTank.type().isNone() || oilTank.type() == fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsOilburnerPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!acceptsConfiguredOilburnerFluid(fluid)) {
                return 0;
            }
            int filled = oilTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                setChanged();
                syncToClient();
            }
            return filled;
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

    private final class HeatExchangerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private HeatExchangerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> heatExInputTank.getFluidInTank(0);
                case 1 -> heatExOutputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> heatExInputTank.capacity();
                case 1 -> heatExOutputTank.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty() || !allowsHeatExchangerPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return fluid == heatExchangerFluid && HbmThermalConversions.heatExchangerCoolingStep(fluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsHeatExchangerPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != heatExchangerFluid || HbmThermalConversions.heatExchangerCoolingStep(fluid).isEmpty()) {
                return 0;
            }
            int filled = heatExInputTank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                syncToClient();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsHeatExchangerPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != heatExOutputTank.type()) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsHeatExchangerPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = heatExOutputTank.drain(maxDrain, action);
            if (!drained.isEmpty() && action.execute()) {
                syncToClient();
            }
            return drained;
        }
    }

    private final class SmokeOutputFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private SmokeOutputFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 3;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            HbmFluidTank hbmTank = smokeTankByIndex(tank);
            return hbmTank == null ? FluidStack.EMPTY : hbmTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            HbmFluidTank hbmTank = smokeTankByIndex(tank);
            return hbmTank == null ? 0 : hbmTank.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsSmokePort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank hbmTank = smokeTankForFluid(fluid);
            if (hbmTank == null) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = hbmTank.drain(resource, action);
            if (!drained.isEmpty() && action.execute()) {
                setChanged();
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsSmokePort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            for (int i = 0; i < getTanks(); i++) {
                HbmFluidTank hbmTank = smokeTankByIndex(i);
                if (hbmTank == null || hbmTank.amount() <= 0 || hbmTank.type().isNone()) {
                    continue;
                }
                FluidStack drained = hbmTank.drain(maxDrain, action);
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        setChanged();
                    }
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        @Nullable
        private HbmFluidTank smokeTankByIndex(int tank) {
            return switch (tank) {
                case 0 -> smokeTank;
                case 1 -> smokeLeadedTank;
                case 2 -> smokePoisonTank;
                default -> null;
            };
        }

        @Nullable
        private HbmFluidTank smokeTankForFluid(HbmFluidDefinition fluid) {
            if (fluid == smokeTank.type()) {
                return smokeTank;
            }
            if (fluid == smokeLeadedTank.type()) {
                return smokeLeadedTank;
            }
            if (fluid == smokePoisonTank.type()) {
                return smokePoisonTank;
            }
            return null;
        }
    }

    private void tickClientAnimation(BlockState state) {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = this.doorAngle / 10.0F + 3.0F;
        if (this.playersUsing > 0) {
            this.doorAngle = Math.min(135.0F, this.doorAngle + swingSpeed);
        } else {
            this.doorAngle = Math.max(0.0F, this.doorAngle - swingSpeed);
        }

        if ((this.kind == Kind.FIREBOX || this.kind == Kind.OVEN) && this.burnTime > 0 && this.level != null && this.level.getGameTime() % 5 == 0) {
            Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
            double x = this.worldPosition.getX() + 0.5D + facing.getStepX() * 0.75D;
            double y = this.worldPosition.getY() + 0.25D;
            double z = this.worldPosition.getZ() + 0.5D + facing.getStepZ() * 0.75D;
            this.level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            if (this.level.random.nextInt(3) == 0) {
                this.level.addParticle(ParticleTypes.SMOKE, x, y + 0.15D, z, 0.0D, 0.03D, 0.0D);
            }
        }
    }

    public static int fuelDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        // 1.7.10 fireboxes query Forge's raw fuel time. Wood burner bonuses
        // are specific to the wood burner and must not leak into heaters.
        return Math.max(0, stack.getBurnTime(null));
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    public enum Kind {
        FIREBOX("firebox"),
        OVEN("oven"),
        OILBURNER("oilburner"),
        ELECTRIC("electric"),
        HEATEX("heatex");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        private static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return FIREBOX;
        }
    }
}
