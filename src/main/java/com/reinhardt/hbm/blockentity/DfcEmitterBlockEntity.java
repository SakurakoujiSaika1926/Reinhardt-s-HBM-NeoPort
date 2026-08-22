package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.DfcEmitterMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class DfcEmitterBlockEntity extends DfcInventoryBlockEntity implements DfcLaserTarget, MenuProvider, PowerEndpoint {
    public static final long MAX_POWER = 1_000_000_000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int RANGE = 50;
    public static final int DATA_COUNT = 10;

    private final HbmFluidTank tank = new HbmFluidTank(cryogel(), TANK_CAPACITY);
    private final IFluidHandler fluidHandler = new DfcFluidHandler(new HbmFluidTank[]{this.tank}, DfcEmitterBlockEntity::isCryogel);
    private long power;
    private long lastInput;
    private int watts = 1;
    private int beam;
    private long joules;
    private long prev;
    private boolean on;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcEmitterBlockEntity.this.tank.type().oldId();
                case 1 -> DfcEmitterBlockEntity.this.tank.amount();
                case 2 -> DfcEmitterBlockEntity.this.tank.capacity();
                case 3 -> (int) DfcEmitterBlockEntity.this.power;
                case 4 -> (int) MAX_POWER;
                case 5 -> DfcEmitterBlockEntity.this.watts;
                case 6 -> (int) DfcEmitterBlockEntity.this.prev;
                case 7 -> DfcEmitterBlockEntity.this.beam;
                case 8 -> DfcEmitterBlockEntity.this.on ? 1 : 0;
                case 9 -> (int) DfcEmitterBlockEntity.this.lastInput;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcEmitterBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcEmitterBlockEntity.this.tank.setAmount(value);
                case 3 -> DfcEmitterBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 5 -> DfcEmitterBlockEntity.this.watts = value;
                case 6 -> DfcEmitterBlockEntity.this.prev = Integer.toUnsignedLong(value);
                case 7 -> DfcEmitterBlockEntity.this.beam = value;
                case 8 -> DfcEmitterBlockEntity.this.on = value != 0;
                case 9 -> DfcEmitterBlockEntity.this.lastInput = Integer.toUnsignedLong(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcEmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_EMITTER.get(), pos, blockState, 0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcEmitterBlockEntity emitter) {
        if (!level.isClientSide) {
            emitter.tickServer(level);
        }
    }

    public HbmFluidTank tank() {
        return this.tank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.fluidHandler;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int watts() {
        return this.watts;
    }

    public int beam() {
        return this.beam;
    }

    public long prev() {
        return this.prev;
    }

    public boolean on() {
        return this.on;
    }

    public void setWatts(int watts) {
        this.watts = clampWatts(watts);
        setChangedAndSync(true);
    }

    public void toggle() {
        this.on = !this.on;
        setChangedAndSync(true);
    }

    @Override
    public void addDfcEnergy(long energy, Direction beamDirection) {
        if (beamDirection.getOpposite() != facing()) {
            this.joules += Math.max(0L, energy);
            setChangedAndSync(true);
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastInput = receivedInput;
        this.power = Math.min(MAX_POWER, this.power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + "/" + MAX_POWER + " HE");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.dfc_emitter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcEmitterMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cryogel", this.tank.save());
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Watts", this.watts);
        tag.putInt("Beam", this.beam);
        tag.putLong("Joules", this.joules);
        tag.putLong("Prev", this.prev);
        tag.putBoolean("On", this.on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank.load(tag.getCompound("Cryogel"));
        if (this.tank.type().isNone() && this.tank.amount() == 0) {
            this.tank.setType(cryogel());
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.watts = clampWatts(tag.getInt("Watts"));
        this.beam = tag.getInt("Beam");
        this.joules = Math.max(0L, tag.getLong("Joules"));
        this.prev = Math.max(0L, tag.getLong("Prev"));
        this.on = tag.getBoolean("On");
    }

    private void tickServer(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.watts = clampWatts(this.watts);
        long demand = MAX_POWER * this.watts / 2000L;
        this.beam = 0;

        if ((this.joules > 0L || this.prev > 0L) && !consumeCryogel(level)) {
            return;
        }

        if (this.on) {
            if (this.power >= demand) {
                this.power -= demand;
                this.joules += this.watts * 100L;
            }
            this.prev = this.joules;

            if (this.joules > 0L) {
                emitBeam(level, this.joules * 95L / 100L);
                this.joules = 0L;
            }
        } else {
            this.joules = 0L;
            this.prev = 0L;
        }

        setChangedAndSync(level.getGameTime() % 10L == 0L || this.beam > 0);
    }

    private void emitBeam(Level level, long energy) {
        Direction direction = facing();
        long out = energy;
        for (int distance = 1; distance <= RANGE; distance++) {
            this.beam = distance;
            BlockPos targetPos = this.worldPosition.relative(direction, distance);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof DfcLaserTarget laserTarget) {
                laserTarget.addDfcEnergy(out, direction);
                break;
            }
            if (target instanceof DfcCoreBlockEntity core) {
                out = core.burn(out);
                continue;
            }

            BlockState hitState = level.getBlockState(targetPos);
            if (!hitState.isAir()) {
                if (!hitState.getFluidState().isEmpty()) {
                    level.levelEvent(1501, targetPos, 0);
                    level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
                if (hitState.getBlock().getExplosionResistance() < 6000.0F && level.random.nextInt(20) == 0) {
                    level.destroyBlock(targetPos, false);
                }
                break;
            }
        }

        AABB area = beamArea(direction, this.beam);
        for (Entity entity : level.getEntities(null, area)) {
            entity.hurt(entity.damageSources().magic(), 50.0F);
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 200));
        }
    }

    private AABB beamArea(Direction direction, int distance) {
        BlockPos end = this.worldPosition.relative(direction, Math.max(0, distance));
        double minX = Math.min(this.worldPosition.getX(), end.getX()) + 0.2D;
        double maxX = Math.max(this.worldPosition.getX(), end.getX()) + 0.8D;
        double minY = Math.min(this.worldPosition.getY(), end.getY()) + 0.2D;
        double maxY = Math.max(this.worldPosition.getY(), end.getY()) + 0.8D;
        double minZ = Math.min(this.worldPosition.getZ(), end.getZ()) + 0.2D;
        double maxZ = Math.max(this.worldPosition.getZ(), end.getZ()) + 0.8D;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean consumeCryogel(Level level) {
        if (this.tank.amount() >= 20) {
            this.tank.drain(this.tank.type(), 20, false);
            return true;
        }
        level.setBlock(this.worldPosition, Blocks.LAVA.defaultBlockState(), 3);
        return false;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
    }

    private static int clampWatts(int watts) {
        return Math.max(1, Math.min(100, watts));
    }

    private static boolean isCryogel(HbmFluidDefinition fluid) {
        return fluid != null && fluid.name().equals("cryogel");
    }

    private static HbmFluidDefinition cryogel() {
        return HbmFluids.byName("cryogel").orElse(HbmFluids.none());
    }
}
