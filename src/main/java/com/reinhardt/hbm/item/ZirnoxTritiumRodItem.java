package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** 1.7.10 FluidContainerRegistry counterpart for rod_zirnox_tritium. */
public final class ZirnoxTritiumRodItem extends Item {
    public static final int TRITIUM_AMOUNT_MB = 2_000;

    public ZirnoxTritiumRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return emptyRod();
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack);
    }

    private static HbmFluidDefinition tritium() {
        return HbmFluids.byName("tritium").orElse(HbmFluids.none());
    }

    private static FluidStack tritiumStack() {
        HbmFluidDefinition tritium = tritium();
        return tritium.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(tritium, TRITIUM_AMOUNT_MB);
    }

    private static ItemStack emptyRod() {
        return new ItemStack(HbmItems.ROD_ZIRNOX_EMPTY.get());
    }

    private static final class Handler implements IFluidHandlerItem {
        private ItemStack container;

        private Handler(ItemStack container) {
            this.container = container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || !isFullRod()) {
                return FluidStack.EMPTY;
            }
            return tritiumStack();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? TRITIUM_AMOUNT_MB : 0;
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
            if (resource.isEmpty() || resource.getAmount() < TRITIUM_AMOUNT_MB) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(TRITIUM_AMOUNT_MB, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!isFullRod() || maxDrain < TRITIUM_AMOUNT_MB) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = tritiumStack();
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                this.container = emptyRod();
            }
            return stored;
        }

        @Override
        public ItemStack getContainer() {
            return this.container;
        }

        private boolean isFullRod() {
            return this.container.is(HbmItems.ROD_ZIRNOX_TRITIUM.get());
        }
    }
}
