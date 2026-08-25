package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.OreSlopperBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BedrockOreBaseItem;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.OreSlopperMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
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
import net.minecraft.world.entity.Entity;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OreSlopperBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int FLUID_ID_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_END = 9;
    public static final int UPGRADE_START = 9;
    public static final int UPGRADE_END = 11;
    public static final int SLOT_COUNT = 11;
    public static final int DATA_COUNT = 11;
    public static final long MAX_POWER = 100_000L;
    public static final int WATER_USED_BASE = 1_000;
    public static final long CONSUMPTION_BASE = 200L;
    public static final int TANK_CAPACITY = 16_000;

    private static final int[] ACCESSIBLE_SLOTS = {INPUT_SLOT, OUTPUT_START, OUTPUT_START + 1, OUTPUT_START + 2, OUTPUT_START + 3, OUTPUT_START + 4, OUTPUT_START + 5};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank waterTank = new HbmFluidTank(water(), TANK_CAPACITY);
    private final HbmFluidTank slopTank = new HbmFluidTank(slop(), TANK_CAPACITY);
    private final double[] ores = new double[BedrockOreItem.Type.values().length];
    private long power;
    private long lastInput;
    private long consumption = CONSUMPTION_BASE;
    private int progress;
    private int processTime = 600;
    private int waterUsed = WATER_USED_BASE;
    private boolean processing;
    private SlopperAnimation animation = SlopperAnimation.LOWERING;
    private float slider;
    private float prevSlider;
    private float bucket;
    private float prevBucket;
    private float blades;
    private float prevBlades;
    private float fan;
    private float prevFan;
    private int delay;

    public final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) OreSlopperBlockEntity.this.power;
                case 1 -> (int) OreSlopperBlockEntity.this.lastInput;
                case 2 -> OreSlopperBlockEntity.this.progress;
                case 3 -> OreSlopperBlockEntity.this.processTime;
                case 4 -> (int) OreSlopperBlockEntity.this.consumption;
                case 5 -> OreSlopperBlockEntity.this.processing ? 1 : 0;
                case 6 -> OreSlopperBlockEntity.this.waterTank.amount();
                case 7 -> OreSlopperBlockEntity.this.waterTank.type().oldId();
                case 8 -> OreSlopperBlockEntity.this.slopTank.amount();
                case 9 -> OreSlopperBlockEntity.this.slopTank.type().oldId();
                case 10 -> OreSlopperBlockEntity.this.waterUsed;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> OreSlopperBlockEntity.this.power = value;
                case 1 -> OreSlopperBlockEntity.this.lastInput = value;
                case 2 -> OreSlopperBlockEntity.this.progress = value;
                case 3 -> OreSlopperBlockEntity.this.processTime = Math.max(1, value);
                case 4 -> OreSlopperBlockEntity.this.consumption = value;
                case 5 -> OreSlopperBlockEntity.this.processing = value != 0;
                case 6 -> OreSlopperBlockEntity.this.waterTank.setAmount(value);
                case 7 -> OreSlopperBlockEntity.this.waterTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 8 -> OreSlopperBlockEntity.this.slopTank.setAmount(value);
                case 9 -> OreSlopperBlockEntity.this.slopTank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 10 -> OreSlopperBlockEntity.this.waterUsed = Math.max(1, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OreSlopperBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ORE_SLOPPER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OreSlopperBlockEntity slopper) {
        if (level.isClientSide) {
            slopper.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, slopper);
        slopper.tickServer();
    }

    public HbmFluidTank waterTank() {
        return this.waterTank;
    }

    public HbmFluidTank slopTank() {
        return this.slopTank;
    }

    public float slider(float partialTick) {
        return this.prevSlider + (this.slider - this.prevSlider) * partialTick;
    }

    public float bucket(float partialTick) {
        return this.prevBucket + (this.bucket - this.prevBucket) * partialTick;
    }

    public float blades(float partialTick) {
        return this.prevBlades + (this.blades - this.prevBlades) * partialTick;
    }

    public float fan(float partialTick) {
        return this.prevFan + (this.fan - this.prevFan) * partialTick;
    }

    public boolean clientLifting() {
        return this.animation == SlopperAnimation.LIFTING;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!isAutomationPort(queriedPos)) {
            return null;
        }
        return new DualTankHandler();
    }

    public boolean isAutomationPort(BlockPos queriedPos) {
        if (queriedPos.equals(this.worldPosition)) {
            return true;
        }
        return portPositions().stream().anyMatch(port -> port.pos().equals(queriedPos));
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return portPositions().stream().map(Port::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return portPositions().stream().anyMatch(port -> port.pos().equals(connectorPos));
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : Math.min(Math.max(CONSUMPTION_BASE, this.consumption), MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.ore_slopper", this.lastInput, this.consumption, this.power, MAX_POWER);
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
        if (slot == INPUT_SLOT) {
            return stack.is(HbmItems.BEDROCK_ORE_BASE.get());
        }
        if (slot >= UPGRADE_START && slot < UPGRADE_END) {
            MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
            return MachineUpgradeItem.isMachineUpgrade(stack)
                    && (type == MachineUpgradeItem.UpgradeType.SPEED || type == MachineUpgradeItem.UpgradeType.EFFECT);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT && canPlaceItem(slot, stack);
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
        this.items.replaceAll(ignored -> ItemStack.EMPTY);
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
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_ore_slopper");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OreSlopperMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.put("WaterTank", this.waterTank.save());
        tag.put("SlopTank", this.slopTank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("Consumption", this.consumption);
        tag.putInt("Progress", this.progress);
        tag.putInt("ProcessTime", this.processTime);
        tag.putBoolean("Processing", this.processing);
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            tag.putDouble("Ore" + type.ordinal(), this.ores[type.ordinal()]);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Processing", this.processing);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.waterTank.load(tag.getCompound("WaterTank"));
        this.slopTank.load(tag.getCompound("SlopTank"));
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.consumption = tag.contains("Consumption") ? tag.getLong("Consumption") : CONSUMPTION_BASE;
        this.progress = tag.getInt("Progress");
        this.processTime = tag.contains("ProcessTime") ? Math.max(1, tag.getInt("ProcessTime")) : 600;
        this.processing = tag.getBoolean("Processing");
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            this.ores[type.ordinal()] = tag.getDouble("Ore" + type.ordinal());
        }
    }

    private void tickServer() {
        boolean wasProcessing = this.processing;
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        applyFluidIdentifier();
        fillTankFromContainers();
        updateUpgrades();
        pushSlop();
        this.processing = false;

        if (canSlop()) {
            this.power -= this.consumption;
            this.progress++;
            this.processing = true;
            while (this.progress >= this.processTime && canSlop()) {
                this.progress -= this.processTime;
                processOneOre();
            }
            damageEntitiesInShredder();
        } else {
            this.progress = 0;
        }

        emitStoredOreItems();
        setChanged();
        if (wasProcessing != this.processing && this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        this.prevSlider = this.slider;
        this.prevBucket = this.bucket;
        this.prevBlades = this.blades;
        this.prevFan = this.fan;

        if (!this.processing) {
            return;
        }

        if (this.animation == SlopperAnimation.DUMPING && this.level != null) {
            Direction dir = OreSlopperBlock.legacyDirFromFacing(facing());
            this.level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                    this.worldPosition.getX() + 0.5D + dir.getStepX() + this.level.random.nextGaussian() * 0.25D,
                    this.worldPosition.getY() + 4.25D,
                    this.worldPosition.getZ() + 0.5D + dir.getStepZ() + this.level.random.nextGaussian() * 0.25D,
                    0.0D,
                    -0.2D,
                    0.0D
            );
        }

        this.blades += 15.0F;
        this.fan += 35.0F;

        if (this.blades >= 360.0F) {
            this.blades -= 360.0F;
            this.prevBlades -= 360.0F;
        }
        if (this.fan >= 360.0F) {
            this.fan -= 360.0F;
            this.prevFan -= 360.0F;
        }

        if (this.delay > 0) {
            this.delay--;
            return;
        }

        switch (this.animation) {
            case LOWERING -> {
                this.bucket += 1.0F / 40.0F;
                if (this.bucket >= 1.0F) {
                    this.bucket = 1.0F;
                    this.animation = SlopperAnimation.LIFTING;
                    this.delay = 20;
                }
            }
            case LIFTING -> {
                this.bucket -= 1.0F / 40.0F;
                if (this.bucket <= 0.0F) {
                    this.bucket = 0.0F;
                    this.animation = SlopperAnimation.MOVE_SHREDDER;
                    this.delay = 10;
                }
            }
            case MOVE_SHREDDER -> {
                this.slider += 1.0F / 50.0F;
                if (this.slider >= 1.0F) {
                    this.slider = 1.0F;
                    this.animation = SlopperAnimation.DUMPING;
                    this.delay = 60;
                }
            }
            case DUMPING -> this.animation = SlopperAnimation.MOVE_BUCKET;
            case MOVE_BUCKET -> {
                this.slider -= 1.0F / 50.0F;
                if (this.slider <= 0.0F) {
                    this.slider = 0.0F;
                    this.animation = SlopperAnimation.LOWERING;
                    this.delay = 10;
                }
            }
        }
    }

    private void updateUpgrades() {
        int speed = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int efficiency = upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT);
        this.processTime = Math.max(150, 600 - speed * 150);
        this.consumption = CONSUMPTION_BASE + (CONSUMPTION_BASE * speed) / 2 + (CONSUMPTION_BASE * efficiency);
        this.waterUsed = WATER_USED_BASE;
    }

    /**
     * 1.7.10 checks this exact one-by-two-by-two volume one block in front of
     * the active shredder and applies the turbofan's lethal damage every tick.
     */
    private void damageEntitiesInShredder() {
        if (this.level == null) {
            return;
        }
        Direction dir = OreSlopperBlock.legacyDirFromFacing(facing());
        AABB blades = new AABB(
                this.worldPosition.getX() - 0.5D,
                this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() - 0.5D,
                this.worldPosition.getX() + 1.5D,
                this.worldPosition.getY() + 3.0D,
                this.worldPosition.getZ() + 1.5D
        ).move(dir.getStepX(), 0.0D, dir.getStepZ());

        for (Entity entity : this.level.getEntitiesOfClass(Entity.class, blades)) {
            boolean wasAlive = entity.isAlive();
            entity.hurt(this.level.damageSources().source(HbmDamageTypes.TURBOFAN), 1_000.0F);
            if (wasAlive && !entity.isAlive() && entity instanceof LivingEntity living) {
                TurretCasingEffects.spawnMaxwellGib(this.level, living, false);
            }
        }
    }

    private void processOneOre() {
        ItemStack input = this.items.get(INPUT_SLOT);
        if (!input.is(HbmItems.BEDROCK_ORE_BASE.get())) {
            return;
        }
        double efficiency = 1.0D + upgradeLevel(MachineUpgradeItem.UpgradeType.EFFECT) * 0.1D;
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            this.ores[type.ordinal()] += BedrockOreBaseItem.getOreAmount(input, type) * efficiency;
        }
        input.shrink(1);
        if (input.isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        this.waterTank.drain(water(), this.waterUsed, false);
        this.slopTank.fill(slop(), this.waterUsed, false);
    }

    private void emitStoredOreItems() {
        for (BedrockOreItem.Type type : BedrockOreItem.Type.values()) {
            while (this.ores[type.ordinal()] >= 1.0D) {
                ItemStack output = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, type);
                if (!insertOutput(output)) {
                    return;
                }
                this.ores[type.ordinal()] -= 1.0D;
            }
        }
    }

    private boolean insertOutput(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack existing = this.items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                return true;
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, stack.copy());
                return true;
            }
        }
        return false;
    }

    private boolean canSlop() {
        return this.waterTank.type() == water()
                && this.waterTank.amount() >= this.waterUsed
                && (this.slopTank.type().isNone() || this.slopTank.type() == slop())
                && this.slopTank.amount() + this.waterUsed <= this.slopTank.capacity()
                && this.power >= this.consumption
                && this.items.get(INPUT_SLOT).is(HbmItems.BEDROCK_ORE_BASE.get());
    }

    private void applyFluidIdentifier() {
        ItemStack identifier = this.items.get(FLUID_ID_SLOT);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition fluid = FluidIdentifierItem.primary(identifier);
            if (fluid == water()) {
                this.waterTank.setType(fluid);
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
                FluidStack drained = handler.drain(this.waterTank.capacity() - this.waterTank.amount(), IFluidHandler.FluidAction.SIMULATE);
                if (!drained.isEmpty()) {
                    HbmFluids.fromNeoFluid(drained.getFluid()).ifPresent(type -> {
                        if (type == water()) {
                            int accepted = this.waterTank.fill(type, drained.getAmount(), true);
                            if (accepted > 0) {
                                handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                                this.waterTank.fill(type, accepted, false);
                            }
                        }
                    });
                }
            });
        }
    }

    private void pushSlop() {
        if (this.level == null || this.slopTank.amount() <= 0 || this.slopTank.type().isNone()) {
            return;
        }
        for (Port port : portPositions()) {
            int amount = Math.min(1_000, this.slopTank.amount());
            FluidStack stack = HbmFluids.toNeoStack(this.slopTank.type(), amount);
            int accepted = com.reinhardt.hbm.fluid.HbmFluidNetworks.fillInto(this.level, port.pos(), port.direction().getOpposite(), stack, this.worldPosition, true);
            if (accepted > 0) {
                this.slopTank.drain(this.slopTank.type(), accepted, false);
            }
        }
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

    private List<Port> portPositions() {
        Direction facing = this.getBlockState().hasProperty(LargeMachineBlock.FACING) ? this.getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction dir = OreSlopperBlock.legacyDirFromFacing(facing);
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
        ArrayList<Port> ports = new ArrayList<>();
        ports.add(new Port(this.worldPosition.relative(dir, 4), dir));
        ports.add(new Port(this.worldPosition.relative(dir.getOpposite(), 4), dir.getOpposite()));
        ports.add(new Port(this.worldPosition.relative(rot, 2), rot));
        ports.add(new Port(this.worldPosition.relative(rot.getOpposite(), 2), rot.getOpposite()));
        ports.add(new Port(this.worldPosition.relative(dir, 2).relative(rot, 2), rot));
        ports.add(new Port(this.worldPosition.relative(dir, 2).relative(rot.getOpposite(), 2), rot.getOpposite()));
        ports.add(new Port(this.worldPosition.relative(dir.getOpposite(), 2).relative(rot, 2), rot));
        ports.add(new Port(this.worldPosition.relative(dir.getOpposite(), 2).relative(rot.getOpposite(), 2), rot.getOpposite()));
        return ports;
    }

    private Direction facing() {
        return this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
    }

    private static HbmFluidDefinition water() {
        return HbmFluids.byName("water").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition slop() {
        return HbmFluids.byName("slop").orElse(HbmFluids.none());
    }

    private record Port(BlockPos pos, Direction direction) {
    }

    private enum SlopperAnimation {
        LOWERING,
        LIFTING,
        MOVE_SHREDDER,
        DUMPING,
        MOVE_BUCKET
    }

    private final class DualTankHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluidInTank(0) : slopTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.capacity() : slopTank.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return tank == 0 && type == water();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            return type == water() ? waterTank.fill(type, resource.getAmount(), action.simulate()) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            HbmFluidDefinition type = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(null);
            com.reinhardt.hbm.fluid.HbmFluidStack drained = type == slop()
                    ? slopTank.drain(type, resource.getAmount(), action.simulate())
                    : com.reinhardt.hbm.fluid.HbmFluidStack.EMPTY;
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            com.reinhardt.hbm.fluid.HbmFluidStack drained = slopTank.drain(slop(), maxDrain, action.simulate());
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
