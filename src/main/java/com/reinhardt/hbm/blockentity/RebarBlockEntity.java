package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.block.ConcreteColoredBlock;
import com.reinhardt.hbm.block.VinylTileBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stores the selected concrete and the original 1,000 mB curing progress. */
public final class RebarBlockEntity extends BlockEntity {
    public static final int CONCRETE_REQUIRED = 1_000;
    private static final int MAX_NETWORK_FILL_PER_REBAR = 50;
    private final HbmFluidTank concrete = new HbmFluidTank(HbmFluids.byName("concrete").orElse(HbmFluids.none()), CONCRETE_REQUIRED);
    private BlockState targetState = HbmBlocks.CONCRETE_REBAR.get().defaultBlockState();
    private int targetMeta;

    public RebarBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.REBAR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RebarBlockEntity rebar) {
        if (level.isClientSide || rebar.concrete.amount() < CONCRETE_REQUIRED) return;
        level.setBlock(pos, rebar.targetState, Block.UPDATE_ALL);
    }

    public void setup(ItemStack selected) {
        if (selected.getItem() instanceof BlockItem blockItem && blockItem.getBlock() != HbmBlocks.REBAR.get()) {
            this.targetMeta = selected.getItem() instanceof com.reinhardt.hbm.item.ConcreteColoredBlockItem
                    ? ConcreteColoredBlock.meta(selected) : 0;
            this.targetState = blockItem.getBlock().defaultBlockState();
            if (this.targetState.hasProperty(ConcreteColoredBlock.META)) {
                this.targetState = this.targetState.setValue(ConcreteColoredBlock.META, this.targetMeta);
            } else if (this.targetState.hasProperty(VinylTileBlock.META)) {
                this.targetState = this.targetState.setValue(VinylTileBlock.META, Math.min(1, Math.max(0, this.targetMeta)));
            }
            sync();
        }
    }

    public int progress() {
        return concrete.amount();
    }

    public BlockState targetState() {
        return targetState;
    }

    public IFluidHandler fluidHandler() {
        return new ConcreteHandler();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Concrete", concrete.save());
        tag.putString("Target", BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString());
        tag.putInt("TargetMeta", targetMeta);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        concrete.load(tag.getCompound("Concrete"));
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Target"));
        Block block = id == null ? HbmBlocks.CONCRETE_REBAR.get() : BuiltInRegistries.BLOCK.get(id);
        targetMeta = tag.getInt("TargetMeta");
        targetState = block == HbmBlocks.REBAR.get() ? HbmBlocks.CONCRETE_REBAR.get().defaultBlockState() : block.defaultBlockState();
        if (targetState.hasProperty(ConcreteColoredBlock.META)) {
            targetState = targetState.setValue(ConcreteColoredBlock.META, targetMeta);
        } else if (targetState.hasProperty(VinylTileBlock.META)) {
            targetState = targetState.setValue(VinylTileBlock.META, Math.min(1, Math.max(0, targetMeta)));
        }
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

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.invalidateCapabilities(worldPosition);
        }
    }

    /**
     * Mirrors BlockRebar.transferFluid: one injection serves every connected
     * rebar at the lowest Y level, equalising them with a 50 mB per-frame cap.
     */
    private int distributeConcrete(int offered, boolean simulate) {
        if (level == null || level.isClientSide || offered <= 0) {
            return 0;
        }

        List<RebarBlockEntity> lowest = lowestNetworkMembers();
        if (lowest.isEmpty()) {
            return 0;
        }

        int progress = 0;
        for (RebarBlockEntity rebar : lowest) {
            progress += rebar.concrete.amount();
        }
        int capacity = CONCRETE_REQUIRED * lowest.size();
        int acceptedLimit = Math.min(Math.min(capacity - progress, offered), MAX_NETWORK_FILL_PER_REBAR * lowest.size());
        if (acceptedLimit <= 0) {
            return 0;
        }

        int target = Math.min((progress + acceptedLimit) / lowest.size(), CONCRETE_REQUIRED);
        int accepted = 0;
        HbmFluidDefinition concreteType = HbmFluids.byName("concrete").orElse(HbmFluids.none());
        for (RebarBlockEntity rebar : lowest) {
            int delta = target - rebar.concrete.amount();
            if (delta <= 0) {
                continue;
            }
            int filled = rebar.concrete.fill(concreteType, delta, simulate);
            accepted += filled;
            if (!simulate && filled > 0) {
                rebar.sync();
            }
        }
        return accepted;
    }

    private List<RebarBlockEntity> lowestNetworkMembers() {
        if (level == null) {
            return List.of();
        }

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<RebarBlockEntity> lowest = new ArrayList<>();
        pending.add(worldPosition);
        int lowestY = Integer.MAX_VALUE;

        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos) || !level.hasChunkAt(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof RebarBlockEntity rebar)) {
                continue;
            }

            if (pos.getY() < lowestY) {
                lowestY = pos.getY();
                lowest.clear();
            }
            if (pos.getY() == lowestY) {
                lowest.add(rebar);
            }
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                pending.addLast(pos.relative(direction));
            }
        }
        return lowest;
    }

    private final class ConcreteHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return concrete.getFluidInTank(tank); }
        @Override public int getTankCapacity(int tank) { return concrete.getTankCapacity(tank); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(def -> def.name().equals("concrete")).isPresent();
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) return 0;
            return distributeConcrete(resource.getAmount(), action.simulate());
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }
}
