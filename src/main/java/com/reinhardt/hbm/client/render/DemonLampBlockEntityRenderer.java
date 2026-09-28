package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.DemonLampBlock;
import com.reinhardt.hbm.blockentity.DemonLampBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** RenderDemonLamp's six-face transform and pair of additive blue radiation cones. */
public final class DemonLampBlockEntityRenderer implements LongRangeBlockEntityRenderer<DemonLampBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/lamp_demon");
    private static final ResourceLocation TEXTURE = com.reinhardt.hbm.ReinhardtsHBM.id(
            "textures/models/machines/demon_lamp.png"
    );
    private static volatile GeometryGeneration geometryGeneration;

    public DemonLampBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        DemonLampBlockEntity.setClientRenderSnapshotUpdater(
                DemonLampBlockEntityRenderer::updateRenderSnapshot);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    /** Atomically replaces the immutable quads/vertices/bounds generation. */
    static void rebuildGeometryTemplateAfterBake() {
        CapturedGeometry[] geometry = new CapturedGeometry[Direction.values().length];
        BakedModel model = MachineModelRenderer.model(MODEL);
        for (Direction facing : Direction.values()) {
            BlockState state = com.reinhardt.hbm.registry.HbmBlocks.LAMP_DEMON.get().defaultBlockState()
                    .setValue(DemonLampBlock.FACING, facing);
            geometry[facing.ordinal()] = captureModelGeometry(model, state);
        }
        GeometryGeneration next = new GeometryGeneration(geometry, buildLocalBounds(geometry));
        geometryGeneration = next;
        DemonLampBlockEntity.setClientRenderSnapshotUpdater(
                DemonLampBlockEntityRenderer::updateRenderSnapshot);
        DemonLampBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    @Override
    public void render(DemonLampBlockEntity lamp, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        DemonLampBlockEntity.RenderSnapshot snapshot = lamp.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        poseStack.pushPose();
        applyWorldPose(poseStack, snapshot.facing());
        renderCapturedModel(generation.geometry()[snapshot.facing().ordinal()], poseStack,
                bufferSource, packedLight, packedOverlay);
        renderCones(poseStack, bufferSource.getBuffer(RenderType.lightning()));
        poseStack.popPose();
    }

    /** Literal ItemRenderLibrary transform for lamp_demon. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(8.0F, 8.0F, 8.0F);
        }
        renderModel(com.reinhardt.hbm.registry.HbmBlocks.LAMP_DEMON.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DemonLampBlockEntity lamp) {
        return lamp.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(DemonLampBlockEntity lamp) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        BlockState state = lamp.getBlockState();
        Direction facing = demonLampFacing(state);
        long revision = DemonLampBlockEntity.clientRenderSnapshotRevision();
        DemonLampBlockEntity.RenderSnapshot previous = lamp.renderSnapshot();
        if (previous.geometryToken() == generation && previous.blockState() == state
                && previous.facing() == facing && previous.revision() == revision) {
            return;
        }
        lamp.publishClientRenderSnapshot(new DemonLampBlockEntity.RenderSnapshot(
                state, facing, generation,
                generation.localBounds()[facing.ordinal()].move(lamp.getBlockPos()), revision));
    }

    private static void applyWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyDemonLampMountPose(poseStack, facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
    }

    private static Direction demonLampFacing(BlockState state) {
        if (!state.hasProperty(DemonLampBlock.FACING)) {
            throw new IllegalStateException("Demon lamp state is missing its owned six-direction facing property: " + state);
        }
        return state.getValue(DemonLampBlock.FACING);
    }

    private static void applyDemonLampMountPose(PoseStack poseStack, Direction facing) {
        // Literal RenderDemonLamp metadata transforms; no shared orientation table is used.
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case UP -> {
            }
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case WEST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case EAST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(270.0F));
            }
        }
    }

    private static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        // RenderDemonLamp binds this texture directly before drawing the OBJ.
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay, TEXTURE, 0.0F, 0.0F);
    }

    private static void renderCapturedModel(CapturedGeometry geometry, PoseStack poseStack,
                                            MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (List<BakedQuad> layer : geometry.layers()) {
            MachineModelRenderer.renderCapturedQuadsStandaloneTexture(
                    layer, poseStack, consumer, packedLight, packedOverlay, 0.0F, 0.0F);
        }
    }

    private static void renderCones(PoseStack poseStack, VertexConsumer consumer) {
        PoseStack.Pose pose = poseStack.last();
        for (int cone = 0; cone < 2; cone++) {
            double nearY = 0.5D + cone * 0.125D;
            double farY = nearY + (cone == 0 ? -0.5D : 0.5D);
            for (int segment = 0; segment < 16; segment++) {
                double start = Math.PI * 2.0D * segment / 16.0D;
                double end = Math.PI * 2.0D * (segment + 1) / 16.0D;
                vertex(consumer, pose, Math.cos(start) * 0.375D, nearY, Math.sin(start) * 0.375D, 64);
                vertex(consumer, pose, Math.cos(start) * 15.0D, farY, Math.sin(start) * 15.0D, 0);
                vertex(consumer, pose, Math.cos(end) * 15.0D, farY, Math.sin(end) * 15.0D, 0);
                vertex(consumer, pose, Math.cos(end) * 0.375D, nearY, Math.sin(end) * 0.375D, 64);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(0, 191, 255, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static AABB[] buildLocalBounds(CapturedGeometry[] geometry) {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : Direction.values()) {
            PoseStack poseStack = new PoseStack();
            applyWorldPose(poseStack, facing);
            RenderBounds.FloatBuilder builder = new RenderBounds.FloatBuilder(BlockPos.ZERO);
            geometry[facing.ordinal()].includeTransformed(builder, poseStack);
            /* Use the exact doubles and float casts from renderCones, then the
             * same PoseStack matrix. This retains the real Quaternionf fringe
             * of all six compound cardinal rotations without an epsilon. */
            for (int cone = 0; cone < 2; cone++) {
                double nearY = 0.5D + cone * 0.125D;
                double farY = nearY + (cone == 0 ? -0.5D : 0.5D);
                for (int segment = 0; segment < 16; segment++) {
                    double start = Math.PI * 2.0D * segment / 16.0D;
                    double end = Math.PI * 2.0D * (segment + 1) / 16.0D;
                    includeConeVertex(builder, poseStack, Math.cos(start) * 0.375D, nearY,
                            Math.sin(start) * 0.375D);
                    includeConeVertex(builder, poseStack, Math.cos(start) * 15.0D, farY,
                            Math.sin(start) * 15.0D);
                    includeConeVertex(builder, poseStack, Math.cos(end) * 15.0D, farY,
                            Math.sin(end) * 15.0D);
                    includeConeVertex(builder, poseStack, Math.cos(end) * 0.375D, nearY,
                            Math.sin(end) * 0.375D);
                }
            }
            bounds[facing.ordinal()] = builder.build();
        }
        return bounds;
    }

    private static void includeConeVertex(RenderBounds.FloatBuilder builder, PoseStack poseStack,
                                          double x, double y, double z) {
        builder.include(poseStack.last().pose().transformPosition(
                new Vector3f((float) x, (float) y, (float) z)));
    }

    /** Captures the exact face-used baked floats emitted by renderModel. */
    private static CapturedGeometry captureModelGeometry(BakedModel model, BlockState state) {
        List<List<BakedQuad>> layers = new ArrayList<>();
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
                layers.add(List.copyOf(quads));
            }
        }
        float[] vertices = new float[coordinates.size()];
        for (int index = 0; index < coordinates.size(); index++) {
            vertices[index] = coordinates.get(index);
        }
        return new CapturedGeometry(List.copyOf(layers), vertices);
    }

    private static void collectQuads(List<BakedQuad> quads, List<Float> coordinates,
                                     BakedModel model, BlockState state,
                                     Direction side, RenderType renderType, RandomSource random) {
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

    private record GeometryGeneration(CapturedGeometry[] geometry, AABB[] localBounds) {
    }

    private record CapturedGeometry(List<List<BakedQuad>> layers, float[] vertices) {
        private void includeTransformed(RenderBounds.FloatBuilder builder, PoseStack poseStack) {
            for (int index = 0; index < vertices.length; index += 3) {
                builder.include(poseStack.last().pose().transformPosition(
                        new Vector3f(vertices[index], vertices[index + 1], vertices[index + 2])));
            }
        }
    }
}
