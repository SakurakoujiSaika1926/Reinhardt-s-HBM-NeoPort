package com.reinhardt.hbm.handler;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.block.PlushieType;
import com.reinhardt.hbm.block.SnowglobeType;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.BlueprintFolderItem;
import com.reinhardt.hbm.item.ConcreteColoredBlockItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.LegacyBobmazonItem;
import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.item.LegacyCustomKitItem;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.item.PlushieBlockItem;
import com.reinhardt.hbm.item.SnowglobeBlockItem;
import com.reinhardt.hbm.item.VendingMachineBlockItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Complete non-firearm 1.7.10 Bobmazon order table. */
public final class LegacyBobmazonOffers {
    public record Offer(ItemStack stack, Requirement requirement, int caps, int rating) { }

    public enum Requirement {
        NONE(null), STEEL("blast_furnace"), ASSEMBLY("assembly"), CHEMICS("chemplant"),
        OIL("desh"), NUCLEAR("technetium"), HIDDEN("hidden");

        private final String advancement;

        Requirement(@Nullable String advancement) {
            this.advancement = advancement;
        }

        public boolean isMet(ServerPlayer player) {
            if (this == NONE || player.getAbilities().instabuild) return true;
            return advancement != null && HbmAdvancements.has(player, advancement);
        }
    }

    private LegacyBobmazonOffers() { }

    public static List<Offer> offers(boolean hidden, HolderLookup.Provider registries) {
        return hidden ? hiddenOffers(registries) : standardOffers();
    }

