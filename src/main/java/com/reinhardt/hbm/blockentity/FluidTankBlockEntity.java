package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FluidBarrelBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.menu.FluidTankMenu;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidTankBlockEntity extends BlockEntity implements MenuProvider, FluidCopiable, WorldlyContainer, MachineInventory {
    public static final String ITEM_DATA_KEY = "fluid_tank_data";
    public static final int ID_SLOT = 0;
    public static final int ID_RESULT_SLOT = 1;
    public static final int INPUT_CONTAINER_SLOT = 2;
    public static final int INPUT_CONTAINER_RESULT_SLOT = 3;
    public static final int OUTPUT_CONTAINER_SLOT = 4;
    public static final int OUTPUT_CONTAINER_RESULT_SLOT = 5;
    public static final int SLOT_COUNT = 6;
    public static final int DATA_COUNT = 5;
    public static final int CAPACITY = 256_000;
    private static final int[] AUTOMATION_SLOTS = {
            ID_SLOT,
            ID_RESULT_SLOT,
            INPUT_CONTAINER_SLOT,
            INPUT_CONTAINER_RESULT_SLOT,
            OUTPUT_CONTAINER_SLOT,
            OUTPUT_CONTAINER_RESULT_SLOT
    };

    private final HbmFluidTank tank;
    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final String displayNameKey;
    private Mode mode = Mode.RECEIVE;
    private boolean hasExploded;
    private boolean onFire;
    @Nullable
    private net.minecraft.world.level.Explosion lastExplosion;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> tank.type().oldId();
                case 1 -> tank.amount();
                case 2 -> tank.capacity();
                case 3 -> tank.pressure();
                case 4 -> mode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 3 -> tank.setPressure(value);
                case 4 -> mode = Mode.byOrdinal(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FluidTankBlockEntity(BlockPos pos, BlockState blockState) {
        this(HbmBlockEntities.FLUID_TANK.get(), pos, blockState, CAPACITY, "container.reinhardtshbm.fluid_tank");
    }

    protected FluidTankBlockEntity(BlockPos pos, BlockState blockState, int capacity, String displayNameKey) {
        this(HbmBlockEntities.FLUID_TANK.get(), pos, blockState, capacity, displayNameKey);
    }

    protected FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int capacity, String displayNameKey) {
        super(type, pos, blockState);
        this.tank = new HbmFluidTank(capacity);
        this.displayNameKey = displayNameKey;
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity tank) {
        if (!tank.hasExploded) {
            tank.tickContainers();
        }
        tank.tickServer(level);
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public Mode mode() {
        return mode;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public void setType(HbmFluidDefinition type) {
        tank.setType(type);
        sync();
    }

    public void loadFromItem(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ITEM_DATA_KEY)) {
            return;
        }
        CompoundTag data = root.getCompound(ITEM_DATA_KEY);
        this.tank.load(data.getCompound("Tank"));
        this.mode = Mode.byOrdinal(data.getByte("Mode"));
        this.hasExploded = data.getBoolean("Exploded");
        this.onFire = data.getBoolean("OnFire");
        sync();
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag data = new CompoundTag();
        if (!this.tank.type().isNone() && this.tank.amount() > 0) {
            data.put("Tank", this.tank.save());
        }
        if (this.mode != Mode.RECEIVE) {
            data.putByte("Mode", (byte) this.mode.ordinal());
        }
        if (this.hasExploded) {
            data.putBoolean("Exploded", true);
            data.putBoolean("OnFire", this.onFire);
        }
        if (data.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ITEM_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{tank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        setType(fluid);
    }

    public void cycleMode() {
        if (this.hasExploded) {
            return;
        }
        mode = Mode.byOrdinal(mode.ordinal() + 1);
        sync();
    }

    public boolean isDamaged() {
        return this.hasExploded;
    }

    public boolean repair(Player player) {
        if (!this.hasExploded || this.level == null || this.level.isClientSide) {
            return false;
        }
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> steelPlates = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "plates/steel"));
        int available = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(steelPlates)) {
                available += stack.getCount();
            }
        }
        if (available < 6) {
            return false;
        }
        int remaining = 6;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(steelPlates)) {
                continue;
            }
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
        }
        this.hasExploded = false;
        this.onFire = false;
        this.lastExplosion = null;
        sync();
        return true;
    }

    public void handleExplosion(net.minecraft.world.level.Explosion explosion) {
        if (this.level == null || this.level.isClientSide || this.lastExplosion == explosion) {
            return;
        }
        this.lastExplosion = explosion;
        if (this.hasExploded) {
            this.level.removeBlock(this.worldPosition, false);
            return;
        }
        this.hasExploded = true;
        this.onFire = this.tank.type().hasTrait(HbmFluidTrait.FLAMMABLE);
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (this.hasExploded || !allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new TankHandler();
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.hasExploded ? null : side == null ? new TankHandler() : fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        return portsFor(this.worldPosition, facing);
    }

    /** Refreshes machine ports after a multiblock has finished placing its dummies. */
    public void refreshConnectionsAfterPlacement() {
        if (this.level != null && !this.level.isClientSide) {
            refreshPortNeighbors(this.level);
        }
    }

    public static List<Port> portsFor(BlockPos pos, Direction facing) {
        return List.of(
                Port.fromLocalConnector(pos, new BlockPos(2, 0, -1), Direction.EAST, facing),
                Port.fromLocalConnector(pos, new BlockPos(2, 0, 1), Direction.EAST, facing),
                Port.fromLocalConnector(pos, new BlockPos(-2, 0, -1), Direction.WEST, facing),
                Port.fromLocalConnector(pos, new BlockPos(-2, 0, 1), Direction.WEST, facing),
                Port.fromLocalConnector(pos, new BlockPos(-1, 0, 2), Direction.SOUTH, facing),
                Port.fromLocalConnector(pos, new BlockPos(1, 0, 2), Direction.SOUTH, facing),
                Port.fromLocalConnector(pos, new BlockPos(-1, 0, -2), Direction.NORTH, facing),
                Port.fromLocalConnector(pos, new BlockPos(1, 0, -2), Direction.NORTH, facing)
        );
    }

    public static List<Port> portsFor(BlockPos pos) {
        return portsFor(pos, Direction.NORTH);
    }

    protected boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || port.face() == side)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items[slot] : ItemStack.EMPTY;
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
        ItemStack stack = getItem(slot);
        if (isValidSlot(slot)) {
            this.items[slot] = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items[slot] = stack;
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case ID_SLOT -> stack.getItem() instanceof FluidIdentifierItem;
            case INPUT_CONTAINER_SLOT -> isDrainableContainer(stack);
            case OUTPUT_CONTAINER_SLOT -> isFillableContainer(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == ID_SLOT || slot == INPUT_CONTAINER_SLOT || slot == OUTPUT_CONTAINER_SLOT) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == ID_RESULT_SLOT || slot == INPUT_CONTAINER_RESULT_SLOT || slot == OUTPUT_CONTAINER_RESULT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.EMPTY;
        }
        setChanged();
    }

    protected boolean canAcceptFluid(HbmFluidDefinition fluid) {
        return fluid != null && !fluid.isNone();
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }
        if (!this.hasExploded) {
            if (mode.canSend() && tank.amount() > 0 && !tank.type().isNone()) {
                int budget = Math.min(maxProviderTransfer(), tank.amount());
                for (Port port : ports(level)) {
                    if (budget <= 0) {
                        break;
                    }
                    FluidStack offered = HbmFluids.toNeoStack(tank.type(), budget);
                    int accepted = HbmFluidNetworks.fillInto(
                            level,
                            port.connectorPos(),
                            port.face().getOpposite(),
                            offered,
                            this.worldPosition,
                            true
                    );
                    if (accepted > 0) {
                        tank.drain(tank.type(), accepted, false);
                        budget -= accepted;
                        sync();
                    }
                }
            }
            checkHazardousFluid(level);
        } else {
            leak(level);
        }
    }

    private void checkHazardousFluid(Level level) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        HbmFluidDefinition fluid = tank.type();
        if (fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.5D,
                    worldPosition.getZ() + 0.5D, 5.0F, false, Level.ExplosionInteraction.NONE);
            markDamaged();
            tank.setAmount(0);
            sync();
            return;
        }
        if (isHighlyCorrosive(fluid)) {
            markDamaged();
            sync();
        }
    }

    private void leak(Level level) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        HbmFluidDefinition fluid = tank.type();
        int leaking = fluid.hasTrait(HbmFluidTrait.ANTIMATTER)
                ? tank.amount()
                : isGaseous(fluid) ? Math.min(tank.amount(), Math.max(1, tank.capacity() / 100))
                : Math.min(tank.amount(), Math.max(1, tank.capacity() / 10_000));
        if (leaking <= 0) {
            return;
        }
        tank.drain(fluid, leaking, false);
        if (fluid.hasTrait(HbmFluidTrait.FLAMMABLE) && onFire) {
            AABB area = new AABB(worldPosition).inflate(1.5D, 4.0D, 1.5D);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
                entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 100));
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.hurt(level.damageSources().onFire(), 5.0F);
                }
            }
            if (level.getGameTime() % 5L == 0L && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(HbmParticleTypes.GAS_FLARE_FLAME.get(),
                        worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                        1, 0.0D, 0.1D, 0.0D, 0.0D);
            }
            HbmPollution.polluteFluid(level, worldPosition, fluid, HbmPollution.ReleaseType.BURN, leaking * 5.0D);
        } else if (isGaseous(fluid)) {
            if (level.getGameTime() % 5L == 0L && level instanceof ServerLevel serverLevel) {
                int color = fluid.color();
                serverLevel.sendParticles(HbmParticleTypes.DRAIN_TOWER.get(),
                        worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D,
                        0, ((color >>> 16) & 0xFF) / 255.0D, ((color >>> 8) & 0xFF) / 255.0D,
                        (color & 0xFF) / 255.0D, 1.0D);
            }
            HbmPollution.polluteFluid(level, worldPosition, fluid, HbmPollution.ReleaseType.SPILL, leaking * 5.0D);
        }
        sync();
    }

    private void markDamaged() {
        this.hasExploded = true;
        this.onFire = this.tank.type().hasTrait(HbmFluidTrait.FLAMMABLE);
    }

    private static boolean isGaseous(HbmFluidDefinition fluid) {
        return fluid.hasTrait(HbmFluidTrait.GASEOUS) || fluid.hasTrait(HbmFluidTrait.EVAPORATES);
    }

    private static boolean isHighlyCorrosive(HbmFluidDefinition fluid) {
        for (String token : fluid.rawTraits().split("\\|")) {
            String[] parts = token.split(":");
            if (parts.length >= 2 && parts[0].equals("CORROSIVE")) {
                try {
                    return Double.parseDouble(parts[1]) > 50.0D;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    protected int maxProviderTransfer() {
        return Math.max(500, tank.amount() / 100);
    }

    protected int maxReceiverTransfer() {
        return Math.max(500, (tank.capacity() - tank.amount()) / 100);
    }

    private void tickContainers() {
        boolean changed = false;
        changed |= applyIdentifier();
        changed |= drainContainerIntoTank();
        changed |= fillContainerFromTank();
        if (changed) {
            sync();
        }
    }

    private boolean applyIdentifier() {
        ItemStack input = this.items[ID_SLOT];
        if (!(input.getItem() instanceof FluidIdentifierItem) || !this.items[ID_RESULT_SLOT].isEmpty()) {
            return false;
        }
        HbmFluidDefinition next = FluidIdentifierItem.primary(input);
        if (next == this.tank.type() || !canAcceptFluid(next)) {
            return false;
        }
        this.tank.setType(next);
        ItemStack result = input.copy();
        result.setCount(1);
        this.items[ID_RESULT_SLOT] = result;
        input.shrink(1);
        if (input.isEmpty()) {
            this.items[ID_SLOT] = ItemStack.EMPTY;
        }
        return true;
    }

    private boolean drainContainerIntoTank() {
        ItemStack input = this.items[INPUT_CONTAINER_SLOT];
        if (input.isEmpty()) {
            return false;
        }
        return HbmFluidContainerTransfer.drainIntoTank(
                input,
                this.tank,
                this::canAcceptFluid,
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean fillContainerFromTank() {
        ItemStack input = this.items[OUTPUT_CONTAINER_SLOT];
        return HbmFluidContainerTransfer.fillFromTank(
                input,
                this.tank,
                output -> canPlaceOutput(OUTPUT_CONTAINER_RESULT_SLOT, output),
                output -> placeOutput(OUTPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean isDrainableContainer(ItemStack stack) {
        return HbmFluidContainerTransfer.canDrainIntoTank(
                stack,
                this.tank,
                this::canAcceptFluid,
                output -> canPlaceOutput(INPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean isFillableContainer(ItemStack stack) {
        return HbmFluidContainerTransfer.canFillFromTank(
                stack,
                this.tank,
                output -> canPlaceOutput(OUTPUT_CONTAINER_RESULT_SLOT, output)
        );
    }

    private boolean canPlaceOutput(int slot, ItemStack stack) {
        ItemStack current = this.items[slot];
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack stack) {
        ItemStack current = this.items[slot];
        if (current.isEmpty()) {
            this.items[slot] = stack.copy();
        } else {
            current.grow(stack.getCount());
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(displayNameKey);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidTankMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        dropInventoryContents(level, pos);
        if (this.tank.amount() > 0 && !this.tank.type().isNone()) {
            HbmPollution.polluteFluid(level, pos, this.tank.type(), HbmPollution.ReleaseType.SPILL, this.tank.amount());
        }
    }

    protected void dropInventoryContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.items.length; i++) {
            drop(level, pos, this.items[i]);
            this.items[i] = ItemStack.EMPTY;
        }
    }

    /** Drops container/identifier slots while leaving persistent tank contents on the block item. */
    public void dropInventoryContentsOnly(Level level, BlockPos pos) {
        dropInventoryContents(level, pos);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            tag.put("Slot" + i, this.items[i].saveOptional(registries));
        }
        tag.put("Tank", tank.save());
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putBoolean("Exploded", this.hasExploded);
        tag.putBoolean("OnFire", this.onFire);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + i));
        }
        tank.load(tag.getCompound("Tank"));
        mode = Mode.byOrdinal(tag.getByte("Mode"));
        hasExploded = tag.getBoolean("Exploded");
        onFire = tag.getBoolean("OnFire");
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

    protected void sync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (this.getBlockState().getBlock() instanceof FluidBarrelBlock) {
                FluidBarrelBlock.refreshConnections(this.level, this.worldPosition);
            }
            if (!this.level.isClientSide) {
                refreshPortNeighbors(this.level);
            }
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            if (this.getBlockState().getBlock() instanceof FluidBarrelBlock) {
                this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            }
        }
    }

    protected void refreshPortNeighbors(LevelAccessor level) {
        for (Port port : ports(level)) {
            if (level instanceof Level realLevel) {
                realLevel.invalidateCapabilities(port.pos());
            }
            refreshFluidDuctsAround(level, port.connectorPos());
        }
    }

    protected static void refreshFluidDuctsAround(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() instanceof com.reinhardt.hbm.block.FluidDuctBlock duct) {
                duct.refreshConnections(level, neighbor);
            }
        }
    }

    public enum Mode {
        RECEIVE,
        BUFFER,
        SEND,
        DISABLED;

        public boolean canReceive() {
            return this == RECEIVE || this == BUFFER;
        }

        public boolean canSend() {
            return this == SEND || this == BUFFER;
        }

        public static Mode byOrdinal(int ordinal) {
            Mode[] values = values();
            return values[Math.floorMod(ordinal, values.length)];
        }
    }

    public record Port(BlockPos pos, Direction face) {
        public static Port fromLocalConnector(BlockPos corePos, BlockPos connectorOffset, Direction localFace, Direction facing) {
            Direction face = rotate(localFace, facing);
            BlockPos connectorPos = corePos.offset(rotate(connectorOffset, facing));
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }

        private static BlockPos rotate(BlockPos pos, Direction facing) {
            return switch (facing) {
                case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
                case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
                case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
                default -> pos;
            };
        }

        private static Direction rotate(Direction direction, Direction facing) {
            if (direction.getAxis().isVertical()) {
                return direction;
            }
            return switch (facing) {
                case EAST -> direction.getClockWise();
                case SOUTH -> direction.getOpposite();
                case WEST -> direction.getCounterClockWise();
                default -> direction;
            };
        }
    }

    private final class TankHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? tank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? tank.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return tankIndex == 0
                    && mode.canReceive()
                    && tank.isFluidValid(0, stack)
                    && HbmFluids.fromNeoFluid(stack.getFluid()).filter(FluidTankBlockEntity.this::canAcceptFluid).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!mode.canReceive()) {
                return 0;
            }
            if (HbmFluids.fromNeoFluid(resource.getFluid()).filter(FluidTankBlockEntity.this::canAcceptFluid).isEmpty()) {
                return 0;
            }
            FluidStack limited = resource.copy();
            limited.setAmount(Math.min(resource.getAmount(), maxReceiverTransfer()));
            int filled = tank.fill(limited, action);
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!mode.canSend()) {
                return FluidStack.EMPTY;
            }
            FluidStack limited = resource.copy();
            limited.setAmount(Math.min(resource.getAmount(), maxProviderTransfer()));
            FluidStack drained = tank.drain(limited, action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!mode.canSend()) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = tank.drain(Math.min(maxDrain, maxProviderTransfer()), action);
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained;
        }
    }
}
