package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.PwrFuelItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.menu.PwrMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PwrControllerBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 16;
    private static final long CORE_HEAT_CAPACITY_BASE = 10_000_000L;
    private static final long HULL_HEAT_CAPACITY_BASE = 10_000_000L;

    private final net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank coolant = new HbmFluidTank(HbmFluids.byName("coolant").orElse(HbmFluids.none()), 128_000);
    private final HbmFluidTank hotCoolant = new HbmFluidTank(HbmFluids.byName("coolant_hot").orElse(HbmFluids.none()), 128_000);
    private final List<BlockPos> assembledParts = new ArrayList<>();
    private final List<BlockPos> ports = new ArrayList<>();
    private final List<BlockPos> rods = new ArrayList<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> clampInt(coreHeat);
                case 1 -> clampInt(hullHeat);
                case 2 -> clampInt(coreHeatCapacity);
                case 3 -> (int) Math.round(flux);
                case 4 -> (int) Math.round(progress);
                case 5 -> (int) Math.round(processTime);
                case 6 -> typeLoaded;
                case 7 -> amountLoaded;
                case 8 -> rodCount;
                case 9 -> (int) Math.round(rodLevel * 100.0D);
                case 10 -> (int) Math.round(rodTarget * 100.0D);
                case 11 -> coolant.type().oldId();
                case 12 -> coolant.amount();
                case 13 -> hotCoolant.type().oldId();
                case 14 -> hotCoolant.amount();
                case 15 -> assembled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 9) rodLevel = value / 100.0D;
            if (index == 10) rodTarget = value / 100.0D;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private long coreHeat;
    private long coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;
    private long hullHeat;
    private double flux;
    private double rodLevel = 100.0D;
    private double rodTarget = 100.0D;
    private int typeLoaded = -1;
    private int amountLoaded;
    private double progress;
    private double processTime;
    private int rodCount;
    private int connections;
    private int connectionsControlled;
    private int heatexCount;
    private int heatsinkCount;
    private int channelCount;
    private int sourceCount;
    private int unloadDelay;
    private boolean assembled;

    public PwrControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PWR_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PwrControllerBlockEntity reactor) {
        if (level.isClientSide) {
            return;
        }
        reactor.tickServer();
    }

    private void tickServer() {
        setupTanks();
        if (unloadDelay > 0) {
            unloadDelay--;
        }
        if (!assembled) {
            return;
        }
        transferPorts();
        if (unloadDelay > 0) {
            hullHeat = 0;
            coreHeat = 0;
            sync();
            return;
        }
        loadFuel();
        moveControlRods();
        runFission();
        updateCoolant();
        coreHeat = (long) (coreHeat * 0.999D);
        hullHeat = (long) (hullHeat * 0.999D);
        if (coreHeat > coreHeatCapacity) {
            meltDown();
        }
        sync();
    }

    public void assemble() {
        if (level == null || level.isClientSide) {
            return;
        }
        disassemble();
        Direction facing = getBlockState().hasProperty(PwrBlock.FACING) ? getBlockState().getValue(PwrBlock.FACING) : Direction.NORTH;
        BlockPos start = worldPosition.relative(facing);
        Set<BlockPos> found = floodFill(start);
        if (found.isEmpty()) {
            return;
        }

        Map<BlockPos, Block> partMap = new HashMap<>();
        Map<BlockPos, Block> rodMap = new HashMap<>();
        int sourceBlocks = 0;
        for (BlockPos pos : found) {
            BlockState original = level.getBlockState(pos);
            Block block = original.getBlock();
            if (!(block instanceof PwrBlock pwr)) {
                continue;
            }
            partMap.put(pos, block);
            if (pwr.kind() == PwrBlock.Kind.FUEL) {
                rodMap.put(pos, block);
            }
            if (pwr.kind() == PwrBlock.Kind.NEUTRON_SOURCE) {
                sourceBlocks++;
            }
        }
        if (rodMap.isEmpty() || sourceBlocks == 0) {
            return;
        }

        for (BlockPos pos : found) {
            BlockState original = level.getBlockState(pos);
            if (!(original.getBlock() instanceof PwrBlock pwr)) {
                continue;
            }
            BlockState replacement = HbmBlocks.PWR_BLOCK.get().defaultBlockState()
                    .setValue(PwrBlock.PORT, pwr.kind() == PwrBlock.Kind.PORT);
            level.setBlock(pos, replacement, 3);
            if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
                part.bind(original, worldPosition, pwr.kind());
            }
        }
        setup(partMap, rodMap);
        assembled = true;
        setChanged();
    }

    public void disassemble() {
        if (level == null || level.isClientSide || !assembled) {
            return;
        }
        List<BlockPos> restore = new ArrayList<>(assembledParts);
        for (BlockPos pos : new HashSet<>(restore)) {
            if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
                part.restoreOriginal();
            }
        }
        assembled = false;
        assembledParts.clear();
        ports.clear();
        rods.clear();
        setChanged();
    }

    public void invalidateAssemblyOnly() {
        assembled = false;
        setChanged();
    }

    private Set<BlockPos> floodFill(BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!isValidPwrCore(level.getBlockState(start).getBlock()) && !isValidPwrCasing(level.getBlockState(start).getBlock())) {
            return visited;
        }
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < 4096) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            Block block = level.getBlockState(pos).getBlock();
            if (isValidPwrCasing(block)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                Block nextBlock = level.getBlockState(next).getBlock();
                if (!visited.contains(next) && (isValidPwrCore(nextBlock) || isValidPwrCasing(nextBlock))) {
                    queue.add(next);
                }
            }
        }
        visited.remove(worldPosition);
        return visited;
    }

    private static boolean isValidPwrCore(Block block) {
        return isFuelRod(block)
                || block == HbmBlocks.PWR_CONTROL.get()
                || block == HbmBlocks.PWR_CHANNEL.get()
                || block == HbmBlocks.PWR_HEATEX.get()
                || block == HbmBlocks.PWR_HEATSINK.get()
                || block == HbmBlocks.PWR_NEUTRON_SOURCE.get();
    }

    private static boolean isValidPwrCasing(Block block) {
        return block == HbmBlocks.PWR_CASING.get()
                || block == HbmBlocks.PWR_REFLECTOR.get()
                || block == HbmBlocks.PWR_PORT.get();
    }

    private void setup(Map<BlockPos, Block> partMap, Map<BlockPos, Block> rodMap) {
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();
        assembledParts.clear();

        int connectionsDouble = 0;
        int controlledDouble = 0;
        for (Map.Entry<BlockPos, Block> entry : partMap.entrySet()) {
            Block block = entry.getValue();
            assembledParts.add(entry.getKey());
            if (isFuelRod(block)) rodCount++;
            if (block == HbmBlocks.PWR_HEATEX.get()) heatexCount++;
            if (block == HbmBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == HbmBlocks.PWR_HEATSINK.get()) heatsinkCount++;
            if (block == HbmBlocks.PWR_NEUTRON_SOURCE.get()) sourceCount++;
            if (block == HbmBlocks.PWR_PORT.get()) ports.add(entry.getKey());
        }

        for (BlockPos fuelPos : rodMap.keySet()) {
            rods.add(fuelPos);
            for (Direction direction : Direction.values()) {
                boolean controlled = false;
                for (int i = 1; i < 16; i++) {
                    BlockPos check = fuelPos.relative(direction, i);
                    Block at = partMap.get(check);
                    if (at == null || at == HbmBlocks.PWR_CASING.get()) break;
                    if (at == HbmBlocks.PWR_CONTROL.get()) controlled = true;
                    if (isFuelRod(at)) {
                        if (controlled) controlledDouble++; else connectionsDouble++;
                        break;
                    }
                    if (at == HbmBlocks.PWR_REFLECTOR.get()) {
                        if (controlled) controlledDouble += 2; else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }
        connections = connectionsDouble / 2;
        connectionsControlled = controlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);
        coreHeatCapacity = CORE_HEAT_CAPACITY_BASE + heatsinkCount * (CORE_HEAT_CAPACITY_BASE / 20L);
    }

    private static boolean isFuelRod(Block block) {
        return block == HbmBlocks.PWR_FUELROD.get()
                || block == HbmBlocks.PWR_FUEL.get();
    }

    private void loadFuel() {
        ItemStack input = items.get(SLOT_INPUT);
        if ((typeLoaded == -1 || amountLoaded <= 0) && input.getItem() instanceof PwrFuelItem) {
            typeLoaded = PwrFuelItem.fuelIndex(input);
            amountLoaded++;
            input.shrink(1);
            if (input.isEmpty()) items.set(SLOT_INPUT, ItemStack.EMPTY);
        } else if (input.getItem() instanceof PwrFuelItem && PwrFuelItem.fuelIndex(input) == typeLoaded && amountLoaded < rodCount) {
            amountLoaded++;
            input.shrink(1);
            if (input.isEmpty()) items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        if (amountLoaded <= 0) {
            typeLoaded = -1;
        }
        if (amountLoaded > rodCount) {
            amountLoaded = rodCount;
        }
    }

    private void moveControlRods() {
        double diff = rodLevel - rodTarget;
        if (diff < 1.0D && diff > -1.0D) rodLevel = rodTarget;
        if (rodTarget > rodLevel) rodLevel++;
        if (rodTarget < rodLevel) rodLevel--;
    }

    private void runFission() {
        int newFlux = sourceCount * 20;
        if (typeLoaded >= 0 && amountLoaded > 0 && typeLoaded < PwrFuelItem.FUELS.size() && rodCount > 0) {
            PwrFuelItem.Fuel fuel = PwrFuelItem.FUELS.get(typeLoaded);
            double usedRods = getTotalProcessMultiplier();
            double fluxPerRod = flux / rodCount;
            double outputPerRod = fuel.function().output(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * usedRods;
            double totalHeatOutput = totalOutput * fuel.heatEmission();
            coreHeat += (long) totalHeatOutput;
            newFlux += (int) totalOutput;
            processTime = fuel.yield();
            progress += totalOutput;
            if (progress >= processTime) {
                progress -= processTime;
                ItemStack output = items.get(SLOT_OUTPUT);
                if (output.isEmpty()) {
                    items.set(SLOT_OUTPUT, PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL_HOT.get(), typeLoaded));
                } else if (output.getItem() == HbmItems.PWR_FUEL_HOT.get() && PwrFuelItem.fuelIndex(output) == typeLoaded && output.getCount() < output.getMaxStackSize()) {
                    output.grow(1);
                }
                amountLoaded--;
            }
        }
        flux = newFlux;
    }

    private void updateCoolant() {
        double approach = getXOverE((double) heatexCount * 5.0D / (double) Math.max(1, getRodCountForCoolant()), 2.0D) / 2.0D;
        long average = (coreHeat + hullHeat) / 2L;
        coreHeat -= (long) ((coreHeat - average) * approach);
        hullHeat -= (long) ((hullHeat - average) * approach);
        HbmThermalConversions.firstHeatExchangerStep(coolant.type()).ifPresent(step -> {
            double coolingEff = (double) channelCount / (double) Math.max(1, getRodCountForCoolant()) * 0.1D;
            if (coolingEff > 1.0D) coolingEff = 1.0D;
            int heatToUse = (int) Math.min(Math.min(hullHeat, hullHeat * coolingEff), 2_000_000_000L);
            int coolCycles = coolant.amount() / step.amountReq();
            int hotCycles = (hotCoolant.capacity() - hotCoolant.amount()) / step.amountProduced();
            int heatCycles = heatToUse / step.heatReq();
            int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));
            if (cycles > 0) {
                hullHeat -= (long) step.heatReq() * cycles;
                coolant.setAmount(coolant.amount() - step.amountReq() * cycles);
                hotCoolant.fill(step.output(), step.amountProduced() * cycles, false);
            }
        });
    }

    private void setupTanks() {
        if (coolant.amount() <= 0 && coolant.type().isNone()) {
            coolant.setType(HbmFluids.byName("coolant").orElse(HbmFluids.none()));
        }
        HbmThermalConversions.firstHeatExchangerStep(coolant.type()).ifPresentOrElse(
                step -> hotCoolant.setType(step.output()),
                () -> {
                    if (coolant.amount() <= 0) coolant.setType(HbmFluids.none());
                    if (hotCoolant.amount() <= 0) hotCoolant.setType(HbmFluids.none());
                }
        );
    }

    private void transferPorts() {
        for (BlockPos port : ports) {
            for (Direction direction : Direction.values()) {
                BlockPos connector = port.relative(direction);
                if (hotCoolant.amount() > 0) {
                    FluidStack stack = HbmFluids.toNeoStack(hotCoolant.type(), Math.min(1000, hotCoolant.amount()));
                    int filled = HbmFluidNetworks.fillInto(level, connector, direction.getOpposite(), stack, worldPosition, true);
                    if (filled > 0) hotCoolant.drain(hotCoolant.type(), filled, false);
                }
                int space = coolant.capacity() - coolant.amount();
                if (space > 0) {
                    var drained = HbmFluidNetworks.drainFrom(level, connector, direction.getOpposite(), coolant.type(), Math.min(1000, space), worldPosition, true);
                    if (!drained.isEmpty()) coolant.fill(coolant.type(), drained.getAmount(), false);
                }
            }
        }
    }

    private void meltDown() {
        if (level == null) return;
        BlockPos center = rods.isEmpty() ? worldPosition : rods.get(rods.size() / 2);
        for (BlockPos rod : rods) {
            level.setBlock(rod, HbmBlocks.BLOCK_CORIUM.get().defaultBlockState(), 3);
        }
        level.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 15.0F, Level.ExplosionInteraction.BLOCK);
        assembled = false;
    }

    private int getRodCountForCoolant() {
        return rodCount + (int) Math.ceil(heatsinkCount / 4.0D);
    }

    private double getTotalProcessMultiplier() {
        double totalConnections = connections + connectionsControlled * (1.0D - (rodLevel / 100.0D));
        return connectinFunc(totalConnections);
    }

    private double connectinFunc(double connections) {
        return connections / 10.0D * (1.0D - getXOverE(connections, 300.0D)) + connections / 150.0D * getXOverE(connections, 300.0D);
    }

    private double getXOverE(double x, double d) {
        return 1.0D - Math.pow(Math.E, -x / d);
    }

    public boolean assembled() { return assembled; }
    public void setRodTarget(double target) { this.rodTarget = Math.max(0.0D, Math.min(100.0D, target)); sync(); }
    public HbmFluidTank coolantTank() { return coolant; }
    public HbmFluidTank hotCoolantTank() { return hotCoolant; }
    public boolean isPort(BlockPos pos) { return ports.contains(pos); }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int clampInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    public int getContainerSize() { return SLOT_COUNT; }
    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }
    @Override
    public ItemStack removeItem(int slot, int amount) { ItemStack stack = ContainerHelper.removeItem(items, slot, amount); if (!stack.isEmpty()) sync(); return stack; }
    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); sync(); }
    @Override
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override
    public void clearContent() { items.clear(); sync(); }
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) { return slot == SLOT_INPUT && stack.getItem() instanceof PwrFuelItem; }
    @Override
    public int[] getSlotsForFace(Direction side) { return new int[]{SLOT_INPUT, SLOT_OUTPUT}; }
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) { return canPlaceItem(slot, stack); }
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return slot == SLOT_OUTPUT; }
    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.reinhardtshbm.pwr"); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PwrMenu(containerId, inventory, this, menuData, worldPosition);
    }

    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!queriedPos.equals(worldPosition) && !isPort(queriedPos)) {
            return null;
        }
        return new PwrFluidHandler();
    }

    private final class PwrFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? coolant.getFluidInTank(0) : hotCoolant.getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? coolant.capacity() : hotCoolant.capacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && coolant.isFluidValid(0, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return coolant.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return hotCoolant.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return hotCoolant.drain(maxDrain, action); }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("coolant", coolant.save());
        tag.put("hotCoolant", hotCoolant.save());
        tag.putLong("coreHeat", coreHeat);
        tag.putLong("coreHeatCapacity", coreHeatCapacity);
        tag.putLong("hullHeat", hullHeat);
        tag.putDouble("flux", flux);
        tag.putDouble("rodLevel", rodLevel);
        tag.putDouble("rodTarget", rodTarget);
        tag.putInt("typeLoaded", typeLoaded);
        tag.putInt("amountLoaded", amountLoaded);
        tag.putDouble("progress", progress);
        tag.putDouble("processTime", processTime);
        tag.putBoolean("assembled", assembled);
        tag.putInt("rodCount", rodCount);
        tag.putInt("connections", connections);
        tag.putInt("connectionsControlled", connectionsControlled);
        tag.putInt("heatexCount", heatexCount);
        tag.putInt("heatsinkCount", heatsinkCount);
        tag.putInt("channelCount", channelCount);
        tag.putInt("sourceCount", sourceCount);
        tag.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) tag.putLong("p" + i, ports.get(i).asLong());
        tag.putInt("rodPosCount", rods.size());
        for (int i = 0; i < rods.size(); i++) tag.putLong("r" + i, rods.get(i).asLong());
        tag.putInt("assembledPartCount", assembledParts.size());
        for (int i = 0; i < assembledParts.size(); i++) tag.putLong("a" + i, assembledParts.get(i).asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        coolant.load(tag.getCompound("coolant"));
        hotCoolant.load(tag.getCompound("hotCoolant"));
        coreHeat = tag.getLong("coreHeat");
        coreHeatCapacity = Math.max(CORE_HEAT_CAPACITY_BASE, tag.getLong("coreHeatCapacity"));
        hullHeat = tag.getLong("hullHeat");
        flux = tag.getDouble("flux");
        rodLevel = tag.getDouble("rodLevel");
        rodTarget = tag.getDouble("rodTarget");
        typeLoaded = tag.getInt("typeLoaded");
        amountLoaded = tag.getInt("amountLoaded");
        progress = tag.getDouble("progress");
        processTime = tag.getDouble("processTime");
        assembled = tag.getBoolean("assembled");
        rodCount = tag.getInt("rodCount");
        connections = tag.getInt("connections");
        connectionsControlled = tag.getInt("connectionsControlled");
        heatexCount = tag.getInt("heatexCount");
        heatsinkCount = tag.getInt("heatsinkCount");
        channelCount = tag.getInt("channelCount");
        sourceCount = tag.getInt("sourceCount");
        ports.clear();
        for (int i = 0; i < tag.getInt("portCount"); i++) ports.add(BlockPos.of(tag.getLong("p" + i)));
        rods.clear();
        for (int i = 0; i < tag.getInt("rodPosCount"); i++) rods.add(BlockPos.of(tag.getLong("r" + i)));
        assembledParts.clear();
        for (int i = 0; i < tag.getInt("assembledPartCount"); i++) assembledParts.add(BlockPos.of(tag.getLong("a" + i)));
    }
}
