package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.ScrapsItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FoundryWireItemModel implements IDynamicBakedModel {
    private static final Map<String, VariantTarget> TARGETS = targets();

    private final BakedModel delegate;
    private final ItemOverrides overrides;

    private FoundryWireItemModel(BakedModel delegate, VariantTarget target, Map<String, BakedModel> variants) {
        this.delegate = delegate;
        this.overrides = new MaterialOverrides(delegate, target, variants);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (VariantTarget target : TARGETS.values()) {
            for (ModelResourceLocation location : target.variants().values()) {
                event.register(location);
            }
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        for (VariantTarget target : TARGETS.values()) {
            replaceModel(models, target);
        }
    }

    public static boolean usesMaterialOverride(ItemStack stack, @Nullable FoundryMaterial material) {
        if (material == null) {
            return false;
        }
        VariantTarget target = targetFor(stack);
        return target != null && target.variants().containsKey(material.name());
    }

    private static void replaceModel(Map<ModelResourceLocation, BakedModel> models, VariantTarget target) {
        ModelResourceLocation targetLocation = modelLocation(target.itemPath());
        BakedModel base = models.get(targetLocation);
        if (base == null || base instanceof FoundryWireItemModel) {
            return;
        }

        Map<String, BakedModel> variants = new LinkedHashMap<>();
        for (Map.Entry<String, ModelResourceLocation> entry : target.variants().entrySet()) {
            BakedModel variant = models.get(entry.getValue());
            if (variant != null) {
                variants.put(entry.getKey(), variant);
            }
        }
        if (variants.isEmpty()) {
            return;
        }

        models.put(targetLocation, new FoundryWireItemModel(base, target, Map.copyOf(variants)));
        ReinhardtsHBM.LOGGER.info("Installed legacy {} material item model overrides for {} materials", target.itemPath(), variants.size());
    }

    @Override
    public java.util.List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                              ModelData extraData, @Nullable RenderType renderType) {
        return this.delegate.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    private static Map<String, VariantTarget> targets() {
        Map<String, VariantTarget> targets = new LinkedHashMap<>();
        targets.put("scraps", new VariantTarget("scraps", MaterialSource.SCRAPS, scrapsOverrides()));
        return Map.copyOf(targets);
    }

    private static Map<String, ModelResourceLocation> scrapsOverrides() {
        Map<String, ModelResourceLocation> overrides = new LinkedHashMap<>();
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (material.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE
                    || material.behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE) {
                overrides.put(material.name(), sideLoadedModelLocation(authoredModel("scraps", material.name()).orElseGet(() -> generatedModel("scraps", material.name()))));
            }
        }
        return Map.copyOf(overrides);
    }

    private static Optional<String> authoredModel(String itemPath, String material) {
        if ("scraps".equals(itemPath) && "bismuth".equals(material)) {
            return Optional.of("scraps_bismuth");
        }
        return Optional.empty();
    }

    private static String generatedModel(String itemPath, String material) {
        return "material/" + itemPath + "_" + material;
    }

    @Nullable
    private static VariantTarget targetFor(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        if ("scraps".equals(id.getPath()) && ScrapsItem.isLiquid(stack)) {
            return null;
        }
        return TARGETS.get(id.getPath());
    }

    @Nullable
    private static FoundryMaterial materialFromStack(ItemStack stack, MaterialSource source) {
        return switch (source) {
            case SCRAPS -> {
                var contents = ScrapsItem.contents(stack);
                yield contents == null ? null : contents.material();
            }
        };
    }

    private static ModelResourceLocation modelLocation(String path) {
        return new ModelResourceLocation(ReinhardtsHBM.id(path), ModelResourceLocation.INVENTORY_VARIANT);
    }

    private static ModelResourceLocation sideLoadedModelLocation(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("item/" + path));
    }

    private enum MaterialSource {
        SCRAPS
    }

    private record VariantTarget(String itemPath, MaterialSource source, Map<String, ModelResourceLocation> variants) {
    }

    private static final class MaterialOverrides extends ItemOverrides {
        private final BakedModel fallback;
        private final VariantTarget target;
        private final Map<String, BakedModel> variants;

        private MaterialOverrides(BakedModel fallback, VariantTarget target, Map<String, BakedModel> variants) {
            super();
            this.fallback = fallback;
            this.target = target;
            this.variants = variants;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!("scraps".equals(this.target.itemPath()) && ScrapsItem.isLiquid(stack))) {
                FoundryMaterial material = materialFromStack(stack, this.target.source());
                if (material != null && this.variants.containsKey(material.name())) {
                    return this.variants.get(material.name());
                }
            }
            BakedModel resolved = this.fallback.getOverrides().resolve(this.fallback, stack, level, entity, seed);
            return resolved == null ? this.fallback : resolved;
        }
    }
}
