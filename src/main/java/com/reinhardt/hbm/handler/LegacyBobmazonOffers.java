package com.reinhardt.hbm.handler;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyBobmazonItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/** The 1.7.10 Bobmazon order table. Entries are enabled only after their full port exists. */
public final class LegacyBobmazonOffers {
    public record Offer(ItemStack stack, Requirement requirement, int caps, int rating) { }

    public enum Requirement {
        NONE(null), STEEL("bobmazon_steel"), ASSEMBLY("bobmazon_assembly"), CHEMICS("bobmazon_chemics"),
        OIL("bobmazon_oil"), NUCLEAR("bobmazon_nuclear"), HIDDEN("bobmazon_hidden");

        private final String advancement;

        Requirement(@Nullable String advancement) {
            this.advancement = advancement;
        }

        public boolean isMet(ServerPlayer player) {
            if (this == NONE || player.getAbilities().instabuild) {
                return true;
            }
            MinecraftServer server = player.getServer();
            if (server == null || advancement == null) {
                return false;
            }
            var holder = server.getAdvancements().get(ReinhardtsHBM.id(advancement));
            return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
        }
    }

    private static final List<Offer> STANDARD = List.of(
            offer("minecraft:torch", 64, Requirement.NONE, 2, 0),
            offer("reinhardtshbm:definitelyfood", 16, Requirement.NONE, 4, 0),
            offer("reinhardtshbm:nitra", 4, Requirement.CHEMICS, 16, 0),
            offer("reinhardtshbm:geiger_counter", 1, Requirement.NONE, 16, 0),
            offer("reinhardtshbm:matchstick", 16, Requirement.STEEL, 2, 0),
            offer("reinhardtshbm:blueprint_folder", 1, Requirement.ASSEMBLY, 64, 0)
    );
    private static final List<Offer> HIDDEN = List.of();

    private LegacyBobmazonOffers() { }

    public static List<Offer> offers(boolean hidden) {
        return hidden ? HIDDEN : STANDARD;
    }

    @Nullable
    public static Offer request(ServerPlayer player, int index) {
        ItemStack held = player.getMainHandItem();
        LegacyBobmazonItem catalog;
        if (held.getItem() instanceof LegacyBobmazonItem mainHandCatalog) {
            catalog = mainHandCatalog;
        } else {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof LegacyBobmazonItem offhandCatalog)) {
                return null;
            }
            catalog = offhandCatalog;
        }
        List<Offer> offers = offers(catalog.hidden());
        return index >= 0 && index < offers.size() ? offers.get(index) : null;
    }

    public static boolean isBottleCap(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        return switch (id.getPath()) {
            case "cap_fritz", "cap_korl", "cap_nuka", "cap_quantum", "cap_rad", "cap_sparkle" -> true;
            default -> false;
        };
    }

    private static Offer offer(String itemId, int count, Requirement requirement, int caps, int rating) {
        return new Offer(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)), count), requirement, caps, rating);
    }
}
