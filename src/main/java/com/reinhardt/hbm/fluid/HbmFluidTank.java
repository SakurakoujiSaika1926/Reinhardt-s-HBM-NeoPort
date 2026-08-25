package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class HbmFluidTank implements IFluidHandler {
    private HbmFluidDefinition type;
    private int amount;
    private int capacity;
    private int pressure;

    public HbmFluidTank(int capacity) {
        this(HbmFluids.none(), capacity);
    }

    public HbmFluidTank(HbmFluidDefinition type, int capacity) {
        this.type = type == null ? HbmFluids.none() : type;
        this.capacity = Math.max(0, capacity);
    }

    public HbmFluidDefinition type() {
        return type;
    }

    public int amount() {
        return amount;
    }

    public int capacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        if (this.amount > this.capacity) {
            this.amount = this.capacity;
        }
        if (this.amount == 0) {
            clear();
        }
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, Math.min(this.capacity, amount));
    }

    public int pressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        if (this.pressure != pressure) {
            this.amount = 0;
        }
        this.pressure = Math.max(0, pressure);
    }

    public void setType(HbmFluidDefinition type) {
        HbmFluidDefinition next = type == null ? HbmFluids.none() : type;
        if (this.type != next) {
            this.type = next;
            this.amount = 0;
        }
    }

    public void conform(HbmFluidDefinition type, int pressure) {
        setType(type);
        setPressure(pressure);
    }

    public void clear() {
        this.type = HbmFluids.none();
        this.amount = 0;
        this.pressure = 0;
    }

    public int fill(HbmFluidDefinition fillType, int offered, boolean simulate) {
        return fill(fillType, offered, 0, simulate);
    }

    public int fill(HbmFluidDefinition fillType, int offered, int fillPressure, boolean simulate) {
        if (fillType == null || fillType.isNone() || offered <= 0 || fillPressure < 0) {
            return 0;
        }
        if (!type.isNone() && type != fillType) {
            return 0;
        }
        if (amount > 0 && pressure != fillPressure) {
            return 0;
        }
        int accepted = Math.min(offered, capacity - amount);
        if (!simulate && accepted > 0) {
            type = fillType;
            pressure = fillPressure;
            amount += accepted;
        }
        return accepted;
    }

    public HbmFluidStack drain(HbmFluidDefinition drainType, int requested, boolean simulate) {
        if (requested <= 0 || type.isNone() || (drainType != null && drainType != type)) {
            return HbmFluidStack.EMPTY;
        }
        int drained = Math.min(requested, amount);
        HbmFluidStack stack = new HbmFluidStack(type, drained, pressure);
        if (!simulate && drained > 0) {
            amount -= drained;
            if (amount <= 0) {
                clear();
            }
        }
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0 || amount <= 0 || type.isNone()) {
            return FluidStack.EMPTY;
        }
        return HbmFluids.toNeoStack(type, amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? capacity : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(def -> !def.isNone()).isPresent();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        return HbmFluids.fromNeoFluid(resource.getFluid())
                .map(def -> fill(def, resource.getAmount(), action.simulate()))
                .orElse(0);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(null);
        HbmFluidStack drained = drain(requested, resource.getAmount(), action.simulate());
        return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        HbmFluidStack drained = drain(null, maxDrain, action.simulate());
        return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("amount", amount);
        tag.putInt("capacity", capacity);
        tag.putInt("pressure", pressure);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            clear();
            return;
        }
        if (tag.contains("capacity")) {
            this.capacity = Math.max(0, tag.getInt("capacity"));
        }
        this.type = HbmFluids.byName(tag.getString("type")).orElse(HbmFluids.none());
        this.amount = Math.max(0, Math.min(capacity, tag.getInt("amount")));
        this.pressure = Math.max(0, tag.getInt("pressure"));
        if (amount == 0) {
            clear();
        }
    }
}
