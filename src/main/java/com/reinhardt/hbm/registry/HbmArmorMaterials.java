package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class HbmArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PAA = register(
            "paa", 3, 6, 8, 3, 25, 2.0F, commonTag("plates/paa")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT = register(
            "hazmat", 1, 2, 3, 1, 5, 0.0F, item("hazmat_cloth")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_RED = register(
            "hazmat_red", 1, 2, 3, 1, 5, 0.0F, item("hazmat_cloth_red")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_GREY = register(
            "hazmat_grey", 1, 2, 3, 1, 5, 0.0F, item("hazmat_cloth_grey")
    );
    /** Separate layer for the PAA hazmat variant; 1.7.10 used a distinct texture set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_PAA = register(
            "hazmat_paa", 3, 6, 8, 3, 25, 2.0F, commonTag("plates/paa")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ASBESTOS = register(
            "asbestos", 1, 3, 4, 1, 5, 0.0F, item("asbestos_cloth")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL = register(
            "steel", 2, 5, 6, 2, 5, 0.0F, commonTag("ingots/steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TITANIUM = register(
            "titanium", 3, 6, 8, 3, 9, 2.0F, commonTag("ingots/titanium")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ALLOY = register(
            "alloy", 3, 6, 8, 3, 12, 0.0F, commonTag("ingots/advanced_alloy")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> COBALT = register(
            "cobalt", 3, 6, 8, 3, 25, 2.0F, Ingredient::of
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SECURITY = register(
            "security", 3, 6, 8, 3, 15, 2.0F, commonTag("plates/kevlar")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STARMETAL = register(
            "starmetal", 3, 6, 8, 3, 100, 2.0F, Ingredient::of
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROBES = register(
            "robes", 2, 5, 6, 2, 5, 0.0F, commonTag("ingots/steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DNT = register(
            "dnt", 1, 1, 1, 1, 0, 0.0F, Ingredient::of
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CMB = register(
            "cmb", 3, 6, 8, 3, 50, 2.0F, commonTag("ingots/combine_steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SCHRABIDIUM = register(
            "schrabidium", 3, 6, 8, 3, 50, 2.0F, commonTag("ingots/schrabidium")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BISMUTH = register(
            "bismuth", 3, 6, 8, 3, 100, 0.0F, commonTag("plates/bismuth")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EUPHEMIUM = register(
            "euphemium", 3, 6, 8, 3, 100, 0.0F, commonTag("ingots/euphemium")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ZIRCONIUM = register(
            "zirconium", 1, 3, 5, 2, 1_000, 0.0F, commonTag("ingots/zirconium")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RAGS = register(
            "rags", 1, 1, 1, 1, 0, 0.0F, item("rag")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOGGLES = register(
            "goggles", 2, 5, 6, 2, 9, 0.0F, Ingredient::of
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ASH_GLASSES = register(
            "ashglasses", 2, 5, 6, 2, 9, 0.0F, Ingredient::of
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RAGS_DAMP = register(
            "rag_damp", 1, 1, 1, 1, 0, 0.0F, item("rag")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RAGS_PISS = register(
            "rag_piss", 1, 1, 1, 1, 0, 0.0F, item("rag")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAT = register(
            "hat", 3, 6, 8, 3, 12, 0.0F, commonTag("ingots/advanced_alloy")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NO9 = register(
            "no9", 3, 6, 8, 3, 5, 0.0F, commonTag("ingots/steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JACKT = register(
            "jackt", 3, 6, 8, 3, 5, 0.0F, commonTag("ingots/steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JACKT2 = register(
            "jackt2", 3, 6, 8, 3, 5, 0.0F, commonTag("ingots/steel")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LIQUIDATOR = register(
            "liquidator", 3, 6, 8, 3, 10, 0.0F, commonTag("plates/lead")
    );
    /** Vanilla 1.7.10 ArmorMaterial.IRON values used exclusively by MaskOfInfamy. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INFAMY = register(
            "infamy", 2, 5, 6, 2, 9, 0.0F, Ingredient::of
    );
    // 1.7.10 powered-suit materials. The behaviour is defined by their item groups;
    // these entries preserve the original defense and repair component data.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> T51 = register(
            "t51", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_titanium")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEAMSUIT = register(
            "steamsuit", 3, 8, 6, 3, 0, 0.0F, commonTag("ingots/desh")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DIESELSUIT = register(
            "dieselsuit", 3, 8, 6, 3, 0, 0.0F, commonTag("plates/copper")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> AJR = register(
            "ajr", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_ajr")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NCRPA = register(
            "ncrpa", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_ajr")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLACKJACK = register(
            "blackjack", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_lunar")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ENVSUIT = register(
            "envsuit", 3, 8, 6, 3, 10, 0.0F, item("plate_armor_hev")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HEV = register(
            "hev", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_hev")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FAU = register(
            "fau", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_fau")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DNS = register(
            "dns", 3, 8, 6, 3, 0, 0.0F, item("plate_armor_dnt")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TAURUN = register(
            "taurun", 3, 8, 6, 3, 10, 0.0F, commonTag("plates/iron")
    );
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRENCHMASTER = register(
            "trenchmaster", 3, 8, 6, 3, 0, 0.0F, commonTag("plates/iron")
    );

    private HbmArmorMaterials() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(
            String name,
            int boots,
            int leggings,
            int chestplate,
            int helmet,
            int enchantment,
            float toughness,
            Supplier<Ingredient> repairIngredient
    ) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                defense(boots, leggings, chestplate, helmet),
                enchantment,
                SoundEvents.ARMOR_EQUIP_GENERIC,
                repairIngredient,
                List.of(new ArmorMaterial.Layer(ReinhardtsHBM.id(name))),
                toughness,
                0.0F
        ));
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chestplate);
        return defense;
    }

    private static Supplier<Ingredient> commonTag(String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        return () -> Ingredient.of(tag);
    }

    private static Supplier<Ingredient> item(String path) {
        return () -> BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id(path))
                .map(Ingredient::of)
                .orElseGet(Ingredient::of);
    }

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
