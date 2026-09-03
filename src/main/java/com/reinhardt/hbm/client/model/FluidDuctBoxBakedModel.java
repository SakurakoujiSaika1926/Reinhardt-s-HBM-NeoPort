package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FluidDuctBlock;
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
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** The 1.7.10 box duct renderer expressed as a baked model. */
public final class FluidDuctBoxBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("fluid_duct_box");
    private static final ChunkRenderTypeSet CUTOUT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel fallback;
    private final Textures textures;
    private final boolean forBlock;
    private final List<BakedQuad>[] blockQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private FluidDuctBoxBakedModel(BakedModel fallback, Textures textures, boolean forBlock) {
        this.fallback = fallback;
        this.textures = textures;
        this.forBlock = forBlock;
        if (forBlock) {
            this.blockQuads = new List[64];
            for (int mask = 0; mask < this.blockQuads.length; mask++) {
                this.blockQuads[mask] = bakeWorld(mask);
            }
            this.itemQuads = List.of();
        } else {
            this.blockQuads = new List[0];
            this.itemQuads = bakeInventory();
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> blockLocations = models.keySet().stream()
                .filter(location -> BLOCK_ID.equals(location.id()))
                .filter(location -> !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(BLOCK_ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel blockFallback = blockLocations.stream()
                .map(models::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(models.get(itemLocation));
        if (blockFallback == null) {
            ReinhardtsHBM.LOGGER.warn("Unable to install box fluid duct model: no baked model was found");
            return;
        }

        Textures textures = Textures.load(textureGetter);
        FluidDuctBoxBakedModel blockModel = new FluidDuctBoxBakedModel(blockFallback, textures, true);
        for (ModelResourceLocation location : blockLocations) {
            models.put(location, blockModel);
        }
        BakedModel itemFallback = models.getOrDefault(itemLocation, blockFallback);
        models.put(itemLocation, new FluidDuctBoxBakedModel(itemFallback, textures, false));
        ReinhardtsHBM.LOGGER.info("Installed legacy box fluid duct model for {} block models and 1 item model",
                blockLocations.size());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                   ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }
        if (!this.forBlock) {
            return this.itemQuads;
        }
        return this.blockQuads[connectionMask(state)];
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return CUTOUT_RENDER_TYPES;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.textures.straight;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.textures.straight;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return this.fallback.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.fallback.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.fallback.getOverrides();
    }

    private List<BakedQuad> bakeWorld(int mask) {
        int count = Integer.bitCount(mask);
        float lower = 0.125F;
        float upper = 0.875F;
        float junctionLower = 0.0625F;
        float junctionUpper = 0.9375F;
        List<Box> boxes = new ArrayList<>();

        if (isStraightX(mask)) {
            boxes.add(new Box(0.0F, lower, lower, 1.0F, upper, upper));
        } else if (isStraightY(mask)) {
            boxes.add(new Box(lower, 0.0F, lower, upper, 1.0F, upper));
        } else if (isStraightZ(mask)) {
            boxes.add(new Box(lower, lower, 0.0F, upper, upper, 1.0F));
        } else if (count == 2) {
            boxes.add(new Box(lower, lower, lower, upper, upper, upper));
            addArms(boxes, mask, lower, upper, lower, upper);
        } else {
            boxes.add(new Box(junctionLower, junctionLower, junctionLower, junctionUpper, junctionUpper, junctionUpper));
            addArms(boxes, mask, lower, upper, junctionLower, junctionUpper);
        }

        List<BakedQuad> quads = new ArrayList<>(boxes.size() * 6);
        for (Box box : boxes) {
            for (Direction face : Direction.values()) {
                quads.add(bakeFace(box, face, iconFor(mask, face), false));
            }
        }
        return List.copyOf(quads);
    }

    private List<BakedQuad> bakeInventory() {
        Box box = new Box(0.125F, 0.125F, 0.0F, 0.875F, 0.875F, 1.0F);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            TextureAtlasSprite sprite = face == Direction.NORTH || face == Direction.SOUTH
                    ? this.textures.end : this.textures.straight;
            quads.add(bakeFace(box, face, sprite, true));
        }
        return List.copyOf(quads);
    }

    private void addArms(List<Box> boxes, int mask, float lower, float upper,
                         float junctionLower, float junctionUpper) {
        if (has(mask, Direction.DOWN)) {
            boxes.add(new Box(lower, 0.0F, lower, upper, junctionLower, upper));
        }
        if (has(mask, Direction.UP)) {
            boxes.add(new Box(lower, junctionUpper, lower, upper, 1.0F, upper));
        }
        if (has(mask, Direction.WEST)) {
            boxes.add(new Box(0.0F, lower, lower, junctionLower, upper, upper));
        }
        if (has(mask, Direction.EAST)) {
            boxes.add(new Box(junctionUpper, lower, lower, 1.0F, upper, upper));
        }
        if (has(mask, Direction.NORTH)) {
            boxes.add(new Box(lower, lower, 0.0F, upper, upper, junctionLower));
        }
        if (has(mask, Direction.SOUTH)) {
            boxes.add(new Box(lower, lower, junctionUpper, upper, upper, 1.0F));
        }
    }

    private TextureAtlasSprite iconFor(int mask, Direction side) {
        if (isStraightX(mask)) {
            return side.getAxis() == Direction.Axis.X ? this.textures.end : this.textures.straight;
        }
        if (isStraightY(mask)) {
            return side.getAxis() == Direction.Axis.Y ? this.textures.end : this.textures.straight;
        }
        if (isStraightZ(mask)) {
            return side.getAxis() == Direction.Axis.Z ? this.textures.end : this.textures.straight;
        }

        if (Integer.bitCount(mask) != 2) {
            return this.textures.junction;
        }

        Direction first = firstConnection(mask);
        Direction second = secondConnection(mask, first);
        if (side == first || side == second) {
            return this.textures.end;
        }
        boolean east = has(mask, Direction.EAST);
        boolean west = has(mask, Direction.WEST);
        boolean up = has(mask, Direction.UP);
        boolean down = has(mask, Direction.DOWN);
        boolean north = has(mask, Direction.NORTH);
        boolean south = has(mask, Direction.SOUTH);

        if (down) {
            if (south) return side == Direction.WEST ? this.textures.curveBr : this.textures.curveBl;
            if (north) return side == Direction.EAST ? this.textures.curveBr : this.textures.curveBl;
            if (east) return side == Direction.SOUTH ? this.textures.curveBr : this.textures.curveBl;
            if (west) return side == Direction.NORTH ? this.textures.curveBr : this.textures.curveBl;
        }
        if (up) {
            if (south) return side == Direction.WEST ? this.textures.curveTr : this.textures.curveTl;
            if (north) return side == Direction.EAST ? this.textures.curveTr : this.textures.curveTl;
            if (east) return side == Direction.SOUTH ? this.textures.curveTr : this.textures.curveTl;
            if (west) return side == Direction.NORTH ? this.textures.curveTr : this.textures.curveTl;
        }
        if (east && north) return this.textures.curveTr;
        if (east && south) return this.textures.curveBr;
        if (west && north) return this.textures.curveTl;
        if (west && south) return this.textures.curveBl;
        return this.textures.straight;
    }

    private BakedQuad bakeFace(Box box, Direction face, TextureAtlasSprite sprite, boolean inventory) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(BlockElementTint.NONE);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        float[][] vertices = vertices(box, face);
        for (float[] vertex : vertices) {
            baker.addVertex(vertex[0], vertex[1], vertex[2]);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(vertex[3]), sprite.getV(vertex[4]));
            baker.setLight(0);
            baker.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        return baker.bakeQuad();
    }

    private static float[][] vertices(Box box, Direction face) {
        return switch (face) {
            case DOWN -> new float[][]{{box.minX, box.minY, box.maxZ, 0, 0}, {box.minX, box.minY, box.minZ, 0, 1},
                    {box.maxX, box.minY, box.minZ, 1, 1}, {box.maxX, box.minY, box.maxZ, 1, 0}};
            case UP -> new float[][]{{box.maxX, box.maxY, box.maxZ, 0, 0}, {box.maxX, box.maxY, box.minZ, 0, 1},
                    {box.minX, box.maxY, box.minZ, 1, 1}, {box.minX, box.maxY, box.maxZ, 1, 0}};
            case NORTH -> new float[][]{{box.minX, box.maxY, box.minZ, 0, 0}, {box.maxX, box.maxY, box.minZ, 1, 0},
                    {box.maxX, box.minY, box.minZ, 1, 1}, {box.minX, box.minY, box.minZ, 0, 1}};
            case SOUTH -> new float[][]{{box.maxX, box.maxY, box.maxZ, 0, 0}, {box.minX, box.maxY, box.maxZ, 1, 0},
                    {box.minX, box.minY, box.maxZ, 1, 1}, {box.maxX, box.minY, box.maxZ, 0, 1}};
            case WEST -> new float[][]{{box.minX, box.maxY, box.maxZ, 0, 0}, {box.minX, box.maxY, box.minZ, 1, 0},
                    {box.minX, box.minY, box.minZ, 1, 1}, {box.minX, box.minY, box.maxZ, 0, 1}};
            case EAST -> new float[][]{{box.maxX, box.maxY, box.minZ, 0, 0}, {box.maxX, box.maxY, box.maxZ, 1, 0},
                    {box.maxX, box.minY, box.maxZ, 1, 1}, {box.maxX, box.minY, box.minZ, 0, 1}};
        };
    }

    private static int connectionMask(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof FluidDuctBlock)) return 0;
        return (state.getValue(FluidDuctBlock.EAST) ? 32 : 0)
                | (state.getValue(FluidDuctBlock.WEST) ? 16 : 0)
                | (state.getValue(FluidDuctBlock.UP) ? 8 : 0)
                | (state.getValue(FluidDuctBlock.DOWN) ? 4 : 0)
                | (state.getValue(FluidDuctBlock.SOUTH) ? 2 : 0)
                | (state.getValue(FluidDuctBlock.NORTH) ? 1 : 0);
    }

    private static boolean has(int mask, Direction direction) {
        return switch (direction) {
            case EAST -> (mask & 32) != 0;
            case WEST -> (mask & 16) != 0;
            case UP -> (mask & 8) != 0;
            case DOWN -> (mask & 4) != 0;
            case SOUTH -> (mask & 2) != 0;
            case NORTH -> (mask & 1) != 0;
        };
    }

    private static boolean isStraightX(int mask) {
        return (mask & 15) == 0 && mask != 0;
    }

    private static boolean isStraightY(int mask) {
        return (mask & 51) == 0 && mask != 0;
    }

    private static boolean isStraightZ(int mask) {
        return (mask & 60) == 0 && mask != 0;
    }

    private static Direction firstConnection(int mask) {
        for (Direction direction : Direction.values()) if (has(mask, direction)) return direction;
        return Direction.NORTH;
    }

    private static Direction secondConnection(int mask, Direction first) {
        for (Direction direction : Direction.values()) if (direction != first && has(mask, direction)) return direction;
        return first;
    }

    private record Box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }

    private static final class Textures {
        private final TextureAtlasSprite straight;
        private final TextureAtlasSprite end;
        private final TextureAtlasSprite curveTl;
        private final TextureAtlasSprite curveTr;
        private final TextureAtlasSprite curveBl;
        private final TextureAtlasSprite curveBr;
        private final TextureAtlasSprite junction;

        private Textures(TextureAtlasSprite straight, TextureAtlasSprite end, TextureAtlasSprite curveTl,
                         TextureAtlasSprite curveTr, TextureAtlasSprite curveBl, TextureAtlasSprite curveBr,
                         TextureAtlasSprite junction) {
            this.straight = straight;
            this.end = end;
            this.curveTl = curveTl;
            this.curveTr = curveTr;
            this.curveBl = curveBl;
            this.curveBr = curveBr;
            this.junction = junction;
        }

        private static Textures load(Function<Material, TextureAtlasSprite> getter) {
            return new Textures(
                    sprite(getter, "boxduct_silver_straight"),
                    sprite(getter, "boxduct_silver_end"),
                    sprite(getter, "boxduct_silver_curve_tl"),
                    sprite(getter, "boxduct_silver_curve_tr"),
                    sprite(getter, "boxduct_silver_curve_bl"),
                    sprite(getter, "boxduct_silver_curve_br"),
                    sprite(getter, "boxduct_silver_junction_0")
            );
        }

        private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> getter, String name) {
            return getter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + name)));
        }
    }

    private static final class BlockElementTint {
        private static final int NONE = -1;
    }
}
