package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.GasFlareMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.util.SettingsCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasFlareBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, SettingsCopiable {
    public static final long MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_INPUT = 1;
    public static final int SLOT_FLUID_OUTPUT = 2;
    public static final int SLOT_IDENTIFIER = 3;
    public static final int SLOT_SPEED_UPGRADE = 4;
    public static final int SLOT_EFFECT_UPGRADE = 5;
    public static final int SLOT_COUNT = 6;
    public static final int DATA_COUNT = 10;
    private static final int BASE_VENT_RATE = 50;
    private static final int BASE_BURN_RATE = 10;
    private static final int[] ALL_SLOTS = {
            SLOT_BATTERY,
            SLOT_FLUID_INPUT,
            SLOT_FLUID_OUTPUT,
            SLOT_IDENTIFIER,
            SLOT_SPEED_UPGRADE,
            SLOT_EFFECT_UPGRADE
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(defaultFluid(), TANK_CAPACITY);
    private HbmFluidDefinition configuredFluid = defaultFluid();
    private long power;
    private boolean valveOpen;
    private boolean ignitionEnabled;
    private boolean active;
    private int fluidUsed;
    private int output;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) GasFlareBlockEntity.this.power;
                case 1 -> (int) MAX_POWER;
                case 2 -> GasFlareBlockEntity.this.valveOpen ? 1 : 0;
                case 3 -> GasFlareBlockEntity.this.ignitionEnabled ? 1 : 0;
                case 4 -> GasFlareBlockEntity.this.active ? 1 : 0;
                case 5 -> GasFlareBlockEntity.this.displayFluid().oldId();
                case 6 -> GasFlareBlockEntity.this.tank.amount();
                case 7 -> GasFlareBlockEntity.this.tank.capacity();
                case 8 -> GasFlareBlockEntity.this.fluidUsed;
                case 9 -> GasFlareBlockEntity.this.output;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> GasFlareBlockEntity.this.power = Math.max(0L, Math.min(MAX_POWER, value));
                case 2 -> GasFlareBlockEntity.this.valveOpen = value != 0;
                case 3 -> GasFlareBlockEntity.this.ignitionEnabled = value != 0;
                case 4 -> GasFlareBlockEntity.this.active = value != 0;
                case 8 -> GasFlareBlockEntity.this.fluidUsed = Math.max(0, value);
                case 9 -> GasFlareBlockEntity.this.output = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public GasFlareBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.GAS_FLARE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GasFlareBlockEntity flare) {
        if (level.isClientSide) {
            flare.tickClient(level);
        } else {
            flare.tickServer(level);
        }
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public HbmFluidDefinition configuredFluid() {
        return this.configuredFluid;
    }

    public boolean valveOpen() {
        return this.valveOpen;
    }

    public boolean ignitionEnabled() {
        return this.ignitionEnabled;
    }

    public boolean active() {
        return this.active;
    }

    public void toggleValve() {
        this.valveOpen = !this.valveOpen;
        sync();
    }

    public void toggleIgnition() {
        this.ignitionEnabled = !this.ignitionEnabled;
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new FlareFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return ports().stream().map(Port::connectorPos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : ports()) {
            if (port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return this.power;
    }

    @Override
    public long getRequestedInput() {
        return 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.max(0L, this.power - usedOutput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.gas_flare",
                this.output,
                this.power,
                MAX_POWER
        );
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
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> BatteryPackItem.isBattery(stack);
            case SLOT_FLUID_INPUT -> canDrainContainer(stack);
            case SLOT_IDENTIFIER -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_SPEED_UPGRADE -> isUpgrade(stack, MachineUpgradeItem.UpgradeType.SPEED);
            case SLOT_EFFECT_UPGRADE -> isUpgrade(stack, MachineUpgradeItem.UpgradeType.EFFECT);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_FLUID_OUTPUT) {
            return true;
        }
        return slot == SLOT_BATTERY
                && BatteryPackItem.isBattery(stack)
                && BatteryPackItem.charge(stack) >= BatteryPackItem.capacity(stack);
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
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.gas_flare");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasFlareMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag settings = new CompoundTag();
        settings.putIntArray("fluidID", new int[]{this.configuredFluid.oldId()});
        settings.putBoolean("isOn", this.valveOpen);
        settings.putBoolean("doesBurn", this.ignitionEnabled);
        return settings;
    }

    @Override
    public void pasteSettings(CompoundTag settings, int index, Level level, Player player, BlockPos pos) {
        int[] fluidIds = settings.getIntArray("fluidID");
        if (index >= 0 && index < fluidIds.length) {
            HbmFluidDefinition fluid = HbmFluids.byOldId(fluidIds[index]).orElse(defaultFluid());
            if (!fluid.isNone()) {
                this.configuredFluid = fluid;
                if (this.tank.amount() > 0 && this.tank.type() != fluid) {
                    this.tank.clear();
                }
                ensureConfiguredTankType();
            }
        }
        if (settings.contains("isOn")) {
            this.valveOpen = settings.getBoolean("isOn");
        }
        if (settings.contains("doesBurn")) {
            this.ignitionEnabled = settings.getBoolean("doesBurn");
        }
        sync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        stack.copy()
                ));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putBoolean("ValveOpen", this.valveOpen);
        tag.putBoolean("IgnitionEnabled", this.ignitionEnabled);
        tag.putBoolean("Active", this.active);
        tag.putInt("FluidUsed", this.fluidUsed);
        tag.putInt("Output", this.output);
        tag.putString("ConfiguredFluid", this.configuredFluid.name());
        tag.put("Tank", this.tank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.valveOpen = tag.getBoolean("ValveOpen");
        this.ignitionEnabled = tag.getBoolean("IgnitionEnabled");
        this.active = tag.getBoolean("Active");
        this.fluidUsed = Math.max(0, tag.getInt("FluidUsed"));
        this.output = Math.max(0, tag.getInt("Output"));
        this.configuredFluid = HbmFluids.byName(tag.getString("ConfiguredFluid")).orElse(defaultFluid());
        if (this.configuredFluid.isNone()) {
            this.configuredFluid = defaultFluid();
        }
        this.tank.load(tag.getCompound("Tank"));
        if (this.tank.amount() > 0 && this.tank.type() != this.configuredFluid) {
            this.tank.clear();
        }
        ensureConfiguredTankType();
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

    private void tickServer(Level level) {
        boolean changed = applyFluidIdentifier();
        ensureConfiguredTankType();
        changed |= transferFluidInput();

        long previousPower = this.power;
        this.power = BatteryPackItem.chargeFromMachine(this.items.get(SLOT_BATTERY), this.power);
        changed |= previousPower != this.power;

        this.active = false;
        this.fluidUsed = 0;
        this.output = 0;

        HbmFluidDefinition fluid = this.tank.type();
        if (this.valveOpen && this.tank.amount() > 0 && !fluid.isNone()) {
            int speedLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3);
            int effectLevel = Math.min(upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT), 3);
            int ventRate = BASE_VENT_RATE + BASE_VENT_RATE * speedLevel;
            int burnRate = BASE_BURN_RATE + BASE_BURN_RATE * speedLevel;

            if (this.ignitionEnabled && fluid.hasTrait(HbmFluidTrait.FLAMMABLE)) {
                int consumed = Math.min(burnRate, this.tank.amount());
                this.tank.drain(fluid, consumed, false);
                // Keep the old multiplication/division order.  The 1.7.10
                // source multiplies total heat first, then converts the
                // consumed millibuckets to HE.
                long generated = (fluid.flammableHeatEnergy() * consumed) / 1_000L;
                generated /= isGaseous(fluid) ? 5L : 10L;
                generated += generated * effectLevel / 3L;
                this.power = Math.min(MAX_POWER, this.power + generated);
                this.fluidUsed = consumed;
                this.output = (int) Math.min(Integer.MAX_VALUE, generated);
                this.active = consumed > 0;
                if (consumed > 0) {
                    damageAbove(level);
                    if (level.getGameTime() % 3L == 0L) {
                        level.playSound(
                                null,
                                this.worldPosition.getX() + 0.5D,
                                this.worldPosition.getY() + 11.0D,
                                this.worldPosition.getZ() + 0.5D,
                                HbmSoundEvents.FLAMETHROWER_SHOOT.get(),
                                SoundSource.BLOCKS,
                                1.5F,
                                0.75F
                        );
                    }
                    if (level.getGameTime() % 5L == 0L) {
                        HbmPollution.polluteFluid(level, this.worldPosition, fluid, HbmPollution.ReleaseType.BURN, consumed * 5.0D);
                    }
                }
                changed = true;
            } else if (isGaseous(fluid)) {
                int consumed = Math.min(ventRate, this.tank.amount());
                this.tank.drain(fluid, consumed, false);
                this.fluidUsed = consumed;
                this.active = consumed > 0;
                if (consumed > 0) {
                    if (level.getGameTime() % 7L == 0L) {
                        level.playSound(
                                null,
                                this.worldPosition.getX() + 0.5D,
                                this.worldPosition.getY() + 11.0D,
                                this.worldPosition.getZ() + 0.5D,
                                SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.BLOCKS,
                                1.5F,
                                0.5F
                        );
                    }
                    if (level.getGameTime() % 5L == 0L) {
                        HbmPollution.polluteFluid(level, this.worldPosition, fluid, HbmPollution.ReleaseType.SPILL, consumed * 5.0D);
                    }
                }
                changed = true;
            }
        }

        ensureConfiguredTankType();
        PowerNetworkManager.tickFromEndpoint(level, this);
        if (changed || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level) {
        HbmFluidDefinition fluid = displayFluid();
        if (!this.valveOpen || this.tank.amount() <= 0 || fluid.isNone()) {
            return;
        }

        if (this.ignitionEnabled && fluid.hasTrait(HbmFluidTrait.FLAMMABLE)) {
            level.addParticle(
                    HbmParticleTypes.GAS_FLARE_FLAME.get(),
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 11.75D,
                    this.worldPosition.getZ() + 0.5D,
                    level.random.nextGaussian() * 0.15D,
                    0.2D,
                    level.random.nextGaussian() * 0.15D
            );
            boolean even = level.getGameTime() % 2L == 0L;
            level.addParticle(
                    HbmParticleTypes.GAS_FLARE_BURN_SMOKE.get(),
                    this.worldPosition.getX() + (even ? 1.5D : 1.125D),
                    this.worldPosition.getY() + (even ? 10.75D : 11.75D),
                    this.worldPosition.getZ() + (even ? 1.5D : -0.5D),
                    0.0D,
                    0.0D,
                    0.0D
            );
        } else if (isGaseous(fluid)) {
            int color = fluid.color();
            level.addParticle(
                    HbmParticleTypes.GAS_FLARE_SMOKE.get(),
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 11.0D,
                    this.worldPosition.getZ() + 0.5D,
                    ((color >>> 16) & 0xFF) / 255.0D,
                    ((color >>> 8) & 0xFF) / 255.0D,
                    (color & 0xFF) / 255.0D
            );
        }
    }

    private void damageAbove(Level level) {
        AABB damageBox = new AABB(
                this.worldPosition.getX() - 1.0D,
                this.worldPosition.getY() + 12.0D,
                this.worldPosition.getZ() - 2.0D,
                this.worldPosition.getX() + 2.0D,
                this.worldPosition.getY() + 17.0D,
                this.worldPosition.getZ() + 2.0D
        );
        for (Entity entity : level.getEntities((Entity) null, damageBox, entity -> entity.isAlive())) {
            entity.igniteForSeconds(5.0F);
            entity.hurt(entity.damageSources().onFire(), 5.0F);
        }
    }

    private boolean applyFluidIdentifier() {
        ItemStack identifier = this.items.get(SLOT_IDENTIFIER);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition next = FluidIdentifierItem.primary(identifier);
        if (next == null || next.isNone() || next == this.configuredFluid) {
            return false;
        }
        this.configuredFluid = next;
        this.tank.clear();
        ensureConfiguredTankType();
        return true;
    }

    private boolean transferFluidInput() {
        ItemStack input = this.items.get(SLOT_FLUID_INPUT);
        return HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.tank,
                this::acceptsConfiguredFluid,
                this::canPlaceFluidOutput,
                this::placeFluidOutput
        );
    }

    private boolean canDrainContainer(ItemStack stack) {
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack,
                this.tank,
                this::acceptsConfiguredFluid,
                this::canPlaceFluidOutput
        );
    }

    private boolean canPlaceFluidOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack output = this.items.get(SLOT_FLUID_OUTPUT);
        return output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= output.getMaxStackSize();
    }

    private void placeFluidOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack output = this.items.get(SLOT_FLUID_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_FLUID_OUTPUT, stack.copy());
        } else if (ItemStack.isSameItemSameComponents(output, stack)) {
            output.grow(stack.getCount());
        }
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = SLOT_SPEED_UPGRADE; slot <= SLOT_EFFECT_UPGRADE; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            return side == null;
        }
        for (Port port : ports()) {
            if (port.proxyPos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptsConfiguredFluid(HbmFluidDefinition fluid) {
        return fluid != null && !fluid.isNone() && fluid == this.configuredFluid;
    }

    private HbmFluidDefinition displayFluid() {
        return this.tank.amount() > 0 && !this.tank.type().isNone() ? this.tank.type() : this.configuredFluid;
    }

    private void ensureConfiguredTankType() {
        if (this.tank.amount() == 0 && this.tank.type().isNone() && !this.configuredFluid.isNone()) {
            this.tank.setType(this.configuredFluid);
        }
    }

    private List<Port> ports() {
        return List.of(
                port(Direction.EAST),
                port(Direction.WEST),
                port(Direction.SOUTH),
                port(Direction.NORTH)
        );
    }

    private Port port(Direction direction) {
        return new Port(
                this.worldPosition.relative(direction).immutable(),
                this.worldPosition.relative(direction, 2).immutable(),
                direction
        );
    }

    private void sync() {
        setChanged();
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.level.invalidateCapabilities(this.worldPosition);
        for (Port port : ports()) {
            this.level.invalidateCapabilities(port.proxyPos());
        }
        this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static HbmFluidDefinition defaultFluid() {
        return HbmFluids.byName("gas").orElse(HbmFluids.none());
    }

    private static boolean isGaseous(HbmFluidDefinition fluid) {
        return fluid != null && fluid.hasTrait(HbmFluidTrait.GASEOUS);
    }

    private static boolean isUpgrade(ItemStack stack, MachineUpgradeItem.UpgradeType type) {
        return MachineUpgradeItem.isMachineUpgrade(stack) && MachineUpgradeItem.upgradeType(stack) == type;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private record Port(BlockPos proxyPos, BlockPos connectorPos, Direction face) {
    }

    private final class FlareFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private FlareFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? GasFlareBlockEntity.this.tank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? GasFlareBlockEntity.this.tank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tankIndex != 0 || stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return acceptsConfiguredFluid(fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!acceptsConfiguredFluid(fluid)) {
                return 0;
            }
            int filled = GasFlareBlockEntity.this.tank.fill(fluid, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
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
}
