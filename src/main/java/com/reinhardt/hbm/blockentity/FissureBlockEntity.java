package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FissureBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FissureBlockEntity extends BlockEntity {
    private final HbmFluidTank lava = new HbmFluidTank(lava(), 1_000);

    public FissureBlockEntity(BlockPos pos, BlockState state) { super(HbmBlockEntities.FISSURE.get(), pos, state); lava.setAmount(1_000); }

    public static void tick(Level level, BlockPos pos, BlockState state, FissureBlockEntity fissure) {
        if (level.isClientSide) return;
        fissure.lava.setAmount(1_000);
        IFluidHandler handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
        if (handler != null) handler.fill(HbmFluids.toNeoStack(lava(), 1_000), IFluidHandler.FluidAction.EXECUTE);
    }

    public IFluidHandler fluidHandler(Direction side) {
        return side != Direction.UP ? null : new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tank) { return lava.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { return lava.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
            @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return drain(resource.getAmount(), action); }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { var drained = lava.drain(lava(), maxDrain, action.simulate()); return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount()); }
        };
    }
    private static HbmFluidDefinition lava() { return HbmFluids.byName("lava").orElse(HbmFluids.none()); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.put("Lava", lava.save()); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); lava.load(tag.getCompound("Lava")); lava.setAmount(1_000); }
}
