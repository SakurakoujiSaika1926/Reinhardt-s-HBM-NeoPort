package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.worldgen.BedrockOreDepositFeature;
import com.reinhardt.hbm.worldgen.BedrockOilDepositFeature;
import com.reinhardt.hbm.worldgen.AustraliumRegionFeature;
import com.reinhardt.hbm.worldgen.ChlorineGeyserFeature;
import com.reinhardt.hbm.worldgen.ColtanDepositFeature;
import com.reinhardt.hbm.worldgen.ConfigGatedOreFeature;
import com.reinhardt.hbm.worldgen.DepthClusterDepositFeature;
import com.reinhardt.hbm.worldgen.LegacySurfaceWorldgenFeature;
import com.reinhardt.hbm.worldgen.MeteoriteFeature;
import com.reinhardt.hbm.worldgen.LanternBehemothFeature;
import com.reinhardt.hbm.worldgen.GlyphidHiveFeature;
import com.reinhardt.hbm.worldgen.NetherDepthNeodymiumFeature;
import com.reinhardt.hbm.worldgen.NetherOreLegacyFeature;
import com.reinhardt.hbm.worldgen.OreCaveFeature;
import com.reinhardt.hbm.worldgen.OilBubbleFeature;
import com.reinhardt.hbm.worldgen.OilSandBubbleFeature;
import com.reinhardt.hbm.worldgen.RadiationHotspotFeature;
import com.reinhardt.hbm.worldgen.ResourceOreLayer3DFeature;
import com.reinhardt.hbm.worldgen.SchistStratumFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmWorldgenFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<Feature<?>, OilBubbleFeature> OIL_BUBBLE =
            FEATURES.register("oil_bubble", () -> new OilBubbleFeature());
    public static final DeferredHolder<Feature<?>, OilSandBubbleFeature> OIL_SAND_BUBBLE =
            FEATURES.register("oil_sand_bubble", () -> new OilSandBubbleFeature());
    public static final DeferredHolder<Feature<?>, BedrockOreDepositFeature> BEDROCK_ORE_DEPOSIT =
            FEATURES.register("bedrock_ore_deposit", () -> new BedrockOreDepositFeature());
    public static final DeferredHolder<Feature<?>, BedrockOilDepositFeature> BEDROCK_OIL_DEPOSIT =
            FEATURES.register("bedrock_oil_deposit", () -> new BedrockOilDepositFeature());
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_CLUSTER_IRON =
            FEATURES.register("depth_cluster_iron", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEEPSLATE_IRON, 24));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_CLUSTER_TITANIUM =
            FEATURES.register("depth_cluster_titanium", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEEPSLATE_TITANIUM, 32));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_CLUSTER_TUNGSTEN =
            FEATURES.register("depth_cluster_tungsten", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEEPSLATE_TUNGSTEN, 32));
    // Legacy DepthDeposit entries (1.7.10: size 5, fill 0.8, one deposit per 16 chunks).
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_ORE_CINNEBAR =
            FEATURES.register("depth_ore_cinnebar", () -> new DepthClusterDepositFeature(HbmBlocks.ORE_DEPTH_CINNEBAR, 16, 0.8D));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_ORE_BORAX =
            FEATURES.register("depth_ore_borax", () -> new DepthClusterDepositFeature(HbmBlocks.ORE_DEPTH_BORAX, 16, 0.8D));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_ORE_ZIRCONIUM =
            FEATURES.register("depth_ore_zirconium", () -> new DepthClusterDepositFeature(HbmBlocks.ORE_DEPTH_ZIRCONIUM, 16, 0.8D));
    public static final DeferredHolder<Feature<?>, AustraliumRegionFeature> AUSTRALIUM_REGION =
            FEATURES.register("australium_region", () -> new AustraliumRegionFeature());
    public static final DeferredHolder<Feature<?>, MeteoriteFeature> METEORITE =
            FEATURES.register("meteorite", () -> new MeteoriteFeature());
    public static final DeferredHolder<Feature<?>, LanternBehemothFeature> LANTERN_BEHEMOTH =
            FEATURES.register("lantern_behemoth", LanternBehemothFeature::new);
    public static final DeferredHolder<Feature<?>, GlyphidHiveFeature> GLYPHID_HIVE =
            FEATURES.register("glyphid_hive", () -> new GlyphidHiveFeature());
    public static final DeferredHolder<Feature<?>, SchistStratumFeature> SCHIST_STRATUM =
            FEATURES.register("schist_stratum", () -> new SchistStratumFeature());
    public static final DeferredHolder<Feature<?>, ResourceOreLayer3DFeature> RESOURCE_LAYER_HEMATITE =
            FEATURES.register("resource_layer_hematite", ResourceOreLayer3DFeature::hematite);
    public static final DeferredHolder<Feature<?>, ResourceOreLayer3DFeature> RESOURCE_LAYER_BAUXITE =
            FEATURES.register("resource_layer_bauxite", ResourceOreLayer3DFeature::bauxite);
    public static final DeferredHolder<Feature<?>, ResourceOreLayer3DFeature> RESOURCE_LAYER_MALACHITE =
            FEATURES.register("resource_layer_malachite", ResourceOreLayer3DFeature::malachite);
    public static final DeferredHolder<Feature<?>, ConfigGatedOreFeature> ORE_COLTAN_528 =
            FEATURES.register("ore_coltan_528", () -> new ConfigGatedOreFeature(OreConfiguration.CODEC, () -> HbmConfig.ENABLE_528_COLTAN_SPAWN.get()));
    public static final DeferredHolder<Feature<?>, ColtanDepositFeature> COLTAN_DEPOSIT =
            FEATURES.register("coltan_deposit", () -> new ColtanDepositFeature());
    public static final DeferredHolder<Feature<?>, NetherOreLegacyFeature> NETHER_ORE_LEGACY =
            FEATURES.register("nether_ore_legacy", () -> new NetherOreLegacyFeature());
    public static final DeferredHolder<Feature<?>, NetherDepthNeodymiumFeature> NETHER_DEPTH_NEODYMIUM =
            FEATURES.register("nether_depth_neodymium", () -> new NetherDepthNeodymiumFeature());
    public static final DeferredHolder<Feature<?>, ChlorineGeyserFeature> CHLORINE_GEYSER =
            FEATURES.register("chlorine_geyser", () -> new ChlorineGeyserFeature());
    public static final DeferredHolder<Feature<?>, LegacySurfaceWorldgenFeature> LEGACY_SURFACE_WORLDGEN =
            FEATURES.register("legacy_surface_worldgen", () -> new LegacySurfaceWorldgenFeature());
    public static final DeferredHolder<Feature<?>, OreCaveFeature> SULFUR_CAVE =
            FEATURES.register("sulfur_cave", OreCaveFeature::sulfur);
    public static final DeferredHolder<Feature<?>, OreCaveFeature> ASBESTOS_CAVE =
            FEATURES.register("asbestos_cave", OreCaveFeature::asbestos);
    public static final DeferredHolder<Feature<?>, RadiationHotspotFeature> RADIATION_HOTSPOT =
            FEATURES.register("radiation_hotspot", () -> new RadiationHotspotFeature());

    private HbmWorldgenFeatures() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
