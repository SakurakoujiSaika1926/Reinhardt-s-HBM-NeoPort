package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PaintableCableBlockEntity;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
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
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Two-pass legacy paintable cable: paintable block surface plus optional cable-port overlay. */
public final class PaintableCableBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_paintable");
    private static final ModelProperty<PaintData> PAINT_DATA = new ModelProperty<>(Objects::nonNull);
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final SimpleModelState IDENTITY_MODEL_STATE = new SimpleModelState(Transformation.identity());
    private final BakedModel fallback;
    private final TextureAtlasSprite base;
    private final TextureAtlasSprite overlay;
    private final Map<Direction, List<BakedQuad>> baseFaces;
    private final Map<Direction, List<BakedQuad>> overlayFaces;
    private final List<BakedQuad> baseGeneral;
    private final List<BakedQuad> overlayGeneral;

    private PaintableCableBakedModel(BakedModel fallback, TextureAtlasSprite base, TextureAtlasSprite overlay) {
        this.fallback = fallback;
        this.base = base;
        this.overlay = overlay;
        this.baseFaces = bakeFaces(base, false);
        this.overlayFaces = bakeFaces(overlay, true);
        this.baseGeneral = flatten(this.baseFaces);
        this.overlayGeneral = flatten(this.overlayFaces);
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
        boolean renderCable = renderType == null || renderType == RenderType.cutout();

        // RenderBlockMultipass renders only pass zero for inventory in 1.7.10.
        if (state == null) {
            if (!renderCable) {
                return List.of();
            }
            return side == null ? baseGeneral : baseFaces.get(side);
        }

        PaintData paint = data.get(PAINT_DATA);
        List<BakedQuad> quads = new ArrayList<>();
        if (paint != null && paint.state != null) {
            BakedModel surface = Minecraft.getInstance().getBlockRenderer().getBlockModel(paint.state);
            quads.addAll(surface.getQuads(paint.state, side, random, ModelData.EMPTY, renderType));
        } else if (renderCable) {
            quads.addAll(side == null ? baseGeneral : baseFaces.get(side));
        }
        boolean ports = paint == null || paint.portVisible;
        if (ports && renderCable) {
            quads.addAll(side == null ? overlayGeneral : overlayFaces.get(side));
        }
        return List.copyOf(quads);
    }

    private static Map<Direction, List<BakedQuad>> bakeFaces(TextureAtlasSprite sprite, boolean offset) {
        FaceBakery bakery = new FaceBakery();
        Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            Vector3f from = new Vector3f(0.0F, 0.0F, 0.0F);
            Vector3f to = new Vector3f(16.0F, 16.0F, 16.0F);
            if (offset) {
                offsetFace(face, from, to);
            }
            faces.put(face, List.of(bakery.bakeQuad(
                    from,
                    to,
                    new BlockElementFace(null, BlockElementFace.NO_TINT, "#surface",
                            new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)),
                    sprite,
                    face,
                    IDENTITY_MODEL_STATE,
                    null,
                    false
            )));
        }
        return Map.copyOf(faces);
    }

    private static void offsetFace(Direction face, Vector3f from, Vector3f to) {
        float epsilon = 0.001F;
        switch (face) {
            case DOWN -> from.set(from.x(), -epsilon, from.z());
            case UP -> to.set(to.x(), 16.0F + epsilon, to.z());
            case NORTH -> from.set(from.x(), from.y(), -epsilon);
            case SOUTH -> to.set(to.x(), to.y(), 16.0F + epsilon);
            case WEST -> from.set(-epsilon, from.y(), from.z());
            case EAST -> to.set(16.0F + epsilon, to.y(), to.z());
        }
    }

    private static List<BakedQuad> flatten(Map<Direction, List<BakedQuad>> faces) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction face : Direction.values()) {
            quads.addAll(faces.get(face));
        }
        return List.copyOf(quads);
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
