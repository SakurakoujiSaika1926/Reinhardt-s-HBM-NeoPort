package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/** Exact-capacity port of the 1.7.10 ItemPipette fluid container. */
public final class LegacyPipetteItem extends Item {
    private static final String FLUID_TAG = "fluid";
    private static final String FILL_TAG = "fill";
    private static final String CAPACITY_TAG = "capacity";

    public enum Kind {
        NORMAL(1_000, 50, 50),
        BORON(1_000, 50, 50),
        LABORATORY(50, 1, 1);

        private final int maximum;
        private final int increment;
        private final int minimum;

        Kind(int maximum, int increment, int minimum) {
            this.maximum = maximum;
            this.increment = increment;
            this.minimum = minimum;
        }
    }

    private final Kind kind;

    public LegacyPipetteItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack, this);
    }

    public int capacity(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.contains(CAPACITY_TAG) ? Math.clamp(tag.getInt(CAPACITY_TAG), kind.minimum, kind.maximum) : kind.maximum;
    }

    public int fill(ItemStack stack) {
        return Math.max(0, data(stack).getInt(FILL_TAG));
    }

    public Kind kind() {
        return kind;
    }

    public HbmFluidDefinition fluid(ItemStack stack) {
        return HbmFluids.byName(data(stack).getString(FLUID_TAG)).orElse(HbmFluids.none());
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof LegacyPipetteItem pipette) || tintIndex == 0) {
            return 0xFFFFFFFF;
        }
        return pipette.fill(stack) == 0 ? 0x00FFFFFF : 0xFF000000 | pipette.fluid(stack).color();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (fill(stack) > 0) {
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.pipette.cannot_resize"), true);
            } else {
                int delta = player.isShiftKeyDown() ? -kind.increment : kind.increment;
                int capacity = Math.clamp(capacity(stack) + delta, kind.minimum, kind.maximum);
                CompoundTag tag = data(stack);
                tag.putInt(CAPACITY_TAG, capacity);
                save(stack, tag);
                player.displayClientMessage(Component.literal(capacity + "/" + kind.maximum + "mB"), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        switch (kind) {
            case NORMAL -> tooltip.add(Component.translatable("item.reinhardtshbm.pipette.non_corrosive").withStyle(ChatFormatting.GRAY));
            case BORON -> tooltip.add(Component.translatable("item.reinhardtshbm.pipette.corrosive").withStyle(ChatFormatting.GRAY));
            case LABORATORY -> {
                tooltip.add(Component.translatable("item.reinhardtshbm.pipette.corrosive").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("item.reinhardtshbm.pipette.laboratory").withStyle(ChatFormatting.GRAY));
            }
        }
        HbmFluidDefinition fluid = fluid(stack);
        tooltip.add(Component.translatable("item.reinhardtshbm.pipette.fluid", Component.translatable(fluid.translationKey())));
        tooltip.add(Component.translatable("item.reinhardtshbm.pipette.amount", fill(stack), capacity(stack), kind.maximum));
    }

    private boolean accepts(HbmFluidDefinition fluid, ItemStack stack) {
        if (fluid.isNone() || fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) {
            return false;
        }
        if (kind == Kind.NORMAL && fluid.hasTrait(HbmFluidTrait.CORROSIVE) && !fluid.name().equals("peroxide")) {
            return false;
        }
        return fill(stack) == 0 || fluid(stack).name().equals(fluid.name());
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(tag.getInt(FILL_TAG) > 0 ? 1 : 0));
    }

    private static final class Handler implements IFluidHandlerItem {
        private ItemStack container;
        private final LegacyPipetteItem item;

        private Handler(ItemStack container, LegacyPipetteItem item) {
            this.container = container;
            this.item = item;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || item.fill(container) == 0) {
                return FluidStack.EMPTY;
            }
            return HbmFluids.toNeoStack(item.fluid(container), item.fill(container));
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? item.capacity(container) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            if (tank != 0 || resource.isEmpty()) {
                return false;
            }
            return HbmFluids.fromNeoFluid(resource.getFluid()).map(fluid -> item.accepts(fluid, container)).orElse(false);
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!item.accepts(fluid, container)) {
                return 0;
            }
            int inserted = Math.min(item.capacity(container) - item.fill(container), resource.getAmount());
            if (inserted > 0 && action.execute()) {
                CompoundTag tag = data(container);
                tag.putString(FLUID_TAG, fluid.name());
                tag.putInt(FILL_TAG, item.fill(container) + inserted);
                save(container, tag);
            }
            return inserted;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int amount, IFluidHandler.FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || amount <= 0) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(amount, stored.getAmount());
            FluidStack result = stored.copyWithAmount(drained);
            if (action.execute()) {
                CompoundTag tag = data(container);
                int remaining = item.fill(container) - drained;
                if (remaining == 0) {
                    tag.remove(FLUID_TAG);
                    tag.remove(FILL_TAG);
                } else {
                    tag.putInt(FILL_TAG, remaining);
                }
                save(container, tag);
            }
            return result;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }
    }
}
