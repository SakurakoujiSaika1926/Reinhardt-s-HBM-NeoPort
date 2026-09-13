package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.api.machine.RadarCommandReceiver;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.entity.LegacyLauncherMissileEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.*;
import com.reinhardt.hbm.menu.LauncherMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.*;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** 1.7.10 LaunchPad[Base/Large/Rusted], CompactLauncher and LaunchTable state machines. */
public class LauncherBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory,
        WorldlyContainer, MenuProvider, IFluidHandler, RadarCommandReceiver {
    public enum Kind { PAD_SMALL, PAD_RUSTED, PAD_LARGE, COMPACT, TABLE, SOYUZ }
    public static final long MAX_POWER = 100_000L;
    public static final int STATE_MISSING = 0, STATE_LOADING = 1, STATE_READY = 2;
    private final Kind kind;
    private final NonNullList<ItemStack> items;
    private final HbmFluidTank fuelTank, oxidizerTank;
    private long power;
    private int solidFuel, state, delay, redstonePower, prevRedstonePower;
    private final Set<BlockPos> activatedBlocks = new HashSet<>();
    private boolean missileLoaded, erected, readyToLoad, scheduleErect, liftMoving, erectorMoving;
    private int formFactor = -1, scaffoldHeight = 10;
    private MissilePartItem.Size tableSize = MissilePartItem.Size.SIZE_10;
    private float lift = 1F, erector = 90F, prevLift = 1F, prevErector = 90F, syncLift = 1F, syncErector = 90F;
    private int animationSync;
    private String customName = "";

    private final ContainerData data = new ContainerData() {
        public int get(int i) { return switch (i) {
            case 0 -> (int)power; case 1 -> solidFuel; case 2 -> fuelTank.amount();
            case 3 -> oxidizerTank.amount(); case 4 -> fuelTank.type().oldId(); case 5 -> oxidizerTank.type().oldId();
            case 6 -> state; case 7 -> missileLoaded ? 1 : 0; case 8 -> tableSize.ordinal();
            default -> throw new IndexOutOfBoundsException(i);
        }; }
        public void set(int i, int v) { switch (i) {
            case 0 -> power = v; case 1 -> solidFuel = v; case 2 -> fuelTank.setAmount(v);
            case 3 -> oxidizerTank.setAmount(v); case 4 -> setSyncedType(fuelTank, v);
            case 5 -> setSyncedType(oxidizerTank, v); case 6 -> state = v;
            case 7 -> missileLoaded = v != 0; case 8 -> tableSize = MissilePartItem.Size.values()[v];
            default -> throw new IndexOutOfBoundsException(i);
        } }
        public int getCount() { return 9; }
    };

    public LauncherBlockEntity(BlockPos pos, BlockState state) { this(pos, state, Kind.PAD_SMALL); }
    public LauncherBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(HbmBlockEntities.LAUNCHER.get(), pos, state);
        this.kind = kind;
        items = NonNullList.withSize(kind == Kind.PAD_RUSTED ? 4 : custom() ? 8 : 7, ItemStack.EMPTY);
        int capacity = kind == Kind.PAD_RUSTED ? 0 : kind == Kind.TABLE ? 100_000 : kind == Kind.COMPACT ? 25_000 : 24_000;
        fuelTank = new HbmFluidTank(capacity);
        oxidizerTank = new HbmFluidTank(capacity);
        delay = kind == Kind.PAD_LARGE ? 20 : 0;
    }
    private boolean custom() { return kind == Kind.COMPACT || kind == Kind.TABLE; }
    public Kind kind() { return kind; }
    public ContainerData menuData() { return data; }
    public long power() { return power; }
    public int solidFuel() { return solidFuel; }
    public int solidCapacity() { return kind == Kind.TABLE ? 100_000 : kind == Kind.COMPACT ? 25_000 : 0; }
    public int state() { return state; }
    public boolean missileLoaded() { return missileLoaded; }
    /** Exact state used by the 1.7.10 silo ruin's pre-loaded rusted pad. */
    public void setMissileLoadedFromStructure(boolean missileLoaded) {
        if (this.kind != Kind.PAD_RUSTED) {
            throw new IllegalStateException("Only the rusted launch pad may receive legacy structure missile state");
        }
        this.missileLoaded = missileLoaded;
        setChanged();
    }
    public boolean erected() { return erected; }
    public boolean readyToLoad() { return readyToLoad; }
    public int formFactor() { return formFactor; }
    public float lift(float partialTick) { return Mth.lerp(partialTick, prevLift, lift); }
    public float erector(float partialTick) { return Mth.lerp(partialTick, prevErector, erector); }
    public boolean liftMoving() { return liftMoving; }
    public boolean erectorMoving() { return erectorMoving; }
    public MissilePartItem.Size tableSize() { return tableSize; }
    public int scaffoldHeight() { return scaffoldHeight; }
    public void setScaffoldHeight(int height) { scaffoldHeight = height; }
    public HbmFluidTank fuelTank() { return fuelTank; }
    public HbmFluidTank oxidizerTank() { return oxidizerTank; }
    public Direction facing() { return getBlockState().hasProperty(LargeMachineBlock.FACING)
            ? getBlockState().getValue(LargeMachineBlock.FACING) : Direction.NORTH; }

    public static void tick(Level level, BlockPos pos, BlockState state, LauncherBlockEntity be) {
        if (level.isClientSide) {
            be.prevLift = be.lift; be.prevErector = be.erector;
            if (be.animationSync > 0) {
                be.lift += (be.syncLift - be.lift) / be.animationSync;
                be.erector += (be.syncErector - be.erector) / be.animationSync--;
            } else { be.lift = be.syncLift; be.erector = be.syncErector; }
            return;
        }
        if (be.kind == Kind.PAD_RUSTED) be.tickRedstoneEdge();
        else if (be.custom()) {
            be.setFuelTypes();
            PowerNetworkManager.tickFromEndpoint(level, be);
            be.drainContainers();
            be.power = BatteryPackItem.dischargeIntoMachine(be.items.get(5), be.power, MAX_POWER);
            if (be.items.get(4).is(HbmItems.ROCKET_FUEL.get()) && be.solidFuel + 250 <= be.solidCapacity()) {
                be.items.get(4).shrink(1); be.solidFuel += 250;
            }
            int radius = be.kind == Kind.TABLE ? 4 : 1;
            outer: for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (level.hasNeighborSignal(pos.offset(x, 0, z)) && be.canLaunch()) {
                    be.launchFromDesignator(); break outer;
                }
            }
        } else {
            if (be.kind == Kind.PAD_SMALL) be.tickSilo();
            if (be.kind == Kind.PAD_LARGE) be.tickLargePad();
            PowerNetworkManager.tickFromEndpoint(level, be);
            be.tickRedstoneEdge();
            be.power = BatteryPackItem.dischargeIntoMachine(be.items.get(2), be.power, MAX_POWER);
            be.drainContainers();
            if (be.validMissile(be.items.get(0))) be.setFuelTypes();
        }
        be.sync();
    }
    private void tickSilo() {
        if (delay > 0) delay--;
        if (!validMissile(items.get(0)) || !hasFuel()) { delay = 100; state = STATE_MISSING; }
        else state = delay > 0 ? STATE_LOADING : STATE_READY;
    }
    private void tickLargePad() {
        prevLift = lift; prevErector = erector;
        float rotationSpeed = 1.5F, liftSpeed = 0.025F;
        if (validMissile(items.get(0))) {
            var factor = ((LegacyMissileItem)items.get(0).getItem()).formFactor();
            formFactor = factor.ordinal();
            if (factor == LegacyMissileItem.FormFactor.ATLAS || factor == LegacyMissileItem.FormFactor.HUGE) {
                rotationSpeed /= 2F; liftSpeed /= 2F;
            }
            if (erector == 90F && lift == 1F) readyToLoad = true;
        } else { readyToLoad = false; erected = false; delay = 20; }
        if (power >= 75_000) {
            if (delay > 0) {
                delay--;
                if (delay < 10 && scheduleErect) { erected = true; scheduleErect = false; }
                if (items.get(0).isEmpty() || !readyToLoad) retract(rotationSpeed, liftSpeed);
            } else if (!erected && readyToLoad) {
                state = STATE_LOADING;
                if (erector != 0F) {
                    erector = Math.max(erector - rotationSpeed, 0F);
                    if (erector == 0F) delay = 20;
                } else if (lift > 0F) {
                    lift = Math.max(lift - liftSpeed, 0F);
                    if (lift == 0F) { scheduleErect = true; delay = 20; }
                }
            } else retract(rotationSpeed, liftSpeed);
        }
        if (!hasFuel() || !validMissile(items.get(0))) state = STATE_MISSING;
        if (erected && canLaunch()) state = STATE_READY;
        boolean wasLiftMoving = liftMoving, wasErectorMoving = erectorMoving;
        liftMoving = lift != prevLift; erectorMoving = erector != prevErector;
        if (wasLiftMoving && !liftMoving) playSound("door.wgh_stop", 2F);
        if (wasErectorMoving && !erectorMoving) playSound("door.garage_stop", 2F);
    }
    private void retract(float rotationSpeed, float liftSpeed) {
        if (erector < 90F) {
            erector = Math.min(erector + rotationSpeed, 90F);
            if (erector == 90F) delay = 20;
        } else if (lift < 1F) {
            lift = Math.min(lift + liftSpeed, 1F);
            // Literal old test is erector == 1, not lift == 1.
            if (erector == 1F) { readyToLoad = true; delay = 20; }
        }
    }
    public void updateRedstonePower(BlockPos pos) {
        if (level == null || level.isClientSide || custom()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && activatedBlocks.add(pos.immutable())) {
            if (redstonePower == -1) redstonePower = 0;
            redstonePower++;
        } else if (!powered && activatedBlocks.remove(pos)) {
            if (--redstonePower == 0) redstonePower = -1;
        }
        setChanged();
    }
    private void tickRedstoneEdge() {
        if (redstonePower > 0 && prevRedstonePower <= 0) launchFromDesignator();
        prevRedstonePower = redstonePower;
    }
    public boolean validMissile(ItemStack stack) {
        if (kind == Kind.PAD_RUSTED) return false;
        if (custom()) {
            CustomMissileData missile = CustomMissileData.read(stack);
            return missile != null && missile.fuselageDefinition().top()
                    == (kind == Kind.COMPACT ? MissilePartItem.Size.SIZE_10 : tableSize);
        }
        return stack.getItem() instanceof LegacyMissileItem m && m.launchable();
    }
    public int fuelRequirement() {
        if (custom()) { var m = CustomMissileData.read(items.get(0)); return m == null ? 0 : m.fuelRequired(); }
        return items.get(0).getItem() instanceof LegacyMissileItem m ? m.fuelCapacity() : 0;
    }
    public int solidState() {
        var m = CustomMissileData.read(items.get(0));
        return m == null || m.fuselageDefinition().fuel() != MissilePartItem.Fuel.SOLID
                ? -1 : solidFuel >= m.fuelRequired() ? 1 : 0;
    }
    public int liquidState() {
        var m = CustomMissileData.read(items.get(0));
        return m == null || m.fuselageDefinition().fuel() == MissilePartItem.Fuel.SOLID
                ? -1 : fuelTank.amount() >= m.fuelRequired() ? 1 : 0;
    }
    public int oxidizerState() {
        var m = CustomMissileData.read(items.get(0));
        return m == null || m.fuselageDefinition().fuel() == MissilePartItem.Fuel.SOLID
                || m.fuselageDefinition().fuel() == MissilePartItem.Fuel.XENON
                ? -1 : oxidizerTank.amount() >= m.fuelRequired() ? 1 : 0;
    }
    public int gaugeState(int tank) {
        if (!(items.get(0).getItem() instanceof LegacyMissileItem m) || m.fuel() == LegacyMissileItem.Fuel.SOLID) return 0;
        return (tank == 0 ? fuelTank : oxidizerTank).amount() >= m.fuelCapacity() ? 1 : -1;
    }
    public boolean hasFuel() {
        if (custom()) return solidState() != 0 && liquidState() != 0 && oxidizerState() != 0;
        return power >= 75_000 && items.get(0).getItem() instanceof LegacyMissileItem
                && fuelTank.amount() >= fuelRequirement() && oxidizerTank.amount() >= fuelRequirement();
    }
    public static boolean isDesignator(ItemStack stack) {
        return stack.getItem() instanceof LegacyCoordinateDesignatorItem || stack.getItem() instanceof LegacyRangeDesignatorItem;
    }
    public boolean hasDesignator() { return target() != null; }
    @Nullable private BlockPos target() {
        ItemStack stack = items.get(kind == Kind.PAD_RUSTED ? 3 : 1);
        return isDesignator(stack) ? LegacyRangeDesignatorItem.target(stack) : null;
    }
    public boolean canLaunch() {
        if (kind == Kind.PAD_RUSTED) return missileLoaded && itemIs(items.get(1), "launch_code")
                && itemIs(items.get(2), "launch_key") && hasDesignator();
        if (!validMissile(items.get(0)) || power < 75_000 || !hasFuel()) return false;
        return switch (kind) {
            case PAD_SMALL -> delay <= 0; case PAD_LARGE -> erected && readyToLoad;
            case COMPACT -> hasDesignator(); case TABLE -> true; default -> false;
        };
    }
    public boolean launchFromDesignator() {
        if (!canLaunch()) return false;
        BlockPos target = target();
        boolean abm = itemIs(items.get(0), "missile_anti_ballistic") && !custom();
        if (target == null && !abm) return false;
        return launchTo(target == null ? worldPosition.getX() : target.getX(),
                target == null ? worldPosition.getZ() : target.getZ(), null);
    }
    private boolean launchTo(int x, int z, @Nullable Entity tracking) {
        if (level == null || level.isClientSide || !canLaunch()) return false;
        ItemStack stack = kind == Kind.PAD_RUSTED ? legacyItem("missile_doomsday_rusted") : items.get(0).copy();
        CustomMissileData customMissile = CustomMissileData.read(stack);
        if (customMissile != null) {
            Vec3 error = new Vec3(worldPosition.getX() - x, 0, worldPosition.getZ() - z)
                    .scale(customMissile.inaccuracy()).yRot(level.random.nextFloat() * 360F);
            x += (int)error.x; z += (int)error.z;
        }
        double launchY = custom() ? 2.5 : kind == Kind.PAD_LARGE ? 2 : 1;
        Entity missile = LegacyLauncherMissileEntity.create(level, stack, worldPosition.getX() + 0.5,
                worldPosition.getY() + launchY, worldPosition.getZ() + 0.5, x, z, facing());
        if (tracking != null && missile instanceof LegacyLauncherMissileEntity launched) launched.setTrackingTarget(tracking);
        if (!level.addFreshEntity(missile)) return false;
        // Commit only after spawning the actual missile, never consume on a rejected launch.
        if (kind == Kind.PAD_RUSTED) { missileLoaded = false; items.get(1).shrink(1); }
        else {
            int amount = fuelRequirement();
            if (customMissile != null && customMissile.fuselageDefinition().fuel() == MissilePartItem.Fuel.SOLID) solidFuel -= amount;
            else {
                fuelTank.setAmount(fuelTank.amount() - amount);
                if (customMissile == null || customMissile.fuselageDefinition().fuel() != MissilePartItem.Fuel.XENON)
                    oxidizerTank.setAmount(oxidizerTank.amount() - amount);
            }
            power -= 75_000;
            if (custom()) items.set(0, ItemStack.EMPTY); else items.get(0).shrink(1);
            if (kind == Kind.PAD_SMALL) delay = 100;
            if (kind == Kind.PAD_LARGE) erected = false;
        }
        level.playSound(null, worldPosition, HbmSoundEvents.WEAPON_MISSILE_TAKEOFF.get(), SoundSource.BLOCKS, custom() ? 10F : 2F, 1F);
        sync();
        return true;
    }
    @Override public boolean sendCommandPosition(int x, int y, int z) { return kind != Kind.PAD_RUSTED && launchTo(x, z, null); }
    @Override public boolean sendCommandEntity(Entity target) {
        if (kind == Kind.PAD_RUSTED) return false;
        // Compact/table both used posX for Z in their 1.7.10 radar command.
        return launchTo(Mth.floor(target.getX()), Mth.floor(custom() ? target.getX() : target.getZ()), custom() ? null : target);
    }
    public void releaseMissile() {
        if (kind == Kind.PAD_RUSTED && missileLoaded && items.get(0).isEmpty()) {
            missileLoaded = false; items.set(0, legacyItem("missile_doomsday_rusted")); sync();
        }
    }
    public void setTableSize(MissilePartItem.Size size) {
        if (kind != Kind.TABLE || (size != MissilePartItem.Size.SIZE_10 && size != MissilePartItem.Size.SIZE_15 && size != MissilePartItem.Size.SIZE_20)) return;
        tableSize = size;
        sync();
    }
    public void cycleTableSize() {
        if (kind != Kind.TABLE) return;
        tableSize = switch (tableSize) {
            case SIZE_10 -> MissilePartItem.Size.SIZE_15; case SIZE_15 -> MissilePartItem.Size.SIZE_20;
            case SIZE_20 -> MissilePartItem.Size.SIZE_10;
            default -> throw new IllegalStateException("Invalid launch table size " + tableSize);
        };
        sync();
    }
    private static ItemStack legacyItem(String id) { return new ItemStack(BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id(id)).orElseThrow()); }
    private static boolean itemIs(ItemStack stack, String id) { return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id(id)); }
    private static HbmFluidDefinition fluid(String name) { return HbmFluids.byName(name).orElseThrow(); }
    private static void setSyncedType(HbmFluidTank tank, int id) {
        int amount = tank.amount(); tank.setType(HbmFluids.byOldId(id).orElseThrow()); tank.setAmount(amount);
    }
    private void setFuelTypes() {
        if (custom()) {
            var m = CustomMissileData.read(items.get(0));
            if (m == null) return;
            switch (m.fuselageDefinition().fuel()) {
                case KEROSENE -> types("kerosene", "peroxide");
                case HYDROGEN -> types("hydrogen", "oxygen");
                case BALEFIRE -> types("balefire", "peroxide");
                case XENON -> fuelTank.setType(fluid("xenon")); case SOLID -> { }
            }
        } else if (items.get(0).getItem() instanceof LegacyMissileItem m) {
            switch (m.fuel()) {
                case ETHANOL_PEROXIDE -> types("ethanol", "peroxide");
                case KEROSENE_PEROXIDE -> types("kerosene", "peroxide");
                case KEROSENE_LOXY -> types("kerosene", "oxygen");
                case JETFUEL_LOXY -> types("kerosene_reform", "oxygen"); case SOLID -> { }
            }
        }
    }
    private void types(String fuel, String oxidizer) { fuelTank.setType(fluid(fuel)); oxidizerTank.setType(fluid(oxidizer)); }
    private void drainContainers() {
        drainContainer(custom() ? 2 : 3, custom() ? 6 : 4, fuelTank);
        drainContainer(custom() ? 3 : 5, custom() ? 7 : 6, oxidizerTank);
    }
    private void drainContainer(int input, int output, HbmFluidTank tank) {
        if (tank.type().isNone()) return;
        HbmFluidContainerTransfer.drainIntoTank(items.get(input), tank, f -> f == tank.type(),
                out -> items.get(output).isEmpty() || (ItemStack.isSameItemSameComponents(items.get(output), out)
                        && items.get(output).getCount() + out.getCount() <= items.get(output).getMaxStackSize()),
                out -> { if (items.get(output).isEmpty()) items.set(output, out.copy()); else items.get(output).grow(out.getCount()); });
    }
    public record Port(BlockPos pos, Direction face) { }
    public List<Port> ports() {
        List<Port> ports = new ArrayList<>();
        if (kind == Kind.PAD_RUSTED) return ports;
        if (kind == Kind.TABLE) {
            for (int i = -4; i <= 4; i++) {
                ports.add(new Port(worldPosition.offset(i, 0, 5), Direction.SOUTH));
                ports.add(new Port(worldPosition.offset(i, 0, -5), Direction.NORTH));
                ports.add(new Port(worldPosition.offset(5, 0, i), Direction.EAST));
                ports.add(new Port(worldPosition.offset(-5, 0, i), Direction.WEST));
            }
        } else {
            int edge = kind == Kind.PAD_LARGE ? 5 : 2, offset = kind == Kind.PAD_LARGE ? 2 : 1;
            for (int i : new int[] {-offset, offset}) {
                ports.add(new Port(worldPosition.offset(edge, 0, i), Direction.EAST));
                ports.add(new Port(worldPosition.offset(-edge, 0, i), Direction.WEST));
                ports.add(new Port(worldPosition.offset(i, 0, edge), Direction.SOUTH));
                ports.add(new Port(worldPosition.offset(i, 0, -edge), Direction.NORTH));
            }
            if (kind == Kind.COMPACT) for (int x : new int[] {-1, 1}) for (int z : new int[] {-1, 1})
                ports.add(new Port(worldPosition.offset(x, -1, z), Direction.DOWN));
        }
        return ports;
    }
    @Nullable public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null || ports().stream().noneMatch(p -> p.face == side && p.pos.relative(side.getOpposite()).equals(queriedPos))) return null;
        return this;
    }
    @Override public BlockPos getPowerPos() { return worldPosition; }
    @Override public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) { return ports().stream().map(Port::pos).toList(); }
    @Override public boolean canConnectPower(LevelAccessor level, BlockPos pos, Direction side) { return ports().contains(new Port(pos, side)); }
    @Override public long getAvailableOutput() { return 0; }
    @Override public long getRequestedInput() { return kind == Kind.PAD_RUSTED ? 0 : MAX_POWER - power; }
    @Override public void applyPower(long used, long received) { power = Math.min(MAX_POWER, power + Math.max(0, received)); setChanged(); }
    @Override public Component getPowerStatus() { return Component.literal(power + " / " + MAX_POWER); }
    @Override public int getTanks() { return 2; }
    @Override public FluidStack getFluidInTank(int tank) { return (tank == 0 ? fuelTank : oxidizerTank).getFluidInTank(0); }
    @Override public int getTankCapacity(int tank) { return (tank == 0 ? fuelTank : oxidizerTank).capacity(); }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        HbmFluidTank target = tank == 0 ? fuelTank : oxidizerTank;
        return !target.type().isNone() && HbmFluids.fromNeoFluid(stack.getFluid()).filter(f -> f == target.type()).isPresent();
    }
    @Override public int fill(FluidStack stack, FluidAction action) {
        int filled = isFluidValid(0, stack) ? fuelTank.fill(stack, action) : isFluidValid(1, stack) ? oxidizerTank.fill(stack, action) : 0;
        if (filled > 0 && action.execute()) setChanged();
        return filled;
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int i) { return items.get(i); }
    @Override public ItemStack removeItem(int i, int amount) { ItemStack out = items.get(i).split(amount); setChanged(); return out; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack out = items.get(i); items.set(i, ItemStack.EMPTY); return out; }
    @Override public void setItem(int i, ItemStack stack) { items.set(i, stack); setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack stack) { return !custom() && kind != Kind.PAD_RUSTED && i == 0 && validMissile(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return kind == Kind.PAD_RUSTED ? new int[0] : new int[] {0}; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack stack, @Nullable Direction side) { return canPlaceItem(i, stack); }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack stack, Direction side) { return false; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.replaceAll(stack -> ItemStack.EMPTY); setChanged(); }
    @Override public Component getDisplayName() {
        return customName.isEmpty() ? Component.translatable(switch (kind) {
            case COMPACT -> "block.reinhardtshbm.compact_launcher";
            case TABLE -> "block.reinhardtshbm.launch_table";
            case PAD_RUSTED -> "block.reinhardtshbm.launch_pad_rusted";
            default -> "block.reinhardtshbm.launch_pad";
        }) : Component.literal(customName);
    }
    public void setCustomName(String name) { customName = name; setChanged(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new LauncherMenu(id, inv, this, data); }
    @Override public void dropContents(Level level, BlockPos pos) { for (ItemStack stack : items) if (!stack.isEmpty()) Block.popResource(level, pos, stack); clearContent(); }
    private void playSound(String id, float volume) {
        level.playSound(null, worldPosition, BuiltInRegistries.SOUND_EVENT.getOptional(ReinhardtsHBM.id(id)).orElseThrow(), SoundSource.BLOCKS, volume, 1F);
    }
    public void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("power", power); tag.putInt("solidfuel", solidFuel);
        tag.put("Fuel", fuelTank.save()); tag.put("Oxidizer", oxidizerTank.save());
        tag.putBoolean("missileLoaded", missileLoaded); tag.putString("name", customName);
        tag.putInt("padSize", tableSize.ordinal()); tag.putInt("height", scaffoldHeight);
        tag.putBoolean("erected", erected); tag.putBoolean("readyToLoad", readyToLoad);
        tag.putFloat("lift", lift); tag.putFloat("erector", erector); tag.putInt("formFactor", formFactor);
        tag.putInt("redstonePower", redstonePower); tag.putInt("prevRedstonePower", prevRedstonePower);
        tag.putLongArray("activatedBlocks", activatedBlocks.stream().mapToLong(BlockPos::asLong).toArray());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.replaceAll(stack -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        power = tag.getLong("power"); solidFuel = tag.getInt("solidfuel");
        fuelTank.load(tag.getCompound("Fuel")); oxidizerTank.load(tag.getCompound("Oxidizer"));
        missileLoaded = tag.getBoolean("missileLoaded"); customName = tag.getString("name");
        if (tag.contains("padSize")) tableSize = MissilePartItem.Size.values()[tag.getInt("padSize")];
        if (tag.contains("height")) scaffoldHeight = tag.getInt("height");
        erected = tag.getBoolean("erected"); readyToLoad = tag.getBoolean("readyToLoad");
        if (tag.contains("lift")) lift = tag.getFloat("lift");
        if (tag.contains("erector")) erector = tag.getFloat("erector");
        if (tag.contains("formFactor")) formFactor = tag.getInt("formFactor");
        redstonePower = tag.getInt("redstonePower"); prevRedstonePower = tag.getInt("prevRedstonePower");
        activatedBlocks.clear(); for (long pos : tag.getLongArray("activatedBlocks")) activatedBlocks.add(BlockPos.of(pos));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag, registries);
        tag.putInt("State", state); tag.putBoolean("LiftMoving", liftMoving); tag.putBoolean("ErectorMoving", erectorMoving);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        float oldLift = lift, oldErector = erector;
        loadAdditional(tag, registries);
        syncLift = lift; syncErector = erector; lift = oldLift; erector = oldErector;
        animationSync = 3; state = tag.getInt("State");
        liftMoving = tag.getBoolean("LiftMoving"); erectorMoving = tag.getBoolean("ErectorMoving");
    }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
