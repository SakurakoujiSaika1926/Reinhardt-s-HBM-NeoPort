package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;
import java.util.Locale;

public class InfiniteFluidContainerItem extends Item {
    private final String fluidName;
    private final int amountPerTick;

    public InfiniteFluidContainerItem(Properties properties, String fluidName, int amountPerTick) {
        super(properties);
        this.fluidName = fluidName;
        this.amountPerTick = amountPerTick;
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byName(fluidName).orElse(HbmFluids.none());
    }

    /** The legacy infinite barrel has no fixed fluid; it uses the tank's type. */
    public boolean isUniversal() {
        return fluidName == null;
    }

    public HbmFluidDefinition sourceFor(HbmFluidTank tank) {
        return isUniversal() ? tank.type() : fluid();
    }

    /** Only the legacy universal barrel may fill pressurised tanks. */
    public boolean allowsPressure(int pressure) {
        return isUniversal() || pressure == 0;
    }

    public int amountPerTick() {
        return amountPerTick;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        float bucketsPerSecond = amountPerTick * 0.02F;
        tooltip.add(Component.translatable(
                "desc.reinhardtshbm.canister_infinite",
                String.format(Locale.ROOT, "%.1f", bucketsPerSecond)
        ).withStyle(ChatFormatting.GREEN));
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack, this);
    }

    private static final class Handler implements IFluidHandlerItem {
        private final ItemStack container;
        private final InfiniteFluidContainerItem item;

        private Handler(ItemStack container, InfiniteFluidContainerItem item) {
            this.container = container;
            this.item = item;
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
            HbmFluidDefinition fluid = item.fluid();
            return fluid.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? Integer.MAX_VALUE : 0;
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
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (item.isUniversal()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = item.fluid();
            if (fluid.isNone() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int amount = Math.min(maxDrain, item.amountPerTick() * Math.max(1, container.getCount()));
            return HbmFluids.toNeoStack(fluid, amount);
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }
    }
}
