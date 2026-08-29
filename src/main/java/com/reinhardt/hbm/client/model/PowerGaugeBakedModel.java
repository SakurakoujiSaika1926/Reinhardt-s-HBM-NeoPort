package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PowerGaugeBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Full-cube power gauge with the legacy gauge plate on its placed front. */
public final class PowerGaugeBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_gauge");
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private final BakedModel fallback;
    private final TextureAtlasSprite base;
    private final TextureAtlasSprite gauge;

    private PowerGaugeBakedModel(BakedModel fallback, TextureAtlasSprite base, TextureAtlasSprite gauge) {
        this.fallback = fallback;
        this.base = base;
        this.gauge = gauge;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textures) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(models.get(itemLocation));
        if (fallback == null) return;
        PowerGaugeBakedModel model = new PowerGaugeBakedModel(fallback,
                sprite(textures, "deco_red_copper"), sprite(textures, "cable_gauge"));
        for (ModelResourceLocation location : locations) models.put(location, model);
        if (models.containsKey(itemLocation)) models.put(itemLocation, model);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>(fallback.getQuads(state, side, random, ModelData.EMPTY, renderType));
        if (state == null && side == null) {
            quads.add(CableModelGeometry.face(Direction.NORTH, 0.0F, 0.0F, -0.0005F, 1.0F, 1.0F, -0.0005F, gauge));
        } else if (state != null && side == state.getValue(PowerGaugeBlock.FACING)) {
            quads.add(overlay(side));
        }
        return List.copyOf(quads);
    }

    private BakedQuad overlay(Direction side) {
        float epsilon = 0.0005F;
        return switch (side) {
            case DOWN -> CableModelGeometry.face(side, 0.0F, -epsilon, 0.0F, 1.0F, -epsilon, 1.0F, gauge);
            case UP -> CableModelGeometry.face(side, 0.0F, 1.0F + epsilon, 0.0F, 1.0F, 1.0F + epsilon, 1.0F, gauge);
            case NORTH -> CableModelGeometry.face(side, 0.0F, 0.0F, -epsilon, 1.0F, 1.0F, -epsilon, gauge);
            case SOUTH -> CableModelGeometry.face(side, 0.0F, 0.0F, 1.0F + epsilon, 1.0F, 1.0F, 1.0F + epsilon, gauge);
            case WEST -> CableModelGeometry.face(side, -epsilon, 0.0F, 0.0F, -epsilon, 1.0F, 1.0F, gauge);
            case EAST -> CableModelGeometry.face(side, 1.0F + epsilon, 0.0F, 0.0F, 1.0F + epsilon, 1.0F, 1.0F, gauge);
        };
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return RENDER_TYPES; }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return base; }
    @Override public TextureAtlasSprite getParticleIcon() { return base; }
    @Override public boolean useAmbientOcclusion() { return fallback.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String path) {
        return textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + path)));
    }
}
