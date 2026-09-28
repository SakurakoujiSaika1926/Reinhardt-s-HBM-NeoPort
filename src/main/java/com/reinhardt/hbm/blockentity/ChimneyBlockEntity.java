package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FluidDuctBlock;
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
        if (chimney.industrial) {
            chimney.tickIndustrialChimney(level, pos);
        } else {
            chimney.tickBrickChimney(level, pos);
        }
    }

    private void tickBrickChimney(Level level, BlockPos pos) {
        if (!level.isClientSide && level.getGameTime() % 20L == 0L) {
            refreshBrickChimneyPortDucts(level);
            pullBrickChimneySmoke(level);
        }
        if (this.onTicks <= 0) {
            return;
        }
        this.onTicks--;
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2L == 0L) {
            serverLevel.sendParticles(HbmParticleTypes.CHIMNEY_SMOKE_BRICK.get(),
                    pos.getX() + 0.5D, pos.getY() + 12.0D, pos.getZ() + 0.5D, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void tickIndustrialChimney(Level level, BlockPos pos) {
        if (!level.isClientSide && level.getGameTime() % 20L == 0L) {
            refreshIndustrialChimneyPortDucts(level);
            pullIndustrialChimneySmoke(level);
        }
        if (this.onTicks <= 0) {
            return;
        }
        this.onTicks--;
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2L == 0L) {
            serverLevel.sendParticles(HbmParticleTypes.CHIMNEY_SMOKE_INDUSTRIAL.get(),
                    pos.getX() + 0.5D, pos.getY() + 22.0D, pos.getZ() + 0.5D, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        // Legacy chimney ports are exposed through the outer dummy blocks, not the core block.
        return null;
    }

    public boolean active() {
        return this.onTicks > 0;
    }

    public boolean industrial() {
        return this.industrial;
    }

    public boolean canConnectLegacyExhaustPort(BlockPos queriedPos, @Nullable Direction side, HbmFluidDefinition fluid) {
        if (!isAcceptedSmokeFluid(fluid)) {
            return false;
        }
        return this.industrial
                ? allowsIndustrialChimneyFluidPort(queriedPos, side)
                : allowsBrickChimneyFluidPort(queriedPos, side);
    }

    private void refreshBrickChimneyPortDucts(Level level) {
        refreshBrickChimneyPortDuct(level, Direction.EAST);
        refreshBrickChimneyPortDuct(level, Direction.WEST);
        refreshBrickChimneyPortDuct(level, Direction.SOUTH);
        refreshBrickChimneyPortDuct(level, Direction.NORTH);
    }

    private void refreshBrickChimneyPortDuct(Level level, Direction portDirection) {
        BlockPos pipePos = this.worldPosition.relative(portDirection, 2);
        BlockState pipeState = level.getBlockState(pipePos);
        if (pipeState.getBlock() instanceof FluidDuctBlock duct && duct.kind().isExhaust()) {
            duct.refreshConnections(level, pipePos);
        }
    }

    private void refreshIndustrialChimneyPortDucts(Level level) {
        refreshIndustrialChimneyPortDuct(level, Direction.EAST);
        refreshIndustrialChimneyPortDuct(level, Direction.WEST);
        refreshIndustrialChimneyPortDuct(level, Direction.SOUTH);
        refreshIndustrialChimneyPortDuct(level, Direction.NORTH);
    }

    private void refreshIndustrialChimneyPortDuct(Level level, Direction portDirection) {
        BlockPos pipePos = this.worldPosition.relative(portDirection, 2);
        BlockState pipeState = level.getBlockState(pipePos);
        if (pipeState.getBlock() instanceof FluidDuctBlock duct && duct.kind().isExhaust()) {
            duct.refreshConnections(level, pipePos);
        }
    }

    private void pullBrickChimneySmoke(Level level) {
        for (String name : ACCEPTED_FLUIDS) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                continue;
            }
            pullFromBrickChimneyPort(level, Direction.EAST, fluid);
            pullFromBrickChimneyPort(level, Direction.WEST, fluid);
            pullFromBrickChimneyPort(level, Direction.SOUTH, fluid);
            pullFromBrickChimneyPort(level, Direction.NORTH, fluid);
        }
    }

    private void pullFromBrickChimneyPort(Level level, Direction portDirection, HbmFluidDefinition fluid) {
        BlockPos pipePos = this.worldPosition.relative(portDirection, 2);
        Direction pipeAccessSide = portDirection.getOpposite();
        FluidStack drained = HbmFluidNetworks.drainFrom(level, pipePos, pipeAccessSide, fluid, SUBSCRIBE_AMOUNT, this.worldPosition, true);
        if (!drained.isEmpty()) {
            acceptBrickChimneySmoke(drained);
        }
    }

    private void pullIndustrialChimneySmoke(Level level) {
        for (String name : ACCEPTED_FLUIDS) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                continue;
            }
            pullFromIndustrialChimneyPort(level, Direction.EAST, fluid);
            pullFromIndustrialChimneyPort(level, Direction.WEST, fluid);
            pullFromIndustrialChimneyPort(level, Direction.SOUTH, fluid);
            pullFromIndustrialChimneyPort(level, Direction.NORTH, fluid);
        }
    }

    private void pullFromIndustrialChimneyPort(Level level, Direction portDirection, HbmFluidDefinition fluid) {
        BlockPos pipePos = this.worldPosition.relative(portDirection, 2);
        Direction pipeAccessSide = portDirection.getOpposite();
        FluidStack drained = HbmFluidNetworks.drainFrom(level, pipePos, pipeAccessSide, fluid, SUBSCRIBE_AMOUNT, this.worldPosition, true);
        if (!drained.isEmpty()) {
            acceptIndustrialChimneySmoke(drained);
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (this.industrial) {
            return allowsIndustrialChimneyFluidPort(queriedPos, side)
                    ? new IndustrialChimneySmokeHandler()
                    : null;
        }
        return allowsBrickChimneyFluidPort(queriedPos, side)
                ? new BrickChimneySmokeHandler()
                : null;
    }

    private boolean allowsBrickChimneyFluidPort(BlockPos queriedPos, @Nullable Direction side) {
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

    private boolean allowsIndustrialChimneyFluidPort(BlockPos queriedPos, @Nullable Direction side) {
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

    private static boolean isAcceptedSmokeFluid(HbmFluidDefinition fluid) {
        if (fluid == null) {
            return false;
        }
        return switch (fluid.name()) {
            case "smoke", "smoke_leaded", "smoke_poison" -> true;
            default -> false;
        };
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

    private double brickChimneyPollutionModifier() {
        return HbmConfig.smokestackPollutionModifier(false);
    }

    private double industrialChimneyPollutionModifier() {
        return HbmConfig.smokestackPollutionModifier(true);
    }

    private int acceptBrickChimneySmoke(FluidStack resource) {
        HbmPollutionType type = smokeType(resource);
        if (type == null || resource.isEmpty() || level == null) {
            return 0;
        }
        int amount = resource.getAmount();
        onTicks = 20;
        captureBrickChimneyAsh(amount);
        HbmPollution.emitSmoke(level, worldPosition, type, amount / 100.0D, brickChimneyPollutionModifier());
        setChanged();
        syncStatus();
        return amount;
    }

    private int acceptIndustrialChimneySmoke(FluidStack resource) {
        HbmPollutionType type = smokeType(resource);
        if (type == null || resource.isEmpty() || level == null) {
            return 0;
        }
        int amount = resource.getAmount();
        onTicks = 20;
        captureIndustrialChimneyAsh(type, amount);
        HbmPollution.emitSmoke(level, worldPosition, type, amount / 100.0D, industrialChimneyPollutionModifier());
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

    private void captureBrickChimneyAsh(int amount) {
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof AshpitBlockEntity ashpit)) {
            return;
        }
        ashpit.addFlyAsh(amount);
    }

    private void captureIndustrialChimneyAsh(HbmPollutionType type, int amount) {
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof AshpitBlockEntity ashpit)) {
            return;
        }
        ashpit.addFlyAsh(amount);
        if (type == HbmPollutionType.SOOT) {
            ashpit.addSoot(amount);
        }
    }

    private abstract class ChimneySmokeHandler implements IFluidHandler {
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

        protected final boolean canFill(FluidStack resource) {
            HbmPollutionType type = smokeType(resource);
            return type != null && !resource.isEmpty();
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

    private final class BrickChimneySmokeHandler extends ChimneySmokeHandler {
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canFill(resource)) {
                return 0;
            }
            return action.execute() ? acceptBrickChimneySmoke(resource) : resource.getAmount();
        }
    }

    private final class IndustrialChimneySmokeHandler extends ChimneySmokeHandler {
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canFill(resource)) {
                return 0;
            }
            return action.execute() ? acceptIndustrialChimneySmoke(resource) : resource.getAmount();
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
            case "smoke_leaded" -> HbmPollutionType.HEAVYMETAL;
            case "smoke_poison" -> HbmPollutionType.POISON;
            default -> null;
        };
    }

}
