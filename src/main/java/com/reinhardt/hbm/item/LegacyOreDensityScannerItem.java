package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.BedrockOreItem.Type;
import com.reinhardt.hbm.network.PlayerInformPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/** Direct equivalent of ItemOreDensityScanner's persistent bedrock-ore readout. */
public final class LegacyOreDensityScannerItem extends Item {
    public LegacyOreDensityScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof ServerPlayer player) || level.getGameTime() % 5L != 0L) {
            return;
        }

        double totalLevel = 0.0D;
        for (Type type : Type.values()) {
            double levelValue = BedrockOreBaseItem.getOreLevel(player.getBlockX(), player.getBlockZ(), type);
            String density = densityKey(levelValue);
            PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                    777 + type.ordinal(),
                    "message.reinhardtshbm.ore_density_scanner.entry",
                    densityColor(levelValue),
                    4000,
                    "@item.reinhardtshbm.bedrock_ore_new.type." + type.id() + ".name",
                    String.format(Locale.ROOT, "%.2f", levelValue),
                    "@" + density
            ));
            totalLevel += levelValue;
        }

        totalLevel /= Type.values().length;
        int tier = boreTier(totalLevel);
        HbmFluidDefinition fluid = boreFluid(totalLevel);
        PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                777 + Type.values().length,
                "message.reinhardtshbm.ore_density_scanner.summary",
                0xFFFF55,
                4000,
                Integer.toString(tier),
                fluid == null ? "" : " - 2000mB ",
                fluid == null ? "" : "@" + fluid.translationKey()
        ));
    }

    private static String densityKey(double density) {
        if (density <= 0.1D) return "message.reinhardtshbm.ore_density_scanner.very_poor";
        if (density <= 0.35D) return "message.reinhardtshbm.ore_density_scanner.poor";
        if (density <= 0.75D) return "message.reinhardtshbm.ore_density_scanner.low";
        if (density >= 1.9D) return "message.reinhardtshbm.ore_density_scanner.excellent";
        if (density >= 1.65D) return "message.reinhardtshbm.ore_density_scanner.very_high";
        if (density >= 1.25D) return "message.reinhardtshbm.ore_density_scanner.high";
        return "message.reinhardtshbm.ore_density_scanner.moderate";
    }

    private static int densityColor(double density) {
        if (density <= 0.1D) return 0xAA0000;
        if (density <= 0.35D) return 0xFF5555;
        if (density <= 0.75D) return 0xFFAA00;
        if (density > 2.0D) return 0xFF55FF;
        if (density >= 1.9D) return 0x55FFFF;
        if (density >= 1.65D) return 0x5555FF;
        if (density >= 1.25D) return 0x55FF55;
        return 0xFFFF55;
    }

    private static int boreTier(double density) {
        if (density > 1.5D) return 4;
        if (density > 1.0D) return 3;
        if (density > 0.75D) return 2;
        return 1;
    }

    private static HbmFluidDefinition boreFluid(double density) {
        if (density > 1.5D) return com.reinhardt.hbm.registry.HbmFluids.byName("solvent").orElse(null);
        if (density > 1.0D) return com.reinhardt.hbm.registry.HbmFluids.byName("sulfuric_acid").orElse(null);
        if (density > 0.75D) return com.reinhardt.hbm.registry.HbmFluids.byName("water").orElse(null);
        return null;
    }
}
