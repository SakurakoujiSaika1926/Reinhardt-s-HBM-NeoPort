package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.PACoilItem;
import com.reinhardt.hbm.menu.ParticleAcceleratorMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ParticleAcceleratorRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
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
import net.minecraft.util.Mth;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ParticleAcceleratorBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int BATTERY_SLOT = 0;
    public static final int INPUT_A_SLOT = 1;
    public static final int INPUT_B_SLOT = 2;
    public static final int OUTPUT_A_SLOT = 3;
    public static final int OUTPUT_B_SLOT = 4;
    public static final int SLOT_COUNT = 5;
    public static final int DATA_COUNT = 13;

    private static final int[] SOURCE_RED = {INPUT_A_SLOT, OUTPUT_A_SLOT, OUTPUT_B_SLOT};
    private static final int[] SOURCE_YELLOW = {INPUT_B_SLOT, OUTPUT_A_SLOT, OUTPUT_B_SLOT};
    private static final int[] SOURCE_OUTPUTS = {OUTPUT_A_SLOT, OUTPUT_B_SLOT};
    private static final int[] DETECTOR_ACCESS = {INPUT_A_SLOT, INPUT_B_SLOT, OUTPUT_A_SLOT, OUTPUT_B_SLOT};
    private static final int[] BATTERY_ONLY = {BATTERY_SLOT};
    private static final int[] BATTERY_AND_COIL = {BATTERY_SLOT, INPUT_A_SLOT};
    private static final int[] NO_SLOTS = {};

    private static final long SOURCE_USAGE = 100_000L;
    private static final long RFC_USAGE = 250_000L;
    private static final long MAGNET_USAGE = 100_000L;
    private static final long DETECTOR_USAGE = 100_000L;
    private static final int MOMENTUM_GAIN = 100;
    private static final int DEFOCUS_GAIN = 100;
    private static final int FOCUS_GAIN = 100;
    private static final int COOLANT_CAPACITY = 4_000;
    private static final float KELVIN = 273.0F;
    private static final float DEFAULT_TEMPERATURE = KELVIN + 20.0F;
    private static final float TARGET_TEMPERATURE = KELVIN - 150.0F;
    private static final float TEMP_CHANGE_PER_MB = 0.5F;
    private static final float TEMP_PASSIVE_HEATING = 2.5F;
    private static final float TEMP_CHANGE_MAX = 7.5F;

    private final ParticleAcceleratorBlock.Kind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank coldTank = new HbmFluidTank(fluid("perfluoromethyl_cold"), COOLANT_CAPACITY);
    private final HbmFluidTank hotTank = new HbmFluidTank(fluid("perfluoromethyl"), COOLANT_CAPACITY);
    private long power;
    private float temperature = DEFAULT_TEMPERATURE;
    private Particle particle;
    private PAState state = PAState.IDLE;
    private int lastSpeed;
    private int debugSpeed;
    private int dirLower;
    private int dirUpper;
    private int dirRedstone;
    private int threshold;
    private boolean window;
    private boolean didPass;
    private float light;
    private float prevLight;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, power);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, maxPower());
                case 2 -> coldTank.amount();
                case 3 -> hotTank.amount();
                case 4 -> COOLANT_CAPACITY;
                case 5 -> Math.round(temperature * 10.0F);
                case 6 -> Math.round(TARGET_TEMPERATURE * 10.0F);
                case 7 -> state.ordinal();
                case 8 -> lastSpeed;
                case 9 -> debugSpeed;
                case 10 -> dirLower;
                case 11 -> dirUpper;
                case 12 -> dirRedstone;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> power = Math.max(0L, value);
                case 2 -> coldTank.setAmount(value);
                case 3 -> hotTank.setAmount(value);
                case 5 -> temperature = value / 10.0F;
                case 7 -> state = PAState.byOrdinal(value);
                case 8 -> lastSpeed = value;
                case 9 -> debugSpeed = value;
                case 10 -> dirLower = Math.floorMod(value, 4);
                case 11 -> dirUpper = Math.floorMod(value, 4);
                case 12 -> dirRedstone = Math.floorMod(value, 4);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ParticleAcceleratorBlockEntity(BlockPos pos, BlockState state, ParticleAcceleratorBlock.Kind kind) {
        super(HbmBlockEntities.PARTICLE_ACCELERATOR.get(), pos, state);
        this.kind = kind;
    }

    public ParticleAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof ParticleAcceleratorBlock block ? block.kind() : ParticleAcceleratorBlock.Kind.SOURCE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ParticleAcceleratorBlockEntity accelerator) {
        if (level.isClientSide) {
            accelerator.tickClient();
            return;
        }
        if (accelerator.kind.hasMenu()) {
            PowerNetworkManager.tickFromEndpoint(level, accelerator);
        }
        accelerator.tickServer();
    }

    public ParticleAcceleratorBlock.Kind kind() {
        return this.kind;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank coldTank() {
        return this.coldTank;
    }

    public HbmFluidTank hotTank() {
        return this.hotTank;
    }

    public long power() {
        return this.power;
    }

    public long maxPower() {
        return switch (this.kind) {
            case SOURCE -> 10_000_000L;
            case RFC, DETECTOR -> 1_000_000L;
            case QUADRUPOLE, DIPOLE -> 2_500_000L;
            case BEAMLINE -> 0L;
        };
    }

    public float temperature() {
        return this.temperature;
    }

    public PAState state() {
        return this.state;
    }

    public int threshold() {
        return this.threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = Mth.clamp(threshold, 0, 999_999_999);
        sync();
    }

    public void cycleLowerDirection() {
        this.dirLower = (this.dirLower + 1) & 3;
        sync();
    }

    public void cycleUpperDirection() {
        this.dirUpper = (this.dirUpper + 1) & 3;
        sync();
    }

    public void cycleRedstoneDirection() {
        this.dirRedstone = (this.dirRedstone + 1) & 3;
        sync();
    }

    public void cancelParticle() {
        this.particle = null;
        this.state = PAState.IDLE;
        sync();
    }

    public boolean hasBeamlineWindow() {
        return this.window;
    }

    public float beamlineLight(float partialTick) {
        return this.prevLight + (this.light - this.prevLight) * partialTick;
    }

    public void toggleBeamlineWindow() {
        if (this.kind != ParticleAcceleratorBlock.Kind.BEAMLINE) {
            return;
        }
        this.window = !this.window;
        sync();
    }

    private void tickClient() {
        if (this.kind != ParticleAcceleratorBlock.Kind.BEAMLINE) {
            return;
        }
        this.prevLight = this.light;
        if (this.light > 0.0F) {
            this.light = Math.max(0.0F, this.light - 0.25F);
        }
    }

    private void tickServer() {
        if (this.kind.hasMenu()) {
            this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, maxPower());
        }
        switch (this.kind) {
            case SOURCE -> tickSource();
            case BEAMLINE -> {
                if (this.didPass) {
                    sync();
                    this.didPass = false;
                }
            }
            default -> {
            }
        }
        if (this.kind != ParticleAcceleratorBlock.Kind.BEAMLINE) {
            tickCooling();
        }
        sync();
    }

    private void tickSource() {
        int steps = this.particle == null ? 1 : 1 + Mth.clamp(this.particle.momentum / 1_000, 0, 9);
        for (int i = 0; i < steps; i++) {
            if (this.particle != null) {
                this.state = PAState.RUNNING;
                stepParticle();
                this.debugSpeed = this.particle == null ? 0 : this.particle.momentum;
                if (this.particle != null && this.particle.invalid) {
                    this.particle = null;
                }
            } else if (this.power >= SOURCE_USAGE && !this.items.get(INPUT_A_SLOT).isEmpty() && !this.items.get(INPUT_B_SLOT).isEmpty()) {
                tryRunSource();
                break;
            }
        }
    }

    private void tryRunSource() {
        if (!isCool()) {
            return;
        }
        ItemStack first = this.items.get(INPUT_A_SLOT);
        ItemStack second = this.items.get(INPUT_B_SLOT);
        ItemStack firstContainer = first.getCraftingRemainingItem();
        ItemStack secondContainer = second.getCraftingRemainingItem();
        if (!firstContainer.isEmpty() && !canAcceptStack(OUTPUT_A_SLOT, firstContainer)) {
            return;
        }
        if (!secondContainer.isEmpty() && !canAcceptStack(OUTPUT_B_SLOT, secondContainer)) {
            return;
        }
        if (!firstContainer.isEmpty()) {
            addStack(OUTPUT_A_SLOT, firstContainer);
        }
        if (!secondContainer.isEmpty()) {
            addStack(OUTPUT_B_SLOT, secondContainer);
        }

        this.power -= SOURCE_USAGE;
        Direction axis = beamAxis();
        BlockPos start = this.worldPosition.relative(axis, 5);
        this.particle = new Particle(start, axis, first.copyWithCount(1), second.copyWithCount(1));
        this.items.set(INPUT_A_SLOT, ItemStack.EMPTY);
        this.items.set(INPUT_B_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    private void stepParticle() {
        if (this.level == null || this.particle == null) {
            return;
        }
        if (!this.level.hasChunk(this.particle.pos.getX() >> 4, this.particle.pos.getZ() >> 4)) {
            this.state = PAState.PAUSE_UNLOADED;
            return;
        }
        Particle particle = this.particle;
        BlockEntity target = particleTarget(this.level, particle.pos);
        if (!(target instanceof ParticleAcceleratorBlockEntity accelerator)) {
            particle.crash(this, PAState.CRASH_DERAIL);
            return;
        }
        if (!accelerator.canParticleEnter(particle, particle.direction, particle.pos)) {
            particle.crash(this, PAState.CRASH_CANNOT_ENTER);
            return;
        }
        accelerator.onEnter(particle, particle.direction, this);
        if (particle.invalid) {
            return;
        }
        BlockPos exit = accelerator.getExitPos(particle);
        if (exit != null) {
            particle.move(this, exit);
        }
    }

    @Nullable
    private static BlockEntity particleTarget(Level level, BlockPos pos) {
        BlockEntity direct = level.getBlockEntity(pos);
        if (direct instanceof ParticleAcceleratorBlockEntity) {
            return direct;
        }
        if (direct instanceof MachineDummyBlockEntity dummy) {
            return level.getBlockEntity(dummy.getCorePos());
        }
        return null;
    }

    private boolean canParticleEnter(Particle particle, Direction direction, BlockPos particlePos) {
        Direction axis = beamAxis();
        return switch (this.kind) {
            case BEAMLINE, QUADRUPOLE -> particlePos.equals(this.worldPosition.relative(axis, -1)) && direction == axis;
            case RFC, DETECTOR -> particlePos.equals(this.worldPosition.relative(axis, -4)) && direction == axis;
            case DIPOLE -> particlePos.getY() == this.worldPosition.getY()
                    && (particlePos.getX() == this.worldPosition.getX() || particlePos.getZ() == this.worldPosition.getZ());
            case SOURCE -> false;
        };
    }

    private void onEnter(Particle particle, Direction direction, ParticleAcceleratorBlockEntity source) {
        switch (this.kind) {
            case BEAMLINE -> {
                particle.addDistance(3);
                this.didPass = true;
                this.light = 2.0F;
                sync();
            }
            case RFC -> {
                if (!isCool()) particle.crash(source, PAState.CRASH_NOCOOL);
                if (this.power < RFC_USAGE) particle.crash(source, PAState.CRASH_NOPOWER);
                if (particle.invalid) return;
                particle.addDistance(9);
                particle.momentum += MOMENTUM_GAIN;
                particle.defocus(source, DEFOCUS_GAIN);
                this.power -= RFC_USAGE;
                sync();
            }
            case QUADRUPOLE -> enterQuadrupole(particle, source);
            case DIPOLE -> enterDipole(particle, direction, source);
            case DETECTOR -> enterDetector(particle, source);
            case SOURCE -> {
            }
        }
    }

    private void enterQuadrupole(Particle particle, ParticleAcceleratorBlockEntity source) {
        PACoilItem.Spec spec = coilSpec();
        int multiplier = spec != null && spec.quadMin() > particle.momentum ? 10 : 1;
        if (!isCool()) particle.crash(source, PAState.CRASH_NOCOOL);
        if (this.power < MAGNET_USAGE * multiplier) particle.crash(source, PAState.CRASH_NOPOWER);
        if (spec == null) particle.crash(source, PAState.CRASH_NOCOIL);
        if (spec != null && spec.quadMax() < particle.momentum) particle.crash(source, PAState.CRASH_OVERSPEED);
        if (particle.invalid) return;
        particle.addDistance(3);
        particle.focus(FOCUS_GAIN);
        this.power -= MAGNET_USAGE * multiplier;
        sync();
    }

    private void enterDipole(Particle particle, Direction direction, ParticleAcceleratorBlockEntity source) {
        Direction exit = getExitDirection();
        boolean inline = direction == exit;
        PACoilItem.Spec spec = coilSpec();
        int multiplier = 1;
        if (spec != null && !inline) {
            if (spec.dipoleMin() > particle.momentum) {
                multiplier *= 10;
            }
            if (spec.dipoleDistanceMin() > particle.distanceTraveled) {
                multiplier *= 10;
            }
        }
        if (!isCool()) particle.crash(source, PAState.CRASH_NOCOOL);
        if (this.power < MAGNET_USAGE * multiplier) particle.crash(source, PAState.CRASH_NOPOWER);
        if (spec == null) particle.crash(source, PAState.CRASH_NOCOIL);
        if (spec != null && spec.dipoleMax() < particle.momentum && !inline) particle.crash(source, PAState.CRASH_OVERSPEED);
        if (particle.invalid) return;
        if (inline) {
            particle.addDistance(3);
        } else {
            particle.resetDistance();
        }
        this.power -= MAGNET_USAGE * multiplier;
        sync();
    }

    private void enterDetector(Particle particle, ParticleAcceleratorBlockEntity source) {
        particle.invalid = true;
        if (particle.defocus > 0) {
            particle.crash(source, PAState.CRASH_DEFOCUS);
            return;
        }
        if (this.power < DETECTOR_USAGE) {
            particle.crash(source, PAState.CRASH_NOPOWER);
            return;
        }
        if (!isCool()) {
            particle.crash(source, PAState.CRASH_NOCOOL);
            return;
        }
        this.power -= DETECTOR_USAGE;
        if (this.level == null) {
            particle.crash(source, PAState.CRASH_NORECIPE);
            return;
        }

        for (RecipeHolder<ParticleAcceleratorRecipe> holder : this.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PARTICLE_ACCELERATOR.get())) {
            ParticleAcceleratorRecipe recipe = holder.value();
            if (!recipe.matchesSymmetric(particle.input1, particle.input2)) {
                continue;
            }
            if (particle.momentum < recipe.momentum()) {
                particle.crash(source, PAState.CRASH_UNDERSPEED);
                return;
            }
            if (canAcceptRecipe(recipe)) {
                consumeOutputContainer(recipe.output1(), INPUT_A_SLOT);
                consumeOutputContainer(recipe.output2(), INPUT_B_SLOT);
                addStack(OUTPUT_A_SLOT, recipe.output1());
                if (!recipe.output2().isEmpty()) {
                    addStack(OUTPUT_B_SLOT, recipe.output2());
                }
            }
            particle.crash(source, PAState.SUCCESS);
            sync();
            return;
        }
        particle.crash(source, PAState.CRASH_NORECIPE);
    }

    @Nullable
    private PACoilItem.Spec coilSpec() {
        ItemStack stack = this.items.get(INPUT_A_SLOT);
        return stack.getItem() instanceof PACoilItem coil ? coil.spec(stack) : null;
    }

    private boolean canAcceptRecipe(ParticleAcceleratorRecipe recipe) {
        return canAcceptDetectorOutput(recipe.output1(), INPUT_A_SLOT, OUTPUT_A_SLOT)
                && canAcceptDetectorOutput(recipe.output2(), INPUT_B_SLOT, OUTPUT_B_SLOT);
    }

    private boolean canAcceptDetectorOutput(ItemStack output, int containerSlot, int outputSlot) {
        if (output.isEmpty()) {
            return true;
        }
        if (!canAcceptStack(outputSlot, output)) {
            return false;
        }
        ItemStack container = output.getCraftingRemainingItem();
        if (container.isEmpty()) {
            return true;
        }
        ItemStack helper = this.items.get(containerSlot);
        return !helper.isEmpty() && ItemStack.isSameItemSameComponents(helper, container);
    }

    private void consumeOutputContainer(ItemStack output, int helperSlot) {
        if (!output.isEmpty() && !output.getCraftingRemainingItem().isEmpty()) {
            this.items.get(helperSlot).shrink(1);
            if (this.items.get(helperSlot).isEmpty()) {
                this.items.set(helperSlot, ItemStack.EMPTY);
            }
        }
    }

    @Nullable
    private BlockPos getExitPos(Particle particle) {
        return switch (this.kind) {
            case BEAMLINE, QUADRUPOLE -> this.worldPosition.relative(beamAxis(), 2);
            case RFC, DETECTOR -> this.kind == ParticleAcceleratorBlock.Kind.RFC ? this.worldPosition.relative(beamAxis(), 5) : null;
            case DIPOLE -> {
                Direction exit = getExitDirection();
                particle.direction = exit;
                yield this.worldPosition.relative(exit, 2);
            }
            case SOURCE -> null;
        };
    }

    private Direction getExitDirection() {
        int value = this.particleValueForDipole();
        return switch (value) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private int particleValueForDipole() {
        if (isAnyConnectorPowered()) {
            return this.dirRedstone;
        }
        return this.lastSpeed < this.threshold ? this.dirLower : this.dirUpper;
    }

    private boolean isAnyConnectorPowered() {
        if (this.level == null) {
            return false;
        }
        for (PAConnector connector : connectorSpecs(this.worldPosition, facing(), this.kind)) {
            if (this.level.hasNeighborSignal(connector.pos())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCool() {
        return this.temperature <= TARGET_TEMPERATURE;
    }

    private void tickCooling() {
        this.temperature += TEMP_PASSIVE_HEATING;
        if (this.temperature > DEFAULT_TEMPERATURE) {
            this.temperature = DEFAULT_TEMPERATURE;
        }
        if (this.temperature <= TARGET_TEMPERATURE) {
            return;
        }
        int cyclesTemp = (int) Math.ceil(Math.min(this.temperature - TARGET_TEMPERATURE, TEMP_CHANGE_MAX) / TEMP_CHANGE_PER_MB);
        int cyclesCool = this.coldTank.amount();
        int cyclesHot = this.hotTank.capacity() - this.hotTank.amount();
        int cycles = Math.min(cyclesTemp, Math.min(cyclesCool, cyclesHot));
        if (cycles > 0) {
            this.coldTank.drain(this.coldTank.type(), cycles, false);
            this.hotTank.fill(this.hotTank.type(), cycles, false);
            this.temperature -= TEMP_CHANGE_PER_MB * cycles;
        }
    }

    private boolean canAcceptStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() + stack.getCount() <= getMaxStackSize(current));
    }

    private void addStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, copy);
        } else {
            current.grow(copy.getCount());
        }
    }

    private Direction facing() {
        return this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
    }

    private Direction beamAxis() {
        return LegacyMachineGeometry.forgeRotateDown(facing());
    }

    public static List<BlockPos> connectorPositions(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (ParticleAcceleratorBlock.Kind kind : ParticleAcceleratorBlock.Kind.values()) {
            for (PAConnector connector : connectorSpecs(corePos, facing, kind)) {
                if (!positions.contains(connector.pos())) {
                    positions.add(connector.pos());
                }
            }
        }
        return List.copyOf(positions);
    }

    public static List<BlockPos> connectorPositions(BlockPos corePos, Direction facing, ParticleAcceleratorBlock.Kind kind) {
        return connectorSpecs(corePos, facing, kind).stream()
                .map(PAConnector::pos)
                .toList();
    }

    private static List<PAConnector> connectorSpecs(BlockPos corePos, Direction facing, ParticleAcceleratorBlock.Kind kind) {
        Direction dir = facing;
        Direction rotUp = LegacyMachineGeometry.forgeRotateUp(facing);
        Direction rotDown = LegacyMachineGeometry.forgeRotateDown(facing);
        return switch (kind) {
            case SOURCE -> List.of(
                    new PAConnector(corePos.relative(dir, 2), dir),
                    new PAConnector(corePos.relative(dir, 2).relative(rotUp, 2), dir),
                    new PAConnector(corePos.relative(dir, 2).relative(rotUp, -2), dir),
                    new PAConnector(corePos.relative(dir, -2), dir.getOpposite()),
                    new PAConnector(corePos.relative(dir, -2).relative(rotUp, 2), dir.getOpposite()),
                    new PAConnector(corePos.relative(dir, -2).relative(rotUp, -2), dir.getOpposite()),
                    new PAConnector(corePos.relative(rotUp, 5), rotUp)
            );
            case RFC -> List.of(
                    new PAConnector(corePos.relative(rotUp, 3).above(2), Direction.UP),
                    new PAConnector(corePos.relative(rotUp, -3).above(2), Direction.UP),
                    new PAConnector(corePos.above(2), Direction.UP),
                    new PAConnector(corePos.relative(rotUp, 3).below(2), Direction.DOWN),
                    new PAConnector(corePos.relative(rotUp, -3).below(2), Direction.DOWN),
                    new PAConnector(corePos.below(2), Direction.DOWN)
            );
            case QUADRUPOLE -> List.of(
                    new PAConnector(corePos.above(2), Direction.UP),
                    new PAConnector(corePos.below(2), Direction.DOWN),
                    new PAConnector(corePos.relative(dir, 2), dir),
                    new PAConnector(corePos.relative(dir, -2), dir.getOpposite())
            );
            case DIPOLE -> List.of(
                    new PAConnector(corePos.east().above(2), Direction.UP),
                    new PAConnector(corePos.west().above(2), Direction.UP),
                    new PAConnector(corePos.south().above(2), Direction.UP),
                    new PAConnector(corePos.north().above(2), Direction.UP),
                    new PAConnector(corePos.east().below(2), Direction.DOWN),
                    new PAConnector(corePos.west().below(2), Direction.DOWN),
                    new PAConnector(corePos.south().below(2), Direction.DOWN),
                    new PAConnector(corePos.north().below(2), Direction.DOWN)
            );
            case DETECTOR -> List.of(
                    new PAConnector(corePos.relative(rotDown, 5), rotDown),
                    new PAConnector(corePos.relative(rotDown, 5).above(), rotDown),
                    new PAConnector(corePos.relative(rotDown, 5).below(), rotDown),
                    new PAConnector(corePos.relative(rotDown, 5).relative(dir), rotDown),
                    new PAConnector(corePos.relative(rotDown, 5).relative(dir, -1), rotDown)
            );
            case BEAMLINE -> List.of();
        };
    }

    private List<BlockPos> connectorPositions() {
        return connectorPositions(this.worldPosition, facing(), this.kind);
    }

    private boolean hasConnector(BlockPos connectorPos, @Nullable Direction machineSide) {
        for (PAConnector connector : connectorSpecs(this.worldPosition, facing(), this.kind)) {
            if (connector.pos().equals(connectorPos) && (machineSide == null || connector.face() == machineSide)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return this.kind.hasMenu() && hasConnector(connectorPos, machineSide);
    }

    @Override
    public long getAvailableOutput() {
        return 0;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, maxPower() - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (receivedInput > 0L) {
            this.power = Math.min(maxPower(), this.power + receivedInput);
            sync();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + " / " + maxPower() + " HE");
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (this.kind == ParticleAcceleratorBlock.Kind.BEAMLINE || !hasConnector(queriedPos, side)) {
            return null;
        }
        return new CoolantFluidHandler();
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int getMaxStackSize() {
        return this.kind == ParticleAcceleratorBlock.Kind.SOURCE ? 1 : 64;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            sync();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (this.kind) {
            case SOURCE -> slot == INPUT_A_SLOT || slot == INPUT_B_SLOT || (slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack));
            case RFC, DETECTOR -> slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack)
                    || (this.kind == ParticleAcceleratorBlock.Kind.DETECTOR && (slot == INPUT_A_SLOT || slot == INPUT_B_SLOT));
            case QUADRUPOLE, DIPOLE -> slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack)
                    || (slot == INPUT_A_SLOT && stack.getItem() instanceof PACoilItem);
            case BEAMLINE -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (this.kind) {
            case SOURCE -> sourceSlotsForAccessor(this.worldPosition, side);
            case DETECTOR -> DETECTOR_ACCESS;
            case RFC -> BATTERY_ONLY;
            case QUADRUPOLE, DIPOLE -> BATTERY_AND_COIL;
            case BEAMLINE -> NO_SLOTS;
        };
    }

    public int[] getSlotsForAccessor(BlockPos accessor, @Nullable Direction side) {
        if (this.kind == ParticleAcceleratorBlock.Kind.SOURCE) {
            return sourceSlotsForAccessor(accessor, side);
        }
        return getSlotsForFace(side == null ? Direction.UP : side);
    }

    private int[] sourceSlotsForAccessor(BlockPos accessor, @Nullable Direction side) {
        Direction dir = facing();
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
        if (accessor.equals(this.worldPosition.relative(dir).relative(rot, -2))
                || accessor.equals(this.worldPosition.relative(dir, -1).relative(rot, 2))) {
            return SOURCE_YELLOW;
        }
        if (accessor.equals(this.worldPosition.relative(dir, -1).relative(rot, -2))
                || accessor.equals(this.worldPosition.relative(dir).relative(rot, 2))) {
            return SOURCE_RED;
        }
        return SOURCE_OUTPUTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_A_SLOT || slot == OUTPUT_B_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        sync();
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
        return Component.translatable("container.reinhardtshbm." + this.kind.id());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ParticleAcceleratorMenu(containerId, inventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putString("kind", this.kind.name());
        tag.putLong("power", this.power);
        tag.putFloat("temperature", this.temperature);
        tag.put("cold", this.coldTank.save());
        tag.put("hot", this.hotTank.save());
        tag.putInt("state", this.state.ordinal());
        tag.putInt("lastSpeed", this.lastSpeed);
        tag.putInt("debugSpeed", this.debugSpeed);
        tag.putInt("dirLower", this.dirLower);
        tag.putInt("dirUpper", this.dirUpper);
        tag.putInt("dirRedstone", this.dirRedstone);
        tag.putInt("threshold", this.threshold);
        tag.putBoolean("window", this.window);
        if (this.particle != null) {
            tag.put("particle", this.particle.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.power = Math.max(0L, Math.min(maxPower(), tag.getLong("power")));
        this.temperature = tag.contains("temperature") ? tag.getFloat("temperature") : DEFAULT_TEMPERATURE;
        this.coldTank.load(tag.getCompound("cold"));
        this.hotTank.load(tag.getCompound("hot"));
        this.state = PAState.byOrdinal(tag.getInt("state"));
        this.lastSpeed = Math.max(0, tag.getInt("lastSpeed"));
        this.debugSpeed = Math.max(0, tag.getInt("debugSpeed"));
        this.dirLower = Math.floorMod(tag.getInt("dirLower"), 4);
        this.dirUpper = Math.floorMod(tag.getInt("dirUpper"), 4);
        this.dirRedstone = Math.floorMod(tag.getInt("dirRedstone"), 4);
        this.threshold = Mth.clamp(tag.getInt("threshold"), 0, 999_999_999);
        this.window = tag.getBoolean("window");
        this.particle = tag.contains("particle") ? Particle.load(registries, tag.getCompound("particle")) : null;
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

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[]{this.coldTank.type().oldId(), this.hotTank.type().oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        this.coldTank.conform(fluid, 0);
        sync();
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private record PAConnector(BlockPos pos, Direction face) {
    }

    public enum PAState {
        IDLE(0x8080ff),
        RUNNING(0xffff00),
        SUCCESS(0x00ff00),
        PAUSE_UNLOADED(0x808080),
        CRASH_DEFOCUS(0xff0000),
        CRASH_DERAIL(0xff0000),
        CRASH_CANNOT_ENTER(0xff0000),
        CRASH_NOCOOL(0xff0000),
        CRASH_NOPOWER(0xff0000),
        CRASH_NOCOIL(0xff0000),
        CRASH_OVERSPEED(0xff0000),
        CRASH_UNDERSPEED(0xff0000),
        CRASH_NORECIPE(0xff0000);

        public final int color;

        PAState(int color) {
            this.color = color;
        }

        private static PAState byOrdinal(int ordinal) {
            PAState[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
        }
    }

    private static final class Particle {
        private static final int MAX_DEFOCUS = 1000;
        private BlockPos pos;
        private Direction direction;
        private int momentum;
        private int defocus;
        private int distanceTraveled;
        private boolean invalid;
        private final ItemStack input1;
        private final ItemStack input2;

        private Particle(BlockPos pos, Direction direction, ItemStack input1, ItemStack input2) {
            this.pos = pos.immutable();
            this.direction = direction;
            this.input1 = input1.copy();
            this.input2 = input2.copy();
        }

        private void crash(ParticleAcceleratorBlockEntity source, PAState state) {
            this.invalid = true;
            source.state = state;
            source.sync();
        }

        private void move(ParticleAcceleratorBlockEntity source, BlockPos pos) {
            this.pos = pos.immutable();
            source.lastSpeed = this.momentum;
        }

        private void addDistance(int amount) {
            this.distanceTraveled += amount;
        }

        private void resetDistance() {
            this.distanceTraveled = 0;
        }

        private void defocus(ParticleAcceleratorBlockEntity source, int amount) {
            this.defocus += amount;
            if (this.defocus > MAX_DEFOCUS) {
                crash(source, PAState.CRASH_DEFOCUS);
            }
        }

        private void focus(int amount) {
            this.defocus = Math.max(0, this.defocus - amount);
        }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", this.pos.getX());
            tag.putInt("y", this.pos.getY());
            tag.putInt("z", this.pos.getZ());
            tag.putString("dir", this.direction.getName());
            tag.putInt("momentum", this.momentum);
            tag.putInt("defocus", this.defocus);
            tag.putInt("distance", this.distanceTraveled);
            tag.put("input1", this.input1.saveOptional(registries));
            tag.put("input2", this.input2.saveOptional(registries));
            return tag;
        }

        private static Particle load(HolderLookup.Provider registries, CompoundTag tag) {
            Direction direction = Direction.byName(tag.getString("dir"));
            if (direction == null || direction.getAxis().isVertical()) {
                direction = Direction.NORTH;
            }
            Particle particle = new Particle(
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    direction,
                    ItemStack.parseOptional(registries, tag.getCompound("input1")),
                    ItemStack.parseOptional(registries, tag.getCompound("input2"))
            );
            particle.momentum = Math.max(0, tag.getInt("momentum"));
            particle.defocus = Math.max(0, tag.getInt("defocus"));
            particle.distanceTraveled = Math.max(0, tag.getInt("distance"));
            return particle;
        }
    }

    private final class CoolantFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? coldTank.getFluidInTank(0) : hotTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? coldTank.capacity() : hotTank.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(def -> def == coldTank.type()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return coldTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(null);
            HbmFluidStack drained = hotTank.drain(requested, resource.getAmount(), action.simulate());
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return hotTank.drain(maxDrain, action);
        }
    }
}
