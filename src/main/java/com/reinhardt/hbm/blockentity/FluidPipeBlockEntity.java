package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.item.SettingsToolItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FluidPipeBlockEntity extends BlockEntity implements FluidCopiable {
    private static final String[] LEGACY_EXHAUST_FLUIDS = {"smoke", "smoke_leaded", "smoke_poison"};

    private HbmFluidDefinition type = HbmFluids.none();
    private boolean open = true;

    public FluidPipeBlockEntity(BlockPos pos, BlockState blockState) {
        this(HbmBlockEntities.FLUID_PIPE.get(), pos, blockState);
    }

    protected FluidPipeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                   BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public HbmFluidDefinition type() {
        return type;
    }

    public boolean open() {
        return open;
    }

    public boolean canConnect(HbmFluidDefinition fluid) {
        if (!open || fluid == null) {
            return false;
        }
        if (isExhaustPipe()) {
            return isLegacyExhaustFluid(fluid);
        }
        return this.type == fluid;
    }

    /** Direction-aware counterpart used by both adjacent pipes and anchors. */
    public boolean canConnectFrom(@Nullable Direction direction, HbmFluidDefinition fluid) {
        return direction == null || canConnect(fluid);
    }

    /** Long-distance links are empty for ordinary ducts. */
    public List<BlockPos> networkLinks() {
        return List.of();
    }

    public boolean isExhaustPipe() {
        BlockState state = getBlockState();
        return state.getBlock() instanceof FluidDuctBlock duct && duct.kind().isExhaust();
    }

    public List<HbmFluidDefinition> connectableFluidTypes() {
        if (!open) {
            return List.of();
        }
        if (isExhaustPipe()) {
            ArrayList<HbmFluidDefinition> fluids = new ArrayList<>(LEGACY_EXHAUST_FLUIDS.length);
            for (String name : LEGACY_EXHAUST_FLUIDS) {
                HbmFluids.byName(name).filter(fluid -> !fluid.isNone()).ifPresent(fluids::add);
            }
            return fluids;
        }
        return this.type.isNone() ? List.of() : List.of(this.type);
    }

    public static boolean isLegacyExhaustFluid(HbmFluidDefinition fluid) {
        if (fluid == null) {
            return false;
        }
        return switch (fluid.name()) {
            case "smoke", "smoke_leaded", "smoke_poison" -> true;
            default -> false;
        };
    }

    public static HbmFluidDefinition defaultLegacyExhaustFluid() {
        return HbmFluids.byName(LEGACY_EXHAUST_FLUIDS[0]).orElse(HbmFluids.none());
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new PipeFluidHandler(side);
    }

    public void setType(HbmFluidDefinition type) {
        HbmFluidDefinition next = type == null ? HbmFluids.none() : type;
        if (this.type == next) {
            return;
        }
        this.type = next;
        markNetworkChanged();
    }

    public void setOpen(boolean open) {
        if (this.open == open) {
            return;
        }
        this.open = open;
        markNetworkChanged();
    }

    @Override
    public int[] getFluidIdsToCopy() {
        if (isExhaustPipe()) {
            return connectableFluidTypes().stream().mapToInt(HbmFluidDefinition::oldId).toArray();
        }
        return new int[]{this.type.oldId()};
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        if (isExhaustPipe()) {
            return;
        }
        HbmFluidDefinition previous = this.type;
        if (SettingsToolItem.isCtrlDown(player) && level.getBlockState(this.worldPosition).getBlock() instanceof FluidDuctBlock) {
            FluidDuctBlock.changeTypeRecursively(level, this.worldPosition, previous, fluid, 256);
        } else {
            setType(fluid);
        }
    }

    public void markNetworkChanged() {
        setChanged();
        Level level = this.level;
        if (level == null) {
            return;
        }
        HbmFluidNetworks.registerPipe(level, this.worldPosition, connectableFluidTypes(), this.open);
        BlockState state = level.getBlockState(this.worldPosition);
        level.invalidateCapabilities(this.worldPosition);
        if (state.getBlock() instanceof FluidDuctBlock duct) {
            duct.refreshConnections(level, this.worldPosition);
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = this.worldPosition.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, neighbor);
            }
        }
        BlockState refreshed = level.getBlockState(this.worldPosition);
        level.sendBlockUpdated(this.worldPosition, refreshed, refreshed, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Fluid", this.type.name());
        tag.putBoolean("Open", this.open);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.type = HbmFluids.byName(tag.getString("Fluid")).orElse(HbmFluids.none());
        this.open = !tag.contains("Open") || tag.getBoolean("Open");
        if (this.level != null) {
            HbmFluidNetworks.registerPipe(this.level, this.worldPosition, connectableFluidTypes(), this.open);
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(this.level, this.worldPosition);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = this.worldPosition.relative(direction);
                    BlockState neighborState = this.level.getBlockState(neighbor);
                    if (neighborState.getBlock() instanceof FluidDuctBlock neighborDuct) {
                        neighborDuct.refreshConnections(this.level, neighbor);
                    }
                }
            }
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null) {
            HbmFluidNetworks.registerPipe(this.level, this.worldPosition, connectableFluidTypes(), this.open);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            HbmFluidNetworks.unregisterPipe(this.level, this.worldPosition);
        }
        super.setRemoved();
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

    private final class PipeFluidHandler implements IFluidHandler {
        @Nullable
        private final Direction side;

        private PipeFluidHandler(@Nullable Direction side) {
            this.side = side;
        }

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
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return canConnectFrom(side, fluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            Level level = FluidPipeBlockEntity.this.level;
            if (level == null || resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!canConnectFrom(side, fluid)) {
                return 0;
            }
            BlockPos excluded = side == null ? null : worldPosition.relative(side);
            return HbmFluidNetworks.fillPipeNetwork(level, worldPosition, fluid, resource, excluded, action.execute());
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!canConnectFrom(side, fluid)) {
                return FluidStack.EMPTY;
            }
            return drainFromNetwork(fluid, resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !open) {
                return FluidStack.EMPTY;
            }
            for (HbmFluidDefinition fluid : connectableFluidTypes()) {
                FluidStack drained = drainFromNetwork(fluid, maxDrain, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainFromNetwork(HbmFluidDefinition fluid, int maxDrain, FluidAction action) {
            Level level = FluidPipeBlockEntity.this.level;
            if (level == null || maxDrain <= 0 || !canConnectFrom(side, fluid)) {
                return FluidStack.EMPTY;
            }
            BlockPos excluded = side == null ? null : worldPosition.relative(side);
            return HbmFluidNetworks.drainFromPipeNetwork(level, worldPosition, fluid, maxDrain, excluded, action.execute());
        }
    }
}
