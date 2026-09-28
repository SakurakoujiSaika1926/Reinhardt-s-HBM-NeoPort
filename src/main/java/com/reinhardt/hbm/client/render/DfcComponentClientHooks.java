package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInventoryBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/** Client-only construction of DFC render geometry snapshots. */
public final class DfcComponentClientHooks {
    private static volatile long modelRevision;
    private static volatile BakedModelGeneration geometryGeneration;
    private static final ClientRenderSnapshotInstances<DfcInventoryBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();

    private DfcComponentClientHooks() {
    }

    /**
     * Extracts every face-used float vertex from the same baked model/render-type
     * traversal as {@link MachineModelRenderer#renderUnculled}, then applies the
     * exact PoseStack used by the DFC renderer.  The returned AABB contains only
     * immutable primitive values and is safe for SectionCompiler worker threads.
     */
    public static GeometryResult staticModelGeometry(DfcInventoryBlockEntity blockEntity,
                                                     BlockState state, Direction facing) {
        CLIENT_INSTANCES.track(blockEntity);
        BakedModelGeneration generation = geometryGeneration();
        BakedModel model = generation.model(DfcComponentBlockEntityRenderer.model(blockEntity));
        PoseStack poseStack = new PoseStack();
        if (blockEntity instanceof DfcEmitterBlockEntity) {
            DfcComponentBlockEntityRenderer.applyEmitterPose(poseStack, facing);
        } else if (blockEntity instanceof DfcReceiverBlockEntity) {
            DfcComponentBlockEntityRenderer.applyReceiverPose(poseStack, facing);
        } else if (blockEntity instanceof DfcInjectorBlockEntity) {
            DfcComponentBlockEntityRenderer.applyInjectorPose(poseStack, facing);
        } else if (blockEntity instanceof DfcStabilizerBlockEntity) {
            DfcComponentBlockEntityRenderer.applyStabilizerPose(poseStack, facing);
        } else {
            throw new IllegalArgumentException("Unsupported DFC component for render geometry: "
                    + blockEntity.getClass().getName());
        }
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
        return new GeometryResult(generation, bounds.build(), modelRevision);
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(DfcComponentBlockEntityRenderer.models());
        modelRevision++;
        CLIENT_INSTANCES.refreshAll(DfcInventoryBlockEntity::refreshClientBeamSnapshotAfterModelReload);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static long modelRevision() {
        return modelRevision;
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (DfcComponentClientHooks.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(DfcComponentBlockEntityRenderer.models());
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    public record GeometryResult(Object geometryToken, AABB bounds, long revision) {
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

}
