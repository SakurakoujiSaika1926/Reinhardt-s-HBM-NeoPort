package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileMk2Block;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Current-upstream Chicago Pile MK2 compressed-air vent. */
public final class PileVentBlockEntity extends PileMk2DeviceBlockEntity {
    public static final int CAPACITY = 4_000;

    private final HbmFluidTank air = new HbmFluidTank(airType(), CAPACITY);
    private final IFluidHandler input = new AirInputHandler();
    private boolean active;
    private float fan;
    private float previousFan;

    public PileVentBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PILE_MK2_VENT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileVentBlockEntity vent) {
        if (level.isClientSide) {
            vent.previousFan = vent.fan;
            if (vent.active) {
                vent.fan += 45.0F;
                if (level.random.nextInt(20) == 0) {
                    Direction out = vent.facing();
                    level.addParticle(ParticleTypes.CLOUD,
                            pos.getX() + 0.5D + out.getStepX() * 0.625D,
                            pos.getY() + 1.0D,
                            pos.getZ() + 0.5D + out.getStepZ() * 0.625D,
                            0.0D, 0.05D, 0.0D);
                }
            }
            if (vent.fan >= 360.0F) {
                vent.fan -= 360.0F;
                vent.previousFan -= 360.0F;
            }
            return;
        }
        vent.tickServer();
    }

    private void tickServer() {
        if (level == null) return;
        ensureAirTank();
        Direction outward = facing();
        BlockPos entry = worldPosition.relative(outward.getOpposite());
        boolean nextActive = false;
        BlockState entryState = level.getBlockState(entry);
        if (entryState.getBlock() instanceof PileMk2Block
                && entryState.getValue(PileMk2Block.KIND) == PileMk2Block.Kind.AIR_IN) {
            PileMk2CoreBlockEntity core = resolveCore(entry);
            if (core != null) {
                PileMk2CoreBlockEntity.PileChannel channel = core.getVentilationChannel(entry);
                if (channel != null) {
                    channelNumber = core.getVentilationChannelNumber(channel);
                    int moved = Math.min(air.amount(), PileMk2CoreBlockEntity.PileChannel.MAX_AIR - channel.air());
                    if (moved > 0) {
                        channel.addAir(moved);
                        air.drain(airType(), moved, false);
                        nextActive = true;
                        setChanged();
                    }
                }
            }
        }
        if (!nextActive && (entryState.getBlock() instanceof PileMk2Block
                && entryState.getValue(PileMk2Block.KIND) == PileMk2Block.Kind.AIR_IN)) {
            PileMk2CoreBlockEntity core = resolveCore(entry);
            if (core == null || core.getVentilationChannel(entry) == null) channelNumber = -1;
        }
        if (active != nextActive || level.getGameTime() % 35L == 0L) {
            active = nextActive;
            sync();
        }
    }

    public HbmFluidTank tank() {
        ensureAirTank();
        return air;
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == facing() ? input : null;
    }

    public boolean active() {
        return active;
    }

    public float renderedFan(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, previousFan, fan);
    }

    private void ensureAirTank() {
        air.setCapacity(CAPACITY);
        if (air.type().isNone()) air.setType(airType());
    }

    private static HbmFluidDefinition airType() {
        return HbmFluids.byName("air").orElse(HbmFluids.none());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Air", air.save());
        tag.putBoolean("Active", active);
        tag.putFloat("Fan", fan);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Air")) air.load(tag.getCompound("Air"));
        ensureAirTank();
        active = tag.getBoolean("Active");
        fan = tag.getFloat("Fan");
        previousFan = fan;
    }

    private final class AirInputHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return air.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return air.getTankCapacity(tank); }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none()) == airType();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()
                    || HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none()) != airType()) return 0;
            int accepted = air.fill(airType(), resource.getAmount(), action.simulate());
            if (!action.simulate() && accepted > 0) sync();
            return accepted;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }
}
