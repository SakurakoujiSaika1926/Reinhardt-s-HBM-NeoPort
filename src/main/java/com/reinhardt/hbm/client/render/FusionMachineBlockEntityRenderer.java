package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FusionMachineBlockEntityRenderer implements LongRangeBlockEntityRenderer<FusionMachineBlockEntity> {
    private static final ModelResourceLocation TORUS_TORUS = MachineModelRenderer.standalone("block/fusion_torus_torus");
    private static final ModelResourceLocation TORUS_MAGNET = MachineModelRenderer.standalone("block/fusion_torus_magnet");
    private static final ModelResourceLocation TORUS_BOLTS1 = MachineModelRenderer.standalone("block/fusion_torus_bolts1");
    private static final ModelResourceLocation TORUS_BOLTS2 = MachineModelRenderer.standalone("block/fusion_torus_bolts2");
    private static final ModelResourceLocation TORUS_BOLTS3 = MachineModelRenderer.standalone("block/fusion_torus_bolts3");
    private static final ModelResourceLocation TORUS_BOLTS4 = MachineModelRenderer.standalone("block/fusion_torus_bolts4");
    private static final ModelResourceLocation TORUS_PLASMA = MachineModelRenderer.standalone("block/fusion_torus_plasma");
    private static final ModelResourceLocation TORUS_PLASMA_GLOW = MachineModelRenderer.standalone("block/fusion_torus_plasma_glow");
    private static final ModelResourceLocation TORUS_PLASMA_SPARKLE = MachineModelRenderer.standalone("block/fusion_torus_plasma_sparkle");

    private static final ModelResourceLocation KLYSTRON_BODY = MachineModelRenderer.standalone("block/fusion_klystron_body");
    private static final ModelResourceLocation KLYSTRON_ROTOR = MachineModelRenderer.standalone("block/fusion_klystron_rotor");
    private static final ModelResourceLocation KLYSTRON_CREATIVE_BODY = MachineModelRenderer.standalone("block/fusion_klystron_creative_body");
    private static final ModelResourceLocation KLYSTRON_CREATIVE_ROTOR = MachineModelRenderer.standalone("block/fusion_klystron_creative_rotor");
    private static final ModelResourceLocation BOILER = MachineModelRenderer.standalone("block/fusion_boiler");
    private static final ModelResourceLocation BREEDER = MachineModelRenderer.standalone("block/fusion_breeder");
    private static final ModelResourceLocation COLLECTOR = MachineModelRenderer.standalone("block/fusion_collector");
    private static final ModelResourceLocation COUPLER = MachineModelRenderer.standalone("block/fusion_coupler");
    private static final ModelResourceLocation MHDT_TURBINE = MachineModelRenderer.standalone("block/fusion_mhdt_turbine");
    private static final ModelResourceLocation MHDT_COILS = MachineModelRenderer.standalone("block/fusion_mhdt_coils");
    private static final ModelResourceLocation PLASMA_FORGE_BODY = MachineModelRenderer.standalone("block/fusion_plasma_forge_body");
    private static final ModelResourceLocation PLASMA_FORGE_PLASMA = MachineModelRenderer.standalone("block/fusion_plasma_forge_plasma");
    private static final ModelResourceLocation PLASMA_FORGE_PLASMA_GLOW = MachineModelRenderer.standalone("block/fusion_plasma_forge_plasma_glow");
    private static final ModelResourceLocation PLASMA_FORGE_SLIDER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_slider_striker");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_LOWER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_lower_striker");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_UPPER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_upper_striker");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_MOUNT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_mount");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_LEFT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_left");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_RIGHT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_right");
    private static final ModelResourceLocation PLASMA_FORGE_PISTON_LEFT = MachineModelRenderer.standalone("block/fusion_plasma_forge_piston_left");
    private static final ModelResourceLocation PLASMA_FORGE_PISTON_RIGHT = MachineModelRenderer.standalone("block/fusion_plasma_forge_piston_right");
    private static final ModelResourceLocation PLASMA_FORGE_SLIDER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_slider_jet");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_LOWER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_lower_jet");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_UPPER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_upper_jet");
    private static final ModelResourceLocation PLASMA_FORGE_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_jet");
    private static final List<ModelResourceLocation> ALL_MODELS = List.of(
            TORUS_TORUS, TORUS_MAGNET, TORUS_BOLTS1, TORUS_BOLTS2, TORUS_BOLTS3, TORUS_BOLTS4,
            TORUS_PLASMA, TORUS_PLASMA_GLOW, TORUS_PLASMA_SPARKLE,
            KLYSTRON_BODY, KLYSTRON_ROTOR, KLYSTRON_CREATIVE_BODY, KLYSTRON_CREATIVE_ROTOR,
            BOILER, BREEDER, COLLECTOR, COUPLER, MHDT_TURBINE, MHDT_COILS,
            PLASMA_FORGE_BODY, PLASMA_FORGE_PLASMA, PLASMA_FORGE_PLASMA_GLOW,
            PLASMA_FORGE_SLIDER_STRIKER, PLASMA_FORGE_ARM_LOWER_STRIKER,
            PLASMA_FORGE_ARM_UPPER_STRIKER, PLASMA_FORGE_STRIKER_MOUNT,
            PLASMA_FORGE_STRIKER_LEFT, PLASMA_FORGE_STRIKER_RIGHT,
            PLASMA_FORGE_PISTON_LEFT, PLASMA_FORGE_PISTON_RIGHT,
            PLASMA_FORGE_SLIDER_JET, PLASMA_FORGE_ARM_LOWER_JET,
            PLASMA_FORGE_ARM_UPPER_JET, PLASMA_FORGE_JET
    );
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static volatile GeometryGeneration geometryGeneration;
    private static final double FLOAT_UNIT_ROUNDOFF = 0x1.0p-24;
    /*
     * The longest emitted path is base translate/yaw (2), ring yaw (1), four
     * rotateAround triples (12), one piston translation (1), then one
     * position transform.  A 4x4 postmultiply has 64 products and 48 sums;
     * each of the six angle/quaternion constructions is bounded by 32 more
     * rounded operations.  The resulting count covers the complete path.
     */
    private static final int FLOAT_PIPELINE_OPERATIONS = 16 * 112 + 6 * 32 + 7;
    private static final double FLOAT_PIPELINE_GAMMA =
            FLOAT_PIPELINE_OPERATIONS * FLOAT_UNIT_ROUNDOFF
                    / (1.0D - FLOAT_PIPELINE_OPERATIONS * FLOAT_UNIT_ROUNDOFF);
    private static final GeometryTemplate PROCEDURAL_JET = proceduralJetTemplate();

    public FusionMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        FusionMachineBlockEntity.setClientRenderSnapshotUpdater(FusionMachineBlockEntityRenderer::updateRenderSnapshot);
    }

    static void rebuildGeometryGenerationAfterBake() {
        Map<ModelResourceLocation, BakedModel> models = new HashMap<>();
        for (ModelResourceLocation location : ALL_MODELS) {
            models.put(location, MachineModelRenderer.model(location));
        }
        List<BlockState> states = new ArrayList<>();
        for (net.minecraft.world.level.block.Block block : List.of(
                HbmBlocks.FUSION_TORUS.get(), HbmBlocks.FUSION_KLYSTRON.get(),
                HbmBlocks.FUSION_KLYSTRON_CREATIVE.get(), HbmBlocks.FUSION_BREEDER.get(),
                HbmBlocks.FUSION_COLLECTOR.get(), HbmBlocks.FUSION_BOILER.get(),
                HbmBlocks.FUSION_MHDT.get(), HbmBlocks.FUSION_COUPLER.get(),
                HbmBlocks.FUSION_PLASMA_FORGE.get())) {
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        geometryGeneration = captureGeometryGeneration(Map.copyOf(models), List.copyOf(states));
        FusionMachineBlockEntity.setClientRenderSnapshotUpdater(FusionMachineBlockEntityRenderer::updateRenderSnapshot);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TORUS_TORUS);
        event.register(TORUS_MAGNET);
        event.register(TORUS_BOLTS1);
        event.register(TORUS_BOLTS2);
        event.register(TORUS_BOLTS3);
        event.register(TORUS_BOLTS4);
        event.register(TORUS_PLASMA);
        event.register(TORUS_PLASMA_GLOW);
        event.register(TORUS_PLASMA_SPARKLE);
        event.register(KLYSTRON_BODY);
        event.register(KLYSTRON_ROTOR);
        event.register(KLYSTRON_CREATIVE_BODY);
        event.register(KLYSTRON_CREATIVE_ROTOR);
        event.register(BOILER);
        event.register(BREEDER);
        event.register(COLLECTOR);
        event.register(COUPLER);
        event.register(MHDT_TURBINE);
        event.register(MHDT_COILS);
        event.register(PLASMA_FORGE_BODY);
        event.register(PLASMA_FORGE_PLASMA);
        event.register(PLASMA_FORGE_PLASMA_GLOW);
        event.register(PLASMA_FORGE_SLIDER_STRIKER);
        event.register(PLASMA_FORGE_ARM_LOWER_STRIKER);
        event.register(PLASMA_FORGE_ARM_UPPER_STRIKER);
        event.register(PLASMA_FORGE_STRIKER_MOUNT);
        event.register(PLASMA_FORGE_STRIKER_LEFT);
        event.register(PLASMA_FORGE_STRIKER_RIGHT);
        event.register(PLASMA_FORGE_PISTON_LEFT);
        event.register(PLASMA_FORGE_PISTON_RIGHT);
        event.register(PLASMA_FORGE_SLIDER_JET);
        event.register(PLASMA_FORGE_ARM_LOWER_JET);
        event.register(PLASMA_FORGE_ARM_UPPER_JET);
        event.register(PLASMA_FORGE_JET);
    }

    @Override
    public void render(FusionMachineBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FusionMachineBlockEntity.RenderSnapshot snapshot = machine.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration geometry)) {
            return;
        }
        BlockState state = snapshot.blockState();

        poseStack.pushPose();
        switch (machine.kind()) {
            case TORUS -> renderTorus(geometry, machine, snapshot, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
            case KLYSTRON -> renderKlystron(geometry, machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, klystronFacing(state));
            case KLYSTRON_CREATIVE -> renderCreativeKlystron(geometry, machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, creativeKlystronFacing(state));
            case BOILER -> renderBoiler(geometry, poseStack, bufferSource, state, packedLight, packedOverlay, boilerFacing(state));
            case BREEDER -> renderBreeder(geometry, poseStack, bufferSource, state, packedLight, packedOverlay, breederFacing(state));
            case COLLECTOR -> renderCollector(geometry, poseStack, bufferSource, state, packedLight, packedOverlay, collectorFacing(state));
            case COUPLER -> renderCoupler(geometry, poseStack, bufferSource, state, packedLight, packedOverlay, couplerFacing(state));
            case MHDT -> renderMhdt(geometry, machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, mhdtFacing(state));
            case PLASMA_FORGE -> renderPlasmaForge(geometry, machine, snapshot, partialTick, poseStack, bufferSource,
                    state, packedLight, packedOverlay, plasmaForgeFacing(state));
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FusionMachineBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().renderBounds();
    }

    private static void renderTorus(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            FusionMachineBlockEntity.RenderSnapshot snapshot,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        if (machine.tilted()) {
            // Exact RenderFusionTorus order; this is intentionally not a
            // generic large-machine rotation convention.
            poseStack.translate(0.0D, -1.0D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(5.0F));
        }
        renderCaptured(geometry.part(TORUS_TORUS, state), poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(machine.rotor(partialTick)));
        renderCaptured(geometry.part(TORUS_MAGNET, state), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (snapshot.torusConnection(0)) {
            renderCaptured(geometry.part(TORUS_BOLTS2, state), poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (snapshot.torusConnection(1)) {
            renderCaptured(geometry.part(TORUS_BOLTS4, state), poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (snapshot.torusConnection(2)) {
            renderCaptured(geometry.part(TORUS_BOLTS3, state), poseStack, bufferSource, packedLight, packedOverlay);
        }
        if (snapshot.torusConnection(3)) {
            renderCaptured(geometry.part(TORUS_BOLTS1, state), poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (machine.plasmaEnergy() > 0L) {
            renderTorusPlasma(geometry, machine, poseStack, bufferSource, state, packedOverlay);
        }
    }

    private static void renderKlystron(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        applyKlystronTransform(poseStack, facing);
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderCaptured(geometry.part(KLYSTRON_BODY, state),
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(machine.rotor(partialTick)), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -2.5D, 0.0D);
        renderCaptured(geometry.part(KLYSTRON_ROTOR, state),
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCreativeKlystron(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        applyCreativeKlystronTransform(poseStack, facing);
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderCaptured(geometry.part(KLYSTRON_CREATIVE_BODY, state),
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(machine.rotor(partialTick)), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -2.5D, 0.0D);
        renderCaptured(geometry.part(KLYSTRON_CREATIVE_ROTOR, state),
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBoiler(GeometryGeneration geometry, PoseStack poseStack,
                                     MultiBufferSource bufferSource, BlockState state,
                                     int packedLight, int packedOverlay, Direction facing) {
        applyBoilerTransform(poseStack, facing);
        renderCaptured(geometry.part(BOILER, state), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderBreeder(GeometryGeneration geometry, PoseStack poseStack,
                                      MultiBufferSource bufferSource, BlockState state,
                                      int packedLight, int packedOverlay, Direction facing) {
        applyBreederTransform(poseStack, facing);
        renderCaptured(geometry.part(BREEDER, state), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderCollector(GeometryGeneration geometry, PoseStack poseStack,
                                        MultiBufferSource bufferSource, BlockState state,
                                        int packedLight, int packedOverlay, Direction facing) {
        applyCollectorTransform(poseStack, facing);
        renderCaptured(geometry.part(COLLECTOR, state), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderCoupler(GeometryGeneration geometry, PoseStack poseStack,
                                      MultiBufferSource bufferSource, BlockState state,
                                      int packedLight, int packedOverlay, Direction facing) {
        applyCouplerTransform(poseStack, facing);
        renderCaptured(geometry.part(COUPLER, state), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderMhdt(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        applyMhdtTransform(poseStack, facing);
        renderCaptured(geometry.part(MHDT_TURBINE, state), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(machine.rotor(partialTick) % 15.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        renderCaptured(geometry.part(MHDT_COILS, state), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPlasmaForge(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            FusionMachineBlockEntity.RenderSnapshot snapshot,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        applyPlasmaForgeTransform(poseStack, facing);
        if (snapshot.plasmaForgeConnected()) {
            poseStack.pushPose();
            poseStack.translate(-2.0D, 0.0D, 0.0D);
            renderPart(geometry, TORUS_BOLTS1, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        renderPart(geometry, PLASMA_FORGE_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPlasmaForgePlasma(geometry, machine, poseStack, bufferSource, state, packedOverlay);
        renderPlasmaForgeRecipeItem(snapshot, machine, partialTick, poseStack, bufferSource, packedOverlay);
        renderPlasmaForgeBeam(snapshot, machine, partialTick, poseStack, bufferSource);
        renderPlasmaForgeArms(geometry, machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderPlasmaForgeArms(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        double[] striker = machine.plasmaForgeStriker(partialTick);
        double[] jet = machine.plasmaForgeJet(partialTick);
        double rotor = machine.plasmaForgeRing(partialTick);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) rotor));

        poseStack.pushPose();
        renderPart(geometry, PLASMA_FORGE_SLIDER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -2.75D, 2.5D, 0.0D, -striker[0], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_ARM_LOWER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -2.75D, 3.75D, 0.0D, -striker[1], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_ARM_UPPER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -1.5D, 3.75D, 0.0D, -striker[2], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_STRIKER_MOUNT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        rotateAround(poseStack, 0.0D, 3.375D, 0.5D, striker[3], Axis.X);
        renderPart(geometry, PLASMA_FORGE_STRIKER_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, -striker[4], 0.0D);
        renderPart(geometry, PLASMA_FORGE_PISTON_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        rotateAround(poseStack, 0.0D, 3.375D, -0.5D, -striker[3], Axis.X);
        renderPart(geometry, PLASMA_FORGE_STRIKER_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, -striker[5], 0.0D);
        renderPart(geometry, PLASMA_FORGE_PISTON_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        renderPart(geometry, PLASMA_FORGE_SLIDER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 2.75D, 2.5D, 0.0D, jet[0], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_ARM_LOWER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 2.75D, 3.75D, 0.0D, jet[1], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_ARM_UPPER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 1.5D, 3.75D, 0.0D, jet[2], Axis.Z);
        renderPart(geometry, PLASMA_FORGE_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (machine.plasmaForgeJetActive()) {
            renderPlasmaForgeJet(machine, poseStack, bufferSource);
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderPart(GeometryGeneration geometry, ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        renderCaptured(geometry.part(model, state), poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderCaptured(GeometryPart part, PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        STATIC_MESHES.renderCaptured(part, poseStack, packedLight, packedOverlay);
    }

    private static void renderCapturedTinted(GeometryPart part, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight,
                                             int packedOverlay, int argb) {
        for (CapturedLayer layer : part.layers()) {
            MachineModelRenderer.emitCapturedQuadsTinted(layer.quads(), poseStack,
                    bufferSource.getBuffer(layer.tintedRenderType()), packedLight, packedOverlay, argb);
        }
    }

    private static void renderCapturedTintedUvEyes(GeometryPart part, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedOverlay,
                                                   int argb, float uOffset, float vOffset) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        for (CapturedLayer layer : part.layers()) {
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
            for (BakedQuad quad : layer.quads()) {
                emitCapturedTintedUvQuad(quad, poseStack.last(), consumer, LightTexture.FULL_BRIGHT,
                        packedOverlay, red, green, blue, alpha, uOffset, vOffset);
            }
        }
    }

    private static void emitCapturedTintedUvQuad(BakedQuad quad, PoseStack.Pose pose,
                                                  VertexConsumer consumer, int packedLight,
                                                  int packedOverlay, float red, float green,
                                                  float blue, float alpha, float uOffset,
                                                  float vOffset) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        TextureAtlasSprite sprite = quad.getSprite();
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * stride;
            float[] normal = unpackNormal(vertices, base, stride, quad.getDirection());
            consumer.addVertex(pose,
                            Float.intBitsToFloat(vertices[base]),
                            Float.intBitsToFloat(vertices[base + 1]),
                            Float.intBitsToFloat(vertices[base + 2]))
                    .setColor(Math.round(red * 255.0F), Math.round(green * 255.0F),
                            Math.round(blue * 255.0F), Math.round(alpha * 255.0F))
                    .setUv(transformSpriteCoordinate(Float.intBitsToFloat(vertices[base + 4]),
                                    uOffset, sprite.getU0(), sprite.getU1()),
                            transformSpriteCoordinate(Float.intBitsToFloat(vertices[base + 5]),
                                    vOffset, sprite.getV0(), sprite.getV1()))
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, normal[0], normal[1], normal[2]);
        }
    }

    private static float transformSpriteCoordinate(float value, float offset, float min, float max) {
        if (offset == 0.0F) {
            return value;
        }
        float width = max - min;
        if (width <= 0.0F) {
            return value;
        }
        float normalized = (value - min) / width;
        normalized = normalized + offset;
        normalized = normalized - (float) Math.floor(normalized);
        return min + normalized * width;
    }

    private static float[] unpackNormal(int[] vertices, int base, int stride, Direction fallback) {
        if (stride > 7) {
            int packed = vertices[base + 7];
            float x = (byte) (packed & 0xFF) / 127.0F;
            float y = (byte) ((packed >>> 8) & 0xFF) / 127.0F;
            float z = (byte) ((packed >>> 16) & 0xFF) / 127.0F;
            if (x != 0.0F || y != 0.0F || z != 0.0F) {
                return new float[]{x, y, z};
            }
        }
        return new float[]{fallback.getStepX(), fallback.getStepY(), fallback.getStepZ()};
    }

    private static void renderTorusPlasma(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        long time = System.currentTimeMillis();
        float alpha = 0.35F + (float) (Math.sin(time / 1000.0D) * 0.25D);
        double mainOsc = sps(time / 1000.0D) % 1.0D;
        double glowOsc = Math.sin(time / 2000.0D) % 1.0D;
        double glowExtra = time / 10000.0D % 1.0D;
        double sparkleSpin = time / 500.0D * -1.0D % 1.0D;
        double sparkleOsc = Math.sin(time / 1000.0D) * 0.5D % 1.0D;

        renderPlasmaLayer(geometry, TORUS_PLASMA, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha, 1.0F), 0.0F, (float) mainOsc);
        if (closeEnough(machine, 100.0D)) {
            renderPlasmaLayer(geometry, TORUS_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha * 2.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
            renderPlasmaLayer(geometry, TORUS_PLASMA_SPARKLE, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 0.75F, 2.0F), (float) sparkleSpin, (float) sparkleOsc);
        }
    }

    private static void renderPlasmaForgePlasma(
            GeometryGeneration geometry,
            FusionMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        if (machine.plasmaEnergy() <= 0L) {
            renderCapturedTinted(geometry.part(PLASMA_FORGE_PLASMA, state), poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, packedOverlay, 0xFF000000);
            return;
        }
        long time = System.currentTimeMillis();
        float alpha = 0.5F + (float) (Math.sin(time / 500.0D) * 0.25D);
        double mainOsc = sps(time / 750.0D) % 1.0D;
        double glowOsc = Math.sin(time / 1000.0D) % 1.0D;
        double glowExtra = time / 10000.0D % 1.0D;
        renderPlasmaLayer(geometry, PLASMA_FORGE_PLASMA, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha, 1.0F), 0.0F, (float) mainOsc);
        renderPlasmaLayer(geometry, PLASMA_FORGE_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 1.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
        glowOsc = Math.sin(time / 600.0D + 2.0D) % 1.0D;
        glowExtra = time / 5000.0D % 1.0D;
        renderPlasmaLayer(geometry, PLASMA_FORGE_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 1.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
    }

    private static void renderPlasmaLayer(
            GeometryGeneration geometry,
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay,
            int argb,
            float uOffset,
            float vOffset
    ) {
        renderCapturedTintedUvEyes(geometry.part(model, state), poseStack, bufferSource,
                packedOverlay, argb, uOffset, vOffset);
    }

    private static void renderPlasmaForgeRecipeItem(
            FusionMachineBlockEntity.RenderSnapshot snapshot,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        if (!closeEnough(machine, 35.0D)) {
            return;
        }
        ItemStack stack = snapshot.renderedItem();
        if (stack.isEmpty() || !(snapshot.modelToken() instanceof BakedModel itemModel)) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, 1.75D + Math.sin((Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTick) * 0.1D) * 0.0625D, 0.0D);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.FIXED, false,
                poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, itemModel);
        poseStack.popPose();
    }

    /** Main-thread recipe/model resolution; render and section workers only consume the published record. */
    private static void updateRenderSnapshot(FusionMachineBlockEntity machine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(machine::refreshClientRenderSnapshot);
            return;
        }
        GeometryGeneration geometry = geometryGeneration;
        if (machine.getLevel() == null || geometry == null) {
            return;
        }

        BlockState state = machine.getBlockState();
        FusionMachineBlockEntity.RenderSnapshot previous = machine.renderSnapshot();
        RecipeHolder<PlasmaForgeRecipe> holder = machine.kind() == FusionMachineBlock.Kind.PLASMA_FORGE
                ? machine.plasmaForgeRecipeForRender(machine.getLevel()).orElse(null)
                : null;
        ItemStack source = holder == null || holder.value().displayIcon().isEmpty()
                ? ItemStack.EMPTY
                : holder.value().displayIcon().copyWithCount(1);
        long recipeGeneration = com.reinhardt.hbm.recipe.MachineRecipeCache.generation();
        long revision = FusionMachineBlockEntity.clientRenderSnapshotRevision();

        ItemStack rendered = previous.renderedItem();
        Object modelToken = previous.modelToken();
        AABB iconBounds = previous.iconBounds();
        boolean reuseIcon = previous.revision() == revision
                && previous.recipeGeneration() == recipeGeneration
                && previous.geometryToken() == geometry
                && previous.blockState() == state
                && java.util.Objects.equals(previous.plasmaForgeRecipeId(), holder == null ? null : holder.id())
                && ItemStack.isSameItemSameComponents(previous.sourceItem(), source);
        if (!reuseIcon) {
            PoseStack lowerPose = recipeIconPose(state, 1.6875F);
            PoseStack upperPose = recipeIconPose(state, 1.8125F);
            BakedItemGeometrySnapshot.Result lower = BakedItemGeometrySnapshot.build(
                    source, lowerPose, machine.getBlockPos(), machine.getLevel());
            BakedItemGeometrySnapshot.Result upper = BakedItemGeometrySnapshot.build(
                    source, upperPose, machine.getBlockPos(), machine.getLevel());
            if (!lower.renderedItem().isEmpty()
                    && !upper.renderedItem().isEmpty()
                    && lower.modelToken() == upper.modelToken()) {
                rendered = lower.renderedItem();
                modelToken = lower.modelToken();
                iconBounds = lower.bounds().minmax(upper.bounds());
            } else {
                rendered = ItemStack.EMPTY;
                modelToken = null;
                iconBounds = new AABB(machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ(),
                        machine.getBlockPos().getX(), machine.getBlockPos().getY(), machine.getBlockPos().getZ());
            }
        }

        boolean plasmaForgeConnected = machine.kind() == FusionMachineBlock.Kind.PLASMA_FORGE && machine.connected();
        int torusConnectionMask = 0;
        if (machine.kind() == FusionMachineBlock.Kind.TORUS) {
            for (int index = 0; index < 4; index++) {
                if (machine.torusConnection(index)) {
                    torusConnectionMask |= 1 << index;
                }
            }
        }
        FusionMachineBlockEntity.RenderSnapshot draft = new FusionMachineBlockEntity.RenderSnapshot(
                state, plasmaForgeConnected, torusConnectionMask, holder != null,
                holder == null ? null : holder.id(), source, rendered, modelToken,
                geometry, iconBounds, zeroVolume(machine.getBlockPos()), recipeGeneration, revision);
        AABB renderBounds = renderBounds(machine, draft).minmax(iconBounds);
        machine.publishClientRenderSnapshot(new FusionMachineBlockEntity.RenderSnapshot(
                state, plasmaForgeConnected, torusConnectionMask, holder != null,
                holder == null ? null : holder.id(), source, rendered, modelToken,
                geometry, iconBounds, renderBounds, recipeGeneration, revision));
    }

    private static PoseStack recipeIconPose(BlockState state, float y) {
        PoseStack poseStack = new PoseStack();
        applyPlasmaForgeTransform(poseStack, plasmaForgeFacing(state));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        return poseStack;
    }

    private static AABB renderBounds(FusionMachineBlockEntity machine,
                                     FusionMachineBlockEntity.RenderSnapshot snapshot) {
        FusionMachineBlock.Kind kind = machine.kind();
        BlockPos pos = machine.getBlockPos();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration geometry)) {
            return zeroVolume(pos);
        }
        BlockState state = snapshot.blockState();
        Bounds bounds = new Bounds(pos);
        switch (kind) {
            case TORUS -> includeTorusBounds(bounds, geometry, state, snapshot, machine.tilted());
            case KLYSTRON -> includeKlystronBounds(bounds, geometry, state, klystronFacing(state));
            case KLYSTRON_CREATIVE -> includeCreativeKlystronBounds(bounds, geometry, state, creativeKlystronFacing(state));
            case BOILER -> includeBoilerBounds(bounds, geometry, state, boilerFacing(state));
            case BREEDER -> includeBreederBounds(bounds, geometry, state, breederFacing(state));
            case COLLECTOR -> includeCollectorBounds(bounds, geometry, state, collectorFacing(state));
            case COUPLER -> includeCouplerBounds(bounds, geometry, state, couplerFacing(state));
            case MHDT -> includeMhdtBounds(bounds, geometry, state, mhdtFacing(state));
            case PLASMA_FORGE -> includePlasmaForgeBounds(bounds, geometry, state, plasmaForgeFacing(state), snapshot);
        }
        return bounds.toAabb();
    }

    private static AABB zeroVolume(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static void includeTorusBounds(Bounds bounds, GeometryGeneration geometry, BlockState state,
                                            FusionMachineBlockEntity.RenderSnapshot snapshot, boolean tilted) {
        PoseStack root = new PoseStack();
        root.translate(0.5D, 0.0D, 0.5D);
        if (tilted) {
            root.translate(0.0D, -1.0D, 0.0D);
            root.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(10.0F));
            root.mulPose(com.mojang.math.Axis.YP.rotationDegrees(5.0F));
        }
        geometry.part(TORUS_TORUS, state).template().includeTransformed(bounds, root.last().pose());
        geometry.part(TORUS_PLASMA, state).template().includeTransformed(bounds, root.last().pose());
        geometry.part(TORUS_PLASMA_GLOW, state).template().includeTransformed(bounds, root.last().pose());
        geometry.part(TORUS_PLASMA_SPARKLE, state).template().includeTransformed(bounds, root.last().pose());
        if (snapshot.torusConnection(0)) includeStatic(bounds, geometry, TORUS_BOLTS2, state, root);
        if (snapshot.torusConnection(1)) includeStatic(bounds, geometry, TORUS_BOLTS4, state, root);
        if (snapshot.torusConnection(2)) includeStatic(bounds, geometry, TORUS_BOLTS3, state, root);
        if (snapshot.torusConnection(3)) includeStatic(bounds, geometry, TORUS_BOLTS1, state, root);
        includeAxisSweep(bounds, geometry.part(TORUS_MAGNET, state).template(), root.last().pose(),
                Axis.Y, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 5.0F);
    }

    private static void includeKlystronBounds(Bounds bounds, GeometryGeneration geometry,
                                               BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyKlystronTransform(root, facing);
        root.translate(-1.0D, 0.0D, 0.0D);
        geometry.part(KLYSTRON_BODY, state).template().includeTransformed(bounds, root.last().pose());
        includeAxisSweep(bounds, geometry.part(KLYSTRON_ROTOR, state).template(), root.last().pose(),
                Axis.X, 0.0F, 2.5F, 0.0F, 0.0F, 360.0F, 5.0F);
    }

    private static void includeCreativeKlystronBounds(Bounds bounds, GeometryGeneration geometry,
                                                       BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyCreativeKlystronTransform(root, facing);
        root.translate(-1.0D, 0.0D, 0.0D);
        geometry.part(KLYSTRON_CREATIVE_BODY, state).template().includeTransformed(bounds, root.last().pose());
        includeAxisSweep(bounds, geometry.part(KLYSTRON_CREATIVE_ROTOR, state).template(), root.last().pose(),
                Axis.X, 0.0F, 2.5F, 0.0F, 0.0F, 360.0F, 5.0F);
    }

    private static void includeBoilerBounds(Bounds bounds, GeometryGeneration geometry,
                                             BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyBoilerTransform(root, facing);
        geometry.part(BOILER, state).template().includeTransformed(bounds, root.last().pose());
    }

    private static void includeBreederBounds(Bounds bounds, GeometryGeneration geometry,
                                              BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyBreederTransform(root, facing);
        geometry.part(BREEDER, state).template().includeTransformed(bounds, root.last().pose());
    }

    private static void includeCollectorBounds(Bounds bounds, GeometryGeneration geometry,
                                                BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyCollectorTransform(root, facing);
        geometry.part(COLLECTOR, state).template().includeTransformed(bounds, root.last().pose());
    }

    private static void includeCouplerBounds(Bounds bounds, GeometryGeneration geometry,
                                              BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyCouplerTransform(root, facing);
        geometry.part(COUPLER, state).template().includeTransformed(bounds, root.last().pose());
    }

    private static void includeMhdtBounds(Bounds bounds, GeometryGeneration geometry,
                                          BlockState state, Direction facing) {
        PoseStack root = new PoseStack();
        applyMhdtTransform(root, facing);
        geometry.part(MHDT_TURBINE, state).template().includeTransformed(bounds, root.last().pose());
        includeAxisSweep(bounds, geometry.part(MHDT_COILS, state).template(), root.last().pose(),
                Axis.X, 0.0F, 1.5F, 0.0F, 0.0F, 15.0F, 5.0F);
    }

    private static void includeStatic(Bounds bounds, GeometryGeneration geometry, ModelResourceLocation location,
                                      BlockState state, PoseStack pose) {
        geometry.part(location, state).template().includeTransformed(bounds, pose.last().pose());
    }

    private static void includePlasmaForgeBounds(Bounds bounds, GeometryGeneration geometry,
                                                  BlockState state, Direction facing,
                                                  FusionMachineBlockEntity.RenderSnapshot snapshot) {
        PoseStack root = new PoseStack();
        applyPlasmaForgeTransform(root, facing);
        includeStatic(bounds, geometry, PLASMA_FORGE_BODY, state, root);
        includeStatic(bounds, geometry, PLASMA_FORGE_PLASMA, state, root);
        includeStatic(bounds, geometry, PLASMA_FORGE_PLASMA_GLOW, state, root);
        if (snapshot.plasmaForgeConnected()) {
            root.pushPose();
            root.translate(-2.0D, 0.0D, 0.0D);
            includeStatic(bounds, geometry, TORUS_BOLTS1, state, root);
            root.popPose();
        }
        if (snapshot.plasmaForgeBeamVisible()) {
            bounds.includeBox(root.last().pose(), -0.4375F, 1.0F, -0.4375F,
                    0.4375F, 2.625F, 0.4375F);
        }

        LocalEnvelope ring = new LocalEnvelope();
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_SLIDER_STRIKER, state).template(), List.of(), 0.0F);
        MotionJoint strikerLower = new MotionJoint(Axis.Z, -2.75F, 2.5F, 0.0F, -45.0F, 0.0F, 5.0F);
        MotionJoint strikerUpper = new MotionJoint(Axis.Z, -2.75F, 3.75F, 0.0F, -10.0F, 80.0F, 5.0F);
        MotionJoint strikerMount = new MotionJoint(Axis.Z, -1.5F, 3.75F, 0.0F, -15.0F, 55.0F, 5.0F);
        MotionJoint strikerRight = new MotionJoint(Axis.X, 0.0F, 3.375F, 0.5F, 0.0F, 30.0F, 5.0F);
        MotionJoint strikerLeft = new MotionJoint(Axis.X, 0.0F, 3.375F, -0.5F, -30.0F, 0.0F, 5.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_ARM_LOWER_STRIKER, state).template(),
                List.of(strikerLower), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_ARM_UPPER_STRIKER, state).template(),
                List.of(strikerLower, strikerUpper), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_STRIKER_MOUNT, state).template(),
                List.of(strikerLower, strikerUpper, strikerMount), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_STRIKER_RIGHT, state).template(),
                List.of(strikerLower, strikerUpper, strikerMount, strikerRight), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_PISTON_RIGHT, state).template(),
                List.of(strikerLower, strikerUpper, strikerMount, strikerRight), 0.5F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_STRIKER_LEFT, state).template(),
                List.of(strikerLower, strikerUpper, strikerMount, strikerLeft), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_PISTON_LEFT, state).template(),
                List.of(strikerLower, strikerUpper, strikerMount, strikerLeft), 0.5F);

        includeRingSwept(ring, geometry.part(PLASMA_FORGE_SLIDER_JET, state).template(), List.of(), 0.0F);
        MotionJoint jetLower = new MotionJoint(Axis.Z, 2.75F, 2.5F, 0.0F, 0.0F, 30.0F, 5.0F);
        MotionJoint jetUpper = new MotionJoint(Axis.Z, 2.75F, 3.75F, 0.0F, 0.0F, 50.0F, 5.0F);
        MotionJoint jetHead = new MotionJoint(Axis.Z, 1.5F, 3.75F, 0.0F, -160.0F, 0.0F, 5.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_ARM_LOWER_JET, state).template(),
                List.of(jetLower), 0.0F);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_ARM_UPPER_JET, state).template(),
                List.of(jetLower, jetUpper), 0.0F);
        List<MotionJoint> jetChain = List.of(jetLower, jetUpper, jetHead);
        includeRingSwept(ring, geometry.part(PLASMA_FORGE_JET, state).template(), jetChain, 0.0F);
        includeRingSwept(ring, PROCEDURAL_JET, jetChain, 0.0F);
        bounds.includeBox(root.last().pose(), ring.outwardMinX(), ring.outwardMinY(), ring.outwardMinZ(),
                ring.outwardMaxX(), ring.outwardMaxY(), ring.outwardMaxZ());
    }

    /** Samples the exact float pose and adds the proved maximum motion between adjacent samples. */
    private static void includeAxisSweep(Bounds destination, GeometryTemplate template, Matrix4f root,
                                         Axis axis, float pivotX, float pivotY, float pivotZ,
                                         float minAngle, float maxAngle, float sampleStep) {
        Bounds swept = new Bounds(destination.pos());
        for (float angle : samples(minAngle, maxAngle, sampleStep)) {
            Matrix4f matrix = new Matrix4f(root)
                    .translate(pivotX, pivotY, pivotZ)
                    .rotate(axis.quaternion(angle))
                    .translate(-pivotX, -pivotY, -pivotZ);
            template.includeTransformed(swept, matrix);
        }
        double radius = template.maxRadius(axis, pivotX, pivotY, pivotZ);
        double angleDelta = maximumSampleAngleDelta(sampleStep, minAngle, maxAngle);
        double motion = Math.nextUp(2.0D * radius * Math.sin(angleDelta * 0.5D));
        double magnitude = template.maxL1() + 2.0D * (Math.abs(pivotX) + Math.abs(pivotY) + Math.abs(pivotZ))
                + matrixMagnitude(root);
        swept.expand(Math.nextUp(motion + floatPipelineError(magnitude)));
        destination.include(swept);
    }

    private static void includeRingSwept(LocalEnvelope destination, GeometryTemplate template,
                                         List<MotionJoint> joints, float translationRange) {
        LocalEnvelope sampled = new LocalEnvelope();
        sampleJointPoses(sampled, template, joints, 0, new PoseStack(), translationRange);

        double[] radii = new double[joints.size()];
        for (int index = 0; index < template.vertices().length; index += 3) {
            double x = template.vertices()[index];
            double y = template.vertices()[index + 1];
            double z = template.vertices()[index + 2];
            for (float translation : translationRange == 0.0F
                    ? new float[]{0.0F} : new float[]{0.0F, translationRange}) {
                double downstream = translation;
                for (int joint = joints.size() - 1; joint >= 0; joint--) {
                    MotionJoint current = joints.get(joint);
                    if (joint == joints.size() - 1) {
                        downstream += distance(x, y - translation, z,
                                current.pivotX(), current.pivotY(), current.pivotZ());
                    } else {
                        MotionJoint child = joints.get(joint + 1);
                        downstream += distance(current.pivotX(), current.pivotY(), current.pivotZ(),
                                child.pivotX(), child.pivotY(), child.pivotZ());
                    }
                    radii[joint] = Math.max(radii[joint], downstream);
                }
            }
        }
        double motion = translationRange * 0.5D;
        double magnitude = template.maxL1() + translationRange + 1.0D;
        for (int joint = 0; joint < joints.size(); joint++) {
            MotionJoint current = joints.get(joint);
            double angleDelta = maximumSampleAngleDelta(current.sampleStep(), current.minAngle(), current.maxAngle());
            motion += 2.0D * radii[joint] * Math.sin(angleDelta * 0.5D);
            magnitude += 2.0D * (Math.abs(current.pivotX()) + Math.abs(current.pivotY())
                    + Math.abs(current.pivotZ()));
        }
        sampled.expand(Math.nextUp(motion + floatPipelineError(magnitude)));
        destination.include(sampled);
    }

    private static void sampleJointPoses(LocalEnvelope envelope, GeometryTemplate template,
                                         List<MotionJoint> joints, int index, PoseStack pose,
                                         float translationRange) {
        if (index < joints.size()) {
            MotionJoint joint = joints.get(index);
            for (float angle : samples(joint.minAngle(), joint.maxAngle(), joint.sampleStep())) {
                pose.pushPose();
                rotateAround(pose, joint.pivotX(), joint.pivotY(), joint.pivotZ(), angle, joint.axis());
                sampleJointPoses(envelope, template, joints, index + 1, pose, translationRange);
                pose.popPose();
            }
            return;
        }
        if (translationRange == 0.0F) {
            template.includeRingTransformed(envelope, pose.last().pose());
            return;
        }
        for (float translation : new float[]{0.0F, translationRange}) {
            pose.pushPose();
            pose.translate(0.0F, -translation, 0.0F);
            template.includeRingTransformed(envelope, pose.last().pose());
            pose.popPose();
        }
    }

    private static List<Float> samples(float minimum, float maximum, float step) {
        List<Float> result = new ArrayList<>();
        for (float value = minimum; value < maximum; value += step) {
            result.add(value);
        }
        if (result.isEmpty() || result.get(result.size() - 1) != maximum) {
            result.add(maximum);
        }
        return result;
    }

    private static double maximumSampleAngleDelta(float step, float minimum, float maximum) {
        double largestMagnitude = Math.max(Math.abs((double) minimum), Math.abs((double) maximum));
        double radians = Math.toRadians(largestMagnitude);
        double inputError = Math.ulp((float) radians)
                + largestMagnitude * Math.abs(Math.toRadians(1.0D) - Math.PI / 180.0D);
        return Math.nextUp(Math.toRadians(step) * 0.5D + 2.0D * inputError);
    }

    private static double matrixMagnitude(Matrix4f matrix) {
        return Math.abs(matrix.m00()) + Math.abs(matrix.m10()) + Math.abs(matrix.m20()) + Math.abs(matrix.m30())
                + Math.abs(matrix.m01()) + Math.abs(matrix.m11()) + Math.abs(matrix.m21()) + Math.abs(matrix.m31())
                + Math.abs(matrix.m02()) + Math.abs(matrix.m12()) + Math.abs(matrix.m22()) + Math.abs(matrix.m32());
    }

    private static double floatPipelineError(double magnitude) {
        return Math.nextUp(2.0D * FLOAT_PIPELINE_GAMMA * magnitude);
    }

    private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt((x1 - x2) * (x1 - x2)
                + (y1 - y2) * (y1 - y2)
                + (z1 - z2) * (z1 - z2));
    }

    private static GeometryTemplate proceduralJetTemplate() {
        List<Float> coordinates = new ArrayList<>();
        appendJetLayerVertices(coordinates, 1.0D, 0.01D, 0.125D, 1.375D, 1.625D);
        appendJetLayerVertices(coordinates, 1.125D, 0.01D, 0.125D, 1.375D, 1.625D);
        appendJetLayerVertices(coordinates, 1.5D, 0.09375D, 0.125D, 1.375D, 1.625D);
        appendJetLayerVertices(coordinates, 1.6875D, 0.09375D, 0.125D, 1.375D, 1.625D);
        float[] vertices = new float[coordinates.size()];
        for (int index = 0; index < coordinates.size(); index++) {
            vertices[index] = coordinates.get(index);
        }
        return new GeometryTemplate(vertices);
    }

    private static void appendJetLayerVertices(List<Float> out, double outerLen, double narrow,
                                               double side, double near, double far) {
        appendQuadVertices(out, near, 3.0D, side, far, 3.0D, side,
                far - narrow, 3.0D - outerLen, side - narrow,
                near + narrow, 3.0D - outerLen, side - narrow);
        appendQuadVertices(out, near, 3.0D, -side, far, 3.0D, -side,
                far - narrow, 3.0D - outerLen, -side + narrow,
                near + narrow, 3.0D - outerLen, -side + narrow);
        appendQuadVertices(out, near, 3.0D, side, near, 3.0D, -side,
                near + narrow, 3.0D - outerLen, -side + narrow,
                near + narrow, 3.0D - outerLen, side - narrow);
        appendQuadVertices(out, far, 3.0D, side, far, 3.0D, -side,
                far - narrow, 3.0D - outerLen, -side + narrow,
                far - narrow, 3.0D - outerLen, side - narrow);
    }

    private static void appendQuadVertices(List<Float> out,
                                           double x1, double y1, double z1,
                                           double x2, double y2, double z2,
                                           double x3, double y3, double z3,
                                           double x4, double y4, double z4) {
        for (double value : new double[]{x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4}) {
            out.add((float) value);
        }
    }

    private static void renderPlasmaForgeBeam(
            FusionMachineBlockEntity.RenderSnapshot snapshot,
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (!closeEnough(machine, 50.0D) || !snapshot.plasmaForgeBeamVisible()) {
            return;
        }
        double offset = ((Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTick) / 15.0D) % 1.0D;
        double in = 0.4375D;
        double bottom = 1.0D;
        double top = 2.5D + offset * 0.125D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        quad(pose, consumer, -in, bottom, in, -in, top, in, -in, top, -in, -in, bottom, -in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, top, in, in, bottom, in, in, bottom, -in, in, top, -in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, bottom, in, in, top, in, -in, top, in, -in, bottom, in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, top, -in, in, bottom, -in, -in, bottom, -in, -in, top, -in, 255, 255, 255, 160, 0);
    }

    private static void renderPlasmaForgeJet(FusionMachineBlockEntity machine, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        int r = Math.round(clamp(machine.plasmaRed() * 255.0F, 0.0F, 255.0F));
        int g = Math.round(clamp(machine.plasmaGreen() * 255.0F, 0.0F, 255.0F));
        int b = Math.round(clamp(machine.plasmaBlue() * 255.0F, 0.0F, 255.0F));
        double outerLen = 1.0D + (System.nanoTime() & 0xFFFF) / 65535.0D * 0.125D;
        renderJetLayer(pose, consumer, r, g, b, outerLen, 0.01D, 0.125D, 1.375D, 1.625D);
        renderJetLayer(pose, consumer, r, g, b, outerLen * 1.5D, 0.0625D * 1.5D, 0.125D, 1.375D, 1.625D);
    }

    private static void renderJetLayer(PoseStack.Pose pose, VertexConsumer consumer, int r, int g, int b, double outerLen, double narrow, double side, double near, double far) {
        quad(pose, consumer, near, 3.0D, side, far, 3.0D, side, far - narrow, 3.0D - outerLen, side - narrow, near + narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
        quad(pose, consumer, near, 3.0D, -side, far, 3.0D, -side, far - narrow, 3.0D - outerLen, -side + narrow, near + narrow, 3.0D - outerLen, -side + narrow, r, g, b, 255, 0);
        quad(pose, consumer, near, 3.0D, side, near, 3.0D, -side, near + narrow, 3.0D - outerLen, -side + narrow, near + narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
        quad(pose, consumer, far, 3.0D, side, far, 3.0D, -side, far - narrow, 3.0D - outerLen, -side + narrow, far - narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4,
            int r,
            int g,
            int b,
            int nearAlpha,
            int farAlpha
    ) {
        vertex(pose, consumer, x1, y1, z1, r, g, b, nearAlpha);
        vertex(pose, consumer, x2, y2, z2, r, g, b, nearAlpha);
        vertex(pose, consumer, x3, y3, z3, r, g, b, farAlpha);
        vertex(pose, consumer, x4, y4, z4, r, g, b, farAlpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    private static Direction klystronFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion klystron state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion klystron state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction creativeKlystronFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Creative fusion klystron state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Creative fusion klystron state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction boilerFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion boiler state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion boiler state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction breederFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion breeder state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion breeder state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction collectorFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion collector state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion collector state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction couplerFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion coupler state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion coupler state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction mhdtFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion MHDT state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion MHDT state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction plasmaForgeFacing(BlockState state) {
        if (!state.hasProperty(FusionMachineBlock.FACING)) {
            throw new IllegalStateException("Fusion plasma forge state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(FusionMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Fusion plasma forge state has a vertical facing: " + facing);
        }
        return facing;
    }

    /** RenderFusionKlystron metadata table: N/E/S/W -> 90/0/270/180. */
    private static void applyKlystronTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal klystron facing: " + facing);
        }
    }

    /** RenderFusionKlystronCreative has its own identical metadata table. */
    private static void applyCreativeKlystronTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal creative klystron facing: " + facing);
        }
    }

    /** RenderFusionBoiler metadata table. */
    private static void applyBoilerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal fusion boiler facing: " + facing);
        }
    }

    /** RenderFusionBreeder metadata table. */
    private static void applyBreederTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal fusion breeder facing: " + facing);
        }
    }

    /** RenderFusionCollector metadata table. */
    private static void applyCollectorTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal fusion collector facing: " + facing);
        }
    }

    /** RenderFusionCoupler applies its fixed 90 degrees before the metadata table. */
    private static void applyCouplerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(360.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal fusion coupler facing: " + facing);
        }
    }

    /** RenderFusionMHDT metadata table. */
    private static void applyMhdtTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal MHDT facing: " + facing);
        }
    }

    /** RenderFusionPlasma applies its fixed 90 degrees before the metadata table. */
    private static void applyPlasmaForgeTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(360.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Non-horizontal plasma forge facing: " + facing);
        }
    }

    private static void rotateAround(PoseStack poseStack, double x, double y, double z, double degrees, Axis axis) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(axis.quaternion((float) degrees));
        poseStack.translate(-x, -y, -z);
    }

    private static int plasmaColor(FusionMachineBlockEntity machine, float alpha, float multiplier) {
        int a = Math.round(clamp(alpha * 255.0F, 0.0F, 255.0F));
        int r = Math.round(clamp(machine.plasmaRed() * multiplier * 255.0F, 0.0F, 255.0F));
        int g = Math.round(clamp(machine.plasmaGreen() * multiplier * 255.0F, 0.0F, 255.0F));
        int b = Math.round(clamp(machine.plasmaBlue() * multiplier * 255.0F, 0.0F, 255.0F));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double sps(double value) {
        return Math.sin(Math.PI / 2.0D * Math.cos(value));
    }

    private static boolean closeEnough(FusionMachineBlockEntity machine, double distance) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null
                || minecraft.player.distanceToSqr(
                machine.getBlockPos().getX() + 0.5D,
                machine.getBlockPos().getY() + 2.5D,
                machine.getBlockPos().getZ() + 0.5D
        ) < distance * distance;
    }

    /** Captures every model/state combination once on the post-bake client task. */
    private static GeometryGeneration captureGeometryGeneration(
            Map<ModelResourceLocation, BakedModel> models,
            List<BlockState> states
    ) {
        Map<GeometryKey, GeometryPart> parts = new HashMap<>();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
            for (BlockState state : states) {
                parts.put(new GeometryKey(entry.getKey(), state), captureGeometryPart(entry.getValue(), state));
            }
        }
        return new GeometryGeneration(Map.copyOf(parts));
    }

    private static GeometryPart captureGeometryPart(BakedModel model, BlockState state) {
        RandomSource random = RandomSource.create(42L);
        random.setSeed(42L);
        List<CapturedLayer> layers = new ArrayList<>();
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            captureQuads(quads, model, state, null, renderType, random);
            for (Direction side : Direction.values()) {
                captureQuads(quads, model, state, side, renderType, random);
            }
            layers.add(new CapturedLayer(
                    RenderTypeHelper.getEntityRenderType(renderType, false),
                    RenderTypeHelper.getEntityRenderType(renderType, true),
                    List.copyOf(quads)));
        }
        List<CapturedLayer> capturedLayers = List.copyOf(layers);
        return new GeometryPart(capturedLayers, templateFromCapturedLayers(capturedLayers));
    }

    private static void captureQuads(List<BakedQuad> destination, BakedModel model, BlockState state,
                                     Direction side, RenderType renderType, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
            destination.add(new BakedQuad(
                    quad.getVertices().clone(), quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                    quad.isShade(), quad.hasAmbientOcclusion()));
        }
    }

    /** One immutable post-bake generation shared by rendering and its exact envelopes. */
    private static final class GeometryGeneration {
        private final Map<GeometryKey, GeometryPart> parts;

        private GeometryGeneration(Map<GeometryKey, GeometryPart> parts) {
            this.parts = parts;
        }

        private GeometryPart part(ModelResourceLocation location, BlockState state) {
            GeometryPart part = this.parts.get(new GeometryKey(location, state));
            if (part == null) {
                throw new IllegalStateException("Fusion geometry was not captured after bake for "
                        + location + " and state " + state);
            }
            return part;
        }
    }

    private record GeometryKey(ModelResourceLocation location, BlockState state) {
    }

    private record CapturedLayer(RenderType normalRenderType, RenderType tintedRenderType,
                                 List<BakedQuad> quads) implements StaticMachineVboCache.CapturedLayer {
    }

    private record GeometryPart(List<CapturedLayer> layers, GeometryTemplate template)
            implements StaticMachineVboCache.CapturedGeometry {
    }

    /** Face-used float vertices from the exact captured quad set replayed by this generation. */
    private record GeometryTemplate(float[] vertices) {
        private void includeTransformed(Bounds bounds, Matrix4f matrix) {
            for (int index = 0; index < this.vertices.length; index += 3) {
                bounds.include(matrix.transformPosition(new Vector3f(
                        this.vertices[index], this.vertices[index + 1], this.vertices[index + 2])));
            }
        }

        private void includeRingTransformed(LocalEnvelope envelope, Matrix4f matrix) {
            for (int index = 0; index < this.vertices.length; index += 3) {
                envelope.includeRingPoint(matrix.transformPosition(new Vector3f(
                        this.vertices[index], this.vertices[index + 1], this.vertices[index + 2])));
            }
        }

        private double maxRadius(Axis axis, float pivotX, float pivotY, float pivotZ) {
            double radius = 0.0D;
            for (int index = 0; index < this.vertices.length; index += 3) {
                double x = this.vertices[index] - pivotX;
                double y = this.vertices[index + 1] - pivotY;
                double z = this.vertices[index + 2] - pivotZ;
                double candidate = switch (axis) {
                    case X -> Math.hypot(y, z);
                    case Y -> Math.hypot(x, z);
                    case Z -> Math.hypot(x, y);
                };
                radius = Math.max(radius, Math.nextUp(candidate));
            }
            return radius;
        }

        private double maxL1() {
            double maximum = 0.0D;
            for (int index = 0; index < this.vertices.length; index += 3) {
                maximum = Math.max(maximum, Math.abs((double) this.vertices[index])
                        + Math.abs((double) this.vertices[index + 1])
                        + Math.abs((double) this.vertices[index + 2]));
            }
            return maximum;
        }
    }

    /** Face-used float vertices derived from the exact immutable quads rendered by this generation. */
    private static GeometryTemplate templateFromCapturedLayers(List<CapturedLayer> layers) {
        Set<VertexKey> coordinates = new LinkedHashSet<>();
        for (CapturedLayer layer : layers) {
            for (BakedQuad quad : layer.quads()) {
                int[] data = quad.getVertices();
                int stride = data.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    coordinates.add(new VertexKey(
                            Float.intBitsToFloat(data[offset]),
                            Float.intBitsToFloat(data[offset + 1]),
                            Float.intBitsToFloat(data[offset + 2])));
                }
            }
        }
        float[] vertices = new float[coordinates.size() * 3];
        int index = 0;
        for (VertexKey vertex : coordinates) {
            vertices[index++] = vertex.x();
            vertices[index++] = vertex.y();
            vertices[index++] = vertex.z();
        }
        return new GeometryTemplate(vertices);
    }

    private record VertexKey(float x, float y, float z) {
    }

    private record MotionJoint(Axis axis, float pivotX, float pivotY, float pivotZ,
                               float minAngle, float maxAngle, float sampleStep) {
    }

    private static final class LocalEnvelope {
        private double radius = Double.NEGATIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        private void includeRingPoint(Vector3f point) {
            this.radius = Math.max(this.radius, Math.nextUp(Math.hypot((double) point.x, (double) point.z)));
            this.minY = Math.min(this.minY, point.y);
            this.maxY = Math.max(this.maxY, point.y);
        }

        private void expand(double amount) {
            if (!Double.isFinite(this.radius)) {
                return;
            }
            this.radius = Math.nextUp(this.radius + amount);
            this.minY = Math.nextDown(this.minY - amount);
            this.maxY = Math.nextUp(this.maxY + amount);
        }

        private void include(LocalEnvelope other) {
            if (!Double.isFinite(other.radius)) {
                return;
            }
            this.radius = Math.max(this.radius, other.radius);
            this.minY = Math.min(this.minY, other.minY);
            this.maxY = Math.max(this.maxY, other.maxY);
        }

        private float outwardMinX() {
            return outwardLower(-this.radius);
        }

        private float outwardMaxX() {
            return outwardUpper(this.radius);
        }

        private float outwardMinY() {
            return outwardLower(this.minY);
        }

        private float outwardMaxY() {
            return outwardUpper(this.maxY);
        }

        private float outwardMinZ() {
            return outwardLower(-this.radius);
        }

        private float outwardMaxZ() {
            return outwardUpper(this.radius);
        }

        private static float outwardLower(double value) {
            float rounded = (float) value;
            return (double) rounded > value ? Math.nextDown(rounded) : rounded;
        }

        private static float outwardUpper(double value) {
            float rounded = (float) value;
            return (double) rounded < value ? Math.nextUp(rounded) : rounded;
        }
    }

    private static final class Bounds {
        private final BlockPos pos;
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private BlockPos pos() {
            return this.pos;
        }

        private void include(Vector3f point) {
            this.minX = Math.min(this.minX, point.x);
            this.minY = Math.min(this.minY, point.y);
            this.minZ = Math.min(this.minZ, point.z);
            this.maxX = Math.max(this.maxX, point.x);
            this.maxY = Math.max(this.maxY, point.y);
            this.maxZ = Math.max(this.maxZ, point.z);
        }

        private void includeBox(Matrix4f matrix, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ) {
            for (float x : new float[]{minX, maxX}) {
                for (float y : new float[]{minY, maxY}) {
                    for (float z : new float[]{minZ, maxZ}) {
                        include(matrix.transformPosition(new Vector3f(x, y, z)));
                    }
                }
            }
        }

        private void expand(double amount) {
            if (!Double.isFinite(this.minX)) {
                return;
            }
            this.minX = Math.nextDown(this.minX - amount);
            this.minY = Math.nextDown(this.minY - amount);
            this.minZ = Math.nextDown(this.minZ - amount);
            this.maxX = Math.nextUp(this.maxX + amount);
            this.maxY = Math.nextUp(this.maxY + amount);
            this.maxZ = Math.nextUp(this.maxZ + amount);
        }

        private void include(Bounds other) {
            if (!Double.isFinite(other.minX)) {
                return;
            }
            this.minX = Math.min(this.minX, other.minX);
            this.minY = Math.min(this.minY, other.minY);
            this.minZ = Math.min(this.minZ, other.minZ);
            this.maxX = Math.max(this.maxX, other.maxX);
            this.maxY = Math.max(this.maxY, other.maxY);
            this.maxZ = Math.max(this.maxZ, other.maxZ);
        }

        private AABB toAabb() {
            if (!Double.isFinite(this.minX)) {
                return zeroVolume(this.pos);
            }
            return new AABB(this.pos.getX() + this.minX, this.pos.getY() + this.minY,
                    this.pos.getZ() + this.minZ, this.pos.getX() + this.maxX,
                    this.pos.getY() + this.maxY, this.pos.getZ() + this.maxZ);
        }
    }

    private enum Axis {
        X(1.0F, 0.0F, 0.0F),
        Y(0.0F, 1.0F, 0.0F),
        Z(0.0F, 0.0F, 1.0F);

        private final float x;
        private final float y;
        private final float z;

        Axis(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Quaternionf quaternion(float degrees) {
            return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), this.x, this.y, this.z));
        }
    }
}
