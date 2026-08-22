package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.MiningLaserBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.MiningLaserMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MiningLaserBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_END = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_END = 30;
    public static final int SLOT_COUNT = 30;
    public static final int DATA_COUNT = 10;
    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 10_000;
    public static final int OIL_CAPACITY = 64_000;
    private static final int PUSH_PER_PORT = 16_000;
    private static final int[] INSERT_SLOTS = insertSlots();

    private static final int[] AUTOMATION_SLOTS = outputSlots();
    private static final Set<String> NULLIFIER_BAD_IDS = Set.of(
            "minecraft:dirt",
            "minecraft:stone",
            "minecraft:cobblestone",
            "minecraft:sand",
            "minecraft:sandstone",
            "minecraft:gravel",
            "minecraft:flint",
            "minecraft:snowball",
            "minecraft:wheat_seeds",
            "reinhardtshbm:basalt",
            "reinhardtshbm:stone_gneiss"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank oilTank = new HbmFluidTank(oil(), OIL_CAPACITY);
    private long power;
    private long lastInput;
    private boolean enabled = true;
    private boolean beam;
    private BlockPos target = BlockPos.ZERO;
    private double breakProgress;
    private int currentConsumption = BASE_CONSUMPTION;
    private int range = 1;
    private int completedBlocks;

    public final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) MiningLaserBlockEntity.this.power;
                case 1 -> (int) MiningLaserBlockEntity.this.lastInput;
                case 2 -> MiningLaserBlockEntity.this.enabled ? 1 : 0;
                case 3 -> MiningLaserBlockEntity.this.beam ? 1 : 0;
                case 4 -> (int) Math.round(MiningLaserBlockEntity.this.breakProgress * 1000.0D);
                case 5 -> MiningLaserBlockEntity.this.range;
                case 6 -> MiningLaserBlockEntity.this.currentConsumption;
                case 7 -> MiningLaserBlockEntity.this.oilTank.amount();
                case 8 -> MiningLaserBlockEntity.this.completedBlocks;
                case 9 -> MiningLaserBlockEntity.this.target.getY();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MiningLaserBlockEntity.this.power = value;
                case 1 -> MiningLaserBlockEntity.this.lastInput = value;
                case 2 -> MiningLaserBlockEntity.this.enabled = value != 0;
                case 3 -> MiningLaserBlockEntity.this.beam = value != 0;
                case 4 -> MiningLaserBlockEntity.this.breakProgress = value / 1000.0D;
                case 5 -> MiningLaserBlockEntity.this.range = Math.max(1, value);
                case 6 -> MiningLaserBlockEntity.this.currentConsumption = Math.max(1, value);
                case 7 -> MiningLaserBlockEntity.this.oilTank.setAmount(value);
                case 8 -> MiningLaserBlockEntity.this.completedBlocks = value;
                case 9 -> MiningLaserBlockEntity.this.target = new BlockPos(MiningLaserBlockEntity.this.target.getX(), value, MiningLaserBlockEntity.this.target.getZ());
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MiningLaserBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.MINING_LASER.get(), pos, blockState);
        this.target = pos.below(2);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MiningLaserBlockEntity laser) {
        PowerNetworkManager.tickFromEndpoint(level, laser);
        laser.tickServer(level, state);
    }

    public HbmFluidTank oilTank() {
        return this.oilTank;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
        setChanged();
    }

    public int targetY() {
        return this.target.getY();
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(powerCablePos().immutable());
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(powerCablePos()) && machineSide == Direction.UP;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : Math.min(Math.max(BASE_CONSUMPTION, this.currentConsumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.power.mining_laser",
                this.lastInput,
                this.currentConsumption,
                this.power,
                MAX_POWER,
                this.range,
                this.completedBlocks
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
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
        if (slot == BATTERY_SLOT) {
            return ShredderBlockEntity.isBattery(stack);
        }
        return slot >= UPGRADE_START && slot < UPGRADE_END && MachineUpgradeItem.isMachineUpgrade(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? AUTOMATION_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= OUTPUT_START ? false : canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("OilTank", this.oilTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putBoolean("Enabled", this.enabled);
        tag.putBoolean("Beam", this.beam);
        tag.putInt("TargetX", this.target.getX());
        tag.putInt("TargetY", this.target.getY());
        tag.putInt("TargetZ", this.target.getZ());
        tag.putDouble("BreakProgress", this.breakProgress);
        tag.putInt("CompletedBlocks", this.completedBlocks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.oilTank.load(tag.getCompound("OilTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        this.beam = tag.getBoolean("Beam");
        this.target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        this.breakProgress = tag.getDouble("BreakProgress");
        this.completedBlocks = tag.getInt("CompletedBlocks");
    }

    private void tickServer(Level level, BlockState state) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        updateUpgrades();
        pushOil(level);
        fillAdjacentContainers(level);
        tryEjectOutputs(level);

        boolean shouldRun = this.enabled && !isMultiblockRedstonePowered(level);
        if (!shouldRun || this.power < this.currentConsumption) {
            this.beam = false;
            setLit(level, state, false);
            return;
        }

        if (this.target.getY() <= level.getMinBuildHeight()) {
            this.target = this.worldPosition.below(2);
            this.breakProgress = 0.0D;
        }

        scanForTarget(level);
        if (!this.beam) {
            setLit(level, state, false);
            setChanged();
            return;
        }

        this.power -= this.currentConsumption;
        BlockState targetState = level.getBlockState(this.target);
        float hardness = targetState.getDestroySpeed(level, this.target);
        this.breakProgress += hardness <= 0.0F ? 1.0D : 1.0D / Math.max(1.0D, hardness * 15.0D / speedLevel());

        if (this.breakProgress >= 1.0D && level instanceof ServerLevel serverLevel) {
            breakTarget(serverLevel, targetState);
            buildDam(serverLevel);
            this.breakProgress = 0.0D;
            this.completedBlocks++;
        }

        setLit(level, state, true);
        setChanged();
    }

    private void scanForTarget(Level level) {
        if (canBreak(level, this.target)) {
            this.beam = true;
            return;
        }

        for (int x = -this.range; x <= this.range; x++) {
            for (int z = -this.range; z <= this.range; z++) {
                BlockPos next = new BlockPos(this.worldPosition.getX() + x, this.target.getY(), this.worldPosition.getZ() + z);
                if (canBreak(level, next)) {
                    this.target = next;
                    this.beam = true;
                    return;
                }
            }
        }
        this.target = new BlockPos(this.worldPosition.getX(), this.target.getY() - 1, this.worldPosition.getZ());
        this.beam = false;
    }

    private void breakTarget(ServerLevel level, BlockState targetState) {
        if (!processWithUpgrade(level, targetState)) {
            Block.dropResources(targetState, level, this.target, level.getBlockEntity(this.target));
        }
        level.destroyBlock(this.target, false);
        if (targetState.is(HbmBlocks.ORE_OIL.get())) {
            this.oilTank.fill(oil(), 500, false);
        }
        collectDrops(level);
    }

    private void buildDam(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            placeBags(level, this.target.relative(direction));
        }
    }

    private void placeBags(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) {
            level.setBlock(pos, HbmBlocks.BARRICADE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void collectDrops(ServerLevel level) {
        AABB box = new AABB(this.target).inflate(3.0D, 1.0D, 3.0D);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isRemoved()) {
                continue;
            }
            if (hasUpgrade(MachineUpgradeItem.UpgradeType.NULLIFIER) && isNullifierBad(entity.getItem())) {
                entity.discard();
                continue;
            }
            ItemStack remaining = insertOutput(entity.getItem().copy());
            if (remaining.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remaining);
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box.inflate(-2.0D, 0.0D, -2.0D))) {
            entity.igniteForSeconds(5.0F);
        }
    }

    private ItemStack insertOutput(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            ItemStack current = this.items.get(slot);
            if (ItemStack.isSameItemSameComponents(current, stack)) {
                int moved = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END && !stack.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.items.set(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
        return stack;
    }

    private boolean processWithUpgrade(ServerLevel level, BlockState targetState) {
        ItemStack stack = targetState.getBlock().getCloneItemStack(level, this.target, targetState);
        if (stack.isEmpty()) {
            return false;
        }
        if (hasUpgrade(MachineUpgradeItem.UpgradeType.SMELTER)) {
            return convertDrop(level, net.minecraft.world.item.crafting.RecipeType.SMELTING, stack);
        }
        if (hasUpgrade(MachineUpgradeItem.UpgradeType.SHREDDER)) {
            return convertDrop(level, HbmRecipeTypes.SHREDDER.get(), stack);
        }
        return false;
    }

    private boolean convertDrop(ServerLevel level, net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.SingleRecipeInput>> type, ItemStack input) {
        return level.getRecipeManager()
                .getRecipeFor(type, new net.minecraft.world.item.crafting.SingleRecipeInput(input), level)
                .map(holder -> holder.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(input), level.registryAccess()))
                .filter(result -> !result.isEmpty())
                .map(result -> {
                    level.addFreshEntity(new ItemEntity(level, this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D, result.copy()));
                    return true;
                })
                .orElse(false);
    }

    private boolean canBreak(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        return !state.isAir()
                && !state.liquid()
                && !state.is(Blocks.BEDROCK)
                && hardness >= 0.0F
                && hardness <= 3_500_000.0F;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!isFluidPort(queriedPos)) {
            return null;
        }
        Direction portDirection = directionFromCenter(queriedPos);
        if (side != null && side != portDirection) {
            return null;
        }
        return this.oilTank;
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        return isFluidPort(queriedPos);
    }

    private boolean isFluidPort(BlockPos queriedPos) {
        for (BlockPos port : fluidProxyPorts()) {
            if (port.equals(queriedPos)) {
                return true;
            }
        }
        return false;
    }

    private void pushOil(Level level) {
        if (this.oilTank.amount() <= 0 || this.oilTank.type().isNone()) {
            return;
        }
        for (BlockPos port : fluidCablePorts()) {
            FluidStack stack = HbmFluids.toNeoStack(this.oilTank.type(), Math.min(PUSH_PER_PORT, this.oilTank.amount()));
            int accepted = com.reinhardt.hbm.fluid.HbmFluidNetworks.fillInto(level, port, directionFromCenter(port).getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.oilTank.drain(this.oilTank.type(), accepted, false);
            }
        }
    }

    private void fillAdjacentContainers(Level level) {
        if (this.oilTank.amount() <= 0 || this.oilTank.type().isNone()) {
            return;
        }
        for (BlockPos port : fluidCablePorts()) {
            BlockEntity targetEntity = level.getBlockEntity(port);
            if (targetEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize() && this.oilTank.amount() > 0 && !this.oilTank.type().isNone(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    final int slotIndex = slot;
                    FluidUtil.getFluidHandler(stack).ifPresent(handler -> {
                        FluidStack filled = HbmFluids.toNeoStack(this.oilTank.type(), this.oilTank.amount());
                        int accepted = handler.fill(filled, IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0) {
                            ItemStack result = handler.getContainer();
                            this.oilTank.drain(this.oilTank.type(), accepted, false);
                            container.setItem(slotIndex, result);
                        }
                    });
                }
            }
        }
    }

    private void tryEjectOutputs(Level level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        for (BlockPos port : fluidCablePorts()) {
            Direction side = directionFromCenter(port).getOpposite();
            net.neoforged.neoforge.items.IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, port, side);
            if (handler == null) {
                continue;
            }
            for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
                ItemStack stack = this.items.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
                int moved = stack.getCount() - remaining.getCount();
                if (moved > 0) {
                    stack.shrink(moved);
                    if (stack.isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                    }
                    setChanged();
                }
            }
        }
    }

    private List<BlockPos> fluidProxyPorts() {
        return List.of(
                this.worldPosition.east(),
                this.worldPosition.west(),
                this.worldPosition.south(),
                this.worldPosition.north()
        );
    }

    private List<BlockPos> fluidCablePorts() {
        return List.of(
                this.worldPosition.east(2),
                this.worldPosition.west(2),
                this.worldPosition.south(2),
                this.worldPosition.north(2)
        );
    }

    private Direction directionFromCenter(BlockPos port) {
        int dx = port.getX() - this.worldPosition.getX();
        int dz = port.getZ() - this.worldPosition.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void updateUpgrades() {
        int speed = speedLevel();
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        int overdrive = upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
        this.range = Math.min(25, 1 + upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT) * 2);
        this.currentConsumption = Math.max(1, BASE_CONSUMPTION - BASE_CONSUMPTION * powerLevel / 16 + BASE_CONSUMPTION * speed / 16);
        this.currentConsumption *= 1 + overdrive;
    }

    private boolean isMultiblockRedstonePowered(Level level) {
        for (BlockPos port : fluidProxyPorts()) {
            BlockPos signalPos = port.relative(directionFromCenter(port).getOpposite());
            if (level.hasNeighborSignal(signalPos)) {
                return true;
            }
        }
        return false;
    }

    private int speedLevel() {
        return 1 + upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(1, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return level;
    }

    private boolean hasUpgrade(MachineUpgradeItem.UpgradeType type) {
        return upgradeLevel(type) > 0;
    }

    private static boolean isNullifierBad(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return NULLIFIER_BAD_IDS.contains(id);
    }

    private void setLit(Level level, BlockState state, boolean lit) {
        if (state.getBlock() instanceof MiningLaserBlock && state.getValue(MiningLaserBlock.LIT) != lit) {
            level.setBlock(this.worldPosition, state.setValue(MiningLaserBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private BlockPos powerCablePos() {
        return this.worldPosition.above(2);
    }

    public static List<BlockPos> multiblockPositions(BlockPos corePos) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z).immutable());
                }
            }
        }
        return List.copyOf(positions);
    }

    public Vec3 renderTarget(float partialTick) {
        return Vec3.atCenterOf(this.target);
    }

    public Vec3 renderTargetRaw(float partialTick) {
        return new Vec3(this.target.getX(), this.target.getY(), this.target.getZ());
    }

    public boolean beamActive() {
        return this.beam;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_mining_laser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MiningLaserMenu(containerId, playerInventory, this, this.menuData);
    }

    private static int[] outputSlots() {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] insertSlots() {
        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = BATTERY_SLOT; slot < OUTPUT_START; slot++) {
            slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }
}
