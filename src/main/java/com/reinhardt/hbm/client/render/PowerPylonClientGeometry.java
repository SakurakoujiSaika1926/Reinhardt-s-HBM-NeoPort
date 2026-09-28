package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonRenderGeometry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/** Client-only extraction of the exact immutable geometry rendered by each pylon kind. */
public final class PowerPylonClientGeometry {
    private static final ClientRenderSnapshotInstances<PowerPylonBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile GeometryGeneration geometryGeneration;
    private static volatile long modelRevision;

    private PowerPylonClientGeometry() {
    }

    static void installCommonSideBridge() {
        PowerPylonRenderGeometry.installClientProvider(PowerPylonClientGeometry::staticModelGeometry);
    }

    public static PowerPylonRenderGeometry.GeometryResult staticModelGeometry(
                                                     PowerPylonBlockEntity blockEntity,
                                                     BlockState state,
                                                     PowerPylonBlock.Kind kind,
                                                     Direction facing) {
        CLIENT_INSTANCES.track(blockEntity);
        GeometryGeneration generation = geometryGeneration();
        return switch (kind) {
            case RED_CONNECTOR -> redConnectorGeometry(blockEntity, state, facing, generation);
            case CONNECTOR_RED_SUPER -> connectorRedSuperGeometry(blockEntity, state, facing, generation);
            case RED_PYLON -> redPylonGeometry(blockEntity, generation);
            case RED_PYLON_STEEL -> redPylonSteelGeometry(blockEntity, state, generation);
            case RED_PYLON_MEDIUM_WOOD -> redPylonMediumWoodGeometry(blockEntity, state, facing, generation);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerGeometry(blockEntity, state, facing, generation);
            case RED_PYLON_MEDIUM_STEEL -> redPylonMediumSteelGeometry(blockEntity, state, facing, generation);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerGeometry(blockEntity, state, facing, generation);
            case RED_PYLON_LARGE -> redPylonLargeGeometry(blockEntity, state, facing, generation);
            case SUBSTATION -> substationGeometry(blockEntity, state, facing, generation);
        };
    }

    private static PowerPylonRenderGeometry.GeometryResult redConnectorGeometry(PowerPylonBlockEntity blockEntity,
                                                       BlockState state, Direction facing,
                                                       GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_CONNECTOR);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedConnector(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult connectorRedSuperGeometry(PowerPylonBlockEntity blockEntity,
                                                            BlockState state, Direction facing,
                                                            GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.CONNECTOR_RED_SUPER);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientConnectorRedSuper(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonGeometry(PowerPylonBlockEntity blockEntity,
                                                   GeometryGeneration generation) {
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylon(poseStack);
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(blockEntity.getBlockPos());
        generation.redPylonModel().includeRenderedVertices(bounds, poseStack);
        return new PowerPylonRenderGeometry.GeometryResult(
                generation.redPylonModel(), bounds.build(), modelRevision);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonSteelGeometry(PowerPylonBlockEntity blockEntity,
                                                          BlockState state,
                                                          GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_STEEL);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonSteel(poseStack);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonMediumWoodGeometry(PowerPylonBlockEntity blockEntity,
                                                             BlockState state, Direction facing,
                                                             GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_MEDIUM_WOOD);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonMediumWood(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonMediumWoodTransformerGeometry(PowerPylonBlockEntity blockEntity,
                                                                        BlockState state, Direction facing,
                                                                        GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonMediumWoodTransformer(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonMediumSteelGeometry(PowerPylonBlockEntity blockEntity,
                                                              BlockState state, Direction facing,
                                                              GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_MEDIUM_STEEL);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonMediumSteel(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonMediumSteelTransformerGeometry(PowerPylonBlockEntity blockEntity,
                                                                         BlockState state, Direction facing,
                                                                         GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonMediumSteelTransformer(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult redPylonLargeGeometry(PowerPylonBlockEntity blockEntity,
                                                        BlockState state, Direction facing,
                                                        GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.RED_PYLON_LARGE);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientRedPylonLarge(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult substationGeometry(PowerPylonBlockEntity blockEntity,
                                                     BlockState state, Direction facing,
                                                     GeometryGeneration generation) {
        BakedModel model = generation.models().model(PowerPylonBlockEntityRenderer.SUBSTATION);
        PoseStack poseStack = new PoseStack();
        PowerPylonBlockEntityRenderer.orientSubstation(poseStack, facing);
        return bakedGeometry(blockEntity, state, model, poseStack);
    }

    private static PowerPylonRenderGeometry.GeometryResult bakedGeometry(PowerPylonBlockEntity blockEntity, BlockState state,
                                                BakedModel model, PoseStack poseStack) {
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(blockEntity.getBlockPos());
        RandomSource random = RandomSource.create(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            random.setSeed(42L);
            include(bounds, model.getQuads(state, null, random, ModelData.EMPTY, renderType), poseStack);
            for (Direction side : Direction.values()) {
                random.setSeed(42L);
                include(bounds, model.getQuads(state, side, random, ModelData.EMPTY, renderType), poseStack);
            }
        }
        return new PowerPylonRenderGeometry.GeometryResult(model, bounds.build(), modelRevision);
    }

    private static void include(RenderBounds.FloatBuilder bounds, Iterable<BakedQuad> quads,
                                PoseStack poseStack) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2]))));
            }
        }
    }

    static BakedModel redConnectorModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel connectorRedSuperModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static LegacyRedPylonModel redPylonModel(Object geometryToken) {
        return (LegacyRedPylonModel) geometryToken;
    }

    static BakedModel redPylonSteelModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel redPylonMediumWoodModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel redPylonMediumWoodTransformerModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel redPylonMediumSteelModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel redPylonMediumSteelTransformerModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel redPylonLargeModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static BakedModel substationModel(Object geometryToken) {
        return (BakedModel) geometryToken;
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = captureGeneration();
        modelRevision++;
        CLIENT_INSTANCES.refreshAll(PowerPylonBlockEntity::refreshClientModelGeometryAfterBake);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static long modelRevision() {
        return modelRevision;
    }

    private static GeometryGeneration geometryGeneration() {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (PowerPylonClientGeometry.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = captureGeneration();
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static GeometryGeneration captureGeneration() {
        return new GeometryGeneration(
                BakedModelGeneration.capture(PowerPylonBlockEntityRenderer.geometryModels()),
                // BakingCompleted runs before EntityModelSet has necessarily installed its
                // freshly reloaded layer map.  This model is immutable, self-contained box
                // geometry, so bake its registered definition directly instead of observing
                // the entity-layer reload at an indeterminate point in the same reload.
                new LegacyRedPylonModel(LegacyRedPylonModel.createLayer().bakeRoot())
        );
    }

    private record GeometryGeneration(BakedModelGeneration models,
                                      LegacyRedPylonModel redPylonModel) {
    }
}
