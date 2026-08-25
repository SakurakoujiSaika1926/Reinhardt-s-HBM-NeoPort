package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/** The 1.7.10 ItemChainsaw contract, including its 5000 mB fuel store. */
public final class LegacyChainsawItem extends PickaxeItem {
    public static final int CAPACITY = 5_000;
    private static final String FUEL = "fuel";
    private static final String FUEL_AMOUNT = "fuel_amount";
    private static final HbmToolProfile PROFILE = HbmToolProfile.create(HbmToolTier.CHAINSAW, 25.0F, -2.8F)
            .movement(-0.05D)
            .area(HbmToolBehavior.AreaAbility.RECURSION, 2)
            .harvest(HbmToolBehavior.HarvestAbility.SILK, 0)
            .weapon(HbmToolBehavior.WeaponAbility.CHAINSAW, 1)
            .weapon(HbmToolBehavior.WeaponAbility.BEHEADER, 0);

    private static final List<String> ACCEPTED_FUELS = List.of(
            "diesel", "diesel_crack", "kerosene", "biofuel", "gasoline", "gasoline_leaded",
            "petroil", "petroil_leaded", "coalgas", "coalgas_leaded"
    );

    public LegacyChainsawItem() {
        super(HbmToolTier.CHAINSAW, HbmToolBehavior.properties(PROFILE));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!entity.level().isClientSide && fuelAmount(stack) >= 1) {
            setFuelAmount(stack, fuelAmount(stack) - 1);
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fuelAmount(stack) < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * fuelAmount(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return fuelAmount(stack) > 0 ? super.getDestroySpeed(stack, state) : 1.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HbmToolBehavior.addTooltip(stack, tooltip, flag, PROFILE);
        HbmFluidDefinition fluid = fuel(stack);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.chainsaw.fuel", fuelAmount(stack), CAPACITY)
                .withStyle(ChatFormatting.GOLD));
        for (String acceptedFuel : ACCEPTED_FUELS) {
            HbmFluids.byName(acceptedFuel).ifPresent(accepted -> tooltip.add(
                    Component.translatable("tooltip.reinhardtshbm.chainsaw.accepted_fuel", accepted.translationKey())
                            .withStyle(ChatFormatting.YELLOW)
            ));
        }
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new FuelHandler(stack);
    }

    public static int fuelAmount(ItemStack stack) {
        CompoundTag tag = data(stack);
        return Math.max(0, Math.min(CAPACITY, tag.getInt(FUEL_AMOUNT)));
    }

    public static HbmFluidDefinition fuel(ItemStack stack) {
        String id = data(stack).getString(FUEL);
        return HbmFluids.byName(id).orElse(HbmFluids.none());
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }

    private static void setFuel(ItemStack stack, HbmFluidDefinition fluid, int amount) {
        CompoundTag tag = data(stack);
        if (fluid == null || fluid.isNone() || amount <= 0) {
            tag.remove(FUEL);
            tag.remove(FUEL_AMOUNT);
        } else {
            tag.putString(FUEL, fluid.name());
            tag.putInt(FUEL_AMOUNT, Math.min(CAPACITY, amount));
        }
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static void setFuelAmount(ItemStack stack, int amount) {
        setFuel(stack, fuel(stack), amount);
    }

    private final class FuelHandler implements IFluidHandlerItem {
        private final ItemStack stack;

        private FuelHandler(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || fuel(stack).isNone() || fuelAmount(stack) <= 0) {
                return FluidStack.EMPTY;
            }
            return HbmFluids.toNeoStack(fuel(stack), fuelAmount(stack));
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            return tank == 0 && HbmFluids.fromNeoFluid(resource.getFluid())
                    .map(fluid -> ACCEPTED_FUELS.contains(fluid.name()))
                    .orElse(false);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource) || resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition incoming = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidDefinition current = fuel(stack);
            if (!current.isNone() && current != incoming) {
                return 0;
            }
            int accepted = Math.min(resource.getAmount(), CAPACITY - fuelAmount(stack));
            if (accepted > 0 && action.execute()) {
                setFuel(stack, incoming, fuelAmount(stack) + accepted);
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, getFluidInTank(0))) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int amount = Math.min(maxDrain, stored.getAmount());
            FluidStack result = stored.copy();
            result.setAmount(amount);
            if (action.execute()) {
                setFuelAmount(stack, fuelAmount(stack) - amount);
            }
            return result;
        }

        @Override
        public ItemStack getContainer() {
            return stack;
        }
    }
}
