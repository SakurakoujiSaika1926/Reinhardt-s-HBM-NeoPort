package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.Predicate;

public class DfcFluidHandler implements IFluidHandler {
    private final HbmFluidTank[] tanks;
    private final Predicate<HbmFluidDefinition> accepts;

    public DfcFluidHandler(HbmFluidTank[] tanks, Predicate<HbmFluidDefinition> accepts) {
        this.tanks = tanks;
        this.accepts = accepts;
    }

    @Override
    public int getTanks() {
        return this.tanks.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return valid(tank) ? this.tanks[tank].getFluidInTank(0) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return valid(tank) ? this.tanks[tank].capacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (!valid(tank) || stack.isEmpty()) {
            return false;
        }
        return HbmFluids.fromNeoFluid(stack.getFluid()).filter(this.accepts).isPresent();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
        if (fluid.isNone() || !this.accepts.test(fluid)) {
            return 0;
        }

        int accepted = 0;
        for (HbmFluidTank tank : this.tanks) {
            if (tank.type() == fluid) {
                accepted += tank.fill(fluid, resource.getAmount() - accepted, action.simulate());
                if (accepted >= resource.getAmount()) {
                    return accepted;
                }
            }
        }
        for (HbmFluidTank tank : this.tanks) {
            if (tank.amount() == 0 || tank.type().isNone()) {
                accepted += tank.fill(fluid, resource.getAmount() - accepted, action.simulate());
                if (accepted >= resource.getAmount()) {
                    return accepted;
                }
            }
        }
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
        if (fluid.isNone()) {
            return FluidStack.EMPTY;
        }
        for (HbmFluidTank tank : this.tanks) {
            HbmFluidStack drained = tank.drain(fluid, resource.getAmount(), action.simulate());
            if (!drained.isEmpty()) {
                return HbmFluids.toNeoStack(drained.type(), drained.amount());
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (HbmFluidTank tank : this.tanks) {
            HbmFluidStack drained = tank.drain(null, maxDrain, action.simulate());
            if (!drained.isEmpty()) {
                return HbmFluids.toNeoStack(drained.type(), drained.amount());
            }
        }
        return FluidStack.EMPTY;
    }

    private boolean valid(int tank) {
        return tank >= 0 && tank < this.tanks.length;
    }
}
