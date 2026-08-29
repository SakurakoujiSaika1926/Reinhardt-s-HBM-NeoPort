package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Direct 1.7.10 ItemCustomLore port for ordinary lore-bearing materials and
 * components. The original class supplied descriptions, rarity and the rune
 * glint; it was not a plain Item registration.
 */
public final class LegacyLoreItem extends Item {
    private static final Set<String> LEGACY_IDS = Set.of(
            "ammo_dgk", "ball_resin", "billet_australium", "billet_australium_greater",
            "billet_australium_lesser", "billet_balefire_gold", "billet_flashlead", "billet_gh336",
            "billet_schrabidium", "bolt_spike", "book_secret", "bottle_mercury", "boy_bullet",
            "boy_target", "burnt_bark", "canister_napalm", "circuit_star", "coin_creeper",
            "coin_maskman", "coin_radiation", "coin_ufo", "coin_worm", "crystal_charred",
            "crystal_horn", "crystal_schrabidium", "crystal_schraranium", "custom_amat",
            "custom_dirty", "custom_fall", "custom_hydro", "custom_nuke", "custom_schrab",
            "custom_tnt", "dust", "early_explosive_lenses", "entanglement_kit", "explosive_lenses",
            "flame_conspiracy", "flame_opinion",
            "flame_politics", "flame_pony", "fuse", "gadget_core", "gem_rad", "gem_sodalite",
            "gem_tantalium", "gem_volcanic", "igniter", "ingot_actinium", "ingot_arsenic",
            "ingot_asbestos", "ingot_australium", "ingot_bakelite", "ingot_biorubber",
            "ingot_bismuth", "ingot_combine_steel", "ingot_desh", "ingot_dineutronium",
            "ingot_dura_steel", "ingot_electronium", "ingot_euphemium", "ingot_ferrouranium",
            "ingot_fiberglass", "ingot_gh336", "ingot_lanthanium", "ingot_neptunium",
            "ingot_osmiridium", "ingot_pc", "ingot_polymer", "ingot_pvc", "ingot_rubber",
            "ingot_saturnite", "ingot_schrabidate", "ingot_schrabidium", "ingot_tantalium",
            "key_red", "key_red_cracked", "magnetron", "man_core", "mech_key", "missile_soyuz_lander",
            "nugget_australium", "nugget_australium_greater", "nugget_australium_lesser", "nugget_desh",
            "nugget_dineutronium", "nugget_euphemium", "nugget_gh336", "nugget_osmiridium",
            "nugget_schrabidium", "nugget_tantalium", "nugget_zirconium", "pellet_cluster", "pin",
            "plate_bismuth", "plate_euphemium", "plate_paa", "plate_saturnite", "plate_schrabidium",
            "powder_actinium", "powder_asbestos", "powder_astatine", "powder_australium",
            "powder_bakelite", "powder_boron", "powder_bromine", "powder_caesium", "powder_cerium",
            "powder_cobalt", "powder_dineutronium", "powder_dura_steel", "powder_euphemium",
            "powder_fire", "powder_ice", "powder_iodine", "powder_lanthanium", "powder_neodymium",
            "powder_niobium", "powder_poison", "powder_polymer", "powder_power", "powder_schrabidate",
            "powder_schrabidium", "powder_strontium", "powder_tantalium", "powder_tennessine",
            "powder_thermite", "powder_thorium", "rune_blank", "rune_dagaz", "rune_hagalaz", "rune_isa",
            "rune_jera", "rune_thurisaz", "undefined", "upgrade_template", "watch", "cell_sas3"
    );

    private static final Set<String> EPIC = Set.of(
            "billet_gh336", "ingot_euphemium", "ingot_gh336", "nugget_euphemium", "nugget_gh336",
            "plate_euphemium", "powder_actinium", "powder_astatine", "powder_boron", "powder_bromine",
            "powder_caesium", "powder_cerium", "powder_cobalt", "powder_euphemium", "powder_iodine",
            "powder_lanthanium", "powder_neodymium", "powder_niobium", "powder_strontium",
            "powder_tennessine", "watch"
    );
    private static final Set<String> RARE = Set.of(
            "billet_schrabidium", "crystal_schrabidium", "crystal_schraranium", "ingot_osmiridium",
            "ingot_saturnite", "ingot_schrabidate", "ingot_schrabidium", "nugget_osmiridium",
            "nugget_schrabidium", "plate_saturnite", "plate_schrabidium", "powder_schrabidate",
            "powder_schrabidium", "cell_sas3"
    );
    private static final Set<String> UNCOMMON = Set.of(
            "billet_australium", "billet_australium_greater", "billet_australium_lesser",
            "billet_balefire_gold", "billet_flashlead", "boy_bullet", "boy_target", "circuit_star",
            "coin_creeper", "coin_maskman", "coin_radiation", "coin_ufo", "coin_worm", "gadget_core",
            "gem_rad", "gem_volcanic", "ingot_australium", "man_core", "nugget_australium",
            "nugget_australium_greater", "nugget_australium_lesser", "plate_paa", "powder_australium",
            "powder_power", "powder_thorium"
    );
    private static final Set<String> GLINTING = Set.of(
            "rune_blank", "rune_dagaz", "rune_hagalaz", "rune_isa", "rune_jera", "rune_thurisaz"
    );

    private final String id;
    private final Supplier<Item> craftingRemainder;

    private LegacyLoreItem(String id, Properties properties, Supplier<Item> craftingRemainder) {
        super(properties.rarity(rarityFor(id)));
        this.id = id;
        this.craftingRemainder = craftingRemainder;
    }

    public static boolean isLegacyLoreItem(String id) {
        return LEGACY_IDS.contains(id);
    }

    public static LegacyLoreItem fromLegacyId(String id) {
        return fromLegacyId(id, new Properties());
    }

    public static LegacyLoreItem fromLegacyId(String id, Properties properties) {
        return fromLegacyId(id, properties, null);
    }

    public static LegacyLoreItem fromLegacyId(String id, Properties properties, Supplier<Item> craftingRemainder) {
        if (!isLegacyLoreItem(id)) {
            throw new IllegalArgumentException("Unknown 1.7.10 ItemCustomLore id: " + id);
        }
        return new LegacyLoreItem(id, properties, craftingRemainder);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return GLINTING.contains(id);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return craftingRemainder != null;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return craftingRemainder == null ? ItemStack.EMPTY : new ItemStack(craftingRemainder.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = getDescriptionId() + ".desc";
        String description = Component.translatable(key).getString();
        if (!description.equals(key)) {
            for (String line : description.split("\\\\$")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static Rarity rarityFor(String id) {
        if (EPIC.contains(id)) {
            return Rarity.EPIC;
        }
        if (RARE.contains(id)) {
            return Rarity.RARE;
        }
        return UNCOMMON.contains(id) ? Rarity.UNCOMMON : Rarity.COMMON;
    }
}
