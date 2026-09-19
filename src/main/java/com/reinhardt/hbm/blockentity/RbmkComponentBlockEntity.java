package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.menu.RbmkComponentMenu;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public class RbmkComponentBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, NeutronFluxProvider, MenuProvider, PowerEndpoint {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_BUFFER = 1;
    public static final int AUTOLOADER_INPUT_START = 0;
    public static final int AUTOLOADER_INPUT_END = 9;
    public static final int AUTOLOADER_OUTPUT_START = 9;
    public static final int AUTOLOADER_OUTPUT_END = 18;
    public static final int SLOT_COUNT = 18;
    public static final int DATA_COUNT = 21;
    public static final int WATER_CAPACITY = 10_000;
    public static final int STEAM_CAPACITY = 1_000_000;
    public static final int HEATER_TANK_CAPACITY = 16_000;
    public static final int OUTGASSER_GAS_CAPACITY = 64_000;
    public static final int OUTGASSER_DURATION = 10_000;
    public static final int REASIM_INTERNAL_CAPACITY = 16_000;
    public static final int REASIM_PORT_CAPACITY = 32_000;
    /** Exact 1.7.10 RBMK cooler tank size (cold and warmed perfluoromethyl). */
    public static final int COOLER_TANK_CAPACITY = 4_000;
    public static final int CONSOLE_GRID_SIZE = 15;
    public static final int CONSOLE_COLUMN_COUNT = CONSOLE_GRID_SIZE * CONSOLE_GRID_SIZE;
    public static final int DISPLAY_GRID_SIZE = 7;
    public static final int DISPLAY_COLUMN_COUNT = DISPLAY_GRID_SIZE * DISPLAY_GRID_SIZE;
    private static final double HEAT_EXCHANGER_TU_PER_DEGREE = 2_000.0D;
    /** 1.7.10 ReaSim control rods consume 5,000 HE for every moving tick. */
    private static final long REASIM_CONTROL_CONSUMPTION = 5_000L;
    private static final long REASIM_CONTROL_MAX_POWER = REASIM_CONTROL_CONSUMPTION * 10L;

    private static final int[] FUEL_SLOT = {SLOT_FUEL};
    private static final int[] TWO_SLOTS = {SLOT_FUEL, SLOT_BUFFER};
    private static final int[] STORAGE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] AUTOLOADER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank water = new HbmFluidTank(HbmFluids.byName("water").orElse(HbmFluids.none()), WATER_CAPACITY);
    private final HbmFluidTank steam = new HbmFluidTank(HbmFluids.byName("steam").orElse(HbmFluids.none()), STEAM_CAPACITY);
    private final HbmFluidTank heaterInput = new HbmFluidTank(HbmFluids.byName("coolant").orElse(HbmFluids.none()), HEATER_TANK_CAPACITY);
    private final HbmFluidTank heaterOutput = new HbmFluidTank(HbmFluids.byName("coolant_hot").orElse(HbmFluids.none()), HEATER_TANK_CAPACITY);
    private final HbmFluidTank outgasserGas = new HbmFluidTank(HbmFluids.byName("tritium").orElse(HbmFluids.none()), OUTGASSER_GAS_CAPACITY);
    private final HbmFluidTank reasimInletWater = new HbmFluidTank(HbmFluids.byName("water").orElse(HbmFluids.none()), REASIM_PORT_CAPACITY);
    private final HbmFluidTank reasimOutletSteam = new HbmFluidTank(HbmFluids.byName("superhotsteam").orElse(HbmFluids.none()), REASIM_PORT_CAPACITY);
    private final HbmFluidTank coolerInput = new HbmFluidTank(HbmFluids.byName("perfluoromethyl_cold").orElse(HbmFluids.none()), COOLER_TANK_CAPACITY);
    private final HbmFluidTank coolerOutput = new HbmFluidTank(HbmFluids.byName("perfluoromethyl").orElse(HbmFluids.none()), COOLER_TANK_CAPACITY);
    private int coolerTimer;
    private final RbmkComponentBlockEntity[] coolerNeighbors = new RbmkComponentBlockEntity[25];

    private double heat;
    /** Flux buffered by this column during the current neutron pass. */
    private double lastFlux;
    private double lastFluxFastRatio;
    /** Flux emitted by a fuel rod for the next neutron pass. */
    private double emittedFlux;
    private double emittedFastRatio;
    /**
     * Legacy ReaSim stream fan angle (0..3, in nine-degree increments).  The
     * old tile chose this when it emitted its eight rays; it is deliberately
     * transient because the old stream list was not persisted either.
     */
    private int reasimRayOffset;
    private double outgasserProgress;
    private double controlLevel;
    private double lastControlLevel;
    private double targetControlLevel;
    private double startingControlLevel;
    private long controlPower;
    private boolean controlHasPower;
    private double autoLevelLower;
    private double autoLevelUpper;
    private double autoHeatLower;
    private double autoHeatUpper;
    private AutoControlFunction autoControlFunction = AutoControlFunction.LINEAR;
    private int colorGroup = -1;
    private int redstoneLevel;
    private int steamCompression;
    private int boilerConsumption;
    private int boilerOutput;
    private int reasimWater;
    private int reasimSteam;
    private int craneIndicator;
    private LidType lidType = LidType.NONE;
    private final int[] consoleKinds = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleHeat = new int[CONSOLE_COLUMN_COUNT];
    // The legacy console retained each column's raw double values in its
    // transient RBMKColumn NBT and only truncated after averaging a screen.
    // Keep separate raw scan buffers; the integer arrays remain the compact
    // network/save representation used by the client renderer.
    private final double[] consoleHeatRaw = new double[CONSOLE_COLUMN_COUNT];
    private final double[] consoleControlRaw = new double[CONSOLE_COLUMN_COUNT];
    private final int[] consoleMaxHeat = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFlux = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleControl = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleColorGroups = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleCraneIndicators = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFuelCoreHeat = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFuelHullHeat = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFuelDepletion = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFuelXenon = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFuelMaxHeat = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleWater = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleMaxWater = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleSteam = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleMaxSteam = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleSteamType = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleHeaterInput = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleHeaterOutput = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleHeaterMax = new int[CONSOLE_COLUMN_COUNT];
    private final int[] consoleFluxBuffer = new int[60];
    private final int[] consoleScreenTypes = new int[6];
    private final int[] consoleScreenDisplays = new int[6];
    private final int[][] consoleScreenColumns = new int[6][0];
    private final int[] displayKinds = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayHeat = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayMaxHeat = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayControl = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayColorGroups = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayCraneIndicators = new int[DISPLAY_COLUMN_COUNT];
    private final int[] displayFuelDepletion = new int[DISPLAY_COLUMN_COUNT];
    private int consoleTotalFlux;
    private int consoleRotation;
    private int displayRotation;
    private BlockPos craneCenter;
    private int craneSpanF;
    private int craneSpanB;
    private int craneSpanL;
    private int craneSpanR;
    private int craneHeight;
    private int craneRotationOffset;
    private double craneLastTiltFront;
    private double craneLastTiltLeft;
    private double craneTiltFront;
    private double craneTiltLeft;
    private double craneLastPosFront;
    private double craneLastPosLeft;
    private double cranePosFront;
    private double cranePosLeft;
    private double craneLastProgress = 1.0D;
    private double craneProgress = 1.0D;
    private boolean craneGoesDown;
    private ItemStack craneLoadedItem = ItemStack.EMPTY;
    private double craneLoadedHeat;
    private double craneLoadedEnrichment;
    private boolean craneInputUp;
    private boolean craneInputDown;
    private boolean craneInputLeft;
    private boolean craneInputRight;
    private boolean craneInputLoad;
    /** The player whose packet currently supplies the old first-player input. */
    @Nullable
    private UUID craneInputPlayer;
    private double autoloaderPiston;
    private double autoloaderLastPiston;
    private int autoloaderDelay;
    private int autoloaderCycle = 50;
    private boolean autoloaderRetracting = true;
    private boolean meltingDown;
    private BlockPos linkedReactor;
    private final net.minecraft.world.inventory.ContainerData dataAccess = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(heat);
                case 1 -> (int) Math.round(lastFlux);
                case 2 -> (int) Math.round(controlLevel * 1000.0D);
                case 3 -> (int) Math.round(targetControlLevel * 1000.0D);
                case 4 -> water.amount();
                case 5 -> steam.amount();
                case 6 -> steamCompression;
                case 7 -> boilerOutput;
                case 8 -> heaterInput.amount();
                case 9 -> heaterOutput.amount();
                case 10 -> outgasserGas.amount();
                case 11 -> (int) Math.round(outgasserProgress);
                case 12 -> (int) Math.round(autoloaderPiston * 1000.0D);
                case 13 -> autoloaderCycle;
                case 14 -> autoloaderRetracting ? 1 : 0;
                case 15 -> (int) Math.round(autoLevelUpper);
                case 16 -> (int) Math.round(autoLevelLower);
                case 17 -> (int) Math.round(autoHeatUpper);
                case 18 -> (int) Math.round(autoHeatLower);
                case 19 -> autoControlFunction.ordinal();
                case 20 -> colorGroup;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 2 -> controlLevel = Math.max(0.0D, Math.min(1.0D, value / 1000.0D));
                case 3 -> setTargetControlLevel(value / 1000.0D);
                case 6 -> {
                    setSteamCompression(value);
                    return;
                }
                case 13 -> autoloaderCycle = Math.max(5, Math.min(95, value));
                case 15 -> autoLevelUpper = Math.max(0.0D, Math.min(100.0D, value));
                case 16 -> autoLevelLower = Math.max(0.0D, Math.min(100.0D, value));
                case 17 -> autoHeatUpper = Math.max(0.0D, Math.min(9999.0D, value));
                case 18 -> autoHeatLower = Math.max(0.0D, Math.min(9999.0D, value));
                case 19 -> autoControlFunction = AutoControlFunction.byOrdinal(value);
                case 20 -> colorGroup = value < 0 ? -1 : Math.min(4, value);
                default -> {
                }
            }
            setChangedAndSync();
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RbmkComponentBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.RBMK_COMPONENT.get(), pos, blockState);
        // TileEntityRBMKConsole and TileEntityRBMKDisplay kept integer target
        // coordinates whose Java defaults were all zero.  A freshly placed
        // legacy panel therefore scanned around (0, 0, 0) until the RBMK
        // linker assigned another target; null is not the old state.
        if (blockState.getBlock() instanceof RbmkComponentBlock block
                && (block.kind() == RbmkComponentBlock.Kind.CONSOLE
                || block.kind() == RbmkComponentBlock.Kind.DISPLAY)) {
            linkedReactor = BlockPos.ZERO;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RbmkComponentBlockEntity rbmk) {
        if (level.isClientSide) {
            rbmk.clientTick(level, pos);
            return;
        }
        RbmkComponentBlock.Kind kind = rbmk.kind();
        if (rbmk.meltingDown && kind.isColumn()) {
            return;
        }
        /*
         * 1.7.10 ordering is deliberate: each specialised tile performs its
         * own work first, then calls TileEntityRBMKBase.updateEntity, which
         * decrements the crane indicator, moves heat and applies passive
         * cooling, and finally runs the optional ReaSim boiler.
         */
        if (kind.acceptsFuel()) {
            rbmk.tickFuel(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.MODERATOR) {
            // TileEntityRBMKModerator has no update override in 1.7.10.
        } else if (kind == RbmkComponentBlock.Kind.COOLER) {
            rbmk.tickCooler(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.BOILER) {
            rbmk.boilWater(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.HEATER) {
            rbmk.exchangeHeat(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
            rbmk.tickOutgasser(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            rbmk.tickAutoloader(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
            rbmk.tickReasimInlet(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
            rbmk.tickReasimOutlet(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.CONSOLE) {
            rbmk.tickConsole(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.DISPLAY) {
            rbmk.tickDisplay(level);
        } else if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            rbmk.tickCraneConsole(level, pos, state);
        } else if (kind.isControl()) {
            rbmk.tickControl(level);
        }
        // Legacy boiler/heater/cooler/outgasser tiles actively called
        // tryProvide() every tick, even when no new fluid was produced.
        // ReaSim outlets likewise pushed their tank to all six sides every
        // tick. Keep this pass outside the conversion methods so existing
        // output is still delivered on idle/full ticks.
        if (kind == RbmkComponentBlock.Kind.BOILER) {
            rbmk.pushRbmkFluid(level, rbmk.steam, false);
        } else if (kind == RbmkComponentBlock.Kind.HEATER) {
            rbmk.pushRbmkFluid(level, rbmk.heaterOutput, false);
        } else if (kind == RbmkComponentBlock.Kind.COOLER) {
            rbmk.pushRbmkFluid(level, rbmk.coolerOutput, false);
        } else if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
            rbmk.pushRbmkFluid(level, rbmk.outgasserGas, true);
        } else if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
            rbmk.pushReasimOutlet(level);
        }
        if (kind == RbmkComponentBlock.Kind.STORAGE && level.getGameTime() % 10L == 0L) {
            rbmk.compactStorage();
        }
        // TileEntityRBMKBase.updateEntity() decremented this immediately
        // after each specialised update and before moveHeat().
        if (kind.isColumn() && rbmk.craneIndicator > 0) {
            rbmk.craneIndicator--;
        }
        if (kind.isColumn()) {
            rbmk.diffuseHeat(level, pos);
            if (HbmConfig.RBMK_REASIM_BOILERS.get()) {
                // TileEntityRBMKBase boils ReaSim water after heat movement.
                rbmk.boilReasimWater();
            }
            // TileEntityRBMKRod called TileEntityRBMKBase.updateEntity() before
            // checking maxHeat().  The shared heat pass above is that base
            // update in the merged implementation, so perform the rod's
            // 1,500°C check only after diffusion and optional ReaSim boiling.
            if (kind.acceptsFuel() && rbmk.finishFuelTick(level, pos)) {
                return;
            }
            // TileEntityRBMKBase.networkPackNT() ran every server tick,
            // including for blank/moderator/reflector columns whose only
            // changing state is heat, ReaSim buffers, or the crane lamp.
            // Preserve that unconditional client update instead of relying
            // on a specialised ticker having called setChangedAndSync().
            rbmk.setChangedAndSync();
        }
        rbmk.redstoneLevel = Math.max(0, Math.min(15, (int) Math.round(rbmk.heat / 100.0D)));
    }

    public RbmkComponentBlock.Kind kind() {
        if (getBlockState().getBlock() instanceof RbmkComponentBlock block) {
            return block.kind();
        }
        return RbmkComponentBlock.Kind.BLANK;
    }

    private boolean isReasimControlKind() {
        RbmkComponentBlock.Kind kind = kind();
        return kind == RbmkComponentBlock.Kind.CONTROL_REASIM
                || kind == RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO;
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    /**
     * The legacy ReaSim rods exposed one and only one energy face: the block
     * below the rod.  Other RBMK components are not energy receivers.
     */
    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return isReasimControlKind()
                ? List.of(worldPosition.below().immutable())
                : List.of();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return isReasimControlKind()
                && machineSide == Direction.DOWN
                && worldPosition.below().equals(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return isReasimControlKind()
                ? Math.max(0L, REASIM_CONTROL_MAX_POWER - controlPower)
                : 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (!isReasimControlKind() || receivedInput <= 0L) {
            return;
        }
        controlPower = Math.min(REASIM_CONTROL_MAX_POWER, controlPower + receivedInput);
        setChanged();
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        // Matches TileEntityRBMKControl.getPriority(): LOW (the old comment
        // explicitly noted that this intentionally was not HIGH).
        return PowerEndpoint.ConnectionPriority.LOW;
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(controlPower + " / " + REASIM_CONTROL_MAX_POWER + " HE");
    }

    public int redstoneLevel() {
        return redstoneLevel;
    }

    @Override
    public NeutronFluxProvider.NeutronFlux neutronFluxSpectrum(Level level, BlockPos requesterPos) {
        RbmkComponentBlock.Kind kind = kind();
        if (kind == RbmkComponentBlock.Kind.REFLECTOR) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        if (kind == RbmkComponentBlock.Kind.ABSORBER) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        if (kind.isControl()) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        if (!kind.acceptsFuel() || items.get(SLOT_FUEL).isEmpty()) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        // A lid only suppresses radiation leakage in 1.7.10; it does not
        // attenuate the rod's neutron output.
        double flux = Math.max(0.0D, emittedFlux);
        return NeutronFluxProvider.NeutronFlux.fromRatio(flux, emittedFastRatio);
    }

    public boolean canUseLid() {
        return kind().isColumn() && !kind().isControl();
    }

    public boolean hasLid() {
        // TileEntityRBMKBase.hasLid() in 1.7.10 treated the fixed-lid
        // control variants as permanently covered: their
        // isLidRemovable() implementation returned false.  The modern
        // control renderer has its own lid, so keep that same logical state
        // even though controls never store a removable lid item.
        return kind().isControl() || lidType != LidType.NONE;
    }

    public LidType lidType() {
        return lidType;
    }

    public boolean installLid(LidType type) {
        if (!canUseLid() || type == LidType.NONE || hasLid()) {
            return false;
        }
        if (level != null && !level.isClientSide && !canPlaceLidCollisionDummy()) {
            return false;
        }
        lidType = type;
        updateLidCollisionDummy();
        setChangedAndSync();
        return true;
    }

    public ItemStack removeLidStack() {
        // Fixed-lid controls report hasLid() for radiation/collision logic,
        // but the old screwdriver could not remove their lid.
        if (!canUseLid() || lidType == LidType.NONE) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(lidType == LidType.GLASS
                ? com.reinhardt.hbm.registry.HbmItems.RBMK_LID_GLASS.get()
                : com.reinhardt.hbm.registry.HbmItems.RBMK_LID.get());
        lidType = LidType.NONE;
        updateLidCollisionDummy();
        setChangedAndSync();
        return stack;
    }

    private boolean canPlaceLidCollisionDummy() {
        if (level == null) {
            return true;
        }
        BlockPos lidPos = worldPosition.above(RbmkComponentBlock.columnHeight(level));
        BlockState state = level.getBlockState(lidPos);
        if (state.isAir() || state.canBeReplaced()) {
            return true;
        }
        return state.is(HbmBlocks.MACHINE_DUMMY.get())
                && level.getBlockEntity(lidPos) instanceof MachineDummyBlockEntity dummy
                && dummy.getCorePos().equals(worldPosition);
    }

    private void updateLidCollisionDummy() {
        if (level == null || level.isClientSide || !canUseLid()) {
            return;
        }
        BlockPos lidPos = worldPosition.above(RbmkComponentBlock.columnHeight(level));
        if (hasLid()) {
            BlockState state = level.getBlockState(lidPos);
            if (state.isAir() || state.canBeReplaced()) {
                level.setBlock(lidPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            }
            if (level.getBlockEntity(lidPos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(worldPosition);
            }
        } else if (level.getBlockState(lidPos).is(HbmBlocks.MACHINE_DUMMY.get())
                && level.getBlockEntity(lidPos) instanceof MachineDummyBlockEntity dummy
                && dummy.getCorePos().equals(worldPosition)) {
            MachineDummyBlock.runWithoutCoreDestroy(() -> level.removeBlock(lidPos, false));
        }
        BlockState lidState = level.getBlockState(lidPos);
        level.sendBlockUpdated(lidPos, lidState, lidState, Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(lidPos, lidState.getBlock());
    }

    public boolean handleItemUse(Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        RbmkComponentBlock.Kind kind = kind();
        if (kind.acceptsFuel() && stack.getItem() instanceof RbmkFuelRodItem) {
            if (level != null && !level.isClientSide && items.get(SLOT_FUEL).isEmpty()) {
                ItemStack inserted = stack.copyWithCount(1);
                items.set(SLOT_FUEL, inserted);
                stack.shrink(1);
                setChangedAndSync();
            }
            return true;
        }
        return false;
    }

    public boolean handleEmptyHand(Player player) {
        // The 1.7.10 RBMK block activators delegated to openInv.  Sneaking
        // therefore consumed the click without unloading fuel, cycling a
        // control target, or changing boiler compression; these actions were
        // only available through their GUI/console controls.
        return false;
    }

    public void printInfo(Player player) {
        RbmkComponentBlock.Kind kind = kind();
        if (kind == RbmkComponentBlock.Kind.HEATER) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.heater.input",
                    heaterInput.amount(),
                    heaterInput.capacity()
            ).append(Component.literal("  ")).append(Component.translatable(
                    "rbmk.heater.output",
                    heaterOutput.amount(),
                    heaterOutput.capacity()
            )), true);
            return;
        }
        if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.outgasser.gas",
                    outgasserGas.amount(),
                    outgasserGas.capacity()
            ).append(Component.literal("  ")).append(Component.translatable(
                    "rbmk.outgasser.progress",
                    Math.round(outgasserProgress),
                    OUTGASSER_DURATION
            )), true);
            return;
        }
        if (kind == RbmkComponentBlock.Kind.BOILER) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.boiler.water",
                    water.amount(),
                    water.capacity()
            ).append(Component.literal("  ")).append(Component.translatable(
                    "rbmk.boiler.steam",
                    steam.amount(),
                    steam.capacity()
            )), true);
            return;
        }
        if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.reasim.inlet",
                    reasimInletWater.amount(),
                    reasimInletWater.capacity()
            ), true);
            return;
        }
        if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.reasim.outlet",
                    reasimOutletSteam.amount(),
                    reasimOutletSteam.capacity()
            ), true);
            return;
        }
        if (kind.isColumn() && HbmConfig.RBMK_REASIM_BOILERS.get()) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.reasim.internal",
                    reasimWater,
                    REASIM_INTERNAL_CAPACITY,
                    reasimSteam,
                    REASIM_INTERNAL_CAPACITY
            ), true);
            return;
        }
        if (kind.isControl()) {
            player.displayClientMessage(Component.translatable(
                    "rbmk.control.level",
                    Math.round(controlLevel * 100.0D) + "% -> " + Math.round(targetControlLevel * 100.0D) + "%"
            ), true);
            return;
        }
        player.displayClientMessage(Component.translatable("rbmk.heat", Math.round(heat)), true);
    }

    public double heat() {
        return heat;
    }

    public double lastFlux() {
        return lastFlux;
    }

    public double controlLevel() {
        return controlLevel;
    }

    public double controlLevel(float partialTick) {
        return lastControlLevel + (controlLevel - lastControlLevel) * partialTick;
    }

    public double targetControlLevel() {
        return targetControlLevel;
    }

    public double maxConsoleHeat() {
        // TileEntityRBMKBase.maxHeat() is the column's hull threshold and is
        // fixed at 1500°C in 1.7.10.  A fuel rod's own melting point belongs
        // only to the c_maxHeat/fuel tooltip field, not the column indicator.
        return 1500.0D;
    }

    public int waterAmount() {
        return water.amount();
    }

    public int steamAmount() {
        return steam.amount();
    }

    public int colorGroup() {
        return colorGroup;
    }

    public ItemStack fuelStack() {
        return items.get(SLOT_FUEL).copy();
    }

    public boolean hasFuelRod() {
        return items.get(SLOT_FUEL).getItem() instanceof RbmkFuelRodItem;
    }

    public int fuelChannelColor() {
        ItemStack fuel = items.get(SLOT_FUEL);
        return fuel.getItem() instanceof RbmkFuelRodItem rod ? rod.channelColor(fuel) : 0x304825;
    }

    public HbmFluidTank waterTank() {
        return water;
    }

    public HbmFluidTank steamTank() {
        return steam;
    }

    public HbmFluidTank heaterInputTank() {
        return heaterInput;
    }

    public HbmFluidTank heaterOutputTank() {
        return heaterOutput;
    }

    public HbmFluidTank outgasserGasTank() {
        return outgasserGas;
    }

    /** Receiving (cold) coolant tank of the legacy RBMK cooler. */
    public HbmFluidTank coolerInputTank() {
        return coolerInput;
    }

    /** Sending (warmed) coolant tank of the legacy RBMK cooler. */
    public HbmFluidTank coolerOutputTank() {
        return coolerOutput;
    }

    public double outgasserProgress() {
        return outgasserProgress;
    }

    public int reasimWater() {
        return reasimWater;
    }

    public int reasimSteam() {
        return reasimSteam;
    }

    public Component steamTypeName() {
        return Component.translatable(steamFluid().translationKey());
    }

    public Map<String, String> diagnosticData() {
        TreeMap<String, String> data = new TreeMap<>();
        data.put("heat", stripNumber(heat));
        data.put("reasimWater", Integer.toString(reasimWater));
        data.put("reasimSteam", Integer.toString(reasimSteam));

        if (kind().acceptsFuel()) {
            data.put("fluxSlow", stripNumber(lastFlux * (1.0D - lastFluxFastRatio)));
            data.put("fluxFast", stripNumber(lastFlux * lastFluxFastRatio));
            ItemStack fuel = items.get(SLOT_FUEL);
            boolean hasFuel = fuel.getItem() instanceof RbmkFuelRodItem;
            data.put("hasRod", Boolean.toString(hasFuel));
            if (hasFuel && fuel.getItem() instanceof RbmkFuelRodItem rod) {
                data.put("f_yield", stripNumber((1.0D - RbmkFuelRodItem.depletion(fuel)) * 100.0D) + "%");
                data.put("f_xenon", stripNumber(RbmkFuelRodItem.xenon(fuel) * 100.0D) + "%");
                data.put("f_heat", stripNumber(RbmkFuelRodItem.coreHeat(fuel))
                        + " / " + stripNumber(RbmkFuelRodItem.hullHeat(fuel))
                        + " / " + stripNumber(rod.meltingPoint(fuel)));
            }
        }

        if (kind().isControl()) {
            data.put("level", stripNumber(controlLevel));
            data.put("targetLevel", stripNumber(targetControlLevel));
            if (kind().isAutomaticControl()) {
                data.put("levelLower", stripNumber(autoLevelLower));
                data.put("levelUpper", stripNumber(autoLevelUpper));
                data.put("heatLower", stripNumber(autoHeatLower));
                data.put("heatUpper", stripNumber(autoHeatUpper));
                data.put("function", Integer.toString(autoControlFunction.ordinal()));
            } else if (isManualControlKind()) {
                data.put("startingLevel", stripNumber(startingControlLevel));
                data.put("mult", stripNumber(controlMultiplier()));
                if (colorGroup >= 0) {
                    data.put("color", Integer.toString(colorGroup));
                }
            }
        }

        if (kind() == RbmkComponentBlock.Kind.BOILER) {
            putTankDiagnostic(data, "feed", water);
            putTankDiagnostic(data, "steam", steam);
        } else if (kind() == RbmkComponentBlock.Kind.HEATER) {
            putTankDiagnostic(data, "feed", heaterInput);
            putTankDiagnostic(data, "steam", heaterOutput);
        } else if (kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            data.put("progress", stripNumber(outgasserProgress));
            putTankDiagnostic(data, "gas", outgasserGas);
        } else if (kind() == RbmkComponentBlock.Kind.COOLER) {
            putTankDiagnostic(data, "coolantIn", coolerInput);
            putTankDiagnostic(data, "coolantOut", coolerOutput);
        }
        return data;
    }

    private static void putTankDiagnostic(Map<String, String> data, String key, HbmFluidTank tank) {
        data.put(key + "_amt", Integer.toString(tank.amount()));
        data.put(key + "_max", Integer.toString(tank.capacity()));
        data.put(key + "_type", Integer.toString(tank.type().oldId()));
        data.put(key + "_p", "0");
    }

    private static String stripNumber(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public void applyAutoControl(int function, int levelUpper, int levelLower, int heatUpper, int heatLower,
                                 boolean updateParameters) {
        if (!kind().isAutomaticControl()) {
            return;
        }
        // TileEntityRBMKControlAuto.receiveControl() treated the presence of
        // "function" as an exclusive function update.  Its other packet
        // shape changed only the four thresholds.  Do not overwrite fields
        // that the corresponding old packet did not carry.
        if (updateParameters) {
            autoLevelUpper = Math.max(0.0D, Math.min(100.0D, levelUpper));
            autoLevelLower = Math.max(0.0D, Math.min(100.0D, levelLower));
            autoHeatUpper = Math.max(0.0D, Math.min(9999.0D, heatUpper));
            autoHeatLower = Math.max(0.0D, Math.min(9999.0D, heatLower));
        } else {
            autoControlFunction = AutoControlFunction.byOrdinal(function);
        }
        setChangedAndSync();
    }

    public void applyManualControlLevel(int percent) {
        if (!isManualControlKind()) {
            return;
        }
        setTargetControlLevel(Math.max(0, Math.min(100, percent)) / 100.0D);
        setChangedAndSync();
    }

    public void applyManualControlColor(int color) {
        if (!isManualControlKind()) {
            return;
        }
        int clamped = Math.max(0, Math.min(4, color));
        colorGroup = colorGroup == clamped ? -1 : clamped;
        setChangedAndSync();
    }

    public int consoleKind(int index) {
        return index >= 0 && index < consoleKinds.length ? consoleKinds[index] : -1;
    }

    public int consoleHeat(int index) {
        return index >= 0 && index < consoleHeat.length ? consoleHeat[index] : 0;
    }

    public int consoleMaxHeat(int index) {
        return index >= 0 && index < consoleMaxHeat.length ? consoleMaxHeat[index] : 1500;
    }

    public int consoleFlux(int index) {
        return index >= 0 && index < consoleFlux.length ? consoleFlux[index] : 0;
    }

    public int consoleControl(int index) {
        return index >= 0 && index < consoleControl.length ? consoleControl[index] : 0;
    }

    public int consoleColorGroup(int index) {
        return index >= 0 && index < consoleColorGroups.length ? consoleColorGroups[index] : -1;
    }

    public int consoleCraneIndicator(int index) {
        return index >= 0 && index < consoleCraneIndicators.length ? consoleCraneIndicators[index] : 0;
    }

    public int consoleFuelCoreHeat(int index) {
        return index >= 0 && index < consoleFuelCoreHeat.length ? consoleFuelCoreHeat[index] : 0;
    }

    public int consoleFuelHullHeat(int index) {
        return index >= 0 && index < consoleFuelHullHeat.length ? consoleFuelHullHeat[index] : 0;
    }

    public int consoleFuelDepletion(int index) {
        return index >= 0 && index < consoleFuelDepletion.length ? consoleFuelDepletion[index] : 0;
    }

    public int consoleFuelXenon(int index) {
        return index >= 0 && index < consoleFuelXenon.length ? consoleFuelXenon[index] : 0;
    }

    public int consoleFuelMaxHeat(int index) {
        return index >= 0 && index < consoleFuelMaxHeat.length ? consoleFuelMaxHeat[index] : 0;
    }

    public int consoleWater(int index) {
        return index >= 0 && index < consoleWater.length ? consoleWater[index] : 0;
    }

    public int consoleMaxWater(int index) {
        return index >= 0 && index < consoleMaxWater.length ? consoleMaxWater[index] : 0;
    }

    public int consoleSteam(int index) {
        return index >= 0 && index < consoleSteam.length ? consoleSteam[index] : 0;
    }

    public int consoleMaxSteam(int index) {
        return index >= 0 && index < consoleMaxSteam.length ? consoleMaxSteam[index] : 0;
    }

    public int consoleSteamType(int index) {
        return index >= 0 && index < consoleSteamType.length ? consoleSteamType[index] : 0;
    }

    public int consoleHeaterInput(int index) {
        return index >= 0 && index < consoleHeaterInput.length ? consoleHeaterInput[index] : 0;
    }

    public int consoleHeaterOutput(int index) {
        return index >= 0 && index < consoleHeaterOutput.length ? consoleHeaterOutput[index] : 0;
    }

    public int consoleHeaterMax(int index) {
        return index >= 0 && index < consoleHeaterMax.length ? consoleHeaterMax[index] : 0;
    }

    public int consoleTotalFlux() {
        return consoleTotalFlux;
    }

    public int consoleFluxHistory(int index) {
        return index >= 0 && index < consoleFluxBuffer.length ? consoleFluxBuffer[index] : 0;
    }

    public int consoleFluxHistorySize() {
        return consoleFluxBuffer.length;
    }

    public int consoleScreenType(int index) {
        return index >= 0 && index < consoleScreenTypes.length ? consoleScreenTypes[index] : 0;
    }

    public int consoleScreenDisplay(int index) {
        return index >= 0 && index < consoleScreenDisplays.length ? consoleScreenDisplays[index] : 0;
    }

    public int displayKind(int index) {
        return index >= 0 && index < displayKinds.length ? displayKinds[index] : -1;
    }

    public int displayHeat(int index) {
        return index >= 0 && index < displayHeat.length ? displayHeat[index] : 0;
    }

    public int displayMaxHeat(int index) {
        return index >= 0 && index < displayMaxHeat.length ? displayMaxHeat[index] : 1500;
    }

    public int displayControl(int index) {
        return index >= 0 && index < displayControl.length ? displayControl[index] : 0;
    }

    public int displayColorGroup(int index) {
        return index >= 0 && index < displayColorGroups.length ? displayColorGroups[index] : -1;
    }

    public int displayCraneIndicator(int index) {
        return index >= 0 && index < displayCraneIndicators.length ? displayCraneIndicators[index] : 0;
    }

    public int displayFuelDepletion(int index) {
        return index >= 0 && index < displayFuelDepletion.length ? displayFuelDepletion[index] : 0;
    }

    public boolean craneIsSetUp() {
        return craneCenter != null;
    }

    public BlockPos craneCenter() {
        return craneCenter;
    }

    public int craneSpanF() {
        return craneSpanF;
    }

    public int craneSpanB() {
        return craneSpanB;
    }

    public int craneSpanL() {
        return craneSpanL;
    }

    public int craneSpanR() {
        return craneSpanR;
    }

    public int craneHeight() {
        return craneHeight;
    }

    public int craneRotationOffset() {
        return craneRotationOffset;
    }

    public double craneTiltFront(float partialTick) {
        return craneLastTiltFront + (craneTiltFront - craneLastTiltFront) * partialTick;
    }

    public double craneTiltLeft(float partialTick) {
        return craneLastTiltLeft + (craneTiltLeft - craneLastTiltLeft) * partialTick;
    }

    public double cranePosFront(float partialTick) {
        return craneLastPosFront + (cranePosFront - craneLastPosFront) * partialTick;
    }

    public double cranePosLeft(float partialTick) {
        return craneLastPosLeft + (cranePosLeft - craneLastPosLeft) * partialTick;
    }

    public double craneProgress(float partialTick) {
        return craneLastProgress + (craneProgress - craneLastProgress) * partialTick;
    }

    public boolean craneHasLoadedItem() {
        return !craneLoadedItem.isEmpty();
    }

    public boolean craneIsLoading() {
        return craneProgress != 1.0D;
    }

    public double craneLoadedHeat() {
        return craneLoadedHeat;
    }

    public double craneLoadedEnrichment() {
        return craneLoadedEnrichment;
    }

    public boolean craneAboveValidTarget() {
        if (level == null) {
            return false;
        }
        RbmkComponentBlockEntity target = craneTargetColumn(level);
        // 1.7.10 isAboveValidTarget() only checked whether the column
        // implemented IRBMKLoadable.  It did not require the current load or
        // unload operation to be possible (the lamp therefore stays green for
        // a full/empty loadable column, exactly as it did in the old renderer).
        return isCraneLoadableTarget(target);
    }

    public boolean isPlayerInCraneOperationArea(Player player) {
        if (kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return false;
        }
        // 1.7.10 used getEntitiesWithinAABB, which tests the player's whole
        // bounding box rather than only its feet position.
        return player.getBoundingBox().intersects(craneOperationArea());
    }

    @Nullable
    public BlockPos consoleTarget() {
        return linkedReactor;
    }

    public void setConsoleTarget(BlockPos target) {
        if (!kind().isConsole()) {
            return;
        }
        linkedReactor = target.immutable();
        if (level != null && !level.isClientSide) {
            if (kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                setupCrane(target);
            }
        }
        setChangedAndSync();
    }

    public void applyConsoleControl(int action, int value, int[] selected) {
        if (level == null || level.isClientSide || kind() != RbmkComponentBlock.Kind.CONSOLE) {
            return;
        }
        if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_AZ5) {
            level.playSound(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    HbmSoundEvents.RBMK_SHUTDOWN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            applyConsoleToControls(-1, 0);
            return;
        }
        if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_TOGGLE_SCREEN) {
            int slot = Math.floorMod(value, consoleScreenTypes.length);
            consoleScreenTypes[slot] = (consoleScreenTypes[slot] + 1) % ConsoleScreenType.values().length;
            setChangedAndSync();
            return;
        }
        if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_ASSIGN_SCREEN) {
            int slot = Math.floorMod(value, consoleScreenTypes.length);
            consoleScreenColumns[slot] = validConsoleIndices(selected);
            setChangedAndSync();
            return;
        }
        for (int index : selected) {
            if (index < 0 || index >= CONSOLE_COLUMN_COUNT || linkedReactor == null) {
                continue;
            }
            BlockPos target = consoleIndexPos(index);
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, target);
            if (rbmk == null) {
                continue;
            }
            if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_SET_CONTROL && rbmk.isManualControlKind()) {
                rbmk.setTargetControlLevel(value / 100.0D);
                rbmk.setChangedAndSync();
            } else if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_CYCLE_COMPRESSOR && rbmk.kind() == RbmkComponentBlock.Kind.BOILER) {
                rbmk.cycleSteamCompression();
                rbmk.setChangedAndSync();
            } else if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_ASSIGN_COLOR && rbmk.isManualControlKind()) {
                int clamped = Math.max(0, Math.min(4, value));
                // The legacy console's assignColor packet always assigned the
                // requested group; only the standalone control GUI toggled a
                // same-color click back to unassigned.
                rbmk.colorGroup = clamped;
                rbmk.setChangedAndSync();
            }
        }
        // The legacy console refreshed its cached column/screen data only on
        // the ten-tick rescan cadence.  Control packets changed the selected
        // rods/screens immediately but did not force an out-of-band rescan.
        setChangedAndSync();
    }

    public void rotateConsoleScan() {
        if (kind() != RbmkComponentBlock.Kind.CONSOLE) {
            return;
        }
        consoleRotation = (consoleRotation + 1) & 3;
        setChangedAndSync();
    }

    public void rotateDisplayScan() {
        if (kind() != RbmkComponentBlock.Kind.DISPLAY) {
            return;
        }
        displayRotation = (displayRotation + 1) & 3;
        setChangedAndSync();
    }

    public void cycleCraneRotation() {
        if (kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return;
        }
        craneRotationOffset = (craneRotationOffset + 90) % 360;
        setChangedAndSync();
    }

    public void applyCraneInput(Player player, boolean up, boolean down, boolean left, boolean right, boolean load) {
        if (level == null || level.isClientSide || kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            clearCraneInput();
            return;
        }
        // TileEntityCraneConsole selected players.get(0) from the AABB each
        // tick.  Accept only that same first player, otherwise a second player
        // could overwrite the legacy operator's key state.
        Player first = firstCraneOperator();
        if (first == null) {
            clearCraneInput();
            return;
        }
        if (!first.getUUID().equals(player.getUUID())) {
            return;
        }
        craneInputPlayer = first.getUUID();
        craneInputUp = up;
        craneInputDown = down;
        craneInputLeft = left;
        craneInputRight = right;
        craneInputLoad = load;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        if (!kind().hasMenu()) {
            return null;
        }
        return new RbmkComponentMenu(containerId, playerInventory, this, this.dataAccess, worldPosition);
    }

    public net.minecraft.world.inventory.ContainerData dataAccess() {
        return dataAccess;
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(worldPosition, side);
    }

    /**
     * Resolve the handler at the physical block position queried by a pipe.
     * RBMK fluid machines did not expose one unrestricted tank on every face
     * in 1.7.10: their subscriptions and provisions were registered at the
     * exact bottom/top/loader coordinates.  Keeping that coordinate here is
     * also what lets a column dummy represent its top output port.
     */
    @Nullable
    public IFluidHandler fluidHandler(BlockPos exposedPos, @Nullable Direction side) {
        RbmkComponentBlock.Kind kind = kind();
        if (!kind.hasFluid() && kind != RbmkComponentBlock.Kind.OUTGASSER) {
            return null;
        }
        if (!hasPotentialFluidPort(exposedPos, side)) {
            return null;
        }
        return new RbmkFluidHandler(exposedPos.immutable(), side);
    }

    private boolean hasPotentialFluidPort(BlockPos exposedPos, @Nullable Direction side) {
        RbmkComponentBlock.Kind kind = kind();
        if (side == null || kind == RbmkComponentBlock.Kind.STEAM_INLET
                || kind == RbmkComponentBlock.Kind.STEAM_OUTLET
                || kind == RbmkComponentBlock.Kind.LOADER) {
            return true;
        }
        if (kind == RbmkComponentBlock.Kind.BOILER
                || kind == RbmkComponentBlock.Kind.HEATER
                || kind == RbmkComponentBlock.Kind.COOLER
                || kind == RbmkComponentBlock.Kind.OUTGASSER) {
            if (exposedPos.equals(worldPosition) && side == Direction.DOWN) {
                return true;
            }
            if (exposedPos.equals(worldPosition.above(RbmkComponentBlock.columnHeight(level)))
                    && side == Direction.DOWN) {
                return true;
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER
                    && !hasRbmkLoaderBelow()
                    && exposedPos.equals(worldPosition.below())
                    && side == Direction.UP) {
                return true;
            }
            return isAnyLoaderOutputPort(exposedPos, side);
        }
        return false;
    }

    private boolean hasRbmkLoaderBelow() {
        return level != null && (level.getBlockState(worldPosition.below()).is(HbmBlocks.RBMK_LOADER.get())
                || level.getBlockState(worldPosition.below(2)).is(HbmBlocks.RBMK_LOADER.get()));
    }

    private boolean isAnyLoaderOutputPort(BlockPos exposedPos, Direction side) {
        if (level == null) {
            return false;
        }
        BlockPos loader = null;
        if (level.getBlockState(worldPosition.below()).is(HbmBlocks.RBMK_LOADER.get())) {
            loader = worldPosition.below();
        } else if (level.getBlockState(worldPosition.below(2)).is(HbmBlocks.RBMK_LOADER.get())) {
            loader = worldPosition.below(2);
        }
        if (loader == null) {
            return false;
        }
        return (exposedPos.equals(loader.east()) && side == Direction.WEST)
                || (exposedPos.equals(loader.west()) && side == Direction.EAST)
                || (exposedPos.equals(loader.south()) && side == Direction.NORTH)
                || (exposedPos.equals(loader.north()) && side == Direction.SOUTH)
                || (exposedPos.equals(loader.below()) && side == Direction.UP);
    }

    private void tickFuel(Level level, BlockPos pos) {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.isEmpty()) {
            // The 1.7.10 rod has no special empty-channel cooling.  It calls
            // the common base ticker, whose passive-cooling step runs after
            // this method.
            lastFlux = 0.0D;
            lastFluxFastRatio = 0.0D;
            emittedFlux = 0.0D;
            emittedFastRatio = 0.0D;
            reasimRayOffset = 0;
            return;
        }
        NeutronFluxProvider.NeutronFlux flux = rbmkIncomingSpectrum(level, pos);
        lastFlux = flux.total();
        lastFluxFastRatio = flux.fastRatio();
        if (!(fuel.getItem() instanceof RbmkFuelRodItem rod)) {
            emittedFlux = 0.0D;
            emittedFastRatio = 0.0D;
            reasimRayOffset = 0;
            return;
        }
        RbmkFuelRodItem.FuelTickResult result = rod.burnAndProvideHeat(
                fuel,
                rod.inputFlux(fuel, flux),
                heat,
                reactivityMod(),
                fuelDiffusionMod(),
                fuelHeatProvision(),
                depletionEnabled(),
                xenonEnabled()
        );
        emittedFlux = result.outputFlux();
        emittedFastRatio = rod.outputFastRatio(fuel);
        // TileEntityRBMKRodReaSim chose one random 0/9/18/27 degree starting
        // angle every time it emitted a non-zero fan of eight streams.
        if (isReasimFuelKind(kind()) && emittedFlux > 0.0D) {
            reasimRayOffset = level.random.nextInt(4);
        } else {
            reasimRayOffset = 0;
        }
        // All legacy rod variants add provideHeat() directly; moderator and
        // ReaSim variants do not receive an extra multiplier here.
        heat += result.providedHeat();
        if (!hasLid() && flux.total() > 0.0D && level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(pos, flux.total() * 0.05D);
        }
        // The common ticker performs the rod's maxHeat/cap handling after
        // this method, matching the old specialised-update -> base-update ->
        // failure-check order.
    }

    /**
     * Finish a fuel-rod tick after the merged implementation has run the
     * legacy TileEntityRBMKBase heat movement and ReaSim boiler pass.
     *
     * @return true when the column entered the old overheat return path
     */
    private boolean finishFuelTick(Level level, BlockPos pos) {
        // Column failure is governed by TileEntityRBMKBase.maxHeat() (1500°C)
        // in 1.7.10; the fuel rod's own melting point only governs its hull
        // state and is not the channel meltdown threshold.
        if (heat > 1500.0D) {
            if (meltdownsDisabled()) {
                if (level instanceof ServerLevel serverLevel) {
                    // 1.7.10 used ParticleUtil.spawnGasFlame here (not the
                    // RBMK fire-sheet particle), with a straight upward
                    // velocity of 0.2.
                    serverLevel.sendParticles(HbmParticleTypes.GAS_FLARE_FLAME.get(), pos.getX() + 0.5D, pos.getY() + RbmkComponentBlock.columnHeight(level) - 0.5D, pos.getZ() + 0.5D, 1, 0.0D, 0.2D, 0.0D, 0.0D);
                }
            } else {
                meltdown(level, pos);
            }
            lastFlux = 0.0D;
            lastFluxFastRatio = 0.0D;
            emittedFlux = 0.0D;
            emittedFastRatio = 0.0D;
            reasimRayOffset = 0;
            setChangedAndSync();
            return true;
        }
        // TileEntityRBMKRod caps an overheated channel at 10,000°C after the
        // 1,500°C meltdown check (the cap is reachable when meltdown rules
        // are disabled). Keep the ordering identical to 1.7.10.
        if (heat > 10_000.0D) {
            heat = 10_000.0D;
        }
        return false;
    }

    /**
     * The 1.7.10 neutron walker converted every stream coordinate with
     * {@code floor(0.5 + component * distance)}.  Keeping that operation in
     * one place is important for the diagonal ReaSim fan: rounding the vector
     * with block-position helpers changes which columns receive a stream.
     */
    private static int legacyRayCoordinate(double component, int distance) {
        return (int) Math.floor(0.5D + component * distance);
    }

    private static boolean isReasimFuelKind(RbmkComponentBlock.Kind kind) {
        return kind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM
                || kind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD;
    }

    /** TileEntityRBMKBase.isModerated(), including the moderated control rod. */
    private static boolean isModeratedKind(RbmkComponentBlock.Kind kind) {
        return kind == RbmkComponentBlock.Kind.MODERATOR
                || kind == RbmkComponentBlock.Kind.FUEL_ROD_MOD
                || kind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD
                || kind == RbmkComponentBlock.Kind.CONTROL_MOD;
    }

    private NeutronFluxProvider.NeutronFlux rbmkIncomingSpectrum(Level level, BlockPos pos) {
        NeutronFluxProvider.NeutronFlux total = NeutronFluxProvider.NeutronFlux.ZERO;
        int range = Math.max(1, Math.min(100, HbmConfig.RBMK_FLUX_RANGE.get()));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            NeutronFluxProvider.NeutronFlux directional = scanRbmkFluxLine(level, pos, direction, range);
            total = total.add(directional);
        }

        // ReaSim did not emit cardinal streams.  It selected one of four
        // nine-degree offsets and emitted eight rays separated by 45 degrees;
        // each ray carried 75% of the rod output.  Inverting those exact
        // floor-rounded paths here gives the same result while retaining the
        // modern buffered (target-driven) neutron pass.
        for (int rayOffset = 0; rayOffset < 4; rayOffset++) {
            double offset = Math.toRadians(rayOffset * 9.0D);
            for (int ray = 0; ray < 8; ray++) {
                double angle = offset + Math.toRadians(ray * 45.0D);
                double forwardX = Math.cos(angle);
                // Vec3.rotateAroundYDeg in 1.7.10 uses a negative Z sine.
                double forwardZ = -Math.sin(angle);
                total = total.add(scanRbmkRay(level, pos,
                        -forwardX, -forwardZ, forwardX, forwardZ,
                        range, rayOffset));
            }
        }
        return total;
    }

    private NeutronFluxProvider.NeutronFlux scanRbmkFluxLine(Level level, BlockPos origin, Direction direction, int range) {
        for (int distance = 1; distance <= range; distance++) {
            BlockPos cursor = origin.relative(direction, distance);
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, cursor);
            if (rbmk != null) {
                RbmkComponentBlock.Kind kind = rbmk.kind();
                if (kind == RbmkComponentBlock.Kind.REFLECTOR) {
                    // A reflector with efficiency below one attenuates the
                    // outbound stream and lets it continue.  Only an exact
                    // one reflects the stream back to its origin in 1.7.10.
                    if (reflectorEfficiency() == 1.0D) {
                        return reflectedFluxToSelf(level, origin, direction, distance);
                    }
                    continue;
                }
                if (kind.acceptsFuel()) {
                    // TileEntityRBMKRod only received and stopped a stream
                    // when it actually contained a rod; an empty channel was
                    // transparent to the legacy neutron walker.
                    if (rbmk.items.get(SLOT_FUEL).isEmpty()) {
                        continue;
                    }
                    // ReaSim rods emit only their eight diagonal streams.  A
                    // cardinal stream therefore stops at a loaded ReaSim rod
                    // instead of incorrectly using its full output here.
                    if (isReasimFuelKind(kind)) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    NeutronFluxProvider.NeutronFlux emitted = rbmk.neutronFluxSpectrum(level, origin);
                    return streamFlux(level, cursor, direction.getOpposite(), distance, emitted, true);
                }
                if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                    // An outgasser consumes a stream only while a recipe can
                    // actually run.  The legacy neutron walker continued
                    // past an idle/full outgasser instead of treating it as
                    // an unconditional barrier.
                    RecipeHolder<RbmkOutgasserRecipe> recipe = outgasserRecipe(level, rbmk.items.get(SLOT_FUEL));
                    if (recipe != null && rbmk.canProcessOutgasser(recipe.value())) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    continue;
                }
                continue;
            }
            if (opaqueColumnHits(level, cursor) >= RbmkComponentBlock.columnHeight(level)) {
                return NeutronFluxProvider.NeutronFlux.ZERO;
            }
        }
        return NeutronFluxProvider.NeutronFlux.ZERO;
    }

    /**
     * Scan one of the eight diagonal ReaSim rays.  The scan is performed from
     * a possible receiver back toward a possible source; the source is
     * accepted only when its transient random nine-degree fan offset matches
     * this ray.  Every coordinate uses the old floor(0.5 + vector * i)
     * conversion, including the reverse lookup.
     */
    private NeutronFluxProvider.NeutronFlux scanRbmkRay(
            Level level,
            BlockPos origin,
            double reverseX,
            double reverseZ,
            double forwardX,
            double forwardZ,
            int range,
            int rayOffset) {
        for (int distance = 1; distance <= range; distance++) {
            BlockPos cursor = origin.offset(
                    legacyRayCoordinate(reverseX, distance),
                    0,
                    legacyRayCoordinate(reverseZ, distance));
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, cursor);
            if (rbmk != null) {
                RbmkComponentBlock.Kind kind = rbmk.kind();
                if (kind == RbmkComponentBlock.Kind.REFLECTOR) {
                    if (reflectorEfficiency() != 1.0D) {
                        // Non-perfect reflectors continue the original ray;
                        // they do not return flux to the source rod.
                        continue;
                    }
                    return reflectedRayFluxToSelf(level, origin, cursor,
                            forwardX, forwardZ, distance, rayOffset);
                }
                if (kind.acceptsFuel()) {
                    if (rbmk.items.get(SLOT_FUEL).isEmpty()) {
                        continue;
                    }
                    // Only a ReaSim rod can be the source of a diagonal fan;
                    // a loaded ordinary rod receives/stops a ray instead.
                    if (!isReasimFuelKind(kind)
                            || rbmk.reasimRayOffset != rayOffset
                            || rbmk.emittedFlux <= 0.0D) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    NeutronFluxProvider.NeutronFlux emitted =
                            NeutronFluxProvider.NeutronFlux.fromRatio(
                                    rbmk.emittedFlux * 0.75D,
                                    rbmk.emittedFastRatio);
                    return streamFluxVector(level, cursor, forwardX, forwardZ,
                            distance, emitted, true, true, null);
                }
                if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                    RecipeHolder<RbmkOutgasserRecipe> recipe = outgasserRecipe(level, rbmk.items.get(SLOT_FUEL));
                    if (recipe != null && rbmk.canProcessOutgasser(recipe.value())) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    continue;
                }
                continue;
            }
            if (opaqueColumnHits(level, cursor) >= RbmkComponentBlock.columnHeight(level)) {
                return NeutronFluxProvider.NeutronFlux.ZERO;
            }
        }
        return NeutronFluxProvider.NeutronFlux.ZERO;
    }

    private NeutronFluxProvider.NeutronFlux reflectedFluxToSelf(Level level, BlockPos origin, Direction directionToReflector, int distance) {
        if (!kind().acceptsFuel()
                || isReasimFuelKind(kind())
                || items.get(SLOT_FUEL).isEmpty()
                || emittedFlux <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        NeutronFluxProvider.NeutronFlux reflected =
                NeutronFluxProvider.NeutronFlux.fromRatio(emittedFlux, emittedFastRatio);
        int[] moderatedCount = {0};
        reflected = streamFluxVector(level, origin,
                directionToReflector.getStepX(), directionToReflector.getStepZ(),
                distance, reflected, false, false, moderatedCount);
        if (reflected.total() <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }

        BlockPos reflectorPos = origin.relative(directionToReflector, distance);
        RbmkComponentBlockEntity reflector = rbmkColumnAt(level, reflectorPos);
        if (reflector != null && !reflector.hasLid() && level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(reflectorPos, reflected.total() * 0.05D);
        }

        // The old handler added the origin's moderated flag at the reflector,
        // then re-applied moderation for the complete count on the reflected
        // stream.  This is intentionally not a single generic multiplier.
        if (isModeratedKind(kind())) {
            moderatedCount[0]++;
        }
        if (reflected.fastRatio() > 0.0D) {
            for (int i = 0; i < moderatedCount[0]; i++) {
                reflected = moderate(reflected);
            }
        }
        return NeutronFluxProvider.NeutronFlux.fromRatio(
                reflected.total() * reflectorEfficiency(), reflected.fastRatio());
    }

    private NeutronFluxProvider.NeutronFlux reflectedRayFluxToSelf(
            Level level,
            BlockPos origin,
            BlockPos reflectorPos,
            double forwardX,
            double forwardZ,
            int distance,
            int rayOffset) {
        if (!isReasimFuelKind(kind())
                || reasimRayOffset != rayOffset
                || items.get(SLOT_FUEL).isEmpty()
                || emittedFlux <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        NeutronFluxProvider.NeutronFlux reflected =
                NeutronFluxProvider.NeutronFlux.fromRatio(emittedFlux * 0.75D, emittedFastRatio);
        int[] moderatedCount = {0};
        reflected = streamFluxVector(level, origin, forwardX, forwardZ,
                distance, reflected, false, false, moderatedCount);
        if (reflected.total() <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        RbmkComponentBlockEntity reflector = rbmkColumnAt(level, reflectorPos);
        if (reflector != null && !reflector.hasLid() && level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(reflectorPos, reflected.total() * 0.05D);
        }
        if (isModeratedKind(kind())) {
            moderatedCount[0]++;
        }
        if (reflected.fastRatio() > 0.0D) {
            for (int i = 0; i < moderatedCount[0]; i++) {
                reflected = moderate(reflected);
            }
        }
        return NeutronFluxProvider.NeutronFlux.fromRatio(
                reflected.total() * reflectorEfficiency(), reflected.fastRatio());
    }

    private NeutronFluxProvider.NeutronFlux streamFlux(
            Level level,
            BlockPos source,
            Direction streamDirection,
            int distanceToTarget,
            NeutronFluxProvider.NeutronFlux flux,
            boolean includeTarget) {
        return streamFluxVector(level, source,
                streamDirection.getStepX(), streamDirection.getStepZ(),
                distanceToTarget, flux, includeTarget, true, null);
    }

    /** Run a legacy floor-rounded stream along either a cardinal or diagonal vector. */
    private NeutronFluxProvider.NeutronFlux streamFluxVector(
            Level level,
            BlockPos source,
            double vectorX,
            double vectorZ,
            int distanceToTarget,
            NeutronFluxProvider.NeutronFlux flux,
            boolean includeTarget,
            boolean applyReflectorReturnModeration,
            @Nullable int[] reflectedModerationCount) {
        NeutronFluxProvider.NeutronFlux current = flux;
        int moderatedCount = 0;
        RbmkComponentBlockEntity sourceRbmk = rbmkColumnAt(level, source);
        for (int step = 1; step <= distanceToTarget; step++) {
            if (current.total() <= 0.0D) {
                return NeutronFluxProvider.NeutronFlux.ZERO;
            }
            boolean targetStep = step == distanceToTarget;
            if (targetStep && !includeTarget) {
                break;
            }
            BlockPos cursor = source.offset(
                    legacyRayCoordinate(vectorX, step),
                    0,
                    legacyRayCoordinate(vectorZ, step));
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, cursor);
            if (rbmk != null) {
                RbmkComponentBlock.Kind kind = rbmk.kind();
                // runStreamInteraction irradiated every RBMK node before its
                // type-specific receive/control logic, including the target.
                if (!rbmk.hasLid() && level instanceof ServerLevel serverLevel) {
                    ChunkRadiationData.get(serverLevel).incrementRadiation(
                            cursor, current.total() * 0.05D);
                }
                if (isModeratedKind(kind)) {
                    moderatedCount++;
                    current = moderate(current);
                }
                if (kind.isControl()) {
                    if (rbmk.controlLevel <= 0.0D) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    current = NeutronFluxProvider.NeutronFlux.fromRatio(
                            current.total() * rbmk.controlMultiplier(), current.fastRatio());
                } else if (kind == RbmkComponentBlock.Kind.REFLECTOR) {
                    if (applyReflectorReturnModeration) {
                        if (sourceRbmk != null && isModeratedKind(sourceRbmk.kind())) {
                            moderatedCount++;
                        }
                        if (current.fastRatio() > 0.0D) {
                            for (int i = 0; i < moderatedCount; i++) {
                                current = moderate(current);
                            }
                        }
                    }
                    if (reflectorEfficiency() == 1.0D) {
                        // Perfect reflection returns to the origin and cannot
                        // reach a downstream target.
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    // A partial reflector is an outbound attenuation point;
                    // the old handler continued the same stream.
                    current = NeutronFluxProvider.NeutronFlux.fromRatio(
                            current.total() * reflectorEfficiency(), current.fastRatio());
                } else if (kind == RbmkComponentBlock.Kind.ABSORBER) {
                    current = rbmk.absorbFlux(current);
                } else if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                    RecipeHolder<RbmkOutgasserRecipe> recipe = outgasserRecipe(level, rbmk.items.get(SLOT_FUEL));
                    if (recipe != null && rbmk.canProcessOutgasser(recipe.value())) {
                        // The target outgasser receives this flux; an
                        // intermediate processable outgasser consumes it.
                        return targetStep ? current : NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                } else if (kind.acceptsFuel() && !targetStep
                        && !rbmk.items.get(SLOT_FUEL).isEmpty()) {
                    // A loaded rod receives and stops a stream even when it is
                    // not the requested receiver.  Empty channels are the
                    // only transparent fuel nodes in the legacy walker.
                    return NeutronFluxProvider.NeutronFlux.ZERO;
                }
                if (current.total() <= 0.0D) {
                    return NeutronFluxProvider.NeutronFlux.ZERO;
                }
                continue;
            }
            int hits = opaqueColumnHits(level, cursor);
            if (hits >= RbmkComponentBlock.columnHeight(level)) {
                return NeutronFluxProvider.NeutronFlux.ZERO;
            }
            if (hits > 0) {
                double multiplier = 1.0D - (double) hits / RbmkComponentBlock.columnHeight(level);
                current = NeutronFluxProvider.NeutronFlux.fromRatio(
                        current.total() * multiplier, current.fastRatio());
                if (level instanceof ServerLevel serverLevel) {
                    ChunkRadiationData.get(serverLevel).incrementRadiation(
                            cursor, current.total() * 0.05D, 2_000.0D);
                }
            } else if (level instanceof ServerLevel serverLevel && current.total() > 0.0D) {
                ChunkRadiationData.get(serverLevel).incrementRadiation(cursor, current.total() * 0.05D);
            }
        }
        if (reflectedModerationCount != null) {
            reflectedModerationCount[0] = moderatedCount;
        }
        return current;
    }

    private static NeutronFluxProvider.NeutronFlux moderate(NeutronFluxProvider.NeutronFlux flux) {
        double efficiency = moderatorEfficiency();
        double moderatedFast = flux.fast() * efficiency;
        return new NeutronFluxProvider.NeutronFlux(flux.slow() + moderatedFast, flux.fast() - moderatedFast);
    }

    private NeutronFluxProvider.NeutronFlux absorbFlux(NeutronFluxProvider.NeutronFlux flux) {
        heat += flux.total() * absorberHeatConversion();
        setChangedAndSync();
        double efficiency = absorberEfficiency();
        if (efficiency >= 1.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        return NeutronFluxProvider.NeutronFlux.fromRatio(flux.total() * Math.max(0.0D, efficiency), flux.fastRatio());
    }

    private static int opaqueColumnHits(Level level, BlockPos pos) {
        int hits = 0;
        int height = RbmkComponentBlock.columnHeight(level);
        for (int y = 0; y < height; y++) {
            BlockPos target = pos.above(y);
            BlockState state = level.getBlockState(target);
            // Legacy used Block#isOpaqueCube(), not collision-shape fullness.
            if (!state.isAir() && state.isSolidRender(level, target)) {
                hits++;
            }
        }
        return hits;
    }

    public boolean triggerBreakMeltdown(Level level, BlockPos pos) {
        if (level.isClientSide || meltingDown || !kind().acceptsFuel() || meltdownsDisabled()) {
            return false;
        }
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.getItem() instanceof RbmkFuelRodItem && RbmkFuelRodItem.hullHeat(fuel) >= 1500.0F) {
            meltdown(level, pos);
            return true;
        }
        return false;
    }

    /** Entry point for the original ItemDyatlov, which bypassed fuel safety checks. */
    public void forceMeltdown(Level level, BlockPos pos) {
        meltdown(level, pos);
    }

    private void meltdown(Level level, BlockPos pos) {
        if (level.isClientSide || meltingDown || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        meltingDown = true;
        Set<RbmkComponentBlockEntity> columns = connectedColumns(level, pos);
        if (columns.isEmpty()) {
            columns.add(this);
        }
        List<RbmkComponentBlockEntity> orderedColumns = new ArrayList<>(columns);
        orderedColumns.sort(Comparator
                .comparingInt((RbmkComponentBlockEntity rbmk) -> rbmk.getBlockPos().getX())
                .thenComparingInt(rbmk -> rbmk.getBlockPos().getZ())
                .thenComparingInt(rbmk -> rbmk.getBlockPos().getY()));
        List<BlockPos> boilerOutputs = new ArrayList<>();
        int minX = pos.getX();
        int maxX = pos.getX();
        int minZ = pos.getZ();
        int maxZ = pos.getZ();
        boolean digamma = false;
        int height = RbmkComponentBlock.columnHeight(level);
        for (RbmkComponentBlockEntity rbmk : orderedColumns) {
            BlockPos columnPos = rbmk.getBlockPos();
            minX = Math.min(minX, columnPos.getX());
            maxX = Math.max(maxX, columnPos.getX());
            minZ = Math.min(minZ, columnPos.getZ());
            maxZ = Math.max(maxZ, columnPos.getZ());
            ItemStack fuel = rbmk.items.get(SLOT_FUEL);
            if (fuel.getItem() instanceof RbmkFuelRodItem rod && rod.isDigammaFuel(fuel)) {
                digamma = true;
            }
            if (rbmk.kind() == RbmkComponentBlock.Kind.BOILER) {
                // TileEntityRBMKBoiler collected its fluid networks before
                // replacing its own block in onMelt(). Preserve that order;
                // after melting, kind() is no longer BOILER.
                boilerOutputs.addAll(rbmkBoilerOutputPositions(level, columnPos, height));
            }
        }

        List<RbmkMeltdownManager.ColumnSnapshot> snapshots = new ArrayList<>();
        for (RbmkComponentBlockEntity rbmk : orderedColumns) {
            BlockPos columnPos = rbmk.getBlockPos();
            int minDist = Math.min(
                    columnPos.getX() - minX,
                    Math.min(maxX - columnPos.getX(), Math.min(columnPos.getZ() - minZ, maxZ - columnPos.getZ()))
            );
            snapshots.add(rbmk.prepareMeltdownSnapshot(level, columnPos, minDist + 1, height));
        }
        int smallDim = Math.min(maxX - minX, maxZ - minZ);
        int avgX = minX + (maxX - minX) / 2;
        int avgZ = minZ + (maxZ - minZ) / 2;
        RbmkMeltdownManager.schedule(serverLevel, new RbmkMeltdownManager.Request(
                pos,
                avgX,
                avgZ,
                smallDim,
                digamma,
                level.random.nextLong(),
                snapshots,
                boilerOutputs
        ));
    }

    private RbmkMeltdownManager.ColumnSnapshot prepareMeltdownSnapshot(Level level, BlockPos pos, int reduce, int height) {
        RbmkComponentBlock.Kind meltKind = kind();
        boolean hasFuel = meltKind.acceptsFuel() && items.get(SLOT_FUEL).getItem() instanceof RbmkFuelRodItem;
        boolean hadNormalLid = lidType == LidType.NORMAL;
        clearContent();
        lidType = LidType.NONE;
        updateLidCollisionDummy();
        heat = 0.0D;
        lastFlux = 0.0D;
        lastFluxFastRatio = 0.0D;
        emittedFlux = 0.0D;
        emittedFastRatio = 0.0D;
        reasimRayOffset = 0;
        redstoneLevel = 0;
        meltingDown = true;
        setChangedAndSync();
        return new RbmkMeltdownManager.ColumnSnapshot(pos, meltKind, hasFuel, hadNormalLid, reduce, height);
    }

    void finishAsyncMeltdown() {
        meltingDown = false;
        setChangedAndSync();
    }

    private static Set<RbmkComponentBlockEntity> connectedColumns(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Set<RbmkComponentBlockEntity> result = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof RbmkComponentBlockEntity rbmk) || !rbmk.kind().isColumn()) {
                continue;
            }
            result.add(rbmk);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                queue.add(pos.relative(direction));
            }
        }
        return result;
    }

    private static List<BlockPos> rbmkBoilerOutputPositions(Level level, BlockPos base, int height) {
        List<BlockPos> outputs = new ArrayList<>();
        // Legacy output is base.y + effectiveHeight + 1.  Modern height is
        // the total four-block column, so this is base.y + height (y + 4).
        outputs.add(base.above(height));
        if (level.getBlockState(base.below()).is(HbmBlocks.RBMK_LOADER.get())) {
            addLoaderOutputPositions(outputs, base, 1);
        } else if (level.getBlockState(base.below(2)).is(HbmBlocks.RBMK_LOADER.get())) {
            addLoaderOutputPositions(outputs, base, 2);
        }
        return outputs;
    }

    private static void addLoaderOutputPositions(List<BlockPos> outputs, BlockPos base, int offsetBelow) {
        BlockPos loader = base.below(offsetBelow);
        outputs.add(loader.east());
        outputs.add(loader.west());
        outputs.add(loader.south());
        outputs.add(loader.north());
        outputs.add(loader.below());
    }

    /**
     * The 1.7.10 RBMK fluid machines actively called tryProvide() every
     * server tick.  A capability by itself is only pull-based in the modern
     * fluid network, so mirror the old sender pass at the exact old output
     * coordinates.  The side is the receiving face (the opposite of the
     * sender's ForgeDirection).
     */
    private void pushRbmkFluid(Level level, HbmFluidTank tank, boolean outgasser) {
        if (tank.amount() <= 0 || tank.type().isNone()) {
            return;
        }
        List<RbmkFluidOutput> outputs = new ArrayList<>();
        int height = RbmkComponentBlock.columnHeight(level);
        outputs.add(new RbmkFluidOutput(worldPosition.above(height), Direction.DOWN));

        BlockPos loader = null;
        if (level.getBlockState(worldPosition.below()).is(HbmBlocks.RBMK_LOADER.get())) {
            loader = worldPosition.below();
        } else if (level.getBlockState(worldPosition.below(2)).is(HbmBlocks.RBMK_LOADER.get())) {
            loader = worldPosition.below(2);
        }
        if (loader != null) {
            // Exact TileEntityRBMK* getOutputPos() order: +X, -X, +Z,
            // -Z, then the block below the loader.
            outputs.add(new RbmkFluidOutput(loader.east(), Direction.WEST));
            outputs.add(new RbmkFluidOutput(loader.west(), Direction.EAST));
            outputs.add(new RbmkFluidOutput(loader.south(), Direction.NORTH));
            outputs.add(new RbmkFluidOutput(loader.north(), Direction.SOUTH));
            outputs.add(new RbmkFluidOutput(loader.below(), Direction.UP));
        } else if (outgasser) {
            // Outgasser has one additional downward output when no loader is
            // installed; boiler/heater/cooler do not.
            outputs.add(new RbmkFluidOutput(worldPosition.below(), Direction.UP));
        }

        for (RbmkFluidOutput output : outputs) {
            if (tank.amount() <= 0) {
                break;
            }
            FluidStack offered = HbmFluids.toNeoStack(tank.type(), tank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    output.target(),
                    output.receivingSide(),
                    offered,
                    worldPosition,
                    true
            );
            if (accepted > 0) {
                tank.drain(tank.type(), accepted, false);
                setChanged();
            }
        }
    }

    private record RbmkFluidOutput(BlockPos target, Direction receivingSide) {
    }

    private static boolean meltdownsDisabled() {
        try {
            return HbmConfig.RBMK_DISABLE_MELTDOWNS.get();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private void boilWater(Level level, BlockPos pos) {
        boilerConsumption = 0;
        boilerOutput = 0;
        double heatCap = steamHeatCap();
        double heatProvided = heat - heatCap;
        if (heatProvided <= 0.0D || water.amount() <= 0) {
            return;
        }
        HbmFluidDefinition steamType = steamFluid();
        double factor = steamFactor();
        int waterUsed;
        int steamProduced;
        if (steamCompression == 3) {
            steamProduced = (int) Math.floor((heatProvided / boilerHeatConsumption()) * 100.0D / factor);
            waterUsed = (int) Math.floor(steamProduced / 100.0D * factor);
            if (water.amount() < waterUsed) {
                steamProduced = (int) Math.floor(water.amount() * 100.0D / factor);
                waterUsed = (int) Math.floor(steamProduced / 100.0D * factor);
            }
        } else {
            waterUsed = (int) Math.floor(heatProvided / boilerHeatConsumption());
            waterUsed = Math.min(waterUsed, water.amount());
            steamProduced = (int) Math.floor((waterUsed * 100.0D) / factor);
        }
        // The legacy boiler consumed the calculated water/heat before
        // clamping an over-capacity steam fill.  A full steam tank therefore
        // still consumes water and heat for that tick; do not scale
        // waterUsed back to the amount that happened to fit.
        if (waterUsed <= 0 || steamProduced <= 0 || steamType.isNone()) {
            return;
        }
        boilerConsumption = waterUsed;
        boilerOutput = steamProduced;
        water.drain(water.type(), waterUsed, false);
        // HbmFluidTank.fill naturally clamps to the 1.7.10 tank capacity,
        // matching FluidTank#setFill followed by its explicit cap.
        steam.fill(steamType, steamProduced, false);
        heat -= waterUsed * boilerHeatConsumption();
        setChangedAndSync();
    }

    private void exchangeHeat(Level level, BlockPos pos) {
        boilerConsumption = 0;
        boilerOutput = 0;
        // TileEntityRBMKHeater called FluidTank#setType(0, slots) before
        // resolving its first heating step.  The legacy slotted base did not
        // expose this slot for insertion, but old saves (and manually
        // migrated inventories) can still contain a fluid identifier there.
        // Apply that identifier exactly once per server tick before the
        // exchanger examines the tank type.
        ItemStack identifier = items.get(SLOT_FUEL);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
            if (heaterInput.type() != selected) {
                heaterInput.setType(selected);
            }
        }
        HbmThermalConversions.firstHeatExchangerStep(heaterInput.type()).ifPresentOrElse(step -> {
            // The legacy heater changes its output tank type on every server
            // tick as soon as the input has a heat-exchanger step.  A type
            // change clears the old output tank, exactly as FluidTank#setTankType
            // did in 1.7.10.
            heaterOutput.setType(step.output());
            double tempRange = heat - step.output().temperatureCelsius();
            double efficiency = step.boilerEfficiency();
            if (efficiency <= 0.0D) {
                heaterInput.clear();
                heaterOutput.clear();
                setChangedAndSync();
                return;
            }
            if (tempRange <= 0.0D) {
                return;
            }
            double tuPerDegree = HEAT_EXCHANGER_TU_PER_DEGREE * efficiency;
            int inputOps = heaterInput.amount() / step.amountReq();
            int outputOps = (heaterOutput.capacity() - heaterOutput.amount()) / step.amountProduced();
            int tempOps = (int) Math.floor((tempRange * tuPerDegree) / step.heatReq());
            int ops = Math.min(inputOps, Math.min(outputOps, tempOps));
            if (ops <= 0) {
                return;
            }
            int inputUsed = step.amountReq() * ops;
            int outputMade = step.amountProduced() * ops;
            heaterInput.drain(step.input(), inputUsed, false);
            heaterOutput.fill(step.output(), outputMade, false);
            heat -= (step.heatReq() * ops / tuPerDegree) * efficiency;
            boilerConsumption = inputUsed;
            boilerOutput = outputMade;
            setChangedAndSync();
        }, () -> {
            // The old heater cleared both tank types for an invalid/non-
            // heatable input, even when the tank was already empty.
            heaterInput.clear();
            heaterOutput.clear();
            setChangedAndSync();
        });
    }

    private void tickOutgasser(Level level, BlockPos pos) {
        RecipeHolder<RbmkOutgasserRecipe> holder = outgasserRecipe(level, items.get(SLOT_FUEL));
        if (holder == null || !canProcessOutgasser(holder.value())) {
            if (outgasserProgress != 0.0D) {
                outgasserProgress = 0.0D;
                setChangedAndSync();
            }
            return;
        }
        NeutronFluxProvider.NeutronFlux flux = rbmkIncomingSpectrum(level, pos);
        lastFlux = flux.total();
        lastFluxFastRatio = flux.fastRatio();
        if (flux.total() <= 0.0D) {
            return;
        }
        double efficiency = Math.min(1.0D - flux.fastRatio() * 0.8D, 1.0D);
        outgasserProgress += flux.total() * efficiency * outgasserSpeedMod();
        // TileEntityRBMKOutgasser.receiveFlux() used a strict `> duration`
        // check; exactly 10,000 accumulated flux remains pending until the
        // next neutron tick.
        if (outgasserProgress > OUTGASSER_DURATION) {
            processOutgasser(holder.value());
        }
        setChangedAndSync();
    }

    private void tickAutoloader(Level level, BlockPos pos) {
        autoloaderLastPiston = autoloaderPiston;
        if (autoloaderCycle < 5 || autoloaderCycle > 95) {
            autoloaderCycle = Math.max(5, Math.min(95, autoloaderCycle == 0 ? 50 : autoloaderCycle));
        }
        if (autoloaderDelay > 0) {
            autoloaderDelay--;
        }
        if (autoloaderDelay <= 0 && autoloaderRetracting && autoloaderPiston > 0.0D) {
            autoloaderPiston -= 0.005D;
            if (autoloaderPiston <= 0.0D) {
                autoloaderPiston = 0.0D;
                autoloaderDelay = 40;
            }
            setChangedAndSync();
        }
        if (autoloaderRetracting
                && level.getGameTime() % 20L == 0L
                && hasAutoloaderFuel()
                && hasAutoloaderSpace()
                && canServiceRodBelow(level, pos)) {
            autoloaderRetracting = false;
            setChangedAndSync();
        }
        if (autoloaderDelay <= 0 && !autoloaderRetracting && autoloaderPiston < 1.0D) {
            autoloaderPiston += 0.005D;
            if (autoloaderPiston >= 1.0D) {
                autoloaderPiston = 1.0D;
                autoloaderDelay = 40;
            }
            setChangedAndSync();
        }
        if (!autoloaderRetracting && autoloaderPiston >= 1.0D) {
            serviceRodBelow(level, pos);
            autoloaderRetracting = true;
            autoloaderDelay = 40;
            setChangedAndSync();
        }
    }

    /**
     * The 1.7.10 RBMK cooler is a 4,000 mB cold-perfluoromethyl tank feeding a
     * 4,000 mB warm tank.  Every 60 ticks it caches the 5x5 horizontal area;
     * each 50 mB conversion removes exactly 200°C from every cached RBMK base
     * tile (including the cooler itself), with a floor of 20°C.  This is kept
     * as a dedicated path instead of the generic column cooling code because
     * the old implementation explicitly used this area effect.
     */
    private void tickCooler(Level level, BlockPos pos) {
        if (coolerTimer <= 0) {
            coolerTimer = 60;
            int index = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    coolerNeighbors[index++] = rbmkColumnAt(level, pos.offset(dx, 0, dz));
                }
            }
        } else {
            coolerTimer--;
        }

        if (coolerInput.amount() < 50 || coolerOutput.capacity() - coolerOutput.amount() < 50) {
            return;
        }
        HbmFluidDefinition cold = HbmFluids.byName("perfluoromethyl_cold").orElse(HbmFluids.none());
        HbmFluidDefinition warm = HbmFluids.byName("perfluoromethyl").orElse(HbmFluids.none());
        if (coolerInput.type() != cold || coolerOutput.type() != warm && coolerOutput.amount() > 0) {
            return;
        }
        coolerInput.drain(cold, 50, false);
        coolerOutput.fill(warm, 50, false);
        for (RbmkComponentBlockEntity neighbor : coolerNeighbors) {
            if (neighbor != null) {
                neighbor.heat = Math.max(20.0D, neighbor.heat - 200.0D);
            }
        }
    }

    private void clientTick(Level level, BlockPos pos) {
        RbmkComponentBlock.Kind kind = kind();
        if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            // TileEntityCraneConsole reset the visual tilt on every client
            // tick, then applied the local operator's key state.  Position,
            // progress, and loading remained server-authoritative; only the
            // two short-lived tilt angles were predicted locally.
            craneLastTiltFront = craneTiltFront;
            craneLastTiltLeft = craneTiltLeft;
            craneTiltFront = 0.0D;
            craneTiltLeft = 0.0D;
            return;
        }
        if (kind.isControl()) {
            // TileEntityRBMKControl only copied level to lastLevel on the
            // client.  Movement (and ReaSim power gating) was server-side;
            // simulating it here makes an unpowered rod appear to move.
            lastControlLevel = controlLevel;
        }
        if (kind != RbmkComponentBlock.Kind.AUTOLOADER) {
            return;
        }
        double previous = autoloaderLastPiston;
        autoloaderLastPiston = autoloaderPiston;
        boolean wasMoving = Math.abs(previous - autoloaderPiston) > 0.0001D;
        boolean isMoving = autoloaderPiston > 0.01D && autoloaderPiston < 0.99D;
        if (isMoving && !wasMoving) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, HbmSoundEvents.WGH_START.get(), SoundSource.BLOCKS, 0.75F, 1.0F, false);
        } else if (!isMoving && wasMoving) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, HbmSoundEvents.WGH_STOP.get(), SoundSource.BLOCKS, 2.0F, 1.0F, false);
        }
        if (autoloaderPiston > 0.99D) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        pos.getX() + 0.5D + (level.random.nextGaussian() * 0.125D),
                        pos.getY() + 0.25D,
                        pos.getZ() + 0.5D + (level.random.nextGaussian() * 0.125D),
                        (level.random.nextDouble() - 0.5D) * 0.05D,
                        0.08D + level.random.nextDouble() * 0.04D,
                        (level.random.nextDouble() - 0.5D) * 0.05D
                );
            }
        }
    }

    /** Apply only the legacy client-side crane tilt prediction. */
    public void applyClientCraneVisualInput(boolean up, boolean down, boolean left, boolean right) {
        if (level == null || !level.isClientSide || kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE
                || craneIsLoading()) {
            return;
        }
        if (up && !down) {
            craneTiltFront = 30.0D;
        } else if (!up && down) {
            craneTiltFront = -30.0D;
        }
        if (left && !right) {
            craneTiltLeft = 30.0D;
        } else if (!left && right) {
            craneTiltLeft = -30.0D;
        }
    }

    public double autoloaderPiston(float partialTick) {
        return autoloaderLastPiston + (autoloaderPiston - autoloaderLastPiston) * partialTick;
    }

    private boolean canServiceRodBelow(Level level, BlockPos pos) {
        RbmkComponentBlockEntity rod = rodBelow(level, pos);
        if (rod == null || !coldEnoughForAutoloader(rod.items.get(SLOT_FUEL))) {
            return false;
        }
        ItemStack installed = rod.items.get(SLOT_FUEL);
        // The legacy autoloader only replaced an empty slot or an installed
        // *fuel rod* below its cycle threshold.  A non-rod item made
        // coldEnoughForAutoloader() true, but did not satisfy the old
        // instanceof/remaining-fuel branch and therefore was left in place.
        return installed.isEmpty()
                || (installed.getItem() instanceof RbmkFuelRodItem
                && remainingFuelPercent(installed) < autoloaderCycle);
    }

    private void serviceRodBelow(Level level, BlockPos pos) {
        RbmkComponentBlockEntity rod = rodBelow(level, pos);
        if (rod == null || !coldEnoughForAutoloader(rod.items.get(SLOT_FUEL))) {
            return;
        }
        ItemStack installed = rod.items.get(SLOT_FUEL);
        if (!installed.isEmpty() && hasAutoloaderSpace()) {
            for (int slot = AUTOLOADER_OUTPUT_START; slot < AUTOLOADER_OUTPUT_END; slot++) {
                if (items.get(slot).isEmpty()) {
                    items.set(slot, installed.copy());
                    rod.items.set(SLOT_FUEL, ItemStack.EMPTY);
                    rod.setChangedAndSync();
                    break;
                }
            }
        }
        if (rod.items.get(SLOT_FUEL).isEmpty()) {
            for (int slot = AUTOLOADER_INPUT_START; slot < AUTOLOADER_INPUT_END; slot++) {
                ItemStack candidate = items.get(slot);
                if (!candidate.isEmpty()
                        && candidate.getItem() instanceof RbmkFuelRodItem
                        && remainingFuelPercent(candidate) >= autoloaderCycle) {
                    rod.items.set(SLOT_FUEL, candidate.copyWithCount(1));
                    candidate.shrink(1);
                    if (candidate.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    rod.setChangedAndSync();
                    break;
                }
            }
        }
    }

    @Nullable
    private RbmkComponentBlockEntity rodBelow(Level level, BlockPos pos) {
        RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, pos.below());
        return rbmk != null && rbmk.kind().acceptsFuel() ? rbmk : null;
    }

    /**
     * Resolve an RBMK column from either its base block entity or one of the
     * MachineDummy segments used for the old multi-block column.  The 1.7.10
     * code consistently called RBMKBase.findCore/Compat.getTileStandard for
     * these lookups, so checking only the directly stored BE loses columns
     * when a port or neutron path touches a dummy segment.
     */
    @Nullable
    private static RbmkComponentBlockEntity rbmkColumnAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn()) {
            return rbmk;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof RbmkComponentBlockEntity rbmk
                && rbmk.kind().isColumn()) {
            return rbmk;
        }
        return null;
    }

    private boolean hasAutoloaderFuel() {
        for (int slot = AUTOLOADER_INPUT_START; slot < AUTOLOADER_INPUT_END; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof RbmkFuelRodItem
                    && remainingFuelPercent(stack) >= autoloaderCycle) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAutoloaderSpace() {
        for (int slot = AUTOLOADER_OUTPUT_START; slot < AUTOLOADER_OUTPUT_END; slot++) {
            if (items.get(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean coldEnoughForAutoloader(ItemStack stack) {
        return stack.isEmpty() || !(stack.getItem() instanceof RbmkFuelRodItem) || RbmkFuelRodItem.hullHeat(stack) <= 1000.0F;
    }

    private static boolean coldEnoughForManual(ItemStack stack) {
        return stack.isEmpty() || !(stack.getItem() instanceof RbmkFuelRodItem) || RbmkFuelRodItem.hullHeat(stack) <= 200.0F;
    }

    private static float remainingFuelPercent(ItemStack stack) {
        if (!(stack.getItem() instanceof RbmkFuelRodItem)) {
            return 0.0F;
        }
        return (float) Math.max(0.0D, Math.min(100.0D, (1.0D - RbmkFuelRodItem.depletion(stack)) * 100.0D));
    }

    private boolean canProcessOutgasser(RbmkOutgasserRecipe recipe) {
        if (recipe.fusionOnly()) {
            return false;
        }
        if (recipe.hasFluidOutput()) {
            // The legacy canProcess() retargets an empty gas tank before the
            // capacity check. Preserve that side effect so the first recipe
            // can fill even when the persisted/default tank type differs.
            if (outgasserGas.amount() == 0 && outgasserGas.type() != recipe.fluidOutput().fluid()) {
                outgasserGas.setType(recipe.fluidOutput().fluid());
            } else if (outgasserGas.amount() > 0 && outgasserGas.type() != recipe.fluidOutput().fluid()) {
                return false;
            }
            if (outgasserGas.amount() + recipe.fluidOutput().amount() > outgasserGas.capacity()) {
                return false;
            }
        }
        if (recipe.hasItemOutput()) {
            ItemStack existing = items.get(SLOT_BUFFER);
            if (!existing.isEmpty() && (existing.getItem() != recipe.itemOutput().getItem()
                    || existing.getDamageValue() != recipe.itemOutput().getDamageValue()
                    || existing.getCount() + recipe.itemOutput().getCount() > existing.getMaxStackSize())) {
                return false;
            }
        }
        return true;
    }

    private void processOutgasser(RbmkOutgasserRecipe recipe) {
        items.get(SLOT_FUEL).shrink(1);
        if (items.get(SLOT_FUEL).isEmpty()) {
            items.set(SLOT_FUEL, ItemStack.EMPTY);
        }
        outgasserProgress = 0.0D;
        if (recipe.hasFluidOutput()) {
            outgasserGas.fill(recipe.fluidOutput().fluid(), recipe.fluidOutput().amount(), false);
        }
        if (recipe.hasItemOutput()) {
            ItemStack existing = items.get(SLOT_BUFFER);
            if (existing.isEmpty()) {
                items.set(SLOT_BUFFER, recipe.itemOutput().copy());
            } else {
                existing.grow(recipe.itemOutput().getCount());
            }
        }
        setChangedAndSync();
    }

    private void diffuseHeat(Level level, BlockPos pos) {
        RbmkComponentBlockEntity[] members = new RbmkComponentBlockEntity[5];
        members[0] = this;
        int count = 1;
        double total = this.heat;
        int totalWater = this.reasimWater;
        int totalSteam = this.reasimSteam;
        boolean distributeReasim = HbmConfig.RBMK_REASIM_BOILERS.get();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RbmkComponentBlockEntity other = rbmkColumnAt(level, pos.relative(direction));
            if (other != null) {
                members[count++] = other;
                total += other.heat;
                if (distributeReasim) {
                    totalWater += other.reasimWater;
                    totalSteam += other.reasimSteam;
                }
            }
        }
        if (count > 1) {
            double target = total / count;
            int waterEach = totalWater / count;
            int waterRemainder = totalWater % count;
            int steamEach = totalSteam / count;
            int steamRemainder = totalSteam % count;
            for (int i = 0; i < count; i++) {
                RbmkComponentBlockEntity rbmk = members[i];
                rbmk.heat += (target - rbmk.heat) * columnHeatFlow();
                if (distributeReasim) {
                    rbmk.reasimWater = waterEach;
                    rbmk.reasimSteam = steamEach;
                }
                rbmk.setChanged();
            }
            if (distributeReasim) {
                this.reasimWater += waterRemainder;
                this.reasimSteam += steamRemainder;
            }
        }
        coolPassively(count - 1);
    }

    private void boilReasimWater() {
        if (heat <= 100.0D || reasimWater <= 0 || reasimSteam >= REASIM_INTERNAL_CAPACITY) {
            return;
        }
        double availableHeat = (heat - 100.0D) / boilerHeatConsumption();
        double speed = Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_REASIM_BOILER_SPEED.get()));
        int processed = (int) Math.floor(Math.min(availableHeat, Math.min(reasimWater, REASIM_INTERNAL_CAPACITY - reasimSteam)) * speed);
        if (processed <= 0) {
            return;
        }
        reasimWater -= processed;
        reasimSteam += processed;
        heat -= processed * boilerHeatConsumption();
        setChangedAndSync();
    }

    private void tickReasimInlet(Level level, BlockPos pos) {
        if (!HbmConfig.RBMK_REASIM_BOILERS.get() || reasimInletWater.amount() <= 0) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, pos.relative(direction));
            if (rbmk != null) {
                int moved = Math.min(REASIM_INTERNAL_CAPACITY - rbmk.reasimWater, reasimInletWater.amount());
                if (moved > 0) {
                    rbmk.reasimWater += moved;
                    reasimInletWater.drain(reasimInletWater.type(), moved, false);
                    rbmk.setChangedAndSync();
                    setChangedAndSync();
                }
                if (reasimInletWater.amount() <= 0) {
                    return;
                }
            }
        }
    }

    private void tickReasimOutlet(Level level, BlockPos pos) {
        if (!HbmConfig.RBMK_REASIM_BOILERS.get() || reasimOutletSteam.amount() >= reasimOutletSteam.capacity()) {
            return;
        }
        HbmFluidDefinition superhotSteam = HbmFluids.byName("superhotsteam").orElse(HbmFluids.none());
        if (superhotSteam.isNone()) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, pos.relative(direction));
            if (rbmk != null) {
                int moved = Math.min(reasimOutletSteam.capacity() - reasimOutletSteam.amount(), rbmk.reasimSteam);
                if (moved > 0) {
                    rbmk.reasimSteam -= moved;
                    reasimOutletSteam.fill(superhotSteam, moved, false);
                    rbmk.setChangedAndSync();
                    setChangedAndSync();
                }
                if (reasimOutletSteam.amount() >= reasimOutletSteam.capacity()) {
                    return;
                }
            }
        }
    }

    private void pushReasimOutlet(Level level) {
        if (reasimOutletSteam.amount() <= 0 || reasimOutletSteam.type().isNone()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (reasimOutletSteam.amount() <= 0) {
                break;
            }
            FluidStack offered = HbmFluids.toNeoStack(reasimOutletSteam.type(), reasimOutletSteam.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    worldPosition.relative(direction),
                    direction.getOpposite(),
                    offered,
                    worldPosition,
                    true
            );
            if (accepted > 0) {
                reasimOutletSteam.drain(reasimOutletSteam.type(), accepted, false);
                setChanged();
            }
        }
    }

    private void coolPassively(int neighbors) {
        double clampedNeighbors = Math.max(0.0D, Math.min(4.0D, neighbors));
        double cooling = passiveCoolingInner() + (passiveCoolingEdge() - passiveCoolingInner()) * ((4.0D - clampedNeighbors) / 4.0D);
        heat = Math.max(20.0D, heat - cooling);
    }

    private void tickControl(Level level) {
        long previousPower = controlPower;
        boolean previousHasPower = controlHasPower;
        double previousTarget = targetControlLevel;

        // TileEntityRBMKControlAuto calculated its target before entering
        // TileEntityRBMKControl.updateEntity().  Preserve that order so the
        // target is based on this tick's current heat, even while a ReaSim
        // control is waiting for power.
        if (kind().isAutomaticControl()) {
            updateAutomaticControlTarget();
        }

        // TileEntityRBMKControl subscribed to the network before checking its
        // 5,000 HE movement cost.  Normal rods are always powered; only the
        // two ReaSim variants participate in the modern power graph.
        controlHasPower = true;
        if (isReasimControlKind()) {
            PowerNetworkManager.tickFromEndpoint(level, this);
            controlHasPower = controlPower >= REASIM_CONTROL_CONSUMPTION;
        }

        lastControlLevel = controlLevel;
        if (controlHasPower) {
            if (controlLevel < targetControlLevel) {
                controlLevel = Math.min(targetControlLevel, controlLevel + controlSpeed());
            } else if (controlLevel > targetControlLevel) {
                controlLevel = Math.max(targetControlLevel, controlLevel - controlSpeed());
            }

            // The old tile charged only for a tick in which the rod actually
            // moved, never merely for having a target or being powered.
            if (isReasimControlKind() && controlLevel != lastControlLevel) {
                controlPower = Math.max(0L, controlPower - REASIM_CONTROL_CONSUMPTION);
            }
        }

        // The legacy TileEntityRBMKBase network-packed every server tick.
        // In particular, an automatic rod's target can change while its
        // physical level is stationary (for example while a ReaSim rod is
        // unpowered); that target still must reach the client.
        if (previousPower != controlPower || previousHasPower != controlHasPower
                || controlLevel != lastControlLevel || targetControlLevel != previousTarget) {
            setChangedAndSync();
        } else {
            setChanged();
        }
    }

    private void updateAutomaticControlTarget() {
        double lowerBound = Math.min(autoHeatLower, autoHeatUpper);
        double upperBound = Math.max(autoHeatLower, autoHeatUpper);
        double fauxLevel;
        if (heat < lowerBound) {
            fauxLevel = autoLevelLower;
        } else if (heat > upperBound) {
            fauxLevel = autoLevelUpper;
        } else {
            // Keep the legacy interpolation exactly.  The 1.7.10 code did
            // not special-case equal heat bounds; an equal-bound setup is
            // therefore allowed to produce the same IEEE-754 result rather
            // than being silently replaced by a fallback level.
            fauxLevel = switch (autoControlFunction) {
                case LINEAR -> (heat - autoHeatLower) * ((autoLevelUpper - autoLevelLower) / (autoHeatUpper - autoHeatLower)) + autoLevelLower;
                case QUAD_UP -> Math.pow((heat - autoHeatLower) / (autoHeatUpper - autoHeatLower), 2.0D) * (autoLevelUpper - autoLevelLower) + autoLevelLower;
                case QUAD_DOWN -> Math.pow((heat - autoHeatUpper) / (autoHeatLower - autoHeatUpper), 2.0D) * (autoLevelLower - autoLevelUpper) + autoLevelUpper;
            };
        }
        targetControlLevel = Math.max(0.0D, Math.min(1.0D, fauxLevel * 0.01D));
    }

    private void setTargetControlLevel(double target) {
        double clamped = Math.max(0.0D, Math.min(1.0D, target));
        // TileEntityRBMKControlManual.setTarget always captured the current
        // level, even when the requested target was unchanged.  That baseline
        // is what the old insertion-surge calculation uses.
        if (isManualControlKind()) {
            startingControlLevel = controlLevel;
        }
        targetControlLevel = clamped;
    }

    private double controlMultiplier() {
        if (!isManualControlKind()) {
            return Math.max(0.0D, Math.min(1.0D, controlLevel));
        }
        double surge = 0.0D;
        if (targetControlLevel < startingControlLevel && Math.abs(controlLevel - targetControlLevel) > 0.01D) {
            surge = Math.sin(Math.pow(1.0D - controlLevel, 15.0D) * Math.PI)
                    * (startingControlLevel - targetControlLevel)
                    * surgeMod();
        }
        // TileEntityRBMKControlManual.getMult returned the raw level plus
        // surge.  It intentionally did not clamp the transient value; the
        // neutron handler therefore sees the same insertion surge as 1.7.10.
        return controlLevel + surge;
    }

    private boolean isManualControlKind() {
        RbmkComponentBlock.Kind kind = kind();
        return kind == RbmkComponentBlock.Kind.CONTROL
                || kind == RbmkComponentBlock.Kind.CONTROL_MOD
                || kind == RbmkComponentBlock.Kind.CONTROL_REASIM;
    }

    private static double passiveCoolingEdge() {
        return Math.max(0.0D, HbmConfig.RBMK_PASSIVE_COOLING.get());
    }

    private static double passiveCoolingInner() {
        return Math.max(0.0D, HbmConfig.RBMK_PASSIVE_COOLING_INNER.get());
    }

    private static double columnHeatFlow() {
        return Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_COLUMN_HEAT_FLOW.get()));
    }

    private static double fuelDiffusionMod() {
        return Math.max(0.0D, HbmConfig.RBMK_FUEL_DIFFUSION_MOD.get());
    }

    private static double fuelHeatProvision() {
        return Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_HEAT_PROVISION.get()));
    }

    private static double boilerHeatConsumption() {
        return Math.max(0.000001D, HbmConfig.RBMK_BOILER_HEAT_CONSUMPTION.get());
    }

    private static double controlSpeed() {
        return 0.00277D * Math.max(0.0D, HbmConfig.RBMK_CONTROL_SPEED.get());
    }

    private static double reactivityMod() {
        return Math.max(0.0D, HbmConfig.RBMK_REACTIVITY_MOD.get());
    }

    private static double surgeMod() {
        return Math.max(0.0D, HbmConfig.RBMK_SURGE_MOD.get());
    }

    private static double outgasserSpeedMod() {
        return Math.max(0.0D, HbmConfig.RBMK_OUTGASSER_SPEED_MOD.get());
    }

    private static boolean depletionEnabled() {
        return HbmConfig.RBMK_ENABLE_DEPLETION.get();
    }

    private static boolean xenonEnabled() {
        return HbmConfig.RBMK_ENABLE_XENON.get();
    }

    private static double moderatorEfficiency() {
        return Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_MODERATOR_EFFICIENCY.get()));
    }

    private static double absorberEfficiency() {
        return Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_ABSORBER_EFFICIENCY.get()));
    }

    private static double absorberHeatConversion() {
        return Math.max(0.0D, HbmConfig.RBMK_ABSORBER_HEAT_CONVERSION.get());
    }

    private static double reflectorEfficiency() {
        return Math.max(0.0D, Math.min(1.0D, HbmConfig.RBMK_REFLECTOR_EFFICIENCY.get()));
    }

    private void tickConsole(Level level, BlockPos pos) {
        if (level.getGameTime() % 10L == 0L) {
            scanConsole(level);
            prepareConsoleScreens();
        }
        // TileEntityRBMKConsole.networkPackNT() ran every server tick; the
        // expensive 15x15 rescan was the part throttled to every ten ticks.
        // Keep the two cadences separate so live flux/control state does not
        // wait for the next rescan before reaching the client.
        setChangedAndSync();
    }

    private void tickDisplay(Level level) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        scanDisplay(level);
        setChangedAndSync();
    }

    private void scanDisplay(Level level) {
        java.util.Arrays.fill(displayKinds, -1);
        java.util.Arrays.fill(displayHeat, 0);
        java.util.Arrays.fill(displayMaxHeat, 1500);
        java.util.Arrays.fill(displayControl, 0);
        java.util.Arrays.fill(displayColorGroups, -1);
        java.util.Arrays.fill(displayCraneIndicators, 0);
        java.util.Arrays.fill(displayFuelDepletion, 0);
        if (linkedReactor == null) {
            return;
        }
        for (int index = 0; index < DISPLAY_COLUMN_COUNT; index++) {
            BlockPos scanPos = displayIndexPos(index);
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, scanPos);
            if (rbmk == null) {
                continue;
            }
            displayKinds[index] = rbmk.kind().ordinal();
            displayHeat[index] = (int) Math.round(rbmk.heat);
            displayMaxHeat[index] = (int) Math.round(rbmk.maxConsoleHeat());
            displayControl[index] = (int) Math.round(rbmk.controlLevel * 100.0D);
            displayColorGroups[index] = rbmk.isManualControlKind() ? rbmk.colorGroup : -1;
            displayCraneIndicators[index] = rbmk.craneIndicator;
            if (rbmk.kind().acceptsFuel()) {
                ItemStack fuel = rbmk.items.get(SLOT_FUEL);
                if (fuel.getItem() instanceof RbmkFuelRodItem) {
                    displayFuelDepletion[index] = (int) Math.round(RbmkFuelRodItem.depletion(fuel) * 1000.0F);
                }
            }
        }
    }

    private void scanConsole(Level level) {
        java.util.Arrays.fill(consoleKinds, -1);
        java.util.Arrays.fill(consoleHeat, 0);
        java.util.Arrays.fill(consoleHeatRaw, 0.0D);
        java.util.Arrays.fill(consoleControlRaw, 0.0D);
        java.util.Arrays.fill(consoleMaxHeat, 1500);
        java.util.Arrays.fill(consoleFlux, 0);
        java.util.Arrays.fill(consoleControl, 0);
        java.util.Arrays.fill(consoleColorGroups, -1);
        java.util.Arrays.fill(consoleCraneIndicators, 0);
        java.util.Arrays.fill(consoleFuelCoreHeat, 0);
        java.util.Arrays.fill(consoleFuelHullHeat, 0);
        java.util.Arrays.fill(consoleFuelDepletion, 0);
        java.util.Arrays.fill(consoleFuelXenon, 0);
        java.util.Arrays.fill(consoleFuelMaxHeat, 0);
        java.util.Arrays.fill(consoleWater, 0);
        java.util.Arrays.fill(consoleMaxWater, 0);
        java.util.Arrays.fill(consoleSteam, 0);
        java.util.Arrays.fill(consoleMaxSteam, 0);
        java.util.Arrays.fill(consoleSteamType, 0);
        java.util.Arrays.fill(consoleHeaterInput, 0);
        java.util.Arrays.fill(consoleHeaterOutput, 0);
        java.util.Arrays.fill(consoleHeaterMax, 0);
        consoleTotalFlux = 0;
        double rawTotalFlux = 0.0D;
        if (linkedReactor == null) {
            return;
        }
        for (int index = 0; index < CONSOLE_COLUMN_COUNT; index++) {
            BlockPos scanPos = consoleIndexPos(index);
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, scanPos);
            if (rbmk == null) {
                continue;
            }
            consoleKinds[index] = rbmk.kind().ordinal();
            consoleHeatRaw[index] = rbmk.heat;
            consoleControlRaw[index] = rbmk.controlLevel * 100.0D;
            consoleHeat[index] = (int) Math.round(rbmk.heat);
            consoleMaxHeat[index] = (int) Math.round(rbmk.maxConsoleHeat());
            consoleFlux[index] = (int) Math.round(rbmk.lastFlux);
            consoleControl[index] = (int) Math.round(rbmk.controlLevel * 100.0D);
            consoleColorGroups[index] = rbmk.isManualControlKind() ? rbmk.colorGroup : -1;
            consoleCraneIndicators[index] = rbmk.craneIndicator;
            if (rbmk.kind().acceptsFuel()) {
                // The old console accumulated each rod's raw double
                // lastFluxQuantity and truncated only after the complete
                // scan (its field was an int).  Summing already-rounded cells
                // can differ by one or more units for fractional flux.
                rawTotalFlux += rbmk.lastFlux;
                ItemStack fuel = rbmk.items.get(SLOT_FUEL);
                if (fuel.getItem() instanceof RbmkFuelRodItem rod) {
                    consoleFuelCoreHeat[index] = (int) Math.round(RbmkFuelRodItem.coreHeat(fuel));
                    consoleFuelHullHeat[index] = (int) Math.round(RbmkFuelRodItem.hullHeat(fuel));
                    consoleFuelDepletion[index] = (int) Math.round(RbmkFuelRodItem.depletion(fuel) * 1000.0F);
                    consoleFuelXenon[index] = (int) Math.round(RbmkFuelRodItem.xenon(fuel) * 1000.0F);
                    consoleFuelMaxHeat[index] = (int) Math.round(rod.meltingPoint(fuel));
                }
            } else if (rbmk.kind() == RbmkComponentBlock.Kind.BOILER) {
                consoleWater[index] = rbmk.water.amount();
                consoleMaxWater[index] = rbmk.water.capacity();
                consoleSteam[index] = rbmk.steam.amount();
                consoleMaxSteam[index] = rbmk.steam.capacity();
                consoleSteamType[index] = rbmk.steamCompression;
            } else if (rbmk.kind() == RbmkComponentBlock.Kind.HEATER) {
                consoleHeaterInput[index] = rbmk.heaterInput.amount();
                consoleHeaterOutput[index] = rbmk.heaterOutput.amount();
                consoleHeaterMax[index] = rbmk.heaterInput.capacity();
            }
        }
        consoleTotalFlux = (int) rawTotalFlux;
        for (int index = 0; index < consoleFluxBuffer.length - 1; index++) {
            consoleFluxBuffer[index] = consoleFluxBuffer[index + 1];
        }
        consoleFluxBuffer[consoleFluxBuffer.length - 1] = consoleTotalFlux;
    }

    private void applyConsoleToControls(int selectedIndex, int levelPercent) {
        if (level == null || linkedReactor == null) {
            return;
        }
        for (int index = 0; index < CONSOLE_COLUMN_COUNT; index++) {
            if (selectedIndex >= 0 && selectedIndex != index) {
                continue;
            }
            BlockPos target = consoleIndexPos(index);
            RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, target);
            if (rbmk != null && rbmk.isManualControlKind()) {
                rbmk.setTargetControlLevel(levelPercent / 100.0D);
                rbmk.setChangedAndSync();
            }
        }
        // Rescans remain on the legacy ten-tick scheduler.
        setChangedAndSync();
    }

    private BlockPos consoleIndexPos(int index) {
        int baseX = index % CONSOLE_GRID_SIZE - CONSOLE_GRID_SIZE / 2;
        int baseZ = index / CONSOLE_GRID_SIZE - CONSOLE_GRID_SIZE / 2;
        int relX = switch (consoleRotation & 3) {
            case 1 -> -baseZ;
            case 2 -> -baseX;
            case 3 -> baseZ;
            default -> baseX;
        };
        int relZ = switch (consoleRotation & 3) {
            case 1 -> baseX;
            case 2 -> -baseZ;
            case 3 -> -baseX;
            default -> baseZ;
        };
        return linkedReactor == null ? worldPosition : linkedReactor.offset(relX, 0, relZ);
    }

    private BlockPos displayIndexPos(int index) {
        int baseX = index % DISPLAY_GRID_SIZE - DISPLAY_GRID_SIZE / 2;
        int baseZ = index / DISPLAY_GRID_SIZE - DISPLAY_GRID_SIZE / 2;
        int relX = switch (displayRotation & 3) {
            case 1 -> -baseZ;
            case 2 -> -baseX;
            case 3 -> baseZ;
            default -> baseX;
        };
        int relZ = switch (displayRotation & 3) {
            case 1 -> baseX;
            case 2 -> -baseZ;
            case 3 -> -baseX;
            default -> baseZ;
        };
        return linkedReactor == null ? worldPosition : linkedReactor.offset(relX, 0, relZ);
    }

    private void prepareConsoleScreens() {
        for (int slot = 0; slot < consoleScreenTypes.length; slot++) {
            ConsoleScreenType type = ConsoleScreenType.byOrdinal(consoleScreenTypes[slot]);
            if (type == ConsoleScreenType.NONE) {
                consoleScreenDisplays[slot] = 0;
                continue;
            }
            double value = 0.0D;
            int count = 0;
            for (int index : consoleScreenColumns[slot]) {
                if (index < 0 || index >= CONSOLE_COLUMN_COUNT || consoleKinds[index] < 0) {
                    continue;
                }
                RbmkComponentBlock.Kind columnKind = RbmkComponentBlock.Kind.values()[consoleKinds[index]];
                switch (type) {
                    case COL_TEMP -> {
                        value += consoleHeatRaw[index];
                        count++;
                    }
                    case ROD_EXTRACTION -> {
                        if (columnKind.isControl()) {
                            value += consoleControlRaw[index];
                            count++;
                        }
                    }
                    case FUEL_DEPLETION -> {
                        ItemStack rod = consoleFuelStack(index);
                        if (!rod.isEmpty()) {
                            value += RbmkFuelRodItem.depletion(rod) * 100.0D;
                            count++;
                        }
                    }
                    case FUEL_POISON -> {
                        ItemStack rod = consoleFuelStack(index);
                        if (!rod.isEmpty()) {
                            // 1.7.10's getNBTForConsole stores xenon as a
                            // percentage (getPoison(), 0..100), while the
                            // modern item helper exposes the normalized
                            // 0..1 value.  Keep the console's old percent
                            // arithmetic before converting to tenths below.
                            value += RbmkFuelRodItem.xenon(rod) * 100.0D;
                            count++;
                        }
                    }
                    case FUEL_TEMP -> {
                        ItemStack rod = consoleFuelStack(index);
                        if (!rod.isEmpty()) {
                            // 1.7.10's FUEL_TEMP screen reads c_heat, which
                            // getNBTForConsole fills with the rod hull/skin
                            // temperature (c_coreHeat is a separate field).
                            value += RbmkFuelRodItem.hullHeat(rod);
                            count++;
                        }
                    }
                    default -> {
                    }
                }
            }
            // Legacy prepareScreenInfo used an integer cast (truncate toward
            // zero), not round(), before formatting one decimal place.
            consoleScreenDisplays[slot] = count <= 0 ? 0 : (int) (value / count * 10.0D);
        }
    }

    private ItemStack consoleFuelStack(int index) {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        BlockPos target = consoleIndexPos(index);
        RbmkComponentBlockEntity rbmk = rbmkColumnAt(level, target);
        if (rbmk != null && rbmk.kind().acceptsFuel()) {
            return rbmk.items.get(SLOT_FUEL);
        }
        return ItemStack.EMPTY;
    }

    private static int[] validConsoleIndices(int[] selected) {
        return java.util.Arrays.stream(selected)
                .filter(index -> index >= 0 && index < CONSOLE_COLUMN_COUNT)
                .distinct()
                .toArray();
    }

    private void tickCraneConsole(Level level, BlockPos pos, BlockState state) {
        craneLastTiltFront = craneTiltFront;
        craneLastTiltLeft = craneTiltLeft;
        craneLastPosFront = cranePosFront;
        craneLastPosLeft = cranePosLeft;
        craneLastProgress = craneProgress;

        RbmkComponentBlockEntity aboveColumn = craneTargetColumn(level);
        if (aboveColumn != null) {
            aboveColumn.craneIndicator = 10;
        }

        if (craneGoesDown) {
            if (craneProgress > 0.0D) {
                craneProgress -= 0.04D;
            } else {
                craneProgress = 0.0D;
                craneGoesDown = false;
                craneInteractWithTarget(aboveColumn);
            }
        } else if (craneProgress != 1.0D) {
            craneProgress = Math.min(1.0D, craneProgress + 0.04D);
        }

        craneTiltFront = 0.0D;
        craneTiltLeft = 0.0D;
        // The legacy tick read only the first player returned by its AABB
        // query. Keep that ordering and discard stale input when the first
        // player changes or leaves the area.
        Player operator = firstCraneOperator();
        boolean operatorOwnsInput = operator != null
                && operator.getUUID().equals(craneInputPlayer);
        if (!operatorOwnsInput) {
            clearCraneInput();
        }
        if (!craneIsLoading() && operatorOwnsInput) {
            if (craneInputUp && !craneInputDown) {
                craneTiltFront = 30.0D;
                cranePosFront += 0.05D;
            }
            if (!craneInputUp && craneInputDown) {
                craneTiltFront = -30.0D;
                cranePosFront -= 0.05D;
            }
            if (craneInputLeft && !craneInputRight) {
                craneTiltLeft = 30.0D;
                cranePosLeft += 0.05D;
            }
            if (!craneInputLeft && craneInputRight) {
                craneTiltLeft = -30.0D;
                cranePosLeft -= 0.05D;
            }
            if (craneInputLoad) {
                craneGoesDown = true;
            }
        }

        cranePosFront = Mth.clamp(cranePosFront, -craneSpanB, craneSpanF);
        cranePosLeft = Mth.clamp(cranePosLeft, -craneSpanR, craneSpanL);

        if (craneLoadedItem.getItem() instanceof RbmkFuelRodItem) {
            craneLoadedHeat = RbmkFuelRodItem.hullHeat(craneLoadedItem);
            craneLoadedEnrichment = Math.max(0.0D, 1.0D - RbmkFuelRodItem.depletion(craneLoadedItem));
        } else {
            craneLoadedHeat = 0.0D;
            craneLoadedEnrichment = 0.0D;
        }
        setChangedAndSync();
    }

    private void setupCrane(BlockPos target) {
        if (level == null || kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return;
        }
        // Legacy RBMKDials.getColumnHeight() returns the gamerule value minus
        // one: with a raw height of 4, the top body block is coreY + 3 and
        // setTarget stores centerY = coreY + 3 + 1 = coreY + 4.  The modern
        // columnHeight() is the total body height (the body occupies offsets
        // 0..height-1), so this same center is coreY + columnHeight().  The
        // target resolver subtracts one and therefore lands on the top body
        // block, never on the optional lid dummy at offset height.
        craneCenter = target.immutable().above(RbmkComponentBlock.columnHeight(level));
        int girderY = craneCenter.getY() + 6;
        Direction dir = getBlockState().getValue(RbmkComponentBlock.FACING).getOpposite();
        craneSpanF = findCraneRoomExtent(target.getX(), girderY, target.getZ(), dir, 16);
        dir = dir.getClockWise();
        craneSpanR = findCraneRoomExtent(target.getX(), girderY, target.getZ(), dir, 16);
        dir = dir.getClockWise();
        craneSpanB = findCraneRoomExtent(target.getX(), girderY, target.getZ(), dir, 16);
        dir = dir.getClockWise();
        craneSpanL = findCraneRoomExtent(target.getX(), girderY, target.getZ(), dir, 16);
        craneHeight = 7;
    }

    private int findCraneRoomExtent(int x, int y, int z, Direction direction, int max) {
        if (level == null) {
            return 0;
        }
        for (int i = 1; i < max; i++) {
            BlockPos pos = new BlockPos(x + direction.getStepX() * i, y, z + direction.getStepZ() * i);
            if (!level.getBlockState(pos).isAir()) {
                return i - 1;
            }
        }
        return max;
    }

    @Nullable
    private RbmkComponentBlockEntity craneTargetColumn(Level level) {
        if (craneCenter == null) {
            return null;
        }
        // The old getColumnAtPos() used only the console block metadata.  The
        // screwdriver's craneRotationOffset rotates the rendered bridge, but
        // is deliberately not part of the logical target coordinates.
        Direction dir = getBlockState().getValue(RbmkComponentBlock.FACING);
        Direction left = dir.getCounterClockWise();
        int x = (int) Math.floor(craneCenter.getX() - dir.getStepX() * cranePosFront - left.getStepX() * cranePosLeft + 0.5D);
        int y = craneCenter.getY() - 1;
        int z = (int) Math.floor(craneCenter.getZ() - dir.getStepZ() * cranePosFront - left.getStepZ() * cranePosLeft + 0.5D);
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            blockEntity = level.getBlockEntity(dummy.getCorePos());
        }
        return blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn() ? rbmk : null;
    }

    private boolean craneCanTargetInteract(@Nullable RbmkComponentBlockEntity target) {
        if (!isCraneLoadableTarget(target)) {
            return false;
        }
        if (!craneLoadedItem.isEmpty()) {
            return craneCanLoad(target, craneLoadedItem);
        }
        return craneCanUnload(target);
    }

    private void craneInteractWithTarget(@Nullable RbmkComponentBlockEntity target) {
        if (!craneCanTargetInteract(target)) {
            return;
        }
        if (!craneLoadedItem.isEmpty()) {
            // TileEntityRBMKRod/TileEntityRBMKStorage.load() copied the whole
            // supplied stack; do not silently truncate a legacy stack here.
            target.items.set(craneLoadSlot(target), craneLoadedItem.copy());
            craneLoadedItem = ItemStack.EMPTY;
            target.setChangedAndSync();
        } else {
            int slot = craneUnloadSlot(target);
            craneLoadedItem = target.items.get(slot).copy();
            target.items.set(slot, ItemStack.EMPTY);
            target.setChangedAndSync();
        }
        setChangedAndSync();
    }

    private boolean craneCanLoad(RbmkComponentBlockEntity target, ItemStack stack) {
        if (!isCraneLoadableTarget(target) || stack.isEmpty()) {
            return false;
        }
        if (target.kind().acceptsFuel()) {
            return target.items.get(SLOT_FUEL).isEmpty();
        }
        return target.kind() == RbmkComponentBlock.Kind.STORAGE && target.items.get(11).isEmpty();
    }

    private boolean craneCanUnload(RbmkComponentBlockEntity target) {
        if (!isCraneLoadableTarget(target)) {
            return false;
        }
        if (target.kind().acceptsFuel()) {
            return !target.items.get(SLOT_FUEL).isEmpty();
        }
        return target.kind() == RbmkComponentBlock.Kind.STORAGE && !target.items.get(0).isEmpty();
    }

    /** The exact two 1.7.10 IRBMKLoadable implementations: rods and storage. */
    private static boolean isCraneLoadableTarget(@Nullable RbmkComponentBlockEntity target) {
        return target != null
                && (target.kind().acceptsFuel() || target.kind() == RbmkComponentBlock.Kind.STORAGE);
    }

    private int craneLoadSlot(RbmkComponentBlockEntity target) {
        return target.kind() == RbmkComponentBlock.Kind.STORAGE ? 11 : SLOT_FUEL;
    }

    private int craneUnloadSlot(RbmkComponentBlockEntity target) {
        return target.kind() == RbmkComponentBlock.Kind.STORAGE ? 0 : SLOT_FUEL;
    }

    private AABB craneOperationArea() {
        Direction dir = getBlockState().getValue(RbmkComponentBlock.FACING);
        Direction side = dir.getClockWise();
        double minX = worldPosition.getX() + 0.5D - side.getStepX() * 1.5D;
        double maxX = worldPosition.getX() + 0.5D + side.getStepX() * 1.5D + dir.getStepX() * 2.0D;
        double minZ = worldPosition.getZ() + 0.5D - side.getStepZ() * 1.5D;
        double maxZ = worldPosition.getZ() + 0.5D + side.getStepZ() * 1.5D + dir.getStepZ() * 2.0D;
        return new AABB(
                Math.min(minX, maxX),
                worldPosition.getY(),
                Math.min(minZ, maxZ),
                Math.max(minX, maxX),
                worldPosition.getY() + 2.0D,
                Math.max(minZ, maxZ)
        );
    }

    @Nullable
    private Player firstCraneOperator() {
        if (level == null || kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return null;
        }
        List<Player> players = level.getEntitiesOfClass(Player.class, craneOperationArea());
        return players.isEmpty() ? null : players.get(0);
    }

    private void clearCraneInput() {
        craneInputPlayer = null;
        craneInputUp = false;
        craneInputDown = false;
        craneInputLeft = false;
        craneInputRight = false;
        craneInputLoad = false;
    }

    private void compactStorage() {
        if (kind() != RbmkComponentBlock.Kind.STORAGE) {
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < STORAGE_SLOTS.length - 1; slot++) {
            if (items.get(slot).isEmpty() && !items.get(slot + 1).isEmpty()) {
                items.set(slot, items.get(slot + 1));
                items.set(slot + 1, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            setChangedAndSync();
        }
    }

    private void cycleSteamCompression() {
        setSteamCompression((steamCompression + 1) & 3);
    }

    private void setSteamCompression(int value) {
        int next = Math.max(0, Math.min(3, value));
        if (steamCompression == next) {
            return;
        }
        // TileEntityRBMKBoiler.cyceCompressor() converted the amount while
        // changing the actual tank type.  It did not empty the tank: STEAM,
        // HOTSTEAM and SUPERHOTSTEAM are divided by ten on each step, while
        // ULTRAHOTSTEAM wraps to STEAM and is multiplied by one thousand.
        // HbmFluidTank#setType clears its amount, so retain the old amount
        // explicitly around the type change.
        int previousCompression = steamCompressionFor(steam.type());
        if (previousCompression < 0) {
            // HbmFluidTank clears its type when the amount reaches zero;
            // legacy FluidTank retained the selected steam type in that case.
            previousCompression = steamCompression;
        }
        int previousAmount = steam.amount();
        int convertedAmount = previousAmount;
        if (previousCompression == 3 && next == 0) {
            convertedAmount = (int) Math.min((long) previousAmount * 1000L, steam.capacity());
        } else if (next == previousCompression + 1) {
            convertedAmount = previousAmount / 10;
        }
        HbmFluidDefinition nextType = steamDefinition(next);
        steamCompression = next;
        steam.setType(nextType);
        steam.setAmount(convertedAmount);
        setChangedAndSync();
    }

    private HbmFluidDefinition steamFluid() {
        return steamDefinition(steamCompression);
    }

    private static HbmFluidDefinition steamDefinition(int compression) {
        String name = switch (compression) {
            case 1 -> "hotsteam";
            case 2 -> "superhotsteam";
            case 3 -> "ultrahotsteam";
            default -> "steam";
        };
        return HbmFluids.byName(name).orElse(HbmFluids.byName("steam").orElse(HbmFluids.none()));
    }

    private static int steamCompressionFor(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return -1;
        }
        return switch (fluid.name()) {
            case "steam" -> 0;
            case "hotsteam" -> 1;
            case "superhotsteam" -> 2;
            case "ultrahotsteam" -> 3;
            default -> -1;
        };
    }

    private double steamHeatCap() {
        return switch (steamCompression) {
            case 1 -> 300.0D;
            case 2 -> 450.0D;
            case 3 -> 600.0D;
            default -> 100.0D;
        };
    }

    private double steamFactor() {
        return factorFor(steamCompression);
    }

    private int factorFor(int compression) {
        return switch (compression) {
            case 1 -> 10;
            case 2 -> 100;
            case 3 -> 1000;
            default -> 1;
        };
    }

    @Nullable
    private static RecipeHolder<RbmkOutgasserRecipe> outgasserRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(HbmRecipeTypes.RBMK_OUTGASSER.get(), new RbmkOutgasserRecipe.Input(input), level)
                .orElse(null);
    }

    private static LidType parseLidType(String name) {
        try {
            return name == null || name.isBlank() ? LidType.NONE : LidType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return LidType.NONE;
        }
    }

    public enum LidType {
        NONE,
        NORMAL,
        GLASS
    }

    public enum ConsoleScreenType {
        NONE,
        COL_TEMP,
        ROD_EXTRACTION,
        FUEL_DEPLETION,
        FUEL_POISON,
        FUEL_TEMP;

        public static ConsoleScreenType byOrdinal(int ordinal) {
            ConsoleScreenType[] values = values();
            return ordinal < 0 || ordinal >= values.length ? NONE : values[ordinal];
        }
    }

    public enum AutoControlFunction {
        LINEAR,
        QUAD_UP,
        QUAD_DOWN;

        public static AutoControlFunction byOrdinal(int ordinal) {
            AutoControlFunction[] values = values();
            return values[Math.floorMod(ordinal, values.length)];
        }

        public static AutoControlFunction byName(String name) {
            try {
                return name == null || name.isBlank() ? LINEAR : valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return LINEAR;
            }
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChangedAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChangedAndSync();
    }

    @Override
    public boolean stillValid(Player player) {
        // TileEntityMachineBase/TileEntityRBMKActiveBase used the same
        // 128-block-squared menu validity check for every RBMK GUI in 1.7.10.
        double maxDistance = 128.0D;
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= maxDistance;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            return slot >= AUTOLOADER_INPUT_START
                    && slot < AUTOLOADER_INPUT_END
                    && stack.getItem() instanceof RbmkFuelRodItem
                    && remainingFuelPercent(stack) >= autoloaderCycle;
        }
        // The legacy outgasser accepts its recipe input in slot 0.  Check this
        // before the generic RBMK fuel slot rule: an outgasser is not a fuel
        // column, so routing slot 0 through acceptsFuel() would reject every
        // valid recipe item.
        if (kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            return slot == SLOT_FUEL
                    && !stack.isEmpty()
                    && (level == null || level.isClientSide || outgasserRecipe(level, stack) != null);
        }
        if (slot == SLOT_FUEL) {
            return (kind().acceptsFuel() || kind() == RbmkComponentBlock.Kind.STORAGE) && stack.getItem() instanceof RbmkFuelRodItem;
        }
        if (kind() == RbmkComponentBlock.Kind.STORAGE) {
            return stack.getItem() instanceof RbmkFuelRodItem;
        }
        return kind() == RbmkComponentBlock.Kind.STORAGE;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            return TWO_SLOTS;
        }
        if (kind() == RbmkComponentBlock.Kind.STORAGE) {
            return STORAGE_SLOTS;
        }
        if (kind() == RbmkComponentBlock.Kind.AUTOLOADER && autoloaderPiston <= 0.0D) {
            return AUTOLOADER_SLOTS;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            return slot >= AUTOLOADER_OUTPUT_START && slot < AUTOLOADER_OUTPUT_END && autoloaderPiston <= 0.0D;
        }
        if (kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            return slot == SLOT_BUFFER;
        }
        return kind() == RbmkComponentBlock.Kind.STORAGE;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble("heat", heat);
        tag.putDouble("lastFlux", lastFlux);
        tag.putDouble("lastFluxFastRatio", lastFluxFastRatio);
        tag.putDouble("emittedFlux", emittedFlux);
        tag.putDouble("emittedFastRatio", emittedFastRatio);
        tag.putDouble("controlLevel", controlLevel);
        tag.putDouble("targetControlLevel", targetControlLevel);
        tag.putDouble("startingControlLevel", startingControlLevel);
        if (kind().isControl()) {
            // The old base serializer carried power/hasPower for every
            // control variant.  Ordinary rods keep their fixed powered state,
            // while ReaSim rods use the same fields for their energy buffer.
            tag.putLong("controlPower", controlPower);
            tag.putBoolean("controlHasPower", controlHasPower);
        }
        tag.putDouble("autoLevelLower", autoLevelLower);
        tag.putDouble("autoLevelUpper", autoLevelUpper);
        tag.putDouble("autoHeatLower", autoHeatLower);
        tag.putDouble("autoHeatUpper", autoHeatUpper);
        tag.putString("autoControlFunction", autoControlFunction.name());
        tag.putInt("colorGroup", colorGroup);
        tag.putInt("redstoneLevel", redstoneLevel);
        tag.putInt("steamCompression", steamCompression);
        tag.putInt("boilerConsumption", boilerConsumption);
        tag.putInt("boilerOutput", boilerOutput);
        tag.putInt("reasimWater", reasimWater);
        tag.putInt("reasimSteam", reasimSteam);
        tag.putString("lidType", lidType.name());
        tag.putIntArray("consoleKinds", consoleKinds);
        tag.putIntArray("consoleHeat", consoleHeat);
        tag.putIntArray("consoleMaxHeat", consoleMaxHeat);
        tag.putIntArray("consoleFlux", consoleFlux);
        tag.putIntArray("consoleControl", consoleControl);
        tag.putIntArray("consoleColorGroups", consoleColorGroups);
        tag.putIntArray("consoleCraneIndicators", consoleCraneIndicators);
        tag.putIntArray("consoleFuelCoreHeat", consoleFuelCoreHeat);
        tag.putIntArray("consoleFuelHullHeat", consoleFuelHullHeat);
        tag.putIntArray("consoleFuelDepletion", consoleFuelDepletion);
        tag.putIntArray("consoleFuelXenon", consoleFuelXenon);
        tag.putIntArray("consoleFuelMaxHeat", consoleFuelMaxHeat);
        tag.putIntArray("consoleWater", consoleWater);
        tag.putIntArray("consoleMaxWater", consoleMaxWater);
        tag.putIntArray("consoleSteam", consoleSteam);
        tag.putIntArray("consoleMaxSteam", consoleMaxSteam);
        tag.putIntArray("consoleSteamType", consoleSteamType);
        tag.putIntArray("consoleHeaterInput", consoleHeaterInput);
        tag.putIntArray("consoleHeaterOutput", consoleHeaterOutput);
        tag.putIntArray("consoleHeaterMax", consoleHeaterMax);
        tag.putIntArray("consoleFluxBuffer", consoleFluxBuffer);
        tag.putIntArray("consoleScreenTypes", consoleScreenTypes);
        tag.putIntArray("consoleScreenDisplays", consoleScreenDisplays);
        for (int index = 0; index < consoleScreenColumns.length; index++) {
            tag.putIntArray("consoleScreenColumns" + index, consoleScreenColumns[index]);
        }
        tag.putIntArray("displayKinds", displayKinds);
        tag.putIntArray("displayHeat", displayHeat);
        tag.putIntArray("displayMaxHeat", displayMaxHeat);
        tag.putIntArray("displayControl", displayControl);
        tag.putIntArray("displayColorGroups", displayColorGroups);
        tag.putIntArray("displayCraneIndicators", displayCraneIndicators);
        tag.putIntArray("displayFuelDepletion", displayFuelDepletion);
        tag.putInt("consoleTotalFlux", consoleTotalFlux);
        tag.putInt("consoleRotation", consoleRotation);
        tag.putInt("displayRotation", displayRotation);
        tag.putBoolean("crane", craneCenter != null);
        tag.putInt("craneRotationOffset", craneRotationOffset);
        if (craneCenter != null) {
            tag.putLong("craneCenter", craneCenter.asLong());
        }
        tag.putInt("craneSpanF", craneSpanF);
        tag.putInt("craneSpanB", craneSpanB);
        tag.putInt("craneSpanL", craneSpanL);
        tag.putInt("craneSpanR", craneSpanR);
        tag.putInt("craneHeight", craneHeight);
        tag.putDouble("cranePosFront", cranePosFront);
        tag.putDouble("cranePosLeft", cranePosLeft);
        // TileEntityCraneConsole.writeToNBT persisted only the geometry,
        // carriage coordinates, and held stack.  Tilt/progress/goesDown and
        // the two meter values were runtime fields; they were supplied to
        // clients by the old network packet and deliberately reset after a
        // server reload.  They are added back only by getUpdateTag below.
        if (!craneLoadedItem.isEmpty()) {
            tag.put("craneLoadedItem", craneLoadedItem.saveOptional(registries));
        }
        tag.putDouble("outgasserProgress", outgasserProgress);
        tag.putDouble("autoloaderPiston", autoloaderPiston);
        tag.putInt("autoloaderDelay", autoloaderDelay);
        tag.putInt("autoloaderCycle", autoloaderCycle);
        tag.putBoolean("autoloaderRetracting", autoloaderRetracting);
        tag.put("water", water.save());
        tag.put("steam", steam.save());
        tag.put("heaterInput", heaterInput.save());
        tag.put("heaterOutput", heaterOutput.save());
        tag.put("outgasserGas", outgasserGas.save());
        tag.put("reasimInletWater", reasimInletWater.save());
        tag.put("reasimOutletSteam", reasimOutletSteam.save());
        // TileEntityRBMKCooler used the legacy t0/t1 keys for its receiving
        // cold and sending warm perfluoromethyl tanks.  Keep those exact keys
        // so old worlds retain their coolant when the BE class is merged.
        if (kind() == RbmkComponentBlock.Kind.COOLER) {
            tag.put("t0", coolerInput.save());
            tag.put("t1", coolerOutput.save());
        }
        if (linkedReactor != null) {
            tag.putLong("linkedReactor", linkedReactor.asLong());
        }
        // Keep the exact 1.7.10 panel keys alongside the modern link key.
        // Console/Display TileEntities persisted their target as tX/tY/tZ;
        // the console also persisted rotation and six screen selections.
        // Writing both forms makes the migrated state round-trip without
        // changing the modern fields used by the network payloads.
        if (kind() == RbmkComponentBlock.Kind.CONSOLE || kind() == RbmkComponentBlock.Kind.DISPLAY) {
            BlockPos target = linkedReactor == null ? BlockPos.ZERO : linkedReactor;
            tag.putInt("tX", target.getX());
            tag.putInt("tY", target.getY());
            tag.putInt("tZ", target.getZ());
            tag.putInt("rotation", kind() == RbmkComponentBlock.Kind.CONSOLE ? consoleRotation : displayRotation);
            if (kind() == RbmkComponentBlock.Kind.CONSOLE) {
                for (int index = 0; index < consoleScreenTypes.length; index++) {
                    tag.putByte("t" + index, (byte) consoleScreenTypes[index]);
                    tag.putIntArray("s" + index, consoleScreenColumns[index]);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        // 1.7.10 used a lower-case `items` list with a byte `slot` field;
        // ContainerHelper only understands the modern `Items` list.  Read
        // the legacy list when the modern list is absent so rods, outgasser
        // inputs and crane/autoloader inventories survive a world migration.
        if (!tag.contains("Items") && tag.contains("items", Tag.TAG_LIST)) {
            ListTag legacyItems = tag.getList("items", Tag.TAG_COMPOUND);
            for (int index = 0; index < legacyItems.size(); index++) {
                CompoundTag itemTag = legacyItems.getCompound(index);
                int slot = itemTag.getByte("slot") & 0xFF;
                if (slot >= 0 && slot < items.size()) {
                    items.set(slot, ItemStack.parseOptional(registries, itemTag));
                }
            }
        }
        heat = tag.getDouble("heat");
        if (tag.contains("lastFlux")) {
            lastFlux = tag.getDouble("lastFlux");
        } else if (tag.contains("fluxQuantity")) {
            // Old rods persisted the incoming buffer under fluxQuantity and
            // its fast-neutron ratio under fluxMod.
            lastFlux = tag.getDouble("fluxQuantity");
        } else if (tag.contains("fluxFast") || tag.contains("fluxSlow")) {
            lastFlux = tag.getDouble("fluxFast") + tag.getDouble("fluxSlow");
        } else {
            lastFlux = 0.0D;
        }
        if (tag.contains("lastFluxFastRatio")) {
            lastFluxFastRatio = Math.max(0.0D, Math.min(1.0D, tag.getDouble("lastFluxFastRatio")));
        } else if (tag.contains("fluxMod")) {
            lastFluxFastRatio = Math.max(0.0D, Math.min(1.0D, tag.getDouble("fluxMod")));
        } else if (tag.contains("fluxFast") || tag.contains("fluxSlow")) {
            double total = tag.getDouble("fluxFast") + tag.getDouble("fluxSlow");
            lastFluxFastRatio = total > 0.0D ? tag.getDouble("fluxFast") / total : 0.0D;
        } else {
            lastFluxFastRatio = 0.0D;
        }
        emittedFlux = tag.contains("emittedFlux") ? Math.max(0.0D, tag.getDouble("emittedFlux")) : lastFlux;
        emittedFastRatio = tag.contains("emittedFastRatio")
                ? Math.max(0.0D, Math.min(1.0D, tag.getDouble("emittedFastRatio")))
                : lastFluxFastRatio;
        controlLevel = tag.contains("controlLevel") ? tag.getDouble("controlLevel") : tag.getDouble("level");
        targetControlLevel = tag.contains("targetControlLevel") ? tag.getDouble("targetControlLevel") : tag.getDouble("targetLevel");
        startingControlLevel = tag.contains("startingControlLevel")
                ? tag.getDouble("startingControlLevel")
                : (tag.contains("startingLevel") ? tag.getDouble("startingLevel") : controlLevel);
        controlPower = tag.contains("controlPower")
                ? Math.max(0L, Math.min(REASIM_CONTROL_MAX_POWER, tag.getLong("controlPower")))
                : Math.max(0L, Math.min(REASIM_CONTROL_MAX_POWER, tag.getLong("power")));
        controlHasPower = tag.contains("controlHasPower") ? tag.getBoolean("controlHasPower") : tag.getBoolean("hasPower");
        autoLevelLower = tag.contains("autoLevelLower") ? tag.getDouble("autoLevelLower") : tag.getDouble("levelLower");
        autoLevelUpper = tag.contains("autoLevelUpper") ? tag.getDouble("autoLevelUpper") : tag.getDouble("levelUpper");
        autoHeatLower = tag.contains("autoHeatLower") ? tag.getDouble("autoHeatLower") : tag.getDouble("heatLower");
        autoHeatUpper = tag.contains("autoHeatUpper") ? tag.getDouble("autoHeatUpper") : tag.getDouble("heatUpper");
        if (tag.contains("autoControlFunction")) {
            autoControlFunction = AutoControlFunction.byName(tag.getString("autoControlFunction"));
        } else if (tag.contains("function")) {
            autoControlFunction = AutoControlFunction.byOrdinal(tag.getInt("function"));
        } else {
            autoControlFunction = AutoControlFunction.LINEAR;
        }
        colorGroup = tag.contains("colorGroup") ? tag.getInt("colorGroup") : (tag.contains("color") ? tag.getInt("color") : -1);
        colorGroup = colorGroup < 0 ? -1 : Math.min(4, colorGroup);
        redstoneLevel = tag.getInt("redstoneLevel");
        steamCompression = Math.max(0, Math.min(3, tag.getInt("steamCompression")));
        boilerConsumption = tag.getInt("boilerConsumption");
        boilerOutput = tag.getInt("boilerOutput");
        reasimWater = Math.max(0, Math.min(REASIM_INTERNAL_CAPACITY, tag.getInt("reasimWater")));
        reasimSteam = Math.max(0, Math.min(REASIM_INTERNAL_CAPACITY, tag.getInt("reasimSteam")));
        craneIndicator = tag.getInt("craneIndicator");
        lidType = parseLidType(tag.getString("lidType"));
        outgasserProgress = tag.getDouble("outgasserProgress");
        autoloaderPiston = tag.contains("autoloaderPiston") ? tag.getDouble("autoloaderPiston") : tag.getDouble("piston");
        autoloaderDelay = tag.contains("autoloaderDelay") ? tag.getInt("autoloaderDelay") : tag.getInt("delay");
        autoloaderCycle = tag.contains("autoloaderCycle") ? tag.getInt("autoloaderCycle") : (tag.contains("cycle") ? tag.getInt("cycle") : 50);
        autoloaderCycle = Math.max(5, Math.min(95, autoloaderCycle));
        autoloaderRetracting = tag.contains("autoloaderRetracting")
                ? tag.getBoolean("autoloaderRetracting")
                : (!tag.contains("ret") || tag.getBoolean("ret"));
        loadTankCompat(water, tag, "water", "feed");
        loadTankCompat(steam, tag, "steam", "steam");
        // The selected steam type is authoritative in the old FluidTank NBT.
        // Keep it in sync with the migrated compression field, including when
        // a tank was empty (old FluidTank retained its type at zero fill).
        int loadedCompression = steamCompressionFor(steam.type());
        if (loadedCompression >= 0) {
            steamCompression = loadedCompression;
        } else if (steam.amount() == 0) {
            steam.setType(steamDefinition(steamCompression));
        }
        loadTankCompat(heaterInput, tag, "heaterInput", "feed");
        loadTankCompat(heaterOutput, tag, "heaterOutput", "steam");
        loadTankCompat(outgasserGas, tag, "outgasserGas", "gas");
        loadTankCompat(reasimInletWater, tag, "reasimInletWater", "tank");
        loadTankCompat(reasimOutletSteam, tag, "reasimOutletSteam", "tank");
        if (kind() == RbmkComponentBlock.Kind.COOLER) {
            if (tag.contains("t0")) {
                loadCoolerTank(coolerInput, tag, "t0");
            } else if (tag.contains("coolerInput")) {
                coolerInput.load(tag.getCompound("coolerInput"));
            }
            if (tag.contains("t1")) {
                loadCoolerTank(coolerOutput, tag, "t1");
            } else if (tag.contains("coolerOutput")) {
                coolerOutput.load(tag.getCompound("coolerOutput"));
            }
            // The old timer was transient and was not serialized.
            coolerTimer = 0;
        }
        boolean panelTarget = kind() == RbmkComponentBlock.Kind.CONSOLE
                || kind() == RbmkComponentBlock.Kind.DISPLAY;
        if (tag.contains("linkedReactor")) {
            linkedReactor = BlockPos.of(tag.getLong("linkedReactor"));
        } else if (panelTarget && (tag.contains("tX") || tag.contains("tY") || tag.contains("tZ"))) {
            // 1.7.10 persisted panel targets as three integer tags.  Read
            // those names when loading an old world instead of silently
            // dropping the link.
            linkedReactor = new BlockPos(tag.getInt("tX"), tag.getInt("tY"), tag.getInt("tZ"));
        } else {
            // The old fields were primitive ints and therefore defaulted to
            // the origin even before the linker tool was used.
            linkedReactor = panelTarget ? BlockPos.ZERO : null;
        }
        loadIntArray(tag, "consoleKinds", consoleKinds, -1);
        loadIntArray(tag, "consoleHeat", consoleHeat, 0);
        loadIntArray(tag, "consoleMaxHeat", consoleMaxHeat, 1500);
        loadIntArray(tag, "consoleFlux", consoleFlux, 0);
        loadIntArray(tag, "consoleControl", consoleControl, 0);
        loadIntArray(tag, "consoleColorGroups", consoleColorGroups, -1);
        loadIntArray(tag, "consoleCraneIndicators", consoleCraneIndicators, 0);
        loadIntArray(tag, "consoleFuelCoreHeat", consoleFuelCoreHeat, 0);
        loadIntArray(tag, "consoleFuelHullHeat", consoleFuelHullHeat, 0);
        loadIntArray(tag, "consoleFuelDepletion", consoleFuelDepletion, 0);
        loadIntArray(tag, "consoleFuelXenon", consoleFuelXenon, 0);
        loadIntArray(tag, "consoleFuelMaxHeat", consoleFuelMaxHeat, 0);
        loadIntArray(tag, "consoleWater", consoleWater, 0);
        loadIntArray(tag, "consoleMaxWater", consoleMaxWater, 0);
        loadIntArray(tag, "consoleSteam", consoleSteam, 0);
        loadIntArray(tag, "consoleMaxSteam", consoleMaxSteam, 0);
        loadIntArray(tag, "consoleSteamType", consoleSteamType, 0);
        loadIntArray(tag, "consoleHeaterInput", consoleHeaterInput, 0);
        loadIntArray(tag, "consoleHeaterOutput", consoleHeaterOutput, 0);
        loadIntArray(tag, "consoleHeaterMax", consoleHeaterMax, 0);
        loadIntArray(tag, "consoleFluxBuffer", consoleFluxBuffer, 0);
        loadIntArray(tag, "consoleScreenTypes", consoleScreenTypes, 0);
        loadIntArray(tag, "consoleScreenDisplays", consoleScreenDisplays, 0);
        for (int index = 0; index < consoleScreenColumns.length; index++) {
            String modernColumnsKey = "consoleScreenColumns" + index;
            if (tag.contains(modernColumnsKey)) {
                consoleScreenColumns[index] = validConsoleIndices(tag.getIntArray(modernColumnsKey));
            } else {
                // Legacy Console used s0..s5 for the selected column list.
                consoleScreenColumns[index] = validConsoleIndices(tag.getIntArray("s" + index));
            }
            // Legacy Console used a byte t0..t5 for each screen type.  The
            // modern int array has priority when present, so this fallback
            // only applies to an actual 1.7.10 tag.
            if (kind() == RbmkComponentBlock.Kind.CONSOLE
                    && !tag.contains("consoleScreenTypes")
                    && tag.contains("t" + index)) {
                consoleScreenTypes[index] = tag.getByte("t" + index);
            }
        }
        loadIntArray(tag, "displayKinds", displayKinds, -1);
        loadIntArray(tag, "displayHeat", displayHeat, 0);
        loadIntArray(tag, "displayMaxHeat", displayMaxHeat, 1500);
        loadIntArray(tag, "displayControl", displayControl, 0);
        loadIntArray(tag, "displayColorGroups", displayColorGroups, -1);
        loadIntArray(tag, "displayCraneIndicators", displayCraneIndicators, 0);
        loadIntArray(tag, "displayFuelDepletion", displayFuelDepletion, 0);
        consoleTotalFlux = tag.getInt("consoleTotalFlux");
        consoleRotation = tag.contains("consoleRotation")
                ? tag.getInt("consoleRotation") & 3
                : (kind() == RbmkComponentBlock.Kind.CONSOLE && tag.contains("rotation")
                ? tag.getInt("rotation") & 3 : 0);
        displayRotation = tag.contains("displayRotation")
                ? tag.getInt("displayRotation") & 3
                : (kind() == RbmkComponentBlock.Kind.DISPLAY && tag.contains("rotation")
                ? tag.getInt("rotation") & 3 : 0);
        // Old deserialize() received syncFront/syncLeft/syncProgress while
        // leaving the current client values untouched; the next client tick
        // then rendered from the previous value to the sync value. Preserve
        // that interpolation when a NeoForge block-update packet arrives.
        // A disk load intentionally has no craneRenderSync marker: the old
        // writeToNBT omitted all of these runtime fields and therefore reset
        // them after a restart.
        boolean clientUpdate = level != null && level.isClientSide && tag.getBoolean("craneRenderSync");
        double previousCraneTiltFront = craneTiltFront;
        double previousCraneTiltLeft = craneTiltLeft;
        double previousCranePosFront = cranePosFront;
        double previousCranePosLeft = cranePosLeft;
        double previousCraneProgress = craneProgress;
        boolean legacyCraneGeometry = tag.contains("centerX") || tag.contains("centerY") || tag.contains("centerZ")
                || tag.contains("spanF") || tag.contains("spanB") || tag.contains("spanL") || tag.contains("spanR");
        boolean craneConfigured = tag.contains("crane") ? tag.getBoolean("crane") : legacyCraneGeometry;
        if (craneConfigured) {
            craneCenter = tag.contains("craneCenter")
                    ? BlockPos.of(tag.getLong("craneCenter"))
                    : new BlockPos(tag.getInt("centerX"), tag.getInt("centerY"), tag.getInt("centerZ"));
        } else {
            craneCenter = null;
        }
        craneRotationOffset = tag.getInt("craneRotationOffset");
        craneSpanF = tag.contains("craneSpanF") ? tag.getInt("craneSpanF") : tag.getInt("spanF");
        craneSpanB = tag.contains("craneSpanB") ? tag.getInt("craneSpanB") : tag.getInt("spanB");
        craneSpanL = tag.contains("craneSpanL") ? tag.getInt("craneSpanL") : tag.getInt("spanL");
        craneSpanR = tag.contains("craneSpanR") ? tag.getInt("craneSpanR") : tag.getInt("spanR");
        craneHeight = tag.contains("craneHeight") ? tag.getInt("craneHeight") : tag.getInt("height");
        craneTiltFront = clientUpdate ? tag.getDouble("craneTiltFront") : 0.0D;
        craneLastTiltFront = clientUpdate ? previousCraneTiltFront : craneTiltFront;
        craneTiltLeft = clientUpdate ? tag.getDouble("craneTiltLeft") : 0.0D;
        craneLastTiltLeft = clientUpdate ? previousCraneTiltLeft : craneTiltLeft;
        cranePosFront = tag.contains("cranePosFront") ? tag.getDouble("cranePosFront") : tag.getDouble("posFront");
        craneLastPosFront = clientUpdate ? previousCranePosFront : cranePosFront;
        cranePosLeft = tag.contains("cranePosLeft") ? tag.getDouble("cranePosLeft") : tag.getDouble("posLeft");
        craneLastPosLeft = clientUpdate ? previousCranePosLeft : cranePosLeft;
        craneProgress = clientUpdate && tag.contains("craneProgress") ? tag.getDouble("craneProgress") : 1.0D;
        craneLastProgress = clientUpdate ? previousCraneProgress : craneProgress;
        craneGoesDown = clientUpdate && tag.getBoolean("craneGoesDown");
        craneLoadedHeat = clientUpdate ? tag.getDouble("craneLoadedHeat") : 0.0D;
        craneLoadedEnrichment = clientUpdate ? tag.getDouble("craneLoadedEnrichment") : 0.0D;
        craneLoadedItem = ItemStack.parseOptional(registries,
                tag.contains("craneLoadedItem") ? tag.getCompound("craneLoadedItem") : tag.getCompound("held"));
    }

    private static void loadIntArray(CompoundTag tag, String key, int[] target, int fallback) {
        int[] source = tag.getIntArray(key);
        java.util.Arrays.fill(target, fallback);
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    /**
     * Load an RBMK tank from either the modern nested representation or the
     * flat FluidTank representation emitted by 1.7.10.  The old tank kept its
     * selected fluid and pressure even when the amount was zero; populate the
     * amount last so HbmFluidTank's type-change rules do not erase that state.
     */
    private static void loadTankCompat(HbmFluidTank tank, CompoundTag root, String modernKey, String legacyKey) {
        if (root.get(modernKey) instanceof CompoundTag nested) {
            loadNestedTankPreservingEmpty(tank, nested);
            return;
        }
        if (!hasFlatTank(root, legacyKey)) {
            return;
        }
        int capacity = root.contains(legacyKey + "_max") && root.getInt(legacyKey + "_max") > 0
                ? root.getInt(legacyKey + "_max") : tank.capacity();
        int amount = root.getInt(legacyKey);
        HbmFluidDefinition type = fluidTypeFromLegacy(root, legacyKey, tank.type());
        int pressure = root.contains(legacyKey + "_p") ? Math.max(0, root.getShort(legacyKey + "_p")) : 0;
        tank.setCapacity(capacity);
        tank.setType(type);
        tank.setPressure(pressure);
        tank.setAmount(amount);
    }

    private static boolean hasFlatTank(CompoundTag root, String prefix) {
        return root.contains(prefix) || root.contains(prefix + "_max")
                || root.contains(prefix + "_type") || root.contains(prefix + "_p");
    }

    private static void loadNestedTankPreservingEmpty(HbmFluidTank tank, CompoundTag nested) {
        int capacity = nested.contains("capacity") && nested.getInt("capacity") > 0
                ? nested.getInt("capacity") : tank.capacity();
        int amount = nested.getInt("amount");
        HbmFluidDefinition type = HbmFluids.byName(nested.getString("type")).orElse(tank.type());
        int pressure = Math.max(0, nested.getInt("pressure"));
        tank.setCapacity(capacity);
        tank.setType(type);
        tank.setPressure(pressure);
        tank.setAmount(amount);
    }

    private static HbmFluidDefinition fluidTypeFromLegacy(CompoundTag root, String prefix, HbmFluidDefinition fallback) {
        HbmFluidDefinition byName = HbmFluids.byName(root.getString(prefix + "_type")).orElse(null);
        return byName != null ? byName : HbmFluids.byOldId(root.getInt(prefix + "_type")).orElse(fallback);
    }

    /**
     * Read either the modern nested tank tag or the flat 1.7.10 FluidTank
     * keys ({@code t0}, {@code t0_max}, {@code t0_type}, {@code t0_p}).
     * The latter is what TileEntityRBMKCooler.writeToNBT emitted.
     */
    private static void loadCoolerTank(HbmFluidTank tank, CompoundTag root, String key) {
        loadTankCompat(tank, root, key, key);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        // TileEntityRBMKBase serialized this transient lamp counter in its
        // network packet, but never in writeToNBT().  Keep it out of disk
        // saves while retaining the old client/display update.
        tag.putInt("craneIndicator", craneIndicator);
        if (kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            // NeoForge uses this tag for the client-side block-entity update
            // packet.  Keep the legacy disk/network split explicit: these
            // values correspond to TileEntityCraneConsole.serialize(), not
            // its writeToNBT() persistence payload.
            tag.putBoolean("craneRenderSync", true);
            tag.putDouble("craneTiltFront", craneTiltFront);
            tag.putDouble("craneTiltLeft", craneTiltLeft);
            tag.putDouble("craneProgress", craneProgress);
            tag.putBoolean("craneGoesDown", craneGoesDown);
            tag.putDouble("craneLoadedHeat", craneLoadedHeat);
            tag.putDouble("craneLoadedEnrichment", craneLoadedEnrichment);
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class RbmkFluidHandler implements IFluidHandler {
        private final BlockPos exposedPos;
        @Nullable
        private final Direction side;

        private RbmkFluidHandler(BlockPos exposedPos, @Nullable Direction side) {
            this.exposedPos = exposedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                return loaderSource() == null ? 0 : 1;
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET || kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return 1;
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return 2;
            }
            return kind() == RbmkComponentBlock.Kind.OUTGASSER ? 1 : 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                RbmkComponentBlockEntity source = loaderSource();
                return source == null ? FluidStack.EMPTY : loaderOutputFluid(source);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return reasimInletWater.getFluidInTank(tank);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.getFluidInTank(tank);
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return tank == 0 ? coolerInput.getFluidInTank(0) : coolerOutput.getFluidInTank(0);
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                return tank == 0 ? heaterInput.getFluidInTank(0) : heaterOutput.getFluidInTank(0);
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                return tank == 0 ? outgasserGas.getFluidInTank(0) : FluidStack.EMPTY;
            }
            return tank == 0 ? water.getFluidInTank(0) : steam.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                RbmkComponentBlockEntity source = loaderSource();
                return source == null ? 0 : loaderOutputCapacity(source);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return reasimInletWater.getTankCapacity(tank);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.getTankCapacity(tank);
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return tank == 0 ? coolerInput.capacity() : coolerOutput.capacity();
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                return tank == 0 ? heaterInput.capacity() : heaterOutput.capacity();
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                return tank == 0 ? outgasserGas.capacity() : 0;
            }
            return tank == 0 ? water.capacity() : steam.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return tank == 0 && isWaterStack(stack);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET || kind == RbmkComponentBlock.Kind.LOADER) {
                return false;
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return canFillPort() && tank == 0 && isCoolantInputStack(stack);
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER || tank == 1 || !canFillPort()) {
                return false;
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                return HbmFluids.fromNeoFluid(stack.getFluid())
                        .flatMap(HbmThermalConversions::firstHeatExchangerStep)
                        .isPresent();
            }
            return water.isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                if (!isWaterStack(resource)) {
                    return 0;
                }
                return reasimInletWater.fill(resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET || kind == RbmkComponentBlock.Kind.LOADER) {
                return 0;
            }
            if (!canFillPort()) {
                return 0;
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                if (!isCoolantInputStack(resource)) {
                    return 0;
                }
                return coolerInput.fill(resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                HbmFluidDefinition incoming = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
                if (HbmThermalConversions.firstHeatExchangerStep(incoming).isEmpty()) {
                    return 0;
                }
                // FluidTank in 1.7.10 was retargeted by the accepted fluid's
                // trait before filling.  The migrated tank starts with a
                // diagnostic default type, so an empty tank must be retargeted
                // before HbmFluidTank#fill would otherwise reject the fluid.
                if (heaterInput.amount() == 0 && heaterInput.type() != incoming) {
                    if (action.simulate()) {
                        return Math.min(resource.getAmount(), heaterInput.capacity());
                    }
                    heaterInput.setType(incoming);
                }
                return heaterInput.fill(resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                return 0;
            }
            return water.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return FluidStack.EMPTY;
            }
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                RbmkComponentBlockEntity source = loaderSource();
                return source == null || !loaderPortAllows(loaderOutputDefinition(source))
                        ? FluidStack.EMPTY : loaderDrain(source, resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.drain(resource, action);
            }
            if (!canDrainPort()) {
                return FluidStack.EMPTY;
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return coolerOutput.drain(resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                return heaterOutput.drain(resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                return outgasserGas.drain(resource, action);
            }
            return steam.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return FluidStack.EMPTY;
            }
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                RbmkComponentBlockEntity source = loaderSource();
                return source == null || !loaderPortAllows(loaderOutputDefinition(source))
                        ? FluidStack.EMPTY : loaderDrain(source, maxDrain, action);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.drain(maxDrain, action);
            }
            if (!canDrainPort()) {
                return FluidStack.EMPTY;
            }
            if (kind == RbmkComponentBlock.Kind.COOLER) {
                return coolerOutput.drain(maxDrain, action);
            }
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                return heaterOutput.drain(maxDrain, action);
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                return outgasserGas.drain(maxDrain, action);
            }
            return steam.drain(maxDrain, action);
        }

        private boolean isWaterStack(FluidStack stack) {
            return !stack.isEmpty()
                    && HbmFluids.fromNeoFluid(stack.getFluid())
                    .map(definition -> definition == HbmFluids.byName("water").orElse(HbmFluids.none()))
                    .orElse(false);
        }

        private boolean isCoolantInputStack(FluidStack stack) {
            return !stack.isEmpty()
                    && HbmFluids.fromNeoFluid(stack.getFluid())
                    .map(definition -> definition == HbmFluids.byName("perfluoromethyl_cold").orElse(HbmFluids.none()))
                    .orElse(false);
        }

        private boolean canFillPort() {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET) {
                return true;
            }
            return (kind == RbmkComponentBlock.Kind.BOILER
                    || kind == RbmkComponentBlock.Kind.HEATER
                    || kind == RbmkComponentBlock.Kind.COOLER)
                    && exposedPos.equals(worldPosition)
                    && (side == null || side == Direction.DOWN);
        }

        private boolean canDrainPort() {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return true;
            }
            if (!(kind == RbmkComponentBlock.Kind.BOILER
                    || kind == RbmkComponentBlock.Kind.HEATER
                    || kind == RbmkComponentBlock.Kind.COOLER
                    || kind == RbmkComponentBlock.Kind.OUTGASSER)) {
                return false;
            }
            if (side == null) {
                return true;
            }
            if (exposedPos.equals(worldPosition.above(RbmkComponentBlock.columnHeight(level)))
                    && side == Direction.DOWN) {
                return true;
            }
            if (kind == RbmkComponentBlock.Kind.OUTGASSER
                    && !hasRbmkLoaderBelow()
                    && exposedPos.equals(worldPosition.below())
                    && side == Direction.UP) {
                return true;
            }
            return isAnyLoaderOutputPort(exposedPos, side);
        }

        /** Exact 1.7.10 RBMKLoader.canConnect direction/type rule. */
        private boolean loaderPortAllows(@Nullable HbmFluidDefinition type) {
            if (type == null || type.isNone() || side == null) {
                return side == null;
            }
            if (side == Direction.UP) {
                return isLegacyHeatable(type);
            }
            return isLegacyCoolable(type);
        }

        private boolean isLegacyHeatable(HbmFluidDefinition type) {
            return switch (type.name()) {
                case "air", "water", "oil", "oil_ds", "crackoil", "crackoil_ds",
                        "coolant", "perfluoromethyl_cold", "perfluoromethyl", "mug", "blood",
                        "heavywater", "sodium", "lead", "thorium_salt" -> true;
                default -> false;
            };
        }

        private boolean isLegacyCoolable(HbmFluidDefinition type) {
            return type.name().equals("perfluoromethyl") || switch (type.name()) {
                case "steam", "hotsteam", "superhotsteam", "ultrahotsteam",
                        "hotoil", "hotoil_ds", "hotcrackoil", "hotcrackoil_ds",
                        "coolant_hot", "perfluoromethyl_hot", "mug_hot", "blood_hot",
                        "heavywater_hot", "sodium_hot", "lead_hot", "thorium_salt_hot" -> true;
                default -> false;
            };
        }

        @Nullable
        private RbmkComponentBlockEntity loaderSource() {
            if (level == null || kind() != RbmkComponentBlock.Kind.LOADER) {
                return null;
            }
            BlockEntity above = level.getBlockEntity(worldPosition.above());
            if (above instanceof RbmkComponentBlockEntity source && loaderCanExpose(source.kind())) {
                return source;
            }
            BlockEntity aboveTwo = level.getBlockEntity(worldPosition.above(2));
            if (aboveTwo instanceof RbmkComponentBlockEntity source && loaderCanExpose(source.kind())) {
                return source;
            }
            return null;
        }

        private boolean loaderCanExpose(RbmkComponentBlock.Kind sourceKind) {
            return sourceKind == RbmkComponentBlock.Kind.BOILER
                    || sourceKind == RbmkComponentBlock.Kind.HEATER
                    || sourceKind == RbmkComponentBlock.Kind.OUTGASSER
                    || sourceKind == RbmkComponentBlock.Kind.COOLER;
        }

        private FluidStack loaderOutputFluid(RbmkComponentBlockEntity source) {
            return switch (source.kind()) {
                case BOILER -> source.steam.getFluidInTank(0);
                case HEATER -> source.heaterOutput.getFluidInTank(0);
                case OUTGASSER -> source.outgasserGas.getFluidInTank(0);
                case COOLER -> source.coolerOutput.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Nullable
        private HbmFluidDefinition loaderOutputDefinition(RbmkComponentBlockEntity source) {
            FluidStack stack = loaderOutputFluid(source);
            return stack.isEmpty() ? null : HbmFluids.fromNeoFluid(stack.getFluid()).orElse(null);
        }

        private int loaderOutputCapacity(RbmkComponentBlockEntity source) {
            return switch (source.kind()) {
                case BOILER -> source.steam.capacity();
                case HEATER -> source.heaterOutput.capacity();
                case OUTGASSER -> source.outgasserGas.capacity();
                case COOLER -> source.coolerOutput.capacity();
                default -> 0;
            };
        }

        private FluidStack loaderDrain(RbmkComponentBlockEntity source, FluidStack resource, FluidAction action) {
            return switch (source.kind()) {
                case BOILER -> source.steam.drain(resource, action);
                case HEATER -> source.heaterOutput.drain(resource, action);
                case OUTGASSER -> source.outgasserGas.drain(resource, action);
                case COOLER -> source.coolerOutput.drain(resource, action);
                default -> FluidStack.EMPTY;
            };
        }

        private FluidStack loaderDrain(RbmkComponentBlockEntity source, int maxDrain, FluidAction action) {
            return switch (source.kind()) {
                case BOILER -> source.steam.drain(maxDrain, action);
                case HEATER -> source.heaterOutput.drain(maxDrain, action);
                case OUTGASSER -> source.outgasserGas.drain(maxDrain, action);
                case COOLER -> source.coolerOutput.drain(maxDrain, action);
                default -> FluidStack.EMPTY;
            };
        }
    }
}
