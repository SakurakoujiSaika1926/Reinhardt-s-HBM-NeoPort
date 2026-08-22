package com.reinhardt.hbm.datagen;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class HbmDataGenerators {
    private HbmDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(
                event.includeClient(),
                new HbmFluidBlockStateProvider(generator.getPackOutput(), event.getExistingFileHelper())
        );
    }

    private static final class HbmFluidBlockStateProvider extends BlockStateProvider {
        private HbmFluidBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, ReinhardtsHBM.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            com.reinhardt.hbm.registry.HbmFluids.bootstrap();
            for (com.reinhardt.hbm.registry.HbmFluids.HbmFluidEntry entry
                    : com.reinhardt.hbm.registry.HbmFluids.entries()) {
                entry.blockEntry().ifPresent(blockEntry -> {
                    net.minecraft.resources.ResourceLocation blockId =
                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockEntry.get());
                    if (!blockId.getPath().startsWith("fluid_")) {
                        return;
                    }
                    String modelName = blockId.getPath();
                    ModelFile model = models()
                            .withExistingParent(modelName, mcLoc("block/water"))
                            .texture("particle", entry.definition().forgeFluidStillTexture());
                    simpleBlock(blockEntry.get(), model);
                });
            }
        }
    }
}
