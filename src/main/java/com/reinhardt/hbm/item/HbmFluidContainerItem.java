package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class HbmFluidContainerItem extends Item {
    private static final String FLUID = "fluid";

    private final Kind kind;
    private final boolean filled;

    public HbmFluidContainerItem(Properties properties, Kind kind, boolean filled) {
        super(properties);
        this.kind = Objects.requireNonNull(kind);
        this.filled = filled;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isFilledContainer() {
        return filled;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!filled) {
            return super.getName(stack);
        }
        HbmFluidDefinition fluid = fluid(stack);
        if (fluid.isNone()) {
            return super.getName(stack);
        }
        return Component.translatable(kind.nameKey(), Component.translatable(fluid.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        if (!filled) {
            tooltip.add(Component.translatable("info.reinhardtshbm.fluid.empty_container").withStyle(ChatFormatting.GRAY));
            return;
        }

        HbmFluidDefinition fluid = fluid(stack);
        String amount = kind.capacity() + "/" + kind.capacity() + " mB";
        if (stack.getCount() > 1) {
            amount = stack.getCount() + "x " + amount;
        }
        tooltip.add(Component.literal(amount).withStyle(ChatFormatting.GRAY));
        HbmFluidTooltip.appendTraitInfo(tooltip, fluid, flag.isAdvanced());
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        if (!filled) {
            output.accept(this);
            return;
        }
        for (HbmFluidDefinition definition : HbmFluids.niceOrder()) {
            if (kind.allows(definition)) {
                output.accept(filledStack(definition));
            }
        }
    }

    public ItemStack filledStack(HbmFluidDefinition fluid) {
        ItemStack stack = new ItemStack(this);
        setFluid(stack, fluid);
        return stack;
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack, kind, filled);
    }

    public static HbmFluidDefinition fluid(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String fluidName = tag.getString(FLUID);
        if (fluidName.isBlank() && stack.getItem() instanceof HbmFluidContainerItem container) {
            fluidName = container.kind.defaultFluid();
        }
        return HbmFluids.byName(fluidName).orElse(HbmFluids.none());
    }

    public static ItemStack makeFull(Supplier<? extends Item> item, HbmFluidDefinition fluid) {
        ItemStack stack = new ItemStack(item.get());
        setFluid(stack, fluid);
        return stack;
    }

    public static void setFluid(ItemStack stack, HbmFluidDefinition fluid) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(FLUID, fluid == null ? "none" : fluid.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof HbmFluidContainerItem item) || !item.filled || tintIndex == 0) {
            return 0xFFFFFFFF;
        }
        HbmFluidDefinition fluid = fluid(stack);
        if (item.kind == Kind.CANISTER) {
            return 0xFF000000 | parseColorOr(fluid.canisterColor(), fluid.color());
        }
        if (item.kind == Kind.GAS_TANK) {
            String[] colors = fluid.gasTankColors().split("/");
            int fallback = tintIndex == 1 ? fluid.color() : 0xFFFFFF;
            if (tintIndex == 1 && colors.length > 0) {
                return 0xFF000000 | parseColorOr(colors[0], fallback);
            }
            if (tintIndex == 2 && colors.length > 1) {
                return 0xFF000000 | parseColorOr(colors[1], fallback);
            }
            return 0xFF000000 | fallback;
        }
        return 0xFF000000 | fluid.color();
    }

    private static int parseColorOr(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String clean = raw.trim();
        try {
            if (clean.startsWith("0x") || clean.startsWith("0X")) {
                return (int) Long.parseLong(clean.substring(2), 16);
            }
            return Integer.parseInt(clean);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public enum Kind {
        CANISTER(1_000, "item.reinhardtshbm.canister_full.named", HbmFluidContainerItem::allowsCanister),
        GAS_TANK(1_000, "item.reinhardtshbm.gas_full.named", HbmFluidContainerItem::allowsGasTank),
        FLUID_TANK(1_000, "item.reinhardtshbm.fluid_tank_full.named", HbmFluidContainerItem::allowsRegularTank),
        LEAD_TANK(1_000, "item.reinhardtshbm.fluid_tank_lead_full.named", HbmFluidContainerItem::allowsLeadTank),
        FLUID_BARREL(16_000, "item.reinhardtshbm.fluid_barrel_full.named", HbmFluidContainerItem::allowsRegularTank),
        FLUID_PACK(32_000, "item.reinhardtshbm.fluid_pack_full.named", HbmFluidContainerItem::allowsRegularTank),
        DISPERSER(2_000, "item.reinhardtshbm.disperser_canister.named", HbmFluidContainerItem::allowsDisperser),
        GLYPHID_GLAND(4_000, "item.reinhardtshbm.glyphid_gland.named", HbmFluidContainerItem::allowsGlyphidGland),
        CELL(1_000, "item.reinhardtshbm.cell_tritium.named", HbmFluidContainerItem::allowsTritiumCell);

        private final int capacity;
        private final String nameKey;
        private final Predicate<HbmFluidDefinition> filter;

        Kind(int capacity, String nameKey, Predicate<HbmFluidDefinition> filter) {
            this.capacity = capacity;
            this.nameKey = nameKey;
            this.filter = filter;
        }

        public int capacity() {
            return capacity;
        }

        public String nameKey() {
            return nameKey;
        }

        public ItemStack emptyStack() {
            return switch (this) {
                case CANISTER -> new ItemStack(HbmItems.CANISTER_EMPTY.get());
                case GAS_TANK -> new ItemStack(HbmItems.GAS_EMPTY.get());
                case FLUID_TANK -> new ItemStack(HbmItems.FLUID_TANK_EMPTY.get());
                case LEAD_TANK -> new ItemStack(HbmItems.FLUID_TANK_LEAD_EMPTY.get());
                case FLUID_BARREL -> new ItemStack(HbmItems.FLUID_BARREL_EMPTY.get());
                case FLUID_PACK -> new ItemStack(HbmItems.FLUID_PACK_EMPTY.get());
                case DISPERSER -> new ItemStack(HbmItems.DISPERSER_CANISTER_EMPTY.get());
                case GLYPHID_GLAND -> new ItemStack(HbmItems.GLYPHID_GLAND_EMPTY.get());
                case CELL -> new ItemStack(HbmItems.CELL_EMPTY.get());
            };
        }

        public String defaultFluid() {
            return this == CELL ? "tritium" : "";
        }

        public boolean allows(HbmFluidDefinition fluid) {
            return filter.test(fluid);
        }
    }

    private static boolean allowsCanister(HbmFluidDefinition fluid) {
        return !fluid.isNone() && !fluid.canisterColor().isBlank() && !fluid.hasTrait(HbmFluidTrait.NO_CONTAINER);
    }

    private static boolean allowsGasTank(HbmFluidDefinition fluid) {
        return !fluid.isNone() && !fluid.gasTankColors().isBlank() && !fluid.hasTrait(HbmFluidTrait.NO_CONTAINER);
    }

    private static boolean allowsRegularTank(HbmFluidDefinition fluid) {
        return fluid.allowsRegularContainer();
    }

    private static boolean allowsLeadTank(HbmFluidDefinition fluid) {
        return !fluid.isNone() && !fluid.hasTrait(HbmFluidTrait.NO_CONTAINER);
    }

    private static boolean allowsDisperser(HbmFluidDefinition fluid) {
        return !fluid.isNone()
                && !fluid.hasTrait(HbmFluidTrait.NO_CONTAINER)
                && !fluid.hasTrait(HbmFluidTrait.ANTIMATTER)
                && !fluid.hasTrait(HbmFluidTrait.VISCOUS);
    }

    private static boolean allowsGlyphidGland(HbmFluidDefinition fluid) {
        return fluid.name().equals("pheromone") || fluid.name().equals("sulfuric_acid");
    }

    private static boolean allowsTritiumCell(HbmFluidDefinition fluid) {
        return fluid.name().equals("tritium");
    }

    private static final class Handler implements IFluidHandlerItem {
        private ItemStack container;
        private final Kind kind;
        private final boolean fullItem;

        private Handler(ItemStack container, Kind kind, boolean fullItem) {
            this.container = container;
            this.kind = kind;
            this.fullItem = fullItem;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || !fullItem) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = fluid(container);
            return fluid.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, kind.capacity());
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? kind.capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(kind::allows).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (fullItem || resource.isEmpty() || resource.getAmount() < kind.capacity()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (!kind.allows(fluid)) {
                return 0;
            }
            if (action.execute()) {
                ItemStack full = fullStackForKind(kind, fluid);
                full.setCount(1);
                this.container = full;
            }
            return kind.capacity();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getAmount() < kind.capacity()) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(kind.capacity(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!fullItem || maxDrain < kind.capacity()) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                this.container = kind.emptyStack();
            }
            return stored;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        private static ItemStack fullStackForKind(Kind kind, HbmFluidDefinition fluid) {
            return switch (kind) {
                case CANISTER -> makeFull(HbmItems.CANISTER_FULL::get, fluid);
                case GAS_TANK -> makeFull(HbmItems.GAS_FULL::get, fluid);
                case FLUID_TANK -> makeFull(HbmItems.FLUID_TANK_FULL::get, fluid);
                case LEAD_TANK -> makeFull(HbmItems.FLUID_TANK_LEAD_FULL::get, fluid);
                case FLUID_BARREL -> makeFull(HbmItems.FLUID_BARREL_FULL::get, fluid);
                case FLUID_PACK -> makeFull(HbmItems.FLUID_PACK_FULL::get, fluid);
                case DISPERSER -> makeFull(HbmItems.DISPERSER_CANISTER::get, fluid);
                case GLYPHID_GLAND -> makeFull(HbmItems.GLYPHID_GLAND::get, fluid);
                case CELL -> makeFull(HbmItems.CELL_TRITIUM::get, fluid);
            };
        }
    }
}
