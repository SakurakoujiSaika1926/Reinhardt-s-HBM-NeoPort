package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.FusionMachineClientSounds;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fusion.FusionNetwork;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.recipe.FusionBreederFluidRecipe;
import com.reinhardt.hbm.recipe.FusionBreederItemRecipe;
import com.reinhardt.hbm.menu.FusionMachineMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FusionMachineBlockEntity extends BlockEntity implements PowerEndpoint, WorldlyContainer, MachineInventory, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int BLUEPRINT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int BREEDER_FLUID_ID_SLOT = 0;
    public static final int BREEDER_INPUT_SLOT = 1;
    public static final int BREEDER_OUTPUT_SLOT = 2;
    public static final int PLASMA_BOOSTER_SLOT = 2;
    public static final int PLASMA_INPUT_START = 3;
    public static final int PLASMA_INPUT_END = 15;
    public static final int PLASMA_OUTPUT_SLOT = 15;
    public static final int SLOT_COUNT = 16;
    public static final int DATA_COUNT = 37;

    public static final long TORUS_MAX_POWER = 10_000_000L;
    public static final long KLYSTRON_MAX_OUTPUT = 1_000_000L;
    public static final int KLYSTRON_AIR_CONSUMPTION = 2_500;
    public static final int KLYSTRON_AIR_CAPACITY = KLYSTRON_AIR_CONSUMPTION * 60;
    public static final int TORUS_TANK_CAPACITY = 4_000;
    public static final int BOILER_TANK_CAPACITY = 32_000;
    public static final int BREEDER_TANK_CAPACITY = 16_000;
    public static final int PLASMA_FORGE_TANK_CAPACITY = 16_000;
    public static final double BREEDER_CAPACITY = 10_000.0D;
    public static final double MHDT_PLASMA_EFFICIENCY = 1.35D;
    public static final int MHDT_COOLANT_USE = 50;
    public static final long MHDT_MINIMUM_PLASMA = 5_000_000L;
    public static final float KELVIN = 273.0F;
    public static final float TORUS_TARGET_TEMP = KELVIN - 150.0F;
    public static final float TORUS_PASSIVE_HEAT = 2.5F;
    public static final float TORUS_COOL_PER_MB = 0.5F;
    public static final float TORUS_COOL_MAX = 5.0F + TORUS_PASSIVE_HEAT;

    private static final int[] TORUS_SLOTS = {BATTERY_SLOT, BLUEPRINT_SLOT, OUTPUT_SLOT};
    private static final int[] KLYSTRON_SLOTS = {BATTERY_SLOT};
    private static final int[] BREEDER_INPUT_SLOTS = {BREEDER_INPUT_SLOT};
    private static final int[] PLASMA_FORGE_SLOTS = {PLASMA_BOOSTER_SLOT, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, PLASMA_OUTPUT_SLOT};
    private static final int[] OUTPUT_ONLY = {OUTPUT_SLOT};
    private static final int[] NO_SLOTS = {};
    private static final Random PLASMA_FORGE_RANDOM = new Random();

    private final FusionMachineBlock.Kind kind;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank[] tanks;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> kind.ordinal();
                case 1 -> clampInt(power);
                case 2 -> clampInt(maxPower());
                case 3 -> clampInt(lastInput);
                case 4 -> clampInt(lastOutput);
                case 5 -> clampInt(outputTarget);
                case 6 -> clampInt(output);
                case 7 -> clampInt(klystronEnergy);
                case 8 -> clampInt(plasmaEnergy);
                case 9 -> (int) Math.round(neutronEnergySync);
                case 10 -> (int) Math.round(progress * 10_000.0D);
                case 11 -> (int) Math.round(fuelConsumption * 10_000.0D);
                case 12 -> (int) temperature;
                case 13 -> didProcess ? 1 : 0;
                case 14 -> connected ? 1 : 0;
                case 15 -> switch (kind) {
                    case PLASMA_FORGE -> selectedPlasmaForgeRecipe == null ? -1 : selectedPlasmaForgeRecipeIndex();
                    default -> selectedFusionRecipe == null ? -1 : selectedFusionRecipeIndex();
                };
                case 16 -> booster;
                case 17 -> maxBooster;
                case 36 -> torusConnectionMask;
                default -> tankData(index - 18);
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 1 -> power = Math.max(0L, value);
                case 5 -> outputTarget = Math.max(0L, Math.min(KLYSTRON_MAX_OUTPUT, value));
                case 10 -> progress = Math.max(0.0D, value / 10_000.0D);
                case 12 -> temperature = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private long power;
    private long lastInput;
    private long lastOutput;
    private long outputTarget;
    private long output;
    private long klystronEnergy;
    private long plasmaEnergy;
    private long plasmaEnergySync;
    private double neutronEnergy;
    private double neutronEnergySync;
    private double progress;
    private double bonusProgress;
    private double fuelConsumption;
    private boolean didProcess;
    private boolean connected;
    private float temperature = KELVIN + 20.0F;
    private float plasmaRed = 0.7F;
    private float plasmaGreen = 0.7F;
    private float plasmaBlue = 1.0F;
    private int booster;
    private int maxBooster;
    private int torusConnectionMask;
    private ResourceLocation selectedFusionRecipe;
    private ResourceLocation selectedPlasmaForgeRecipe;
    private float rotor;
    private float prevRotor;
    private float rotorSpeed;
    private double plasmaForgeRing;
    private double prevPlasmaForgeRing;
    private double plasmaForgeRingSpeed;
    private double plasmaForgeRingTarget;
    private int plasmaForgeRingDelay;
    private final ForgeArm plasmaForgeStriker = new ForgeArm(ForgeArmType.STRIKER);
    private final ForgeArm plasmaForgeJet = new ForgeArm(ForgeArmType.JET);

    public FusionMachineBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, kindFromState(blockState));
    }

    public FusionMachineBlockEntity(BlockPos pos, BlockState blockState, FusionMachineBlock.Kind kind) {
        super(HbmBlockEntities.FUSION_MACHINE.get(), pos, blockState);
        this.kind = kind;
        this.tanks = createTanks(kind);
        if (kind == FusionMachineBlock.Kind.KLYSTRON) {
            this.outputTarget = 0L;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionMachineBlockEntity machine) {
        if (level.isClientSide) {
            machine.tickClient();
            return;
        }
        if (machine.kind.hasPowerPorts()) {
            PowerNetworkManager.tickFromEndpoint(level, machine);
        }
        machine.tickServer(level);
    }

    public FusionMachineBlock.Kind kind() {
        return this.kind;
    }

    public boolean hasMenu() {
        return this.kind == FusionMachineBlock.Kind.TORUS
                || this.kind == FusionMachineBlock.Kind.KLYSTRON
                || this.kind == FusionMachineBlock.Kind.BREEDER
                || this.kind == FusionMachineBlock.Kind.PLASMA_FORGE;
    }

    public HbmFluidTank inputTank() {
        return firstReceivingTank();
    }

    public HbmFluidTank outputTank() {
        return firstSendingTank();
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index] : emptyTank();
    }

    public long power() {
        return this.power;
    }

    public long maxPower() {
        return switch (this.kind) {
            case TORUS -> TORUS_MAX_POWER;
            case KLYSTRON -> Math.max(1_000_000L, this.outputTarget * 100L);
            case PLASMA_FORGE -> Math.max(100_000L, Math.max(this.power, selectedPlasmaForgeRecipe(this.level).map(holder -> holder.value().power() * 100L).orElse(10_000_000L)));
            case MHDT -> Math.max(1L, this.power);
            default -> 0L;
        };
    }

    public long plasmaEnergy() {
        return this.plasmaEnergySync > 0L ? this.plasmaEnergySync : this.plasmaEnergy;
    }

    public long outputTarget() {
        return this.outputTarget;
    }

    public long output() {
        return this.output;
    }

    public double progress() {
        return this.progress;
    }

    public ResourceLocation selectedFusionRecipe() {
        return this.selectedFusionRecipe;
    }

    @Nullable
    public ResourceLocation selectedPlasmaForgeRecipeId() {
        return this.selectedPlasmaForgeRecipe;
    }

    public void setSelectedFusionRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.level != null && findFusionRecipe(this.level, recipeId).isEmpty()) {
            return;
        }
        this.selectedFusionRecipe = recipeId;
        this.progress = 0.0D;
        this.bonusProgress = 0.0D;
        setChangedAndSync(true);
    }

    public void setSelectedPlasmaForgeRecipe(@Nullable ResourceLocation recipeId) {
        if (recipeId != null && this.level != null && findPlasmaForgeRecipe(this.level, recipeId).isEmpty()) {
            return;
        }
        this.selectedPlasmaForgeRecipe = recipeId;
        this.progress = 0.0D;
        configurePlasmaForgeTank(selectedPlasmaForgeRecipe(this.level).map(RecipeHolder::value).orElse(null));
        setChangedAndSync(true);
    }

    public void setOutputTarget(long outputTarget) {
        this.outputTarget = Math.max(0L, Math.min(KLYSTRON_MAX_OUTPUT, outputTarget));
        setChangedAndSync(true);
    }

    public List<RecipeHolder<FusionRecipe>> availableFusionRecipes(Level level) {
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                .stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public List<RecipeHolder<PlasmaForgeRecipe>> availablePlasmaForgeRecipes(Level level) {
        Optional<String> pool = installedBlueprintPool();
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get())
                .stream()
                .filter(holder -> holder.value().isVisibleForPool(pool))
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
    }

    public int selectedFusionRecipeIndex() {
        if (this.level == null || this.selectedFusionRecipe == null) {
            return -1;
        }
        List<RecipeHolder<FusionRecipe>> recipes = availableFusionRecipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedFusionRecipe)) {
                return index;
            }
        }
        return -1;
    }

    public int selectedPlasmaForgeRecipeIndex() {
        if (this.level == null || this.selectedPlasmaForgeRecipe == null) {
            return -1;
        }
        List<RecipeHolder<PlasmaForgeRecipe>> recipes = availablePlasmaForgeRecipes(this.level);
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.selectedPlasmaForgeRecipe)) {
                return index;
            }
        }
        return -1;
    }

    public Optional<RecipeHolder<PlasmaForgeRecipe>> selectedPlasmaForgeRecipe(@Nullable Level level) {
        if (level == null || this.selectedPlasmaForgeRecipe == null) {
            return Optional.empty();
        }
        Optional<RecipeHolder<PlasmaForgeRecipe>> recipe = findPlasmaForgeRecipe(level, this.selectedPlasmaForgeRecipe)
                .filter(holder -> holder.value().isVisibleForPool(installedBlueprintPool()));
        if (recipe.isEmpty()) {
            this.selectedPlasmaForgeRecipe = null;
            this.progress = 0.0D;
        }
        return recipe;
    }

    public float rotor(float partialTick) {
        return this.prevRotor + (this.rotor - this.prevRotor) * partialTick;
    }

    public float rotorSpeed() {
        return this.rotorSpeed;
    }

    public float plasmaRed() {
        return this.plasmaRed;
    }

    public float plasmaGreen() {
        return this.plasmaGreen;
    }

    public float plasmaBlue() {
        return this.plasmaBlue;
    }

    public boolean connected() {
        return this.connected;
    }

    public boolean torusConnection(int index) {
        return index >= 0 && index < 4 && (this.torusConnectionMask & (1 << index)) != 0;
    }

    public double plasmaForgeRing(float partialTick) {
        return this.prevPlasmaForgeRing + (this.plasmaForgeRing - this.prevPlasmaForgeRing) * partialTick;
    }

    public double[] plasmaForgeStriker(float partialTick) {
        return this.plasmaForgeStriker.positions(partialTick);
    }

    public double[] plasmaForgeJet(float partialTick) {
        return this.plasmaForgeJet.positions(partialTick);
    }

    public boolean plasmaForgeJetActive() {
        return this.didProcess
                && this.plasmaForgeJet.angles[2] == this.plasmaForgeJet.prevAngles[2]
                && this.plasmaForgeJet.angles[2] != 0.0D;
    }

    public void printInfo(Player player) {
        player.displayClientMessage(Component.translatable("message.reinhardtshbm.fusion.kind", this.kind.displayName()), false);
        if (this.kind.hasPowerPorts()) {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.fusion.power", this.power, maxPower(), this.lastInput, this.lastOutput), false);
        }
        for (int index = 0; index < this.tanks.length; index++) {
            HbmFluidTank tank = this.tanks[index];
            if (tank.capacity() > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.reinhardtshbm.fusion.tank",
                        index,
                        Component.translatable(tank.type().translationKey()),
                        tank.amount(),
                        tank.capacity()
                ), false);
            }
        }
        if (this.kind == FusionMachineBlock.Kind.TORUS
                || this.kind == FusionMachineBlock.Kind.MHDT
                || this.kind == FusionMachineBlock.Kind.BOILER
                || this.kind == FusionMachineBlock.Kind.PLASMA_FORGE) {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.fusion.plasma", this.plasmaEnergySync), false);
        }
    }

    private void tickServer(Level level) {
        this.connected = false;
        switch (this.kind) {
            case KLYSTRON -> tickKlystron(level, false);
            case KLYSTRON_CREATIVE -> tickKlystron(level, true);
            case TORUS -> tickTorus(level);
            case BOILER -> tickBoiler();
            case MHDT -> tickMhdt();
            case BREEDER -> tickBreeder(level);
            case COLLECTOR -> tickCollector(level);
            case COUPLER -> tickCoupler(level);
            case PLASMA_FORGE -> tickPlasmaForge(level);
        }
        if (level.getGameTime() % 10L == 0L) {
            setChangedAndSync(false);
        }
    }

    private void tickKlystron(Level level, boolean creative) {
        if (creative) {
            this.output = maxFusionIgnition(level);
            this.connected = FusionNetwork.provideKlystron(level, this, this.output);
            return;
        }

        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, maxPower());
        this.output = 0L;
        double powerFactor = speedScaled(maxPower(), this.power);
        double airFactor = speedScaled(tank(0).capacity(), tank(0).amount());
        double factor = Math.min(powerFactor, airFactor);
        long powerReq = (long) Math.ceil(this.outputTarget * factor);
        int airReq = (int) Math.ceil(KLYSTRON_AIR_CONSUMPTION * factor);
        if (this.outputTarget > 0L && this.power >= powerReq && tank(0).amount() >= airReq) {
            this.output = powerReq;
            this.power -= powerReq;
            tank(0).drain(fluid("air"), airReq, false);
        }
        if (this.output < this.outputTarget / 50L) {
            this.output = 0L;
        }
        this.connected = FusionNetwork.provideKlystron(level, this, this.output);
    }

    private void tickTorus(Level level) {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, maxPower());
        coolTorus();

        RecipeHolder<FusionRecipe> holder = this.selectedFusionRecipe == null
                ? null
                : findFusionRecipe(level, this.selectedFusionRecipe).orElse(null);
        FusionRecipe recipe = holder == null ? null : holder.value();
        configureTorusTanks(recipe);

        int receiverCount = 0;
        int collectors = 0;
        List<FusionMachineBlockEntity> receivers = FusionNetwork.connectedPlasmaReceivers(level, this);
        for (FusionMachineBlockEntity receiver : receivers) {
            if (receiver.receivesSharedFusionPower()) {
                receiverCount++;
            }
            if (receiver.kind == FusionMachineBlock.Kind.COLLECTOR) {
                collectors++;
            }
        }
        updateTorusConnectionMask(level);

        this.didProcess = false;
        this.plasmaEnergy = 0L;
        this.fuelConsumption = 0.0D;
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;

        if (recipe != null && this.temperature <= TORUS_TARGET_TEMP && this.klystronEnergy >= recipe.ignitionTemp()) {
            double factor = torusProcessFactor(recipe);
            if (factor > 0.0D && canProcessTorus(recipe, factor)) {
                consumeTorusTick(recipe, factor);
                this.progress += Math.min(factor / Math.max(1, recipe.duration()), 1.0D);
                this.bonusProgress = Math.min(1.5D, this.bonusProgress + Math.min(factor / Math.max(1, recipe.duration()), 1.0D) * collectors * 0.5D);
                this.plasmaEnergy = (long) Math.ceil(recipe.outputTemp() * factor);
                this.fuelConsumption = factor;
                r = recipe.r();
                g = recipe.g();
                b = recipe.b();
                this.plasmaRed = r;
                this.plasmaGreen = g;
                this.plasmaBlue = b;
                this.didProcess = true;
                if (this.progress >= 1.0D) {
                    produceTorusOutput(recipe);
                    this.progress = canProcessTorus(recipe, factor) ? this.progress - 1.0D : 0.0D;
                }
                if (this.bonusProgress >= 1.0D && canFitTorusOutput(recipe)) {
                    produceTorusOutput(recipe);
                    this.bonusProgress -= 1.0D;
                }
            } else {
                this.progress = 0.0D;
            }
        } else {
            this.progress = 0.0D;
        }

        double outputIntensity = outputIntensity(receiverCount);
        double outputFlux = recipe != null ? recipe.neutronFlux() * this.fuelConsumption : 0.0D;
        if (this.plasmaEnergy > 0L) {
            long receiverPower = (long) Math.ceil(this.plasmaEnergy * outputIntensity);
            for (FusionMachineBlockEntity receiver : receivers) {
                receiver.receiveFusionPower(receiverPower, outputFlux, r, g, b);
                this.connected = true;
            }
        } else {
            this.connected = !receivers.isEmpty();
        }
        this.klystronEnergy = 0L;
        this.plasmaEnergySync = this.plasmaEnergy;
    }

    private void updateTorusConnectionMask(Level level) {
        if (this.kind != FusionMachineBlock.Kind.TORUS) {
            this.torusConnectionMask = 0;
            return;
        }
        int mask = 0;
        List<FusionNetwork.Node> klystronNodes = klystronReceiverNodes();
        List<FusionNetwork.Node> plasmaNodes = plasmaProviderNodes();
        for (int index = 0; index < 4; index++) {
            boolean connectedKlystron = index < klystronNodes.size()
                    && FusionNetwork.hasConnectedKlystronProvider(level, this, klystronNodes.get(index));
            boolean connectedPlasma = index < plasmaNodes.size()
                    && FusionNetwork.hasConnectedPlasmaReceiver(level, this, plasmaNodes.get(index));
            if (connectedKlystron || connectedPlasma) {
                mask |= 1 << index;
            }
        }
        this.torusConnectionMask = mask;
    }

    private void tickBoiler() {
        this.plasmaEnergySync = this.plasmaEnergy;
        this.plasmaEnergy = 0L;
        outputFluids();
    }

    private void tickMhdt() {
        this.plasmaEnergySync = this.plasmaEnergy;
        if (tank(0).amount() >= MHDT_COOLANT_USE && tank(1).amount() + MHDT_COOLANT_USE <= tank(1).capacity()) {
            long generated = (long) Math.floor(this.plasmaEnergy * MHDT_PLASMA_EFFICIENCY);
            if (this.plasmaEnergy < MHDT_MINIMUM_PLASMA) {
                generated /= 2L;
            }
            this.power = generated;
            tank(0).drain(fluid("perfluoromethyl_cold"), MHDT_COOLANT_USE, false);
            tank(1).fill(fluid("perfluoromethyl"), MHDT_COOLANT_USE, false);
        }
        this.plasmaEnergy = 0L;
        outputFluids();
    }

    private void tickBreeder(Level level) {
        configureBreederInputTankFromIdentifier();
        this.neutronEnergySync = this.neutronEnergy;
        if (!canProcessBreederSolid(level) && !canProcessBreederLiquid(level)) {
            this.progress = 0.0D;
        }
        outputFluids();
        this.neutronEnergy = 0.0D;
    }

    private void tickCollector(Level level) {
        this.connected = !FusionNetwork.connectedPlasmaReceivers(level, this).isEmpty();
    }

    private void tickCoupler(Level level) {
        this.plasmaEnergySync = this.plasmaEnergy;
        if (this.plasmaEnergy > 0L) {
            this.connected = FusionNetwork.provideKlystron(level, this, this.plasmaEnergy);
        }
        this.plasmaEnergy = 0L;
        this.neutronEnergy = 0.0D;
    }

    private void tickPlasmaForge(Level level) {
        this.plasmaEnergySync = this.plasmaEnergy;
        this.plasmaEnergy = 0L;
        this.neutronEnergySync = this.neutronEnergy;
        this.didProcess = false;
        this.fuelConsumption = 0.0D;

        loadPlasmaBooster();
        RecipeHolder<PlasmaForgeRecipe> holder = selectedPlasmaForgeRecipe(level).orElse(null);
        PlasmaForgeRecipe recipe = holder == null ? null : holder.value();
        configurePlasmaForgeTank(recipe);
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, maxPower());

        if (recipe != null && maybeAutoSwitchPlasmaForgeRecipe(level, recipe)) {
            recipe = null;
        }

        if (recipe != null && this.plasmaEnergySync >= recipe.ignitionTemp() && canProcessPlasmaForge(recipe)) {
            double speed = this.booster > 0 ? 4.0D : 1.0D;
            this.power -= recipe.power();
            this.progress += Math.min(speed / recipe.duration(), 1.0D);
            this.didProcess = true;
            this.fuelConsumption = 1.0D;
            if (this.progress >= 1.0D) {
                consumePlasmaForgeInputs(recipe);
                mergeOutput(PLASMA_OUTPUT_SLOT, recipe.result().copy());
                this.progress = canProcessPlasmaForge(recipe) && this.plasmaEnergySync >= recipe.ignitionTemp()
                        ? this.progress - 1.0D
                        : 0.0D;
            }
        } else {
            this.progress = 0.0D;
        }

        if (this.didProcess && this.booster > 0) {
            this.booster--;
        }
        this.connected = FusionNetwork.providePlasma(level, this, (long) Math.ceil(this.plasmaEnergySync * 0.75D), this.neutronEnergy, this.plasmaRed, this.plasmaGreen, this.plasmaBlue);
        this.neutronEnergy = 0.0D;
    }

    private void loadPlasmaBooster() {
        if (this.booster > 0) {
            return;
        }
        ItemStack stack = this.items.get(PLASMA_BOOSTER_SLOT);
        int value = boosterValue(stack);
        if (value <= 0) {
            return;
        }
        this.maxBooster = value;
        this.booster = value;
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.items.set(PLASMA_BOOSTER_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean maybeAutoSwitchPlasmaForgeRecipe(Level level, PlasmaForgeRecipe currentRecipe) {
        if (currentRecipe.autoSwitchGroup().isEmpty()) {
            return false;
        }
        ItemStack firstInput = this.items.get(PLASMA_INPUT_START);
        if (firstInput.isEmpty()) {
            return false;
        }
        String group = currentRecipe.autoSwitchGroup().get();
        for (RecipeHolder<PlasmaForgeRecipe> holder : availablePlasmaForgeRecipes(level)) {
            PlasmaForgeRecipe next = holder.value();
            if (holder.id().equals(this.selectedPlasmaForgeRecipe)
                    || next.autoSwitchGroup().filter(group::equals).isEmpty()
                    || next.inputItems().isEmpty()) {
                continue;
            }
            if (next.inputItems().getFirst().ingredient().test(firstInput)) {
                this.selectedPlasmaForgeRecipe = holder.id();
                this.progress = 0.0D;
                configurePlasmaForgeTank(next);
                setChangedAndSync(true);
                return true;
            }
        }
        return false;
    }

    private boolean canProcessPlasmaForge(PlasmaForgeRecipe recipe) {
        if (this.power < recipe.power() || !canMergeOutput(PLASMA_OUTPUT_SLOT, recipe.result())) {
            return false;
        }
        return PlasmaForgeRecipe.canCraft(plasmaForgeInputStacks(), List.of(new HbmFluidStack(tank(0).type(), tank(0).amount(), tank(0).pressure())), recipe.inputItems(), recipe.inputFluids());
    }

    private void consumePlasmaForgeInputs(PlasmaForgeRecipe recipe) {
        for (int index = 0; index < recipe.inputItems().size(); index++) {
            int slot = PLASMA_INPUT_START + index;
            ItemStack stack = this.items.get(slot);
            stack.shrink(recipe.inputItems().get(index).count());
            if (stack.isEmpty()) {
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
        if (!recipe.inputFluids().isEmpty()) {
            PlasmaForgeRecipe.PlasmaFluidStack fluid = recipe.inputFluids().getFirst();
            tank(0).drain(fluid.type(), fluid.amount(), false);
        }
    }

    private List<ItemStack> plasmaForgeInputStacks() {
        ArrayList<ItemStack> stacks = new ArrayList<>(PlasmaForgeRecipe.ITEM_LIMIT);
        for (int slot = PLASMA_INPUT_START; slot < PLASMA_OUTPUT_SLOT; slot++) {
            stacks.add(this.items.get(slot));
        }
        return List.copyOf(stacks);
    }

    private void configurePlasmaForgeTank(@Nullable PlasmaForgeRecipe recipe) {
        HbmFluidTank tank = tank(0);
        if (recipe == null || recipe.inputFluids().isEmpty()) {
            tank.setCapacity(Math.max(PLASMA_FORGE_TANK_CAPACITY, tank.amount()));
            if (tank.amount() == 0) {
                tank.clear();
            }
            return;
        }
        PlasmaForgeRecipe.PlasmaFluidStack fluid = recipe.inputFluids().getFirst();
        tank.setCapacity(Math.max(Math.max(tank.amount(), fluid.amount() * 2), PLASMA_FORGE_TANK_CAPACITY));
        tank.conform(fluid.type(), fluid.pressure());
    }

    private void coolTorus() {
        this.temperature += TORUS_PASSIVE_HEAT;
        if (this.temperature > KELVIN + 20.0F) {
            this.temperature = KELVIN + 20.0F;
        }
        if (this.temperature <= TORUS_TARGET_TEMP) {
            return;
        }
        int cyclesTemp = (int) Math.ceil(Math.min(this.temperature - TORUS_TARGET_TEMP, TORUS_COOL_MAX) / TORUS_COOL_PER_MB);
        int cycles = Math.min(cyclesTemp, Math.min(tank(4).amount(), tank(5).capacity() - tank(5).amount()));
        if (cycles > 0) {
            tank(4).drain(fluid("perfluoromethyl_cold"), cycles, false);
            tank(5).fill(fluid("perfluoromethyl"), cycles, false);
            this.temperature -= TORUS_COOL_PER_MB * cycles;
        }
    }

    private void configureTorusTanks(@Nullable FusionRecipe recipe) {
        if (recipe == null) {
            return;
        }
        for (int index = 0; index < 3; index++) {
            if (index < recipe.inputFluids().size()) {
                tank(index).setType(recipe.inputFluids().get(index).fluid());
            }
        }
        if (!recipe.outputFluids().isEmpty()) {
            tank(3).setType(recipe.outputFluids().get(0).fluid());
        }
    }

    private double torusProcessFactor(FusionRecipe recipe) {
        double factor = speedScaled(maxPower(), this.power);
        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            factor = Math.min(factor, speedScaled(tank(index).capacity(), tank(index).amount()));
        }
        return factor;
    }

    private boolean canProcessTorus(FusionRecipe recipe, double factor) {
        long powerReq = (long) Math.ceil(recipe.power() * factor);
        if (this.power < powerReq) {
            return false;
        }
        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            FusionRecipe.FusionFluidStack required = recipe.inputFluids().get(index);
            int amount = (int) Math.ceil(required.amount() * factor);
            if (tank(index).type() != required.fluid() || (tank(index).amount() > 0 && tank(index).amount() < amount)) {
                return false;
            }
        }
        return canFitTorusOutput(recipe);
    }

    private void consumeTorusTick(FusionRecipe recipe, double factor) {
        long powerReq = (long) Math.ceil(recipe.power() * factor);
        this.power = Math.max(0L, this.power - powerReq);
        for (int index = 0; index < recipe.inputFluids().size(); index++) {
            FusionRecipe.FusionFluidStack required = recipe.inputFluids().get(index);
            tank(index).drain(required.fluid(), (int) Math.ceil(required.amount() * factor), false);
        }
    }

    private boolean canFitTorusOutput(FusionRecipe recipe) {
        if (!recipe.outputItem().orElse(ItemStack.EMPTY).isEmpty() && !canMergeOutput(OUTPUT_SLOT, recipe.outputItem().get())) {
            return false;
        }
        for (FusionRecipe.FusionFluidStack outputFluid : recipe.outputFluids()) {
            if (!tank(3).type().isNone() && tank(3).type() != outputFluid.fluid()) {
                return false;
            }
            if (tank(3).amount() + outputFluid.amount() > tank(3).capacity()) {
                return false;
            }
        }
        return true;
    }

    private void produceTorusOutput(FusionRecipe recipe) {
        recipe.outputItem().ifPresent(stack -> mergeOutput(OUTPUT_SLOT, stack.copy()));
        for (FusionRecipe.FusionFluidStack outputFluid : recipe.outputFluids()) {
            tank(3).fill(outputFluid.fluid(), outputFluid.amount(), false);
        }
    }

    private void configureBreederInputTankFromIdentifier() {
        ItemStack identifier = this.items.get(BREEDER_FLUID_ID_SLOT);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            tank(0).setType(FluidIdentifierItem.primary(identifier));
        }
    }

    private boolean canProcessBreederSolid(Level level) {
        ItemStack input = this.items.get(BREEDER_INPUT_SLOT);
        RecipeHolder<FusionBreederItemRecipe> fusionItemHolder = fusionBreederItemRecipe(level, input);
        if (fusionItemHolder != null) {
            FusionBreederItemRecipe recipe = fusionItemHolder.value();
            return this.neutronEnergy >= recipe.flux()
                    && canMergeOutput(BREEDER_OUTPUT_SLOT, recipe.result());
        }
        RecipeHolder<RbmkOutgasserRecipe> holder = outgasserRecipe(level, input);
        if (holder == null) {
            return false;
        }
        RbmkOutgasserRecipe recipe = holder.value();
        if (!recipe.itemOutput().isEmpty() && !canMergeOutput(BREEDER_OUTPUT_SLOT, recipe.itemOutput())) {
            return false;
        }
        if (!recipe.fluidOutput().isEmpty()) {
            HbmFluidDefinition fluid = recipe.fluidOutput().fluid();
            if (!tank(1).type().isNone() && tank(1).type() != fluid) {
                return false;
            }
            if (tank(1).amount() + recipe.fluidOutput().amount() > tank(1).capacity()) {
                return false;
            }
        }
        return true;
    }

    private boolean canProcessBreederLiquid(Level level) {
        RecipeHolder<FusionBreederFluidRecipe> holder = breederFluidRecipe(level);
        if (holder == null) {
            return false;
        }
        FusionBreederFluidRecipe recipe = holder.value();
        if (tank(0).type() != recipe.input().fluid() || tank(0).amount() < recipe.input().amount()) {
            return false;
        }
        if (!tank(1).type().isNone() && tank(1).type() != recipe.output().fluid()) {
            return false;
        }
        return tank(1).amount() + recipe.output().amount() <= tank(1).capacity();
    }

    private void processBreeder(Level level) {
        if (this.neutronEnergy <= 0.0D) {
            return;
        }
        if (canProcessBreederSolid(level)) {
            this.progress += this.neutronEnergy;
            if (this.progress > BREEDER_CAPACITY) {
                processBreederSolid(level);
                this.progress = 0.0D;
            }
            return;
        }
        if (canProcessBreederLiquid(level)) {
            this.progress += this.neutronEnergy;
            if (this.progress > BREEDER_CAPACITY) {
                processBreederLiquid(level);
                this.progress = 0.0D;
            }
        } else {
            this.progress = 0.0D;
        }
    }

    private void processBreederSolid(Level level) {
        ItemStack input = this.items.get(BREEDER_INPUT_SLOT);
        RecipeHolder<FusionBreederItemRecipe> fusionItemHolder = fusionBreederItemRecipe(level, input);
        if (fusionItemHolder != null) {
            FusionBreederItemRecipe recipe = fusionItemHolder.value();
            input.shrink(1);
            if (input.isEmpty()) {
                this.items.set(BREEDER_INPUT_SLOT, ItemStack.EMPTY);
            }
            mergeOutput(BREEDER_OUTPUT_SLOT, recipe.result().copy());
            return;
        }
        RecipeHolder<RbmkOutgasserRecipe> holder = outgasserRecipe(level, input);
        if (holder == null) {
            return;
        }
        RbmkOutgasserRecipe recipe = holder.value();
        this.items.get(BREEDER_INPUT_SLOT).shrink(1);
        if (this.items.get(BREEDER_INPUT_SLOT).isEmpty()) {
            this.items.set(BREEDER_INPUT_SLOT, ItemStack.EMPTY);
        }
        if (!recipe.itemOutput().isEmpty()) {
            mergeOutput(BREEDER_OUTPUT_SLOT, recipe.itemOutput().copy());
        }
        if (!recipe.fluidOutput().isEmpty()) {
            tank(1).fill(recipe.fluidOutput().fluid(), recipe.fluidOutput().amount(), false);
        }
    }

    private void processBreederLiquid(Level level) {
        RecipeHolder<FusionBreederFluidRecipe> holder = breederFluidRecipe(level);
        if (holder == null) {
            return;
        }
        FusionBreederFluidRecipe recipe = holder.value();
        tank(0).drain(recipe.input().fluid(), recipe.input().amount(), false);
        tank(1).fill(recipe.output().fluid(), recipe.output().amount(), false);
    }

    @Nullable
    private RecipeHolder<RbmkOutgasserRecipe> outgasserRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        for (RecipeHolder<RbmkOutgasserRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.RBMK_OUTGASSER.get())) {
            if (holder.value().ingredient().test(input)) {
                return holder;
            }
        }
        return null;
    }

    @Nullable
    private RecipeHolder<FusionBreederItemRecipe> fusionBreederItemRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        for (RecipeHolder<FusionBreederItemRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION_BREEDER_ITEM.get())) {
            if (holder.value().ingredient().test(input)) {
                return holder;
            }
        }
        return null;
    }

    @Nullable
    private RecipeHolder<FusionBreederFluidRecipe> breederFluidRecipe(Level level) {
        HbmFluidDefinition fluid = tank(0).type();
        if (fluid.isNone()) {
            return null;
        }
        for (RecipeHolder<FusionBreederFluidRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION_BREEDER_FLUID.get())) {
            if (holder.value().input().fluid() == fluid) {
                return holder;
            }
        }
        return null;
    }

    public boolean receivesSharedFusionPower() {
        return this.kind == FusionMachineBlock.Kind.BOILER
                || this.kind == FusionMachineBlock.Kind.MHDT
                || this.kind == FusionMachineBlock.Kind.COUPLER
                || this.kind == FusionMachineBlock.Kind.PLASMA_FORGE;
    }

    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        switch (this.kind) {
            case BOILER -> receiveBoilerFusionPower(fusionPower);
            case MHDT, COUPLER, PLASMA_FORGE -> {
                this.plasmaEnergy = fusionPower;
                this.neutronEnergy = neutronPower;
                this.plasmaRed = r;
                this.plasmaGreen = g;
                this.plasmaBlue = b;
            }
            case BREEDER -> {
                this.neutronEnergy = neutronPower;
                if (this.level != null) {
                    processBreeder(this.level);
                }
            }
            default -> {
            }
        }
    }

    private void receiveBoilerFusionPower(long fusionPower) {
        this.plasmaEnergy = fusionPower;
        int waterCycles = Math.min(tank(0).amount(), tank(1).capacity() - tank(1).amount());
        int steamCycles = (int) Math.min(fusionPower / 200L, waterCycles);
        if (steamCycles > 0) {
            tank(0).drain(fluid("water"), steamCycles, false);
            tank(1).fill(fluid("superhotsteam"), steamCycles, false);
            if (this.level != null && this.level.random.nextInt(200) == 0) {
                this.level.playSound(
                        null,
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 2.0D,
                        this.worldPosition.getZ() + 0.5D,
                        HbmSoundEvents.BOILER_GROAN.get(),
                        SoundSource.BLOCKS,
                        2.5F,
                        1.0F
                );
            }
        }
    }

    public void receiveKlystronEnergy(long energy) {
        this.klystronEnergy += Math.max(0L, energy);
        setChangedAndSync(false);
    }

    public List<FusionNetwork.Node> klystronProviderNodes() {
        Direction facing = facing();
        return switch (this.kind) {
            case KLYSTRON, KLYSTRON_CREATIVE -> List.of(node(facing.getOpposite(), 4, 2));
            case COUPLER -> {
                Direction dir = facing.getOpposite();
                Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
                yield List.of(node(rot, 1, 2));
            }
            default -> List.of();
        };
    }

    public List<FusionNetwork.Node> klystronReceiverNodes() {
        if (this.kind != FusionMachineBlock.Kind.TORUS) {
            return List.of();
        }
        return cardinalNodes(7, 2);
    }

    public List<FusionNetwork.Node> plasmaProviderNodes() {
        return switch (this.kind) {
            case TORUS -> cardinalNodes(7, 2);
            case PLASMA_FORGE -> {
                Direction rot = LegacyMachineGeometry.forgeRotateUp(facing());
                yield List.of(node(rot.getOpposite(), 5, 2));
            }
            default -> List.of();
        };
    }

    public List<FusionNetwork.Node> plasmaReceiverNodes() {
        Direction facing = facing();
        return switch (this.kind) {
            case BOILER -> List.of(node(facing.getOpposite(), 4, 2));
            case MHDT -> List.of(node(facing.getOpposite(), 6, 2));
            case BREEDER, COLLECTOR -> List.of(node(facing.getOpposite(), 2, 2));
            case COUPLER -> {
                Direction dir = facing.getOpposite();
                Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
                yield List.of(node(rot.getOpposite(), 1, 2));
            }
            case PLASMA_FORGE -> {
                Direction rot = LegacyMachineGeometry.forgeRotateUp(facing);
                yield List.of(node(rot, 5, 2));
            }
            default -> List.of();
        };
    }

    public boolean acceptsKlystronNode(FusionNetwork.Node provider) {
        return klystronReceiverNodes().stream().anyMatch(receiver -> receiver.connectsTo(provider));
    }

    public boolean acceptsPlasmaNode(FusionNetwork.Node provider) {
        return plasmaReceiverNodes().stream().anyMatch(receiver -> receiver.connectsTo(provider));
    }

    private List<FusionNetwork.Node> cardinalNodes(int distance, int y) {
        ArrayList<FusionNetwork.Node> nodes = new ArrayList<>(4);
        nodes.add(node(Direction.NORTH, distance, y));
        nodes.add(node(Direction.SOUTH, distance, y));
        nodes.add(node(Direction.WEST, distance, y));
        nodes.add(node(Direction.EAST, distance, y));
        return List.copyOf(nodes);
    }

    private FusionNetwork.Node node(Direction direction, int distance, int y) {
        BlockPos pos = this.worldPosition.offset(direction.getStepX() * distance, y, direction.getStepZ() * distance);
        return new FusionNetwork.Node(pos, direction);
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!this.kind.hasFluidPorts() || !allowsPort(queriedPos, side)) {
            return null;
        }
        return new FusionFluidHandler(queriedPos.immutable(), side);
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
        if (!this.kind.hasPowerPorts()) {
            return List.of();
        }
        ArrayList<BlockPos> connectors = new ArrayList<>();
        for (Port port : portsFor(this.worldPosition, facing(), this.kind)) {
            if (port.power()) {
                connectors.add(port.connectorPos().immutable());
            }
        }
        return List.copyOf(connectors);
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        if (!this.kind.hasPowerPorts()) {
            return false;
        }
        for (Port port : portsFor(this.worldPosition, facing(), this.kind)) {
            if (port.power() && port.connectorPos().equals(connectorPos) && port.face() == machineSide) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return this.kind == FusionMachineBlock.Kind.MHDT ? this.power : 0L;
    }

    @Override
    public long getRequestedInput() {
        return switch (this.kind) {
            case KLYSTRON, TORUS, PLASMA_FORGE -> Math.max(0L, maxPower() - this.power);
            default -> 0L;
        };
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastOutput = usedOutput;
        this.lastInput = receivedInput;
        if (receivedInput > 0L) {
            this.power = Math.min(maxPower(), this.power + receivedInput);
        }
        if (usedOutput > 0L) {
            this.power = Math.max(0L, this.power - usedOutput);
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.fusion.power_status", this.power, maxPower());
    }

    @Override
    public Component getDisplayName() {
        return this.kind.displayName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!hasMenu()) {
            return null;
        }
        return new FusionMachineMenu(containerId, playerInventory, this, this.menuData);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
            setChangedAndSync(true);
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
        setChangedAndSync(true);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (this.kind) {
            case TORUS -> slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack) || slot == BLUEPRINT_SLOT && BlueprintItem.isBlueprint(stack);
            case KLYSTRON -> slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack);
            case BREEDER -> slot == BREEDER_FLUID_ID_SLOT && stack.getItem() instanceof FluidIdentifierItem
                    || slot == BREEDER_INPUT_SLOT && canAcceptBreederInput(stack);
            case PLASMA_FORGE -> slot == BATTERY_SLOT && BatteryPackItem.isBattery(stack)
                    || slot == BLUEPRINT_SLOT && BlueprintItem.isBlueprint(stack)
                    || slot == PLASMA_BOOSTER_SLOT && boosterValue(stack) > 0
                    || canAcceptPlasmaForgeInput(slot, stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (this.kind) {
            case TORUS -> side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{BATTERY_SLOT};
            case KLYSTRON -> KLYSTRON_SLOTS;
            case BREEDER -> side == Direction.DOWN ? new int[]{BREEDER_OUTPUT_SLOT} : BREEDER_INPUT_SLOTS;
            case PLASMA_FORGE -> PLASMA_FORGE_SLOTS;
            default -> NO_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return switch (this.kind) {
            case PLASMA_FORGE -> slot == PLASMA_OUTPUT_SLOT || isPlasmaForgeSlotClogged(slot);
            case BREEDER -> slot == BREEDER_OUTPUT_SLOT;
            default -> slot == OUTPUT_SLOT;
        };
    }

    private boolean canAcceptBreederInput(ItemStack stack) {
        return !stack.isEmpty()
                && this.level != null
                && (fusionBreederItemRecipe(this.level, stack) != null
                || outgasserRecipe(this.level, stack) != null);
    }

    private boolean canAcceptPlasmaForgeInput(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < PLASMA_INPUT_START || slot >= PLASMA_OUTPUT_SLOT || this.level == null) {
            return false;
        }
        Optional<RecipeHolder<PlasmaForgeRecipe>> selected = selectedPlasmaForgeRecipe(this.level);
        if (selected.isPresent() && matchesPlasmaForgeInputSlot(selected.get().value(), slot, stack)) {
            return true;
        }
        if (slot != PLASMA_INPUT_START) {
            return false;
        }
        for (RecipeHolder<PlasmaForgeRecipe> holder : availablePlasmaForgeRecipes(this.level)) {
            PlasmaForgeRecipe recipe = holder.value();
            if (recipe.inputItems().isEmpty()) {
                continue;
            }
            if (recipe.inputItems().getFirst().ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPlasmaForgeInputSlot(PlasmaForgeRecipe recipe, int slot, ItemStack stack) {
        int inputIndex = slot - PLASMA_INPUT_START;
        return inputIndex >= 0
                && inputIndex < recipe.inputItems().size()
                && recipe.inputItems().get(inputIndex).ingredient().test(stack);
    }

    private boolean isPlasmaForgeSlotClogged(int slot) {
        if (slot < PLASMA_INPUT_START || slot >= PLASMA_OUTPUT_SLOT || this.level == null) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        return !stack.isEmpty() && !canAcceptPlasmaForgeInput(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.replaceAll(stack -> ItemStack.EMPTY);
        setChangedAndSync(true);
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
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putString("Kind", this.kind.name());
        for (int index = 0; index < this.tanks.length; index++) {
            tag.put("Tank" + index, this.tanks[index].save());
        }
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putLong("LastOutput", this.lastOutput);
        tag.putLong("OutputTarget", this.outputTarget);
        tag.putLong("Output", this.output);
        tag.putLong("KlystronEnergy", this.klystronEnergy);
        tag.putLong("PlasmaEnergy", this.plasmaEnergy);
        tag.putLong("PlasmaEnergySync", this.plasmaEnergySync);
        tag.putDouble("NeutronEnergy", this.neutronEnergy);
        tag.putDouble("NeutronEnergySync", this.neutronEnergySync);
        tag.putDouble("Progress", this.progress);
        tag.putDouble("BonusProgress", this.bonusProgress);
        tag.putDouble("FuelConsumption", this.fuelConsumption);
        tag.putBoolean("DidProcess", this.didProcess);
        tag.putBoolean("Connected", this.connected);
        tag.putFloat("Temperature", this.temperature);
        tag.putFloat("PlasmaRed", this.plasmaRed);
        tag.putFloat("PlasmaGreen", this.plasmaGreen);
        tag.putFloat("PlasmaBlue", this.plasmaBlue);
        tag.putInt("Booster", this.booster);
        tag.putInt("MaxBooster", this.maxBooster);
        tag.putInt("TorusConnectionMask", this.torusConnectionMask);
        if (this.selectedFusionRecipe != null) {
            tag.putString("SelectedFusionRecipe", this.selectedFusionRecipe.toString());
        }
        if (this.selectedPlasmaForgeRecipe != null) {
            tag.putString("SelectedPlasmaForgeRecipe", this.selectedPlasmaForgeRecipe.toString());
        }
        tag.putFloat("Rotor", this.rotor);
        tag.putFloat("PrevRotor", this.prevRotor);
        tag.putFloat("RotorSpeed", this.rotorSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        for (int index = 0; index < this.tanks.length; index++) {
            this.tanks[index].load(tag.getCompound("Tank" + index));
        }
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.lastOutput = tag.getLong("LastOutput");
        this.outputTarget = tag.getLong("OutputTarget");
        this.output = tag.getLong("Output");
        this.klystronEnergy = tag.getLong("KlystronEnergy");
        this.plasmaEnergy = tag.getLong("PlasmaEnergy");
        this.plasmaEnergySync = tag.getLong("PlasmaEnergySync");
        this.neutronEnergy = tag.getDouble("NeutronEnergy");
        this.neutronEnergySync = tag.getDouble("NeutronEnergySync");
        this.progress = tag.getDouble("Progress");
        this.bonusProgress = tag.getDouble("BonusProgress");
        this.fuelConsumption = tag.getDouble("FuelConsumption");
        this.didProcess = tag.getBoolean("DidProcess");
        this.connected = tag.getBoolean("Connected");
        this.temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : KELVIN + 20.0F;
        this.plasmaRed = tag.getFloat("PlasmaRed");
        this.plasmaGreen = tag.getFloat("PlasmaGreen");
        this.plasmaBlue = tag.getFloat("PlasmaBlue");
        this.booster = tag.getInt("Booster");
        this.maxBooster = tag.getInt("MaxBooster");
        this.torusConnectionMask = tag.getInt("TorusConnectionMask");
        this.selectedFusionRecipe = tag.contains("SelectedFusionRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedFusionRecipe")) : null;
        this.selectedPlasmaForgeRecipe = tag.contains("SelectedPlasmaForgeRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedPlasmaForgeRecipe")) : null;
        this.rotor = tag.getFloat("Rotor");
        this.prevRotor = tag.getFloat("PrevRotor");
        this.rotorSpeed = tag.getFloat("RotorSpeed");
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

    private void tickClient() {
        this.prevRotor = this.rotor;
        updateLegacyFusionRotor();
        this.rotor += this.rotorSpeed;
        if (this.rotor >= 360.0F) {
            this.rotor -= 360.0F;
            this.prevRotor -= 360.0F;
        }
        FusionMachineClientSounds.tick(this);
    }

    private void updateLegacyFusionRotor() {
        switch (this.kind) {
            case TORUS -> {
                float max = 30.0F * (float) speedScaled(maxPower(), this.power);
                this.rotorSpeed += this.didProcess ? 0.25F : -0.25F;
                this.rotorSpeed = clampFloat(this.rotorSpeed, 0.0F, max);
            }
            case KLYSTRON, KLYSTRON_CREATIVE -> {
                double multiplier = this.kind == FusionMachineBlock.Kind.KLYSTRON_CREATIVE
                        ? (this.output > 0L ? 1.0D : 0.0D)
                        : speedScaled(this.outputTarget, this.output);
                this.rotorSpeed += this.output > 0L ? (float) (0.125D * multiplier) : -0.125F;
                this.rotorSpeed = clampFloat(this.rotorSpeed, 0.0F, (float) (5.0D * multiplier));
            }
            case MHDT -> {
                boolean cool = tank(0).amount() >= MHDT_COOLANT_USE && tank(1).amount() + MHDT_COOLANT_USE <= tank(1).capacity();
                this.rotorSpeed += plasmaEnergy() > 0L && cool ? 0.125F : -0.125F;
                this.rotorSpeed = clampFloat(this.rotorSpeed, 0.0F, plasmaEnergy() >= MHDT_MINIMUM_PLASMA ? 15.0F : 10.0F);
            }
            case PLASMA_FORGE -> {
                this.rotorSpeed = plasmaEnergy() > 0L ? Math.min(5.0F, this.rotorSpeed + 0.125F) : Math.max(0.0F, this.rotorSpeed - 0.125F);
                updatePlasmaForgeClient();
            }
            default -> this.rotorSpeed = Math.max(0.0F, this.rotorSpeed - 0.125F);
        }
    }

    private void updatePlasmaForgeClient() {
        this.plasmaForgeStriker.update(this.didProcess, this::playPlasmaForgeStrikerSound);
        this.plasmaForgeJet.update(this.didProcess, () -> {
        });

        this.prevPlasmaForgeRing = this.plasmaForgeRing;
        if (!this.didProcess) {
            return;
        }
        if (this.plasmaForgeRing != this.plasmaForgeRingTarget) {
            double ringDelta = Math.abs(this.plasmaForgeRingTarget - this.plasmaForgeRing);
            if (ringDelta <= this.plasmaForgeRingSpeed) {
                this.plasmaForgeRing = this.plasmaForgeRingTarget;
            }
            if (this.plasmaForgeRingTarget > this.plasmaForgeRing) {
                this.plasmaForgeRing += this.plasmaForgeRingSpeed;
            }
            if (this.plasmaForgeRingTarget < this.plasmaForgeRing) {
                this.plasmaForgeRing -= this.plasmaForgeRingSpeed;
            }
            if (this.plasmaForgeRingTarget == this.plasmaForgeRing) {
                double sub = this.plasmaForgeRingTarget >= 360.0D ? -360.0D : 360.0D;
                this.plasmaForgeRingTarget += sub;
                this.plasmaForgeRing += sub;
                this.prevPlasmaForgeRing += sub;
                this.plasmaForgeRingDelay = 100 + PLASMA_FORGE_RANDOM.nextInt(41);
            }
            return;
        }
        if (this.plasmaForgeRingDelay > 0) {
            this.plasmaForgeRingDelay--;
        }
        if (this.plasmaForgeRingDelay <= 0) {
            this.plasmaForgeRingTarget += (PLASMA_FORGE_RANDOM.nextDouble() + 1.0D) * 60.0D * (PLASMA_FORGE_RANDOM.nextBoolean() ? -1.0D : 1.0D);
            this.plasmaForgeRingSpeed = 2.5D;
        }
    }

    private void playPlasmaForgeStrikerSound() {
        if (this.level != null) {
            this.level.playLocalSound(
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 0.5D,
                    this.worldPosition.getZ() + 0.5D,
                    HbmSoundEvents.BOLT_GUN.get(),
                    SoundSource.BLOCKS,
                    0.25F,
                    1.25F,
                    false
            );
        }
    }

    private void outputFluids() {
        // Fluid pipes pull through capabilities; this keeps the old per-tick sender role visible to overlays.
    }

    private boolean allowsPort(BlockPos queriedPos, @Nullable Direction side) {
        for (Port port : portsFor(this.worldPosition, facing(), this.kind)) {
            if (port.fluid() && port.proxyPos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
    }

    private void setChangedAndSync(boolean immediate) {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            if (immediate || this.level.getGameTime() % 10L == 0L) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static Optional<RecipeHolder<FusionRecipe>> findFusionRecipe(Level level, ResourceLocation recipeId) {
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FUSION.get())
                .stream()
                .filter(holder -> holder.id().equals(recipeId))
                .findFirst();
    }

    private static Optional<RecipeHolder<PlasmaForgeRecipe>> findPlasmaForgeRecipe(Level level, ResourceLocation recipeId) {
        return level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get())
                .stream()
                .filter(holder -> holder.id().equals(recipeId))
                .findFirst();
    }

    private Optional<String> installedBlueprintPool() {
        return BlueprintItem.pool(this.items.get(BLUEPRINT_SLOT));
    }

    public static int boosterValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (id) {
            case "nugget_co60" -> 20;
            case "billet_co60" -> 120;
            case "ingot_co60", "powder_co60" -> 200;
            case "nugget_sr90", "powder_sr90_tiny" -> 40;
            case "billet_sr90" -> 240;
            case "ingot_sr90", "powder_sr90" -> 400;
            case "nugget_au198" -> 60;
            case "billet_au198" -> 360;
            case "ingot_au198", "powder_au198" -> 600;
            case "powder_i131_tiny" -> 60;
            case "powder_i131" -> 600;
            case "powder_xe135_tiny" -> 60;
            case "powder_xe135" -> 600;
            case "powder_cs137_tiny" -> 50;
            case "powder_cs137" -> 500;
            case "powder_at209" -> 1_200;
            default -> 0;
        };
    }

    private long maxFusionIgnition(Level level) {
        long max = 0L;
        for (RecipeHolder<FusionRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION.get())) {
            max = Math.max(max, holder.value().ignitionTemp());
        }
        return max;
    }

    private boolean canMergeOutput(int slot, ItemStack output) {
        ItemStack held = this.items.get(slot);
        return held.isEmpty()
                || (ItemStack.isSameItemSameComponents(held, output) && held.getCount() + output.getCount() <= held.getMaxStackSize());
    }

    private void mergeOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack held = this.items.get(slot);
        if (held.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(held, output)) {
            held.grow(output.getCount());
        }
    }

    private HbmFluidTank firstReceivingTank() {
        for (int index : receivingTankIndexes()) {
            return tank(index);
        }
        return emptyTank();
    }

    private HbmFluidTank firstSendingTank() {
        for (int index : sendingTankIndexes()) {
            return tank(index);
        }
        return emptyTank();
    }

    private int[] receivingTankIndexes() {
        return switch (this.kind) {
            case TORUS -> new int[]{0, 1, 2, 4};
            case KLYSTRON -> new int[]{0};
            case BOILER -> new int[]{0};
            case MHDT -> new int[]{0};
            case BREEDER -> new int[]{0};
            case PLASMA_FORGE -> new int[]{0};
            default -> new int[0];
        };
    }

    private int[] sendingTankIndexes() {
        return switch (this.kind) {
            case TORUS -> new int[]{3, 5};
            case BOILER, MHDT, BREEDER -> new int[]{1};
            default -> new int[0];
        };
    }

    private int tankData(int localIndex) {
        int tank = localIndex / 3;
        int field = localIndex % 3;
        if (tank < 0 || tank >= Math.min(6, this.tanks.length)) {
            return 0;
        }
        HbmFluidTank hbmTank = this.tanks[tank];
        return switch (field) {
            case 0 -> hbmTank.type().oldId();
            case 1 -> hbmTank.amount();
            case 2 -> hbmTank.capacity();
            default -> 0;
        };
    }

    private static HbmFluidTank[] createTanks(FusionMachineBlock.Kind kind) {
        return switch (kind) {
            case TORUS -> new HbmFluidTank[]{
                    new HbmFluidTank(TORUS_TANK_CAPACITY),
                    new HbmFluidTank(TORUS_TANK_CAPACITY),
                    new HbmFluidTank(TORUS_TANK_CAPACITY),
                    new HbmFluidTank(TORUS_TANK_CAPACITY),
                    new HbmFluidTank(fluid("perfluoromethyl_cold"), TORUS_TANK_CAPACITY),
                    new HbmFluidTank(fluid("perfluoromethyl"), TORUS_TANK_CAPACITY)
            };
            case KLYSTRON -> new HbmFluidTank[]{new HbmFluidTank(fluid("air"), KLYSTRON_AIR_CAPACITY)};
            case BOILER -> new HbmFluidTank[]{new HbmFluidTank(fluid("water"), BOILER_TANK_CAPACITY), new HbmFluidTank(fluid("superhotsteam"), BOILER_TANK_CAPACITY)};
            case MHDT -> new HbmFluidTank[]{new HbmFluidTank(fluid("perfluoromethyl_cold"), TORUS_TANK_CAPACITY), new HbmFluidTank(fluid("perfluoromethyl"), TORUS_TANK_CAPACITY)};
            case BREEDER -> new HbmFluidTank[]{new HbmFluidTank(BREEDER_TANK_CAPACITY), new HbmFluidTank(BREEDER_TANK_CAPACITY)};
            case PLASMA_FORGE -> new HbmFluidTank[]{new HbmFluidTank(PLASMA_FORGE_TANK_CAPACITY)};
            default -> new HbmFluidTank[0];
        };
    }

    private static HbmFluidTank emptyTank() {
        return new HbmFluidTank(0);
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private static double speedScaled(double max, double level) {
        if (max == 0.0D) {
            return 0.0D;
        }
        if (level >= max * 0.5D) {
            return 1.0D;
        }
        return level / max * 2.0D;
    }

    private static double outputIntensity(int receiverCount) {
        if (receiverCount == 1) {
            return 1.0D;
        }
        if (receiverCount == 2) {
            return 0.625D;
        }
        if (receiverCount == 3) {
            return 0.5D;
        }
        return 0.4375D;
    }

    private static int clampInt(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static FusionMachineBlock.Kind kindFromState(BlockState state) {
        if (state.getBlock() instanceof FusionMachineBlock block) {
            return block.kind();
        }
        return FusionMachineBlock.Kind.TORUS;
    }

    public static List<Port> portsFor(BlockPos corePos, Direction facing, FusionMachineBlock.Kind kind) {
        if (!kind.hasPowerPorts() && !kind.hasFluidPorts()) {
            return List.of();
        }

        java.util.HashSet<BlockPos> occupied = new java.util.HashSet<>();
        for (BlockPos offset : kind.footprint().offsets()) {
            occupied.add(corePos.offset(LegacyMachineGeometry.rotateLegacySouth(offset, facing)));
        }

        ArrayList<Port> ports = new ArrayList<>();
        for (FusionMachineBlock.LocalPort local : kind.localPorts()) {
            FusionMachineBlock.LocalPort rotated = local.rotate(facing);
            BlockPos proxy = corePos.offset(rotated.offset());
            Direction face = rotated.face();
            BlockPos connector = proxy.relative(face);
            if (!occupied.contains(connector)) {
                ports.add(new Port(proxy.immutable(), connector.immutable(), face, kind.hasPowerPorts(), kind.hasFluidPorts()));
            }
        }
        return List.copyOf(ports);
    }

    public record Port(BlockPos proxyPos, BlockPos connectorPos, Direction face, boolean power, boolean fluid) {
    }

    private enum ForgeArmState {
        REPOSITION,
        EXTEND1,
        EXTEND2,
        RETRACT1,
        RETRACT2,
        RETIRE
    }

    private enum ForgeArmType {
        STRIKER(6),
        JET(4);

        private final int angleCount;

        ForgeArmType(int angleCount) {
            this.angleCount = angleCount;
        }
    }

    private static final double[][] STRIKER_POSITIONS = {
            {20.0D, -30.0D, -20.0D, 30.0D},
            {45.0D, -80.0D, 15.0D, 30.0D},
            {30.0D, -45.0D, -10.0D, 30.0D},
            {15.0D, -20.0D, -30.0D, 30.0D},
            {0.0D, 10.0D, -55.0D, 30.0D}
    };

    private static final double[][] JET_POSITIONS = {
            {10.0D, 45.0D, -120.0D},
            {20.0D, 45.0D, -140.0D},
            {0.0D, 30.0D, -80.0D},
            {0.0D, 40.0D, -100.0D},
            {30.0D, 50.0D, -160.0D}
    };

    private static final class ForgeArm {
        private final ForgeArmType type;
        private final double[] angles;
        private final double[] prevAngles;
        private final double[] targetAngles;
        private final double[] speed;
        private ForgeArmState state = ForgeArmState.RETIRE;
        private int actionDelay;

        private ForgeArm(ForgeArmType type) {
            this.type = type;
            this.angles = new double[type.angleCount];
            this.prevAngles = new double[type.angleCount];
            this.targetAngles = new double[type.angleCount];
            this.speed = new double[type.angleCount];
            for (int i = 0; i < this.speed.length; i++) {
                if (i < 3 || i == 4) {
                    this.speed[i] = 15.0D;
                }
                if (i == 3) {
                    this.speed[i] = 15.0D;
                }
                if (i > 4) {
                    this.speed[i] = 0.5D;
                }
            }
        }

        private void update(boolean didProcess, Runnable strikerSound) {
            System.arraycopy(this.angles, 0, this.prevAngles, 0, this.angles.length);
            if (!didProcess) {
                this.state = ForgeArmState.RETIRE;
            }
            if (this.state == ForgeArmState.RETIRE) {
                this.actionDelay = 0;
            }
            if (this.actionDelay > 0) {
                this.actionDelay--;
                return;
            }
            if (this.type == ForgeArmType.STRIKER) {
                updateStriker(strikerSound);
            } else {
                updateJet();
            }
        }

        private void updateStriker(Runnable strikerSound) {
            switch (this.state) {
                case REPOSITION -> {
                    if (move()) {
                        this.actionDelay = 5;
                        this.state = ForgeArmState.EXTEND1;
                        this.targetAngles[4] = 0.5D;
                    }
                }
                case EXTEND1 -> {
                    if (move()) {
                        this.actionDelay = 0;
                        this.state = ForgeArmState.RETRACT1;
                        this.targetAngles[4] = 0.0D;
                        strikerSound.run();
                    }
                }
                case RETRACT1 -> {
                    if (move()) {
                        this.actionDelay = 0;
                        this.state = ForgeArmState.EXTEND2;
                        this.targetAngles[5] = 0.5D;
                    }
                }
                case EXTEND2 -> {
                    if (move()) {
                        this.actionDelay = 0;
                        this.state = ForgeArmState.RETRACT2;
                        this.targetAngles[5] = 0.0D;
                        strikerSound.run();
                    }
                }
                case RETRACT2 -> {
                    if (move()) {
                        if (PLASMA_FORGE_RANDOM.nextInt(3) == 0) {
                            this.actionDelay = 10;
                            this.state = ForgeArmState.REPOSITION;
                            choosePosition(this, STRIKER_POSITIONS);
                        } else {
                            this.actionDelay = 5;
                            this.state = ForgeArmState.EXTEND1;
                            this.targetAngles[4] = 0.5D;
                        }
                    }
                }
                case RETIRE -> {
                    for (int i = 0; i < this.targetAngles.length; i++) {
                        this.targetAngles[i] = 0.0D;
                    }
                    if (move()) {
                        this.actionDelay = 10;
                        this.state = ForgeArmState.REPOSITION;
                        choosePosition(this, STRIKER_POSITIONS);
                    }
                }
            }
        }

        private void updateJet() {
            switch (this.state) {
                case REPOSITION -> {
                    if (move()) {
                        this.actionDelay = 20 + PLASMA_FORGE_RANDOM.nextInt(3) * 10;
                        this.state = ForgeArmState.REPOSITION;
                        choosePosition(this, JET_POSITIONS);
                    }
                }
                case RETIRE -> {
                    for (int i = 0; i < this.targetAngles.length; i++) {
                        this.targetAngles[i] = 0.0D;
                    }
                    if (move()) {
                        this.actionDelay = 10;
                        this.state = ForgeArmState.REPOSITION;
                        choosePosition(this, JET_POSITIONS);
                    }
                }
                default -> {
                }
            }
        }

        private boolean move() {
            boolean didMove = false;
            for (int i = 0; i < this.angles.length; i++) {
                if (this.angles[i] == this.targetAngles[i]) {
                    continue;
                }
                didMove = true;
                double angle = this.angles[i];
                double target = this.targetAngles[i];
                double turn = this.speed[i];
                double delta = Math.abs(angle - target);
                if (delta <= turn) {
                    this.angles[i] = target;
                } else if (angle < target) {
                    this.angles[i] += turn;
                } else {
                    this.angles[i] -= turn;
                }
            }
            return !didMove;
        }

        private double[] positions(float partialTick) {
            double[] positions = new double[this.angles.length];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = this.prevAngles[i] + (this.angles[i] - this.prevAngles[i]) * partialTick;
            }
            return positions;
        }
    }

    private static void choosePosition(ForgeArm arm, double[][] positions) {
        double[] target = positions[PLASMA_FORGE_RANDOM.nextInt(positions.length)];
        for (int i = 0; i < target.length; i++) {
            arm.targetAngles[i] = target[i];
        }
    }

    private final class FusionFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private FusionFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return FusionMachineBlockEntity.this.tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < FusionMachineBlockEntity.this.tanks.length
                    ? FusionMachineBlockEntity.this.tanks[tank].getFluidInTank(0)
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < FusionMachineBlockEntity.this.tanks.length
                    ? FusionMachineBlockEntity.this.tanks[tank].getTankCapacity(0)
                    : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (!allowsPort(this.queriedPos, this.side) || tank < 0 || tank >= FusionMachineBlockEntity.this.tanks.length) {
                return false;
            }
            for (int index : receivingTankIndexes()) {
                if (index == tank) {
                    return FusionMachineBlockEntity.this.tanks[tank].isFluidValid(0, stack);
                }
            }
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return 0;
            }
            Optional<HbmFluidDefinition> fluid = HbmFluids.fromNeoFluid(resource.getFluid());
            if (fluid.isEmpty() || fluid.get().isNone()) {
                return 0;
            }
            int remaining = resource.getAmount();
            int filled = 0;
            for (int index : receivingTankIndexes()) {
                HbmFluidTank tank = FusionMachineBlockEntity.this.tanks[index];
                if (!tank.type().isNone() && tank.type() != fluid.get()) {
                    continue;
                }
                int accepted = tank.fill(fluid.get(), remaining, action.simulate());
                filled += accepted;
                remaining -= accepted;
                if (remaining <= 0) {
                    break;
                }
            }
            if (filled > 0 && action.execute()) {
                setChangedAndSync(true);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (requested.isNone()) {
                return FluidStack.EMPTY;
            }
            for (int index : sendingTankIndexes()) {
                HbmFluidStack drained = FusionMachineBlockEntity.this.tanks[index].drain(requested, resource.getAmount(), action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        setChangedAndSync(true);
                    }
                    return HbmFluids.toNeoStack(drained.type(), drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            for (int index : sendingTankIndexes()) {
                HbmFluidStack drained = FusionMachineBlockEntity.this.tanks[index].drain(null, maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        setChangedAndSync(true);
                    }
                    return HbmFluids.toNeoStack(drained.type(), drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
