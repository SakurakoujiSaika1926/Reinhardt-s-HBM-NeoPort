package com.reinhardt.hbm.util;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HbmFluidContainerTransfer {
    private HbmFluidContainerTransfer() {
    }

    public static boolean canDrainIntoTank(
            ItemStack input,
            HbmFluidTank tank,
            Predicate<HbmFluidDefinition> acceptsFluid,
            Predicate<ItemStack> canPlaceOutput
    ) {
        if (input.isEmpty()) {
            return false;
        }
        if (input.getItem() instanceof InfiniteFluidContainerItem infinite) {
            HbmFluidDefinition fluid = infinite.sourceFor(tank);
            return infinite.allowsPressure(tank.pressure())
                    && !fluid.isNone()
                    && acceptsFluid.test(fluid)
                    && tank.fill(fluid, infinite.amountPerTick(), infinite.isUniversal() ? tank.pressure() : 0, true) > 0;
        }
        if (tank.pressure() != 0) {
            return false;
        }
        ContainerMove move = simulateDrain(input, tank, acceptsFluid);
        return move != null && (move.output().isEmpty() || canPlaceOutput.test(move.output()));
    }

    public static boolean drainIntoTank(
            ItemStack input,
            HbmFluidTank tank,
            Predicate<HbmFluidDefinition> acceptsFluid,
            Predicate<ItemStack> canPlaceOutput,
            Consumer<ItemStack> placeOutput
    ) {
        if (input.isEmpty()) {
            return false;
        }
        if (input.getItem() instanceof InfiniteFluidContainerItem infinite) {
            HbmFluidDefinition fluid = infinite.sourceFor(tank);
            return infinite.allowsPressure(tank.pressure())
                    && !fluid.isNone()
                    && acceptsFluid.test(fluid)
                    && tank.fill(fluid, infinite.amountPerTick(), infinite.isUniversal() ? tank.pressure() : 0, false) > 0;
        }
        if (tank.pressure() != 0) {
            return false;
        }

        ContainerMove move = simulateDrain(input, tank, acceptsFluid);
        if (move == null || (!move.output().isEmpty() && !canPlaceOutput.test(move.output()))) {
            return false;
        }

        tank.fill(move.fluid(), move.amount(), false);
        input.shrink(1);
        placeOutput.accept(move.output());
        return true;
    }

    public static boolean canFillFromTank(ItemStack input, HbmFluidTank tank, Predicate<ItemStack> canPlaceOutput) {
        ContainerMove move = simulateFill(input, tank);
        return move != null && (move.output().isEmpty() || canPlaceOutput.test(move.output()));
    }

    public static boolean fillFromTank(
            ItemStack input,
            HbmFluidTank tank,
            Predicate<ItemStack> canPlaceOutput,
            Consumer<ItemStack> placeOutput
    ) {
        ContainerMove move = simulateFill(input, tank);
        if (move == null || (!move.output().isEmpty() && !canPlaceOutput.test(move.output()))) {
            return false;
        }

        tank.drain(move.fluid(), move.amount(), false);
        input.shrink(1);
        placeOutput.accept(move.output());
        return true;
    }

    @Nullable
    private static ContainerMove simulateDrain(
            ItemStack input,
            HbmFluidTank tank,
            Predicate<HbmFluidDefinition> acceptsFluid
    ) {
        ItemStack single = input.copyWithCount(1);
        Optional<IFluidHandlerItem> optionalHandler = FluidUtil.getFluidHandler(single);
        if (optionalHandler.isEmpty()) {
            return null;
        }
        IFluidHandlerItem handler = optionalHandler.get();
        FluidStack drainable = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (drainable.isEmpty()) {
            return null;
        }
        HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(drainable.getFluid()).orElse(HbmFluids.none());
        if (fluid.isNone() || !acceptsFluid.test(fluid)) {
            return null;
        }

        int accepted = tank.fill(fluid, drainable.getAmount(), true);
        if (accepted <= 0) {
            return null;
        }
        FluidStack request = HbmFluids.toNeoStack(fluid, accepted);
        FluidStack drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || !FluidStack.isSameFluidSameComponents(drained, request)) {
            return null;
        }
        HbmFluidDefinition drainedFluid = HbmFluids.fromNeoFluid(drained.getFluid()).orElse(HbmFluids.none());
        if (drainedFluid != fluid || tank.fill(fluid, drained.getAmount(), true) != drained.getAmount()) {
            return null;
        }
        return new ContainerMove(fluid, drained.getAmount(), singleOutput(handler.getContainer()));
    }

    @Nullable
    private static ContainerMove simulateFill(ItemStack input, HbmFluidTank tank) {
        if (input.isEmpty() || tank.type().isNone() || tank.amount() <= 0 || tank.pressure() != 0) {
            return null;
        }

        ItemStack single = input.copyWithCount(1);
        Optional<IFluidHandlerItem> optionalHandler = FluidUtil.getFluidHandler(single);
        if (optionalHandler.isEmpty()) {
            return null;
        }
        IFluidHandlerItem handler = optionalHandler.get();
        HbmFluidDefinition fluid = tank.type();
        int offered = tank.amount();
        int accepted = handler.fill(HbmFluids.toNeoStack(fluid, offered), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0 || tank.drain(fluid, accepted, true).amount() != accepted) {
            return null;
        }
        int filled = handler.fill(HbmFluids.toNeoStack(fluid, accepted), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0 || tank.drain(fluid, filled, true).amount() != filled) {
            return null;
        }
        return new ContainerMove(fluid, filled, singleOutput(handler.getContainer()));
    }

    private static ItemStack singleOutput(ItemStack output) {
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = output.copy();
        copy.setCount(1);
        return copy;
    }

    private record ContainerMove(HbmFluidDefinition fluid, int amount, ItemStack output) {
    }
}
