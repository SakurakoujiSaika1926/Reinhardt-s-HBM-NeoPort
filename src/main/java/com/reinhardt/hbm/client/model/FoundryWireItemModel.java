package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.RawIngotItem;
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
        addFoundryTarget(targets, "wire_fine", FoundryShape.WIRE);
        addFoundryTarget(targets, "wire_dense", FoundryShape.DENSE_WIRE);
        addFoundryTarget(targets, "pipe", FoundryShape.PIPE);
        addFoundryTarget(targets, "bolt", FoundryShape.BOLT);
        addFoundryTarget(targets, "shell", FoundryShape.SHELL);
        addFoundryTarget(targets, "plate_cast", FoundryShape.CAST_PLATE);
        addFoundryTarget(targets, "plate_welded", FoundryShape.WELDED_PLATE);
        addFoundryTarget(targets, "part_mechanism", FoundryShape.MECHANISM);
        addFoundryTarget(targets, "part_barrel_light", FoundryShape.LIGHT_BARREL);
        addFoundryTarget(targets, "part_barrel_heavy", FoundryShape.HEAVY_BARREL);
        addFoundryTarget(targets, "part_receiver_light", FoundryShape.LIGHT_RECEIVER);
        addFoundryTarget(targets, "part_receiver_heavy", FoundryShape.HEAVY_RECEIVER);
        addFoundryTarget(targets, "part_stock", FoundryShape.STOCK);
        addFoundryTarget(targets, "part_grip", FoundryShape.GRIP);
        targets.put("ingot_raw", new VariantTarget("ingot_raw", MaterialSource.RAW_INGOT, rawIngotOverrides()));
        targets.put("scraps", new VariantTarget("scraps", MaterialSource.SCRAPS, scrapsOverrides()));
        return Map.copyOf(targets);
    }

    private static void addFoundryTarget(Map<String, VariantTarget> targets, String itemPath, FoundryShape shape) {
        targets.put(itemPath, new VariantTarget(itemPath, MaterialSource.FOUNDRY_SHAPE, foundryOverrides(itemPath, shape)));
    }

    private static Map<String, ModelResourceLocation> foundryOverrides(String itemPath, FoundryShape shape) {
        Map<String, ModelResourceLocation> overrides = new LinkedHashMap<>();
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (FoundryShapeItem.supports(shape, material)) {
                overrides.put(material.name(), sideLoadedModelLocation(authoredModel(itemPath, material.name()).orElseGet(() -> generatedModel(itemPath, material.name()))));
            }
        }
        return Map.copyOf(overrides);
    }

    private static Map<String, ModelResourceLocation> rawIngotOverrides() {
        Map<String, ModelResourceLocation> overrides = new LinkedHashMap<>();
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (RawIngotItem.supports(material)) {
                overrides.put(material.name(), sideLoadedModelLocation(generatedModel("ingot_raw", material.name())));
            }
        }
        return Map.copyOf(overrides);
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
        if ("wire_fine".equals(itemPath)) {
            return switch (material) {
                case "aluminium" -> Optional.of("wire_aluminium");
                case "copper" -> Optional.of("wire_copper");
                case "red_copper" -> Optional.of("wire_red_copper");
                case "gold" -> Optional.of("wire_gold");
                case "tungsten" -> Optional.of("wire_tungsten");
                case "carbon" -> Optional.of("wire_carbon");
                case "schrabidium" -> Optional.of("wire_schrabidium");
                case "magnetized_tungsten" -> Optional.of("wire_magnetized_tungsten");
                default -> Optional.empty();
            };
        }
        if ("scraps".equals(itemPath) && "bismuth".equals(material)) {
            return Optional.of("scraps_bismuth");
        }
        return Optional.empty();
    }

    private static String generatedModel(String itemPath, String material) {
        if ("wire_fine".equals(itemPath)) {
            return "wire_fine_legacy_" + material;
        }
        if ("wire_dense".equals(itemPath)) {
            return "wire_dense_legacy_" + material;
        }
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
            case FOUNDRY_SHAPE -> stack.getItem() instanceof FoundryShapeItem shapeItem ? shapeItem.material(stack) : null;
            case RAW_INGOT -> stack.getItem() instanceof RawIngotItem rawIngot ? rawIngot.material(stack) : null;
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
        FOUNDRY_SHAPE,
        RAW_INGOT,
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
