package com.reinhardt.hbm;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.config.HbmClientConfig;
import com.reinhardt.hbm.item.LegacyItemComponents;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmArmorMaterials;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmCreativeTabs;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.network.HbmNetwork;
import com.reinhardt.hbm.registry.HbmConditionSerializers;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.registry.HbmWorldgenFeatures;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import com.reinhardt.hbm.registry.LegacyHbmContent;
import com.reinhardt.hbm.registry.PortStatus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ReinhardtsHBM.MOD_ID)
public class ReinhardtsHBM {
    public static final String MOD_ID = "reinhardtshbm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ReinhardtsHBM(IEventBus modEventBus) {
        modEventBus.addListener(HbmNetwork::register);
        modEventBus.addListener(HbmChunkTickets::register);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, HbmConfig.SPEC);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, HbmClientConfig.SPEC);

        HbmFluids.bootstrap();
        LegacyHbmContent.bootstrap();

        HbmArmorMaterials.register(modEventBus);
        HbmFluids.register(modEventBus);
        HbmItems.register(modEventBus);
        HbmBlocks.register(modEventBus);
        HbmBlockEntities.register(modEventBus);
        HbmMenus.register(modEventBus);
        HbmParticleTypes.register(modEventBus);
        HbmEntityTypes.register(modEventBus);
        HbmMobEffects.register(modEventBus);
        HbmRecipeTypes.register(modEventBus);
        LegacyItemComponents.register(modEventBus);
        HbmConditionSerializers.register(modEventBus);
        HbmSoundEvents.register(modEventBus);
        HbmWorldgenFeatures.register(modEventBus);
        HbmWorldgenStructures.register(modEventBus);
        HbmCreativeTabs.register(modEventBus);
        HbmDataAttachments.register(modEventBus);

        PortStatus.logBootstrapSummary(LOGGER);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
