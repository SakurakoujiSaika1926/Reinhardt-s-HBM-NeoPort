package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/** Exact-capacity port of ItemBlowtorch, including its separate acetylene and oxygen reservoirs. */
public final class BlowtorchItem extends Item {
    public enum Kind {
        BLOWTORCH("gas", 4_000, 250, null, 0),
        ACETYLENE("unsaturateds", 8_000, 20, "oxygen", 16_000, 10);

        private final String primaryFluid;
        private final int primaryCapacity;
        private final int primaryCost;
        private final String secondaryFluid;
        private final int secondaryCapacity;
        private final int secondaryCost;

        Kind(String primaryFluid, int primaryCapacity, int primaryCost, String secondaryFluid, int secondaryCapacity) {
            this(primaryFluid, primaryCapacity, primaryCost, secondaryFluid, secondaryCapacity, 0);
        }

        Kind(String primaryFluid, int primaryCapacity, int primaryCost, String secondaryFluid, int secondaryCapacity, int secondaryCost) {
            this.primaryFluid = primaryFluid;
            this.primaryCapacity = primaryCapacity;
            this.primaryCost = primaryCost;
            this.secondaryFluid = secondaryFluid;
            this.secondaryCapacity = secondaryCapacity;
            this.secondaryCost = secondaryCost;
        }

        boolean hasSecondary() {
            return secondaryFluid != null;
        }
    }

    private final Kind kind;

    public BlowtorchItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack);
    }

    public boolean canTorch(ItemStack stack) {
        return amount(stack, 0) >= kind.primaryCost
                && (!kind.hasSecondary() || amount(stack, 1) >= kind.secondaryCost);
    }

    /** Consume only after the target successfully performed its old TORCH action. */
    public void consumeTorchFuel(ItemStack stack) {
        setAmount(stack, 0, amount(stack, 0) - kind.primaryCost);
        if (kind.hasSecondary()) {
            setAmount(stack, 1, amount(stack, 1) - kind.secondaryCost);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fillFraction(stack) < 1.0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) fillFraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFFD119;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(fluidLine(kind.primaryFluid, amount(stack, 0), kind.primaryCapacity, ChatFormatting.YELLOW));
        if (kind.hasSecondary()) {
            tooltip.add(fluidLine(kind.secondaryFluid, amount(stack, 1), kind.secondaryCapacity, ChatFormatting.AQUA));
        }
    }

    private Component fluidLine(String fluidId, int amount, int capacity, ChatFormatting color) {
        HbmFluidDefinition fluid = HbmFluids.byName(fluidId).orElse(HbmFluids.none());
        return Component.translatable("tooltip.reinhardtshbm.blowtorch.fluid",
                Component.translatable(fluid.translationKey()), amount, capacity).withStyle(color);
    }

    private double fillFraction(ItemStack stack) {
        double primary = (double) amount(stack, 0) / kind.primaryCapacity;
        if (!kind.hasSecondary()) {
            return primary;
        }
        return Math.min(primary, (double) amount(stack, 1) / kind.secondaryCapacity);
    }

    private int tanks() {
        return kind.hasSecondary() ? 2 : 1;
    }

    private String fluidId(int tank) {
        return tank == 0 ? kind.primaryFluid : kind.secondaryFluid;
    }

    private int capacity(int tank) {
        return tank == 0 ? kind.primaryCapacity : kind.secondaryCapacity;
    }

    private int amount(ItemStack stack, int tank) {
        if (tank < 0 || tank >= tanks()) {
            return 0;
        }
        CompoundTag tag = data(stack);
        String key = key(tank);
        // The old ItemBlowtorch initializes an untouched stack as full.
        return tag.contains(key) ? Math.clamp(tag.getInt(key), 0, capacity(tank)) : capacity(tank);
    }

    private void setAmount(ItemStack stack, int tank, int amount) {
        CompoundTag tag = data(stack);
        tag.putInt(key(tank), Math.clamp(amount, 0, capacity(tank)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private String key(int tank) {
        return "blowtorch_" + fluidId(tank);
    }

    private final class Handler implements IFluidHandlerItem {
        private final ItemStack stack;

        private Handler(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int getTanks() {
            return tanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank < 0 || tank >= tanks()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.byName(fluidId(tank)).orElse(HbmFluids.none());
            return HbmFluids.toNeoStack(fluid, amount(stack, tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < tanks() ? capacity(tank) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            return tank >= 0 && tank < tanks() && HbmFluids.fromNeoFluid(resource.getFluid())
                    .map(fluid -> fluid.name().equals(fluidId(tank)))
                    .orElse(false);
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            for (int tank = 0; tank < tanks(); tank++) {
                if (!isFluidValid(tank, resource)) {
                    continue;
                }
                int accepted = Math.min(resource.getAmount(), capacity(tank) - amount(stack, tank));
                if (accepted > 0 && action.execute()) {
                    setAmount(stack, tank, amount(stack, tank) + accepted);
                }
                return accepted;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public ItemStack getContainer() {
            return stack;
        }
    }
}
