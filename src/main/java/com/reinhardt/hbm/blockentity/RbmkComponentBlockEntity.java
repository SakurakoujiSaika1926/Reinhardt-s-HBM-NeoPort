package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.DigammaSpearEntity;
import com.reinhardt.hbm.entity.RbmkDebrisEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmThermalConversions;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.menu.RbmkComponentMenu;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RbmkComponentBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, NeutronFluxProvider, MenuProvider {
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
    public static final int CONSOLE_GRID_SIZE = 15;
    public static final int CONSOLE_COLUMN_COUNT = CONSOLE_GRID_SIZE * CONSOLE_GRID_SIZE;
    public static final int DISPLAY_GRID_SIZE = 7;
    public static final int DISPLAY_COLUMN_COUNT = DISPLAY_GRID_SIZE * DISPLAY_GRID_SIZE;
    private static final double HEAT_EXCHANGER_TU_PER_DEGREE = 2_000.0D;

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

    private double heat;
    private double lastFlux;
    private double lastFluxFastRatio;
    private double outgasserProgress;
    private double controlLevel;
    private double lastControlLevel;
    private double targetControlLevel;
    private double startingControlLevel;
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
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RbmkComponentBlockEntity rbmk) {
        if (level.isClientSide) {
            rbmk.clientTick(level, pos);
            return;
        }
        RbmkComponentBlock.Kind kind = rbmk.kind();
        if (kind.isColumn()) {
            if (rbmk.craneIndicator > 0) {
                rbmk.craneIndicator--;
            }
            rbmk.diffuseHeat(level, pos);
            if (HbmConfig.RBMK_REASIM_BOILERS.get()) {
                rbmk.boilReasimWater();
            }
        }
        if (kind.acceptsFuel()) {
            rbmk.tickFuel(level, pos);
        } else if (kind == RbmkComponentBlock.Kind.MODERATOR) {
            rbmk.heat = Math.max(20.0D, rbmk.heat * 0.999D);
        } else if (kind == RbmkComponentBlock.Kind.COOLER) {
            rbmk.heat = Math.max(20.0D, rbmk.heat - 1.0D);
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
            rbmk.tickControl();
        }
        if (kind == RbmkComponentBlock.Kind.STORAGE && level.getGameTime() % 10L == 0L) {
            rbmk.compactStorage();
        }
        rbmk.redstoneLevel = Math.max(0, Math.min(15, (int) Math.round(rbmk.heat / 100.0D)));
    }

    public RbmkComponentBlock.Kind kind() {
        if (getBlockState().getBlock() instanceof RbmkComponentBlock block) {
            return block.kind();
        }
        return RbmkComponentBlock.Kind.BLANK;
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
        double flux = Math.max(0.0D, lastFlux);
        if (lidType != LidType.NONE) {
            flux *= 0.75D;
        }
        return NeutronFluxProvider.NeutronFlux.fromRatio(flux, lastFluxFastRatio);
    }

    public boolean canUseLid() {
        return kind().isColumn() && !kind().isControl();
    }

    public boolean hasLid() {
        return lidType != LidType.NONE;
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
        if (!hasLid()) {
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
        RbmkComponentBlock.Kind kind = kind();
        if (kind.acceptsFuel() && !items.get(SLOT_FUEL).isEmpty()) {
            if (level != null && !level.isClientSide) {
                if (!coldEnoughForManual(items.get(SLOT_FUEL))) {
                    player.displayClientMessage(Component.translatable("rbmk.rod.too_hot"), true);
                    return true;
                }
                ItemStack extracted = items.get(SLOT_FUEL);
                items.set(SLOT_FUEL, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(extracted);
                setChangedAndSync();
            }
            return true;
        }
        if (kind.isControl()) {
            if (level != null && !level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    double target = targetControlLevel - 0.25D;
                    if (target < 0.0D) {
                        target = 1.0D;
                    }
                    setTargetControlLevel(target);
                } else {
                    double target = targetControlLevel + 0.25D;
                    if (target > 1.0D) {
                        target = 0.0D;
                    }
                    setTargetControlLevel(target);
                }
                setChangedAndSync();
            }
            return true;
        }
        if (kind == RbmkComponentBlock.Kind.BOILER && player.isShiftKeyDown()) {
            if (level != null && !level.isClientSide) {
                cycleSteamCompression();
                player.displayClientMessage(Component.translatable(
                        "rbmk.boiler.type",
                        Component.translatable(steamFluid().translationKey())
                ), true);
            }
            return true;
        }
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
        ItemStack fuel = items.get(SLOT_FUEL);
        if (kind().acceptsFuel() && fuel.getItem() instanceof RbmkFuelRodItem rod) {
            return rod.meltingPoint(fuel);
        }
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
                data.put("f_xenon", stripNumber(RbmkFuelRodItem.xenon(fuel)) + "%");
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
        if (Math.rint(value) == value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    public void applyAutoControl(int function, int levelUpper, int levelLower, int heatUpper, int heatLower) {
        if (!kind().isAutomaticControl()) {
            return;
        }
        autoControlFunction = AutoControlFunction.byOrdinal(function);
        autoLevelUpper = Math.max(0.0D, Math.min(100.0D, levelUpper));
        autoLevelLower = Math.max(0.0D, Math.min(100.0D, levelLower));
        autoHeatUpper = Math.max(0.0D, Math.min(9999.0D, heatUpper));
        autoHeatLower = Math.max(0.0D, Math.min(9999.0D, heatLower));
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
        return target != null && craneCanTargetInteract(target);
    }

    public boolean isPlayerInCraneOperationArea(Player player) {
        if (kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return false;
        }
        return craneOperationArea().contains(player.getX(), player.getY(), player.getZ());
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
            } else if (kind() == RbmkComponentBlock.Kind.DISPLAY) {
                scanDisplay(level);
            } else {
                scanConsole(level);
                prepareConsoleScreens();
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
            prepareConsoleScreens();
            setChangedAndSync();
            return;
        }
        if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_ASSIGN_SCREEN) {
            int slot = Math.floorMod(value, consoleScreenTypes.length);
            consoleScreenColumns[slot] = validConsoleIndices(selected);
            prepareConsoleScreens();
            setChangedAndSync();
            return;
        }
        for (int index : selected) {
            if (index < 0 || index >= CONSOLE_COLUMN_COUNT || linkedReactor == null) {
                continue;
            }
            BlockPos target = consoleIndexPos(index);
            if (!(level.getBlockEntity(target) instanceof RbmkComponentBlockEntity rbmk)) {
                continue;
            }
            if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_SET_CONTROL && rbmk.kind().isControl()) {
                rbmk.setTargetControlLevel(value / 100.0D);
                rbmk.setChangedAndSync();
            } else if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_CYCLE_COMPRESSOR && rbmk.kind() == RbmkComponentBlock.Kind.BOILER) {
                rbmk.cycleSteamCompression();
                rbmk.setChangedAndSync();
            } else if (action == com.reinhardt.hbm.network.RbmkConsoleControlPayload.ACTION_ASSIGN_COLOR && rbmk.kind().isControl()) {
                int clamped = Math.max(0, Math.min(4, value));
                rbmk.colorGroup = rbmk.colorGroup == clamped ? -1 : clamped;
                rbmk.setChangedAndSync();
            }
        }
        scanConsole(level);
        prepareConsoleScreens();
        setChangedAndSync();
    }

    public void rotateConsoleScan() {
        if (kind() != RbmkComponentBlock.Kind.CONSOLE) {
            return;
        }
        consoleRotation = (consoleRotation + 1) & 3;
        if (level != null && !level.isClientSide) {
            scanConsole(level);
        }
        setChangedAndSync();
    }

    public void rotateDisplayScan() {
        if (kind() != RbmkComponentBlock.Kind.DISPLAY) {
            return;
        }
        displayRotation = (displayRotation + 1) & 3;
        if (level != null && !level.isClientSide) {
            scanDisplay(level);
        }
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
        if (level == null || level.isClientSide || kind() != RbmkComponentBlock.Kind.CRANE_CONSOLE || !isPlayerInCraneOperationArea(player)) {
            craneInputUp = false;
            craneInputDown = false;
            craneInputLeft = false;
            craneInputRight = false;
            craneInputLoad = false;
            return;
        }
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
        RbmkComponentBlock.Kind kind = kind();
        if (!kind.hasFluid() && kind != RbmkComponentBlock.Kind.OUTGASSER) {
            return null;
        }
        return new RbmkFluidHandler();
    }

    private void tickFuel(Level level, BlockPos pos) {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.isEmpty()) {
            heat = Math.max(20.0D, heat - 0.05D);
            lastFlux = 0.0D;
            lastFluxFastRatio = 0.0D;
            return;
        }
        NeutronFluxProvider.NeutronFlux flux = rbmkIncomingSpectrum(level, pos);
        if (!(fuel.getItem() instanceof RbmkFuelRodItem rod)) {
            lastFlux = 0.0D;
            lastFluxFastRatio = 0.0D;
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
        lastFlux = result.outputFlux();
        lastFluxFastRatio = rod.outputFastRatio(fuel);
        double multiplier = kind() == RbmkComponentBlock.Kind.FUEL_ROD_MOD || kind() == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD
                ? 1.25D
                : 1.0D;
        heat += result.providedHeat() * multiplier;
        if (!hasLid() && flux.total() > 0.0D && level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(pos, flux.total() * 0.05D);
        }
        if (heat > rod.meltingPoint(fuel)) {
            if (meltdownsDisabled()) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(HbmParticleTypes.RBMK_FIRE.get(), pos.getX() + 0.5D, pos.getY() + RbmkComponentBlock.columnHeight(level) - 0.5D, pos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            } else {
                meltdown(level, pos);
            }
            lastFlux = 0.0D;
            lastFluxFastRatio = 0.0D;
            setChangedAndSync();
            return;
        }
        setChangedAndSync();
    }

    private NeutronFluxProvider.NeutronFlux rbmkIncomingSpectrum(Level level, BlockPos pos) {
        NeutronFluxProvider.NeutronFlux total = NeutronFluxProvider.NeutronFlux.ZERO;
        int range = Math.max(1, Math.min(100, HbmConfig.RBMK_FLUX_RANGE.get()));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            NeutronFluxProvider.NeutronFlux directional = scanRbmkFluxLine(level, pos, direction, range);
            total = total.add(directional);
        }
        return total;
    }

    private NeutronFluxProvider.NeutronFlux scanRbmkFluxLine(Level level, BlockPos origin, Direction direction, int range) {
        for (int distance = 1; distance <= range; distance++) {
            BlockPos cursor = origin.relative(direction, distance);
            BlockEntity blockEntity = level.getBlockEntity(cursor);
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
                RbmkComponentBlock.Kind kind = rbmk.kind();
                if (kind == RbmkComponentBlock.Kind.REFLECTOR) {
                    return reflectedFluxToSelf(level, origin, direction, distance);
                }
                if (kind.acceptsFuel()) {
                    NeutronFluxProvider.NeutronFlux emitted = rbmk.neutronFluxSpectrum(level, origin);
                    return streamFlux(level, cursor, direction.getOpposite(), distance, emitted, true);
                }
                if (kind == RbmkComponentBlock.Kind.OUTGASSER) {
                    return NeutronFluxProvider.NeutronFlux.ZERO;
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
        if (!kind().acceptsFuel() || items.get(SLOT_FUEL).isEmpty() || lastFlux <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        NeutronFluxProvider.NeutronFlux reflected = NeutronFluxProvider.NeutronFlux.fromRatio(lastFlux, lastFluxFastRatio);
        reflected = streamFlux(level, origin, directionToReflector, distance, reflected, false);
        if (reflected.total() <= 0.0D) {
            return NeutronFluxProvider.NeutronFlux.ZERO;
        }
        if (kind() == RbmkComponentBlock.Kind.FUEL_ROD_MOD || kind() == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD) {
            reflected = moderate(reflected);
        }
        return NeutronFluxProvider.NeutronFlux.fromRatio(reflected.total() * reflectorEfficiency(), reflected.fastRatio());
    }

    private NeutronFluxProvider.NeutronFlux streamFlux(Level level, BlockPos source, Direction streamDirection, int distanceToTarget, NeutronFluxProvider.NeutronFlux flux, boolean includeTarget) {
        NeutronFluxProvider.NeutronFlux current = flux;
        for (int step = 1; step <= distanceToTarget; step++) {
            boolean targetStep = step == distanceToTarget;
            if (targetStep && !includeTarget) {
                break;
            }
            BlockPos cursor = source.relative(streamDirection, step);
            if (level.getBlockEntity(cursor) instanceof RbmkComponentBlockEntity rbmk) {
                RbmkComponentBlock.Kind kind = rbmk.kind();
                if (kind == RbmkComponentBlock.Kind.MODERATOR || kind == RbmkComponentBlock.Kind.FUEL_ROD_MOD || kind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD) {
                    current = moderate(current);
                }
                if (kind.isControl()) {
                    if (rbmk.controlLevel <= 0.0D) {
                        return NeutronFluxProvider.NeutronFlux.ZERO;
                    }
                    current = NeutronFluxProvider.NeutronFlux.fromRatio(current.total() * rbmk.controlMultiplier(), current.fastRatio());
                } else if (kind == RbmkComponentBlock.Kind.ABSORBER) {
                    current = rbmk.absorbFlux(current);
                } else if (!targetStep && (kind.acceptsFuel() || kind == RbmkComponentBlock.Kind.OUTGASSER)) {
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
                current = NeutronFluxProvider.NeutronFlux.fromRatio(current.total() * multiplier, current.fastRatio());
                if (level instanceof ServerLevel serverLevel) {
                    ChunkRadiationData.get(serverLevel).incrementRadiation(cursor, current.total() * 0.05D, 2_000.0D);
                }
            } else if (level instanceof ServerLevel serverLevel && current.total() > 0.0D) {
                ChunkRadiationData.get(serverLevel).incrementRadiation(cursor, current.total() * 0.05D);
            }
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
            if (!state.isAir() && state.isCollisionShapeFullBlock(level, target)) {
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
        if (level.isClientSide || meltingDown) {
            return;
        }
        meltingDown = true;
        Set<RbmkComponentBlockEntity> columns = connectedColumns(level, pos);
        if (columns.isEmpty()) {
            columns.add(this);
        }
        int minX = pos.getX();
        int maxX = pos.getX();
        int minZ = pos.getZ();
        int maxZ = pos.getZ();
        boolean digamma = false;
        for (RbmkComponentBlockEntity rbmk : columns) {
            BlockPos columnPos = rbmk.getBlockPos();
            minX = Math.min(minX, columnPos.getX());
            maxX = Math.max(maxX, columnPos.getX());
            minZ = Math.min(minZ, columnPos.getZ());
            maxZ = Math.max(maxZ, columnPos.getZ());
            ItemStack fuel = rbmk.items.get(SLOT_FUEL);
            if (fuel.getItem() instanceof RbmkFuelRodItem rod && rod.isDigammaFuel(fuel)) {
                digamma = true;
            }
        }

        List<BlockPos> coriumCores = new ArrayList<>();
        for (RbmkComponentBlockEntity rbmk : columns) {
            BlockPos columnPos = rbmk.getBlockPos();
            int minDist = Math.min(
                    columnPos.getX() - minX,
                    Math.min(maxX - columnPos.getX(), Math.min(columnPos.getZ() - minZ, maxZ - columnPos.getZ()))
            );
            if (rbmk.meltColumn(level, columnPos, minDist + 1)) {
                coriumCores.add(columnPos);
            }
        }

        for (BlockPos core : coriumCores) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos target = core.offset(x, y, z);
                        BlockState targetState = level.getBlockState(target);
                        if (level.random.nextInt(3) == 0
                                && (targetState.is(HbmBlocks.PRIBRIS.get()) || targetState.is(HbmBlocks.PRIBRIS_BURNING.get()))) {
                            level.setBlock(target, digamma
                                    ? HbmBlocks.PRIBRIS_DIGAMMA.get().defaultBlockState()
                                    : HbmBlocks.PRIBRIS_RADIATING.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        if (HbmConfig.RBMK_OVERPRESSURE.get()) {
            overpressureFluidPipes(level, columns);
        }

        int smallDim = Math.min(maxX - minX, maxZ - minZ);
        int avgX = minX + (maxX - minX) / 2;
        int avgZ = minZ + (maxZ - minZ) / 2;
        if (level instanceof ServerLevel serverLevel) {
            double scale = Math.max(1.0D, smallDim);
            serverLevel.sendParticles(HbmParticleTypes.RBMK_MUSH.get(), avgX + 0.5D, pos.getY() + 1.0D, avgZ + 0.5D, 0, scale, 0.0D, 0.0D, 1.0D);
            serverLevel.sendParticles(HbmParticleTypes.FALLOUT_RAIN.get(), avgX + 0.5D, pos.getY() + 1.0D, avgZ + 0.5D, 0, rbmkFalloutVisualRange(digamma), 0.0D, 0.0D, 1.0D);
            NuclearFalloutTerrainEffects.scheduleRbmkMeltdown(serverLevel, new BlockPos(avgX, pos.getY(), avgZ), digamma);
        }
        level.playSound(null, avgX + 0.5D, pos.getY() + 1.0D, avgZ + 0.5D,
                HbmSoundEvents.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
        if (digamma) {
            DigammaSpearEntity spear = new DigammaSpearEntity(HbmEntityTypes.DIGAMMA_SPEAR.get(), level);
            spear.setPos(avgX + 0.5D, pos.getY() + 100.0D, avgZ + 0.5D);
            level.addFreshEntity(spear);
        }
        for (RbmkComponentBlockEntity rbmk : columns) {
            rbmk.meltingDown = false;
        }
        meltingDown = false;
    }

    private static double rbmkFalloutVisualRange(boolean digamma) {
        try {
            int range = Math.max(0, Math.min(1024, HbmConfig.RBMK_FALLOUT_RANGE.get()));
            return digamma ? range * 2.0D : range;
        } catch (IllegalStateException ignored) {
            return digamma ? 200.0D : 100.0D;
        }
    }

    private boolean meltColumn(Level level, BlockPos pos, int reduce) {
        int height = RbmkComponentBlock.columnHeight(level);
        RbmkComponentBlock.Kind meltKind = kind();
        boolean hasFuel = meltKind.acceptsFuel() && items.get(SLOT_FUEL).getItem() instanceof RbmkFuelRodItem;
        boolean hadNormalLid = lidType == LidType.NORMAL;
        clearContent();
        lidType = LidType.NONE;
        updateLidCollisionDummy();
        heat = 0.0D;
        lastFlux = 0.0D;
        lastFluxFastRatio = 0.0D;

        if (meltKind.acceptsFuel()) {
            meltFuelRodColumn(level, pos, reduce, height, meltKind, hasFuel, hadNormalLid);
            return hasFuel;
        }

        if (meltKind.isControl()) {
            meltControlColumn(level, pos, reduce, height, meltKind);
            return false;
        }

        switch (meltKind) {
            case BLANK, ABSORBER, REFLECTOR, BOILER, HEATER -> {
                spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.BLANK, 1 + level.random.nextInt(2));
                standardMeltColumn(level, pos, reduce, height);
                spawnLidIfNormal(level, pos, hadNormalLid);
            }
            case OUTGASSER -> {
                spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.BLANK, 4 + level.random.nextInt(2));
                standardMeltColumn(level, pos, reduce, height);
                spawnLidIfNormal(level, pos, hadNormalLid);
            }
            case MODERATOR -> {
                spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + level.random.nextInt(2));
                standardMeltColumn(level, pos, reduce, height);
                spawnLidIfNormal(level, pos, hadNormalLid);
            }
            default -> {
                standardMeltColumn(level, pos, reduce, height);
                spawnLidIfNormal(level, pos, hadNormalLid);
            }
        }
        return false;
    }

    private void meltFuelRodColumn(Level level, BlockPos pos, int reduce, int height, RbmkComponentBlock.Kind meltKind, boolean hasFuel, boolean hadNormalLid) {
        if (hasFuel) {
            for (int y = height - 1; y >= 0; y--) {
                level.setBlock(pos.above(y), HbmBlocks.CORIUM_BLOCK.get().defaultBlockState(), 3);
            }
            spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.FUEL, 1 + level.random.nextInt(Math.max(1, height)));
        } else {
            standardMeltColumn(level, pos, reduce, height);
        }
        if (meltKind == RbmkComponentBlock.Kind.FUEL_ROD_MOD || meltKind == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD) {
            spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + level.random.nextInt(2));
        }
        spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.ELEMENT);
        spawnLidIfNormal(level, pos, hadNormalLid);
    }

    private void meltControlColumn(Level level, BlockPos pos, int reduce, int height, RbmkComponentBlock.Kind meltKind) {
        if (meltKind == RbmkComponentBlock.Kind.CONTROL_MOD) {
            spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + level.random.nextInt(2));
        }
        spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.ROD, 2 + level.random.nextInt(2));
        standardMeltColumn(level, pos, reduce, height);
    }

    private static void standardMeltColumn(Level level, BlockPos pos, int reduce, int height) {
        int clampedReduce = Math.max(1, Math.min(height, reduce));
        if (level.random.nextInt(3) == 0) {
            clampedReduce++;
        }
        for (int y = height - 1; y >= 0; y--) {
            BlockPos target = pos.above(y);
            if (y <= height - clampedReduce) {
                if (clampedReduce > 1 && y == height - clampedReduce) {
                    level.setBlock(target, HbmBlocks.PRIBRIS_BURNING.get().defaultBlockState(), 3);
                } else {
                    level.setBlock(target, HbmBlocks.PRIBRIS.get().defaultBlockState(), 3);
                }
            } else {
                level.removeBlock(target, false);
            }
        }
    }

    private static void spawnLidIfNormal(Level level, BlockPos pos, boolean hadNormalLid) {
        if (hadNormalLid) {
            spawnDebris(level, pos, RbmkDebrisEntity.DebrisType.LID);
        }
    }

    private static void spawnDebris(Level level, BlockPos pos, RbmkDebrisEntity.DebrisType type, int count) {
        for (int i = 0; i < count; i++) {
            spawnDebris(level, pos, type);
        }
    }

    private static void spawnDebris(Level level, BlockPos pos, RbmkDebrisEntity.DebrisType type) {
        if (level.isClientSide) {
            return;
        }
        RbmkDebrisEntity debris = new RbmkDebrisEntity(level, pos.getX() + 0.5D, pos.getY() + 4.0D, pos.getZ() + 0.5D, type);
        double motionX = level.random.nextGaussian() * 0.25D;
        double motionZ = level.random.nextGaussian() * 0.25D;
        double motionY = 0.25D + level.random.nextDouble() * 1.25D;
        if (type == RbmkDebrisEntity.DebrisType.LID) {
            motionX *= 0.5D;
            motionY += 0.5D;
            motionZ *= 0.5D;
        }
        debris.setDeltaMovement(motionX, motionY, motionZ);
        level.addFreshEntity(debris);
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

    private static void overpressureFluidPipes(Level level, Set<RbmkComponentBlockEntity> columns) {
        Set<BlockPos> pipes = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int height = RbmkComponentBlock.columnHeight(level);
        for (RbmkComponentBlockEntity rbmk : columns) {
            if (rbmk.kind() != RbmkComponentBlock.Kind.BOILER) {
                continue;
            }
            BlockPos base = rbmk.getBlockPos();
            for (BlockPos output : rbmkBoilerOutputPositions(level, base, height)) {
                if (level.getBlockState(output).getBlock() instanceof com.reinhardt.hbm.block.FluidDuctBlock) {
                    queue.add(output);
                }
            }
        }
        while (!queue.isEmpty() && pipes.size() < 4096) {
            BlockPos pipe = queue.removeFirst();
            if (!pipes.add(pipe)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pipe.relative(direction);
                if (level.getBlockState(neighbor).getBlock() instanceof com.reinhardt.hbm.block.FluidDuctBlock && !pipes.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        int max = Math.min(pipes.size() / 5, 100);
        int count = 0;
        for (BlockPos pipe : pipes) {
            if (count++ >= max) {
                break;
            }
            level.destroyBlock(pipe, false);
            level.explode(null, pipe.getX() + 0.5D, pipe.getY() + 0.5D, pipe.getZ() + 0.5D, 1.5F, Level.ExplosionInteraction.NONE);
        }
    }

    private static List<BlockPos> rbmkBoilerOutputPositions(Level level, BlockPos base, int height) {
        List<BlockPos> outputs = new ArrayList<>();
        outputs.add(base.above(height + 1));
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
        int acceptedSteam = Math.min(steamProduced, steam.capacity() - steam.amount());
        if (waterUsed <= 0 || acceptedSteam <= 0 || steamType.isNone()) {
            return;
        }
        if (acceptedSteam < steamProduced) {
            waterUsed = steamCompression == 3
                    ? (int) Math.floor(acceptedSteam / 100.0D * factor)
                    : (int) Math.ceil(acceptedSteam * factor / 100.0D);
        }
        boilerConsumption = waterUsed;
        boilerOutput = acceptedSteam;
        water.drain(water.type(), waterUsed, false);
        steam.fill(steamType, acceptedSteam, false);
        heat -= waterUsed * boilerHeatConsumption();
        setChangedAndSync();
    }

    private void exchangeHeat(Level level, BlockPos pos) {
        boilerConsumption = 0;
        boilerOutput = 0;
        HbmThermalConversions.firstHeatExchangerStep(heaterInput.type()).ifPresentOrElse(step -> {
            double tempRange = heat - step.output().temperatureCelsius();
            if (tempRange <= 0.0D) {
                return;
            }
            double efficiency = step.boilerEfficiency();
            int inputOps = heaterInput.amount() / step.amountReq();
            int outputOps = (heaterOutput.capacity() - heaterOutput.amount()) / step.amountProduced();
            int tempOps = (int) Math.floor((tempRange * HEAT_EXCHANGER_TU_PER_DEGREE * efficiency) / step.heatReq());
            int ops = Math.min(inputOps, Math.min(outputOps, tempOps));
            if (ops <= 0) {
                return;
            }
            int inputUsed = step.amountReq() * ops;
            int outputMade = step.amountProduced() * ops;
            heaterInput.drain(step.input(), inputUsed, false);
            heaterOutput.fill(step.output(), outputMade, false);
            heat -= (step.heatReq() * ops / HEAT_EXCHANGER_TU_PER_DEGREE) * efficiency;
            boilerConsumption = inputUsed;
            boilerOutput = outputMade;
            setChangedAndSync();
        }, () -> {
            if (heaterInput.amount() <= 0) {
                return;
            }
            heaterInput.clear();
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
        if (outgasserProgress >= OUTGASSER_DURATION) {
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

    private void clientTick(Level level, BlockPos pos) {
        RbmkComponentBlock.Kind kind = kind();
        if (kind.isControl()) {
            lastControlLevel = controlLevel;
            if (controlLevel < targetControlLevel) {
                controlLevel = Math.min(targetControlLevel, controlLevel + controlSpeed());
            } else if (controlLevel > targetControlLevel) {
                controlLevel = Math.max(targetControlLevel, controlLevel - controlSpeed());
            }
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

    public double autoloaderPiston(float partialTick) {
        return autoloaderLastPiston + (autoloaderPiston - autoloaderLastPiston) * partialTick;
    }

    private boolean canServiceRodBelow(Level level, BlockPos pos) {
        RbmkComponentBlockEntity rod = rodBelow(level, pos);
        if (rod == null || !coldEnoughForAutoloader(rod.items.get(SLOT_FUEL))) {
            return false;
        }
        ItemStack installed = rod.items.get(SLOT_FUEL);
        return installed.isEmpty() || remainingFuelPercent(installed) < autoloaderCycle;
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
        BlockEntity blockEntity = level.getBlockEntity(pos.below());
        return blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().acceptsFuel() ? rbmk : null;
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
        return Math.max(0.0F, Math.min(100.0F, (1.0F - RbmkFuelRodItem.depletion(stack)) * 100.0F));
    }

    private boolean canProcessOutgasser(RbmkOutgasserRecipe recipe) {
        if (recipe.fusionOnly()) {
            return false;
        }
        if (recipe.hasFluidOutput()) {
            if (outgasserGas.amount() > 0 && outgasserGas.type() != recipe.fluidOutput().fluid()) {
                return false;
            }
            if (outgasserGas.amount() + recipe.fluidOutput().amount() > outgasserGas.capacity()) {
                return false;
            }
        }
        if (recipe.hasItemOutput()) {
            ItemStack existing = items.get(SLOT_BUFFER);
            if (!existing.isEmpty() && (!ItemStack.isSameItemSameComponents(existing, recipe.itemOutput())
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
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor instanceof RbmkComponentBlockEntity other && other.kind().isColumn()) {
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
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn()) {
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
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn()) {
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

    private void coolPassively(int neighbors) {
        double clampedNeighbors = Math.max(0.0D, Math.min(4.0D, neighbors));
        double cooling = passiveCoolingInner() + (passiveCoolingEdge() - passiveCoolingInner()) * ((4.0D - clampedNeighbors) / 4.0D);
        heat = Math.max(20.0D, heat - cooling);
    }

    private void tickControl() {
        lastControlLevel = controlLevel;
        if (kind().isAutomaticControl()) {
            updateAutomaticControlTarget();
        }
        if (controlLevel < targetControlLevel) {
            controlLevel = Math.min(targetControlLevel, controlLevel + controlSpeed());
        } else if (controlLevel > targetControlLevel) {
            controlLevel = Math.max(targetControlLevel, controlLevel - controlSpeed());
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
        } else if (Math.abs(autoHeatUpper - autoHeatLower) < 0.0001D) {
            fauxLevel = autoLevelLower;
        } else {
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
        if (Math.abs(clamped - targetControlLevel) > 0.0001D) {
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
        return Math.max(0.0D, controlLevel + surge);
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
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        scanConsole(level);
        prepareConsoleScreens();
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
            if (!(level.getBlockEntity(scanPos) instanceof RbmkComponentBlockEntity rbmk) || !rbmk.kind().isColumn()) {
                continue;
            }
            displayKinds[index] = rbmk.kind().ordinal();
            displayHeat[index] = (int) Math.round(rbmk.heat);
            displayMaxHeat[index] = (int) Math.round(rbmk.maxConsoleHeat());
            displayControl[index] = (int) Math.round(rbmk.controlLevel * 100.0D);
            displayColorGroups[index] = rbmk.kind().isControl() ? rbmk.colorGroup : -1;
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
        if (linkedReactor == null) {
            return;
        }
        for (int index = 0; index < CONSOLE_COLUMN_COUNT; index++) {
            BlockPos scanPos = consoleIndexPos(index);
            if (!(level.getBlockEntity(scanPos) instanceof RbmkComponentBlockEntity rbmk) || !rbmk.kind().isColumn()) {
                continue;
            }
            consoleKinds[index] = rbmk.kind().ordinal();
            consoleHeat[index] = (int) Math.round(rbmk.heat);
            consoleMaxHeat[index] = (int) Math.round(rbmk.maxConsoleHeat());
            consoleFlux[index] = (int) Math.round(rbmk.lastFlux);
            consoleControl[index] = (int) Math.round(rbmk.controlLevel * 100.0D);
            consoleColorGroups[index] = rbmk.kind().isControl() ? rbmk.colorGroup : -1;
            consoleCraneIndicators[index] = rbmk.craneIndicator;
            if (rbmk.kind().acceptsFuel()) {
                consoleTotalFlux += consoleFlux[index];
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
            if (level.getBlockEntity(target) instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isControl()) {
                rbmk.setTargetControlLevel(levelPercent / 100.0D);
                rbmk.setChangedAndSync();
            }
        }
        scanConsole(level);
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
                        value += consoleHeat[index];
                        count++;
                    }
                    case ROD_EXTRACTION -> {
                        if (columnKind.isControl()) {
                            value += consoleControl[index];
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
                            value += RbmkFuelRodItem.xenon(rod);
                            count++;
                        }
                    }
                    case FUEL_TEMP -> {
                        ItemStack rod = consoleFuelStack(index);
                        if (!rod.isEmpty()) {
                            value += RbmkFuelRodItem.hullHeat(rod);
                            count++;
                        }
                    }
                    default -> {
                    }
                }
            }
            consoleScreenDisplays[slot] = count <= 0 ? 0 : (int) Math.round(value / count * 10.0D);
        }
    }

    private ItemStack consoleFuelStack(int index) {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        BlockPos target = consoleIndexPos(index);
        if (level.getBlockEntity(target) instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().acceptsFuel()) {
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
        if (!craneIsLoading() && level.getEntitiesOfClass(Player.class, craneOperationArea()).stream().anyMatch(this::isPlayerInCraneOperationArea)) {
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
        cranePosFront = 0.0D;
        cranePosLeft = 0.0D;
        craneLastPosFront = 0.0D;
        craneLastPosLeft = 0.0D;
        craneProgress = 1.0D;
        craneLastProgress = 1.0D;
        craneGoesDown = false;
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
        Direction dir = craneDirection();
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

    private Direction craneDirection() {
        Direction direction = getBlockState().getValue(RbmkComponentBlock.FACING);
        int turns = Math.floorMod(craneRotationOffset / 90, 4);
        for (int i = 0; i < turns; i++) {
            direction = direction.getClockWise();
        }
        return direction;
    }

    private boolean craneCanTargetInteract(@Nullable RbmkComponentBlockEntity target) {
        if (target == null) {
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
            target.items.set(craneLoadSlot(target), craneLoadedItem.copyWithCount(1));
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
        if (!(stack.getItem() instanceof RbmkFuelRodItem)) {
            return false;
        }
        if (target.kind().acceptsFuel()) {
            return target.items.get(SLOT_FUEL).isEmpty();
        }
        return target.kind() == RbmkComponentBlock.Kind.STORAGE && target.items.get(11).isEmpty();
    }

    private boolean craneCanUnload(RbmkComponentBlockEntity target) {
        if (target.kind().acceptsFuel()) {
            return !target.items.get(SLOT_FUEL).isEmpty();
        }
        return target.kind() == RbmkComponentBlock.Kind.STORAGE && !target.items.get(0).isEmpty();
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
        steamCompression = next;
        steam.clear();
        setChangedAndSync();
    }

    private HbmFluidDefinition steamFluid() {
        String name = switch (steamCompression) {
            case 1 -> "hotsteam";
            case 2 -> "superhotsteam";
            case 3 -> "ultrahotsteam";
            default -> "steam";
        };
        return HbmFluids.byName(name).orElse(HbmFluids.byName("steam").orElse(HbmFluids.none()));
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
        double maxDistance = kind() == RbmkComponentBlock.Kind.CONSOLE ? 400.0D : 64.0D;
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
        if (slot == SLOT_FUEL) {
            return (kind().acceptsFuel() || kind() == RbmkComponentBlock.Kind.STORAGE) && stack.getItem() instanceof RbmkFuelRodItem;
        }
        if (kind() == RbmkComponentBlock.Kind.STORAGE) {
            return stack.getItem() instanceof RbmkFuelRodItem;
        }
        if (kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            return slot == SLOT_FUEL
                    && !stack.isEmpty()
                    && (level == null || level.isClientSide || outgasserRecipe(level, stack) != null);
        }
        return kind() == RbmkComponentBlock.Kind.STORAGE;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (kind().acceptsFuel() || kind() == RbmkComponentBlock.Kind.HEATER) {
            return FUEL_SLOT;
        }
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
        return kind() != RbmkComponentBlock.Kind.OUTGASSER || slot == SLOT_BUFFER;
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
        tag.putDouble("controlLevel", controlLevel);
        tag.putDouble("targetControlLevel", targetControlLevel);
        tag.putDouble("startingControlLevel", startingControlLevel);
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
        tag.putInt("craneIndicator", craneIndicator);
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
        tag.putDouble("craneTiltFront", craneTiltFront);
        tag.putDouble("craneTiltLeft", craneTiltLeft);
        tag.putDouble("cranePosFront", cranePosFront);
        tag.putDouble("cranePosLeft", cranePosLeft);
        tag.putDouble("craneProgress", craneProgress);
        tag.putBoolean("craneGoesDown", craneGoesDown);
        tag.putDouble("craneLoadedHeat", craneLoadedHeat);
        tag.putDouble("craneLoadedEnrichment", craneLoadedEnrichment);
        tag.put("craneLoadedItem", craneLoadedItem.saveOptional(registries));
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
        if (linkedReactor != null) {
            tag.putLong("linkedReactor", linkedReactor.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        heat = tag.getDouble("heat");
        lastFlux = tag.getDouble("lastFlux");
        lastFluxFastRatio = Math.max(0.0D, Math.min(1.0D, tag.getDouble("lastFluxFastRatio")));
        controlLevel = tag.getDouble("controlLevel");
        targetControlLevel = tag.getDouble("targetControlLevel");
        startingControlLevel = tag.contains("startingControlLevel") ? tag.getDouble("startingControlLevel") : controlLevel;
        autoLevelLower = Math.max(0.0D, Math.min(100.0D, tag.getDouble("autoLevelLower")));
        autoLevelUpper = Math.max(0.0D, Math.min(100.0D, tag.getDouble("autoLevelUpper")));
        autoHeatLower = Math.max(0.0D, Math.min(9999.0D, tag.getDouble("autoHeatLower")));
        autoHeatUpper = Math.max(0.0D, Math.min(9999.0D, tag.getDouble("autoHeatUpper")));
        autoControlFunction = AutoControlFunction.byName(tag.getString("autoControlFunction"));
        colorGroup = tag.contains("colorGroup") ? tag.getInt("colorGroup") : -1;
        colorGroup = colorGroup < 0 ? -1 : Math.min(4, colorGroup);
        redstoneLevel = tag.getInt("redstoneLevel");
        steamCompression = tag.getInt("steamCompression");
        boilerConsumption = tag.getInt("boilerConsumption");
        boilerOutput = tag.getInt("boilerOutput");
        reasimWater = Math.max(0, Math.min(REASIM_INTERNAL_CAPACITY, tag.getInt("reasimWater")));
        reasimSteam = Math.max(0, Math.min(REASIM_INTERNAL_CAPACITY, tag.getInt("reasimSteam")));
        craneIndicator = tag.getInt("craneIndicator");
        lidType = parseLidType(tag.getString("lidType"));
        outgasserProgress = tag.getDouble("outgasserProgress");
        autoloaderPiston = tag.getDouble("autoloaderPiston");
        autoloaderDelay = tag.getInt("autoloaderDelay");
        autoloaderCycle = tag.contains("autoloaderCycle") ? tag.getInt("autoloaderCycle") : 50;
        autoloaderCycle = Math.max(5, Math.min(95, autoloaderCycle));
        autoloaderRetracting = !tag.contains("autoloaderRetracting") || tag.getBoolean("autoloaderRetracting");
        water.load(tag.getCompound("water"));
        steam.load(tag.getCompound("steam"));
        heaterInput.load(tag.getCompound("heaterInput"));
        heaterOutput.load(tag.getCompound("heaterOutput"));
        outgasserGas.load(tag.getCompound("outgasserGas"));
        reasimInletWater.load(tag.getCompound("reasimInletWater"));
        reasimOutletSteam.load(tag.getCompound("reasimOutletSteam"));
        linkedReactor = tag.contains("linkedReactor") ? BlockPos.of(tag.getLong("linkedReactor")) : null;
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
            consoleScreenColumns[index] = validConsoleIndices(tag.getIntArray("consoleScreenColumns" + index));
        }
        loadIntArray(tag, "displayKinds", displayKinds, -1);
        loadIntArray(tag, "displayHeat", displayHeat, 0);
        loadIntArray(tag, "displayMaxHeat", displayMaxHeat, 1500);
        loadIntArray(tag, "displayControl", displayControl, 0);
        loadIntArray(tag, "displayColorGroups", displayColorGroups, -1);
        loadIntArray(tag, "displayCraneIndicators", displayCraneIndicators, 0);
        loadIntArray(tag, "displayFuelDepletion", displayFuelDepletion, 0);
        consoleTotalFlux = tag.getInt("consoleTotalFlux");
        consoleRotation = tag.getInt("consoleRotation") & 3;
        displayRotation = tag.getInt("displayRotation") & 3;
        craneCenter = tag.getBoolean("crane") && tag.contains("craneCenter") ? BlockPos.of(tag.getLong("craneCenter")) : null;
        craneRotationOffset = tag.getInt("craneRotationOffset");
        craneSpanF = tag.getInt("craneSpanF");
        craneSpanB = tag.getInt("craneSpanB");
        craneSpanL = tag.getInt("craneSpanL");
        craneSpanR = tag.getInt("craneSpanR");
        craneHeight = tag.getInt("craneHeight");
        craneTiltFront = tag.getDouble("craneTiltFront");
        craneLastTiltFront = craneTiltFront;
        craneTiltLeft = tag.getDouble("craneTiltLeft");
        craneLastTiltLeft = craneTiltLeft;
        cranePosFront = tag.getDouble("cranePosFront");
        craneLastPosFront = cranePosFront;
        cranePosLeft = tag.getDouble("cranePosLeft");
        craneLastPosLeft = cranePosLeft;
        craneProgress = tag.contains("craneProgress") ? tag.getDouble("craneProgress") : 1.0D;
        craneLastProgress = craneProgress;
        craneGoesDown = tag.getBoolean("craneGoesDown");
        craneLoadedHeat = tag.getDouble("craneLoadedHeat");
        craneLoadedEnrichment = tag.getDouble("craneLoadedEnrichment");
        craneLoadedItem = ItemStack.parseOptional(registries, tag.getCompound("craneLoadedItem"));
    }

    private static void loadIntArray(CompoundTag tag, String key, int[] target, int fallback) {
        int[] source = tag.getIntArray(key);
        java.util.Arrays.fill(target, fallback);
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
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

    private final class RbmkFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            RbmkComponentBlock.Kind kind = kind();
            if (kind == RbmkComponentBlock.Kind.LOADER) {
                return loaderSource() == null ? 0 : 1;
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_INLET || kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return 1;
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
            if (kind == RbmkComponentBlock.Kind.OUTGASSER || tank == 1) {
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
            if (kind == RbmkComponentBlock.Kind.HEATER) {
                if (HbmFluids.fromNeoFluid(resource.getFluid()).flatMap(HbmThermalConversions::firstHeatExchangerStep).isEmpty()) {
                    return 0;
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
                return source == null ? FluidStack.EMPTY : loaderDrain(source, resource, action);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.drain(resource, action);
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
                return source == null ? FluidStack.EMPTY : loaderDrain(source, maxDrain, action);
            }
            if (kind == RbmkComponentBlock.Kind.STEAM_OUTLET) {
                return reasimOutletSteam.drain(maxDrain, action);
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
                    || sourceKind == RbmkComponentBlock.Kind.OUTGASSER;
        }

        private FluidStack loaderOutputFluid(RbmkComponentBlockEntity source) {
            return switch (source.kind()) {
                case BOILER -> source.steam.getFluidInTank(0);
                case HEATER -> source.heaterOutput.getFluidInTank(0);
                case OUTGASSER -> source.outgasserGas.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        private int loaderOutputCapacity(RbmkComponentBlockEntity source) {
            return switch (source.kind()) {
                case BOILER -> source.steam.capacity();
                case HEATER -> source.heaterOutput.capacity();
                case OUTGASSER -> source.outgasserGas.capacity();
                default -> 0;
            };
        }

        private FluidStack loaderDrain(RbmkComponentBlockEntity source, FluidStack resource, FluidAction action) {
            return switch (source.kind()) {
                case BOILER -> source.steam.drain(resource, action);
                case HEATER -> source.heaterOutput.drain(resource, action);
                case OUTGASSER -> source.outgasserGas.drain(resource, action);
                default -> FluidStack.EMPTY;
            };
        }

        private FluidStack loaderDrain(RbmkComponentBlockEntity source, int maxDrain, FluidAction action) {
            return switch (source.kind()) {
                case BOILER -> source.steam.drain(maxDrain, action);
                case HEATER -> source.heaterOutput.drain(maxDrain, action);
                case OUTGASSER -> source.outgasserGas.drain(maxDrain, action);
                default -> FluidStack.EMPTY;
            };
        }
    }
}
