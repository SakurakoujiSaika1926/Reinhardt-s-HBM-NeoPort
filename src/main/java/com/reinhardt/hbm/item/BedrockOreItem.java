package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class BedrockOreItem extends Item {
    private static final String GRADE = "grade";
    private static final String TYPE = "type";

    public BedrockOreItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Grade grade = grade(stack);
        Type type = type(stack);
        return Component.translatable(
                "item.reinhardtshbm.bedrock_ore_new.grade." + grade.id() + ".name",
                Component.translatable("item.reinhardtshbm.bedrock_ore_new.type." + type.id() + ".name")
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (Trait trait : grade(stack).traits()) {
            tooltip.add(Component.translatable("item.reinhardtshbm.bedrock_ore_new.trait." + trait.id())
                    .withStyle(trait.color()));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            for (Grade grade : Grade.values()) {
                output.accept(stackFor(HbmItems.BEDROCK_ORE_NEW, grade, type));
            }
        }
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, Grade grade, Type type) {
        return stackFor(item, grade, type, 1);
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, Grade grade, Type type, int count) {
        ItemStack stack = new ItemStack(item.get(), count);
        CompoundTag tag = new CompoundTag();
        tag.putString(GRADE, grade.id());
        tag.putString(TYPE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(grade.ordinal() * Type.values().length + type.ordinal()));
        return stack;
    }

    private Grade grade(ItemStack stack) {
        return Grade.byId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(GRADE));
    }

    private Type type(ItemStack stack) {
        return Type.byId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(TYPE));
    }

    public enum Type {
        LIGHT_METAL("light"),
        HEAVY_METAL("heavy"),
        RARE_EARTH("rare"),
        ACTINIDE("actinide"),
        NON_METAL("nonmetal"),
        CRYSTALLINE("crystal");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }

        public static Type byId(String id) {
            if (id != null && !id.isBlank()) {
                for (Type type : values()) {
                    if (type.id.equals(id) || type.name().equalsIgnoreCase(id)) {
                        return type;
                    }
                }
            }
            return LIGHT_METAL;
        }
    }

    public enum Trait {
        ROASTED(ChatFormatting.YELLOW),
        ARC(ChatFormatting.GOLD),
        WASHED(ChatFormatting.AQUA),
        CENTRIFUGED(ChatFormatting.BLUE),
        SULFURIC(ChatFormatting.GOLD),
        SOLVENT(ChatFormatting.WHITE),
        RAD(ChatFormatting.GREEN);

        private final ChatFormatting color;

        Trait(ChatFormatting color) {
            this.color = color;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting color() {
            return this.color;
        }
    }

    public enum Grade {
        BASE("base"),
        BASE_ROASTED("base_roasted", Trait.ROASTED),
        BASE_WASHED("base_washed", Trait.WASHED),
        PRIMARY("primary", Trait.CENTRIFUGED),
        PRIMARY_ROASTED("primary_roasted", Trait.ROASTED),
        PRIMARY_SULFURIC("primary_sulfuric", Trait.SULFURIC),
        PRIMARY_NOSULFURIC("primary_nosulfuric", Trait.CENTRIFUGED, Trait.SULFURIC),
        PRIMARY_SOLVENT("primary_solvent", Trait.SOLVENT),
        PRIMARY_NOSOLVENT("primary_nosolvent", Trait.CENTRIFUGED, Trait.SOLVENT),
        PRIMARY_RAD("primary_rad", Trait.RAD),
        PRIMARY_NORAD("primary_norad", Trait.CENTRIFUGED, Trait.RAD),
        PRIMARY_FIRST("primary_first", Trait.CENTRIFUGED),
        PRIMARY_SECOND("primary_second", Trait.CENTRIFUGED),
        CRUMBS("crumbs", Trait.CENTRIFUGED),
        SULFURIC_BYPRODUCT("sulfuric_byproduct", Trait.CENTRIFUGED, Trait.SULFURIC),
        SULFURIC_ROASTED("sulfuric_roasted", Trait.ROASTED, Trait.SULFURIC),
        SULFURIC_ARC("sulfuric_arc", Trait.ARC, Trait.SULFURIC),
        SULFURIC_WASHED("sulfuric_washed", Trait.WASHED, Trait.SULFURIC),
        SOLVENT_BYPRODUCT("solvent_byproduct", Trait.CENTRIFUGED, Trait.SOLVENT),
        SOLVENT_ROASTED("solvent_roasted", Trait.ROASTED, Trait.SOLVENT),
        SOLVENT_ARC("solvent_arc", Trait.ARC, Trait.SOLVENT),
        SOLVENT_WASHED("solvent_washed", Trait.WASHED, Trait.SOLVENT),
        RAD_BYPRODUCT("rad_byproduct", Trait.CENTRIFUGED, Trait.RAD),
        RAD_ROASTED("rad_roasted", Trait.ROASTED, Trait.RAD),
        RAD_ARC("rad_arc", Trait.ARC, Trait.RAD),
        RAD_WASHED("rad_washed", Trait.WASHED, Trait.RAD);

        private final String id;
        private final List<Trait> traits;

        Grade(String id, Trait... traits) {
            this.id = id;
            this.traits = List.of(traits);
        }

        public String id() {
            return this.id;
        }

        public List<Trait> traits() {
            return this.traits;
        }

        public static Grade byId(String id) {
            if (id != null && !id.isBlank()) {
                for (Grade grade : values()) {
                    if (grade.id.equals(id) || grade.name().equalsIgnoreCase(id)) {
                        return grade;
                    }
                }
            }
            return BASE;
        }
    }
}
