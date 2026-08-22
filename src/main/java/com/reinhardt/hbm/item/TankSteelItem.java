package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** The empty 10-bucket steel tank used by the three legacy filled barrels. */
public class TankSteelItem extends Item {
    public static final int CAPACITY = 10_000;

    public TankSteelItem(Properties properties) {
        super(properties);
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack);
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
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && barrelFor(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getAmount() < CAPACITY) {
                return 0;
            }
            Item filledBarrel = barrelFor(resource);
            if (filledBarrel == null) {
                return 0;
            }
            if (action.execute()) {
                this.container = new ItemStack(filledBarrel);
            }
            return CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public ItemStack getContainer() {
            return this.container;
        }

        private static Item barrelFor(FluidStack stack) {
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return switch (fluid.name()) {
                case "diesel" -> HbmItems.RED_BARREL_ITEM.get();
                case "kerosene" -> HbmItems.PINK_BARREL_ITEM.get();
                case "oxygen" -> HbmItems.LOX_BARREL_ITEM.get();
                default -> null;
            };
        }
    }
}
