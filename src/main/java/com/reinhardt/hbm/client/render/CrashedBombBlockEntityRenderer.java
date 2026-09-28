package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.blockentity.CrashedBombBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Direct RenderCrashedBomb transform, intentionally independent from machine facing transforms. */
public final class CrashedBombBlockEntityRenderer implements LongRangeBlockEntityRenderer<CrashedBombBlockEntity> {
    private static final ModelResourceLocation[] MODELS = {
            model("crashed_bomb_balefire"), model("crashed_bomb_conventional"),
            model("crashed_bomb_nuke"), model("crashed_bomb_salted")
    };
    /** One immutable post-bake generation shared by both rendering and culling. */
    private static volatile GeometryGeneration geometryGeneration;

    public CrashedBombBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        CrashedBombBlockEntity.setClientRenderSnapshotUpdater(
                CrashedBombBlockEntityRenderer::updateRenderSnapshot);
    }

    /** Called on the client thread after the new model bake has completed. */
    static void rebuildGeometryTemplatesAfterBake() {
        geometryGeneration = captureGeometryGeneration();
        CrashedBombBlockEntity.setClientRenderSnapshotUpdater(
                CrashedBombBlockEntityRenderer::updateRenderSnapshot);
        CrashedBombBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    private static GeometryGeneration captureGeometryGeneration() {
        List<CapturedGeometry> captured = new ArrayList<>(MODELS.length);
        BlockState baseState = HbmBlocks.CRASHED_BOMB.get().defaultBlockState();
        for (CrashedBombBlock.Type type : CrashedBombBlock.Type.values()) {
            BlockState state = baseState.setValue(CrashedBombBlock.VARIANT, type.ordinal());
            captured.add(captureGeometry(MachineModelRenderer.model(MODELS[type.ordinal()]), state));
        }
        return new GeometryGeneration(List.copyOf(captured));
    }

    /** Clears world-owned snapshots while retaining the still-current resource generation. */
    static void clearClientStateAfterLogout() {
        CrashedBombBlockEntity.clearClientRenderSnapshotsAfterLogout();
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : MODELS) {
            event.register(model);
        }
    }

    @Override
    public void render(CrashedBombBlockEntity bomb, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        CrashedBombBlockEntity.RenderSnapshot snapshot = bomb.renderSnapshot();
        GeometryGeneration generation;
        if (snapshot.geometryToken() instanceof GeometryGeneration snapshotGeneration) {
            generation = snapshotGeneration;
        } else {
            // A world snapshot is allowed to be absent briefly while a world is
            // loading. The resource-scoped generation is rebuilt on demand, so
            // that lifecycle gap can never make the dud invisible.
            generation = geometryGeneration();
        }
        CrashedBombBlock.Type type = snapshot.type();
        poseStack.pushPose();
        switch (type) {
            case BALEFIRE -> applyBalefireBombPose(poseStack, bomb.getBlockPos());
            case CONVENTIONAL -> applyConventionalBombPose(poseStack, bomb.getBlockPos());
            case NUKE -> applyNuclearBombPose(poseStack, bomb.getBlockPos());
            case SALTED -> applySaltedBombPose(poseStack, bomb.getBlockPos());
        }
        generation.geometry().get(type.ordinal()).renderDefault(
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrashedBombBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(CrashedBombBlockEntity blockEntity) {
        GeometryGeneration generation = geometryGeneration();
        CrashedBombBlock.Type type = CrashedBombBlock.type(blockEntity.getBlockState());
        PoseStack poseStack = new PoseStack();
        switch (type) {
            case BALEFIRE -> applyBalefireBombPose(poseStack, blockEntity.getBlockPos());
            case CONVENTIONAL -> applyConventionalBombPose(poseStack, blockEntity.getBlockPos());
            case NUKE -> applyNuclearBombPose(poseStack, blockEntity.getBlockPos());
            case SALTED -> applySaltedBombPose(poseStack, blockEntity.getBlockPos());
        }
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(blockEntity.getBlockPos());
        generation.geometry().get(type.ordinal()).includeTransformed(bounds, poseStack);
        blockEntity.publishClientRenderSnapshot(
                new CrashedBombBlockEntity.RenderSnapshot(type, generation, bounds.build(),
                        CrashedBombBlockEntity.clientRenderSnapshotRevision()));
    }

    /**
     * Model bakes are resource-pack scoped, while client logout is world scoped.
     * Re-entering a world does not fire another BakingCompleted event, so a
     * world-lifetime cache clear must be able to reconstruct this immutable
     * generation on the next client snapshot refresh.
     */
    private static GeometryGeneration geometryGeneration() {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (CrashedBombBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = captureGeometryGeneration();
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static void applyBalefireBombPose(PoseStack poseStack, BlockPos pos) {
        RandomSource random = RandomSource.create(pos.asLong());
        float yaw = random.nextFloat() * 360.0F;
        float pitch = random.nextFloat() * 45.0F + 45.0F;
        float roll = random.nextFloat() * 360.0F;
        float offset = random.nextFloat() * 2.0F - 1.0F;
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0.0D, 0.0D, -offset);
    }

    private static void applyConventionalBombPose(PoseStack poseStack, BlockPos pos) {
        RandomSource random = RandomSource.create(pos.asLong());
        float yaw = random.nextFloat() * 360.0F;
        float pitch = random.nextFloat() * 45.0F + 45.0F;
        float roll = random.nextFloat() * 360.0F;
        float offset = random.nextFloat() * 2.0F - 1.0F;
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0.0D, 0.0D, -offset);
    }

    private static void applyNuclearBombPose(PoseStack poseStack, BlockPos pos) {
        RandomSource random = RandomSource.create(pos.asLong());
        float yaw = random.nextFloat() * 360.0F;
        float pitch = random.nextFloat() * 45.0F + 45.0F;
        float roll = random.nextFloat() * 360.0F;
        float offset = random.nextFloat() * 2.0F - 1.0F;
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0.0D, 0.0D, -offset + 1.25F);
    }

    private static void applySaltedBombPose(PoseStack poseStack, BlockPos pos) {
        RandomSource random = RandomSource.create(pos.asLong());
        float yaw = random.nextFloat() * 360.0F;
        float pitch = random.nextFloat() * 45.0F + 45.0F;
        float roll = random.nextFloat() * 360.0F;
        float offset = random.nextFloat() * 2.0F - 1.0F;
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0.0D, 0.0D, -offset + 0.5F);
    }

    /** Freezes the exact render batches and face-used floats consumed by renderUnculled. */
    private static CapturedGeometry captureGeometry(BakedModel model, BlockState state) {
        List<CapturedBatch> batches = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        random.setSeed(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, renderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, renderType, random);
            }
            if (!quads.isEmpty()) {
                batches.add(new CapturedBatch(
                        RenderTypeHelper.getEntityRenderType(renderType, false),
                        List.copyOf(quads)));
            }
        }
        float[] vertices = new float[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            vertices[i] = coordinates.get(i);
        }
        return new CapturedGeometry(List.copyOf(batches), vertices);
    }

    private static void collectQuads(List<BakedQuad> quads, List<Float> coordinates,
                                     BakedModel model, BlockState state, Direction side,
                                     RenderType renderType, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
            quads.add(quad);
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                coordinates.add(Float.intBitsToFloat(data[offset]));
                coordinates.add(Float.intBitsToFloat(data[offset + 1]));
                coordinates.add(Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }

    private record GeometryGeneration(List<CapturedGeometry> geometry) {
    }

    private record CapturedBatch(RenderType entityRenderType, List<BakedQuad> quads) {
    }

    private static final class CapturedGeometry {
        private final List<CapturedBatch> batches;
        private final float[] vertices;

        private CapturedGeometry(List<CapturedBatch> batches, float[] vertices) {
            this.batches = batches;
            this.vertices = vertices.clone();
        }

        private void renderDefault(PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
            for (CapturedBatch batch : batches) {
                MachineModelRenderer.renderCapturedQuads(batch.quads(), poseStack,
                        bufferSource.getBuffer(batch.entityRenderType()), packedLight, packedOverlay);
            }
        }

        private void includeTransformed(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
            for (int i = 0; i < vertices.length; i += 3) {
                bounds.include(poseStack.last().pose().transformPosition(
                        new Vector3f(vertices[i], vertices[i + 1], vertices[i + 2])));
            }
        }
    }

    private static ModelResourceLocation model(String path) {
        return new ModelResourceLocation(ReinhardtsHBM.id("block/" + path), ModelResourceLocation.STANDALONE_VARIANT);
    }
}
