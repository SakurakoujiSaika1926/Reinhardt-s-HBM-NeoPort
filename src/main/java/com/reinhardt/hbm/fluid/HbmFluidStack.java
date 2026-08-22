package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.nbt.CompoundTag;

public final class HbmFluidStack {
    public static final HbmFluidStack EMPTY = new HbmFluidStack(HbmFluids.none(), 0, 0);

    private final HbmFluidDefinition type;
    private final int amount;
    private final int pressure;

    public HbmFluidStack(HbmFluidDefinition type, int amount) {
        this(type, amount, 0);
    }

    public HbmFluidStack(HbmFluidDefinition type, int amount, int pressure) {
        this.type = type == null ? HbmFluids.none() : type;
        this.amount = Math.max(0, amount);
        this.pressure = Math.max(0, pressure);
    }

    public HbmFluidDefinition type() {
        return type;
    }

    public int amount() {
        return amount;
    }

    public int pressure() {
        return pressure;
    }

    public boolean isEmpty() {
        return amount <= 0 || type.isNone();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("amount", amount);
        tag.putInt("pressure", pressure);
        return tag;
    }

    public static HbmFluidStack load(CompoundTag tag) {
        if (tag == null || !tag.contains("type")) {
            return EMPTY;
        }
        HbmFluidDefinition type = HbmFluids.byName(tag.getString("type")).orElse(HbmFluids.none());
        return new HbmFluidStack(type, tag.getInt("amount"), tag.getInt("pressure"));
    }
}
