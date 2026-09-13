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
            bakeWorldBox(quads, box, mask);
        }
        return List.copyOf(quads);
    }

    private List<BakedQuad> bakeInventory() {
        Box box = new Box(0.125F, 0.125F, 0.0F, 0.875F, 0.875F, 1.0F);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            TextureAtlasSprite sprite = face == Direction.NORTH || face == Direction.SOUTH
                    ? this.textures.end : this.textures.straight;
            quads.add(bakeFace(box, face, sprite, inventoryFaceUvLayout(face), false));
        }
        return List.copyOf(quads);
    }

    /**
     * Exact RenderBoxDuct world path for the legacy silver/default state.  The
     * old renderer applied its UV layout for every individual cube it emitted;
     * doing the same here is essential because boxduct textures intentionally
     * contain transparent pixels outside their face artwork.
     */
    private void bakeWorldBox(List<BakedQuad> quads, Box box, int mask) {
        for (Direction face : Direction.values()) {
            quads.add(bakeFace(box, face, iconFor(mask, face), worldFaceUvLayout(mask, face), true));
        }
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

        // FluidDuctBox#getIcon returns the straight icon for the face
        // opposite each curve connection before selecting curve_* artwork.
        if ((side == Direction.UP && down)
                || (side == Direction.DOWN && up)
                || (side == Direction.SOUTH && north)
                || (side == Direction.NORTH && south)
                || (side == Direction.EAST && west)
                || (side == Direction.WEST && east)) {
            return this.textures.straight;
        }

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

    private static int inventoryFaceUvLayout(Direction face) {
        return switch (face) {
            // RenderBoxDuct#renderInventoryBlock sets uvRotateNorth=1 and
            // uvRotateSouth=2 before emitting its six faces.
            // In RenderBlocks, those legacy field names are applied by
            // renderFaceXNeg (WEST) and renderFaceXPos (EAST), respectively.
            case WEST -> 1;
            case EAST -> 2;
            case DOWN, UP, NORTH, SOUTH -> 0;
        };
    }

    private static int worldFaceUvLayout(int mask, Direction face) {
        if (isStraightX(mask)) {
            return switch (face) {
                // RenderBoxDuct straight-X: top/bottom=1,
                // uvRotateEast=2 (NORTH/Z-), uvRotateWest=1 (SOUTH/Z+).
                case DOWN, UP -> 1;
                case NORTH -> 2;
                case SOUTH -> 1;
                case WEST, EAST -> 0;
            };
        }
        if (isStraightZ(mask)) {
            return switch (face) {
                // RenderBoxDuct straight-Z: uvRotateNorth=1 (WEST/X-),
                // uvRotateSouth=2 (EAST/X+).
                case WEST -> 1;
                case EAST -> 2;
                case DOWN, UP, NORTH, SOUTH -> 0;
            };
        }
        if (Integer.bitCount(mask) != 2) {
            return 0;
        }

        boolean east = has(mask, Direction.EAST);
        boolean west = has(mask, Direction.WEST);
        boolean up = has(mask, Direction.UP);
        boolean down = has(mask, Direction.DOWN);
        if ((down || up) && (east || west) && (face == Direction.DOWN || face == Direction.UP)) {
            // RenderBoxDuct curves with a vertical/X connection set top and
            // bottom to layout 1 before drawing all participating cubes.
            return 1;
        }
        if (!down && !up) {
            return switch (face) {
                // RenderBoxDuct horizontal curves: north=1/south=2 fields
                // map to WEST/EAST faces; east=2/west=1 map to NORTH/SOUTH.
                case WEST, SOUTH -> 1;
                case EAST, NORTH -> 2;
                case DOWN, UP -> 0;
            };
        }
        return 0;
    }

    private BakedQuad bakeFace(Box box, Direction face, TextureAtlasSprite sprite, int oldUvLayout,
                               boolean renderBlocksNtFixes) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(BlockElementTint.NONE);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        float[][] vertices = vertices(box, face);
        FaceUv uv = oldFaceUv(box, face, oldUvLayout, renderBlocksNtFixes);
        float[][] uvs = {
                {uv.u0, uv.v0}, {uv.u1, uv.v1}, {uv.u2, uv.v2}, {uv.u3, uv.v3}
        };
        for (int index = 0; index < vertices.length; index++) {
            float[] vertex = vertices[index];
            baker.addVertex(vertex[0], vertex[1], vertex[2]);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(uvs[index][0] / 16.0F), sprite.getV(uvs[index][1] / 16.0F));
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
            case SOUTH -> new float[][]{{box.minX, box.maxY, box.maxZ, 0, 0}, {box.minX, box.minY, box.maxZ, 1, 0},
                    {box.maxX, box.minY, box.maxZ, 1, 1}, {box.maxX, box.maxY, box.maxZ, 0, 1}};
            case WEST -> new float[][]{{box.minX, box.maxY, box.maxZ, 0, 0}, {box.minX, box.maxY, box.minZ, 1, 0},
                    {box.minX, box.minY, box.minZ, 1, 1}, {box.minX, box.minY, box.maxZ, 0, 1}};
            case EAST -> new float[][]{{box.maxX, box.minY, box.maxZ, 0, 0}, {box.maxX, box.minY, box.minZ, 1, 0},
                    {box.maxX, box.maxY, box.minZ, 1, 1}, {box.maxX, box.maxY, box.maxZ, 0, 1}};
        };
    }

    /** Direct modern equivalent of the old RenderBlocks face UV layouts. */
    private static FaceUv oldFaceUv(Box box, Direction face, int layout, boolean renderBlocksNtFixes) {
        float x0 = box.minX * 16.0F;
        float x1 = box.maxX * 16.0F;
        float y0 = box.minY * 16.0F;
        float y1 = box.maxY * 16.0F;
        float z0 = box.minZ * 16.0F;
        float z1 = box.maxZ * 16.0F;
        return switch (face) {
            case DOWN -> switch (layout) {
                case 0 -> uv(x0, z1, x0, z0, x1, z0, x1, z1);
                case 1 -> uv(16.0F - z1, x0, 16.0F - z0, x0, 16.0F - z0, x1, 16.0F - z1, x1);
                case 2 -> uv(z1, 16.0F - x0, z0, 16.0F - x0, z0, 16.0F - x1, z1, 16.0F - x1);
                case 3 -> uv(16.0F - x0, 16.0F - z1, 16.0F - x0, 16.0F - z0, 16.0F - x1, 16.0F - z0, 16.0F - x1, 16.0F - z1);
                default -> throw invalidOldUvLayout(layout);
            };
            case UP -> switch (layout) {
                case 0 -> uv(x1, z1, x1, z0, x0, z0, x0, z1);
                case 1 -> uv(z1, 16.0F - x1, z0, 16.0F - x1, z0, 16.0F - x0, z1, 16.0F - x0);
                case 2 -> uv(16.0F - z1, x1, 16.0F - z0, x1, 16.0F - z0, x0, 16.0F - z1, x0);
                case 3 -> uv(16.0F - x1, 16.0F - z1, 16.0F - x1, 16.0F - z0, 16.0F - x0, 16.0F - z0, 16.0F - x0, 16.0F - z1);
                default -> throw invalidOldUvLayout(layout);
            };
            case NORTH -> renderBlocksNtFixes
                    ? renderBlocksNtNorthUv(x0, x1, y0, y1, layout)
                    : switch (layout) {
                case 0 -> uv(x1, 16.0F - y1, x0, 16.0F - y1, x0, 16.0F - y0, x1, 16.0F - y0);
                case 1 -> uv(16.0F - y0, x0, 16.0F - y0, x1, 16.0F - y1, x1, 16.0F - y1, x0);
                case 2 -> uv(y0, 16.0F - x0, y0, 16.0F - x1, y1, 16.0F - x1, y1, 16.0F - x0);
                case 3 -> uv(16.0F - x1, y1, 16.0F - x0, y1, 16.0F - x0, y0, 16.0F - x1, y0);
                default -> throw invalidOldUvLayout(layout);
            };
            case SOUTH -> switch (layout) {
                case 0 -> uv(x0, 16.0F - y1, x0, 16.0F - y0, x1, 16.0F - y0, x1, 16.0F - y1);
                case 1 -> uv(y0, 16.0F - x0, y1, 16.0F - x0, y1, 16.0F - x1, y0, 16.0F - x1);
                case 2 -> uv(16.0F - y0, x0, 16.0F - y1, x0, 16.0F - y1, x1, 16.0F - y0, x1);
                case 3 -> uv(16.0F - x0, y1, 16.0F - x0, y0, 16.0F - x1, y0, 16.0F - x1, y1);
                default -> throw invalidOldUvLayout(layout);
            };
            case WEST -> switch (layout) {
                case 0 -> uv(z1, 16.0F - y1, z0, 16.0F - y1, z0, 16.0F - y0, z1, 16.0F - y0);
                case 1 -> uv(y0, 16.0F - z1, y0, 16.0F - z0, y1, 16.0F - z0, y1, 16.0F - z1);
                case 2 -> uv(16.0F - y0, z1, 16.0F - y0, z0, 16.0F - y1, z0, 16.0F - y1, z1);
                case 3 -> uv(16.0F - z1, y1, 16.0F - z0, y1, 16.0F - z0, y0, 16.0F - z1, y0);
                default -> throw invalidOldUvLayout(layout);
            };
            case EAST -> renderBlocksNtFixes
                    ? renderBlocksNtEastUv(z0, z1, y0, y1, layout)
                    : switch (layout) {
                case 0 -> uv(z0, 16.0F - y0, z1, 16.0F - y0, z1, 16.0F - y1, z0, 16.0F - y1);
                case 1 -> uv(16.0F - y1, z1, 16.0F - y1, z0, 16.0F - y0, z0, 16.0F - y0, z1);
                case 2 -> uv(y1, 16.0F - z1, y1, 16.0F - z0, y0, 16.0F - z0, y0, 16.0F - z1);
                case 3 -> uv(16.0F - z0, y0, 16.0F - z1, y0, 16.0F - z1, y1, 16.0F - z0, y1);
                default -> throw invalidOldUvLayout(layout);
            };
        };
    }

    /** UVs emitted by the legacy RenderBlocksNT Z-negative face renderer. */
    private static FaceUv renderBlocksNtNorthUv(float x0, float x1, float y0, float y1, int layout) {
        float minU = 16.0F - x0;
        float maxU = 16.0F - x1;
        float maxV = 16.0F - y1;
        float minV = 16.0F - y0;
        float minU2 = minU;
        float maxU2 = maxU;
        float maxV2 = maxV;
        float minV2 = minV;
        switch (layout) {
            case 1 -> {
                maxU = 16.0F - y1;
                minU = 16.0F - y0;
                maxV = x1;
                minV = x0;
                minU2 = minU;
                maxU2 = maxU;
                maxU = minU;
                minU = maxU2;
                maxV2 = minV;
                minV2 = maxV;
            }
            case 2 -> {
                maxU = y0;
                minU = y1;
                maxV = 16.0F - x0;
                minV = 16.0F - x1;
                maxV2 = maxV;
                minV2 = minV;
                minU2 = maxU;
                maxU2 = minU;
                maxV = minV;
                minV = maxV2;
            }
            case 3 -> {
                maxU = 16.0F - x0;
                minU = 16.0F - x1;
                maxV = y1;
                minV = y0;
                minU2 = minU;
                maxU2 = maxU;
                maxV2 = maxV;
                minV2 = minV;
            }
            case 0 -> {
                // Initial values are already the no-rotation path.
            }
            default -> throw invalidOldUvLayout(layout);
        }
        return uv(minU2, maxV2, maxU, maxV, maxU2, minV2, minU, minV);
    }

    /** UVs emitted by the legacy RenderBlocksNT X-positive face renderer. */
    private static FaceUv renderBlocksNtEastUv(float z0, float z1, float y0, float y1, int layout) {
        float minU = 16.0F - z0;
        float maxU = 16.0F - z1;
        float maxV = 16.0F - y1;
        float minV = 16.0F - y0;
        float minU2 = minU;
        float maxU2 = maxU;
        float maxV2 = maxV;
        float minV2 = minV;
        switch (layout) {
            case 1 -> {
                maxU = 16.0F - y1;
                maxV = z1;
                minU = 16.0F - y0;
                minV = z0;
                minU2 = minU;
                maxU2 = maxU;
                maxU = minU;
                minU = maxU2;
                maxV2 = minV;
                minV2 = maxV;
            }
            case 2 -> {
                maxU = y0;
                maxV = 16.0F - z0;
                minU = y1;
                minV = 16.0F - z1;
                maxV2 = maxV;
                minV2 = minV;
                minU2 = maxU;
                maxU2 = minU;
                maxV = minV;
                minV = maxV2;
            }
            case 3 -> {
                maxU = 16.0F - z0;
                minU = 16.0F - z1;
                maxV = y1;
                minV = y0;
                minU2 = minU;
                maxU2 = maxU;
                maxV2 = maxV;
                minV2 = minV;
            }
            case 0 -> {
                // Initial values are already the no-rotation path.
            }
            default -> throw invalidOldUvLayout(layout);
        }
        return uv(maxU2, minV2, minU, minV, minU2, maxV2, maxU, maxV);
    }

    private static FaceUv uv(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        return new FaceUv(u0, v0, u1, v1, u2, v2, u3, v3);
    }

    private static IllegalArgumentException invalidOldUvLayout(int layout) {
        return new IllegalArgumentException("Unsupported RenderBoxDuct UV layout: " + layout);
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

    private record FaceUv(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
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
