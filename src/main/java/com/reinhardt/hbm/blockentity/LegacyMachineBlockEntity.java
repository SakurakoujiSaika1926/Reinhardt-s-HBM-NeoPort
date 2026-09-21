package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.item.BlueprintItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.item.NuclearWasteItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.RtgDepletedPelletItem;
import com.reinhardt.hbm.item.RtgPelletItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.item.RadarLinkerItem;
import com.reinhardt.hbm.api.machine.RadarCommandReceiver;
import com.reinhardt.hbm.block.MustardWillowTallBlock;
import com.reinhardt.hbm.api.entity.LegacyRadarDetectable;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.entity.SawbladeEntity;
import com.reinhardt.hbm.machine.AnnihilatorRecipes;
import com.reinhardt.hbm.machine.AnnihilatorSavedData;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.CombustibleFuelGrade;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.recipe.CrackingRecipe;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.recipe.PyroOvenRecipe;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionConstants;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmHazardSystem;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Entity backing the machine_ blocks that were present in the original 1.7.10
 * registry. It deliberately owns the core inventory and power endpoint; dummy
 * blocks never become independent machines.
 */
public final class LegacyMachineBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    private static final DustParticleOptions TELEPORTER_PARTICLE =
            new DustParticleOptions(new Vector3f(0.4F, 0.8F, 1.0F), 1.0F);
    public static final int MAX_SLOT_COUNT = 27;
    public static final int SLOT_COUNT = MAX_SLOT_COUNT;
    public static final int DATA_COUNT = 17;

    private final NonNullList<ItemStack> items;
    private final int[] slotsForFace;
    private final HbmFluidTank[] tanks;
    private final HbmFluidTank inputTank;
    private final HbmFluidTank outputTank;
    private long energy;
    private long lastInput;
    private int progress;
    private int completed;
    private int autocrafterRecipeIndex;
    private int autocrafterRecipeCount;
    private final String[] autocrafterModes = new String[9];
    private int orbusMode;
    private int forcefieldHealth = 100;
    private int forcefieldMaxHealth = 100;
    private int forcefieldRadius = 16;
    private int forcefieldPowerConsumption;
    private int forcefieldColor = 0x0000FF;
    private boolean forcefieldOn;
    private int forcefieldCooldown;
    private int forcefieldBlink;
    private final List<Entity> forcefieldOutside = new ArrayList<>();
    private final List<Entity> forcefieldInside = new ArrayList<>();
    private int teleporterTargetX = -1;
    private int teleporterTargetY = -1;
    private int teleporterTargetZ = -1;
    private String teleporterTargetDimension = "minecraft:overworld";
    private int radiolysisHeat;
    private boolean sawmillHasBlade = true;
    private int sawmillHeat;
    private float sawmillRotation;
    private float sawmillRotationSpeed;
    private int sawmillWarnCooldown;
    private int sawmillOverspeed;
    private boolean autosawOn;
    private boolean autosawSuspended;
    private float autosawYaw;
    private float autosawPitch;
    private float autosawPreviousYaw;
    private float autosawPreviousPitch;
    private float autosawSyncYaw;
    private float autosawSyncPitch;
    private int autosawTurnProgress;
    private int autosawState;
    private int autosawForceSkip;
    private float autosawSpin;
    private float autosawPreviousSpin;
    private boolean thresherOn;
    private boolean thresherSuspended;
    private int thresherDelay;
    private int thresherState;
    private float thresherAngle;
    private float thresherPreviousAngle;
    private float thresherSpin;
    private float thresherPreviousSpin;
    private float thresherSyncAngle;
    private int thresherTurnProgress;
    private double conveyorPress;
    private boolean conveyorRetracting;
    private int conveyorDelay;
    private boolean pyroProgressing;
    private boolean pyroVenting;
    private int pyroAnimation;
    private int pyroDuration = 1;
    private final int[] radgenProgress = new int[12];
    private final int[] radgenDuration = new int[12];
    private final int[] radgenProduction = new int[12];
    private final ItemStack[] radgenProcessing = new ItemStack[12];
    private boolean radgenOn;
    private int rtgHeat;
    @Nullable
    private ResourceLocation precisionAssemblerRecipe;
    private long precisionAssemblerMaxPower = 100_000L;
    private double precisionAssemblerProgress;
    private int precisionAssemblerRecipeIndex;
    private int precisionAssemblerRecipeCount;
    private boolean precisionAssemblerWorking;
    private long precisionAssemblerDemand = 100L;
    private double precisionAssemblerPreviousRing;
    private double precisionAssemblerRing;
    private double precisionAssemblerRingSpeed;
    private double precisionAssemblerRingTarget;
    private int precisionAssemblerRingDelay;
    private final double[] precisionAssemblerArmAngles = {45.0D, -30.0D, 45.0D};
    private final double[] precisionAssemblerPreviousArmAngles = {45.0D, -30.0D, 45.0D};
    private final double[] precisionAssemblerStrikers = new double[4];
    private final double[] precisionAssemblerPreviousStrikers = new double[4];
    private final boolean[] precisionAssemblerStrikerDirection = new boolean[4];
    private int precisionAssemblerStrikerIndex;
    private int precisionAssemblerStrikerDelay;
    private long precisionAssemblerLastClientTick = Long.MIN_VALUE;
    private int precisionAssemblerMotorSoundDelay;
    private String annihilatorPool = "Recycling";
    private BigInteger annihilatorMonitor = BigInteger.ZERO;
    private int annihilatorSyncTicks;
    // TileEntityMachineTurbofan's synchronized render and GUI state.
    private int turbofanAfterburner;
    private boolean turbofanWasOn;
    private boolean turbofanShowBlood;
    private int turbofanOutput;
    private int turbofanConsumption;
    private float turbofanSpin;
    private float turbofanLastSpin;
    private int turbofanMomentum;
    // TileEntityMachineRadarNT: scan state remains server-authoritative and is mirrored with the BE update tag.
    private boolean radarScanMissiles = true;
    private boolean radarScanShells = true;
    private boolean radarScanPlayers = true;
    private boolean radarSmartMode = true;
    private boolean radarRedMode = true;
    private boolean radarShowMap;
    private boolean radarJammed;
    private int radarPingTimer;
    private int radarLastRedPower;
    private float radarRotation;
    private float radarPreviousRotation;
    private byte[] radarMap = new byte[40_000];
    private final List<RadarTarget> radarTargets = new ArrayList<>();
    private final Map<Long, CompletableFuture<Boolean>> radarChunkReads = new HashMap<>();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            String id = LegacyMachineBlockEntity.this.machineId();
            return switch (index) {
                case 0 -> (int) LegacyMachineBlockEntity.this.energy;
                case 1 -> id.equals("machine_forcefield")
                        ? LegacyMachineBlockEntity.this.forcefieldHealth : LegacyMachineBlockEntity.this.lastInputAsInt();
                case 2 -> id.equals("machine_forcefield")
                        ? LegacyMachineBlockEntity.this.forcefieldMaxHealth : LegacyMachineBlockEntity.this.progress;
                case 3 -> id.equals("machine_forcefield")
                        ? LegacyMachineBlockEntity.this.forcefieldRadius : LegacyMachineBlockEntity.this.completed;
                case 4 -> id.equals("machine_forcefield")
                        ? (LegacyMachineBlockEntity.this.forcefieldOn ? 1 : 0)
                        : id.equals("machine_rtg_grey") ? LegacyMachineBlockEntity.this.rtgHeat
                        : LegacyMachineBlockEntity.this.radiolysisHeat;
                case 5 -> id.equals("machine_teleporter")
                        ? LegacyMachineBlockEntity.this.teleporterTargetX : LegacyMachineBlockEntity.this.tankOldId(0);
                case 6 -> id.equals("machine_teleporter")
                        ? LegacyMachineBlockEntity.this.teleporterTargetY : LegacyMachineBlockEntity.this.tankAmount(0);
                case 7 -> id.equals("machine_teleporter")
                        ? LegacyMachineBlockEntity.this.teleporterTargetZ : LegacyMachineBlockEntity.this.tankOldId(1);
                case 8 -> id.equals("machine_teleporter")
                        ? LegacyMachineBlockEntity.this.teleporterLegacyDimensionId() : LegacyMachineBlockEntity.this.tankAmount(1);
                case 9 -> id.equals("machine_autocrafter")
                        ? LegacyMachineBlockEntity.this.autocrafterRecipeIndex
                        : id.equals("machine_orbus") ? LegacyMachineBlockEntity.this.orbusMode : LegacyMachineBlockEntity.this.tankOldId(2);
                case 10 -> id.equals("machine_autocrafter")
                        ? LegacyMachineBlockEntity.this.autocrafterRecipeCount : LegacyMachineBlockEntity.this.tankAmount(2);
                case 11 -> id.equals("machine_turbofan") ? LegacyMachineBlockEntity.this.turbofanAfterburner : LegacyMachineBlockEntity.this.pyroDuration;
                case 12 -> id.equals("machine_turbofan") ? (LegacyMachineBlockEntity.this.turbofanShowBlood ? 1 : 0)
                        : id.equals("machine_precass") ? (int) Math.min(Integer.MAX_VALUE, LegacyMachineBlockEntity.this.precisionAssemblerMaxPower) : 0;
                case 13 -> id.equals("machine_missile_assembly") ? LegacyMachineBlockEntity.this.missileAssemblyStateMask()
                        : id.equals("machine_precass") ? 1_000 : 0;
                case 14 -> id.equals("machine_missile_assembly") && LegacyMachineBlockEntity.this.canConstructMissile() ? 1
                        : id.equals("machine_precass") ? LegacyMachineBlockEntity.this.precisionAssemblerRecipeIndex : 0;
                case 15 -> id.equals("machine_precass") ? LegacyMachineBlockEntity.this.precisionAssemblerRecipeCount : 0;
                case 16 -> id.equals("machine_precass") && LegacyMachineBlockEntity.this.precisionAssemblerWorking ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            String id = LegacyMachineBlockEntity.this.machineId();
            switch (index) {
                case 0 -> LegacyMachineBlockEntity.this.energy = Math.max(0, value);
                case 1 -> {
                    if (id.equals("machine_forcefield")) LegacyMachineBlockEntity.this.forcefieldHealth = Math.max(0, value);
                    else if (id.equals("machine_teleporter")) LegacyMachineBlockEntity.this.teleporterTargetX = value;
                    else LegacyMachineBlockEntity.this.lastInput = Math.max(0, value);
                }
                case 2 -> {
                    if (id.equals("machine_forcefield")) LegacyMachineBlockEntity.this.forcefieldMaxHealth = Math.max(1, value);
                    else if (id.equals("machine_teleporter")) LegacyMachineBlockEntity.this.teleporterTargetY = value;
                    else LegacyMachineBlockEntity.this.progress = Math.max(0, value);
                }
                case 3 -> {
                    if (id.equals("machine_forcefield")) LegacyMachineBlockEntity.this.forcefieldRadius = Math.max(0, value);
                    else if (id.equals("machine_teleporter")) LegacyMachineBlockEntity.this.teleporterTargetZ = value;
                    else LegacyMachineBlockEntity.this.completed = Math.max(0, value);
                }
                case 4 -> {
                    if (id.equals("machine_forcefield")) LegacyMachineBlockEntity.this.forcefieldOn = value != 0;
                    else LegacyMachineBlockEntity.this.radiolysisHeat = Math.max(0, value);
                }
                case 8 -> { if (id.equals("machine_teleporter")) LegacyMachineBlockEntity.this.setTeleporterDimensionId(value); }
                case 9 -> {
                    if (id.equals("machine_autocrafter")) LegacyMachineBlockEntity.this.autocrafterRecipeIndex = Math.max(0, value);
                    else if (id.equals("machine_orbus")) LegacyMachineBlockEntity.this.orbusMode = Math.floorMod(value, 4);
                }
                case 10 -> { if (id.equals("machine_autocrafter")) LegacyMachineBlockEntity.this.autocrafterRecipeCount = Math.max(0, value); }
                case 11 -> { if (id.equals("machine_turbofan")) LegacyMachineBlockEntity.this.turbofanAfterburner = Math.max(0, value); }
                case 12 -> {
                    if (id.equals("machine_turbofan")) LegacyMachineBlockEntity.this.turbofanShowBlood = value != 0;
                    else if (id.equals("machine_precass")) LegacyMachineBlockEntity.this.precisionAssemblerMaxPower = Math.max(100_000L, value);
                }
                case 14 -> { if (id.equals("machine_precass")) LegacyMachineBlockEntity.this.precisionAssemblerRecipeIndex = Math.max(0, value); }
                case 15 -> { if (id.equals("machine_precass")) LegacyMachineBlockEntity.this.precisionAssemblerRecipeCount = Math.max(0, value); }
                case 16 -> { if (id.equals("machine_precass")) LegacyMachineBlockEntity.this.precisionAssemblerWorking = value != 0; }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LegacyMachineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_MACHINE.get(), pos, state);
        LegacyMachineProfile profile = profile();
        this.items = NonNullList.withSize(profile.slots(), ItemStack.EMPTY);
        this.slotsForFace = createSlots(profile.slots());
        this.tanks = new HbmFluidTank[profile.fluidTankCapacities().length];
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i] = new HbmFluidTank(profile.fluidTankCapacities()[i]);
        }
        this.inputTank = profile.fillTanks().length == 0 ? null : this.tanks[profile.fillTanks()[0]];
        this.outputTank = profile.drainTanks().length == 0 ? null : this.tanks[profile.drainTanks()[0]];
        if (machineId().equals("machine_autosaw") && this.inputTank != null) {
            this.inputTank.setType(HbmFluids.byName("woodoil").orElse(HbmFluids.none()));
        }
        if (machineId().equals("machine_thresher") && this.inputTank != null) {
            this.inputTank.setType(HbmFluids.byName("woodoil").orElse(HbmFluids.none()));
        }
        if (machineId().equals("machine_turbofan") && this.tanks.length > 0) {
            this.tanks[0].setType(HbmFluids.byName("kerosene").orElse(HbmFluids.none()));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyMachineBlockEntity machine) {
        if (level.isClientSide) {
            machine.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, machine);
        machine.tickServer(level);
    }

    private void tickServer(Level level) {
        if (machineId().equals("machine_annihilator")) {
            tickAnnihilator(level);
        } else if (machineId().equals("machine_autocrafter")) {
            tickAutocrafter(level);
        } else if (machineId().equals("machine_forcefield")) {
            tickForcefield(level);
        } else if (machineId().equals("machine_orbus")) {
            tickOrbus();
        } else if (machineId().equals("machine_teleporter")) {
            tickTeleporter(level);
        } else if (machineId().equals("machine_radgen")) {
            tickRadGen();
        } else if (machineId().equals("machine_radiolysis")) {
            tickRadiolysis(level);
        } else if (machineId().equals("machine_rtg_grey")) {
            tickRtg();
        } else if (machineId().equals("machine_sawmill")) {
            tickSawmill(level);
        } else if (machineId().equals("machine_autosaw")) {
            tickAutosaw(level);
        } else if (machineId().equals("machine_thresher")) {
            tickThresher(level);
        } else if (machineId().equals("machine_conveyor_press")) {
            tickConveyorPress(level);
        } else if (machineId().equals("machine_pyrooven")) {
            tickPyroOven(level);
        } else if (machineId().equals("machine_precass")) {
            tickPrecisionAssembler(level);
        } else if (machineId().equals("machine_turbofan")) {
            tickTurbofan(level);
        } else if (machineId().equals("machine_radar") || machineId().equals("machine_radar_large")) {
            tickRadar(level);
        }
    }

    /** Direct port of TileEntityMachineRadarNT#updateEntity and #allocateTargets. */
    private void tickRadar(Level level) {
        long beforeCharge = this.energy;
        this.energy = BatteryPackItem.dischargeIntoMachine(this.items.get(9), this.energy, energyCapacity());
        this.radarJammed = false;

        List<RadarTarget> previousTargets = List.copyOf(this.radarTargets);
        this.radarTargets.clear();
        int redPower = 0;
        long consumption = radarConsumption();
        if (this.worldPosition.getY() >= radarAltitude() && this.energy >= consumption) {
            this.energy -= consumption;
            int range = radarRange();
            AABB bounds = new AABB(
                    this.worldPosition.getX() + 0.5D - range, level.getMinBuildHeight(), this.worldPosition.getZ() + 0.5D - range,
                    this.worldPosition.getX() + 0.5D + range, level.getMaxBuildHeight(), this.worldPosition.getZ() + 0.5D + range
            );
            LegacyRadarDetectable.RadarScanParams params = new LegacyRadarDetectable.RadarScanParams(
                    this.radarScanMissiles, this.radarScanShells, this.radarScanPlayers, this.radarSmartMode
            );
            for (Entity entity : level.getEntities((Entity) null, bounds, Entity::isAlive)) {
                if (entity.getY() - this.worldPosition.getY() <= radarBuffer()) {
                    continue;
                }
                if (entity instanceof LivingEntity living
                        && HbmLivingRadiation.get(living).getDigamma() > 0.001F) {
                    this.radarJammed = true;
                    this.radarTargets.clear();
                    break;
                }
                RadarTarget target = radarTarget(entity, params);
                if (target == null) {
                    continue;
                }
                this.radarTargets.add(target);
                if (!target.redstone()) {
                    continue;
                }
                if (this.radarRedMode) {
                    double maxRange = range * Math.sqrt(2.0D);
                    double distance = Math.sqrt(Math.pow(target.x() - this.worldPosition.getX(), 2.0D)
                            + Math.pow(target.z() - this.worldPosition.getZ(), 2.0D));
                    redPower = Math.max(redPower, 15 - (int) Math.floor(distance / maxRange * 15.0D));
                } else {
                    redPower = Math.max(redPower, target.blipLevel() + 1);
                }
            }
            if (this.radarShowMap) {
                sampleRadarMap(level, range);
            }
        }

        if (++this.radarPingTimer >= 80 && this.energy > 0L) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.SONAR_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
            this.radarPingTimer = 0;
        }
        if (this.radarLastRedPower != redPower) {
            this.radarLastRedPower = redPower;
            level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            for (BlockPos port : servicePorts()) {
                level.updateNeighborsAt(port, this.getBlockState().getBlock());
            }
        }

        BlockPos linkedPosition = RadarLinkerItem.position(this.items.get(8));
        if (linkedPosition != null && level.getBlockEntity(linkedPosition) instanceof RadarScreenBlockEntity screen) {
            screen.receiveRadarData(this.radarTargets, this.worldPosition, radarRange());
        }

        // 1.7.10 sent its radar state every 50 ticks. The modern BE update tag carries the same data.
        if (beforeCharge != this.energy || !previousTargets.equals(this.radarTargets) || level.getGameTime() % 50L == 0L) {
            if (level.getGameTime() % 50L == 0L || !previousTargets.equals(this.radarTargets)) {
                setChangedAndSync();
            } else {
                setChanged();
            }
        }
    }

    @Nullable
    private RadarTarget radarTarget(Entity entity, LegacyRadarDetectable.RadarScanParams params) {
        if (entity instanceof LegacyRadarDetectable detectable) {
            if (!detectable.canBeSeenBy(this) || !detectable.paramsApplicable(params)) {
                return null;
            }
            return new RadarTarget(
                    detectable.translationKey(),
                    Math.max(0, Math.min(LegacyRadarDetectable.SPECIAL, detectable.blipLevel())),
                    (int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()),
                    entity.getId(), detectable.suppliesRedstone(params)
            );
        }
        if (entity instanceof Player player && this.radarScanPlayers) {
            return new RadarTarget(player.getName().getString(), LegacyRadarDetectable.PLAYER,
                    (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()),
                    player.getId(), true);
        }
        return null;
    }

    private void sampleRadarMap(Level level, int range) {
        if (this.radarMap.length != 40_000) {
            this.radarMap = new byte[40_000];
        }
        ServerLevel serverLevel = level instanceof ServerLevel server ? server : null;
        int chunkLoads = 0;
        int chunkLoadCap = HbmConfig.RADAR_CHUNK_LOAD_CAP.get();
        long page = level.getGameTime() % 400L;
        for (int offset = 0; offset < 100; offset++) {
            int index = (int) page * 100 + offset;
            int x = this.worldPosition.getX() - range + (index % 200) * range * 2 / 200;
            int z = this.worldPosition.getZ() - range + (index / 200) * range * 2 / 200;
            BlockPos samplePos = new BlockPos(x, 0, z);
            if (level.hasChunkAt(samplePos)) {
                writeRadarMapHeight(level, index, x, z);
                continue;
            }
            if (serverLevel == null || this.radarMap[index] != 0 || chunkLoads >= chunkLoadCap) {
                continue;
            }

            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            if (HbmConfig.RADAR_GENERATE_CHUNKS.get()) {
                // This is the direct modern equivalent of getChunkFromChunkCoords.
                serverLevel.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                if (level.hasChunkAt(samplePos)) {
                    writeRadarMapHeight(level, index, x, z);
                }
                chunkLoads++;
                continue;
            }

            CompletableFuture<Boolean> diskRead = this.radarChunkReads.get(chunkKey);
            if (diskRead == null) {
                // WorldUtil.provideChunk only loaded an existing chunk file and never generated terrain.
                this.radarChunkReads.put(chunkKey, serverLevel.getChunkSource().chunkMap
                        .read(new ChunkPos(chunkX, chunkZ))
                        .handle((storedChunk, error) -> error == null && storedChunk.isPresent()));
                chunkLoads++;
                continue;
            }
            if (!diskRead.isDone()) {
                continue;
            }
            this.radarChunkReads.remove(chunkKey);
            if (!diskRead.getNow(false)) {
                continue;
            }
            serverLevel.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            if (level.hasChunkAt(samplePos)) {
                writeRadarMapHeight(level, index, x, z);
            }
            chunkLoads++;
        }
    }

    private void writeRadarMapHeight(Level level, int index, int x, int z) {
        int height = Math.max(50, Math.min(128,
                level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z)));
        this.radarMap[index] = (byte) height;
    }

    private int radarRange() {
        return machineId().equals("machine_radar_large")
                ? HbmConfig.RADAR_LARGE_RANGE.get()
                : HbmConfig.RADAR_RANGE.get();
    }

    private long radarConsumption() {
        return HbmConfig.RADAR_CONSUMPTION.get();
    }

    private int radarBuffer() {
        return HbmConfig.RADAR_BUFFER.get();
    }

    public int radarAltitude() {
        return HbmConfig.RADAR_ALTITUDE.get();
    }

    /** Direct port of TileEntityMachineAnnihilator#updateEntity. */
    private void tickAnnihilator(Level level) {
        configureAnnihilatorFluidInput();
        if (!(level instanceof ServerLevel serverLevel) || this.annihilatorPool.isBlank()) {
            return;
        }

        AnnihilatorSavedData data = AnnihilatorSavedData.get(serverLevel);
        boolean didSomething = false;

        ItemStack trash = this.items.get(0);
        if (!trash.isEmpty()) {
            destroyAnnihilatorItem(serverLevel, trash);
            AnnihilatorRecipes.incrementAll(trash, data, annihilatorPool, trash.getCount());
            tryAddAnnihilatorPayout(AnnihilatorRecipes.highestPayout(trash, data, annihilatorPool, trash.getCount(), false));
            this.items.set(0, ItemStack.EMPTY);
            didSomething = true;
        }

        HbmFluidTank tank = this.tanks.length == 0 ? null : this.tanks[0];
        if (tank != null && tank.amount() > 0 && !tank.type().isNone()) {
            HbmPollution.polluteFluid(level, this.worldPosition, tank.type(), HbmPollution.ReleaseType.BURN, tank.amount() * 2.0D);
            AnnihilatorRecipes.incrementFluid(tank.type().name(), data, annihilatorPool, tank.amount());
            tank.clear();
            didSomething = true;
        }

        ItemStack monitor = this.items.get(8);
        this.annihilatorMonitor = monitor.isEmpty()
                ? BigInteger.ZERO
                : data.count(annihilatorPool, AnnihilatorRecipes.monitorKey(monitor));

        ItemStack request = this.items.get(9);
        if (!request.isEmpty()) {
            ItemStack single = request.copyWithCount(1);
            destroyAnnihilatorItem(serverLevel, single);
            AnnihilatorRecipes.incrementAll(single, data, annihilatorPool, 1);
            AnnihilatorRecipes.Payout payout = AnnihilatorRecipes.highestPayout(single, data, annihilatorPool, 1, true);
            this.items.get(9).shrink(1);
            if (this.items.get(9).isEmpty()) {
                this.items.set(9, ItemStack.EMPTY);
            }
            tryAddAnnihilatorRequestPayout(payout);
            didSomething = true;
        }

        if (didSomething) {
            BlockPos flame = annihilatorFlamePos();
            serverLevel.sendParticles(HbmParticleTypes.GAS_FLARE_FLAME.get(),
                    flame.getX() + 0.5D, flame.getY() - 0.25D, flame.getZ() + 0.5D,
                    1, 0.05D, 0.0D, 0.05D, 0.1D);
            if (level.getGameTime() % 3L == 0L) {
                level.playSound(null, flame.getX() + 0.5D, flame.getY() - 0.25D, flame.getZ() + 0.5D,
                        HbmSoundEvents.FLAMETHROWER_SHOOT.get(), SoundSource.BLOCKS,
                        1.0F, 0.5F + level.random.nextFloat() * 0.25F);
            }
            setChanged();
        }

        if (++this.annihilatorSyncTicks >= 25) {
            this.annihilatorSyncTicks = 0;
            setChangedAndSync();
        }
    }

    private void configureAnnihilatorFluidInput() {
        if (this.tanks.length == 0 || !(this.items.get(1).getItem() instanceof FluidIdentifierItem)) {
            return;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(this.items.get(1));
        if (!selected.isNone() && this.tanks[0].amount() == 0 && this.tanks[0].type() != selected) {
            this.tanks[0].setType(selected);
        }
    }

    private BlockPos annihilatorFlamePos() {
        return this.worldPosition.relative(facing().getOpposite(), 3).above(9);
    }

    private void destroyAnnihilatorItem(ServerLevel level, ItemStack stack) {
        double radiation = HbmHazardSystem.hazards(stack).radiation();
        if (radiation > 0.0D) {
            ChunkRadiationData.get(level).incrementRadiation(annihilatorFlamePos(), Math.min(radiation * 5.0D, 1_000.0D));
        }
    }

    private void tryAddAnnihilatorPayout(@Nullable AnnihilatorRecipes.Payout payout) {
        if (payout == null) {
            return;
        }
        ItemStack reward = payout.stack();
        for (int slot = 2; slot <= 7; slot++) {
            ItemStack present = this.items.get(slot);
            if (ItemStack.isSameItemSameComponents(present, reward)
                    && present.getCount() + reward.getCount() <= present.getMaxStackSize()) {
                present.grow(reward.getCount());
                return;
            }
        }
        for (int slot = 2; slot <= 7; slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, reward);
                return;
            }
        }
    }

    private void tryAddAnnihilatorRequestPayout(@Nullable AnnihilatorRecipes.Payout payout) {
        if (payout == null) {
            return;
        }
        ItemStack reward = payout.stack();
        ItemStack output = this.items.get(10);
        if (output.isEmpty()) {
            this.items.set(10, reward);
        } else if (ItemStack.isSameItemSameComponents(output, reward)
                && output.getCount() + reward.getCount() <= output.getMaxStackSize()) {
            output.grow(reward.getCount());
        }
    }

    /** Client animation state for the legacy precision assembler and turbofan. */
    private void tickClient() {
        if (this.level == null) return;
        if (machineId().equals("machine_teleporter")) {
            tickTeleporterClient();
            return;
        }
        if (machineId().equals("machine_radar") || machineId().equals("machine_radar_large")) {
            this.radarPreviousRotation = this.radarRotation;
            if (this.energy > 0L) {
                this.radarRotation = (this.radarRotation + 5.0F) % 360.0F;
            }
            return;
        }
        if (machineId().equals("machine_thresher")) {
            this.thresherPreviousAngle = this.thresherAngle;
            this.thresherPreviousSpin = this.thresherSpin;
            if (this.thresherOn && !this.thresherSuspended && this.thresherAngle > 0.0F) {
                this.thresherSpin += 15.0F;
            }
            if (this.thresherSpin >= 360.0F) {
                this.thresherSpin -= 360.0F;
                this.thresherPreviousSpin -= 360.0F;
            }
            if (this.thresherTurnProgress > 0) {
                float delta = this.thresherSyncAngle - this.thresherAngle;
                if (Math.abs(delta) <= 0.05F) {
                    this.thresherAngle = this.thresherSyncAngle;
                    this.thresherTurnProgress = 0;
                } else {
                    this.thresherAngle += delta / this.thresherTurnProgress--;
                }
            } else {
                this.thresherAngle = this.thresherSyncAngle;
            }
            if (this.thresherOn && !this.thresherSuspended) {
                // TileEntityMachineThresher emits one stationary smoke particle each client tick.
                Direction dir = facing();
                Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
                this.level.addParticle(ParticleTypes.SMOKE,
                        this.worldPosition.getX() + 0.5D + dir.getStepX() * 0.8125D + rot.getStepX() * 0.375D,
                        this.worldPosition.getY() + 1.5625D,
                        this.worldPosition.getZ() + 0.5D + dir.getStepZ() * 0.8125D + rot.getStepZ() * 0.375D,
                        0.0D, 0.0D, 0.0D);
            }
            com.reinhardt.hbm.client.sound.ThresherClientSounds.tick(this);
            return;
        }
        if (machineId().equals("machine_autosaw")) {
            this.autosawPreviousYaw = this.autosawYaw;
            this.autosawPreviousPitch = this.autosawPitch;
            this.autosawPreviousSpin = this.autosawSpin;
            if (this.autosawOn && !this.autosawSuspended) {
                this.autosawSpin += 15.0F;
            }
            if (this.autosawSpin >= 360.0F) {
                this.autosawSpin -= 360.0F;
                this.autosawPreviousSpin -= 360.0F;
            }
            if (this.autosawTurnProgress > 0) {
                this.autosawYaw += Mth.wrapDegrees(this.autosawSyncYaw - this.autosawYaw) / this.autosawTurnProgress;
                this.autosawPitch += Mth.wrapDegrees(this.autosawSyncPitch - this.autosawPitch) / this.autosawTurnProgress;
                this.autosawTurnProgress--;
            } else {
                this.autosawYaw = this.autosawSyncYaw;
                this.autosawPitch = this.autosawSyncPitch;
            }
            com.reinhardt.hbm.client.sound.AutosawClientSounds.tick(this);
            return;
        }
        if (machineId().equals("machine_turbofan")) {
            tickTurbofanClient();
            return;
        }
        if (machineId().equals("machine_pyrooven")) {
            tickPyroOvenClient();
            return;
        }
        if (!machineId().equals("machine_precass")) return;
        long gameTime = this.level.getGameTime();
        if (this.precisionAssemblerLastClientTick == gameTime) return;
        int steps = this.precisionAssemblerLastClientTick == Long.MIN_VALUE ? 1
                : (int) Math.min(5L, Math.max(1L, gameTime - this.precisionAssemblerLastClientTick));
        for (int step = 0; step < steps; step++) stepPrecisionAssemblerAnimation();
        this.precisionAssemblerLastClientTick = gameTime;
    }

    /** Direct port of TileEntityMachineTeleporter's charged-pad red-dust effect. */
    private void tickTeleporterClient() {
        if (this.teleporterTargetY == -1 || this.energy < 1_000_000L) {
            return;
        }
        double x = this.worldPosition.getX() + 0.5D + this.level.random.nextGaussian() * 0.25D;
        double y = this.worldPosition.getY() + 1.0D + this.level.random.nextDouble() * 2.0D;
        double z = this.worldPosition.getZ() + 0.5D + this.level.random.nextGaussian() * 0.25D;
        this.level.addParticle(TELEPORTER_PARTICLE, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private void tickTurbofanClient() {
        this.turbofanLastSpin = this.turbofanSpin;
        if (this.turbofanWasOn) {
            this.turbofanMomentum = Math.min(100, this.turbofanMomentum + 1);
        } else {
            this.turbofanMomentum = Math.max(0, this.turbofanMomentum - 1);
        }
        this.turbofanSpin += this.turbofanMomentum / 2.0F;
        if (this.turbofanSpin >= 360.0F) {
            this.turbofanSpin -= 360.0F;
            this.turbofanLastSpin -= 360.0F;
        }
        com.reinhardt.hbm.client.sound.TurbofanClientSounds.tick(this);
    }

    /** Client effects copied from TileEntityMachinePyroOven#updateEntity. */
    private void tickPyroOvenClient() {
        Direction dir = facing();
        Direction rot = LegacyMachineGeometry.forgeRotateDown(dir);
        double x = this.worldPosition.getX() + 0.5D - rot.getStepX();
        double z = this.worldPosition.getZ() + 0.5D - rot.getStepZ();
        double y = this.worldPosition.getY() + 3.0D;
        if (this.pyroProgressing) {
            if (this.level.random.nextInt(20) == 0) {
                this.level.addParticle(ParticleTypes.CLOUD, x - dir.getStepX() * 0.875D, y, z - dir.getStepZ() * 0.875D, 0.0D, 0.05D, 0.0D);
            }
            if (this.level.random.nextInt(20) == 0) {
                this.level.addParticle(ParticleTypes.CLOUD, x - dir.getStepX() * 2.375D, y, z - dir.getStepZ() * 2.375D, 0.0D, 0.05D, 0.0D);
            }
            if (this.level.random.nextInt(20) == 0) {
                this.level.addParticle(ParticleTypes.CLOUD, x + dir.getStepX() * 0.875D, y, z + dir.getStepZ() * 0.875D, 0.0D, 0.05D, 0.0D);
            }
            if (this.level.random.nextInt(20) == 0) {
                this.level.addParticle(ParticleTypes.CLOUD, x + dir.getStepX() * 2.375D, y, z + dir.getStepZ() * 2.375D, 0.0D, 0.05D, 0.0D);
            }
        }
        if (this.pyroVenting && this.level.getGameTime() % 2L == 0L) {
            double soot = 0x20 / 255.0D;
            this.level.addParticle(HbmParticleTypes.PYRO_OVEN_TOWER.get(), x, y, z, soot, soot, soot);
        }
        com.reinhardt.hbm.client.sound.PyroOvenClientSounds.tick(this);
    }

    private void stepPrecisionAssemblerAnimation() {
        boolean armsWereWorking = precisionAssemblerArmsAt(45.0D, -15.0D, -5.0D);
        for (int index = 0; index < 3; index++) this.precisionAssemblerPreviousArmAngles[index] = this.precisionAssemblerArmAngles[index];
        for (int index = 0; index < 4; index++) this.precisionAssemblerPreviousStrikers[index] = this.precisionAssemblerStrikers[index];
        this.precisionAssemblerPreviousRing = this.precisionAssemblerRing;

        for (int index = 0; index < 4; index++) {
            if (this.precisionAssemblerStrikerDirection[index]) {
                this.precisionAssemblerStrikers[index] = -0.75D;
                this.precisionAssemblerStrikerDirection[index] = false;
            } else {
                this.precisionAssemblerStrikers[index] = Math.min(0.0D, this.precisionAssemblerStrikers[index] + 0.5D);
            }
        }
        if (this.precisionAssemblerRing != this.precisionAssemblerRingTarget) {
            double delta = Math.abs(this.precisionAssemblerRingTarget - this.precisionAssemblerRing);
            if (delta <= this.precisionAssemblerRingSpeed) this.precisionAssemblerRing = this.precisionAssemblerRingTarget;
            else if (this.precisionAssemblerRingTarget > this.precisionAssemblerRing) this.precisionAssemblerRing += this.precisionAssemblerRingSpeed;
            else this.precisionAssemblerRing -= this.precisionAssemblerRingSpeed;
            if (this.precisionAssemblerRing == this.precisionAssemblerRingTarget) {
                double wrap = this.precisionAssemblerRingTarget >= 360.0D ? -360.0D : 360.0D;
                this.precisionAssemblerRingTarget += wrap;
                this.precisionAssemblerRing += wrap;
                this.precisionAssemblerPreviousRing += wrap;
                this.precisionAssemblerRingDelay = 100 + this.level.random.nextInt(21);
            }
        }
        if (this.precisionAssemblerWorking) {
            if (this.precisionAssemblerRing == this.precisionAssemblerRingTarget && this.precisionAssemblerRingDelay-- <= 0) {
                this.precisionAssemblerRingTarget += 45.0D * (this.level.random.nextBoolean() ? -1.0D : 1.0D);
                this.precisionAssemblerRingSpeed = 10.0D + this.level.random.nextDouble() * 5.0D;
                playPrecisionAssemblerSound(HbmSoundEvents.ASSEMBLER_START.get(), 0.25F, 1.25F + this.level.random.nextFloat() * 0.25F);
            }
            if (this.precisionAssemblerMotorSoundDelay-- <= 0) {
                this.precisionAssemblerMotorSoundDelay = 20;
                playPrecisionAssemblerSound(HbmSoundEvents.ASSEMBLER_MOTOR.get(), 0.5F, 0.75F);
            }
            if (!precisionAssemblerArmsAt(45.0D, -15.0D, -5.0D) && precisionAssemblerArmsCanMove()) {
                movePrecisionAssemblerArms(45.0D, -15.0D, -5.0D);
            }
            if (precisionAssemblerArmsAt(45.0D, -15.0D, -5.0D) && --this.precisionAssemblerStrikerDelay <= 0) {
                this.precisionAssemblerStrikerDirection[this.precisionAssemblerStrikerIndex] = true;
                playPrecisionAssemblerSound(HbmSoundEvents.ASSEMBLER_STRIKE.get(), 0.5F, 1.25F);
                this.precisionAssemblerStrikerIndex = (this.precisionAssemblerStrikerIndex + 1) % this.precisionAssemblerStrikers.length;
                this.precisionAssemblerStrikerDelay = this.precisionAssemblerStrikerIndex == 3 ? 10 + this.level.random.nextInt(3) : 2;
            }
        } else {
            for (int index = 0; index < 4; index++) this.precisionAssemblerStrikerDirection[index] = false;
            if (precisionAssemblerArmsCanMove()) movePrecisionAssemblerArms(45.0D, -30.0D, 45.0D);
            this.precisionAssemblerMotorSoundDelay = 0;
        }
        if (armsWereWorking && !precisionAssemblerArmsAt(45.0D, -15.0D, -5.0D)) {
            playPrecisionAssemblerSound(HbmSoundEvents.ASSEMBLER_STOP.get(), 0.25F, 1.25F + this.level.random.nextFloat() * 0.25F);
        }
    }

    private void playPrecisionAssemblerSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        this.level.playLocalSound(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    private boolean precisionAssemblerArmsCanMove() {
        for (double striker : this.precisionAssemblerStrikers) if (striker != 0.0D) return false;
        return true;
    }

    private boolean precisionAssemblerArmsAt(double first, double second, double third) {
        return this.precisionAssemblerArmAngles[0] == first && this.precisionAssemblerArmAngles[1] == second && this.precisionAssemblerArmAngles[2] == third;
    }

    private void movePrecisionAssemblerArms(double first, double second, double third) {
        double[] target = {first, second, third};
        for (int index = 0; index < this.precisionAssemblerArmAngles.length; index++) {
            double angle = this.precisionAssemblerArmAngles[index];
            if (angle == target[index]) continue;
            if (Math.abs(angle - target[index]) <= 15.0D) this.precisionAssemblerArmAngles[index] = target[index];
            else this.precisionAssemblerArmAngles[index] += angle < target[index] ? 15.0D : -15.0D;
        }
    }

    /** Direct port of ModuleMachinePrecAss + TileEntityMachinePrecAss's server contract. */
    private void tickPrecisionAssembler(Level level) {
        List<RecipeHolder<PrecisionAssemblerRecipe>> recipes = availablePrecisionAssemblerRecipes(level);
        this.precisionAssemblerRecipeCount = recipes.size();
        Optional<RecipeHolder<PrecisionAssemblerRecipe>> selected = selectedPrecisionAssemblerRecipe(level);
        if (selected.isEmpty() && this.precisionAssemblerRecipe != null) {
            this.precisionAssemblerRecipe = null;
            this.precisionAssemblerProgress = 0.0D;
        }

        PrecisionAssemblerRecipe recipe = selected.map(RecipeHolder::value).orElse(null);
        this.precisionAssemblerMaxPower = Math.max(100_000L, this.energy);
        if (recipe != null) this.precisionAssemblerMaxPower = Math.max(this.precisionAssemblerMaxPower, recipe.power() * 100L);
        this.energy = BatteryPackItem.dischargeIntoMachine(this.items.get(0), this.energy, this.precisionAssemblerMaxPower);

        boolean wasWorking = this.precisionAssemblerWorking;
        this.precisionAssemblerWorking = false;
        this.precisionAssemblerDemand = 100L;
        if (recipe != null) {
            setupPrecisionAssemblerTanks(recipe);
            double speed = 1.0D + Math.min(precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3) / 3.0D
                    + Math.min(precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3);
            double powerMultiplier = 1.0D - Math.min(precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType.POWER), 3) * 0.25D
                    + Math.min(precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType.SPEED), 3)
                    + Math.min(precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE), 3) * 10.0D / 3.0D;
            this.precisionAssemblerDemand = Math.max(1L, (long) (recipe.power() * powerMultiplier));
            if (canProcessPrecisionAssembler(recipe)) {
                this.energy -= this.precisionAssemblerDemand;
                this.precisionAssemblerProgress += Math.min(speed / recipe.duration(), 1.0D);
                this.precisionAssemblerWorking = true;
                if (this.precisionAssemblerProgress >= 1.0D) {
                    consumePrecisionAssemblerInputs(recipe);
                    producePrecisionAssemblerOutputs(recipe, level.random);
                    this.completed++;
                    if (canProcessPrecisionAssembler(recipe)) this.precisionAssemblerProgress -= 1.0D;
                    else this.precisionAssemblerProgress = 0.0D;
                }
            } else {
                // Keep partial work while automation is still feeding the
                // selected recipe one item at a time. Missing ingredients,
                // output back-pressure, coolant/fluid shortages and brief
                // power gaps pause the cycle; only changing/losing the
                // selected recipe invalidates its process identity.
                this.precisionAssemblerWorking = false;
            }
        } else {
            this.precisionAssemblerProgress = 0.0D;
        }
        this.progress = (int) Math.round(this.precisionAssemblerProgress * 1_000.0D);
        updatePrecisionAssemblerRecipeIndex(recipes);
        if (wasWorking != this.precisionAssemblerWorking) setChangedAndSync();
        else setChanged();
    }

    private void setupPrecisionAssemblerTanks(PrecisionAssemblerRecipe recipe) {
        if (recipe.inputFluids().isEmpty()) {
            // ModuleMachineBase#setupTanks resets a tank whenever the selected
            // legacy recipe has no matching fluid input.
            this.tanks[0].clear();
            this.tanks[0].setCapacity(4_000);
        } else {
            PrecisionAssemblerRecipe.FluidStack input = recipe.inputFluids().getFirst();
            this.tanks[0].conform(input.type(), input.pressure());
            this.tanks[0].setCapacity(Math.max(Math.max(this.tanks[0].amount(), input.amount() * 2), 4_000));
        }
        if (recipe.outputFluids().isEmpty()) {
            this.tanks[1].clear();
            this.tanks[1].setCapacity(4_000);
        } else {
            PrecisionAssemblerRecipe.FluidStack output = recipe.outputFluids().getFirst();
            this.tanks[1].conform(output.type(), output.pressure());
            this.tanks[1].setCapacity(Math.max(Math.max(this.tanks[1].amount(), output.amount() * 2), 4_000));
        }
    }

    private boolean canProcessPrecisionAssembler(PrecisionAssemblerRecipe recipe) {
        if (this.energy < this.precisionAssemblerDemand) return false;
        List<ItemStack> stacks = new ArrayList<>(9);
        for (int slot = 4; slot < 13; slot++) stacks.add(this.items.get(slot));
        if (!recipe.matches(new PrecisionAssemblerRecipe.Input(stacks, List.of(new HbmFluidStack(this.tanks[0].type(), this.tanks[0].amount(), this.tanks[0].pressure()))), this.level)) return false;
        if (recipe.outputMode() == PrecisionAssemblerRecipe.OutputMode.WEIGHTED) {
            ItemStack output = this.items.get(13);
            if (recipe.outputs().size() > 1 && !output.isEmpty()) return false;
            if (recipe.outputs().size() == 1 && !canInsertPrecisionOutput(output, recipe.outputs().getFirst().stack())) return false;
        } else {
            for (int index = 0; index < recipe.outputs().size(); index++) {
                if (!canInsertPrecisionOutput(this.items.get(13 + index), recipe.outputs().get(index).stack())) return false;
            }
        }
        if (!recipe.outputFluids().isEmpty()) {
            PrecisionAssemblerRecipe.FluidStack fluid = recipe.outputFluids().getFirst();
            if (this.tanks[1].type() != fluid.type() || this.tanks[1].pressure() != fluid.pressure()
                    || this.tanks[1].amount() + fluid.amount() > this.tanks[1].capacity()) return false;
        }
        return true;
    }

    private static boolean canInsertPrecisionOutput(ItemStack existing, ItemStack result) {
        return existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize());
    }

    private void consumePrecisionAssemblerInputs(PrecisionAssemblerRecipe recipe) {
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            ItemStack stack = this.items.get(4 + index);
            stack.shrink(recipe.ingredients().get(index).count());
            if (stack.isEmpty()) this.items.set(4 + index, ItemStack.EMPTY);
        }
        if (!recipe.inputFluids().isEmpty()) {
            PrecisionAssemblerRecipe.FluidStack input = recipe.inputFluids().getFirst();
            this.tanks[0].drain(input.type(), input.amount(), false);
        }
    }

    private void producePrecisionAssemblerOutputs(PrecisionAssemblerRecipe recipe, net.minecraft.util.RandomSource random) {
        List<ItemStack> results = recipe.rollOutputs(random);
        for (int index = 0; index < results.size(); index++) {
            ItemStack result = results.get(index);
            if (result.isEmpty()) continue;
            ItemStack output = this.items.get(13 + index);
            if (output.isEmpty()) this.items.set(13 + index, result);
            else output.grow(result.getCount());
        }
        if (!recipe.outputFluids().isEmpty()) {
            PrecisionAssemblerRecipe.FluidStack fluid = recipe.outputFluids().getFirst();
            this.tanks[1].fill(fluid.type(), fluid.amount(), fluid.pressure(), false);
        }
    }

    private int precisionAssemblerUpgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = 2; slot < 4; slot++) if (MachineUpgradeItem.upgradeType(this.items.get(slot)) == type)
            level += Math.max(0, MachineUpgradeItem.upgradeTier(this.items.get(slot)));
        return level;
    }

    private void updatePrecisionAssemblerRecipeIndex(List<RecipeHolder<PrecisionAssemblerRecipe>> recipes) {
        this.precisionAssemblerRecipeIndex = 0;
        if (this.precisionAssemblerRecipe == null) return;
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(this.precisionAssemblerRecipe)) {
                this.precisionAssemblerRecipeIndex = index + 1;
                return;
            }
        }
    }

    /** Direct port of TileEntityForceField's server-side state machine. */
    private void tickForcefield(Level level) {
        int radiusUpgrades = upgradeCount("upgrade_radius");
        int healthUpgrades = upgradeCount("upgrade_health");
        this.forcefieldRadius = HbmConfig.FORCEFIELD_BASE_RADIUS.get()
                + radiusUpgrades * HbmConfig.FORCEFIELD_RADIUS_UPGRADE.get();
        this.forcefieldMaxHealth = 100 + healthUpgrades * HbmConfig.FORCEFIELD_SHIELD_UPGRADE.get();
        this.forcefieldPowerConsumption = HbmConfig.FORCEFIELD_BASE_CONSUMPTION.get()
                + radiusUpgrades * HbmConfig.FORCEFIELD_RADIUS_CONSUMPTION.get()
                + healthUpgrades * HbmConfig.FORCEFIELD_SHIELD_CONSUMPTION.get();
        this.energy = BatteryPackItem.dischargeIntoMachine(this.items.get(0), this.energy, energyCapacity());

        if (this.forcefieldHealth > this.forcefieldMaxHealth) {
            this.forcefieldHealth = this.forcefieldMaxHealth;
        }
        if (this.forcefieldBlink > 0) {
            this.forcefieldBlink--;
            this.forcefieldColor = 0xFF0000;
        } else {
            this.forcefieldColor = 0x00FF00;
        }
        if (this.forcefieldCooldown > 0) {
            this.forcefieldCooldown--;
        } else if (this.forcefieldHealth < this.forcefieldMaxHealth) {
            this.forcefieldHealth += (int) ((this.forcefieldMaxHealth / 100)
                    * HbmConfig.FORCEFIELD_HEALTH_REGEN_MODIFIER.get());
            this.forcefieldHealth = Math.min(this.forcefieldMaxHealth, this.forcefieldHealth);
        }

        if (this.forcefieldOn && this.forcefieldCooldown == 0 && this.forcefieldHealth > 0
                && this.energy >= this.forcefieldPowerConsumption) {
            applyForcefield(level, this.forcefieldRadius);
            this.energy -= this.forcefieldPowerConsumption;
        } else {
            this.forcefieldOutside.clear();
            this.forcefieldInside.clear();
        }
        if (this.energy < this.forcefieldPowerConsumption) {
            this.energy = 0;
        }
        // TileEntityForceField sent its complete state every server tick. The
        // renderer and menu need the same authoritative radius/color/cooldown.
        setChangedAndSync();
    }

    /** Direct port of TileEntityBarrel's identifier, drain-canister and fill-canister processing. */
    private void tickOrbus() {
        HbmFluidTank tank = tank(0);
        if (tank == null) {
            return;
        }

        boolean changed = false;
        ItemStack identifier = this.items.get(0);
        if (identifier.getItem() instanceof FluidIdentifierItem && this.items.get(1).isEmpty()) {
            HbmFluidDefinition type = FluidIdentifierItem.primary(identifier);
            if (type != tank.type()) {
                tank.setType(type);
                this.items.set(1, identifier.copyWithCount(1));
                this.items.set(0, ItemStack.EMPTY);
                changed = true;
            }
        }

        changed |= HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(2),
                tank,
                fluid -> tank.type().isNone() || tank.type() == fluid,
                output -> canPlaceOrbusOutput(3, output),
                output -> placeOrbusOutput(3, output)
        );
        changed |= HbmFluidContainerTransfer.fillFromTank(
                this.items.get(4),
                tank,
                output -> canPlaceOrbusOutput(5, output),
                output -> placeOrbusOutput(5, output)
        );
        if (changed) {
            setChangedAndSync();
        }
    }

    private boolean canPlaceOrbusOutput(int slot, ItemStack stack) {
        ItemStack current = this.items.get(slot);
        return current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
                && current.getCount() + stack.getCount() <= current.getMaxStackSize();
    }

    private void placeOrbusOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
    }

    private int upgradeCount(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", id));
        ItemStack stack = this.items.get(id.equals("upgrade_radius") ? 1 : 2);
        return stack.getItem() == item ? stack.getCount() : 0;
    }

    private void applyForcefield(Level level, int radius) {
        List<Entity> previousOutside = new ArrayList<>(this.forcefieldOutside);
        List<Entity> previousInside = new ArrayList<>(this.forcefieldInside);
        this.forcefieldOutside.clear();
        this.forcefieldInside.clear();
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        AABB scan = new AABB(
                center.x - radius - 25.0D, center.y - radius - 25.0D, center.z - radius - 25.0D,
                center.x + radius + 25.0D, center.y + radius + 25.0D, center.z + radius + 25.0D
        );
        for (Entity entity : level.getEntities((Entity) null, scan, candidate -> !(candidate instanceof Player))) {
            Vec3 position = entity.position();
            boolean outside = position.distanceToSqr(center) > (double) radius * radius;
            if (!previousOutside.contains(entity) && !previousInside.contains(entity)) {
                (outside ? this.forcefieldOutside : this.forcefieldInside).add(entity);
                continue;
            }
            if (previousOutside.contains(entity) && !outside) {
                ejectForcefieldEntity(level, entity, center, radius + 1.0D, true);
                damageForcefield(entity, forcefieldImpact(entity));
                this.forcefieldOutside.add(entity);
            } else if (previousInside.contains(entity) && outside) {
                ejectForcefieldEntity(level, entity, center, Math.max(0.0D, radius - 1.0D), false);
                damageForcefield(entity, forcefieldImpact(entity));
                this.forcefieldInside.add(entity);
            } else {
                (outside ? this.forcefieldOutside : this.forcefieldInside).add(entity);
            }
        }
    }

    private void ejectForcefieldEntity(Level level, Entity entity, Vec3 center, double distance, boolean crossingInwards) {
        // The old implementation builds a vector from the entity to the
        // emitter, places the entity on the permitted side of the sphere,
        // then nudges it by the reflected motion vector.
        Vec3 direction = center.subtract(entity.position());
        if (direction.lengthSqr() < 1.0E-6D) direction = new Vec3(0.0D, 1.0D, 0.0D);
        direction = direction.normalize();
        Vec3 target = center.subtract(direction.scale(distance));
        double speed = getMotionWithFallback(entity);
        Vec3 motion = direction.scale(crossingInwards ? -speed : speed);
        entity.setYRot(0.0F);
        entity.setXRot(0.0F);
        entity.setPos(target.x, target.y, target.z);
        entity.setDeltaMovement(motion);
        entity.setPos(entity.getX() - motion.x, entity.getY() - motion.y, entity.getZ() - motion.z);
        level.playSound(null, entity.blockPosition(), HbmSoundEvents.WEAPON_SPARK_SHOOT.get(),
                SoundSource.BLOCKS, 2.5F, 1.0F);
    }

    private int forcefieldImpact(Entity entity) {
        double mass = entity.getBbHeight() * entity.getBbWidth() * entity.getBbWidth();
        return (int) (mass * getMotionWithFallback(entity) * 50.0D);
    }

    private double getMotionWithFallback(Entity entity) {
        Vec3 current = entity.getDeltaMovement();
        // Keep the legacy fallback, including its original Y reference in the
        // X component, so zero-motion entities use the same impact estimate.
        Vec3 previous = new Vec3(entity.getX() - entity.yo, entity.getY() - entity.yo,
                entity.getZ() - entity.zo);
        double currentSpeed = current.length();
        double previousSpeed = previous.length();
        if (currentSpeed == 0.0D) return previousSpeed;
        if (previousSpeed == 0.0D) return currentSpeed;
        return Math.min(currentSpeed, previousSpeed);
    }

    private void damageForcefield(Entity entity, int amount) {
        if (amount <= 0) return;
        this.forcefieldHealth -= amount;
        if (amount >= Math.max(1, this.forcefieldMaxHealth / 250)) this.forcefieldBlink = 5;
        if (this.forcefieldHealth <= 0) {
            this.forcefieldHealth = 0;
            this.forcefieldCooldown = 100 + (int) (this.forcefieldRadius
                    * HbmConfig.FORCEFIELD_COOLDOWN_MODIFIER.get());
        }
    }

    private void tickTeleporter(Level level) {
        // TileEntityMachineTeleporter sent its complete target/power state every 15 ticks.
        if (level.getGameTime() % 15L == 0L) {
            setChangedAndSync();
        }
        if (this.teleporterTargetY < 0 || this.energy < 1_000_000L) return;
        AABB pad = new AABB(this.worldPosition.getX() + 0.25D, this.worldPosition.getY(), this.worldPosition.getZ() + 0.25D,
                this.worldPosition.getX() + 0.75D, this.worldPosition.getY() + 2.0D, this.worldPosition.getZ() + 0.75D);
        for (Entity entity : level.getEntities((Entity) null, pad, Entity::isAlive)) {
            teleportEntity(level, entity);
        }
    }

    private int lastInputAsInt() {
        return this.lastInput > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) this.lastInput;
    }

    public int teleporterLegacyDimensionId() {
        return switch (this.teleporterTargetDimension) {
            case "minecraft:the_nether" -> -1;
            case "minecraft:the_end" -> 1;
            default -> 0;
        };
    }

    private void setTeleporterDimensionId(int dimensionId) {
        this.teleporterTargetDimension = switch (dimensionId) {
            case -1 -> "minecraft:the_nether";
            case 1 -> "minecraft:the_end";
            default -> "minecraft:overworld";
        };
    }

    @Nullable
    private ServerLevel teleporterTargetLevel(Level source) {
        if (!(source instanceof ServerLevel serverLevel)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(this.teleporterTargetDimension);
        if (id == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return serverLevel.getServer().getLevel(key);
    }

    public void setTeleporterTarget(BlockPos target, ResourceKey<Level> dimension) {
        this.teleporterTargetX = target.getX();
        this.teleporterTargetY = target.getY();
        this.teleporterTargetZ = target.getZ();
        this.teleporterTargetDimension = dimension.location().toString();
        this.setChangedAndSync();
    }

    public boolean hasTeleporterTarget() {
        return this.teleporterTargetY >= 0;
    }

    public int teleporterTargetX() {
        return this.teleporterTargetX;
    }

    public int teleporterTargetY() {
        return this.teleporterTargetY;
    }

    public int teleporterTargetZ() {
        return this.teleporterTargetZ;
    }

    public void toggleForcefield() {
        if (!machineId().equals("machine_forcefield")) {
            return;
        }
        this.forcefieldOn = !this.forcefieldOn;
        this.setChangedAndSync();
    }

    private void teleportEntity(Level source, Entity entity) {
        if (this.energy < 1_000_000L) return;
        ServerLevel destination = teleporterTargetLevel(source);
        if (destination == null) return;
        source.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.5D, this.worldPosition.getZ() + 0.5D,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        double x = this.teleporterTargetX + 0.5D;
        double y = this.teleporterTargetY + 1.5D + entity.getBbHeight() * 0.5D;
        double z = this.teleporterTargetZ + 0.5D;
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(destination, x, y, z, player.getYRot(), player.getXRot());
        } else if (destination == source) {
            entity.teleportTo(x, y, z);
        } else {
            entity.teleportTo(destination, x, y, z, Set.<RelativeMovement>of(), entity.getYRot(), entity.getXRot());
        }
        source.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.energy -= 1_000_000L;
        setChanged();
    }

    /**
     * Port of TileEntityMachineAutosaw: fuel is consumed once a second, the
     * detector sweeps at one degree per tick, and the arm only starts moving
     * after its five-degree cut cone has found a valid target.
     */
    private void tickAutosaw(Level level) {
        boolean changed = false;
        if (level.getGameTime() % 20L == 0L) {
            boolean wasOn = this.autosawOn;
            this.autosawOn = false;
            if (!this.autosawSuspended && this.inputTank != null && this.inputTank.amount() > 0) {
                HbmFluidDefinition fuel = this.inputTank.type();
                if (isAutosawFuel(fuel)) {
                    this.inputTank.drain(fuel, 1, false);
                    this.autosawOn = true;
                }
            }
            changed = wasOn != this.autosawOn;
        }
        if (!this.autosawOn || this.autosawSuspended) {
            if (changed) setChangedAndSync();
            return;
        }

        Vec3 armTip = autosawArmTip();
        damageAutosawEntities(level, armTip);

        if (this.autosawState == 0) {
            this.autosawYaw = (this.autosawYaw + 1.0F) % 360.0F;
            if (this.autosawForceSkip > 0) {
                this.autosawForceSkip--;
            } else if (findAutosawTarget(level) != null) {
                this.autosawState = 1;
            }
        }

        cutAutosawTip(level, armTip);
        if (this.autosawState == 1) {
            this.autosawPitch = Math.min(80.0F, this.autosawPitch + 2.0F);
            if (this.autosawPitch >= 80.0F) {
                this.autosawState = 2;
            }
        } else if (this.autosawState == 2) {
            this.autosawPitch = Math.max(0.0F, this.autosawPitch - 2.0F);
            if (this.autosawPitch <= 0.0F) {
                this.autosawState = 0;
            }
        }
        // TileEntityMachineAutosaw sends its active pose every server tick;
        // the client mirrors the old three-tick yaw/pitch interpolation.
        setChangedAndSync();
    }

    /** Direct port of TileEntityMachineThresher, including its fixed 7x7 cut path. */
    private void tickThresher(Level level) {
        boolean previousOn = this.thresherOn;
        float previousAngle = this.thresherAngle;
        HbmFluidDefinition previousTankType = this.inputTank == null ? null : this.inputTank.type();
        int previousTankAmount = this.inputTank == null ? 0 : this.inputTank.amount();
        if (level.getGameTime() % 20L == 0L && !this.thresherSuspended) {
            if (this.inputTank != null && this.inputTank.amount() > 0 && isThresherFuel(this.inputTank.type())) {
                this.inputTank.drain(this.inputTank.type(), 1, false);
                this.thresherOn = true;
            } else {
                this.thresherOn = false;
            }
        }
        if (!this.thresherOn || this.thresherSuspended) {
            this.thresherSyncAngle = this.thresherAngle;
            syncThresherState(level, previousOn, previousAngle, previousTankType, previousTankAmount);
            return;
        }

        if (this.thresherState == 0) {
            this.thresherDelay--;
            if (this.thresherDelay <= 0) {
                this.thresherState = 1;
            }
        }

        this.thresherPreviousAngle = this.thresherAngle;
        this.thresherAngle = switch (this.thresherState) {
            case 1 -> Math.min(82.5F, this.thresherAngle + 82.5F / 60.0F);
            case 2 -> Math.max(0.0F, this.thresherAngle - 82.5F / 60.0F);
            default -> this.thresherAngle;
        };

        if (this.thresherState == 1 && this.thresherAngle >= 82.5F) {
            this.thresherAngle = 82.5F;
            this.thresherState = 2;
        } else if (this.thresherState == 2 && this.thresherAngle <= 0.0F) {
            this.thresherAngle = 0.0F;
            this.thresherState = 0;
            this.thresherDelay = 200 + level.random.nextInt(100);
        }

        if (this.thresherAngle > 0.0F) {
            Vec3 end = thresherArmTip();
            thresherDamageEntities(level, end);
            Direction side = LegacyMachineGeometry.forgeRotateDown(facing());
            for (int index = -3; index <= 3; index++) {
                BlockPos target = BlockPos.containing(end.x + side.getStepX() * index, this.worldPosition.getY(),
                        end.z + side.getStepZ() * index);
                BlockState targetState = level.getBlockState(target);
                if (targetState.isSolidRender(level, target)) {
                    this.thresherState = 2;
                    break;
                }
                thresherProcessTarget(level, target, targetState);
            }
        }

        this.thresherSyncAngle = this.thresherAngle;
        syncThresherState(level, previousOn, previousAngle, previousTankType, previousTankAmount);
    }

    /** Mirrors TileEntityLoadedBase#networkPackNT(100): changing packets immediately, otherwise every second. */
    private void syncThresherState(Level level, boolean previousOn, float previousAngle,
                                   @Nullable HbmFluidDefinition previousTankType, int previousTankAmount) {
        boolean changed = previousOn != this.thresherOn
                || Float.compare(previousAngle, this.thresherAngle) != 0
                || (this.inputTank != null && (previousTankType != this.inputTank.type()
                || previousTankAmount != this.inputTank.amount()));
        if (changed || level.getGameTime() % 20L == 0L) {
            setChangedAndSync();
        } else {
            setChanged();
        }
    }

    private Vec3 thresherArmTip() {
        Direction forward = facing();
        float angle = (float) Math.toRadians(82.5F - this.thresherAngle);
        Vec3 upper = new Vec3(-forward.getStepX() * 4.0D, 0.0D, -forward.getStepZ() * 4.0D);
        Vec3 lower = upper;
        if (forward.getStepZ() != 0) {
            upper = upper.xRot(angle);
            lower = lower.xRot(-angle);
        }
        if (forward.getStepX() != 0) {
            upper = upper.zRot(angle);
            lower = lower.zRot(-angle);
        }
        Vec3 pivot = new Vec3(this.worldPosition.getX() + 0.5D - forward.getStepX(), this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D - forward.getStepZ());
        Vec3 tip = new Vec3(-forward.getStepX() * 2.0D, 0.0D, -forward.getStepZ() * 2.0D);
        return pivot.add(upper).add(lower).add(tip);
    }

    private void thresherProcessTarget(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.SUNFLOWER)) {
            if (level.random.nextInt(250) == 0) thresherDrop(level, new ItemStack(Blocks.SUNFLOWER), pos);
            return;
        }
        if (state.is(Blocks.TALL_GRASS)) {
            if (level.random.nextInt(100) == 0) thresherDrop(level, new ItemStack(Items.WHEAT_SEEDS), pos);
            return;
        }
        if (state.getBlock() instanceof MustardWillowTallBlock) {
            thresherCutTall(level, pos, state);
            return;
        }
        if (state.getBlock() == Blocks.SUGAR_CANE || state.getBlock() == Blocks.CACTUS) {
            thresherCutCane(level, pos, state.getBlock());
            return;
        }
        if (state.getBlock() instanceof BonemealableBlock growable
                && !growable.isValidBonemealTarget(level, pos, state)) {
            thresherCutCrop(level, pos, state);
        }
    }

    private void thresherCutTall(Level level, BlockPos pos, BlockState state) {
        BlockPos upper = MustardWillowTallBlock.isUpper(state) ? pos : pos.above();
        BlockState upperState = level.getBlockState(upper);
        if (!(upperState.getBlock() instanceof MustardWillowTallBlock)
                || upperState.getValue(MustardWillowTallBlock.META) == MustardWillowTallBlock.CD2 + 8
                || upperState.getValue(MustardWillowTallBlock.META) == MustardWillowTallBlock.CD3 + 8) {
            return;
        }
        level.levelEvent(2001, upper, Block.getId(upperState));
        for (ItemStack drop : Block.getDrops(upperState, (ServerLevel) level, upper, level.getBlockEntity(upper), null, ItemStack.EMPTY)) {
            thresherDrop(level, drop, upper);
        }
        level.setBlock(upper, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private void thresherCutCane(Level level, BlockPos pos, Block target) {
        int offset = level.getBlockState(pos.below()).is(target) ? -1 : 0;
        for (int index = 2 + offset; index > 0 + offset; index--) {
            BlockPos part = pos.above(index);
            BlockState state = level.getBlockState(part);
            if (!state.is(target)) continue;
            level.levelEvent(2001, part, Block.getId(state));
            for (ItemStack drop : Block.getDrops(state, (ServerLevel) level, part, level.getBlockEntity(part), null, ItemStack.EMPTY)) {
                thresherDrop(level, drop, part);
            }
            level.setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void thresherCutCrop(Level level, BlockPos pos, BlockState state) {
        boolean replanted = false;
        BlockState replacement = Blocks.AIR.defaultBlockState();
        level.levelEvent(2001, pos, Block.getId(state));
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos), null, ItemStack.EMPTY));
        for (ItemStack drop : drops) {
            if (!replanted && drop.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                BlockState candidate = blockItem.getBlock().defaultBlockState();
                if (candidate.canSurvive(level, pos)) {
                    replacement = candidate;
                    drop.shrink(1);
                    replanted = true;
                }
            }
            thresherDrop(level, drop, pos);
        }
        if (!replanted && state.is(Blocks.WHEAT)) {
            replacement = Blocks.WHEAT.defaultBlockState();
        }
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private void thresherDamageEntities(Level level, Vec3 end) {
        Direction forward = facing();
        Direction side = LegacyMachineGeometry.forgeRotateDown(forward);
        AABB area = new AABB(end.x, this.worldPosition.getY() + 0.5D, end.z, end.x, this.worldPosition.getY() + 0.5D, end.z)
                .inflate(Math.abs(forward.getStepX() * 0.5D) + Math.abs(side.getStepX() * 4.5D), 0.5D,
                        Math.abs(forward.getStepZ() * 0.5D) + Math.abs(side.getStepZ() * 4.5D));
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!entity.isAlive() || !entity.hurt(level.damageSources().source(HbmDamageTypes.TURBOFAN), 100.0F)) continue;
            if (entity instanceof net.minecraft.world.entity.monster.Monster && !entity.isAlive()) {
                thresherDrop(level, legacyItem("nitra_small", 1), entity.blockPosition());
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F,
                    0.95F + level.random.nextFloat() * 0.2F);
            if (level instanceof ServerLevel serverLevel) {
                int count = Math.min((int) Math.ceil(entity.getMaxHealth() / 4.0F), 250) * 4;
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), count, 0.0D, 0.0D, 0.0D, 0.1D);
            }
        }
    }

    private void thresherDrop(Level level, ItemStack stack, BlockPos pos) {
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel)) return;
        Direction forward = facing();
        ItemEntity item = new ItemEntity(serverLevel, this.worldPosition.getX() + 0.5D - forward.getStepX() * 0.75D,
                this.worldPosition.getY(), this.worldPosition.getZ() + 0.5D - forward.getStepZ() * 0.75D, stack.copy());
        item.setPickUpDelay(10);
        item.setDeltaMovement(-forward.getStepX() * 0.2D + 0.2D, 0.0D, -forward.getStepZ() * 0.2D);
        serverLevel.addFreshEntity(item);
    }

    private static boolean isThresherFuel(HbmFluidDefinition fluid) {
        return fluid != null && switch (fluid.name()) {
            case "woodoil", "ethanol", "fishoil", "heavyoil", "coalcreosote" -> true;
            default -> false;
        };
    }

    private static float thresherYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    private Vec3 autosawArmTip() {
        double angle = Math.toRadians(80.0F - this.autosawPitch);
        double yaw = -Math.toRadians(this.autosawYaw);
        Vec3 upper = new Vec3(0.0D, 0.0D, -4.0D).xRot((float) angle).yRot((float) yaw);
        Vec3 lower = new Vec3(0.0D, 0.0D, -4.0D).xRot((float) -angle).yRot((float) yaw);
        Vec3 tip = new Vec3(0.0D, 0.0D, -2.0D).yRot((float) yaw);
        return new Vec3(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.75D, this.worldPosition.getZ() + 0.5D)
                .add(upper).add(lower).add(tip);
    }

    private void damageAutosawEntities(Level level, Vec3 armTip) {
        AABB area = new AABB(armTip.x - 1.0D, armTip.y - 0.25D, armTip.z - 1.0D,
                armTip.x + 1.0D, armTip.y + 0.25D, armTip.z + 1.0D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.isAlive() && entity.hurt(level.damageSources().generic(), 100.0F)) {
                level.playSound(null, entity.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS, 2.0F, 0.95F + level.random.nextFloat() * 0.2F);
            }
        }
    }

    private void cutAutosawTip(Level level, Vec3 armTip) {
        int y = (int) Math.floor(armTip.y);
        int minX = (int) Math.floor(armTip.x - 0.5D);
        int maxX = (int) Math.floor(armTip.x + 0.5D);
        int minZ = (int) Math.floor(armTip.z - 0.5D);
        int maxZ = (int) Math.floor(armTip.z + 0.5D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (isAutosawCrop(state)) {
                    level.destroyBlock(pos, true);
                } else if (state.is(BlockTags.LOGS)) {
                    fellAutosawTree(level, pos);
                    if (this.autosawState == 1) {
                        this.autosawState = 2;
                    }
                }
                if (this.autosawState == 1 && level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) {
                    this.autosawState = 2;
                    this.autosawForceSkip = 5;
                }
            }
        }
    }

    private static boolean isAutosawFuel(HbmFluidDefinition fluid) {
        return fluid != null && switch (fluid.name()) {
            case "woodoil", "ethanol", "fishoil", "heavyoil", "coalcreosote" -> true;
            default -> false;
        };
    }

    /** TileEntityConveyorPress operates on one item entity above its 1x1 belt. */
    private void tickConveyorPress(Level level) {
        if (this.conveyorDelay > 0) {
            this.conveyorDelay--;
            return;
        }
        if (this.conveyorRetracting) {
            if (this.energy < 100L) return;
            this.conveyorPress = Math.max(0.0D, this.conveyorPress - 0.125D);
            this.energy -= 100L;
            if (this.conveyorPress <= 0.0D) {
                this.conveyorRetracting = false;
            }
            setChanged();
            return;
        }

        ItemEntity input = conveyorInput(level);
        if (input == null || this.energy < 100L) return;
        this.conveyorPress = Math.min(1.0D, this.conveyorPress + 0.125D);
        this.energy -= 100L;
        if (this.conveyorPress >= 1.0D) {
            this.conveyorRetracting = true;
            this.conveyorDelay = 5;
            processConveyorInput(level, input);
        }
        setChanged();
    }

    /**
     * Direct port of TileEntityMachinePyroOven's processing path. The oven
     * uses a configured input tank, a single output tank and the inherited
     * three 50 mB pollution buffers at the dedicated upper exhaust.
     */
    private void tickPyroOven(Level level) {
        boolean changed = false;
        LegacyMachineProfile profile = profile();

        long charged = BatteryPackItem.dischargeIntoMachine(this.items.get(0), this.energy, profile.energyCapacity());
        if (charged != this.energy) {
            this.energy = charged;
            changed = true;
        }
        changed |= configurePyroOvenInput();

        // The original sends the process fluid through all five side ports.
        changed |= sendPyroOutput(level);
        boolean wasVenting = this.pyroVenting;
        this.pyroVenting = sendPyroSmoke(level);
        changed |= wasVenting != this.pyroVenting;

        int speed = pyroUpgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerSaving = pyroUpgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        int overdrive = pyroUpgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
        long consumption = pyroConsumption(speed + overdrive * 2, powerSaving);
        // The old tile checks this pre-overdrive requirement, then subtracts
        // the full overdrive draw after the process advances.
        long minimumConsumption = pyroConsumption(speed, powerSaving);
        Optional<PyroOvenRecipe> holder = pyroRecipe(level);
        boolean wasProgressing = this.pyroProgressing;
        this.pyroProgressing = false;

        if (holder.isPresent() && canProcessPyro(holder.get(), minimumConsumption)) {
            PyroOvenRecipe recipe = holder.get();
            int duration = Math.max(1, (recipe.duration() - speed * (recipe.duration() / 4)) / (overdrive * 2 + 1));
            this.pyroDuration = duration;
            this.progress++;
            this.energy -= consumption;
            this.pyroProgressing = true;
            this.pyroAnimation++;
            HbmPollution.bufferedLegacyPollute(
                    level,
                    this.worldPosition,
                    HbmPollutionType.SOOT,
                    HbmPollutionConstants.SOOT_PER_SECOND,
                    this::pyroSmokeTank
            );
            if (this.progress >= duration) {
                this.progress = 0;
                finishPyroRecipe(recipe);
                this.completed++;
            }
            changed = true;
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }
        if (wasProgressing != this.pyroProgressing) {
            changed = true;
        }
        if (changed) {
            setChangedAndSync();
        }
    }

    /** Direct port of TileEntityMachineTurbofan#updateEntity. */
    private void tickTurbofan(Level level) {
        HbmFluidTank fuel = tank(0);
        HbmFluidTank blood = tank(1);
        if (fuel == null || blood == null) {
            return;
        }

        boolean changed = false;
        changed |= configureTurbofanFuel();
        changed |= drainTurbofanContainer();
        HbmFluidDefinition bloodFluid = HbmFluids.byName("blood").orElse(HbmFluids.none());
        if (blood.type() != bloodFluid && blood.amount() == 0) {
            blood.setType(bloodFluid);
            changed = true;
        }

        int previousAfterburner = this.turbofanAfterburner;
        this.turbofanAfterburner = turbofanAfterburnerLevel();
        if (this.turbofanAfterburner != previousAfterburner) {
            changed = true;
        }

        boolean redstone = false;
        for (BlockPos port : servicePorts()) {
            if (level.hasNeighborSignal(port)) {
                redstone = true;
                break;
            }
        }

        HbmFluidDefinition burnedFuel = fuel.type();
        long burnValue = burnedFuel.combustibleFuelGrade() == CombustibleFuelGrade.AERO
                ? burnedFuel.combustibleHeatEnergy() / 1_000L : 0L;
        int amount = 1 + this.turbofanAfterburner;
        int amountToBurn = Math.min(amount, fuel.amount());
        boolean previousWasOn = this.turbofanWasOn;
        this.turbofanWasOn = false;
        this.turbofanOutput = 0;
        this.turbofanConsumption = 0;

        if (!redstone && amountToBurn > 0) {
            // The old machine consumed whatever its identifier subscribed to;
            // only AERO-grade combustible fluid creates output or a jet.
            this.turbofanWasOn = true;
            fuel.drain(burnedFuel, amountToBurn, false);
            // HBM 1.7.10 FluidTank#setFill(0) retained its configured type.
            // The modern shared tank clears its type when drained, which would
            // otherwise make the turbofan stop subscribing to its fuel pipe.
            if (fuel.amount() == 0 && !burnedFuel.isNone()) {
                fuel.setType(burnedFuel);
            }
            this.turbofanOutput = (int) Math.min(Integer.MAX_VALUE,
                    burnValue * amountToBurn * (1.0D + Math.min(this.turbofanAfterburner / 3.0D, 4.0D)));
            this.energy = Math.min(profile().energyCapacity(), this.energy + this.turbofanOutput);
            this.turbofanConsumption = amountToBurn;
            if (level.getGameTime() % 20L == 0L) {
                HbmPollution.bufferedLegacyPolluteFluid(level, this.worldPosition, burnedFuel,
                        HbmPollution.ReleaseType.BURN, amountToBurn * 5.0D, this::turbofanSmokeTank);
            }
            changed = true;
        }

        changed |= pullTurbofanFuel(level);
        long afterCharge = BatteryPackItem.chargeFromMachine(this.items.get(3), this.energy);
        if (afterCharge != this.energy) {
            this.energy = afterCharge;
            changed = true;
        }
        changed |= sendTurbofanOutputs(level);

        // The legacy burn value is assigned inside the non-redstone branch, so
        // an externally powered shutdown never emits a jet or damages entities.
        if (this.turbofanWasOn && burnValue > 0L && amountToBurn > 0) {
            turbofanJetEffects(level);
        }
        if (previousWasOn != this.turbofanWasOn) {
            changed = true;
        }
        if (changed) {
            setChangedAndSync();
        }
    }

    private boolean configureTurbofanFuel() {
        HbmFluidTank fuel = tank(0);
        ItemStack identifier = this.items.get(4);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            // The 1.7.10 constructor and NBT tank state leave an unconfigured
            // turbofan set to kerosene even while empty. HbmFluidTank clears to
            // NONE at zero volume, including after an empty tank is reloaded.
            if (fuel != null && fuel.amount() == 0 && fuel.type().isNone()) {
                fuel.setType(HbmFluids.byName("kerosene").orElse(HbmFluids.none()));
                return true;
            }
            return false;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (selected.isNone() || fuel == null || fuel.type() == selected) {
            return false;
        }
        fuel.setType(selected);
        return true;
    }

    private boolean drainTurbofanContainer() {
        HbmFluidTank fuel = tank(0);
        if (fuel == null) {
            return false;
        }
        ItemStack input = this.items.get(0);
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                input,
                fuel,
                fluid -> fuel.type().isNone() || fuel.type() == fluid,
                output -> canInsertTurbofanContainerOutput(output),
                this::insertTurbofanContainerOutput
        );
        if (input.isEmpty()) {
            this.items.set(0, ItemStack.EMPTY);
        }
        return changed;
    }

    private boolean canInsertTurbofanContainerOutput(ItemStack output) {
        return output.isEmpty() || this.items.get(1).isEmpty()
                || ItemStack.isSameItemSameComponents(this.items.get(1), output)
                && this.items.get(1).getCount() + output.getCount() <= this.items.get(1).getMaxStackSize();
    }

    private void insertTurbofanContainerOutput(ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        if (this.items.get(1).isEmpty()) {
            this.items.set(1, output.copy());
        } else {
            this.items.get(1).grow(output.getCount());
        }
    }

    private int turbofanAfterburnerLevel() {
        ItemStack upgrade = this.items.get(2);
        if (isLegacyItem(upgrade, "flame_pony")) {
            return 100;
        }
        return MachineUpgradeItem.upgradeType(upgrade) == MachineUpgradeItem.UpgradeType.AFTERBURN
                ? Math.max(0, MachineUpgradeItem.upgradeTier(upgrade)) : 0;
    }

    private boolean pullTurbofanFuel(Level level) {
        HbmFluidTank fuel = tank(0);
        if (fuel == null || fuel.type().isNone() || fuel.amount() >= fuel.capacity()) {
            return false;
        }
        boolean changed = false;
        Direction nozzle = LegacyMachineGeometry.forgeRotateUp(facing());
        Direction side = LegacyMachineGeometry.forgeRotateDown(nozzle);
        List<BlockPos> ports = servicePorts();
        for (int index = 0; index < ports.size() && fuel.amount() < fuel.capacity(); index++) {
            Direction portFace = index < 2 ? side : side.getOpposite();
            int requested = fuel.capacity() - fuel.amount();
            FluidStack drained = HbmFluidNetworks.drainFrom(level, ports.get(index), portFace, fuel.type(), requested,
                    this.worldPosition, true);
            if (drained.isEmpty()) {
                continue;
            }
            int inserted = fuel.fill(fuel.type(), drained.getAmount(), false);
            if (inserted > 0) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean sendTurbofanOutputs(Level level) {
        HbmFluidTank blood = tank(1);
        if (blood == null) {
            return false;
        }
        boolean changed = false;
        Direction nozzle = LegacyMachineGeometry.forgeRotateUp(facing());
        Direction side = LegacyMachineGeometry.forgeRotateDown(nozzle);
        List<BlockPos> ports = servicePorts();
        for (int index = 0; index < ports.size(); index++) {
            Direction portFace = index < 2 ? side : side.getOpposite();
            BlockPos port = ports.get(index);
            if (blood.amount() > 0 && !blood.type().isNone()) {
                FluidStack stack = HbmFluids.toNeoStack(blood.type(), blood.amount());
                int accepted = HbmFluidNetworks.fillInto(level, port, portFace, stack, this.worldPosition, true);
                if (accepted > 0) {
                    blood.drain(blood.type(), accepted, false);
                    changed = true;
                }
            }
            changed |= HbmPollution.sendSmoke(level, this.worldPosition, port, portFace,
                    turbofanSmokeTank(HbmPollutionType.SOOT),
                    turbofanSmokeTank(HbmPollutionType.HEAVYMETAL),
                    turbofanSmokeTank(HbmPollutionType.POISON));
        }
        return changed;
    }

    @Nullable
    private HbmFluidTank turbofanSmokeTank(HbmPollutionType type) {
        return switch (type) {
            case SOOT, FALLOUT -> tank(2);
            case HEAVYMETAL -> tank(3);
            case POISON -> tank(4);
        };
    }

    private void turbofanJetEffects(Level level) {
        Direction nozzle = LegacyMachineGeometry.forgeRotateUp(facing());
        Direction side = LegacyMachineGeometry.forgeRotateUp(nozzle);
        if (this.turbofanAfterburner > 0) {
            for (int index = 0; index < 2; index++) {
                double speed = 2.0D + level.random.nextDouble() * 3.0D;
                double deviation = level.random.nextGaussian() * 0.2D;
                level.addParticle(HbmParticleTypes.GAS_FLARE_FLAME.get(),
                        this.worldPosition.getX() + 0.5D - nozzle.getStepX() * (3 - index), this.worldPosition.getY() + 1.5D,
                        this.worldPosition.getZ() + 0.5D - nozzle.getStepZ() * (3 - index),
                        -nozzle.getStepX() * speed + deviation, 0.0D, -nozzle.getStepZ() * speed + deviation);
            }
            if (this.turbofanAfterburner > 90 && level.random.nextInt(30) == 0) {
                level.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.5D,
                        this.worldPosition.getZ() + 0.5D, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                        3.0F, 0.95F + level.random.nextFloat() * 0.2F);
            }
        }

        AABB exhaust = turbofanAabb(nozzle, side, -3.5D, -19.5D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, exhaust)) {
            if (this.turbofanAfterburner > 0) {
                entity.igniteForSeconds(5.0F);
                entity.hurt(level.damageSources().onFire(), 5.0F);
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(-nozzle.getStepX() * 0.2D, 0.0D, -nozzle.getStepZ() * 0.2D));
        }

        AABB intake = turbofanAabb(nozzle, side, 3.5D, 8.5D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, intake)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(-nozzle.getStepX() * 0.2D, 0.0D, -nozzle.getStepZ() * 0.2D));
        }

        AABB blades = turbofanAabb(nozzle, side, 3.5D, 3.75D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, blades)) {
            boolean wasAlive = entity.isAlive();
            entity.hurt(level.damageSources().source(HbmDamageTypes.TURBOFAN), 1_000.0F);
            if (wasAlive && !entity.isAlive() && entity instanceof LivingEntity) {
                HbmFluidTank blood = tank(1);
                if (blood != null) {
                    blood.fill(HbmFluids.byName("blood").orElse(HbmFluids.none()), 50, false);
                    this.turbofanShowBlood = true;
                }
                TurretCasingEffects.spawnMaxwellGib(level, (LivingEntity) entity, false);
            }
        }
    }

    private AABB turbofanAabb(Direction forward, Direction side, double first, double second) {
        Vec3 origin = Vec3.atCenterOf(this.worldPosition);
        Vec3 a = origin.add(forward.getStepX() * first - side.getStepX() * 1.5D, 0.0D,
                forward.getStepZ() * first - side.getStepZ() * 1.5D);
        Vec3 b = origin.add(forward.getStepX() * second + side.getStepX() * 1.5D, 3.0D,
                forward.getStepZ() * second + side.getStepZ() * 1.5D);
        return new AABB(Math.min(a.x, b.x), this.worldPosition.getY(), Math.min(a.z, b.z),
                Math.max(a.x, b.x), this.worldPosition.getY() + 3.0D, Math.max(a.z, b.z));
    }

    private boolean configurePyroOvenInput() {
        ItemStack identifier = this.items.get(3);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (selected.isNone() || this.tanks[0].amount() > 0 || this.tanks[0].type() == selected) {
            return false;
        }
        this.tanks[0].setType(selected);
        return true;
    }

    private Optional<PyroOvenRecipe> pyroRecipe(Level level) {
        PyroOvenRecipe.Input input = new PyroOvenRecipe.Input(
                this.items.get(1), this.tanks[0].type(), this.tanks[0].amount());
        Optional<PyroOvenRecipe> staticRecipe = level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PYRO_OVEN.get()).stream()
                .filter(recipe -> recipe.value().matches(input, level))
                .map(RecipeHolder::value)
                .findFirst();
        if (staticRecipe.isPresent()) {
            return staticRecipe;
        }
        Optional<PyroOvenRecipe> bedrockRecipe = pyroBedrockOreRecipe();
        return bedrockRecipe.isPresent() ? bedrockRecipe : pyroSolidFuelRecipe();
    }

    /** Direct runtime equivalent of PyroOvenRecipes' BedrockOreType loop. */
    private Optional<PyroOvenRecipe> pyroBedrockOreRecipe() {
        ItemStack input = this.items.get(1);
        if (!(input.getItem() instanceof BedrockOreItem)) {
            return Optional.empty();
        }
        BedrockOreItem.Grade roastedGrade = switch (BedrockOreItem.gradeOf(input)) {
            case BASE -> BedrockOreItem.Grade.BASE_ROASTED;
            case PRIMARY -> BedrockOreItem.Grade.PRIMARY_ROASTED;
            case SULFURIC_BYPRODUCT -> BedrockOreItem.Grade.SULFURIC_ROASTED;
            case SOLVENT_BYPRODUCT -> BedrockOreItem.Grade.SOLVENT_ROASTED;
            case RAD_BYPRODUCT -> BedrockOreItem.Grade.RAD_ROASTED;
            default -> null;
        };
        if (roastedGrade == null) {
            return Optional.empty();
        }
        ItemStack output = BedrockOreItem.stackFor(
                HbmItems.BEDROCK_ORE_NEW,
                roastedGrade,
                BedrockOreItem.typeOf(input)
        );
        return Optional.of(new PyroOvenRecipe(
                "pyrolysis.bedrock_ore",
                new PyroOvenRecipe.ItemInput(Ingredient.of(input.getItem()), 1),
                PyroOvenRecipe.FluidInput.EMPTY,
                output,
                new PyroOvenRecipe.FluidOutput(HbmFluids.byName("vitriol").orElse(HbmFluids.none()), 50),
                10
        ));
    }

    /**
     * PyroOvenRecipes#registerSFAuto is generated from every flammable fluid
     * at runtime in 1.7.10. Keep it runtime-derived here as well so fluid
     * heat values remain authoritative and no handwritten fuel list drifts.
     */
    private Optional<PyroOvenRecipe> pyroSolidFuelRecipe() {
        if (!this.items.get(1).isEmpty() || this.tanks[0].amount() <= 0) {
            return Optional.empty();
        }
        HbmFluidDefinition fluid = this.tanks[0].type();
        long heatPerBucket = fluid.flammableHeatEnergy();
        if (fluid.isNone() || heatPerBucket <= 0L) {
            return Optional.empty();
        }
        boolean balefire = fluid.name().equals("balefire");
        long targetHeat = balefire ? 24_000_000L : 1_440_000L;
        long rawAmount = targetHeat * 1_000L / 2L / heatPerBucket;
        int amount = rawAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawAmount;
        if (amount > 10_000) amount -= amount % 1_000;
        else if (amount > 1_000) amount -= amount % 100;
        else if (amount > 100) amount -= amount % 10;
        amount = Math.max(1, amount);
        ItemStack output = new ItemStack(balefire ? HbmItems.SOLID_FUEL_BF.get() : HbmItems.SOLID_FUEL.get());
        return Optional.of(new PyroOvenRecipe(
                "pyrolysis.solid_fuel",
                PyroOvenRecipe.ItemInput.EMPTY,
                new PyroOvenRecipe.FluidInput(fluid, amount),
                output,
                PyroOvenRecipe.FluidOutput.EMPTY,
                60
        ));
    }

    private boolean canProcessPyro(PyroOvenRecipe recipe, long consumption) {
        if (this.energy < consumption) {
            return false;
        }
        if (!recipe.itemInput().isEmpty() && !recipe.itemInput().matches(this.items.get(1))) {
            return false;
        }
        if (recipe.itemInput().isEmpty() && !this.items.get(1).isEmpty()) {
            return false;
        }
        if (!recipe.fluidInput().isEmpty()
                && !recipe.fluidInput().matches(this.tanks[0].type(), this.tanks[0].amount())) {
            return false;
        }
        if (!recipe.itemOutput().isEmpty() && !hasExactOutputSpace(recipe.itemOutput(), 2)) {
            return false;
        }
        return recipe.fluidOutput().isEmpty() || (this.tanks[1].amount() == 0
                || this.tanks[1].type() == recipe.fluidOutput().fluid())
                && this.tanks[1].amount() + recipe.fluidOutput().amount() <= this.tanks[1].capacity();
    }

    private void finishPyroRecipe(PyroOvenRecipe recipe) {
        if (!recipe.itemOutput().isEmpty()) {
            ItemStack output = recipe.group().equals("pyrolysis.tar_soot")
                    ? HbmItems.variantStack(HbmItems.POWDER_ASH_ITEMS, "soot")
                    : recipe.itemOutput().copy();
            insertExactOutput(output, 2);
        }
        if (!recipe.fluidOutput().isEmpty()) {
            this.tanks[1].fill(recipe.fluidOutput().fluid(), recipe.fluidOutput().amount(), false);
        }
        if (!recipe.itemInput().isEmpty()) {
            this.items.get(1).shrink(recipe.itemInput().count());
            if (this.items.get(1).isEmpty()) {
                this.items.set(1, ItemStack.EMPTY);
            }
        }
        if (!recipe.fluidInput().isEmpty()) {
            this.tanks[0].drain(recipe.fluidInput().fluid(), recipe.fluidInput().amount(), false);
        }
    }

    private int pyroUpgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = 4; slot <= 5; slot++) {
            ItemStack stack = this.items.get(slot);
            if (MachineUpgradeItem.isMachineUpgrade(stack) && MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return Math.min(3, level);
    }

    private static long pyroConsumption(int speedAndOverdrive, int powerSaving) {
        long multiplier = speedAndOverdrive + 1L;
        return 10_000L * multiplier * multiplier / (powerSaving + 1L);
    }

    private boolean sendPyroOutput(Level level) {
        if (this.tanks[1].amount() <= 0 || this.tanks[1].type().isNone()) {
            return false;
        }
        boolean changed = false;
        Direction portFace = LegacyMachineGeometry.forgeRotateDown(facing());
        for (BlockPos port : servicePorts()) {
            if (this.tanks[1].amount() <= 0) {
                break;
            }
            FluidStack output = HbmFluids.toNeoStack(this.tanks[1].type(), this.tanks[1].amount());
            int accepted = HbmFluidNetworks.fillInto(level, port, portFace, output, this.worldPosition, true);
            if (accepted > 0) {
                this.tanks[1].drain(this.tanks[1].type(), accepted, false);
                changed = true;
            }
        }
        return changed;
    }

    private boolean sendPyroSmoke(Level level) {
        Direction side = LegacyMachineGeometry.forgeRotateDown(facing());
        BlockPos exhaust = offset(Direction.NORTH, 0, side, -1, 3);
        return HbmPollution.sendSmoke(level, this.worldPosition, exhaust, Direction.UP,
                pyroSmokeTank(HbmPollutionType.SOOT),
                pyroSmokeTank(HbmPollutionType.HEAVYMETAL),
                pyroSmokeTank(HbmPollutionType.POISON));
    }

    @Nullable
    private HbmFluidTank pyroSmokeTank(HbmPollutionType type) {
        return switch (type) {
            case SOOT, FALLOUT -> tank(2);
            case HEAVYMETAL -> tank(3);
            case POISON -> tank(4);
        };
    }

    @Nullable
    private ItemEntity conveyorInput(Level level) {
        if (this.items.isEmpty() || !StampItem.isStamp(this.items.get(0))) return null;
        AABB belt = new AABB(this.worldPosition.getX(), this.worldPosition.getY() + 1.0D, this.worldPosition.getZ(),
                this.worldPosition.getX() + 1.0D, this.worldPosition.getY() + 1.5D, this.worldPosition.getZ() + 1.0D);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, belt, ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            if (stack.getCount() != 1 || conveyorRecipe(level, stack).isEmpty()) continue;
            if (entity.getX() > this.worldPosition.getX() + 0.35D && entity.getX() < this.worldPosition.getX() + 0.65D
                    && entity.getZ() > this.worldPosition.getZ() + 0.35D && entity.getZ() < this.worldPosition.getZ() + 0.65D) {
                entity.setPos(this.worldPosition.getX() + 0.5D, entity.getY(), this.worldPosition.getZ() + 0.5D);
            }
            return entity;
        }
        return null;
    }

    private Optional<RecipeHolder<PressRecipe>> conveyorRecipe(Level level, ItemStack input) {
        PressRecipe.Input recipeInput = new PressRecipe.Input(input, this.items.get(0));
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get()).stream()
                .filter(recipe -> recipe.value().matches(recipeInput, level))
                .findFirst();
    }

    private void processConveyorInput(Level level, ItemEntity entity) {
        Optional<RecipeHolder<PressRecipe>> holder = conveyorRecipe(level, entity.getItem());
        if (holder.isEmpty() || entity.getItem().getCount() != 1) return;
        ItemStack output = holder.get().value().assemble(
                new PressRecipe.Input(entity.getItem(), this.items.get(0)), level.registryAccess());
        if (output.isEmpty()) return;
        ItemEntity result = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), output.copy());
        entity.discard();
        level.addFreshEntity(result);
        StampItem.damageStamp(this.items.get(0));
        level.playSound(null, this.worldPosition, HbmSoundEvents.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        this.completed++;
    }

    private BlockPos findAutosawTarget(Level level) {
        double scanAngle = Math.toRadians((this.autosawYaw + 270.0D) % 360.0D);
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared <= 4 || distanceSquared > 81) continue;
                double angle = Math.atan2(dz, dx);
                double relative = Math.abs((angle - scanAngle + Math.PI) % (Math.PI * 2.0D) - Math.PI);
                if (relative > Math.toRadians(5.0D)) continue;
                BlockPos pos = this.worldPosition.offset(dx, 1, dz);
                if (isAutosawCuttable(level.getBlockState(pos))) return pos;
            }
        }
        return null;
    }

    private static boolean isAutosawCuttable(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS);
    }

    private static boolean isAutosawCrop(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS);
    }

    /** The old machine uses 18-connectivity and assigns nearby trees to their trunk column. */
    private void fellAutosawTree(Level level, BlockPos hit) {
        final int minY = Math.max(level.getMinBuildHeight(), this.worldPosition.getY() + 1 - 16);
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, this.worldPosition.getY() + 1 + 32);
        final int maxRadius = 19;
        Map<BlockPos, BlockPos> trunks = new HashMap<>();
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (dx * dx + dz * dz > 81) continue;
                BlockPos probe = this.worldPosition.offset(dx, 1, dz);
                if (!level.getBlockState(probe).is(BlockTags.LOGS)) continue;
                BlockPos base = probe;
                while (base.getY() > minY && level.getBlockState(base.below()).is(BlockTags.LOGS)) {
                    base = base.below();
                }
                if (canSupportSapling(level, base.below())) {
                    trunks.put(new BlockPos(probe.getX(), -1, probe.getZ()), base);
                }
            }
        }
        BlockPos hitColumn = new BlockPos(hit.getX(), -1, hit.getZ());
        trunks.putIfAbsent(hitColumn, findAutosawTrunkBase(level, hit, minY));

        Map<BlockPos, BlockPos> owner = new HashMap<>();
        ArrayDeque<AutosawSearchNode> queue = new ArrayDeque<>();
        for (Map.Entry<BlockPos, BlockPos> trunk : trunks.entrySet()) {
            queue.addFirst(new AutosawSearchNode(trunk.getValue(), trunk.getKey()));
        }
        while (!queue.isEmpty()) {
            AutosawSearchNode node = queue.removeFirst();
            if (owner.putIfAbsent(node.position(), node.column()) != null) continue;
            for (int[] direction : AUTOSAW_EIGHTEEN_DIRECTIONS) {
                BlockPos next = node.position().offset(direction[0], direction[1], direction[2]);
                int dx = next.getX() - this.worldPosition.getX();
                int dz = next.getZ() - this.worldPosition.getZ();
                if (next.getY() < minY || next.getY() > maxY || dx * dx + dz * dz > maxRadius * maxRadius
                        || owner.containsKey(next)) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) continue;
                AutosawSearchNode nextNode = new AutosawSearchNode(next, node.column());
                if (direction[0] == 0 && direction[2] == 0) queue.addFirst(nextNode);
                else queue.addLast(nextNode);
            }
        }

        for (Map.Entry<BlockPos, BlockPos> entry : owner.entrySet()) {
            if (!hitColumn.equals(entry.getValue())) continue;
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) && isWithinAutosawWorkingArea(pos) && canSupportSapling(level, pos.below())) {
                level.destroyBlock(pos, true);
                level.setBlock(pos, saplingForLog(state), Block.UPDATE_ALL);
            } else {
                level.destroyBlock(pos, true);
            }
        }
    }

    private BlockPos findAutosawTrunkBase(Level level, BlockPos hit, int minY) {
        BlockPos base = hit;
        while (base.getY() > minY && level.getBlockState(base.below()).is(BlockTags.LOGS)) {
            base = base.below();
        }
        return base;
    }

    private boolean isWithinAutosawWorkingArea(BlockPos pos) {
        int dx = pos.getX() - this.worldPosition.getX();
        int dz = pos.getZ() - this.worldPosition.getZ();
        int distanceSquared = dx * dx + dz * dz;
        return distanceSquared > 4 && distanceSquared <= 81;
    }

    private static boolean canSupportSapling(Level level, BlockPos pos) {
        return level.getBlockState(pos).canSustainPlant(level, pos, Direction.UP, Blocks.OAK_SAPLING.defaultBlockState()).isTrue();
    }

    private static BlockState saplingForLog(BlockState log) {
        Block block = log.getBlock();
        if (block == Blocks.SPRUCE_LOG) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (block == Blocks.BIRCH_LOG) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (block == Blocks.JUNGLE_LOG) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (block == Blocks.ACACIA_LOG) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (block == Blocks.DARK_OAK_LOG) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        if (block == Blocks.CHERRY_LOG) return Blocks.CHERRY_SAPLING.defaultBlockState();
        return Blocks.OAK_SAPLING.defaultBlockState();
    }

    private record AutosawSearchNode(BlockPos position, BlockPos column) {
    }

    private static final int[][] AUTOSAW_EIGHTEEN_DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}
    };

    /** Exact 1.7.10 TileEntitySawmill cycle, including its heat-source pull. */
    private void tickSawmill(Level level) {
        if (!this.sawmillHasBlade) {
            this.progress = 0;
            this.sawmillHeat = 0;
            this.sawmillRotationSpeed = 0.0F;
            this.sawmillWarnCooldown = 0;
            this.sawmillOverspeed = 0;
            return;
        }

        BlockEntity below = level.getBlockEntity(this.worldPosition.below());
        if (below instanceof HeatSourceBlockEntity source) {
            int pulled = (int) (source.getHeatStored() * 0.1D);
            if (pulled > 0) {
                source.useHeat(pulled);
                this.sawmillHeat += pulled;
            } else {
                this.sawmillHeat = Math.max(this.sawmillHeat - Math.max(this.sawmillHeat / 1000, 1), 0);
            }
        } else {
            this.sawmillHeat = Math.max(this.sawmillHeat - Math.max(this.sawmillHeat / 1000, 1), 0);
        }

        int receivedHeat = this.sawmillHeat;
        this.sawmillRotationSpeed = receivedHeat * 25.0F / 300.0F;
        this.sawmillRotation = (this.sawmillRotation + this.sawmillRotationSpeed) % 360.0F;
        boolean changed = false;
        if (this.sawmillHeat >= 100) {
            ItemStack result = sawmillOutput(level, this.items.get(0));
            if (!result.isEmpty()) {
                this.progress += this.sawmillHeat / 10;
                if (this.progress >= profile().processTime()) {
                    this.progress = 0;
                    // TileEntitySawmill clears the singleton input and overwrites
                    // both output slots at completion, including automation cases.
                    this.items.set(0, ItemStack.EMPTY);
                    this.items.set(1, result.copy());
                    if (!isSawdust(result)) {
                        float chance = result.is(Items.STICK) ? 0.1F : 0.5F;
                        if (level.random.nextFloat() < chance) {
                            ItemStack sawdust = legacyItem("powder_sawdust", 1);
                            if (!sawdust.isEmpty()) {
                                this.items.set(2, sawdust);
                            }
                        }
                    }
                    this.completed++;
                    changed = true;
                }
            } else if (this.progress != 0) {
                this.progress = 0;
                changed = true;
            }
            sawmillDamageEntities(level);
        } else if (this.progress != 0) {
            this.progress = 0;
            changed = true;
        }

        if (this.sawmillWarnCooldown > 0) {
            this.sawmillWarnCooldown--;
        }
        if (this.sawmillHeat > 300) {
            this.sawmillOverspeed++;
            if (this.sawmillOverspeed > 60 && this.sawmillWarnCooldown == 0) {
                this.sawmillWarnCooldown = 100;
                level.playSound(null, this.worldPosition, HbmSoundEvents.WARN_OVERSPEED.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
            if (this.sawmillOverspeed > 300) {
                ejectSawmillBlade(level);
                changed = true;
            }
        } else {
            this.sawmillOverspeed = 0;
        }

        // The old tile clears received heat at the end of every server tick.
        this.sawmillHeat = 0;
        // 1.7.10 sent the received heat to the client so the blade and both gears
        // could continue their local interpolation. Synchronize the same state at
        // a bounded cadence instead of forcing a block update for every machine tick.
        if (changed || level.getGameTime() % 5L == 0L) {
            setChangedAndSync();
        }
    }

    private void sawmillDamageEntities(Level level) {
        // TileEntitySawmill uses a 0.125-block blade strip, rotated with
        // ForgeDirection#getRotation(UP) rather than the machine render yaw.
        Direction forward = facing();
        Direction bladeSide = LegacyMachineGeometry.forgeRotateUp(forward);
        double centerX = this.worldPosition.getX() + 0.5D + bladeSide.getStepX() * 0.9375D;
        double centerZ = this.worldPosition.getZ() + 0.5D + bladeSide.getStepZ() * 0.9375D;
        double halfX = Math.abs(forward.getStepX()) + Math.abs(bladeSide.getStepX()) * 0.0625D;
        double halfZ = Math.abs(forward.getStepZ()) + Math.abs(bladeSide.getStepZ()) * 0.0625D;
        AABB cutter = new AABB(centerX - halfX, this.worldPosition.getY() + 0.375D, centerZ - halfZ,
                centerX + halfX, this.worldPosition.getY() + 2.375D, centerZ + halfZ);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, cutter)) {
            if (!entity.isAlive() || !entity.hurt(level.damageSources().source(HbmDamageTypes.TURBOFAN), 100.0F)) {
                continue;
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F,
                    0.95F + level.random.nextFloat() * 0.2F);
            if (level instanceof ServerLevel serverLevel) {
                int count = Math.min((int) Math.ceil(entity.getMaxHealth() / 4.0F), 250) * 4;
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        count, 0.0D, 0.0D, 0.0D, 0.1D);
            }
        }
    }

    private void ejectSawmillBlade(Level level) {
        this.sawmillHasBlade = false;
        level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D, 5.0F, Level.ExplosionInteraction.NONE);
        Direction forward = facing();
        Direction flight = LegacyMachineGeometry.forgeRotateDown(forward);
        SawbladeEntity blade = new SawbladeEntity(level,
                this.worldPosition.getX() + 0.5D + forward.getStepX(),
                this.worldPosition.getY() + 1.0D,
                this.worldPosition.getZ() + 0.5D + forward.getStepZ())
                .setOrientation(forward.ordinal());
        blade.setDeltaMovement(flight.getStepX(), 1.0D + (this.sawmillHeat - 100) * 0.0001D, flight.getStepZ());
        level.addFreshEntity(blade);
        this.sawmillOverspeed = 0;
        this.sawmillWarnCooldown = 0;
    }

    private ItemStack sawmillOutput(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.is(ItemTags.LOGS)) {
            CraftingInput craftInput = CraftingInput.of(1, 1, List.of(input.copyWithCount(1)));
            for (RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> holder
                    : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                if (!holder.value().matches(craftInput, level)) {
                    continue;
                }
                ItemStack result = holder.value().assemble(craftInput, level.registryAccess());
                if (!result.isEmpty()) {
                    result.setCount(Math.max(1, result.getCount() * 6 / 4));
                    return result;
                }
            }
            return ItemStack.EMPTY;
        }
        if (input.is(ItemTags.PLANKS)) {
            return new ItemStack(Items.STICK, 6);
        }
        if (input.is(Items.STICK)) {
            return legacyItem("powder_sawdust", 1);
        }
        if (input.is(ItemTags.SAPLINGS)) {
            return new ItemStack(Items.STICK);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack legacyItem(String path, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static boolean isSawdust(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("powder_sawdust");
    }

    private void tickAutocrafter(Level level) {
        LegacyMachineProfile profile = profile();
        this.energy = BatteryPackItem.dischargeIntoMachine(this.items.get(20), this.energy, profile.energyCapacity());
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipes = autocrafterRecipes(level);
        this.autocrafterRecipeCount = recipes.size();
        if (recipes.isEmpty()) {
            if (!this.items.get(9).isEmpty()) {
                this.items.set(9, ItemStack.EMPTY);
                setChanged();
            }
            resetProgress();
            return;
        }
        if (this.autocrafterRecipeIndex >= recipes.size()) this.autocrafterRecipeIndex = 0;
        RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> selected = recipes.get(this.autocrafterRecipeIndex);
        ItemStack preview = selected.value().assemble(templateGrid(), level.registryAccess());
        if (!ItemStack.isSameItemSameComponents(this.items.get(9), preview)) {
            this.items.set(9, preview.copy());
            setChanged();
        }
        if (this.energy < profile.energyPerTick()) {
            resetProgress();
            return;
        }
        CraftingInput input = inputGrid();
        if (!selected.value().matches(input, level)) {
            resetProgress();
            return;
        }
        ItemStack result = selected.value().assemble(input, level.registryAccess());
        if (result.isEmpty() || !hasExactOutputSpace(result, 19)) {
            resetProgress();
            return;
        }
        this.energy -= profile.energyPerTick();
        this.progress++;
        if (this.progress >= profile.processTime()) {
            for (int slot = 10; slot < 19; slot++) {
                ItemStack stack = this.items.get(slot);
                if (!stack.isEmpty()) {
                    ItemStack ingredient = stack.copyWithCount(1);
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        this.items.set(slot, ItemStack.EMPTY);
                        restoreAutocrafterContainer(slot, ingredient);
                    }
                }
            }
            insertExactOutput(result, 19);
            this.progress = 0;
            this.completed++;
        }
        setChanged();
    }

    private CraftingInput templateGrid() {
        return CraftingInput.of(3, 3, this.items.subList(0, 9));
    }

    private CraftingInput inputGrid() {
        return CraftingInput.of(3, 3, this.items.subList(10, 19));
    }

    private List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> autocrafterRecipes(Level level) {
        CraftingInput template = templateGrid();
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.value().matches(template, level))
                .toList();
    }

    public void setAutocrafterTemplate(int slot, ItemStack source) {
        if (!machineId().equals("machine_autocrafter") || slot < 0 || slot >= 9) {
            return;
        }
        this.items.set(slot, source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1));
        this.autocrafterModes[slot] = smartAutocrafterMode(this.items.get(slot));
        refreshAutocrafterTemplate();
    }

    public void cycleAutocrafterMode(int slot) {
        if (!machineId().equals("machine_autocrafter") || slot < 0 || slot >= 9) {
            return;
        }
        ItemStack pattern = this.items.get(slot);
        if (pattern.isEmpty()) {
            this.autocrafterModes[slot] = "";
        } else {
            List<String> tags = autocrafterItemTags(pattern);
            String current = this.autocrafterModes[slot];
            if (current == null || current.isEmpty()) {
                this.autocrafterModes[slot] = "exact";
            } else if (current.equals("exact")) {
                this.autocrafterModes[slot] = pattern.getItem() instanceof BedrockOreItem ? "bedrock" : "wildcard";
            } else if (current.equals("bedrock")) {
                this.autocrafterModes[slot] = "wildcard";
            } else if (current.equals("wildcard")) {
                this.autocrafterModes[slot] = tags.isEmpty() ? "exact" : tags.getFirst();
            } else {
                int index = tags.indexOf(current);
                this.autocrafterModes[slot] = index < 0 || index + 1 >= tags.size() ? "exact" : tags.get(index + 1);
            }
        }
        this.setChangedAndSync();
    }

    public void nextAutocrafterRecipe() {
        if (!machineId().equals("machine_autocrafter") || this.level == null) {
            return;
        }
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipes = autocrafterRecipes(this.level);
        this.autocrafterRecipeCount = recipes.size();
        this.autocrafterRecipeIndex = recipes.isEmpty() ? 0 : (this.autocrafterRecipeIndex + 1) % recipes.size();
        updateAutocrafterPreview(recipes);
        this.setChangedAndSync();
    }

    public String autocrafterMode(int slot) {
        return slot >= 0 && slot < this.autocrafterModes.length ? this.autocrafterModes[slot] : "";
    }

    public int autocrafterRecipeIndex() {
        return this.autocrafterRecipeIndex;
    }

    public int autocrafterRecipeCount() {
        return this.autocrafterRecipeCount;
    }

    private void refreshAutocrafterTemplate() {
        if (this.level == null) {
            this.autocrafterRecipeIndex = 0;
            this.autocrafterRecipeCount = 0;
            this.items.set(9, ItemStack.EMPTY);
            this.setChangedAndSync();
            return;
        }
        List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipes = autocrafterRecipes(this.level);
        this.autocrafterRecipeCount = recipes.size();
        this.autocrafterRecipeIndex = 0;
        updateAutocrafterPreview(recipes);
        this.setChangedAndSync();
    }

    private void updateAutocrafterPreview(List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> recipes) {
        if (recipes.isEmpty()) {
            this.items.set(9, ItemStack.EMPTY);
            return;
        }
        this.autocrafterRecipeIndex = Math.min(this.autocrafterRecipeIndex, recipes.size() - 1);
        this.items.set(9, recipes.get(this.autocrafterRecipeIndex).value().assemble(templateGrid(), this.level.registryAccess()).copy());
    }

    private static String smartAutocrafterMode(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        List<String> tags = autocrafterItemTags(stack);
        for (String prefix : List.of("ingot", "block", "dust", "nugget", "plate")) {
            for (String tag : tags) {
                if (tagPathMatches(tag, prefix)) {
                    return tag;
                }
            }
        }
        if (stack.getItem() instanceof BedrockOreItem) {
            return "bedrock";
        }
        return hasLegacySubtype(stack) ? "exact" : "wildcard";
    }

    private static boolean autocrafterFilterMatches(ItemStack filter, String mode, ItemStack input) {
        if (filter.isEmpty() || input.isEmpty() || mode == null || mode.isEmpty()) {
            return false;
        }
        return switch (mode) {
            case "exact" -> autocrafterExactMatches(input, filter);
            case "wildcard" -> input.is(filter.getItem());
            case "bedrock" -> input.getItem() instanceof BedrockOreItem
                    && input.is(filter.getItem())
                    && bedrockGrade(input).equals(bedrockGrade(filter));
            default -> autocrafterItemTags(input).contains(mode);
        };
    }

    private static String bedrockGrade(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("grade");
    }

    /**
     * 1.7.10's ItemStack#isItemEqual compared the item and metadata only.  The
     * migration stores former metadata in these explicit variant components;
     * machine state, durability and arbitrary custom data must not affect a
     * template match.
     */
    private static boolean autocrafterExactMatches(ItemStack input, ItemStack filter) {
        return input.is(filter.getItem())
                && legacySubtypeKey(input).equals(legacySubtypeKey(filter));
    }

    private static boolean hasLegacySubtype(ItemStack stack) {
        if (stack.getItem() instanceof LegacyVariantItem) {
            return true;
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.contains("variant") || data.contains("material_id") || data.contains("material")
                || stack.get(DataComponents.CUSTOM_MODEL_DATA) != null;
    }

    private static String legacySubtypeKey(ItemStack stack) {
        if (stack.getItem() instanceof LegacyVariantItem variantItem) {
            return "variant:" + variantItem.variant(stack).id();
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        for (String key : List.of("variant", "material_id", "material")) {
            if (data.contains(key, Tag.TAG_STRING)) {
                return key + ":string:" + data.getString(key);
            }
            if (data.contains(key, Tag.TAG_INT)) {
                return key + ":int:" + data.getInt(key);
            }
        }
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return modelData == null ? "default" : "model:" + modelData.value();
    }

    private void restoreAutocrafterContainer(int slot, ItemStack ingredient) {
        Item item = ingredient.getItem();
        if (!item.hasCraftingRemainingItem(ingredient)) {
            return;
        }
        ItemStack container = item.getCraftingRemainingItem(ingredient);
        if (container.isEmpty()) {
            return;
        }
        if (container.isDamageableItem() && container.getDamageValue() > container.getMaxDamage()) {
            return;
        }
        this.items.set(slot, container);
    }

    private static List<String> autocrafterItemTags(ItemStack stack) {
        return BuiltInRegistries.ITEM.getTags()
                .filter(entry -> stack.is(entry.getFirst()))
                .map(entry -> entry.getFirst().location().toString())
                .sorted()
                .toList();
    }

    private static boolean tagPathMatches(String tag, String prefix) {
        ResourceLocation location = ResourceLocation.tryParse(tag);
        return location != null && location.getPath().toLowerCase(java.util.Locale.ROOT).startsWith(prefix);
    }

    private boolean canPlaceAutocrafterInput(int slot, ItemStack stack) {
        if (slot < 10 || slot > 18 || stack.isEmpty() || (stack.getCount() > 1 && stack.hasCraftingRemainingItem())) {
            return false;
        }
        int filterIndex = slot - 10;
        ItemStack filter = this.items.get(filterIndex);
        if (filter.isEmpty() || this.autocrafterModes[filterIndex] == null || this.autocrafterModes[filterIndex].isEmpty()) {
            return false;
        }
        ItemStack existing = this.items.get(slot);
        if (existing.getCount() + stack.getCount() > 4 || stack.getCount() > 4) {
            return false;
        }
        List<Integer> validSlots = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            ItemStack candidate = this.items.get(index);
            String mode = this.autocrafterModes[index];
            if (!candidate.isEmpty() && mode != null && !mode.isEmpty() && autocrafterFilterMatches(candidate, mode, stack)) {
                validSlots.add(index + 10);
                if (index + 10 == slot && this.items.get(slot).isEmpty()) {
                    return true;
                }
            }
        }
        if (!validSlots.contains(slot) || existing.isEmpty()) {
            return false;
        }
        int size = existing.getCount();
        for (int validSlot : validSlots) {
            ItemStack candidate = this.items.get(validSlot);
            if (candidate.isEmpty()) {
                return false;
            }
            if (!ItemStack.isSameItemSameComponents(candidate, stack)) {
                continue;
            }
            if (candidate.getCount() < size) {
                return false;
            }
        }
        return !stack.hasCraftingRemainingItem();
    }

    private void tickRtg() {
        long heat = 0L;
        boolean changed = false;
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.getItem() instanceof RtgPelletItem pellet) {
                heat += pellet.heat(stack);
                this.items.set(slot, pellet.burnTick(stack));
                changed = true;
            }
        }
        long cappedHeat = Math.min(HbmConfig.rtgDecay() ? 600L : 200L, heat);
        this.rtgHeat = (int) cappedHeat;
        if (cappedHeat > 0L && this.energy < profile().energyCapacity()) {
            this.energy = Math.min(profile().energyCapacity(), this.energy + cappedHeat * 5L);
            this.completed++;
            changed = true;
        }
        // Pellet depletion belongs to the item stack and must persist even when
        // the generator itself is already full.
        if (changed) setChanged();
    }

    private void tickRadiolysis(Level level) {
        long heat = 0L;
        boolean changed = false;
        for (int slot = 0; slot < Math.min(10, this.items.size()); slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.getItem() instanceof RtgPelletItem pellet) {
                heat += pellet.heat(stack);
                this.items.set(slot, pellet.burnTick(stack));
                changed = true;
            }
        }
        this.radiolysisHeat = (int) Math.min(Integer.MAX_VALUE, heat);
        changed |= configureRadiolysisInput(level);
        if (heat > 0L && this.energy < profile().energyCapacity()) {
            this.energy = Math.min(profile().energyCapacity(), this.energy + heat * 10L);
            this.completed++;
            changed = true;
        }

        // TileEntityMachineRadiolysis charges its slot 14 battery after the
        // RTGs have produced power. It is an output-only battery slot.
        long charged = BatteryPackItem.chargeFromMachine(this.items.get(14), this.energy);
        if (charged != this.energy) {
            this.energy = charged;
            changed = true;
        }

        RadiolysisOutputs outputs = radiolysisOutputs(level, this.tanks[0].type());
        if (outputs == null) {
            // Old setupTanks() clears all three tanks when the configured
            // input fluid has no radiolysis/cracking recipe.
            if (this.tanks[0].amount() > 0 || this.tanks[1].amount() > 0 || this.tanks[2].amount() > 0) {
                this.tanks[0].clear();
                this.tanks[1].clear();
                this.tanks[2].clear();
                changed = true;
            }
        } else {
            changed |= conformRadiolysisOutputs(outputs);
            if (heat > 100L) {
                int crackTime = (int) Math.max(-0.1D * (heat - 100L) + 30.0D, 5.0D);
                if (level.getGameTime() % crackTime == 0L && crackRadiolysis(outputs)) {
                    changed = true;
                }
            }
        }
        // TileEntityMachineRadiolysis sterilizes contaminated food based only
        // on RTG heat; it does not require a configured radiolysis fluid.
        if (heat >= 200L && level.getGameTime() % 100L == 0L && sterilizeContaminatedFood()) {
            changed = true;
        }
        if (changed) setChanged();
    }

    /** Mirrors the old FluidTank#setType(10, 11, slots) configuration path. */
    private boolean configureRadiolysisInput(Level level) {
        ItemStack identifier = this.items.get(10);
        if (!(identifier.getItem() instanceof FluidIdentifierItem)) return false;
        HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
        if (selected.isNone() || radiolysisOutputs(level, selected) == null) return false;
        if (this.tanks[0].amount() == 0 && this.tanks[0].type() != selected) {
            this.tanks[0].setType(selected);
            return true;
        }
        return false;
    }

    /** 1.7.10 RadiolysisRecipes: water plus every registered cracking recipe. */
    @Nullable
    private RadiolysisOutputs radiolysisOutputs(Level level, HbmFluidDefinition input) {
        if (input == null || input.isNone()) return null;
        if (input == HbmFluids.byName("water").orElse(HbmFluids.none())) {
            return new RadiolysisOutputs(
                    HbmFluids.byName("peroxide").orElse(HbmFluids.none()), 80,
                    HbmFluids.byName("hydrogen").orElse(HbmFluids.none()), 20
            );
        }
        for (RecipeHolder<CrackingRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRACKING.get())) {
            CrackingRecipe recipe = holder.value();
            if (recipe.input().fluid() == input) {
                return new RadiolysisOutputs(
                        recipe.output1().fluid(), recipe.output1().amount(),
                        recipe.output2().fluid(), recipe.output2().amount()
                );
            }
        }
        return null;
    }

    private boolean conformRadiolysisOutputs(RadiolysisOutputs outputs) {
        boolean changed = false;
        if (this.tanks[1].type() != outputs.left()) {
            this.tanks[1].setType(outputs.left());
            changed = true;
        }
        if (this.tanks[2].type() != outputs.right()) {
            this.tanks[2].setType(outputs.right());
            changed = true;
        }
        return changed;
    }

    private boolean crackRadiolysis(RadiolysisOutputs outputs) {
        if (this.tanks[0].amount() < 100
                || this.tanks[1].amount() + outputs.leftAmount() > this.tanks[1].capacity()
                || this.tanks[2].amount() + outputs.rightAmount() > this.tanks[2].capacity()) {
            return false;
        }
        this.tanks[0].drain(this.tanks[0].type(), 100, false);
        if (outputs.leftAmount() > 0) this.tanks[1].fill(outputs.left(), outputs.leftAmount(), false);
        if (outputs.rightAmount() > 0) this.tanks[2].fill(outputs.right(), outputs.rightAmount(), false);
        this.progress++;
        this.completed++;
        return true;
    }

    private boolean sterilizeContaminatedFood() {
        ItemStack input = this.items.get(12);
        if (input.isEmpty() || !input.has(DataComponents.FOOD)) return false;
        CompoundTag data = input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.getBoolean("ntmContagion")) return false;

        ItemStack sterilized = input.copyWithCount(1);
        CompoundTag cleaned = sterilized.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        cleaned.remove("ntmContagion");
        if (cleaned.isEmpty()) sterilized.remove(DataComponents.CUSTOM_DATA);
        else sterilized.set(DataComponents.CUSTOM_DATA, CustomData.of(cleaned));

        ItemStack output = this.items.get(13);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, sterilized)
                || output.getCount() >= output.getMaxStackSize())) {
            return false;
        }
        input.shrink(1);
        if (input.isEmpty()) this.items.set(12, ItemStack.EMPTY);
        if (output.isEmpty()) this.items.set(13, sterilized);
        else output.grow(1);
        return true;
    }

    private record RadiolysisOutputs(HbmFluidDefinition left, int leftAmount, HbmFluidDefinition right, int rightAmount) {
    }

    private void tickRadGen() {
        int[] progress = getIntArrayState("radgen_progress", 12);
        int[] duration = getIntArrayState("radgen_duration", 12);
        int[] production = getIntArrayState("radgen_production", 12);
        boolean changed = false;

        for (int slot = 0; slot < 12; slot++) {
            ItemStack fuel = this.items.get(slot);
            if (fuel.isEmpty() || !isRadGenFuel(fuel) || duration[slot] > 0) {
                continue;
            }
            ItemStack output = radGenOutput(fuel);
            int outputSlot = slot + 12;
            if (!output.isEmpty() && !canAcceptOutput(outputSlot, output)) {
                continue;
            }
            this.items.set(slot, ItemStack.EMPTY);
            progress[slot] = 0;
            duration[slot] = radGenDuration(fuel);
            production[slot] = radGenProduction(fuel);
            this.radgenProcessing[slot] = fuel.copyWithCount(1);
            changed = true;
        }

        boolean active = false;
        for (int slot = 0; slot < 12; slot++) {
            if (duration[slot] <= 0) {
                continue;
            }
            active = true;
            progress[slot]++;
            this.energy = Math.min(profile().energyCapacity(), this.energy + production[slot]);
            if (progress[slot] >= duration[slot]) {
                ItemStack output = radGenOutputFromProcessing(slot);
                if (!output.isEmpty()) insertExactOutput(output, slot + 12);
                this.radgenProcessing[slot] = ItemStack.EMPTY;
                progress[slot] = 0;
                duration[slot] = 0;
                production[slot] = 0;
                changed = true;
            }
        }
        if (this.radgenOn != active) {
            this.radgenOn = active;
            changed = true;
        }
        if (active || changed) {
            if (changed) setChangedAndSync();
            else setChanged();
        }
    }

    private int[] getIntArrayState(String key, int count) {
        // The arrays are rebuilt from the current NBT snapshot only for the
        // server tick; the values are persisted by saveAdditional/loadAdditional.
        return switch (key) {
            case "radgen_progress" -> this.radgenProgress;
            case "radgen_duration" -> this.radgenDuration;
            case "radgen_production" -> this.radgenProduction;
            default -> new int[count];
        };
    }

    private boolean isRadGenFuel(ItemStack stack) {
        return stack.is(HbmItems.NUCLEAR_WASTE_SHORT.get())
                || stack.is(HbmItems.NUCLEAR_WASTE_SHORT_TINY.get())
                || stack.is(HbmItems.NUCLEAR_WASTE_LONG.get())
                || stack.is(HbmItems.NUCLEAR_WASTE_LONG_TINY.get())
                || stack.is(HbmItems.SCRAP_NUCLEAR.get())
                || stack.is(HbmItems.GEM_RAD.get());
    }

    private int radGenProduction(ItemStack stack) {
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT.get())) return 1_500;
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT_TINY.get())) return 150;
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG.get())) return 500;
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG_TINY.get())) return 50;
        if (stack.is(HbmItems.SCRAP_NUCLEAR.get())) return 50;
        if (stack.is(HbmItems.GEM_RAD.get())) return 25_000;
        return 0;
    }

    private int radGenDuration(ItemStack stack) {
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT.get())) return 36_000;
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT_TINY.get())) return 3_600;
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG.get())) return 144_000;
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG_TINY.get())) return 14_400;
        if (stack.is(HbmItems.SCRAP_NUCLEAR.get())) return 6_000;
        if (stack.is(HbmItems.GEM_RAD.get())) return 36_000;
        return 0;
    }

    private ItemStack radGenOutput(ItemStack stack) {
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT.get())) {
            return NuclearWasteItem.copyWasteClass(stack, HbmItems.NUCLEAR_WASTE_SHORT_DEPLETED.get());
        }
        if (stack.is(HbmItems.NUCLEAR_WASTE_SHORT_TINY.get())) {
            return NuclearWasteItem.copyWasteClass(stack, HbmItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get());
        }
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG.get())) {
            return NuclearWasteItem.copyWasteClass(stack, HbmItems.NUCLEAR_WASTE_LONG_DEPLETED.get());
        }
        if (stack.is(HbmItems.NUCLEAR_WASTE_LONG_TINY.get())) {
            return NuclearWasteItem.copyWasteClass(stack, HbmItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get());
        }
        if (stack.is(HbmItems.GEM_RAD.get())) return new ItemStack(net.minecraft.world.item.Items.DIAMOND);
        return ItemStack.EMPTY;
    }

    private ItemStack radGenOutputFromProcessing(int slot) {
        ItemStack processing = this.radgenProcessing[slot];
        return processing == null || processing.isEmpty() ? ItemStack.EMPTY : radGenOutput(processing);
    }

    private boolean canAcceptOutput(int slot, ItemStack output) {
        ItemStack existing = this.items.get(slot);
        return existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, output)
                && existing.getCount() + output.getCount() <= existing.getMaxStackSize());
    }

    private void insertExactOutput(ItemStack output, int slot) {
        ItemStack existing = this.items.get(slot);
        if (existing.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(existing, output)) {
            existing.grow(output.getCount());
        }
    }

    private void resetProgress() {
        if (this.progress != 0) {
            this.progress = 0;
            setChanged();
        }
    }

    private boolean hasOutputSpace(ItemStack result, int outputStart) {
        for (int slot = outputStart; slot < this.items.size(); slot++) {
            ItemStack existing = this.items.get(slot);
            if (existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExactOutputSpace(ItemStack result, int slot) {
        ItemStack existing = this.items.get(slot);
        return existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize());
    }

    private void insertOutput(ItemStack result, int outputStart) {
        for (int slot = outputStart; slot < this.items.size(); slot++) {
            ItemStack existing = this.items.get(slot);
            if (existing.isEmpty()) {
                this.items.set(slot, result);
                return;
            }
            if (ItemStack.isSameItemSameComponents(existing, result)
                    && existing.getCount() + result.getCount() <= existing.getMaxStackSize()) {
                existing.grow(result.getCount());
                return;
            }
        }
    }

    public String machineId() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this.getBlockState().getBlock()).getPath();
    }

    public LegacyMachineProfile profile() {
        return LegacyMachineProfile.forId(machineId());
    }

    public int slotCount() {
        return this.items.size();
    }

    public int outputStart() {
        return profile().outputStart();
    }

    public long energyCapacity() {
        if (machineId().equals("machine_forcefield")) {
            return HbmConfig.FORCEFIELD_MAX_POWER.get();
        }
        if (machineId().equals("machine_radar") || machineId().equals("machine_radar_large")) {
            return HbmConfig.RADAR_POWER_CAP.get();
        }
        return machineId().equals("machine_precass") ? this.precisionAssemblerMaxPower : profile().energyCapacity();
    }

    public long energyStored() {
        return this.energy;
    }

    public int processTime() {
        return machineId().equals("machine_precass") ? 1_000 : profile().processTime();
    }

    public int precisionAssemblerTankCapacity(int tank) {
        return machineId().equals("machine_precass") && tank >= 0 && tank < this.tanks.length ? this.tanks[tank].capacity() : 4_000;
    }

    /** Direct client-visible equivalent of TileEntityMachineRadGen#isOn. */
    public boolean radgenOn() {
        return this.radgenOn;
    }

    public int radgenProgress(int slot) {
        return slot >= 0 && slot < this.radgenProgress.length ? this.radgenProgress[slot] : 0;
    }

    public int radgenDuration(int slot) {
        return slot >= 0 && slot < this.radgenDuration.length ? this.radgenDuration[slot] : 0;
    }

    public int radgenProduction(int slot) {
        return slot >= 0 && slot < this.radgenProduction.length ? this.radgenProduction[slot] : 0;
    }

    public boolean precisionAssemblerWorking() {
        return this.precisionAssemblerWorking;
    }

    public double precisionAssemblerRing(float partialTick) {
        return this.precisionAssemblerPreviousRing + (this.precisionAssemblerRing - this.precisionAssemblerPreviousRing) * partialTick;
    }

    public double precisionAssemblerArmAngle(int arm, float partialTick) {
        return arm < 0 || arm >= 3 ? 0.0D : this.precisionAssemblerPreviousArmAngles[arm]
                + (this.precisionAssemblerArmAngles[arm] - this.precisionAssemblerPreviousArmAngles[arm]) * partialTick;
    }

    public double precisionAssemblerStriker(int striker, float partialTick) {
        return striker < 0 || striker >= 4 ? 0.0D : this.precisionAssemblerPreviousStrikers[striker]
                + (this.precisionAssemblerStrikers[striker] - this.precisionAssemblerPreviousStrikers[striker]) * partialTick;
    }

    public static boolean isBatteryStack(ItemStack stack) {
        return BatteryPackItem.isBattery(stack);
    }

    public List<RecipeHolder<PrecisionAssemblerRecipe>> availablePrecisionAssemblerRecipes(Level level) {
        Optional<String> pool = BlueprintItem.pool(this.items.get(1));
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRECISION_ASSEMBLER.get()).stream()
                .filter(holder -> holder.value().isVisibleForPool(pool))
                .sorted((left, right) -> left.id().toString().compareTo(right.id().toString()))
                .toList();
    }

    public Optional<ResourceLocation> precisionAssemblerRecipeId() {
        return Optional.ofNullable(this.precisionAssemblerRecipe);
    }

    public void setPrecisionAssemblerRecipe(@Nullable ResourceLocation recipeId) {
        if (!machineId().equals("machine_precass")) return;
        ResourceLocation selected = recipeId;
        final ResourceLocation requested = selected;
        if (requested != null && (this.level == null || availablePrecisionAssemblerRecipes(this.level).stream().noneMatch(holder -> holder.id().equals(requested)))) {
            selected = null;
        }
        if (java.util.Objects.equals(this.precisionAssemblerRecipe, selected)) return;
        this.precisionAssemblerRecipe = selected;
        this.precisionAssemblerProgress = 0.0D;
        this.progress = 0;
        setChangedAndSync();
    }

    public Optional<RecipeHolder<PrecisionAssemblerRecipe>> selectedPrecisionAssemblerRecipe(Level level) {
        if (this.precisionAssemblerRecipe == null) return Optional.empty();
        return availablePrecisionAssemblerRecipes(level).stream().filter(holder -> holder.id().equals(this.precisionAssemblerRecipe)).findFirst();
    }

    public boolean canAcceptPrecisionAssemblerInput(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) return false;
        return selectedPrecisionAssemblerRecipe(this.level)
                .map(holder -> holder.value().ingredients().stream().anyMatch(ingredient -> ingredient.ingredient().test(stack)))
                .orElse(false);
    }

    /** Exact ModuleMachineBase#isItemValid contract for the nine ordered inputs. */
    private boolean canAcceptPrecisionAssemblerInput(int slot, ItemStack stack) {
        if (slot < 4 || slot > 12 || stack.isEmpty() || this.level == null) return false;
        int recipeSlot = slot - 4;
        return selectedPrecisionAssemblerRecipe(this.level)
                .map(holder -> recipeSlot < holder.value().ingredients().size()
                        && holder.value().ingredients().get(recipeSlot).ingredient().test(stack))
                .orElse(false);
    }

    public boolean sawmillHasBlade() {
        return this.sawmillHasBlade;
    }

    public void setSawmillHasBlade(boolean hasBlade) {
        if (!machineId().equals("machine_sawmill")) {
            return;
        }
        this.sawmillHasBlade = hasBlade;
        this.sawmillWarnCooldown = 0;
        this.sawmillOverspeed = 0;
        this.setChangedAndSync();
    }

    public float sawmillRotation() {
        return this.sawmillRotation;
    }

    public float sawmillRotationSpeed() {
        return this.sawmillRotationSpeed;
    }

    public boolean hasMenu() {
        return profile().menu();
    }

    /** Implements MachineSawmill's old empty-hand output collection. */
    public boolean handleEmptyHandInteraction(Player player) {
        if (!machineId().equals("machine_sawmill")) {
            return false;
        }
        if (this.level == null || this.level.isClientSide) {
            return true;
        }
        return collectSawmillOutputs(player);
    }

    /** Implements MachineSawmill's old right-click path for held items. */
    public boolean handleItemInteraction(Player player, ItemStack heldStack) {
        if (machineId().equals("machine_orbus")) {
            // MachineOrbus consumes every crouching interaction. With a fluid
            // identifier it changes only the accepted tank type; 1.7.10 never
            // drained or cleared the existing contents here.
            if (!player.isCrouching()) {
                return false;
            }
            if (heldStack.getItem() instanceof FluidIdentifierItem) {
                HbmFluidTank tank = tank(0);
                if (tank != null) {
                    HbmFluidDefinition fluid = FluidIdentifierItem.primary(heldStack);
                    tank.setType(fluid);
                    this.setChangedAndSync();
                    player.displayClientMessage(
                            Component.literal("Changed type to ")
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW)
                                    .append(Component.translatable(fluid.translationKey()))
                                    .append("!"),
                            false
                    );
                }
            }
            return true;
        }
        if (machineId().equals("machine_conveyor_press")) {
            if (isStamp(heldStack) && this.items.get(0).isEmpty()) {
                this.items.set(0, heldStack.copyWithCount(1));
                heldStack.shrink(1);
                this.setChanged();
                return true;
            }
            if (ScrewdriverItem.isScrewdriver(heldStack) && !this.items.get(0).isEmpty()) {
                player.getInventory().placeItemBackInInventory(this.items.get(0).copy());
                this.items.set(0, ItemStack.EMPTY);
                this.setChanged();
                return true;
            }
            return false;
        }
        if (machineId().equals("machine_autosaw")) {
            if (heldStack.getItem() instanceof FluidIdentifierItem) {
                HbmFluidDefinition fuel = FluidIdentifierItem.primary(heldStack);
                if (isAutosawFuel(fuel) && this.inputTank != null) {
                    this.inputTank.setType(fuel);
                    this.setChangedAndSync();
                    return true;
                }
            }
            if (ScrewdriverItem.isScrewdriver(heldStack)) {
                this.autosawSuspended = !this.autosawSuspended;
                this.setChangedAndSync();
                return true;
            }
            return false;
        }
        if (machineId().equals("machine_thresher")) {
            if (!player.isCrouching() && heldStack.getItem() instanceof FluidIdentifierItem) {
                HbmFluidDefinition fuel = FluidIdentifierItem.primary(heldStack);
                if (isThresherFuel(fuel) && this.inputTank != null) {
                    this.inputTank.setType(fuel);
                    this.setChangedAndSync();
                    player.displayClientMessage(
                            Component.literal("Changed type to ")
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW)
                                    .append(Component.translatable(fuel.translationKey()))
                                    .append("!"),
                            false
                    );
                    return true;
                }
            }
            if (ScrewdriverItem.isScrewdriver(heldStack)) {
                this.thresherSuspended = !this.thresherSuspended;
                this.setChangedAndSync();
                return true;
            }
            return false;
        }
        if (machineId().equals("machine_turbofan") && heldStack.getItem() instanceof FluidIdentifierItem) {
            pasteFluidSetting(FluidIdentifierItem.primary(heldStack), this.level, player, this.worldPosition);
            return true;
        }
        if (!machineId().equals("machine_sawmill")) {
            return false;
        }
        if (this.level == null || this.level.isClientSide) {
            return true;
        }
        if (!this.sawmillHasBlade && isLegacyItem(heldStack, "sawblade")) {
            heldStack.shrink(1);
            this.sawmillHasBlade = true;
            this.setChangedAndSync();
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
            return true;
        }
        if (!this.items.get(1).isEmpty() || !this.items.get(2).isEmpty()) {
            return collectSawmillOutputs(player);
        }
        if (this.items.get(0).isEmpty() && !heldStack.isEmpty() && !sawmillOutput(this.level, heldStack).isEmpty()) {
            this.items.set(0, heldStack.copyWithCount(1));
            heldStack.shrink(1);
            this.setChanged();
            return true;
        }
        return false;
    }

    private boolean collectSawmillOutputs(Player player) {
        boolean collected = false;
        for (int slot = 1; slot <= 2; slot++) {
            ItemStack output = this.items.get(slot);
            if (output.isEmpty()) {
                continue;
            }
            player.getInventory().placeItemBackInInventory(output.copy());
            this.items.set(slot, ItemStack.EMPTY);
            collected = true;
        }
        if (collected) {
            this.setChanged();
        }
        return collected;
    }

    private static boolean isLegacyItem(ItemStack stack, String id) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(
                ResourceLocation.fromNamespaceAndPath("reinhardtshbm", id)
        );
    }

    private static boolean isStamp(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("stamp_");
    }

    public HbmFluidTank inputTank() {
        return this.inputTank;
    }

    public HbmFluidTank outputTank() {
        return this.outputTank;
    }

    public boolean forcefieldRenderable() {
        return this.forcefieldOn && this.forcefieldHealth > 0 && this.energy > 0L && this.forcefieldCooldown == 0;
    }

    public int forcefieldColor() {
        return this.forcefieldColor;
    }

    public int forcefieldRadius() {
        return this.forcefieldRadius;
    }

    public int forcefieldCooldown() {
        return this.forcefieldCooldown;
    }

    public void cycleOrbusMode() {
        if (!machineId().equals("machine_orbus")) {
            return;
        }
        this.orbusMode = (this.orbusMode + 1) % 4;
        setChangedAndSync();
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index] : null;
    }

    @Override
    public int[] getFluidIdsToCopy() {
        if (!machineId().equals("machine_turbofan")) {
            return new int[0];
        }
        return new int[]{
                tankOldId(0), tankOldId(1), tankOldId(2), tankOldId(3), tankOldId(4)
        };
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (!machineId().equals("machine_turbofan") || fluid == null || fluid.isNone()) {
            return;
        }
        applyTurbofanFluidSetting(fluid);
    }

    /** Applies a fluid identifier before the block's normal interaction path. */
    public boolean applyTurbofanFluidSetting(HbmFluidDefinition fluid) {
        if (!machineId().equals("machine_turbofan") || fluid == null || fluid.isNone()) {
            return false;
        }
        HbmFluidTank fuel = tank(0);
        if (fuel == null || fuel.type() == fluid) {
            return fuel != null;
        }
        // FluidTank#setType is the old setTankType behavior: changing the
        // identifier also discards the incompatible fuel already stored.
        fuel.setType(fluid);
        setChangedAndSync();
        return true;
    }

    public int radiolysisHeat() {
        return this.radiolysisHeat;
    }

    private int tankOldId(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index].type().oldId() : 0;
    }

    private int tankAmount(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index].amount() : 0;
    }

    @Nullable
    public IFluidHandler fluidHandler() {
        if (this.tanks.length == 0) {
            return null;
        }
        return machineId().equals("machine_orbus") ? orbusFluidHandler() : new LegacyFluidHandler();
    }

    /**
     * Dummies are the actual exposed faces of large legacy machines. The
     * pyro oven has a separate smoke exhaust, so it cannot use the generic
     * all-tank handler at that position.
     */
    @Nullable
    public IFluidHandler fluidHandler(BlockPos accessorPos, @Nullable Direction side) {
        if (this.tanks.length == 0) {
            return null;
        }
        if (machineId().equals("machine_autosaw")) {
            // TileEntityMachineAutosaw subscribes from every direction except UP.
            // Its fluid tank is on the core; it has no dummy, output, or power port.
            if (!this.worldPosition.equals(accessorPos) || side == Direction.UP) {
                return null;
            }
            return new LegacyFluidHandler(new int[]{0}, new int[0]);
        }
        if (machineId().equals("machine_thresher")) {
            // TileEntityMachineThresher only subscribes at the two lateral
            // positions derived from dir.getRotation(UP), never the front,
            // rear, top, or bottom face.
            Direction left = LegacyMachineGeometry.forgeRotateUp(facing());
            Direction right = left.getOpposite();
            if (!this.worldPosition.equals(accessorPos) || (side != left && side != right)) {
                return null;
            }
            return new LegacyFluidHandler(new int[]{0}, new int[0]);
        }
        if (machineId().equals("machine_orbus")) {
            return allowsFluidAutomationPort(accessorPos) ? orbusFluidHandler() : null;
        }
        if (!machineId().equals("machine_pyrooven")) {
            return allowsFluidAutomationPort(accessorPos) ? new LegacyFluidHandler() : null;
        }
        Direction exhaustSide = LegacyMachineGeometry.forgeRotateDown(facing());
        BlockPos exhaust = offset(Direction.NORTH, 0, exhaustSide, -1, 3);
        if (accessorPos.equals(exhaust)) {
            return new LegacyFluidHandler(new int[0], new int[]{2, 3, 4});
        }
        return servicePorts().contains(accessorPos)
                ? new LegacyFluidHandler(new int[]{0}, new int[]{1})
                : null;
    }

    public ContainerData menuData() {
        return this.data;
    }

    private IFluidHandler orbusFluidHandler() {
        return switch (this.orbusMode) {
            case 0 -> new LegacyFluidHandler(new int[]{0}, new int[0]);
            case 1 -> new LegacyFluidHandler(new int[]{0}, new int[]{0});
            case 2 -> new LegacyFluidHandler(new int[0], new int[]{0});
            default -> new LegacyFluidHandler(new int[0], new int[0]);
        };
    }

    public int progress() {
        return this.progress;
    }

    public int completed() {
        return this.completed;
    }

    public boolean autosawOn() {
        return this.autosawOn;
    }

    public boolean autosawSuspended() {
        return this.autosawSuspended;
    }

    public boolean thresherOn() {
        return this.thresherOn;
    }

    public boolean thresherSuspended() {
        return this.thresherSuspended;
    }

    public float thresherAngle(float partialTick) {
        return this.thresherPreviousAngle + (this.thresherAngle - this.thresherPreviousAngle) * partialTick;
    }

    public float thresherSpin(float partialTick) {
        return this.thresherPreviousSpin + (this.thresherSpin - this.thresherPreviousSpin) * partialTick;
    }

    public float autosawYaw(float partialTick) {
        return this.autosawPreviousYaw + (this.autosawYaw - this.autosawPreviousYaw) * partialTick;
    }

    public float autosawPitch(float partialTick) {
        return this.autosawPreviousPitch + (this.autosawPitch - this.autosawPreviousPitch) * partialTick;
    }

    public float autosawSpin(float partialTick) {
        return this.autosawPreviousSpin + (this.autosawSpin - this.autosawPreviousSpin) * partialTick;
    }

    public double conveyorPress() {
        return this.conveyorPress;
    }

    public boolean conveyorRetracting() {
        return this.conveyorRetracting;
    }

    public boolean pyroProgressing() {
        return this.pyroProgressing;
    }

    public boolean pyroVenting() {
        return this.pyroVenting;
    }

    public int pyroAnimation() {
        return this.pyroAnimation;
    }

    public int pyroDuration() {
        return this.pyroDuration;
    }

    public int turbofanAfterburner() {
        return this.turbofanAfterburner;
    }

    public boolean turbofanShowBlood() {
        return this.turbofanShowBlood;
    }

    public boolean turbofanWasOn() {
        return this.turbofanWasOn;
    }

    public float turbofanSpin(float partialTick) {
        return this.turbofanLastSpin + (this.turbofanSpin - this.turbofanLastSpin) * partialTick;
    }

    public int turbofanMomentum() {
        return this.turbofanMomentum;
    }

    public boolean radarScanMissiles() { return this.radarScanMissiles; }
    public boolean radarScanShells() { return this.radarScanShells; }
    public boolean radarScanPlayers() { return this.radarScanPlayers; }
    public boolean radarSmartMode() { return this.radarSmartMode; }
    public boolean radarRedMode() { return this.radarRedMode; }
    public boolean radarShowMap() { return this.radarShowMap; }
    public boolean radarJammed() { return this.radarJammed; }
    public int radarRangeValue() { return radarRange(); }
    public long radarConsumptionValue() { return radarConsumption(); }
    public int radarRedPower() { return this.radarLastRedPower; }
    public float radarRotation(float partialTick) {
        return this.radarPreviousRotation + (this.radarRotation - this.radarPreviousRotation) * partialTick;
    }
    public List<RadarTarget> radarTargets() { return List.copyOf(this.radarTargets); }
    public byte radarMapCell(int index) {
        return index >= 0 && index < this.radarMap.length ? this.radarMap[index] : 0;
    }

    /** Actions mirrored from GUIMachineRadarNT. */
    public void applyRadarControl(int action) {
        if (!machineId().equals("machine_radar") && !machineId().equals("machine_radar_large")) return;
        switch (action) {
            case 0 -> this.radarScanMissiles = !this.radarScanMissiles;
            case 1 -> this.radarScanShells = !this.radarScanShells;
            case 2 -> this.radarScanPlayers = !this.radarScanPlayers;
            case 3 -> this.radarSmartMode = !this.radarSmartMode;
            case 4 -> this.radarRedMode = !this.radarRedMode;
            case 5 -> this.radarShowMap = !this.radarShowMap;
            case 6 -> {
                this.radarMap = new byte[40_000];
                this.radarChunkReads.clear();
            }
            default -> { return; }
        }
        setChangedAndSync();
    }

    public void openRadarSlots(ServerPlayer player) {
        if (!machineId().equals("machine_radar") && !machineId().equals("machine_radar_large")) return;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new com.reinhardt.hbm.menu.RadarSlotsMenu(containerId, inventory, this),
                Component.translatable("block.reinhardtshbm." + machineId())
        ), buffer -> buffer.writeBlockPos(this.worldPosition));
    }

    /** Direct server-side equivalent of GUIMachineRadarNT's 1-8 relay command keys. */
    public boolean issueRadarCommand(ServerPlayer player, int relaySlot, int targetEntityId, int targetX, int targetZ) {
        if ((!machineId().equals("machine_radar") && !machineId().equals("machine_radar_large"))
                || relaySlot < 0 || relaySlot >= 8 || this.level == null) {
            return false;
        }
        ItemStack link = this.items.get(relaySlot);
        if (!(link.getItem() instanceof RadarLinkerItem)) {
            return false;
        }
        BlockPos targetPos = RadarLinkerItem.position(link);
        if (targetPos == null) {
            return false;
        }
        BlockEntity target = this.level.getBlockEntity(targetPos);
        if (target instanceof MachineDummyBlockEntity dummy) {
            target = dummy.core();
        }
        if (!(target instanceof RadarCommandReceiver receiver)) {
            return false;
        }
        boolean accepted;
        if (targetEntityId >= 0) {
            Entity entity = this.level.getEntity(targetEntityId);
            accepted = entity != null && receiver.sendCommandEntity(entity);
        } else {
            accepted = receiver.sendCommandPosition(targetX, this.worldPosition.getY(), targetZ);
        }
        if (accepted) {
            this.level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return accepted;
    }

    public String annihilatorPool() {
        return this.annihilatorPool;
    }

    public BigInteger annihilatorMonitor() {
        return this.annihilatorMonitor;
    }

    public void setAnnihilatorPool(String pool) {
        if (!machineId().equals("machine_annihilator") || pool == null) {
            return;
        }
        String normalized = pool.strip();
        if (normalized.isEmpty()) {
            return;
        }
        this.annihilatorPool = normalized.substring(0, Math.min(20, normalized.length()));
        this.annihilatorMonitor = BigInteger.ZERO;
        setChangedAndSync();
    }

    /** Direct port of TileEntityMachineMissileAssembly#chipState. */
    private int missileChipState() {
        MissilePartItem.Definition definition = MissilePartItem.definition(this.items.get(0));
        return definition != null && definition.type() == MissilePartItem.Type.CHIP ? 1 : 0;
    }

    /** Direct port of TileEntityMachineMissileAssembly#fuselageState. */
    private int missileFuselageState() {
        MissilePartItem.Definition definition = MissilePartItem.definition(this.items.get(2));
        return definition != null && definition.type() == MissilePartItem.Type.FUSELAGE ? 1 : 0;
    }

    /** Direct port of TileEntityMachineMissileAssembly#warheadState. */
    private int missileWarheadState() {
        MissilePartItem.Definition warhead = MissilePartItem.definition(this.items.get(1));
        MissilePartItem.Definition fuselage = MissilePartItem.definition(this.items.get(2));
        MissilePartItem.Definition thruster = MissilePartItem.definition(this.items.get(4));
        if (warhead == null || fuselage == null || thruster == null
                || warhead.type() != MissilePartItem.Type.WARHEAD
                || fuselage.type() != MissilePartItem.Type.FUSELAGE
                || thruster.type() != MissilePartItem.Type.THRUSTER) {
            return 0;
        }
        return warhead.bottom() == fuselage.top() && warhead.secondary() <= thruster.secondary() ? 1 : 0;
    }

    /** Direct port of TileEntityMachineMissileAssembly#stabilityState. */
    private int missileStabilityState() {
        if (this.items.get(3).isEmpty()) return -1;
        MissilePartItem.Definition fins = MissilePartItem.definition(this.items.get(3));
        MissilePartItem.Definition fuselage = MissilePartItem.definition(this.items.get(2));
        return fins != null && fuselage != null && fins.type() == MissilePartItem.Type.FINS
                && fuselage.type() == MissilePartItem.Type.FUSELAGE && fins.top() == fuselage.bottom() ? 1 : 0;
    }

    /** Direct port of TileEntityMachineMissileAssembly#thrusterState. */
    private int missileThrusterState() {
        MissilePartItem.Definition thruster = MissilePartItem.definition(this.items.get(4));
        MissilePartItem.Definition fuselage = MissilePartItem.definition(this.items.get(2));
        return thruster != null && fuselage != null && thruster.type() == MissilePartItem.Type.THRUSTER
                && fuselage.type() == MissilePartItem.Type.FUSELAGE && thruster.top() == fuselage.bottom()
                && thruster.fuel() == fuselage.fuel() ? 1 : 0;
    }

    private int missileAssemblyStateMask() {
        int fins = missileStabilityState();
        int encodedFins = fins < 0 ? 0 : fins == 0 ? 2 : 1;
        return missileChipState()
                | (missileWarheadState() << 2)
                | (missileFuselageState() << 4)
                | (encodedFins << 6)
                | (missileThrusterState() << 8);
    }

    public boolean canConstructMissile() {
        return machineId().equals("machine_missile_assembly") && this.items.get(5).isEmpty()
                && missileChipState() == 1 && missileWarheadState() == 1 && missileFuselageState() == 1
                && missileThrusterState() == 1 && missileStabilityState() != 0;
    }

    /** Direct port of TileEntityMachineMissileAssembly#construct. */
    public void constructMissile() {
        if (!canConstructMissile()) return;
        ItemStack fins = this.items.get(3);
        boolean consumeFins = missileStabilityState() == 1;
        this.items.set(5, CustomMissileItem.build(this.items.get(0), this.items.get(1), this.items.get(2), fins, this.items.get(4)));
        this.items.set(0, ItemStack.EMPTY);
        this.items.set(1, ItemStack.EMPTY);
        this.items.set(2, ItemStack.EMPTY);
        if (consumeFins) this.items.set(3, ItemStack.EMPTY);
        this.items.set(4, ItemStack.EMPTY);
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.MISSILE_ASSEMBLY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        setChangedAndSync();
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return switch (machineId()) {
            // TileEntityConveyorPress exposes all four horizontal faces.
            case "machine_conveyor_press" -> corePorts(false, false);
            // TileEntityMachineRTG and TileEntityMachineAutocrafter enumerate ForgeDirection.VALID_DIRECTIONS.
            case "machine_rtg_grey", "machine_autocrafter", "machine_teleporter" -> corePorts(true, true);
            // TileEntityForceField explicitly excludes its top face.
            case "machine_forcefield" -> corePorts(true, false);
            // TileEntityMachineRadGen: tryProvide(core - facing * 4, facing.opposite()).
            case "machine_radgen" -> List.of(this.worldPosition.relative(facing().getOpposite(), 4).immutable());
            case "machine_radar" -> corePorts(false, false);
            case "machine_precass", "machine_pyrooven", "machine_radiolysis", "machine_radar_large", "machine_turbofan" -> servicePorts();
            default -> List.of();
        };
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos)
                && machineSide != null;
    }

    /** Exact 1.7.10 dummy service locations, not adjacent-volume guesses. */
    public boolean allowsItemAutomationPort(BlockPos position) {
        return servicePorts().contains(position);
    }

    /** Exact 1.7.10 fluid service locations, including dedicated smoke outlets. */
    public boolean allowsFluidAutomationPort(BlockPos position) {
        if (machineId().equals("machine_turbofan")) {
            return turbofanPortDummies().contains(position);
        }
        return fluidPorts().contains(position);
    }

    private Direction facing() {
        return this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
    }

    private List<BlockPos> corePorts(boolean includeDown, boolean includeUp) {
        List<BlockPos> ports = new ArrayList<>(6);
        ports.add(this.worldPosition.north().immutable());
        ports.add(this.worldPosition.east().immutable());
        ports.add(this.worldPosition.south().immutable());
        ports.add(this.worldPosition.west().immutable());
        if (includeDown) ports.add(this.worldPosition.below().immutable());
        if (includeUp) ports.add(this.worldPosition.above().immutable());
        return List.copyOf(ports);
    }

    private List<BlockPos> servicePorts() {
        Direction dir = facing();
        Direction left = LegacyMachineGeometry.forgeRotateDown(dir);
        return switch (machineId()) {
            // TileEntityMachineAnnihilator#getConPos.
            case "machine_annihilator" -> {
                Direction side = LegacyMachineGeometry.forgeRotateUp(dir);
                yield List.of(
                        this.worldPosition.relative(dir, 5).immutable(),
                        offset(dir, 3, side, 2, 0),
                        offset(dir, 3, side, -2, 0)
                );
            }
            // TileEntityMachinePyroOven#getConPos.
            case "machine_pyrooven" -> List.of(
                    offset(dir, 2, left, 3, 0),
                    offset(dir, 1, left, 3, 0),
                    offset(dir, 0, left, 3, 0),
                    offset(dir, -1, left, 3, 0),
                    offset(dir, -2, left, 3, 0)
            );
            // TileEntityMachinePrecAss#getConPos and TileEntityMachineRadiolysis#getConPos.
            case "machine_precass" -> planarGridPorts(2);
            case "machine_radiolysis", "machine_radar_large" -> cardinalPorts(2);
            // TileEntityMachineTurbofan#getConPos.
            case "machine_turbofan" -> {
                Direction nozzle = LegacyMachineGeometry.forgeRotateUp(dir);
                Direction side = LegacyMachineGeometry.forgeRotateDown(nozzle);
                yield List.of(
                        offset(nozzle, 0, side, 2, 0),
                        offset(nozzle, -1, side, 2, 0),
                        offset(nozzle, 0, side, -2, 0),
                        offset(nozzle, -1, side, -2, 0)
                );
            }
            // TileEntityMachineOrbus#getConPos.
            case "machine_orbus" -> orbusPorts(dir);
            default -> List.of();
        };
    }

    /**
     * The turbofan's getConPos() coordinates are the pipe positions one block
     * outside its body. Fluid capabilities belong on the four extra dummies
     * created by MachineTurbofan#fillSpace, which those pipe positions touch.
     */
    private List<BlockPos> turbofanPortDummies() {
        Direction forward = facing();
        Direction lateral = LegacyMachineGeometry.forgeRotateUp(forward);
        return List.of(
                this.worldPosition.relative(forward).immutable(),
                this.worldPosition.relative(forward).relative(lateral.getOpposite()).immutable(),
                this.worldPosition.relative(forward.getOpposite()).immutable(),
                this.worldPosition.relative(forward.getOpposite()).relative(lateral.getOpposite()).immutable()
        );
    }

    private List<BlockPos> fluidPorts() {
        if (machineId().equals("machine_autosaw")) {
            // TileEntityMachineAutosaw accepts fluid on every face except UP.
            return corePorts(true, false);
        }
        List<BlockPos> ports = new ArrayList<>(servicePorts());
        if (machineId().equals("machine_pyrooven")) {
            // TileEntityMachinePyroOven sends smoke through this separate upper exhaust.
            Direction side = LegacyMachineGeometry.forgeRotateDown(facing());
            ports.add(offset(Direction.NORTH, 0, side, -1, 3));
        }
        return List.copyOf(ports);
    }

    private List<BlockPos> cardinalPorts(int distance) {
        return List.of(
                this.worldPosition.east(distance).immutable(),
                this.worldPosition.west(distance).immutable(),
                this.worldPosition.south(distance).immutable(),
                this.worldPosition.north(distance).immutable()
        );
    }

    private List<BlockPos> planarGridPorts(int distance) {
        List<BlockPos> ports = new ArrayList<>(12);
        for (int z = -1; z <= 1; z++) {
            ports.add(this.worldPosition.offset(distance, 0, z).immutable());
            ports.add(this.worldPosition.offset(-distance, 0, z).immutable());
        }
        for (int x = -1; x <= 1; x++) {
            ports.add(this.worldPosition.offset(x, 0, distance).immutable());
            ports.add(this.worldPosition.offset(x, 0, -distance).immutable());
        }
        return List.copyOf(ports);
    }

    private List<BlockPos> orbusPorts(Direction direction) {
        Direction forward = direction.getOpposite();
        Direction side = LegacyMachineGeometry.forgeRotateDown(forward);
        List<BlockPos> ports = new ArrayList<>(8);
        for (int y : new int[]{-1, 5}) {
            ports.add(this.worldPosition.offset(0, y, 0).immutable());
            ports.add(offset(forward, 1, side, 0, y));
            ports.add(offset(forward, 0, side, 1, y));
            ports.add(offset(forward, 1, side, 1, y));
        }
        return List.copyOf(ports);
    }

    private BlockPos offset(Direction forward, int forwardDistance, Direction side, int sideDistance, int vertical) {
        return this.worldPosition.offset(
                forward.getStepX() * forwardDistance + side.getStepX() * sideDistance,
                vertical,
                forward.getStepZ() * forwardDistance + side.getStepZ() * sideDistance
        ).immutable();
    }

    @Override
    public long getAvailableOutput() {
        return profile().energyOutput() ? this.energy : 0L;
    }

    @Override
    public long getRequestedInput() {
        LegacyMachineProfile profile = profile();
        long capacity = energyCapacity();
        if (profile.energyOutput() || capacity <= 0L || this.energy >= capacity) {
            return 0L;
        }
        long demand = machineId().equals("machine_teleporter") ? capacity - this.energy
                : machineId().equals("machine_precass") ? this.precisionAssemblerDemand
                : (machineId().equals("machine_radar") || machineId().equals("machine_radar_large"))
                        ? radarConsumption() : profile.energyPerTick();
        return Math.min(demand, capacity - this.energy);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        LegacyMachineProfile profile = profile();
        if (profile.energyOutput()) {
            this.energy = Math.max(0L, this.energy - Math.max(0L, usedOutput));
        } else {
            this.energy = Math.min(energyCapacity(), this.energy + Math.max(0L, receivedInput));
        }
        this.lastInput = Math.max(0L, receivedInput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.legacy_machine.power",
                Component.translatable("block.reinhardtshbm." + machineId()),
                this.lastInput, this.energy, energyCapacity(), this.progress, processTime(), this.completed);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isValidSlot(slot)) {
            this.items.set(slot, stack.copyWithCount(Math.min(stack.getCount(), getMaxStackSize())));
            if (machineId().equals("machine_missile_assembly")) setChangedAndSync();
            else setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        String id = machineId();
        if (id.equals("machine_annihilator")) {
            return slot == 0 || slot == 8 || slot == 9
                    || (slot == 1 && stack.getItem() instanceof FluidIdentifierItem);
        }
        if (id.equals("machine_radar") || id.equals("machine_radar_large")) {
            if (slot >= 0 && slot <= 7) return stack.is(HbmItems.SAT_RELAY.get()) || stack.getItem() instanceof RadarLinkerItem;
            if (slot == 8) return stack.getItem() instanceof RadarLinkerItem;
            return slot == 9 && BatteryPackItem.isBattery(stack);
        }
        if (id.equals("machine_precass")) {
            if (slot == 0) return BatteryPackItem.isBattery(stack);
            if (slot == 1) return BlueprintItem.isBlueprint(stack);
            if (slot >= 2 && slot <= 3) {
                MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
                return MachineUpgradeItem.isMachineUpgrade(stack)
                        && (type == MachineUpgradeItem.UpgradeType.SPEED || type == MachineUpgradeItem.UpgradeType.POWER || type == MachineUpgradeItem.UpgradeType.OVERDRIVE);
            }
            return canAcceptPrecisionAssemblerInput(slot, stack);
        }
        if (id.equals("machine_autocrafter")) {
            if (slot == 20) return BatteryPackItem.isBattery(stack);
            if (slot <= 8) return true;
            return canPlaceAutocrafterInput(slot, stack);
        }
        if (id.equals("machine_missile_assembly")) {
            MissilePartItem.Definition definition = MissilePartItem.definition(stack);
            if (definition == null) return false;
            return switch (slot) {
                case 0 -> definition.type() == MissilePartItem.Type.CHIP;
                case 1 -> definition.type() == MissilePartItem.Type.WARHEAD;
                case 2 -> definition.type() == MissilePartItem.Type.FUSELAGE;
                case 3 -> definition.type() == MissilePartItem.Type.FINS;
                case 4 -> definition.type() == MissilePartItem.Type.THRUSTER;
                default -> false;
            };
        }
        if (id.equals("machine_forcefield")) {
            return (slot == 0 && BatteryPackItem.isBattery(stack)) || slot == 1;
        }
        if (id.equals("machine_orbus")) {
            HbmFluidTank tank = tank(0);
            if (tank == null) return false;
            if (slot == 0) return stack.getItem() instanceof FluidIdentifierItem;
            if (slot == 2) {
                return HbmFluidContainerTransfer.canDrainIntoTank(stack, tank,
                        fluid -> tank.type().isNone() || tank.type() == fluid,
                        output -> canPlaceOrbusOutput(3, output));
            }
            return slot == 4 && HbmFluidContainerTransfer.canFillFromTank(stack, tank,
                    output -> canPlaceOrbusOutput(5, output));
        }
        if (id.equals("machine_sawmill")) {
            return slot == 0 && this.items.get(0).isEmpty() && this.items.get(1).isEmpty() && this.items.get(2).isEmpty()
                    && stack.getCount() == 1 && this.level != null && !sawmillOutput(this.level, stack).isEmpty();
        }
        if (id.equals("machine_conveyor_press")) {
            return slot == 0 && isStamp(stack);
        }
        if (id.equals("machine_rtg_grey")) {
            return stack.getItem() instanceof RtgPelletItem;
        }
        if (id.equals("machine_radgen")) {
            return canPlaceRadGenFuel(slot, stack);
        }
        if (id.equals("machine_pyrooven")) {
            return slot == 0 ? BatteryPackItem.isBattery(stack)
                    : slot == 1
                    || (slot == 3 && stack.getItem() instanceof FluidIdentifierItem)
                    || (slot >= 4 && slot <= 5 && isPyroUpgrade(stack));
        }
        if (id.equals("machine_radiolysis")) {
            return (slot < 10 && stack.getItem() instanceof RtgPelletItem)
                    || (slot == 10 && stack.getItem() instanceof FluidIdentifierItem)
                    || slot == 12
                    || (slot == 14 && BatteryPackItem.isBattery(stack));
        }
        if (id.equals("machine_turbofan")) {
            if (slot == 0) {
                HbmFluidTank fuel = tank(0);
                return fuel != null && HbmFluidContainerTransfer.canDrainIntoTank(
                        stack, fuel, fluid -> fuel.type().isNone() || fuel.type() == fluid,
                        this::canInsertTurbofanContainerOutput);
            }
            if (slot == 2) {
                return MachineUpgradeItem.upgradeType(stack) == MachineUpgradeItem.UpgradeType.AFTERBURN
                        || isLegacyItem(stack, "flame_pony");
            }
            if (slot == 3) return BatteryPackItem.isBattery(stack);
            return slot == 4 && stack.getItem() instanceof FluidIdentifierItem;
        }
        return slot < profile().outputStart();
    }

    /** Exact TileEntityMachineRadGen#isItemValidForSlot balancing rule. */
    private boolean canPlaceRadGenFuel(int slot, ItemStack stack) {
        if (slot < 0 || slot >= 12 || !isRadGenFuel(stack)) {
            return false;
        }

        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            return true;
        }

        int currentCount = current.getCount();
        for (int inputSlot = 0; inputSlot < 12; inputSlot++) {
            ItemStack held = this.items.get(inputSlot);
            if (held.isEmpty()) {
                return false;
            }
            if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < currentCount) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (machineId().equals("machine_annihilator")) {
            return new int[]{0, 2, 3, 4, 5, 6, 7};
        }
        if (machineId().equals("machine_precass")) {
            return new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
        }
        if (machineId().equals("machine_autocrafter")) {
            return new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        }
        if (machineId().equals("machine_orbus")) {
            return new int[]{2, 3, 4, 5};
        }
        if (machineId().equals("machine_missile_assembly")) {
            // TileEntityMachineMissileAssembly only advertises its chip slot.
            return new int[]{0};
        }
        if (machineId().equals("machine_pyrooven")) {
            return new int[]{1, 2};
        }
        if (machineId().equals("machine_turbofan")) {
            return new int[]{0, 1, 2, 3, 4};
        }
        return this.slotsForFace;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (machineId().equals("machine_missile_assembly")) {
            // The 1.7.10 implementation deliberately rejected automated insertion.
            return false;
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (machineId().equals("machine_annihilator")) {
            return slot >= 2 && slot <= 7;
        }
        if (machineId().equals("machine_orbus")) {
            return slot == 3 || slot == 5;
        }
        if (machineId().equals("machine_precass")) {
            return slot >= 13 || precisionAssemblerInputClogged(slot);
        }
        if (machineId().equals("machine_rtg_grey")) {
            return false;
        }
        if (machineId().equals("machine_autocrafter")) {
            if (slot == 19) {
                return true;
            }
            if (slot >= 10 && slot <= 18) {
                ItemStack filter = this.items.get(slot - 10);
                String mode = this.autocrafterModes[slot - 10];
                return filter.isEmpty() || mode == null || mode.isEmpty()
                        || !autocrafterFilterMatches(filter, mode, stack);
            }
            return false;
        }
        if (machineId().equals("machine_radiolysis")) {
            return (slot < 10 && stack.getItem() instanceof RtgDepletedPelletItem) || slot == 13;
        }
        if (machineId().equals("machine_sawmill")) {
            return slot == 1 || slot == 2;
        }
        if (machineId().equals("machine_conveyor_press")) {
            return false;
        }
        if (machineId().equals("machine_pyrooven")) {
            return slot == 2;
        }
        if (machineId().equals("machine_turbofan")) {
            return slot == 1 || slot == 3;
        }
        if (machineId().equals("machine_missile_assembly")) {
            return false;
        }
        return profile().outputStart() < this.items.size() && slot >= profile().outputStart();
    }

    private boolean precisionAssemblerInputClogged(int slot) {
        if (slot < 4 || slot > 12 || this.level == null) return false;
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) return false;
        Optional<RecipeHolder<PrecisionAssemblerRecipe>> recipe = selectedPrecisionAssemblerRecipe(this.level);
        int recipeSlot = slot - 4;
        return recipe.isEmpty() || recipeSlot >= recipe.get().value().ingredients().size()
                || !recipe.get().value().ingredients().get(recipeSlot).ingredient().test(stack);
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
    public Component getDisplayName() {
        return Component.translatable("block.reinhardtshbm." + machineId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // The 1.7.10 sawmill (and other no-menu legacy machines) is operated
        // directly on the block. Never fall through to the generic menu.
        if (!profile().menu()) {
            return null;
        }
        if (machineId().equals("machine_annihilator")) {
            return new com.reinhardt.hbm.menu.AnnihilatorMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_precass")) {
            return new com.reinhardt.hbm.menu.PrecisionAssemblerMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_autocrafter")) {
            return new com.reinhardt.hbm.menu.AutocrafterMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_forcefield")) {
            return new com.reinhardt.hbm.menu.ForcefieldMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_orbus")) {
            return new com.reinhardt.hbm.menu.OrbusMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_radiolysis")) {
            return new com.reinhardt.hbm.menu.RadiolysisMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_pyrooven")) {
            return new com.reinhardt.hbm.menu.PyroOvenMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_turbofan")) {
            return new com.reinhardt.hbm.menu.TurbofanMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_missile_assembly")) {
            return new com.reinhardt.hbm.menu.MissileAssemblyMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_radar") || machineId().equals("machine_radar_large")) {
            return new com.reinhardt.hbm.menu.RadarMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_radgen")) {
            return new com.reinhardt.hbm.menu.RadGenMenu(containerId, inventory, this);
        }
        if (machineId().equals("machine_rtg_grey")) {
            return new com.reinhardt.hbm.menu.RtgMenu(containerId, inventory, this);
        }
        return null;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        int firstDroppedSlot = machineId().equals("machine_autocrafter") ? 10 : 0;
        for (int slot = firstDroppedSlot; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
        this.items.clear();
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("machine_id", machineId());
        tag.putLong("energy", this.energy);
        tag.putLong("last_input", this.lastInput);
        tag.putInt("progress", this.progress);
        tag.putInt("completed", this.completed);
        tag.putInt("radiolysis_heat", this.radiolysisHeat);
        tag.putBoolean("radgen_on", this.radgenOn);
        tag.putInt("autocrafter_recipe_index", this.autocrafterRecipeIndex);
        tag.putInt("autocrafter_recipe_count", this.autocrafterRecipeCount);
        tag.putInt("orbus_mode", this.orbusMode);
        for (int slot = 0; slot < this.autocrafterModes.length; slot++) {
            if (this.autocrafterModes[slot] != null && !this.autocrafterModes[slot].isEmpty()) {
                tag.putString("autocrafter_mode_" + slot, this.autocrafterModes[slot]);
            }
        }
        tag.putInt("forcefield_health", this.forcefieldHealth);
        tag.putInt("forcefield_max_health", this.forcefieldMaxHealth);
        tag.putInt("forcefield_radius", this.forcefieldRadius);
        tag.putInt("forcefield_power_consumption", this.forcefieldPowerConsumption);
        tag.putInt("forcefield_color", this.forcefieldColor);
        tag.putBoolean("forcefield_on", this.forcefieldOn);
        tag.putInt("forcefield_cooldown", this.forcefieldCooldown);
        tag.putInt("forcefield_blink", this.forcefieldBlink);
        tag.putInt("teleporter_target_x", this.teleporterTargetX);
        tag.putInt("teleporter_target_y", this.teleporterTargetY);
        tag.putInt("teleporter_target_z", this.teleporterTargetZ);
        tag.putString("teleporter_target_dimension", this.teleporterTargetDimension);
        tag.putBoolean("sawmill_has_blade", this.sawmillHasBlade);
        tag.putInt("sawmill_heat", this.sawmillHeat);
        tag.putFloat("sawmill_rotation", this.sawmillRotation);
        tag.putFloat("sawmill_rotation_speed", this.sawmillRotationSpeed);
        tag.putBoolean("autosaw_on", this.autosawOn);
        tag.putBoolean("autosaw_suspended", this.autosawSuspended);
        tag.putFloat("autosaw_yaw", this.autosawYaw);
        tag.putFloat("autosaw_pitch", this.autosawPitch);
        tag.putInt("autosaw_state", this.autosawState);
        tag.putInt("autosaw_force_skip", this.autosawForceSkip);
        tag.putBoolean("thresher_on", this.thresherOn);
        tag.putBoolean("thresher_suspended", this.thresherSuspended);
        tag.putInt("thresher_state", this.thresherState);
        tag.putFloat("thresher_angle", this.thresherAngle);
        tag.putDouble("conveyor_press", this.conveyorPress);
        tag.putBoolean("conveyor_retracting", this.conveyorRetracting);
        tag.putInt("conveyor_delay", this.conveyorDelay);
        tag.putBoolean("pyro_progressing", this.pyroProgressing);
        tag.putBoolean("pyro_venting", this.pyroVenting);
        tag.putInt("pyro_animation", this.pyroAnimation);
        tag.putInt("pyro_duration", this.pyroDuration);
        if (this.precisionAssemblerRecipe != null) tag.putString("precision_assembler_recipe", this.precisionAssemblerRecipe.toString());
        tag.putLong("precision_assembler_max_power", this.precisionAssemblerMaxPower);
        tag.putDouble("precision_assembler_progress", this.precisionAssemblerProgress);
        tag.putInt("precision_assembler_recipe_index", this.precisionAssemblerRecipeIndex);
        tag.putInt("precision_assembler_recipe_count", this.precisionAssemblerRecipeCount);
        tag.putBoolean("precision_assembler_working", this.precisionAssemblerWorking);
        tag.putLong("precision_assembler_demand", this.precisionAssemblerDemand);
        tag.putString("annihilator_pool", this.annihilatorPool);
        tag.putByteArray("annihilator_monitor", this.annihilatorMonitor.toByteArray());
        tag.putInt("turbofan_afterburner", this.turbofanAfterburner);
        tag.putBoolean("turbofan_was_on", this.turbofanWasOn);
        tag.putBoolean("turbofan_show_blood", this.turbofanShowBlood);
        tag.putInt("turbofan_output", this.turbofanOutput);
        tag.putInt("turbofan_consumption", this.turbofanConsumption);
        tag.putBoolean("radar_scan_missiles", this.radarScanMissiles);
        tag.putBoolean("radar_scan_shells", this.radarScanShells);
        tag.putBoolean("radar_scan_players", this.radarScanPlayers);
        tag.putBoolean("radar_smart_mode", this.radarSmartMode);
        tag.putBoolean("radar_red_mode", this.radarRedMode);
        tag.putBoolean("radar_show_map", this.radarShowMap);
        tag.putBoolean("radar_jammed", this.radarJammed);
        tag.putInt("radar_red_power", this.radarLastRedPower);
        tag.putByteArray("radar_map", this.radarMap);
        ListTag radarEntries = new ListTag();
        for (RadarTarget target : this.radarTargets) {
            CompoundTag entry = new CompoundTag();
            entry.putString("name", target.name());
            entry.putInt("blip", target.blipLevel());
            entry.putInt("x", target.x());
            entry.putInt("y", target.y());
            entry.putInt("z", target.z());
            entry.putInt("entity", target.entityId());
            entry.putBoolean("redstone", target.redstone());
            radarEntries.add(entry);
        }
        tag.put("radar_targets", radarEntries);
        for (int tank = 0; tank < this.tanks.length; tank++) {
            tag.put("Tank" + tank, this.tanks[tank].save());
        }
        for (int slot = 0; slot < 12; slot++) {
            tag.putInt("RadGenProgress" + slot, this.radgenProgress[slot]);
            tag.putInt("RadGenDuration" + slot, this.radgenDuration[slot]);
            tag.putInt("RadGenProduction" + slot, this.radgenProduction[slot]);
            if (this.radgenProcessing[slot] != null && !this.radgenProcessing[slot].isEmpty()) {
                tag.put("RadGenProcessing" + slot, this.radgenProcessing[slot].save(registries));
            }
        }
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                tag.put("Slot" + slot, stack.save(registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy = Math.max(0L, tag.getLong("energy"));
        this.lastInput = Math.max(0L, tag.getLong("last_input"));
        this.progress = Math.max(0, tag.getInt("progress"));
        this.completed = Math.max(0, tag.getInt("completed"));
        this.radiolysisHeat = Math.max(0, tag.getInt("radiolysis_heat"));
        this.radgenOn = tag.getBoolean("radgen_on");
        this.autocrafterRecipeIndex = Math.max(0, tag.getInt("autocrafter_recipe_index"));
        this.autocrafterRecipeCount = Math.max(0, tag.getInt("autocrafter_recipe_count"));
        this.orbusMode = Math.floorMod(tag.getInt("orbus_mode"), 4);
        for (int slot = 0; slot < this.autocrafterModes.length; slot++) {
            this.autocrafterModes[slot] = tag.contains("autocrafter_mode_" + slot)
                    ? tag.getString("autocrafter_mode_" + slot) : "";
        }
        this.forcefieldHealth = Math.max(0, tag.getInt("forcefield_health"));
        this.forcefieldMaxHealth = Math.max(1, tag.getInt("forcefield_max_health"));
        this.forcefieldRadius = Math.max(0, tag.getInt("forcefield_radius"));
        this.forcefieldPowerConsumption = Math.max(0, tag.getInt("forcefield_power_consumption"));
        this.forcefieldColor = tag.contains("forcefield_color") ? tag.getInt("forcefield_color") : 0x0000FF;
        this.forcefieldOn = tag.getBoolean("forcefield_on");
        this.forcefieldCooldown = Math.max(0, tag.getInt("forcefield_cooldown"));
        this.forcefieldBlink = Math.max(0, tag.getInt("forcefield_blink"));
        this.teleporterTargetX = tag.contains("teleporter_target_x") ? tag.getInt("teleporter_target_x") : -1;
        this.teleporterTargetY = tag.contains("teleporter_target_y") ? tag.getInt("teleporter_target_y") : -1;
        this.teleporterTargetZ = tag.contains("teleporter_target_z") ? tag.getInt("teleporter_target_z") : -1;
        this.teleporterTargetDimension = tag.contains("teleporter_target_dimension")
                ? tag.getString("teleporter_target_dimension") : "minecraft:overworld";
        this.sawmillHasBlade = !tag.contains("sawmill_has_blade") || tag.getBoolean("sawmill_has_blade");
        this.sawmillHeat = Math.max(0, tag.getInt("sawmill_heat"));
        this.sawmillRotation = tag.getFloat("sawmill_rotation");
        this.sawmillRotationSpeed = Math.max(0.0F, tag.getFloat("sawmill_rotation_speed"));
        this.autosawOn = tag.getBoolean("autosaw_on");
        this.autosawSuspended = tag.getBoolean("autosaw_suspended");
        float savedAutosawYaw = tag.getFloat("autosaw_yaw");
        float savedAutosawPitch = tag.getFloat("autosaw_pitch");
        this.autosawState = Math.max(0, Math.min(2, tag.getInt("autosaw_state")));
        this.autosawForceSkip = Math.max(0, tag.getInt("autosaw_force_skip"));
        if (this.level != null && this.level.isClientSide) {
            // TileEntityMachineAutosaw#deserialize assigns a three-tick turn window.
            this.autosawSyncYaw = savedAutosawYaw;
            this.autosawSyncPitch = savedAutosawPitch;
            this.autosawTurnProgress = 3;
        } else {
            this.autosawYaw = savedAutosawYaw;
            this.autosawPitch = savedAutosawPitch;
            this.autosawPreviousYaw = savedAutosawYaw;
            this.autosawPreviousPitch = savedAutosawPitch;
            this.autosawSyncYaw = savedAutosawYaw;
            this.autosawSyncPitch = savedAutosawPitch;
            this.autosawTurnProgress = 0;
        }
        this.thresherOn = tag.getBoolean("thresher_on");
        this.thresherSuspended = tag.getBoolean("thresher_suspended");
        if (this.level == null || !this.level.isClientSide) {
            // The original TE did not persist the wait delay or client-only wheel phase.
            this.thresherDelay = 0;
            this.thresherSpin = 0.0F;
            this.thresherPreviousSpin = 0.0F;
        }
        this.thresherState = Math.max(0, Math.min(2, tag.getInt("thresher_state")));
        float savedThresherAngle = Math.max(0.0F, Math.min(82.5F, tag.getFloat("thresher_angle")));
        if (this.level != null && this.level.isClientSide) {
            // TileEntityMachineThresher's packet interpolation is three ticks.
            this.thresherSyncAngle = savedThresherAngle;
            this.thresherTurnProgress = 3;
        } else {
            this.thresherAngle = savedThresherAngle;
            this.thresherPreviousAngle = savedThresherAngle;
            this.thresherSyncAngle = savedThresherAngle;
            this.thresherTurnProgress = 0;
        }
        this.conveyorPress = Math.max(0.0D, Math.min(1.0D, tag.getDouble("conveyor_press")));
        this.conveyorRetracting = tag.getBoolean("conveyor_retracting");
        this.conveyorDelay = Math.max(0, tag.getInt("conveyor_delay"));
        this.pyroProgressing = tag.getBoolean("pyro_progressing");
        this.pyroVenting = tag.getBoolean("pyro_venting");
        this.pyroAnimation = Math.max(0, tag.getInt("pyro_animation"));
        this.pyroDuration = Math.max(1, tag.getInt("pyro_duration"));
        this.precisionAssemblerRecipe = tag.contains("precision_assembler_recipe")
                ? ResourceLocation.tryParse(tag.getString("precision_assembler_recipe")) : null;
        this.precisionAssemblerMaxPower = Math.max(100_000L, tag.getLong("precision_assembler_max_power"));
        this.precisionAssemblerProgress = Math.max(0.0D, Math.min(1.0D, tag.getDouble("precision_assembler_progress")));
        this.precisionAssemblerRecipeIndex = Math.max(0, tag.getInt("precision_assembler_recipe_index"));
        this.precisionAssemblerRecipeCount = Math.max(0, tag.getInt("precision_assembler_recipe_count"));
        this.precisionAssemblerWorking = tag.getBoolean("precision_assembler_working");
        this.precisionAssemblerDemand = Math.max(1L, tag.getLong("precision_assembler_demand"));
        this.annihilatorPool = tag.contains("annihilator_pool") && !tag.getString("annihilator_pool").isBlank()
                ? tag.getString("annihilator_pool") : "Recycling";
        try {
            byte[] monitorBytes = tag.getByteArray("annihilator_monitor");
            this.annihilatorMonitor = monitorBytes.length == 0 ? BigInteger.ZERO : new BigInteger(monitorBytes);
        } catch (NumberFormatException ignored) {
            this.annihilatorMonitor = BigInteger.ZERO;
        }
        this.turbofanAfterburner = Math.max(0, tag.getInt("turbofan_afterburner"));
        this.turbofanWasOn = tag.getBoolean("turbofan_was_on");
        this.turbofanShowBlood = tag.getBoolean("turbofan_show_blood");
        this.turbofanOutput = Math.max(0, tag.getInt("turbofan_output"));
        this.turbofanConsumption = Math.max(0, tag.getInt("turbofan_consumption"));
        this.radarScanMissiles = !tag.contains("radar_scan_missiles") || tag.getBoolean("radar_scan_missiles");
        this.radarScanShells = !tag.contains("radar_scan_shells") || tag.getBoolean("radar_scan_shells");
        this.radarScanPlayers = !tag.contains("radar_scan_players") || tag.getBoolean("radar_scan_players");
        this.radarSmartMode = !tag.contains("radar_smart_mode") || tag.getBoolean("radar_smart_mode");
        this.radarRedMode = !tag.contains("radar_red_mode") || tag.getBoolean("radar_red_mode");
        this.radarShowMap = tag.getBoolean("radar_show_map");
        this.radarJammed = tag.getBoolean("radar_jammed");
        this.radarLastRedPower = Math.max(0, Math.min(15, tag.getInt("radar_red_power")));
        this.radarMap = tag.contains("radar_map") ? tag.getByteArray("radar_map") : new byte[40_000];
        if (this.radarMap.length != 40_000) this.radarMap = new byte[40_000];
        this.radarTargets.clear();
        ListTag radarEntries = tag.getList("radar_targets", 10);
        for (int index = 0; index < radarEntries.size(); index++) {
            CompoundTag entry = radarEntries.getCompound(index);
            this.radarTargets.add(new RadarTarget(entry.getString("name"), entry.getInt("blip"),
                    entry.getInt("x"), entry.getInt("y"), entry.getInt("z"), entry.getInt("entity"), entry.getBoolean("redstone")));
        }
        for (int tank = 0; tank < this.tanks.length; tank++) {
            this.tanks[tank].load(tag.getCompound("Tank" + tank));
        }
        for (int slot = 0; slot < 12; slot++) {
            this.radgenProgress[slot] = Math.max(0, tag.getInt("RadGenProgress" + slot));
            this.radgenDuration[slot] = Math.max(0, tag.getInt("RadGenDuration" + slot));
            this.radgenProduction[slot] = Math.max(0, tag.getInt("RadGenProduction" + slot));
            this.radgenProcessing[slot] = ItemStack.parseOptional(registries, tag.getCompound("RadGenProcessing" + slot));
        }
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    private static int[] createSlots(int count) {
        int[] slots = new int[count];
        for (int slot = 0; slot < count; slot++) {
            slots[slot] = slot;
        }
        return slots;
    }

    private static boolean isPyroUpgrade(ItemStack stack) {
        if (!MachineUpgradeItem.isMachineUpgrade(stack)) {
            return false;
        }
        MachineUpgradeItem.UpgradeType type = MachineUpgradeItem.upgradeType(stack);
        return type == MachineUpgradeItem.UpgradeType.SPEED
                || type == MachineUpgradeItem.UpgradeType.POWER
                || type == MachineUpgradeItem.UpgradeType.OVERDRIVE;
    }

    private final class LegacyFluidHandler implements IFluidHandler {
        private final int[] fillTanks;
        private final int[] drainTanks;

        private LegacyFluidHandler() {
            this(profile().fillTanks(), profile().drainTanks());
        }

        private LegacyFluidHandler(int[] fillTanks, int[] drainTanks) {
            this.fillTanks = fillTanks;
            this.drainTanks = drainTanks;
        }

        @Override public int getTanks() {
            return tanks.length;
        }

        @Override public FluidStack getFluidInTank(int tank) {
            return isTank(tank) ? tanks[tank].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override public int getTankCapacity(int tank) {
            return isTank(tank) ? tanks[tank].capacity() : 0;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty() && contains(this.fillTanks, tank);
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            HbmFluidDefinition fluid = com.reinhardt.hbm.registry.HbmFluids.fromNeoFluid(resource.getFluid())
                    .orElse(com.reinhardt.hbm.registry.HbmFluids.none());
            if (fluid.isNone()) return 0;
            int remaining = resource.getAmount();
            for (int tank : this.fillTanks) {
                if (remaining <= 0) break;
                remaining -= tanks[tank].fill(fluid, remaining, action.simulate());
            }
            int filled = resource.getAmount() - remaining;
            if (filled > 0 && action.execute()) setChanged();
            return filled;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            HbmFluidDefinition fluid = com.reinhardt.hbm.registry.HbmFluids.fromNeoFluid(resource.getFluid())
                    .orElse(com.reinhardt.hbm.registry.HbmFluids.none());
            if (fluid.isNone()) return FluidStack.EMPTY;
            for (int tank : this.drainTanks) {
                HbmFluidStack drained = tanks[tank].drain(fluid, resource.getAmount(), action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) setChanged();
                    return com.reinhardt.hbm.registry.HbmFluids.toNeoStack(drained.type(), drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override public FluidStack drain(int amount, FluidAction action) {
            for (int tank : this.drainTanks) {
                HbmFluidStack drained = tanks[tank].drain(null, amount, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) setChanged();
                    return com.reinhardt.hbm.registry.HbmFluids.toNeoStack(drained.type(), drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }

        private boolean isTank(int tank) {
            return tank >= 0 && tank < tanks.length;
        }

        private boolean contains(int[] values, int value) {
            for (int candidate : values) {
                if (candidate == value) return true;
            }
            return false;
        }
    }
}
