package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class ChimneyBlockEntity extends BlockEntity {
    private static final int SUBSCRIBE_AMOUNT = 1_000_000;
    private static final String[] ACCEPTED_FLUIDS = {
            "flue",
            "smoke",
            "smoke_leaded",
            "smoke_poison"
    };

    private final boolean industrial;
    private int onTicks;

    public ChimneyBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, blockState.is(HbmBlocks.CHIMNEY_INDUSTRIAL.get()));
    }

    public ChimneyBlockEntity(BlockPos pos, BlockState blockState, boolean industrial) {
        super(HbmBlockEntities.CHIMNEY.get(), pos, blockState);
        this.industrial = industrial;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChimneyBlockEntity chimney) {
        if (!level.isClientSide && level.getGameTime() % 20L == 0L) {
            chimney.pullSmokeFromLegacyPorts(level);
        }
        if (chimney.onTicks > 0) {
            chimney.onTicks--;
            if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2L == 0L) {
                double y = pos.getY() + (chimney.industrial ? 22.0D : 12.0D);
                serverLevel.sendParticles(chimney.industrial
                                ? HbmParticleTypes.CHIMNEY_SMOKE_INDUSTRIAL.get()
                                : HbmParticleTypes.CHIMNEY_SMOKE_BRICK.get(),
                        pos.getX() + 0.5D, y, pos.getZ() + 0.5D, 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        // Legacy chimney ports are exposed through the outer dummy blocks, not the core block.
        return EmptyFluidHandler.INSTANCE;
    }

    public boolean active() {
        return this.onTicks > 0;
    }

    public boolean industrial() {
        return this.industrial;
    }

    private void pullSmokeFromLegacyPorts(Level level) {
        for (String name : ACCEPTED_FLUIDS) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                continue;
            }
            pullFromLegacyPort(level, Direction.EAST, fluid);
            pullFromLegacyPort(level, Direction.WEST, fluid);
            pullFromLegacyPort(level, Direction.SOUTH, fluid);
            pullFromLegacyPort(level, Direction.NORTH, fluid);
        }
    }

    private void pullFromLegacyPort(Level level, Direction portDirection, HbmFluidDefinition fluid) {
        BlockPos pipePos = this.worldPosition.relative(portDirection, 2);
        Direction pipeAccessSide = portDirection.getOpposite();
        FluidStack drained = HbmFluidNetworks.drainFrom(level, pipePos, pipeAccessSide, fluid, SUBSCRIBE_AMOUNT, this.worldPosition, true);
        if (!drained.isEmpty()) {
            acceptSmoke(drained);
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new SmokeHandler();
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (side == null || side.getAxis().isVertical()) {
            return false;
        }
        BlockPos diff = queriedPos.subtract(this.worldPosition);
        return diff.getY() == 0
                && ((diff.getX() == 1 && diff.getZ() == 0 && side == Direction.EAST)
                || (diff.getX() == -1 && diff.getZ() == 0 && side == Direction.WEST)
                || (diff.getX() == 0 && diff.getZ() == 1 && side == Direction.SOUTH)
                || (diff.getX() == 0 && diff.getZ() == -1 && side == Direction.NORTH));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Industrial", industrial);
        tag.putInt("OnTicks", onTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        onTicks = tag.getInt("OnTicks");
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

    private double pollutionModifier() {
        return industrial ? 0.1D : 0.25D;
    }

    private int acceptSmoke(FluidStack resource) {
        HbmPollutionType type = smokeType(resource);
        if (type == null || resource.isEmpty() || level == null) {
            return 0;
        }
        int amount = resource.getAmount();
        onTicks = 20;
        captureAsh(type, amount);
        double modifier = pollutionModifier();
        if (type == HbmPollutionType.SOOT) {
            modifier *= HbmConfig.POLLUTION_SMOKESTACK_SOOT_MULTIPLIER.get();
        }
        HbmPollution.emitSmoke(level, worldPosition, type, amount / 100.0D, modifier);
        setChanged();
        syncStatus();
        return amount;
    }

    private void syncStatus() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private void captureAsh(HbmPollutionType type, int amount) {
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof AshpitBlockEntity ashpit)) {
            return;
        }
        ashpit.addFlyAsh(amount);
        if (industrial && type == HbmPollutionType.SOOT) {
            ashpit.addSoot(amount);
        }
    }

    private final class SmokeHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1_000_000;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && smokeType(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            HbmPollutionType type = smokeType(resource);
            if (type == null || resource.isEmpty()) {
                return 0;
            }
            if (action.execute()) {
                return acceptSmoke(resource);
            }
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    @Nullable
    private static HbmPollutionType smokeType(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(null);
        if (fluid == null) {
            return null;
        }
        return switch (fluid.name()) {
            case "smoke" -> HbmPollutionType.SOOT;
            case "flue" -> HbmPollutionType.SOOT;
            case "smoke_leaded" -> HbmPollutionType.HEAVYMETAL;
            case "smoke_poison" -> HbmPollutionType.POISON;
            default -> null;
        };
    }

    private enum EmptyFluidHandler implements IFluidHandler {
        INSTANCE;

        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
