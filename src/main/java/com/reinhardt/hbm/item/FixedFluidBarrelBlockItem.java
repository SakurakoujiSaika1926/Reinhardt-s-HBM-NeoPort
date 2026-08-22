package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** A 1.7.10-style filled barrel: the block item itself is the fixed 10-bucket container. */
public class FixedFluidBarrelBlockItem extends BlockItem {
    private final String fluidName;

    public FixedFluidBarrelBlockItem(Block block, Properties properties, String fluidName) {
        super(block, properties.stacksTo(1));
        this.fluidName = fluidName;
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack, this.fluidName);
    }

    private static final class Handler implements IFluidHandlerItem {
        private ItemStack container;
        private final String fluidName;

        private Handler(ItemStack container, String fluidName) {
            this.container = container;
            this.fluidName = fluidName;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.byName(this.fluidName).orElse(HbmFluids.none());
            return fluid.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, TankSteelItem.CAPACITY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? TankSteelItem.CAPACITY : 0;
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
            if (resource.isEmpty() || resource.getAmount() < TankSteelItem.CAPACITY) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(TankSteelItem.CAPACITY, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain < TankSteelItem.CAPACITY) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                this.container = new ItemStack(HbmItems.TANK_STEEL.get());
            }
            return stored;
        }

        @Override
        public ItemStack getContainer() {
            return this.container;
        }
    }
}
