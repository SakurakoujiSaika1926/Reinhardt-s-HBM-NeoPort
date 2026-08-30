package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.worldgen.BedrockOreDepositFeature;
import com.reinhardt.hbm.worldgen.BedrockOilDepositFeature;
import com.reinhardt.hbm.worldgen.ChlorineGeyserFeature;
import com.reinhardt.hbm.worldgen.DepthClusterDepositFeature;
import com.reinhardt.hbm.worldgen.MeteoriteFeature;
import com.reinhardt.hbm.worldgen.LanternBehemothFeature;
import com.reinhardt.hbm.worldgen.GlyphidHiveFeature;
import com.reinhardt.hbm.worldgen.NetherDepthNeodymiumFeature;
import com.reinhardt.hbm.worldgen.NetherOreLegacyFeature;
import com.reinhardt.hbm.worldgen.OilBubbleFeature;
import com.reinhardt.hbm.worldgen.OilSandBubbleFeature;
import com.reinhardt.hbm.worldgen.SchistStratumFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
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
            FEATURES.register("depth_cluster_iron", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEPTH_IRON, 24));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_CLUSTER_TITANIUM =
            FEATURES.register("depth_cluster_titanium", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEPTH_TITANIUM, 32));
    public static final DeferredHolder<Feature<?>, DepthClusterDepositFeature> DEPTH_CLUSTER_TUNGSTEN =
            FEATURES.register("depth_cluster_tungsten", () -> new DepthClusterDepositFeature(HbmBlocks.CLUSTER_DEPTH_TUNGSTEN, 32));
    public static final DeferredHolder<Feature<?>, MeteoriteFeature> METEORITE =
            FEATURES.register("meteorite", () -> new MeteoriteFeature());
    public static final DeferredHolder<Feature<?>, LanternBehemothFeature> LANTERN_BEHEMOTH =
            FEATURES.register("lantern_behemoth", LanternBehemothFeature::new);
    public static final DeferredHolder<Feature<?>, GlyphidHiveFeature> GLYPHID_HIVE =
            FEATURES.register("glyphid_hive", () -> new GlyphidHiveFeature());
    public static final DeferredHolder<Feature<?>, SchistStratumFeature> SCHIST_STRATUM =
            FEATURES.register("schist_stratum", () -> new SchistStratumFeature());
    public static final DeferredHolder<Feature<?>, NetherOreLegacyFeature> NETHER_ORE_LEGACY =
            FEATURES.register("nether_ore_legacy", () -> new NetherOreLegacyFeature());
    public static final DeferredHolder<Feature<?>, NetherDepthNeodymiumFeature> NETHER_DEPTH_NEODYMIUM =
            FEATURES.register("nether_depth_neodymium", () -> new NetherDepthNeodymiumFeature());
    public static final DeferredHolder<Feature<?>, ChlorineGeyserFeature> CHLORINE_GEYSER =
            FEATURES.register("chlorine_geyser", () -> new ChlorineGeyserFeature());

    private HbmWorldgenFeatures() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
