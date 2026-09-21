package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Modern counterpart of the two special mercury entries in the old
 * FluidContainerRegistry. A bottle contains 1000 mB and leaves its glass
 * bottle behind; a mercury drop contains 125 mB and is consumed outright.
 */
public final class LegacyMercuryContainerHandler implements IFluidHandlerItem {
    public static final int BOTTLE_AMOUNT_MB = 1_000;
    public static final int DROP_AMOUNT_MB = 125;

    private ItemStack container;

    public LegacyMercuryContainerHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int amount = tank == 0 ? amountFor(this.container) : 0;
        HbmFluidDefinition mercury = mercury();
        return amount <= 0 || mercury.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(mercury, amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? amountFor(this.container) : 0;
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
        int amount = amountFor(this.container);
        if (amount <= 0 || resource.isEmpty() || resource.getAmount() < amount) {
            return FluidStack.EMPTY;
        }
        FluidStack stored = getFluidInTank(0);
        if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
            return FluidStack.EMPTY;
        }
        return drain(amount, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int amount = amountFor(this.container);
        if (amount <= 0 || maxDrain < amount) {
            return FluidStack.EMPTY;
        }
        FluidStack stored = getFluidInTank(0);
        if (stored.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            this.container = isMercuryBottle(this.container) ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY;
        }
        return stored;
    }

    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    public static boolean isMercuryBottle(ItemStack stack) {
        return hasId(stack, "bottle_mercury");
    }

    public static boolean isMercuryDrop(ItemStack stack) {
        return hasId(stack, "nugget_mercury");
    }

    private static int amountFor(ItemStack stack) {
        if (isMercuryBottle(stack)) {
            return BOTTLE_AMOUNT_MB;
        }
        return isMercuryDrop(stack) ? DROP_AMOUNT_MB : 0;
    }

    private static boolean hasId(ItemStack stack, String path) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.equals(ReinhardtsHBM.id(path));
    }

    private static HbmFluidDefinition mercury() {
        return HbmFluids.byName("mercury").orElse(HbmFluids.none());
    }
}
