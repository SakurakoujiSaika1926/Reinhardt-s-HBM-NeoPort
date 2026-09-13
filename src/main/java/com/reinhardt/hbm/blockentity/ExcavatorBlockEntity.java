package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BedrockOreBaseItem;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ExcavatorMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ShredderRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.item.BatteryPackItem;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcavatorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int FLUID_ID_SLOT = 1;
    public static final int UPGRADE_START = 2;
    public static final int UPGRADE_END = 4;
    public static final int DRILLBIT_SLOT = 4;
    public static final int OUTPUT_START = 5;
    public static final int OUTPUT_END = 14;
    public static final int SLOT_COUNT = 14;
    public static final int DATA_COUNT = 13;
    public static final long MAX_POWER = 1_000_000L;
    public static final long BASE_CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 16_000;

    private static final int[] INSERT_SLOTS = {BATTERY_SLOT, FLUID_ID_SLOT, UPGRADE_START, UPGRADE_START + 1, DRILLBIT_SLOT};
    private static final int[] OUTPUT_SLOTS = outputSlots();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(TANK_CAPACITY);
    private long power;
    private long lastInput;
    private long consumption = BASE_CONSUMPTION;
    private boolean enableDrill;
    private boolean enableCrusher;
    private boolean enableWalling;
    private boolean enableVeinMiner;
    private boolean enableSilkTouch;
    private boolean operational;
    private int ticksWorked;
    private int targetDepth;
    private boolean bedrockDrilling;
    private int chuteTimer;
    private double speed = 1.0D;
    private float drillRotation;
    private float prevDrillRotation;
    private float drillExtension;
    private float prevDrillExtension;
    private float crusherRotation;
    private float prevCrusherRotation;

    public final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) ExcavatorBlockEntity.this.power;
                case 1 -> (int) ExcavatorBlockEntity.this.lastInput;
                case 2 -> ExcavatorBlockEntity.this.enableDrill ? 1 : 0;
                case 3 -> ExcavatorBlockEntity.this.enableCrusher ? 1 : 0;
                case 4 -> ExcavatorBlockEntity.this.enableWalling ? 1 : 0;
                case 5 -> ExcavatorBlockEntity.this.enableVeinMiner ? 1 : 0;
                case 6 -> ExcavatorBlockEntity.this.enableSilkTouch ? 1 : 0;
                case 7 -> ExcavatorBlockEntity.this.operational ? 1 : 0;
                case 8 -> ExcavatorBlockEntity.this.targetDepth;
                case 9 -> ExcavatorBlockEntity.this.chuteTimer;
                case 10 -> (int) ExcavatorBlockEntity.this.consumption;
                case 11 -> ExcavatorBlockEntity.this.tank.amount();
                case 12 -> ExcavatorBlockEntity.this.tank.type().oldId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ExcavatorBlockEntity.this.power = value;
                case 1 -> ExcavatorBlockEntity.this.lastInput = value;
                case 2 -> ExcavatorBlockEntity.this.enableDrill = value != 0;
                case 3 -> ExcavatorBlockEntity.this.enableCrusher = value != 0;
                case 4 -> ExcavatorBlockEntity.this.enableWalling = value != 0;
                case 5 -> ExcavatorBlockEntity.this.enableVeinMiner = value != 0;
                case 6 -> ExcavatorBlockEntity.this.enableSilkTouch = value != 0;
                case 7 -> ExcavatorBlockEntity.this.operational = value != 0;
                case 8 -> ExcavatorBlockEntity.this.targetDepth = value;
                case 9 -> ExcavatorBlockEntity.this.chuteTimer = value;
                case 10 -> ExcavatorBlockEntity.this.consumption = value;
                case 11 -> ExcavatorBlockEntity.this.tank.setAmount(value);
                case 12 -> ExcavatorBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ExcavatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.EXCAVATOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExcavatorBlockEntity excavator) {
        if (level.isClientSide) {
            excavator.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, excavator);
        excavator.tickServer();
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queryPos, @Nullable Direction side) {
        return isAutomationPort(queryPos) ? this.tank : null;
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public float drillRotation(float partialTick) {
        return this.prevDrillRotation + (this.drillRotation - this.prevDrillRotation) * partialTick;
    }

    public float crusherRotation(float partialTick) {
        return this.prevCrusherRotation + (this.crusherRotation - this.prevCrusherRotation) * partialTick;
    }

    public float drillExtension(float partialTick) {
        return this.prevDrillExtension + (this.drillExtension - this.prevDrillExtension) * partialTick;
    }

    public boolean crusherEnabled() {
        return this.enableCrusher;
    }

    public int chuteTimer() {
        return this.chuteTimer;
    }

    public boolean canVeinMine() {
        DrillbitItem.DrillType type = installedDrill();
        return this.enableVeinMiner && type != null && type.vein();
    }

    public boolean canSilkTouch() {
        DrillbitItem.DrillType type = installedDrill();
        return this.enableSilkTouch && type != null && type.silk();
    }

    public boolean hasInstalledDrill() {
        return installedDrill() != null;
    }

    public void toggle(int id) {
        switch (id) {
            case 0 -> this.enableDrill = !this.enableDrill;
            case 1 -> this.enableCrusher = !this.enableCrusher;
            case 2 -> this.enableWalling = !this.enableWalling;
            case 3 -> this.enableVeinMiner = !this.enableVeinMiner;
            case 4 -> this.enableSilkTouch = !this.enableSilkTouch;
            default -> {
            }
        }
        setChanged();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions().stream().map(Port::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (Port port : connectorPositions()) {
            if (port.pos().equals(connectorPos)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAutomationPort(BlockPos queryPos) {
        for (Port port : proxyPositions()) {
            if (port.pos().equals(queryPos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : Math.min(Math.max(BASE_CONSUMPTION, this.consumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.excavator", this.lastInput, this.consumption, this.power, MAX_POWER, this.targetDepth);
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
        if (slot == FLUID_ID_SLOT) {
            return stack.getItem() instanceof FluidIdentifierItem;
        }
        if (slot == DRILLBIT_SLOT) {
            return stack.getItem() instanceof DrillbitItem;
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            return validUpgrade(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.replaceAll(ignored -> ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
            }
        }
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_excavator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ExcavatorMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putBoolean("Drill", this.enableDrill);
        tag.putBoolean("Crusher", this.enableCrusher);
        tag.putBoolean("Walling", this.enableWalling);
        tag.putBoolean("VeinMiner", this.enableVeinMiner);
        tag.putBoolean("SilkTouch", this.enableSilkTouch);
        tag.putBoolean("Operational", this.operational);
        tag.putInt("TicksWorked", this.ticksWorked);
        tag.putInt("TargetDepth", this.targetDepth);
        tag.putInt("ChuteTimer", this.chuteTimer);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("Consumption", this.consumption);
        tag.put("Tank", this.tank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.enableDrill = tag.getBoolean("Drill");
        this.enableCrusher = tag.getBoolean("Crusher");
        this.enableWalling = tag.getBoolean("Walling");
        this.enableVeinMiner = tag.getBoolean("VeinMiner");
        this.enableSilkTouch = tag.getBoolean("SilkTouch");
        this.operational = tag.getBoolean("Operational");
        this.ticksWorked = tag.getInt("TicksWorked");
        this.targetDepth = tag.getInt("TargetDepth");
        this.chuteTimer = tag.getInt("ChuteTimer");
        this.lastInput = tag.getLong("LastInput");
        this.consumption = tag.contains("Consumption") ? tag.getLong("Consumption") : BASE_CONSUMPTION;
        this.tank.load(tag.getCompound("Tank"));
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

    private void tickServer() {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyFluidIdentifier();
        fillTankFromContainers();
        updateConsumption();
        if (this.chuteTimer > 0) {
            this.chuteTimer--;
        }
        if (this.level != null && this.level.getGameTime() % 20L == 0L) {
            tryEjectBuffer();
        }

        boolean wasOperational = this.operational;
        int previousDepth = this.targetDepth;
        int previousChuteTimer = this.chuteTimer;
        this.operational = false;
        DrillbitItem.DrillType type = installedDrill();
        if (this.enableDrill && type != null && this.power >= this.consumption) {
            this.operational = true;
            this.power -= this.consumption;
            this.speed = type.speed() * (1.0D + upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED) / 2.0D);
            int maxDepth = this.worldPosition.getY() - this.level.getMinBuildHeight() - 4;
            if ((this.bedrockDrilling || this.targetDepth <= maxDepth) && tryDrill(1 + upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT) * 2)) {
                this.targetDepth++;
                if (this.targetDepth > maxDepth) {
                    this.enableDrill = false;
                }
            }
        } else {
            this.targetDepth = 0;
        }
        setChanged();
        if (this.level != null && (this.level.getGameTime() % 10L == 0L
                || wasOperational != this.operational
                || previousDepth != this.targetDepth
                || previousChuteTimer != this.chuteTimer)) {
            sync();
        }
    }

    private void tickClient() {
        this.prevDrillExtension = this.drillExtension;
        if (this.drillExtension != this.targetDepth) {
            float diff = Math.abs(this.drillExtension - this.targetDepth);
            float move = Math.max(0.15F, diff / 10.0F);
            this.drillExtension = diff <= move ? this.targetDepth : this.drillExtension - Math.signum(this.drillExtension - this.targetDepth) * move;
        }
        this.prevDrillRotation = this.drillRotation;
        this.prevCrusherRotation = this.crusherRotation;
        if (this.operational) {
            this.drillRotation += 15.0F;
            if (this.enableCrusher) {
                this.crusherRotation += 15.0F;
            }
        }
        if (this.drillRotation >= 360.0F) {
            this.drillRotation -= 360.0F;
            this.prevDrillRotation -= 360.0F;
        }
        if (this.crusherRotation >= 360.0F) {
            this.crusherRotation -= 360.0F;
            this.prevCrusherRotation -= 360.0F;
        }
    }

    private int drillY() {
        return this.worldPosition.getY() - this.targetDepth - 4;
    }

    private boolean tryDrill(int radius) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        int y = drillY();
        if (this.targetDepth == 0 || y <= serverLevel.getMinBuildHeight()) {
            radius = 1;
        }
        for (int ring = 1; ring <= radius; ring++) {
            boolean ignoreAll = true;
            float combinedHardness = 0.0F;
            BlockPos bedrockOre = null;
            this.bedrockDrilling = false;
            for (BlockPos pos : ringPositions(ring, y)) {
                BlockState state = serverLevel.getBlockState(pos);
                if (state.is(HbmBlocks.ORE_BEDROCK_BLOCK.get()) || state.is(HbmBlocks.ORE_BEDROCK_COLTAN.get()) || state.is(HbmBlocks.ORE_BEDROCK_OIL.get())) {
                    combinedHardness = 5.0F * 60.0F * 20.0F;
                    bedrockOre = pos;
                    this.bedrockDrilling = true;
                    this.enableCrusher = false;
                    ignoreAll = false;
                    break;
                }
                if (state.is(HbmBlocks.STONE_DEPTH.get()) || state.is(HbmBlocks.STONE_DEPTH_NETHER.get())) {
                    this.enableDrill = false;
                }
                if (shouldIgnoreBlock(state, pos)) {
                    continue;
                }
                ignoreAll = false;
                combinedHardness += Math.max(0.0F, state.getDestroySpeed(serverLevel, pos));
            }

            if (!ignoreAll) {
                this.ticksWorked++;
                int ticksToWork = (int) Math.ceil(combinedHardness / this.speed);
                if (this.ticksWorked >= ticksToWork) {
                    if (bedrockOre == null) {
                        breakBlocks(ring);
                        buildWall(ring + 1, ring == radius && this.enableWalling);
                        if (ring == radius) {
                            mineOuterOres(ring + 1);
                        }
                        tryCollect(radius + 1);
                    } else {
                        collectBedrock(bedrockOre);
                    }
                    this.ticksWorked = 0;
                }
                return false;
            }
            tryCollect(radius + 1);
        }
        buildWall(radius + 1, this.enableWalling);
        this.ticksWorked = 0;
        return true;
    }

    private void collectBedrock(BlockPos pos) {
        if (!(this.level instanceof ServerLevel serverLevel) || !(serverLevel.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) {
            return;
        }
        DrillbitItem.DrillType drill = installedDrill();
        if (drill == null || ore.resource().isEmpty() || ore.tier() > drill.tier()) {
            return;
        }
        HbmFluidStack acid = ore.acidRequirement();
        if (!acid.isEmpty()) {
            if (acid.type() != this.tank.type() || acid.amount() > this.tank.amount()) {
                return;
            }
            this.tank.drain(acid.type(), acid.amount(), false);
        }
        ItemStack stack = ore.resource().copy();
        if (stack.is(HbmItems.BEDROCK_ORE_BASE.get())) {
            BedrockOreBaseItem.setOreAmount(stack, pos.getX(), pos.getZ(), 1.0D + drill.fortune() * 0.1D);
        }
        insertOrBuffer(stack);
    }

    private void breakBlocks(int ring) {
        int y = drillY();
        for (BlockPos pos : ringPositions(ring, y)) {
            BlockState state = this.level.getBlockState(pos);
            if (!shouldIgnoreBlock(state, pos)) {
                tryMineAtLocation(pos, state);
            }
        }
    }

    private void tryMineAtLocation(BlockPos pos, BlockState state) {
        if (this.enableVeinMiner && canVeinMine() && isOre(state)) {
            Set<BlockPos> visited = new HashSet<>();
            List<BlockPos> mined = new ArrayList<>();
            breakRecursively(pos, state.getBlock(), 10, visited, mined);
            for (ItemEntity item : this.level.getEntitiesOfClass(ItemEntity.class, boundsFor(mined))) {
                item.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            }
            return;
        }
        breakSingleBlock(pos, state);
    }

    private void breakRecursively(BlockPos pos, Block block, int depth, Set<BlockPos> visited, List<BlockPos> mined) {
        if (depth < 0 || !visited.add(pos) || this.level == null || this.level.getBlockState(pos).getBlock() != block) {
            return;
        }
        for (Direction direction : Direction.values()) {
            breakRecursively(pos.relative(direction), block, depth - 1, visited, mined);
        }
        breakSingleBlock(pos, this.level.getBlockState(pos));
        mined.add(pos);
        if (this.enableWalling) {
            this.level.setBlock(pos, HbmBlocks.BARRICADE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void breakSingleBlock(BlockPos pos, BlockState state) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<ItemStack> drops;
        if (canSilkTouch()) {
            drops = List.of(state.getBlock().getCloneItemStack(serverLevel, pos, state));
        } else {
            drops = Block.getDrops(state, serverLevel, pos, serverLevel.getBlockEntity(pos), null, ItemStack.EMPTY);
        }
        if (state.is(HbmBlocks.BARRICADE.get())) {
            drops = List.of();
        }
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack result = this.enableCrusher ? crushed(drop) : drop.copy();
            serverLevel.addFreshEntity(new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, result));
        }
        serverLevel.destroyBlock(pos, false);
    }

    private ItemStack crushed(ItemStack stack) {
        if (this.level == null) {
            return stack.copy();
        }
        return this.level.getRecipeManager()
                .getRecipeFor(HbmRecipeTypes.SHREDDER.get(), new SingleRecipeInput(stack), this.level)
                .map(holder -> {
                    ShredderRecipe recipe = holder.value();
                    int inputCount = recipe.inputCount();
                    if (stack.getCount() < inputCount || stack.getCount() % inputCount != 0) {
                        return stack.copy();
                    }

                    ItemStack result = recipe.assemble(new SingleRecipeInput(stack), this.level.registryAccess());
                    if (isLegacyCrusherReject(result)) {
                        return stack.copy();
                    }
                    int batches = stack.getCount() / inputCount;
                    result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * batches));
                    return result;
                })
                .filter(result -> !result.isEmpty())
                .orElse(stack.copy());
    }

    private void buildWall(int ring, boolean wallEverything) {
        int y = drillY();
        for (BlockPos pos : squarePositions(ring, y)) {
            boolean edge = pos.getX() == this.worldPosition.getX() - ring
                    || pos.getX() == this.worldPosition.getX() + ring
                    || pos.getZ() == this.worldPosition.getZ() - ring
                    || pos.getZ() == this.worldPosition.getZ() + ring;
            BlockState state = this.level.getBlockState(pos);
            if (edge) {
                if ((wallEverything && state.canBeReplaced()) || !state.getFluidState().isEmpty()) {
                    this.level.setBlock(pos, HbmBlocks.BARRICADE.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            } else if (!state.getFluidState().isEmpty()) {
                this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private void mineOuterOres(int ring) {
        int y = drillY();
        for (BlockPos pos : ringPositions(ring, y)) {
            BlockState state = this.level.getBlockState(pos);
            if (!shouldIgnoreBlock(state, pos) && isOre(state)) {
                tryMineAtLocation(pos, state);
            }
        }
    }

    private void tryCollect(int radius) {
        int y = drillY();
        AABB box = new AABB(
                this.worldPosition.getX() - radius, y - 1, this.worldPosition.getZ() - radius,
                this.worldPosition.getX() + radius + 1, y + 2, this.worldPosition.getZ() + radius + 1
        );
        for (ItemEntity item : this.level.getEntitiesOfClass(ItemEntity.class, box, entity -> entity.isAlive() && !entity.getItem().isEmpty())) {
            ItemStack stack = item.getItem();
            insertOrBuffer(stack);
            if (stack.isEmpty()) {
                item.discard();
                item.setPickUpDelay(60);
            }
        }
    }

    private void tryEjectBuffer() {
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                insertToOutputPorts(stack);
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private void insertOrBuffer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        insertToOutputPorts(stack);
        if (stack.isEmpty()) {
            return;
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack existing = this.items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                stack.shrink(move);
                this.chuteTimer = 40;
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, stack.copy());
                stack.setCount(0);
                this.chuteTimer = 40;
                return;
            }
        }
    }

    private void insertToOutputPorts(ItemStack stack) {
        if (this.level == null) {
            return;
        }
        Port port = outputPort();
        Direction side = port.direction().getOpposite();
        IItemHandler handler = this.level.getCapability(Capabilities.ItemHandler.BLOCK, port.pos(), side);
        if (handler != null) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            int moved = stack.getCount() - remaining.getCount();
            if (moved > 0) {
                stack.shrink(moved);
                this.chuteTimer = 40;
            }
            if (stack.isEmpty()) {
                return;
            }
        }

        BlockEntity blockEntity = this.level.getBlockEntity(port.pos());
        if (blockEntity instanceof Container container) {
            ItemStack remaining = HopperBlockEntity.addItem(null, container, stack.copy(), side);
            int moved = stack.getCount() - remaining.getCount();
            if (moved > 0) {
                stack.shrink(moved);
                this.chuteTimer = 40;
            }
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean shouldIgnoreBlock(BlockState state, BlockPos pos) {
        return state.isAir()
                || state.getDestroySpeed(this.level, pos) < 0.0F
                || !state.getFluidState().isEmpty()
                || state.is(Blocks.BEDROCK);
    }

    private boolean isOre(BlockState state) {
        return state.is(Tags.Blocks.ORES);
    }

    private DrillbitItem.DrillType installedDrill() {
        return DrillbitItem.typeOf(this.items.get(DRILLBIT_SLOT));
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return Math.min(3, level);
    }

    private boolean validUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.EFFECT;
    }

    private void updateConsumption() {
        int speedLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        this.consumption = BASE_CONSUMPTION * (1L + speedLevel) / (1L + powerLevel);
    }

    private void applyFluidIdentifier() {
        ItemStack identifier = this.items.get(FLUID_ID_SLOT);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
            if (!fluid.isNone()) {
                this.tank.setType(fluid);
            }
        }
    }

    private void fillTankFromContainers() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            FluidUtil.getFluidHandler(stack).ifPresent(handler -> {
                net.neoforged.neoforge.fluids.FluidStack drained = handler.drain(this.tank.capacity() - this.tank.amount(), IFluidHandler.FluidAction.SIMULATE);
                if (!drained.isEmpty()) {
                    HbmFluids.fromNeoFluid(drained.getFluid()).ifPresent(type -> {
                        int accepted = this.tank.fill(type, drained.getAmount(), true);
                        if (accepted > 0) {
                            handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                            this.tank.fill(type, accepted, false);
                        }
                    });
                }
            });
        }
    }

    private boolean isLegacyCrusherReject(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
        return result.is(HbmItems.SCRAPS.get()) || path.equals("scrap") || path.equals("dust");
    }

    private List<Port> connectorPositions() {
        Direction dir = this.getBlockState().hasProperty(LargeMachineBlock.FACING) ? this.getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
        return List.of(
                new Port(this.worldPosition.relative(dir, 4).relative(rot).above(), dir),
                new Port(this.worldPosition.relative(dir, 4).relative(rot.getOpposite()).above(), dir),
                new Port(this.worldPosition.relative(rot, 4).above(), rot),
                new Port(this.worldPosition.relative(rot.getOpposite(), 4).above(), rot.getOpposite())
        );
    }

    private Port outputPort() {
        Direction dir = this.getBlockState().hasProperty(LargeMachineBlock.FACING) ? this.getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        return new Port(this.worldPosition.relative(dir, 4).below(3), dir);
    }

    private List<Port> proxyPositions() {
        Direction dir = this.getBlockState().hasProperty(LargeMachineBlock.FACING) ? this.getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
        return List.of(
                new Port(this.worldPosition.relative(dir, 3).relative(rot).above(), dir),
                new Port(this.worldPosition.relative(dir, 3).relative(rot.getOpposite()).above(), dir),
                new Port(this.worldPosition.relative(rot, 3).above(), rot),
                new Port(this.worldPosition.relative(rot.getOpposite(), 3).above(), rot.getOpposite())
        );
    }

    private List<BlockPos> ringPositions(int ring, int y) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : squarePositions(ring, y)) {
            if (ring == 1
                    || pos.getX() == this.worldPosition.getX() - ring
                    || pos.getX() == this.worldPosition.getX() + ring
                    || pos.getZ() == this.worldPosition.getZ() - ring
                    || pos.getZ() == this.worldPosition.getZ() + ring) {
                positions.add(pos);
            }
        }
        return positions;
    }

    private List<BlockPos> squarePositions(int ring, int y) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = this.worldPosition.getX() - ring; x <= this.worldPosition.getX() + ring; x++) {
            for (int z = this.worldPosition.getZ() - ring; z <= this.worldPosition.getZ() + ring; z++) {
                positions.add(new BlockPos(x, y, z));
            }
        }
        return positions;
    }

    private AABB boundsFor(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return new AABB(this.worldPosition);
        }
        int minX = positions.stream().mapToInt(BlockPos::getX).min().orElse(this.worldPosition.getX());
        int minY = positions.stream().mapToInt(BlockPos::getY).min().orElse(this.worldPosition.getY());
        int minZ = positions.stream().mapToInt(BlockPos::getZ).min().orElse(this.worldPosition.getZ());
        int maxX = positions.stream().mapToInt(BlockPos::getX).max().orElse(this.worldPosition.getX());
        int maxY = positions.stream().mapToInt(BlockPos::getY).max().orElse(this.worldPosition.getY());
        int maxZ = positions.stream().mapToInt(BlockPos::getZ).max().orElse(this.worldPosition.getZ());
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static int[] outputSlots() {
        int[] slots = new int[OUTPUT_END - OUTPUT_START];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = OUTPUT_START + i;
        }
        return slots;
    }

    private record Port(BlockPos pos, Direction direction) {
    }
}
