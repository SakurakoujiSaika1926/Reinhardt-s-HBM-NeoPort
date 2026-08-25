package com.reinhardt.hbm.item;

import com.reinhardt.hbm.satellite.SatelliteSavedData;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;

/** Server-side port of ItemModLens' scanner-satellite ore sweep. */
public final class LegacyNeutrinoLensItem extends ArmorModItem {
    private static final int RANGE_CHUNKS = 3;
    private static final int MAX_HITS = 100;

    public LegacyNeutrinoLensItem(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, false, false, false);
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack lens = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.EXTRA];
        if (!lens.is(this)) {
            return;
        }
        SatelliteSavedData.SatelliteRecord satellite = SatelliteSavedData.get(level)
                .satellite(SatelliteChipItem.frequency(lens)).orElse(null);
        if (satellite == null || satellite.kind() != SatelliteSavedData.SatelliteKind.SCANNER) {
            return;
        }

        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();
        int height = Math.max(Math.min(player.blockPosition().getY() + 10, 255), 64);
        int y = (int) (level.getGameTime() % height);
        int centerChunkX = x >> 4;
        int centerChunkZ = z >> 4;
        int hits = 0;

        for (int chunkX = centerChunkX - RANGE_CHUNKS; chunkX <= centerChunkX + RANGE_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunkZ - RANGE_CHUNKS; chunkZ <= centerChunkZ + RANGE_CHUNKS; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        BlockPos target = new BlockPos((chunkX << 4) + localX, y, (chunkZ << 4) + localZ);
                        Marker marker = markerFor(level.getBlockState(target));
                        if (marker != null && player.getRandom().nextInt(marker.chance()) == 0) {
                            level.sendParticles(new DustParticleOptions(marker.color(), 1.0F),
                                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                                    1, 0.0D, 0.0D, 0.0D, 0.0D);
                            if (++hits > MAX_HITS) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static Marker markerFor(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return switch (id.getPath()) {
            case "ore_alexandrite" -> marker(1, 0x00FFFF);
            case "ore_oil", "ore_bedrock_oil" -> marker(300, 0xA0A0A0);
            case "ore_coltan" -> marker(5, 0xA0A000);
            case "stone_gneiss" -> marker(5_000, 0x8080FF);
            case "ore_australium" -> marker(1_000, 0xFFFF00);
            case "end_portal_frame" -> marker(1, 0x40B080);
            case "volcano_core" -> marker(1, 0xFF4000);
            case "pink_log" -> marker(1, 0xFF00FF);
            case "bobblehead", "deco_loot", "crate_ammo", "crate_can", "ore_bedrock" -> marker(1, 0xFF0000);
            default -> null;
        };
    }

    private static Marker marker(int chance, int rgb) {
        return new Marker(chance, new Vector3f(((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.satellite.frequency", SatelliteChipItem.frequency(stack))
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private record Marker(int chance, Vector3f color) {
    }
}
