package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** 1.7.10 nuclear waste subtypes, formerly stored in ItemStack metadata. */
public final class NuclearWasteItem extends Item {
    private static final String WASTE_CLASS_TAG = "waste_class";

    private final Family family;
    private final boolean depleted;
    private final boolean tiny;

    public NuclearWasteItem(Properties properties, Family family, boolean depleted, boolean tiny) {
        super(properties.stacksTo(1));
        this.family = family;
        this.depleted = depleted;
        this.tiny = tiny;
    }

    public Family family() {
        return this.family;
    }

    public boolean depleted() {
        return this.depleted;
    }

    public boolean tiny() {
        return this.tiny;
    }

    public WasteClass wasteClass(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return WasteClass.byOrdinal(tag.getInt(WASTE_CLASS_TAG), this.family);
    }

    public ItemStack stackFor(WasteClass wasteClass) {
        ItemStack result = new ItemStack(this);
        CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(WASTE_CLASS_TAG, wasteClass.metadata());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return result;
    }

    public static ItemStack copyWasteClass(ItemStack source, Item target) {
        if (!(target instanceof NuclearWasteItem waste)) {
            return new ItemStack(target);
        }
        WasteClass wasteClass = source.getItem() instanceof NuclearWasteItem sourceWaste
                ? sourceWaste.wasteClass(source)
                : waste.defaultWasteClass();
        return waste.stackFor(wasteClass);
    }

    public static WasteClass classOf(ItemStack stack) {
        return stack.getItem() instanceof NuclearWasteItem waste ? waste.wasteClass(stack) : WasteClass.URANIUM235;
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(wasteClass(stack).translationKey()).withStyle(ChatFormatting.ITALIC));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (WasteClass wasteClass : WasteClass.forFamily(this.family)) {
            output.accept(stackFor(wasteClass));
        }
    }

    private WasteClass defaultWasteClass() {
        return this.family == Family.LONG ? WasteClass.URANIUM235 : WasteClass.URANIUM235;
    }

    public enum Family {
        LONG,
        SHORT
    }

    public enum WasteClass {
        URANIUM235(Family.LONG, "uranium235", 0, 0, 0),
        URANIUM233(Family.LONG, "uranium233", 0, 50, 1),
        NEPTUNIUM(Family.LONG, "neptunium", 0, 100, 2),
        THORIUM(Family.LONG, "thorium232", 0, 0, 3),
        SCHRABIDIUM_LONG(Family.LONG, "schrabidium", 0, 250, 4),
        URANIUM235_SHORT(Family.SHORT, "uranium235", 0, 100, 0),
        URANIUM233_SHORT(Family.SHORT, "uranium233", 50, 100, 1),
        NEPTUNIUM_SHORT(Family.SHORT, "neptunium", 150, 500, 2),
        PLUTONIUM239(Family.SHORT, "plutonium239", 250, 1_000, 3),
        PLUTONIUM240(Family.SHORT, "plutonium240", 350, 1_000, 4),
        PLUTONIUM241(Family.SHORT, "plutonium241", 500, 1_000, 5),
        AMERICIUM242(Family.SHORT, "americium242", 750, 1_000, 6),
        SCHRABIDIUM_SHORT(Family.SHORT, "schrabidium", 1_000, 1_000, 7);

        private final Family family;
        private final String id;
        private final int liquid;
        private final int gas;
        private final int metadata;

        WasteClass(Family family, String id, int liquid, int gas, int metadata) {
            this.family = family;
            this.id = id;
            this.liquid = liquid;
            this.gas = gas;
            this.metadata = metadata;
        }

        public int liquid() {
            return this.liquid;
        }

        public int gas() {
            return this.gas;
        }

        public int metadata() {
            return this.metadata;
        }

        public String translationKey() {
            return "waste_class.reinhardtshbm." + this.id;
        }

        public static WasteClass byOrdinal(int ordinal, Family family) {
            WasteClass[] values = values();
            int first = family == Family.LONG ? 0 : 5;
            int length = family == Family.LONG ? 5 : 8;
            return values[first + Math.floorMod(ordinal, length)];
        }

        public static WasteClass[] forFamily(Family family) {
            return family == Family.LONG
                    ? new WasteClass[]{URANIUM235, URANIUM233, NEPTUNIUM, THORIUM, SCHRABIDIUM_LONG}
                    : new WasteClass[]{URANIUM235_SHORT, URANIUM233_SHORT, NEPTUNIUM_SHORT, PLUTONIUM239, PLUTONIUM240, PLUTONIUM241, AMERICIUM242, SCHRABIDIUM_SHORT};
        }
    }
}
