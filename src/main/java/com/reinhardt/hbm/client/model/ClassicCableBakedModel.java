package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.EnergyCableBlock;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Direct modern equivalent of the 1.7.10 RenderCableClassic renderer. */
public final class ClassicCableBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_classic");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("block/red_cable_classic");
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel fallback;
    private final TextureAtlasSprite sprite;
    private final boolean item;
    private final List<BakedQuad>[] worldQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private ClassicCableBakedModel(BakedModel fallback, TextureAtlasSprite sprite, boolean item) {
        this.fallback = fallback;
        this.sprite = sprite;
        this.item = item;
        this.worldQuads = item ? new List[0] : bakeWorld(sprite);
        this.itemQuads = item ? bakeInventory(sprite) : List.of();
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textures) {
        List<ModelResourceLocation> blockLocations = models.keySet().stream()
                .filter(location -> ID.equals(location.id())
                        && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel fallback = blockLocations.stream().map(models::get).filter(Objects::nonNull).findFirst()
                .orElse(models.get(itemLocation));
        if (fallback == null) {
            return;
        }

        TextureAtlasSprite sprite = textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, TEXTURE));
        ClassicCableBakedModel world = new ClassicCableBakedModel(fallback, sprite, false);
        for (ModelResourceLocation location : blockLocations) {
            models.put(location, world);
        }
        BakedModel itemFallback = models.get(itemLocation);
        if (itemFallback != null) {
            models.put(itemLocation, new ClassicCableBakedModel(itemFallback, sprite, true));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource random, ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        return item ? itemQuads : worldQuads[mask(state)];
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return RENDER_TYPES; }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return sprite; }
    @Override public TextureAtlasSprite getParticleIcon() { return sprite; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[] bakeWorld(TextureAtlasSprite sprite) {
        List<BakedQuad>[] result = new List[64];
        for (int mask = 0; mask < result.length; mask++) {
            result[mask] = bakeState(sprite, mask, false);
        }
        return result;
    }

    private static List<BakedQuad> bakeInventory(TextureAtlasSprite sprite) {
        // The old inventory renderer has four horizontal arms, with a cap on
        // each outer end. It does not render vertical arms in the inventory.
        return bakeState(sprite, 1 | 2 | 16 | 32, true);
    }

    private static List<BakedQuad> bakeState(TextureAtlasSprite sprite, int mask, boolean inventory) {
        boolean east = connected(mask, Direction.EAST);
        boolean west = connected(mask, Direction.WEST);
        boolean up = connected(mask, Direction.UP);
        boolean down = connected(mask, Direction.DOWN);
        boolean south = connected(mask, Direction.SOUTH);
        boolean north = connected(mask, Direction.NORTH);

        float min = 5.5F / 16.0F;
        float max = 10.5F / 16.0F;
        List<BakedQuad> quads = new ArrayList<>();
        if (up) addArm(quads, sprite, Direction.UP, inventory);
        else addCap(quads, sprite, Direction.UP, min, max, inventory);
        if (down) addArm(quads, sprite, Direction.DOWN, inventory);
        else addCap(quads, sprite, Direction.DOWN, min, max, inventory);
        if (east) {
            addArm(quads, sprite, Direction.EAST, inventory);
            if (inventory) addOuterCap(quads, sprite, Direction.EAST, min, max);
        } else addCap(quads, sprite, Direction.EAST, min, max, inventory);
        if (west) {
            addArm(quads, sprite, Direction.WEST, inventory);
            if (inventory) addOuterCap(quads, sprite, Direction.WEST, min, max);
        } else addCap(quads, sprite, Direction.WEST, min, max, inventory);
        if (south) {
            addArm(quads, sprite, Direction.SOUTH, inventory);
            if (inventory) addOuterCap(quads, sprite, Direction.SOUTH, min, max);
        } else addCap(quads, sprite, Direction.SOUTH, min, max, inventory);
        if (north) {
            addArm(quads, sprite, Direction.NORTH, inventory);
            if (inventory) addOuterCap(quads, sprite, Direction.NORTH, min, max);
        } else addCap(quads, sprite, Direction.NORTH, min, max, inventory);
        return List.copyOf(quads);
    }

    private static void addCap(List<BakedQuad> quads, TextureAtlasSprite sprite, Direction direction,
                               float min, float max, boolean inventory) {
        switch (direction) {
            case UP -> quad(quads, sprite, direction, inventory,
                    max, max, min, min, max, min, min, max, max, max, max, max,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case DOWN -> quad(quads, sprite, direction, inventory,
                    min, min, min, max, min, min, max, min, max, min, min, max,
                    0, 0, 5, 0, 5, 5, 0, 5);
            case EAST -> quad(quads, sprite, direction, inventory,
                    max, max, min, max, max, max, max, min, max, max, min, min,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case WEST -> quad(quads, sprite, direction, inventory,
                    min, max, max, min, max, min, min, min, min, min, min, max,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case SOUTH -> quad(quads, sprite, direction, inventory,
                    max, max, max, min, max, max, min, min, max, max, min, max,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case NORTH -> quad(quads, sprite, direction, inventory,
                    max, max, min, min, max, min, min, min, min, max, min, min,
                    5, 0, 0, 0, 0, 5, 5, 5);
        }
    }

    private static void addOuterCap(List<BakedQuad> quads, TextureAtlasSprite sprite, Direction direction,
                                    float min, float max) {
        switch (direction) {
            case EAST -> quad(quads, sprite, direction, true,
                    1, max, min, 1, max, max, 1, min, max, 1, min, min,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case WEST -> quad(quads, sprite, direction, true,
                    0, max, max, 0, max, min, 0, min, min, 0, min, max,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case SOUTH -> quad(quads, sprite, direction, true,
                    max, max, 1, min, max, 1, min, min, 1, max, min, 1,
                    5, 0, 0, 0, 0, 5, 5, 5);
            case NORTH -> quad(quads, sprite, direction, true,
                    min, max, 0, max, max, 0, max, min, 0, min, min, 0,
                    5, 0, 0, 0, 0, 5, 5, 5);
            default -> throw new IllegalArgumentException("Classic cable outer cap must be horizontal");
        }
    }

    private static void addArm(List<BakedQuad> quads, TextureAtlasSprite sprite, Direction direction,
                               boolean inventory) {
        float min = 5.5F / 16.0F;
        float max = 10.5F / 16.0F;
        switch (direction) {
            case UP -> {
                quad(quads, sprite, Direction.NORTH, inventory,
                        max, max, min, min, max, min, min, 1, min, max, 1, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.EAST, inventory,
                        max, max, max, max, max, min, max, 1, min, max, 1, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.SOUTH, inventory,
                        min, max, max, max, max, max, max, 1, max, min, 1, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.WEST, inventory,
                        min, max, min, min, max, max, min, 1, max, min, 1, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
            case DOWN -> {
                quad(quads, sprite, Direction.NORTH, inventory,
                        min, min, min, max, min, min, max, 0, min, min, 0, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.EAST, inventory,
                        max, min, min, max, min, max, max, 0, max, max, 0, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.SOUTH, inventory,
                        max, min, max, min, min, max, min, 0, max, max, 0, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.WEST, inventory,
                        min, min, max, min, min, min, min, 0, min, min, 0, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
            case EAST -> {
                quad(quads, sprite, Direction.UP, inventory,
                        max, max, min, max, max, max, 1, max, max, 1, max, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.NORTH, inventory,
                        max, min, min, max, max, min, 1, max, min, 1, min, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.DOWN, inventory,
                        max, min, max, max, min, min, 1, min, min, 1, min, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.SOUTH, inventory,
                        max, max, max, max, min, max, 1, min, max, 1, max, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
            case WEST -> {
                quad(quads, sprite, Direction.UP, inventory,
                        min, max, max, min, max, min, 0, max, min, 0, max, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.NORTH, inventory,
                        min, max, min, min, min, min, 0, min, min, 0, max, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.DOWN, inventory,
                        min, min, min, min, min, max, 0, min, max, 0, min, min,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.SOUTH, inventory,
                        min, min, max, min, max, max, 0, max, max, 0, min, max,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
            case SOUTH -> {
                quad(quads, sprite, Direction.UP, inventory,
                        max, max, max, min, max, max, min, max, 1, max, max, 1,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.WEST, inventory,
                        min, max, max, min, min, max, min, min, 1, min, max, 1,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.DOWN, inventory,
                        max, min, max, min, min, max, min, min, 1, max, min, 1,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.EAST, inventory,
                        max, min, max, max, max, max, max, max, 1, max, min, 1,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
            case NORTH -> {
                quad(quads, sprite, Direction.UP, inventory,
                        min, max, min, max, max, min, max, max, 0, min, max, 0,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.WEST, inventory,
                        min, min, min, min, max, min, min, max, 0, min, min, 0,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.DOWN, inventory,
                        max, min, min, min, min, min, min, min, 0, max, min, 0,
                        5, 0, 5, 5, 10, 5, 10, 0);
                quad(quads, sprite, Direction.EAST, inventory,
                        max, max, min, max, min, min, max, min, 0, max, max, 0,
                        5, 0, 5, 5, 10, 5, 10, 0);
            }
        }
    }

    private static void quad(List<BakedQuad> quads, TextureAtlasSprite sprite, Direction face,
                             boolean inventory,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1,
                             float u2, float v2, float u3, float v3) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        float[][] vertices = {{x0, y0, z0, u0, v0}, {x1, y1, z1, u1, v1},
                {x2, y2, z2, u2, v2}, {x3, y3, z3, u3, v3}};
        for (float[] vertex : vertices) {
            float x = vertex[0];
            float y = vertex[1];
            float z = vertex[2];
            if (inventory) {
                x = 0.5F + (x - 0.5F) * 1.25F;
                y = 0.5F + (y - 0.5F) * 1.25F;
                z = 0.5F + (z - 0.5F) * 1.25F;
            }
            baker.addVertex(x, y, z);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(vertex[3] / 16.0F), sprite.getV(vertex[4] / 16.0F));
            baker.setLight(0);
            baker.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        quads.add(baker.bakeQuad());
    }

    private static int mask(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof EnergyCableBlock)) return 0;
        return (state.getValue(EnergyCableBlock.EAST) ? 1 : 0)
                | (state.getValue(EnergyCableBlock.WEST) ? 2 : 0)
                | (state.getValue(EnergyCableBlock.UP) ? 4 : 0)
                | (state.getValue(EnergyCableBlock.DOWN) ? 8 : 0)
                | (state.getValue(EnergyCableBlock.SOUTH) ? 16 : 0)
                | (state.getValue(EnergyCableBlock.NORTH) ? 32 : 0);
    }

    private static boolean connected(int mask, Direction direction) {
        return switch (direction) {
            case EAST -> (mask & 1) != 0;
            case WEST -> (mask & 2) != 0;
            case UP -> (mask & 4) != 0;
            case DOWN -> (mask & 8) != 0;
            case SOUTH -> (mask & 16) != 0;
            case NORTH -> (mask & 32) != 0;
        };
    }
}
