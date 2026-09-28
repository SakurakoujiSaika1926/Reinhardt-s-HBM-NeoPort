package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PlushieBlock;
import com.reinhardt.hbm.block.PlushieType;
import com.reinhardt.hbm.blockentity.PlushieBlockEntity;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlushieBlockEntityRenderer implements LongRangeBlockEntityRenderer<PlushieBlockEntity> {
    /*
     * f(s)=1-.025*s*sin(s), s in [0,11].  The roots of
     * sin(s)+s*cos(s)=0 partition the legal interval; evaluating those roots
     * and both endpoints through the renderer's float operations gives the
     * global values 0x3f4d553c and 0x3fa3331d.  The stored values are exactly
     * the adjacent IEEE-754 floats outside that closed range.
     */
    private static final float MIN_SQUISH_SCALE = Float.intBitsToFloat(0x3F4D553B);
    private static final float MAX_SQUISH_SCALE = Float.intBitsToFloat(0x3FA3331E);
    private static final ModelResourceLocation YOMI = model("yomi", "Plane");
    private static final ModelResourceLocation HUNDUN = model("hundun", "goober_posed");
    private static final ModelResourceLocation DERG = model("derg", "Derg");
    private static final ModelResourceLocation DERG_BLEP = model("derg_blep", "Blep");
    private static final ModelResourceLocation DERG_FACE = model("derg_colonthree", "ColonThree");
    private static final Map<String, ModelResourceLocation> HORSE = horseModels();
    private static final ModelResourceLocation NO9_HELMET = no9Model("Helmet");
    private static final ModelResourceLocation NO9_INSIGNIA = no9Model("Insignia");
    private static final List<ModelResourceLocation> ALL_MODELS = allModels();
    private static final ClientRenderSnapshotInstances<PlushieBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile GeometryGeneration geometryGeneration;

    public PlushieBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        PlushieBlockEntity.setClientSnapshotUpdater(PlushieBlockEntityRenderer::updateRenderSnapshot);
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = captureGeometryGeneration();
        PlushieBlockEntity.setClientSnapshotUpdater(PlushieBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(PlushieBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(YOMI);
        event.register(HUNDUN);
        event.register(DERG);
        event.register(DERG_BLEP);
        event.register(DERG_FACE);
        HORSE.values().forEach(event::register);
        event.register(NO9_HELMET);
        event.register(NO9_INSIGNIA);
    }

    @Override
    public void render(PlushieBlockEntity plushie, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlushieBlockEntity.RenderSnapshot snapshot = plushie.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        float squish = plushie.squishTimer(partialTick);
        switch (snapshot.type()) {
            case YOMI -> renderYomiWorld(generation, state, squish, poseStack, bufferSource, packedLight, packedOverlay);
            case NUMBER_NINE -> renderNumberNineWorld(generation, state, squish, poseStack, bufferSource, packedLight, packedOverlay);
            case HUNDUN -> renderHundunWorld(generation, state, squish, poseStack, bufferSource, packedLight, packedOverlay);
            case DERG -> renderDergWorld(generation, state, squish, poseStack, bufferSource, packedLight, packedOverlay);
            case NONE -> renderNoneWorld(state, squish, poseStack);
        }
    }

    public static void renderYomiItem(BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        render(null, YOMI, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderNumberNineItem(BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderNumberNine(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderHundunItem(BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        render(null, HUNDUN, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderDergItem(BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        render(null, DERG, state, poseStack, bufferSource, packedLight, packedOverlay);
        render(null, DERG_FACE, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderNoneItem(BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    }

    private static void renderYomiWorld(GeometryGeneration generation, BlockState state, float squish,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (squish > 0.0F) {
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        render(generation, YOMI, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNumberNineWorld(GeometryGeneration generation, BlockState state, float squish,
                                              PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (squish > 0.0F) {
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        poseStack.scale(0.75F, 0.75F, 0.75F);
        renderNumberNine(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHundunWorld(GeometryGeneration generation, BlockState state, float squish,
                                          PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (squish > 0.0F) {
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        render(generation, HUNDUN, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderDergWorld(GeometryGeneration generation, BlockState state, float squish,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (squish > 0.0F) {
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        render(generation, DERG, state, poseStack, bufferSource, packedLight, packedOverlay);
        render(generation, squish > 0.0F ? DERG_BLEP : DERG_FACE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNoneWorld(BlockState state, float squish, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (squish > 0.0F) {
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        poseStack.popPose();
    }

    private static void renderNumberNine(GeometryGeneration generation, BlockState state,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        poseStack.translate(0.0F, -0.25F, 0.75F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        horsePart(generation, "body", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.125F, -0.4375F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(60.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -1.125F, 0.4375F);
        horsePart(generation, "tail", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.125F, 0.75F, -0.25F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(35.0F));
        poseStack.translate(-0.125F, -0.75F, 0.25F);
        horsePart(generation, "left_back_leg", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(-0.125F, 0.75F, -0.25F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-35.0F));
        poseStack.translate(0.125F, -0.75F, 0.25F);
        horsePart(generation, "right_back_leg", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.125F, 0.75F, 0.3125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(5.0F));
        poseStack.translate(-0.125F, -0.75F, -0.3125F);
        horsePart(generation, "left_front_leg", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(-0.125F, 0.75F, 0.3125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        poseStack.translate(0.125F, -0.75F, -0.3125F);
        horsePart(generation, "right_front_leg", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.125F, 0.375F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(60.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.125F, -0.375F);
        horsePart(generation, "head", state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.6875F);
        poseStack.scale(0.0703125F, 0.0703125F, 0.0703125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        render(generation, NO9_HELMET, state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        render(generation, NO9_INSIGNIA, state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.06F, 1.13F, -0.42F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(60.0F));
        if (generation == null) {
            Item cigarette = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("cigarette"));
            if (cigarette != Items.AIR) {
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(cigarette), ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
            }
        } else if (generation.cigarette() != null) {
            CapturedItem cigarette = generation.cigarette();
            Minecraft.getInstance().getItemRenderer().render(cigarette.stack(), ItemDisplayContext.FIXED,
                    false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, cigarette.model());
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void horsePart(GeometryGeneration generation, String part, BlockState state, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        render(generation, HORSE.get(part), state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void render(GeometryGeneration generation, ModelResourceLocation model, BlockState state,
                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel baked = generation == null
                ? MachineModelRenderer.model(model) : generation.models().model(model);
        MachineModelRenderer.render(baked, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(PlushieBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(PlushieBlockEntity plushie) {
        CLIENT_INSTANCES.track(plushie);
        GeometryGeneration generation = geometryGeneration();
        BlockState state = plushie.getBlockState();
        PlushieType type = plushie.type();
        AABB bounds = captureBounds(generation, plushie, state, type, MIN_SQUISH_SCALE, false)
                .minmax(captureBounds(generation, plushie, state, type, MAX_SQUISH_SCALE, false));
        if (type == PlushieType.DERG) {
            bounds = bounds.minmax(captureBounds(generation, plushie, state, type, MIN_SQUISH_SCALE, true))
                    .minmax(captureBounds(generation, plushie, state, type, MAX_SQUISH_SCALE, true));
        }
        plushie.publishClientRenderSnapshot(new PlushieBlockEntity.RenderSnapshot(
                state, type, generation, bounds, PlushieBlockEntity.clientSnapshotRevision()));
    }

    private static AABB captureBounds(GeometryGeneration generation, PlushieBlockEntity plushie,
                                      BlockState state, PlushieType type,
                                      float squishScale, boolean squishFace) {
        return switch (type) {
            case YOMI -> captureYomiBounds(generation, plushie, state, squishScale);
            case NUMBER_NINE -> captureNumberNineBounds(generation, plushie, state, squishScale);
            case HUNDUN -> captureHundunBounds(generation, plushie, state, squishScale);
            case DERG -> captureDergBounds(generation, plushie, state, squishScale, squishFace);
            case NONE -> captureNoneBounds(plushie, state, squishScale);
        };
    }

    private static AABB captureYomiBounds(GeometryGeneration generation, PlushieBlockEntity plushie,
                                          BlockState state, float squishScale) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        poseStack.scale(1.0F, squishScale, 1.0F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        render(generation, YOMI, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
        return capture.bounds(plushie.getBlockPos());
    }

    private static AABB captureNumberNineBounds(GeometryGeneration generation, PlushieBlockEntity plushie,
                                                BlockState state, float squishScale) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        poseStack.scale(1.0F, squishScale, 1.0F);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        renderNumberNine(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
        return capture.bounds(plushie.getBlockPos());
    }

    private static AABB captureHundunBounds(GeometryGeneration generation, PlushieBlockEntity plushie,
                                            BlockState state, float squishScale) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        poseStack.scale(1.0F, squishScale, 1.0F);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        render(generation, HUNDUN, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
        return capture.bounds(plushie.getBlockPos());
    }

    private static AABB captureDergBounds(GeometryGeneration generation, PlushieBlockEntity plushie,
                                          BlockState state, float squishScale, boolean squishFace) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        poseStack.scale(1.0F, squishScale, 1.0F);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        render(generation, DERG, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
        render(generation, squishFace ? DERG_BLEP : DERG_FACE,
                state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
        return capture.bounds(plushie.getBlockPos());
    }

    private static AABB captureNoneBounds(PlushieBlockEntity plushie, BlockState state, float squishScale) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        poseStack.scale(1.0F, squishScale, 1.0F);
        return new GeometryCaptureBufferSource().bounds(plushie.getBlockPos());
    }

    private static GeometryGeneration geometryGeneration() {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (PlushieBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = captureGeometryGeneration();
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static GeometryGeneration captureGeometryGeneration() {
        CapturedItem cigarette = null;
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("cigarette"));
        if (item != Items.AIR) {
            ItemStack stack = new ItemStack(item);
            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
            if (!model.isCustomRenderer()) {
                cigarette = new CapturedItem(stack, model);
            }
        }
        return new GeometryGeneration(BakedModelGeneration.capture(ALL_MODELS), cigarette);
    }

    private static List<ModelResourceLocation> allModels() {
        ArrayList<ModelResourceLocation> result = new ArrayList<>();
        result.add(YOMI);
        result.add(HUNDUN);
        result.add(DERG);
        result.add(DERG_BLEP);
        result.add(DERG_FACE);
        result.addAll(HORSE.values());
        result.add(NO9_HELMET);
        result.add(NO9_INSIGNIA);
        return List.copyOf(result);
    }

    private static Map<String, ModelResourceLocation> horseModels() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        models.put("body", model("horse_body", "Body"));
        models.put("head", model("horse_head", "Head", "Mane", "NoseFemale"));
        models.put("left_front_leg", model("horse_left_front_leg", "LeftFrontLeg"));
        models.put("right_front_leg", model("horse_right_front_leg", "RightFrontLeg"));
        models.put("left_back_leg", model("horse_left_back_leg", "LeftBackLeg"));
        models.put("right_back_leg", model("horse_right_back_leg", "RightBackLeg"));
        models.put("tail", model("horse_tail", "Tail"));
        return Map.copyOf(models);
    }

    private static ModelResourceLocation model(String name, String... groups) {
        return MachineModelRenderer.standalone("block/trinkets/" + name);
    }

    private static ModelResourceLocation no9Model(String group) {
        return MachineModelRenderer.standalone("block/trinkets/no9_" + group.toLowerCase());
    }

    private record GeometryGeneration(BakedModelGeneration models, CapturedItem cigarette) {
    }

    private record CapturedItem(ItemStack stack, BakedModel model) {
        private CapturedItem {
            stack = stack.copy();
        }
    }
}
