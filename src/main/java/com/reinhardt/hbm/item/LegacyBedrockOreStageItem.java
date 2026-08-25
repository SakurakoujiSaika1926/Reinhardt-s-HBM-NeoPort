package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * The pre-NTM bedrock-ore processing chain used one item per processing stage
 * and metadata for the ore type. It is deliberately separate from
 * bedrock_ore_new, whose grade/type data drives the newer mining chain.
 */
public final class LegacyBedrockOreStageItem extends Item {
    private static final String TYPE_TAG = "ore_type";

    private final String stageId;

    public LegacyBedrockOreStageItem(Properties properties, String stageId) {
        super(properties);
        this.stageId = stageId;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm." + this.stageId,
                Component.translatable("hbmmat." + type(stack).id));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stackFor(this, type));
        }
    }

    public static ItemStack stackFor(Item item, Type type) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE_TAG, type.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.ordinal()));
        return stack;
    }

    public static Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String id = tag.getString(TYPE_TAG);
        for (Type type : Type.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (model != null && model.value() >= 0 && model.value() < Type.values().length) {
            return Type.values()[model.value()];
        }
        return Type.IRON;
    }

    /** The shared second layer is the old ore colour overlay. */
    public static int tint(ItemStack stack, int tintIndex) {
        return tintIndex == 1 ? 0xFF000000 | type(stack).color : 0xFFFFFFFF;
    }

    public enum Type {
        IRON("iron", 0xE2C0AA),
        COPPER("copper", 0xEC9A63),
        BORAX("borax", 0xE4BE74),
        ASBESTOS("asbestos", 0xBFBFB9),
        NIOBIUM("niobium", 0xAF58D8),
        TITANIUM("titanium", 0xF2EFE2),
        TUNGSTEN("tungsten", 0x2C293C),
        GOLD("gold", 0xF9D738),
        URANIUM("uranium", 0x868D82),
        THORIUM232("thorium232", 0x7D401D),
        CHLOROCALCITE("chlorocalcite", 0xCDE036),
        FLUORITE("fluorite", 0xF6F3E7),
        HEMATITE("hematite", 0xA37B72),
        MALACHITE("malachite", 0x66B48C),
        NEODYMIUM("neodymium", 0x8F8F5F);

        private final String id;
        private final int color;

        Type(String id, int color) {
            this.id = id;
            this.color = color;
        }
    }
}
