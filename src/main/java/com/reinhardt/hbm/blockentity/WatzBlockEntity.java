package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.menu.WatzMenu;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WatzBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 24;
    public static final int DATA_COUNT = 15;
    public static final int TANK_CAPACITY = 64_000;
    public static final int[] ALL_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7,
            8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23
    };
    public static final int[][] ELEMENT_OFFSETS = {
            {1, 0}, {2, 0}, {0, 1}, {0, 2}, {-1, 0}, {-2, 0}, {0, -1}, {0, -2},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    public static final int[][] COOLER_OFFSETS = {
            {2, 1}, {2, -1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {1, -2}, {-1, -2}
    };
    public static final int[][] CASING_OFFSETS = buildCasingOffsets();
    private static final BlockPos[] TOP_INPUT_PORTS = {
            new BlockPos(0, 2, 0),
            new BlockPos(2, 2, 0),
            new BlockPos(-2, 2, 0),
            new BlockPos(0, 2, 2),
            new BlockPos(0, 2, -2)
    };
    private static final BlockPos[] BOTTOM_OUTPUT_PORTS = {
            new BlockPos(0, 0, 0),
            new BlockPos(2, 0, 0),
            new BlockPos(-2, 0, 0),
            new BlockPos(0, 0, 2),
            new BlockPos(0, 0, -2)
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final NonNullList<ItemStack> locks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank coolant = new HbmFluidTank(fluid("coolant"), TANK_CAPACITY);
    private final HbmFluidTank hotCoolant = new HbmFluidTank(fluid("coolant_hot"), TANK_CAPACITY);
    private final HbmFluidTank mud = new HbmFluidTank(fluid("watz"), TANK_CAPACITY);
    private int heat;
    private double fluxLastBase;
    private double fluxLastReaction;
    private double fluxDisplay;
    private boolean on;
    private boolean locked;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> WatzBlockEntity.this.heat;
                case 1 -> WatzBlockEntity.this.on ? 1 : 0;
                case 2 -> WatzBlockEntity.this.locked ? 1 : 0;
                case 3 -> (int) Math.min(Integer.MAX_VALUE, Math.round(WatzBlockEntity.this.fluxDisplay));
                case 4 -> WatzBlockEntity.this.coolant.type().oldId();
                case 5 -> WatzBlockEntity.this.coolant.amount();
                case 6 -> WatzBlockEntity.this.coolant.capacity();
                case 7 -> WatzBlockEntity.this.hotCoolant.type().oldId();
                case 8 -> WatzBlockEntity.this.hotCoolant.amount();
                case 9 -> WatzBlockEntity.this.hotCoolant.capacity();
                case 10 -> WatzBlockEntity.this.mud.type().oldId();
                case 11 -> WatzBlockEntity.this.mud.amount();
                case 12 -> WatzBlockEntity.this.mud.capacity();
                case 13 -> (int) Math.min(Integer.MAX_VALUE, Math.round(WatzBlockEntity.this.fluxLastBase));
                case 14 -> (int) Math.min(Integer.MAX_VALUE, Math.round(WatzBlockEntity.this.fluxLastReaction));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> WatzBlockEntity.this.heat = value;
                case 1 -> WatzBlockEntity.this.on = value != 0;
                case 2 -> WatzBlockEntity.this.locked = value != 0;
                case 3 -> WatzBlockEntity.this.fluxDisplay = value;
                case 5 -> WatzBlockEntity.this.coolant.setAmount(value);
                case 8 -> WatzBlockEntity.this.hotCoolant.setAmount(value);
                case 11 -> WatzBlockEntity.this.mud.setAmount(value);
                case 13 -> WatzBlockEntity.this.fluxLastBase = value;
                case 14 -> WatzBlockEntity.this.fluxLastReaction = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public WatzBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WATZ.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WatzBlockEntity watz) {
        watz.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank coolantTank() {
        return this.coolant;
    }

    public HbmFluidTank hotCoolantTank() {
        return this.hotCoolant;
    }

    public HbmFluidTank mudTank() {
        return this.mud;
    }

    public int heat() {
        return this.heat;
    }

    public double fluxDisplay() {
        return this.fluxDisplay;
    }

    public boolean isOn() {
        return this.on;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void toggleLock() {
        if (this.locked) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                this.locks.set(i, ItemStack.EMPTY);
            }
        } else {
            for (int i = 0; i < SLOT_COUNT; i++) {
                this.locks.set(i, this.items.get(i).copy());
            }
        }
        this.locked = !this.locked;
        sync();
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (queriedPos.equals(this.worldPosition)) {
            if (side == null) {
                return new WatzFluidHandler(Port.GUI);
            }
        }
        if (isTopPort(queriedPos) && (side == null || side == Direction.UP)) {
            return new WatzFluidHandler(Port.INPUT);
        }
        if (isBottomPort(queriedPos) && (side == null || side == Direction.DOWN)) {
            return new WatzFluidHandler(Port.OUTPUT);
        }
        return null;
    }

    private void tickServer(Level level) {
        if (updateLock()) {
            return;
        }
        List<WatzBlockEntity> segments = collectSegments(level);
        SharedTanks shared = new SharedTanks();
        for (WatzBlockEntity segment : segments) {
            segment.setupCoolant();
            shared.add(segment);
        }
        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).updateCoolant(shared);
        }
        boolean turnedOn = hasPumpAndRedstone(level);
        segments.get(0).updateReaction(null, shared, turnedOn);
        for (int i = 1; i < segments.size(); i++) {
            segments.get(i).updateReaction(segments.get(i - 1), shared, turnedOn);
        }
        for (WatzBlockEntity segment : segments) {
            segment.on = turnedOn;
            segment.fluxDisplay = segment.fluxLastBase + segment.fluxLastReaction;
            segment.heat = (int) (segment.heat * 0.99D);
            segment.sync();
        }
        for (int i = segments.size() - 1; i >= 0; i--) {
            shared.distributeInto(segments.get(i));
        }
        segments.get(segments.size() - 1).sendOutBottom();
        if (shared.mud > 0) {
            overflowBoom(level);
        }
    }

    private boolean updateLock() {
        return this.level != null && this.level.getBlockEntity(this.worldPosition.above(3)) instanceof WatzBlockEntity;
    }

    private List<WatzBlockEntity> collectSegments(Level level) {
        ArrayList<WatzBlockEntity> segments = new ArrayList<>();
        segments.add(this);
        for (int y = this.worldPosition.getY() - 3; y >= level.getMinBuildHeight(); y -= 3) {
            BlockEntity blockEntity = level.getBlockEntity(new BlockPos(this.worldPosition.getX(), y, this.worldPosition.getZ()));
            if (blockEntity instanceof WatzBlockEntity segment) {
                segments.add(segment);
            } else {
                break;
            }
        }
        return segments;
    }

    private boolean hasPumpAndRedstone(Level level) {
        BlockPos pumpPos = this.worldPosition.above(3);
        return level.getBlockState(pumpPos).is(HbmBlocks.WATZ_PUMP.get())
                && level.hasNeighborSignal(this.worldPosition.above(5));
    }

    private void setupCoolant() {
        this.coolant.conform(fluid("coolant"), 0);
        this.hotCoolant.conform(fluid("coolant_hot"), 0);
        this.mud.conform(fluid("watz"), 0);
    }

    private void updateCoolant(SharedTanks shared) {
        HbmThermalConversions.HeatingStep step = HbmThermalConversions.firstHeatExchangerStep(fluid("coolant")).orElse(null);
        if (step == null) {
            return;
        }
        double heatToUse = this.heat * 0.2D;
        int heatCycles = (int) (heatToUse / step.heatReq());
        int coolCycles = shared.coolant / step.amountReq();
        int hotCycles = (shared.hotCapacity - shared.hotCoolant) / step.amountProduced();
        int cycles = Math.min(heatCycles, Math.min(coolCycles, hotCycles));
        if (cycles <= 0) {
            return;
        }
        this.heat -= cycles * step.heatReq();
        shared.coolant -= cycles * step.amountReq();
        shared.hotCoolant += cycles * step.amountProduced();
    }

    private void updateReaction(@Nullable WatzBlockEntity above, SharedTanks shared, boolean turnedOn) {
        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();
            for (ItemStack stack : this.items) {
                if (WatzPelletItem.isActivePellet(stack)) {
                    pellets.add(stack);
                }
            }
            double baseFlux = 0.0D;
            for (ItemStack stack : pellets) {
                baseFlux += WatzPelletItem.type(stack).passive;
            }
            double inputFlux = baseFlux + this.fluxLastReaction;
            double addedFlux = 0.0D;
            double addedHeat = 0.0D;
            for (ItemStack stack : pellets) {
                WatzPelletItem.Type type = WatzPelletItem.type(stack);
                if (type.burn == null) {
                    continue;
                }
                double div = type.heatDiv != null ? type.heatDiv.apply(this.heat) : 1.0D;
                double burn = type.burn.apply(inputFlux) / Math.max(0.000001D, div);
                WatzPelletItem.setYield(stack, WatzPelletItem.yield(stack) - burn);
                addedFlux += burn;
                addedHeat += type.heatEmission * burn;
                shared.mud += (int) Math.round(type.mudContent * burn);
            }
            for (ItemStack stack : pellets) {
                WatzPelletItem.Type type = WatzPelletItem.type(stack);
                if (type.absorb == null) {
                    continue;
                }
                double absorb = type.absorb.apply(baseFlux + this.fluxLastReaction);
                addedHeat += absorb;
                WatzPelletItem.setYield(stack, WatzPelletItem.yield(stack) - absorb);
                shared.mud += (int) Math.round(type.mudContent * absorb);
            }
            this.heat += (int) Math.round(addedHeat);
            this.fluxLastBase = baseFlux;
            this.fluxLastReaction = addedFlux;
        } else {
            this.fluxLastBase = 0.0D;
            this.fluxLastReaction = 0.0D;
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = this.items.get(i);
            if (WatzPelletItem.isActivePellet(stack) && WatzPelletItem.enrichment(stack) <= 0.0D) {
                WatzPelletItem.Type type = WatzPelletItem.type(stack);
                this.items.set(i, WatzPelletItem.stack(type, HbmItems.WATZ_PELLET_DEPLETED.get()));
            }
        }

        if (above != null) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack bottom = this.items.get(i);
                ItemStack top = above.items.get(i);
                if (bottom.isEmpty() && !top.isEmpty()) {
                    this.items.set(i, top.copy());
                    above.items.set(i, ItemStack.EMPTY);
                } else if (WatzPelletItem.isActivePellet(bottom) && WatzPelletItem.isDepletedPellet(top)) {
                    this.items.set(i, top.copy());
                    above.items.set(i, bottom.copy());
                }
            }
            above.setChanged();
        }
        setChanged();
    }

    private void sendOutBottom() {
        if (this.level == null) {
            return;
        }
        for (BlockPos offset : BOTTOM_OUTPUT_PORTS) {
            BlockPos port = this.worldPosition.offset(offset);
            pushTank(this.hotCoolant, port);
            pushTank(this.mud, port);
        }
    }

    private void pushTank(HbmFluidTank tank, BlockPos port) {
        if (tank.amount() <= 0 || tank.type().isNone() || this.level == null) {
            return;
        }
        BlockPos target = port.below();
        FluidStack offered = HbmFluids.toNeoStack(tank.type(), tank.amount());
        int filled = HbmFluidNetworks.fillInto(this.level, target, Direction.UP, offered, port, true);
        if (filled > 0) {
            tank.drain(tank.type(), filled, false);
        }
    }

    private void overflowBoom(Level level) {
        for (int x = -3; x <= 3; x++) {
            for (int y = 3; y < 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    level.setBlock(this.worldPosition.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        disassemble(level);
        if (level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition.above(), 1_000.0D);
            HbmAdvancements.awardNearby(serverLevel, new AABB(
                    this.worldPosition.getX() - 50.0D, this.worldPosition.getY() - 50.0D, this.worldPosition.getZ() - 50.0D,
                    this.worldPosition.getX() + 51.0D, this.worldPosition.getY() + 51.0D, this.worldPosition.getZ() + 51.0D
            ), "watz_boom");
            serverLevel.sendParticles(
                    HbmParticleTypes.RBMK_MUSH.get(),
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 2.0D,
                    this.worldPosition.getZ() + 0.5D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    5.0D
            );
        }
        level.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 2.0D, this.worldPosition.getZ() + 0.5D,
                HbmSoundEvents.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
    }

    private void disassemble(Level level) {
        level.setBlock(this.worldPosition, HbmBlocks.MUD_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(this.worldPosition.above(), HbmBlocks.MUD_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(this.worldPosition.above(2), HbmBlocks.MUD_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int[] offset : ELEMENT_OFFSETS) {
            setBrokenColumn(level, 0, HbmBlocks.WATZ_ELEMENT.get().defaultBlockState(), offset[0], offset[1]);
        }
        for (int[] offset : COOLER_OFFSETS) {
            setBrokenColumn(level, 0, HbmBlocks.WATZ_COOLER.get().defaultBlockState(), offset[0], offset[1]);
        }
        for (int[] offset : CASING_OFFSETS) {
            setBrokenColumn(level, 1, HbmBlocks.WATZ_END.get().defaultBlockState(), offset[0], offset[1]);
        }
    }

    private void setBrokenColumn(Level level, int minHeight, BlockState block, int x, int z) {
        int height = minHeight + level.random.nextInt(3 - minHeight);
        for (int y = 0; y < 3; y++) {
            level.setBlock(this.worldPosition.offset(x, y, z),
                    y <= height ? block : HbmBlocks.MUD_BLOCK.get().defaultBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    public static void fillDummies(Level level, BlockPos core) {
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (int[] offset : allStructureOffsets()) {
                BlockPos pos = core.offset(offset[0], offset[1], offset[2]);
                if (pos.equals(core)) {
                    continue;
                }
                setDummy(level, pos, core);
            }
        });
    }

    public static void removeDummies(Level level, BlockPos core) {
        if (level.getBlockEntity(core) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (int[] offset : allStructureOffsets()) {
                BlockPos pos = core.offset(offset[0], offset[1], offset[2]);
                removeDummy(level, pos, core);
            }
        });
    }

    public static List<BlockPos> occupiedPositions(BlockPos core) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        positions.add(core);
        for (int[] offset : allStructureOffsets()) {
            positions.add(core.offset(offset[0], offset[1], offset[2]));
        }
        return List.copyOf(positions);
    }

    private static void setDummy(Level level, BlockPos pos, BlockPos core) {
        if (level.getBlockState(pos).is(HbmBlocks.WATZ_PUMP.get())) {
            return;
        }
        level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            dummy.setCorePos(core);
        }
    }

    private static void removeDummy(Level level, BlockPos pos, BlockPos core) {
        if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.getCorePos().equals(core)) {
            level.removeBlock(pos, false);
        }
    }

    private boolean isTopPort(BlockPos pos) {
        return matchesOffset(pos, TOP_INPUT_PORTS);
    }

    private boolean isBottomPort(BlockPos pos) {
        return matchesOffset(pos, BOTTOM_OUTPUT_PORTS);
    }

    private boolean matchesOffset(BlockPos pos, BlockPos[] offsets) {
        for (BlockPos offset : offsets) {
            if (this.worldPosition.offset(offset).equals(pos)) {
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
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return validSlot(slot) ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!validSlot(slot) || !WatzPelletItem.isActivePellet(stack)) {
            return false;
        }
        if (!this.locked) {
            return true;
        }
        ItemStack lock = this.locks.get(slot);
        return WatzPelletItem.isActivePellet(lock) && WatzPelletItem.type(lock) == WatzPelletItem.type(stack);
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
        return !WatzPelletItem.isActivePellet(stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.watz");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WatzMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        this.items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        CompoundTag locksTag = new CompoundTag();
        ContainerHelper.saveAllItems(locksTag, this.locks, registries);
        tag.put("locks", locksTag);
        tag.put("coolant", this.coolant.save());
        tag.put("hotCoolant", this.hotCoolant.save());
        tag.put("mud", this.mud.save());
        tag.putInt("heat", this.heat);
        tag.putDouble("lastFluxB", this.fluxLastBase);
        tag.putDouble("lastFluxR", this.fluxLastReaction);
        tag.putBoolean("isLocked", this.locked);
        tag.putBoolean("isOn", this.on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        if (tag.contains("locks")) {
            ContainerHelper.loadAllItems(tag.getCompound("locks"), this.locks, registries);
        }
        this.coolant.load(tag.getCompound("coolant"));
        this.hotCoolant.load(tag.getCompound("hotCoolant"));
        this.mud.load(tag.getCompound("mud"));
        this.heat = tag.getInt("heat");
        this.fluxLastBase = tag.getDouble("lastFluxB");
        this.fluxLastReaction = tag.getDouble("lastFluxR");
        this.locked = tag.getBoolean("isLocked");
        this.on = tag.getBoolean("isOn");
        this.fluxDisplay = this.fluxLastBase + this.fluxLastReaction;
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

    public void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private static int[][] buildCasingOffsets() {
        ArrayList<int[]> offsets = new ArrayList<>();
        for (int j = -1; j < 2; j++) {
            offsets.add(new int[]{3, j});
            offsets.add(new int[]{j, 3});
            offsets.add(new int[]{-3, j});
            offsets.add(new int[]{j, -3});
        }
        offsets.add(new int[]{2, 2});
        offsets.add(new int[]{2, -2});
        offsets.add(new int[]{-2, 2});
        offsets.add(new int[]{-2, -2});
        return offsets.toArray(int[][]::new);
    }

    private static int[][] allStructureOffsets() {
        ArrayList<int[]> offsets = new ArrayList<>();
        offsets.add(new int[]{0, 1, 0});
        offsets.add(new int[]{0, 2, 0});
        for (int y = 0; y < 3; y++) {
            for (int[] offset : ELEMENT_OFFSETS) {
                offsets.add(new int[]{offset[0], y, offset[1]});
            }
            for (int[] offset : COOLER_OFFSETS) {
                offsets.add(new int[]{offset[0], y, offset[1]});
            }
            for (int[] offset : CASING_OFFSETS) {
                offsets.add(new int[]{offset[0], y, offset[1]});
            }
        }
        return offsets.toArray(int[][]::new);
    }

    private final class WatzFluidHandler implements IFluidHandler {
        private final Port port;

        private WatzFluidHandler(Port port) {
            this.port = port;
        }

        @Override
        public int getTanks() {
            return this.port == Port.GUI ? 3 : this.port == Port.OUTPUT ? 2 : 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tankFor(tank)) {
                case 0 -> WatzBlockEntity.this.coolant.getFluidInTank(0);
                case 1 -> WatzBlockEntity.this.hotCoolant.getFluidInTank(0);
                case 2 -> WatzBlockEntity.this.mud.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tankFor(tank)) {
                case 0 -> WatzBlockEntity.this.coolant.capacity();
                case 1 -> WatzBlockEntity.this.hotCoolant.capacity();
                case 2 -> WatzBlockEntity.this.mud.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            HbmFluidDefinition def = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return this.port != Port.OUTPUT && tankFor(tank) == 0 && def.name().equals("coolant");
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (this.port == Port.OUTPUT || resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition def = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!def.name().equals("coolant")) {
                return 0;
            }
            return WatzBlockEntity.this.coolant.fill(def, resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (this.port == Port.INPUT || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition def = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidStack drained;
            if (def.name().equals("coolant_hot")) {
                drained = WatzBlockEntity.this.hotCoolant.drain(def, resource.getAmount(), action.simulate());
            } else if (def.name().equals("watz")) {
                drained = WatzBlockEntity.this.mud.drain(def, resource.getAmount(), action.simulate());
            } else {
                return FluidStack.EMPTY;
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (this.port == Port.INPUT) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack hot = WatzBlockEntity.this.hotCoolant.drain(null, maxDrain, action.simulate());
            if (!hot.isEmpty()) {
                return HbmFluids.toNeoStack(hot.type(), hot.amount());
            }
            HbmFluidStack mud = WatzBlockEntity.this.mud.drain(null, maxDrain, action.simulate());
            return mud.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(mud.type(), mud.amount());
        }

        private int tankFor(int exposedTank) {
            return switch (this.port) {
                case GUI -> exposedTank;
                case INPUT -> exposedTank == 0 ? 0 : -1;
                case OUTPUT -> exposedTank == 0 ? 1 : exposedTank == 1 ? 2 : -1;
            };
        }
    }

    private enum Port {
        GUI,
        INPUT,
        OUTPUT
    }

    private static final class SharedTanks {
        private int coolant;
        private int hotCoolant;
        private int mud;
        private int coolantCapacity;
        private int hotCapacity;
        private int mudCapacity;

        private void add(WatzBlockEntity segment) {
            this.coolant += segment.coolant.amount();
            this.hotCoolant += segment.hotCoolant.amount();
            this.mud += segment.mud.amount();
            this.coolantCapacity += segment.coolant.capacity();
            this.hotCapacity += segment.hotCoolant.capacity();
            this.mudCapacity += segment.mud.capacity();
        }

        private void distributeInto(WatzBlockEntity segment) {
            int fillCoolant = Math.min(segment.coolant.capacity(), this.coolant);
            segment.coolant.setAmount(fillCoolant);
            this.coolant -= fillCoolant;
            int fillHot = Math.min(segment.hotCoolant.capacity(), this.hotCoolant);
            segment.hotCoolant.setAmount(fillHot);
            this.hotCoolant -= fillHot;
            int fillMud = Math.min(segment.mud.capacity(), this.mud);
            segment.mud.setAmount(fillMud);
            this.mud -= fillMud;
            this.coolant = Math.max(0, this.coolant);
            this.hotCoolant = Math.max(0, this.hotCoolant);
            this.mud = Math.max(0, this.mud);
        }
    }
}
