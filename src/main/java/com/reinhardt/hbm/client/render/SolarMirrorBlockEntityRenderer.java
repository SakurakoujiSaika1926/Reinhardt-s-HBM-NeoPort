package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.SolarMirrorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
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
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SolarMirrorBlockEntityRenderer implements LongRangeBlockEntityRenderer<SolarMirrorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/solar_mirror_base");
    private static final ModelResourceLocation MIRROR = MachineModelRenderer.standalone("block/solar_mirror_mirror");
    private static volatile GeometryTemplates geometryTemplates;

    public SolarMirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        SolarMirrorBlockEntity.setClientRenderSnapshotUpdater(
                SolarMirrorBlockEntityRenderer::updateRenderSnapshot);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(MIRROR);
    }

    /** Captures the post-bake quads on Minecraft's client thread. */
    static void rebuildGeometryTemplatesAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(SolarMirrorBlockEntityRenderer::rebuildGeometryTemplatesAfterBake);
            return;
        }
        BlockState state = HbmBlocks.SOLAR_MIRROR.get().defaultBlockState();
        geometryTemplates = new GeometryTemplates(
                captureGeometry(MachineModelRenderer.model(BASE), state),
                captureGeometry(MachineModelRenderer.model(MIRROR), state));
        SolarMirrorBlockEntity.setClientRenderSnapshotUpdater(
                SolarMirrorBlockEntityRenderer::updateRenderSnapshot);
        SolarMirrorBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    @Override
    public void render(SolarMirrorBlockEntity mirror, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SolarMirrorBlockEntity.RenderSnapshot snapshot = mirror.renderSnapshot();
        BlockState state = snapshot.state();
        if (!(snapshot.baseGeometryToken() instanceof CapturedGeometry baseGeometry)
                || !(snapshot.mirrorGeometryToken() instanceof CapturedGeometry mirrorGeometry)) {
            return;
        }

        poseStack.pushPose();
        applyBasePose(poseStack);
        renderCaptured(baseGeometry, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        applyMirrorPose(poseStack, snapshot.rotationValid(), snapshot.yawRadians(), snapshot.pitchRadians());
        renderCaptured(mirrorGeometry, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolarMirrorBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    /** Runs on the client level thread; section workers only read the published record. */
    private static void updateRenderSnapshot(SolarMirrorBlockEntity mirror) {
        GeometryTemplates templates = geometryTemplates;
        if (templates == null) {
            return;
        }
        SolarMirrorBlockEntity.RenderInputs inputs = mirror.captureRenderInputs();
        Rotation rotation = targetRotation(mirror.getBlockPos(), inputs.target());
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(mirror.getBlockPos());

        PoseStack basePose = new PoseStack();
        applyBasePose(basePose);
        templates.base().includeTransformed(bounds, basePose);

        PoseStack mirrorPose = new PoseStack();
        applyMirrorPose(mirrorPose, rotation.valid(), rotation.yawRadians(), rotation.pitchRadians());
        templates.mirror().includeTransformed(bounds, mirrorPose);

        mirror.publishClientRenderSnapshot(new SolarMirrorBlockEntity.RenderSnapshot(
                inputs.state(), rotation.valid(), rotation.yawRadians(), rotation.pitchRadians(),
                templates.base(), templates.mirror(), bounds.build(),
                SolarMirrorBlockEntity.clientRenderSnapshotRevision()));
    }

    private static void applyBasePose(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
    }

    private static void applyMirrorPose(PoseStack poseStack, boolean valid, float yawRadians, float pitchRadians) {
        applyBasePose(poseStack);
        poseStack.translate(0.0D, 1.0D, 0.0D);
        if (valid) {
            poseStack.mulPose(new Quaternionf(new AxisAngle4f(yawRadians, 0.0F, 1.0F, 0.0F)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f(pitchRadians, 0.0F, 0.0F, 1.0F)));
        }
        poseStack.translate(0.0D, -1.0D, 0.0D);
    }

    private static void renderCaptured(CapturedGeometry geometry, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (CapturedBatch batch : geometry.batches()) {
            MachineModelRenderer.renderCapturedQuads(batch.quads(), poseStack,
                    bufferSource.getBuffer(batch.entityRenderType()), packedLight, packedOverlay);
        }
    }

    /** Freezes exactly the render types/quads consumed by renderUnculled. */
    private static CapturedGeometry captureGeometry(BakedModel model, BlockState state) {
        RandomSource random = RandomSource.create(42L);
        List<CapturedBatch> batches = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, renderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, renderType, random);
            }
            if (!quads.isEmpty()) {
                batches.add(new CapturedBatch(
                        RenderTypeHelper.getEntityRenderType(renderType, false), List.copyOf(quads)));
            }
        }
        float[] vertices = new float[coordinates.size()];
        for (int index = 0; index < coordinates.size(); index++) {
            vertices[index] = coordinates.get(index);
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

    private static Rotation targetRotation(BlockPos pos, BlockPos target) {
        int dx = target.getX() - pos.getX();
        int dy = target.getY() - pos.getY();
        int dz = target.getZ() - pos.getZ();
        double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
        if (target.getY() < pos.getY() || distance <= 0.0001D) {
            return Rotation.invalid();
        }
        double pitch = Math.toDegrees(-Math.asin((dy + 0.5D) / distance)) + 90.0D;
        double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180.0D;
        float yawRadians = (float) Math.toRadians(yaw);
        float pitchRadians = (float) Math.toRadians(pitch);
        return Float.isFinite(yawRadians) && Float.isFinite(pitchRadians)
                ? new Rotation(true, yawRadians, pitchRadians)
                : Rotation.invalid();
    }

    private record Rotation(boolean valid, float yawRadians, float pitchRadians) {
        private static Rotation invalid() {
            return new Rotation(false, 0.0F, 0.0F);
        }
    }

    private record GeometryTemplates(CapturedGeometry base, CapturedGeometry mirror) {
    }

    private record CapturedBatch(RenderType entityRenderType, List<BakedQuad> quads) {
    }

    private record CapturedGeometry(List<CapturedBatch> batches, float[] vertices) {
        private void includeTransformed(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
            for (int index = 0; index < vertices.length; index += 3) {
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        vertices[index], vertices[index + 1], vertices[index + 2])));
            }
        }
    }
}
