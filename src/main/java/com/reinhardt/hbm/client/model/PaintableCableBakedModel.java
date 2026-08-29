package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PaintableCableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Two-pass legacy paintable cable: paintable block surface plus optional cable-port overlay. */
public final class PaintableCableBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_paintable");
    private static final ModelProperty<PaintData> PAINT_DATA = new ModelProperty<>(Objects::nonNull);
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private final BakedModel fallback;
    private final TextureAtlasSprite base;
    private final TextureAtlasSprite overlay;

    private PaintableCableBakedModel(BakedModel fallback, TextureAtlasSprite base, TextureAtlasSprite overlay) {
        this.fallback = fallback;
        this.base = base;
        this.overlay = overlay;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textures) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(models.get(itemLocation));
        if (fallback == null) return;
        PaintableCableBakedModel model = new PaintableCableBakedModel(fallback,
                sprite(textures, "red_cable_base"), sprite(textures, "red_cable_overlay"));
        for (ModelResourceLocation location : locations) models.put(location, model);
        if (models.containsKey(itemLocation)) models.put(itemLocation, model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof PaintableCableBlockEntity cable) {
            BlockState paintState = cable.paintBlock() == null ? null : cable.paintBlock().defaultBlockState();
            return modelData.derive().with(PAINT_DATA, new PaintData(paintState, cable.portVisible())).build();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        PaintData paint = data.get(PAINT_DATA);
        BakedModel surface = fallback;
        BlockState surfaceState = state;
        if (paint != null && paint.state != null) {
            surface = Minecraft.getInstance().getBlockRenderer().getBlockModel(paint.state);
            surfaceState = paint.state;
        }
        List<BakedQuad> quads = new ArrayList<>(surface.getQuads(surfaceState, side, random, ModelData.EMPTY, renderType));
        boolean ports = paint == null || paint.portVisible;
        if (ports && side != null) quads.add(overlay(side));
        if (ports && state == null && side == null) quads.add(overlay(Direction.NORTH));
        return List.copyOf(quads);
    }

    private BakedQuad overlay(Direction side) {
        float epsilon = 0.0005F;
        return switch (side) {
            case DOWN -> CableModelGeometry.face(side, 0.0F, -epsilon, 0.0F, 1.0F, -epsilon, 1.0F, overlay);
            case UP -> CableModelGeometry.face(side, 0.0F, 1.0F + epsilon, 0.0F, 1.0F, 1.0F + epsilon, 1.0F, overlay);
            case NORTH -> CableModelGeometry.face(side, 0.0F, 0.0F, -epsilon, 1.0F, 1.0F, -epsilon, overlay);
            case SOUTH -> CableModelGeometry.face(side, 0.0F, 0.0F, 1.0F + epsilon, 1.0F, 1.0F, 1.0F + epsilon, overlay);
            case WEST -> CableModelGeometry.face(side, -epsilon, 0.0F, 0.0F, -epsilon, 1.0F, 1.0F, overlay);
            case EAST -> CableModelGeometry.face(side, 1.0F + epsilon, 0.0F, 0.0F, 1.0F + epsilon, 1.0F, 1.0F, overlay);
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

    private record PaintData(@Nullable BlockState state, boolean portVisible) {
    }
}
