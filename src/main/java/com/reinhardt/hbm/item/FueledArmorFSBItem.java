package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/** Direct modern equivalent of 1.7.10 ArmorFSBFueled. */
public class FueledArmorFSBItem extends ArmorFSBItem {
    private static final String FUEL_KEY = "fuel";

    private final String fuelId;
    private final int maxFuel;
    private final int fillRate;
    private final int consumption;
    private final int drain;

    public FueledArmorFSBItem(
            String fsbGroup,
            Holder<ArmorMaterial> material,
            ArmorItem.Type type,
            boolean noHelmet,
            List<FullSetEffect> fullSetEffects,
            String fuelId,
            int maxFuel,
            int fillRate,
            int consumption,
            int drain,
            Properties properties
    ) {
        super(fsbGroup, material, type, noHelmet, fullSetEffects, properties);
        this.fuelId = fuelId;
        this.maxFuel = Math.max(1, maxFuel);
        this.fillRate = Math.max(0, fillRate);
        this.consumption = Math.max(0, consumption);
        this.drain = Math.max(0, drain);
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return fuel(stack) > 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player && player.getItemBySlot(this.getEquipmentSlot()) == stack
                && this.drain > 0 && !player.getAbilities().instabuild && level.getGameTime() % 10L == 0L && hasFSBArmor(player)) {
            setFuel(stack, fuel(stack) - this.drain);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fuel(stack) < this.maxFuel;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * fuel(stack) / (float) this.maxFuel), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HbmFluidDefinition fluid = acceptedFuel();
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.armor.fuel",
                Component.translatable(fluid.translationKey()),
                fuel(stack),
                this.maxFuel
        ).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public int fuel(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(FUEL_KEY)) {
            return this.maxFuel;
        }
        return Mth.clamp(tag.getInt(FUEL_KEY), 0, this.maxFuel);
    }

    public void setFuel(ItemStack stack, int fuel) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(FUEL_KEY, Mth.clamp(fuel, 0, this.maxFuel));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack);
    }

    private HbmFluidDefinition acceptedFuel() {
        return HbmFluids.byName(this.fuelId).orElse(HbmFluids.none());
    }

    private final class Handler implements IFluidHandlerItem {
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
            return tank == 0 ? maxFuel : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid())
                    .map(fluid -> fluid == acceptedFuel())
                    .orElse(false);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) {
                return 0;
            }
            int accepted = Math.min(resource.getAmount(), Math.min(fillRate, maxFuel - fuel(container)));
            if (accepted > 0 && action.execute()) {
                setFuel(container, fuel(container) + accepted);
            }
            return accepted;
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
    }
}