    @Nullable
    public static Offer request(ServerPlayer player, int index) {
        ItemStack held = player.getMainHandItem();
        LegacyBobmazonItem catalog;
        if (held.getItem() instanceof LegacyBobmazonItem mainHandCatalog) {
            catalog = mainHandCatalog;
        } else {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof LegacyBobmazonItem offhandCatalog)) return null;
            catalog = offhandCatalog;
        }
        List<Offer> offers = offers(catalog.hidden(), player.registryAccess());
        return index >= 0 && index < offers.size() ? offers.get(index) : null;
    }

    public static boolean isBottleCap(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "cap_fritz", "cap_korl", "cap_nuka", "cap_quantum", "cap_rad", "cap_sparkle" -> true;
            default -> false;
        };
    }

    private static List<Offer> standardOffers() {
        List<Offer> offers = new ArrayList<>();
        offers.add(offer("minecraft:torch", 64, Requirement.NONE, 2, 0));
        offers.add(offer("reinhardtshbm:definitelyfood", 16, Requirement.NONE, 4, 0));
        offers.add(offer("reinhardtshbm:nitra", 4, Requirement.CHEMICS, 16, 0));
        // The old firearm kit was intentionally retired together with HBM's firearm system.
        offers.add(offer("reinhardtshbm:geiger_counter", 1, Requirement.NONE, 16, 0));
        offers.add(offer("reinhardtshbm:matchstick", 16, Requirement.STEEL, 2, 0));
        offers.add(new Offer(BlueprintFolderItem.stackFor(BlueprintFolderItem.Variant.STANDARD), Requirement.ASSEMBLY, 64, 0));
        offers.add(new Offer(BlueprintFolderItem.stackFor(BlueprintFolderItem.Variant.DISCOVER), Requirement.OIL, 256, 0));
        offers.add(vendingMachineOffer(0));
        offers.add(vendingMachineOffer(1));
        offers.add(offer("minecraft:jungle_sapling", 1, Requirement.STEEL, 12, 9));
        offers.add(offer("reinhardtshbm:plant_foxglove", 1, Requirement.STEEL, 16, 5));
        offers.add(offer("reinhardtshbm:plant_tobacco", 1, Requirement.STEEL, 16, 9));
        offers.add(offer("reinhardtshbm:plant_nightshade", 1, Requirement.STEEL, 16, 3));
        offers.add(offer("reinhardtshbm:plant_hemp", 1, Requirement.STEEL, 4, 10));
        offers.add(offer("reinhardtshbm:plant_mustard_willow", 1, Requirement.NUCLEAR, 64, 8));

        Item concreteItem = requiredItem("reinhardtshbm:concrete_colored_ext");
        if (!(concreteItem instanceof ConcreteColoredBlockItem concrete)) throw new IllegalStateException("Bobmazon concrete offer resolved to " + concreteItem);
        for (int meta = 0; meta < 8; meta++) offers.add(new Offer(ConcreteColoredBlockItem.stackFor(concrete, meta).copyWithCount(16), Requirement.CHEMICS, 4, 0));

        Item snowglobeItem = requiredItem("reinhardtshbm:snowglobe");
        if (!(snowglobeItem instanceof SnowglobeBlockItem snowglobe)) throw new IllegalStateException("Bobmazon snowglobe offer resolved to " + snowglobeItem);
        for (SnowglobeType type : SnowglobeType.values()) {
            if (type != SnowglobeType.NONE) offers.add(new Offer(SnowglobeBlockItem.stackFor(snowglobe, type), Requirement.CHEMICS, 128, 0));
        }

        Item plushieItem = requiredItem("reinhardtshbm:plushie");
        if (!(plushieItem instanceof PlushieBlockItem plushie)) throw new IllegalStateException("Bobmazon plushie offer resolved to " + plushieItem);
        for (PlushieType type : PlushieType.values()) {
            if (type != PlushieType.NONE) offers.add(new Offer(PlushieBlockItem.stackFor(plushie, type), Requirement.OIL, 16, type.ordinal() < 3 ? 10 : 0));
        }
        return List.copyOf(offers);
    }

    private static List<Offer> hiddenOffers(HolderLookup.Provider registries) {
        List<Offer> offers = new ArrayList<>();
        add(offers, "minecraft:iron_ingot", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_steel", 64, Requirement.STEEL, 1);
        add(offers, "minecraft:copper_ingot", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_red_copper", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_titanium", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_tungsten", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_cobalt", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_desh", 64, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_tantalium", 64, Requirement.STEEL, 5);
        add(offers, "reinhardtshbm:ingot_bismuth", 16, Requirement.STEEL, 5);
        add(offers, "reinhardtshbm:ingot_schrabidium", 16, Requirement.STEEL, 5);
        add(offers, "reinhardtshbm:ingot_euphemium", 8, Requirement.STEEL, 16);
        add(offers, "reinhardtshbm:ingot_dineutronium", 1, Requirement.STEEL, 16);
        add(offers, "reinhardtshbm:ingot_starmetal", 16, Requirement.STEEL, 8);
        add(offers, "reinhardtshbm:ingot_semtex", 16, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_u235", 16, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:ingot_pu239", 16, Requirement.STEEL, 1);
        // ammo_container and both gun kits belonged to the retired firearm system.
        for (String id : List.of("nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit", "boy_kit", "prototype_kit", "missile_kit")) {
            add(offers, "reinhardtshbm:" + id, 1, Requirement.STEEL, id.equals("prototype_kit") ? 10 : 5);
        }
        add(offers, "reinhardtshbm:jetpack_vector", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:jetpack_tank", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:struct_launcher_core", 1, Requirement.STEEL, 3);
        add(offers, "reinhardtshbm:struct_launcher_core_large", 1, Requirement.STEEL, 3);
        add(offers, "reinhardtshbm:struct_launcher", 40, Requirement.STEEL, 7);
        add(offers, "reinhardtshbm:struct_scaffold", 11, Requirement.STEEL, 7);
        add(offers, "reinhardtshbm:loot_10", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:loot_15", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:loot_misc", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:crate_can", 1, Requirement.STEEL, 1);
        add(offers, "reinhardtshbm:crate_ammo", 1, Requirement.STEEL, 2);
        add(offers, "reinhardtshbm:crucible", 1, Requirement.STEEL, 10);
        add(offers, "reinhardtshbm:chopper", 1, Requirement.STEEL, 10);
        add(offers, "reinhardtshbm:spawn_worm", 1, Requirement.STEEL, 10);
        add(offers, "reinhardtshbm:spawn_ufo", 1, Requirement.STEEL, 10);
        add(offers, "reinhardtshbm:sat_laser", 1, Requirement.HIDDEN, 8);
        add(offers, "reinhardtshbm:sat_gerald", 1, Requirement.HIDDEN, 32);
        add(offers, "reinhardtshbm:billet_yharonite", 4, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:ingot_chainsteel", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:ingot_electronium", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:book_of_", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:mese_pickaxe", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:mysteryshovel", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:ntm_dirt", 1, Requirement.HIDDEN, 16);
        add(offers, "reinhardtshbm:euphemium_kit", 1, Requirement.HIDDEN, 64);
        offers.add(new Offer(fusionManKit(registries), Requirement.HIDDEN, 64, 0));
        offers.add(new Offer(nutBucketKit(registries), Requirement.HIDDEN, 64, 0));
        // The two remaining old custom offers contained retired firearms and ammunition.
        return List.copyOf(offers);
    }

    private static ItemStack fusionManKit(HolderLookup.Provider registries) {
        ItemStack battery = stack("reinhardtshbm:battery_spark", 1);
        BatteryPackItem.setStoredCharge(battery, BatteryPackItem.capacity(battery));
        ItemStack kit = named(stack("reinhardtshbm:kit_custom", 1), "Fusion Man");
        return LegacyCustomKitItem.create(kit, registries, 0xff00ff, 0x800080,
                stack("reinhardtshbm:fusion_klystron", 1), stack("reinhardtshbm:fusion_torus", 1), stack("reinhardtshbm:fusion_mhdt", 1),
                stack("reinhardtshbm:machine_intake", 3), battery, stack("reinhardtshbm:machine_chemical_factory", 4),
                stack("reinhardtshbm:machine_fluidtank", 8), stack("reinhardtshbm:red_wire_coated", 64), stack("reinhardtshbm:red_cable", 64),
                fluidBarrel("deuterium", 64), fluidBarrel("tritium", 64), fluidBarrel("perfluoromethyl", 64),
                stack("reinhardtshbm:red_pylon_large", 8), stack("reinhardtshbm:substation", 4), stack("reinhardtshbm:red_connector", 64),
                stack("reinhardtshbm:wiring_red_copper", 1), stack("reinhardtshbm:machine_chungus", 3), stack("reinhardtshbm:template_folder", 1),
                new ItemStack(Items.PAPER, 64), new ItemStack(Items.INK_SAC, 64));
    }

    private static ItemStack nutBucketKit(HolderLookup.Provider registries) {
        ItemStack conserve = LegacyConserveItem.stackFor(requiredItem("reinhardtshbm:canned_conserve"), LegacyConserveItem.Variant.SLIME).copyWithCount(64);
        return LegacyCustomKitItem.create(named(stack("reinhardtshbm:kit_custom", 1), "The Nut Bucket"), registries, 0xFFFFFF, 0xFFFFFF,
                named(stack("reinhardtshbm:rod_of_discord", 1), "Cock Joke"), named(conserve, "Class A Horse Semen"),
                named(stack("reinhardtshbm:pipe_lead", 1), "Get Nutted, Dumbass"), stack("reinhardtshbm:gem_alexandrite", 1));
    }

    private static ItemStack fluidBarrel(String fluid, int count) {
        ItemStack stack = HbmFluidContainerItem.makeFull(() -> requiredItem("reinhardtshbm:fluid_barrel_full"),
                HbmFluids.byName(fluid).orElseThrow(() -> new IllegalStateException("Missing HBM fluid " + fluid)));
        return stack.copyWithCount(count);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void add(List<Offer> offers, String itemId, int count, Requirement requirement, int caps) {
        offers.add(offer(itemId, count, requirement, caps, 0));
    }

    private static Offer offer(String itemId, int count, Requirement requirement, int caps, int rating) {
        return new Offer(stack(itemId, count), requirement, caps, rating);
    }

    private static ItemStack stack(String itemId, int count) {
        return new ItemStack(requiredItem(itemId), count);
    }

    private static Item requiredItem(String itemId) {
        ResourceLocation id = ResourceLocation.parse(itemId);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(ResourceLocation.withDefaultNamespace("air"))) throw new IllegalStateException("Missing Bobmazon offer item " + id);
        return item;
    }

    private static Offer vendingMachineOffer(int variant) {
        Item item = requiredItem("reinhardtshbm:vending_machine");
        if (!(item instanceof VendingMachineBlockItem vending)) throw new IllegalStateException("Vending-machine Bobmazon offer resolved to " + item);
        return new Offer(LegacyVariantBlockItem.stackFor(vending, variant), Requirement.CHEMICS, 64, 0);
    }
}
