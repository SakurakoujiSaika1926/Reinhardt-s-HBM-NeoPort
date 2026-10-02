package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SolarBoilerBlockEntityRenderer implements LongRangeBlockEntityRenderer<SolarBoilerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    // RenderSolarBoiler in 1.7.10 renders only the OBJ's Base group.  The three
    // concentric auxiliary glow groups belong to the export but are not world geometry.
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_solar_boiler_base");
    /**
     * Legacy RenderSolarBoiler used additive blending with depth testing enabled,
     * but explicitly disabled depth writes. RenderType.lightning() writes depth,
     * which turns overlapping heliostat beams into opaque-looking fan surfaces.
     */
    private static final RenderType HELIOSTAT_BEAM = RenderType.create(
            "reinhardtshbm_heliostat_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .createCompositeState(false)
    );

    public SolarBoilerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
    }

    @Override
    public void render(SolarBoilerBlockEntity boiler, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SolarBoilerBlockEntity.RenderSnapshot snapshot = boiler.renderSnapshot();
        BlockState state = snapshot.blockState();

        poseStack.pushPose();
        applySolarBoilerPose(poseStack, snapshot.facing());
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        // RenderSolarBoiler only emitted the additive beams on fancy graphics.
        if (snapshot.beamsEnabled()) {
            renderMirrorBeams(snapshot.beamVertices(), poseStack, bufferSource);
        }
    }

    @Override
    public AABB getRenderBoundingBox(SolarBoilerBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    private static void applySolarBoilerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Solar boiler facing must be horizontal: " + facing);
        }
    }

    /** Direct vertex translation of RenderSolarBoiler's four additive beam faces. */
    private static void renderMirrorBeams(
            java.util.List<SolarBoilerBlockEntity.BeamVertex> vertices,
            PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(HELIOSTAT_BEAM);
        PoseStack.Pose pose = poseStack.last();
        for (SolarBoilerBlockEntity.BeamVertex vertex : vertices) {
            consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                    .setColor(255, 255, 255, vertex.alpha());
        }
    }
}
