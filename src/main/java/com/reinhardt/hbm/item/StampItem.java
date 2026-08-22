package com.reinhardt.hbm.item;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class StampItem extends Item {
    @Nullable
    private final StampType type;

    public StampItem(Properties properties, @Nullable StampType type) {
        super(properties);
        this.type = type;
    }

    @Nullable
    public StampType type() {
        return this.type;
    }

    public static StampItem fromLegacyId(String id) {
        int durability = durabilityFor(id);
        Properties properties = new Item.Properties().stacksTo(1);
        if (durability > 0) {
            properties = properties.durability(durability);
        }
        return new StampItem(properties, typeFor(id));
    }

    public static boolean isStamp(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StampItem stamp && stamp.type() != null;
    }

    @Nullable
    public static StampType stampType(ItemStack stack) {
        return stack.getItem() instanceof StampItem stamp ? stamp.type() : null;
    }

    public static boolean isStampOfType(ItemStack stack, StampType type) {
        return stampType(stack) == type;
    }

    public static boolean damageStamp(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return false;
        }

        stack.setDamageValue(stack.getDamageValue() + 1);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.shrink(1);
            stack.setDamageValue(0);
            return true;
        }
        return false;
    }

    @Nullable
    private static StampType typeFor(String id) {
        if (id.equals("stamp_9") || id.equals("stamp_desh_9")) {
            return StampType.C9;
        }
        if (id.equals("stamp_50") || id.equals("stamp_desh_50")) {
            return StampType.C50;
        }
        if (id.equals("stamp_357") || id.equals("stamp_desh_357")) {
            return StampType.C357;
        }
        if (id.equals("stamp_44") || id.equals("stamp_desh_44")) {
            return StampType.C44;
        }
        if (id.endsWith("_flat")) {
            return StampType.FLAT;
        }
        if (id.endsWith("_plate")) {
            return StampType.PLATE;
        }
        if (id.endsWith("_wire")) {
            return StampType.WIRE;
        }
        if (id.endsWith("_circuit")) {
            return StampType.CIRCUIT;
        }
        return null;
    }

    private static int durabilityFor(String id) {
        if (id.startsWith("stamp_desh_")) {
            return 0;
        }
        if (id.startsWith("stamp_stone_")) {
            return 32;
        }
        if (id.startsWith("stamp_iron_")) {
            return 64;
        }
        if (id.startsWith("stamp_steel_")) {
            return 192;
        }
        if (id.startsWith("stamp_titanium_")) {
            return 256;
        }
        if (id.startsWith("stamp_obsidian_")) {
            return 512;
        }
        if (id.equals("stamp_357") || id.equals("stamp_44") || id.equals("stamp_9") || id.equals("stamp_50")) {
            return 1_000;
        }
        return 0;
    }

    public static boolean isRegisteredStamp(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ReinhardtsHBM.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("stamp_");
    }

    public enum StampType implements StringRepresentable {
        FLAT("flat"),
        PLATE("plate"),
        WIRE("wire"),
        CIRCUIT("circuit"),
        C357("c357"),
        C44("c44"),
        C50("c50"),
        C9("c9"),
        PRINTING1("printing1"),
        PRINTING2("printing2"),
        PRINTING3("printing3"),
        PRINTING4("printing4"),
        PRINTING5("printing5"),
        PRINTING6("printing6"),
        PRINTING7("printing7"),
        PRINTING8("printing8");

        public static final Codec<StampType> CODEC = StringRepresentable.fromEnum(StampType::values);

        private final String serializedName;

        StampType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
