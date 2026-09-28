package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.blockentity.BobbleheadBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.mojang.math.Axis;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.reinhardt.hbm.item.LegacyVariantItem;

/** Direct OBJ-group port of RenderBobble, including its old pose and head bobbing transforms. */
public final class BobbleheadBlockEntityRenderer implements LongRangeBlockEntityRenderer<BobbleheadBlockEntity> {
    private static final String[] GROUPS = {
            "Body", "Body17", "Drillgon", "Fumo", "FumoHead", "Head", "Head17", "Horn",
            "LA", "LA17", "LL", "LL17", "PeepHat", "PeepTail", "Pellet", "PelletShine",
            "RA", "RA17", "RL", "RL17", "Socket", "Fluoro", "Glow"
    };
    private static final Map<String, ModelResourceLocation> MODELS = models();
    private static final Map<String, ModelResourceLocation> ACCESSORY_MODELS = accessoryModels();
    private static final List<ModelResourceLocation> ALL_MODELS = allModels();
    private static final ClientRenderSnapshotInstances<BobbleheadBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile GeometryGeneration geometryGeneration;
    private static final ResourceLocation SOCKET = ReinhardtsHBM.id("textures/models/trinkets/socket.png");
    private static final ResourceLocation GLOW = ReinhardtsHBM.id("textures/models/trinkets/glow.png");
    private static final ResourceLocation MELLOW_LAMP = ReinhardtsHBM.id("textures/models/trinkets/accessory/fluorescent_lamp.png");
    private static final ResourceLocation HEV_HELMET = ReinhardtsHBM.id("textures/models/trinkets/accessory/hev_helmet.png");
    private static final ResourceLocation HAT = ReinhardtsHBM.id("textures/models/trinkets/accessory/hat.png");
    private static final ResourceLocation REVOLVER = ReinhardtsHBM.id("textures/models/trinkets/accessory/revolver.png");
    private static final ResourceLocation SHIMMER_AXE = ReinhardtsHBM.id("textures/models/trinkets/accessory/shimmer_axe.png");
    private static final ResourceLocation MINI_NUKE = ReinhardtsHBM.id("textures/models/trinkets/accessory/fatman_mininuke.png");
    private static final ResourceLocation SACRED_DRAGON = ReinhardtsHBM.id("textures/models/trinkets/accessory/sacred_dragon.png");

    public BobbleheadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        BobbleheadBlockEntity.setClientSnapshotUpdater(BobbleheadBlockEntityRenderer::updateRenderSnapshot);
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = captureGeometryGeneration();
        BobbleheadBlockEntity.setClientSnapshotUpdater(BobbleheadBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(BobbleheadBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.values().forEach(event::register);
        ACCESSORY_MODELS.values().forEach(event::register);
    }

    @Override
    public void render(BobbleheadBlockEntity bobble, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BobbleheadBlockEntity.RenderSnapshot snapshot = bobble.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        float time = System.currentTimeMillis() * 0.005F;
        float dynamicX = (float) Math.sin(time);
        float dynamicZ = (float) Math.sin(time + Math.PI * 0.5D);
        switch (snapshot.type()) {
            case NONE -> renderNoneWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case STRENGTH -> renderStrengthWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case PERCEPTION -> renderPerceptionWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case ENDURANCE -> renderEnduranceWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case CHARISMA -> renderCharismaWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case INTELLIGENCE -> renderIntelligenceWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case AGILITY -> renderAgilityWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case LUCK -> renderLuckWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case BOB -> renderBobWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case FRIZZLE -> renderFrizzleWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case PU238 -> renderPu238World(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case VT -> renderVtWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case DOC -> renderDocWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case BLUEHAT -> renderBluehatWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case PHEO -> renderPheoWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case ADAM29 -> renderAdam29World(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case UFFR -> renderUffrWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case VAER -> renderVaerWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case NOS -> renderNosWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case DRILLGON -> renderDrillgonWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case CIRNO -> renderCirnoWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case MICROWAVE -> renderMicrowaveWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case PEEP -> renderPeepWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case MELLOW -> renderMellowWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
            case ABEL -> renderAbelWorld(generation, state, poseStack, bufferSource,
                    packedLight, packedOverlay, dynamicX, dynamicZ);
        }
    }

    static void renderNoneItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderNoneContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderStrengthItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderStrengthContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderPerceptionItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderPerceptionContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderEnduranceItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderEnduranceContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderCharismaItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderCharismaContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderIntelligenceItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderIntelligenceContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderAgilityItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderAgilityContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderLuckItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderLuckContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderBobItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderBobContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderFrizzleItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderFrizzleContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderPu238Item(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderPu238Content(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderVtItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderVtContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderDocItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderDocContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderBluehatItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderBluehatContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderPheoItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderPheoContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderAdam29Item(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderAdam29Content(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderUffrItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderUffrContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderVaerItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderVaerContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderNosItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderNosContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderDrillgonItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderDrillgonContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderCirnoItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderCirnoContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderMicrowaveItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderMicrowaveContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderPeepItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderPeepContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderMellowItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderMellowContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    static void renderAbelItem(BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float time = System.currentTimeMillis() * 0.005F;
        renderAbelContent(null, state, poseStack, bufferSource, packedLight, packedOverlay,
                (float) Math.sin(time), (float) Math.sin(time + Math.PI * 0.5D));
    }

    private static void renderNoneWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderNoneContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderStrengthWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderStrengthContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderPerceptionWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderPerceptionContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderEnduranceWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderEnduranceContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderCharismaWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderCharismaContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderIntelligenceWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderIntelligenceContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderAgilityWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderAgilityContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderLuckWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderLuckContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderBobWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderBobContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderFrizzleWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderFrizzleContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderPu238World(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderPu238Content(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderVtWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderVtContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderDocWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderDocContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderBluehatWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderBluehatContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderPheoWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderPheoContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderAdam29World(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderAdam29Content(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderUffrWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderUffrContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderVaerWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderVaerContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderNosWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderNosContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderDrillgonWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderDrillgonContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderCirnoWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderCirnoContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderMicrowaveWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderMicrowaveContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderPeepWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderPeepContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderMellowWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderMellowContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderAbelWorld(GeometryGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderAbelContent(generation, state, poseStack, bufferSource, packedLight, packedOverlay,
                dynamicX, dynamicZ);
        poseStack.popPose();
    }

    private static void renderNoneContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderNoneLayer(generation, BobbleheadType.NONE.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.NONE.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderNoneLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderStrengthContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderStrengthLayer(generation, BobbleheadType.STRENGTH.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.STRENGTH.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderStrengthLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(5.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(25.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPerceptionContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPerceptionLayer(generation, BobbleheadType.PERCEPTION.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.PERCEPTION.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPerceptionLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-15.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderEnduranceContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderEnduranceLayer(generation, BobbleheadType.ENDURANCE.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.ENDURANCE.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderEnduranceLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderCharismaContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderCharismaLayer(generation, BobbleheadType.CHARISMA.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.CHARISMA.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderCharismaLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(5.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderIntelligenceContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderIntelligenceLayer(generation, BobbleheadType.INTELLIGENCE.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.INTELLIGENCE.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderIntelligenceLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(170.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderAgilityContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderAgilityLayer(generation, BobbleheadType.AGILITY.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.AGILITY.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderAgilityLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderLuckContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderLuckLayer(generation, BobbleheadType.LUCK.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.LUCK.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderLuckLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(135.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-135.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderBobContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderBobLayer(generation, BobbleheadType.BOB.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.6875F, 0.625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(0.125F, 0.125F, 0.125F);
        poseStack.translate(-6.0F, 0.0F, 0.0F);
        for (int i = -1; i <= 1; i++) {
            poseStack.translate(3.0F, 0.0F, 0.0F);
            renderAccessory(generation, "fatman", MINI_NUKE, state, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.25F, 0.3125F, -0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.1F, 0.1F, 0.1F);
        renderAccessory(generation, "sacred_dragon", SACRED_DRAGON, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.BOB.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderBobLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head17", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderFrizzleContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderFrizzleLayer(generation, BobbleheadType.FRIZZLE.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        poseStack.pushPose();
        poseStack.translate(0.8F, 1.6F, 0.4F);
        poseStack.scale(0.125F, 0.125F, 0.125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
        renderAccessory(generation, "revolver", REVOLVER, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.3F, 1.4F, -0.2F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderLegacyVariantItem(generation, "weapon_mod_special", "doubloons", poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.FRIZZLE.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderFrizzleLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(2.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-2.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPu238Content(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPart(generation, "Pellet", BobbleheadType.PU238.texture(), state, poseStack,
                bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        float alpha = 0.1F + (float) Math.sin(System.currentTimeMillis() * 0.001D) * 0.05F;
        MachineModelRenderer.renderUnculledTintedLightning(model(generation, MODELS.get("PelletShine")), poseStack,
                bufferSource, state, packedOverlay, (Math.round(alpha * 255.0F) << 24) | 0x00FFFF00);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.PU238.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderVtContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderVtLayer(generation, BobbleheadType.VT.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.VT.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFF0000,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderVtLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-2.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, "Horn", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderDocContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderDocLayer(generation, BobbleheadType.DOC.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.DOC.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderDocLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderBluehatContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderBluehatLayer(generation, BobbleheadType.BLUEHAT.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.875F, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-160.0F));
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderAccessory(generation, "hev", HEV_HELMET, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.BLUEHAT.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderBluehatLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPheoContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPheoLayer(generation, BobbleheadType.PHEO.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F, 0.45F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);
        renderAccessory(generation, "shimmer_axe", SHIMMER_AXE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.PHEO.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPheoLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderAdam29Content(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderAdam29Layer(generation, BobbleheadType.ADAM29.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        poseStack.pushPose();
        poseStack.translate(0.4F, 1.15F, 0.4F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderLegacyItem(generation, "can_redbomb", poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.ADAM29.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderAdam29Layer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(60.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderUffrContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPart(generation, "Fumo", BobbleheadType.UFFR.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.translate(0.0F, -0.75F, 0.0F);
        renderPart(generation, "FumoHead", BobbleheadType.UFFR.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.UFFR.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderVaerContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderVaerLayer(generation, BobbleheadType.VAER.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.VAER.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderVaerLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.25F, 1.9F, 0.075F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-60.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderLegacyItem(generation, "cigarette", poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderNosContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderNosLayer(generation, BobbleheadType.NOS.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.NOS.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderNosLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.095F, 0.095F, 0.095F);
        renderAccessory(generation, "hat", HAT, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderDrillgonContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPart(generation, "Drillgon", BobbleheadType.DRILLGON.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.DRILLGON.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderCirnoContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderCirnoLayer(generation, BobbleheadType.CIRNO.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.CIRNO.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderCirnoLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderMicrowaveContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderMicrowaveLayer(generation, BobbleheadType.MICROWAVE.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.MICROWAVE.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderMicrowaveLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPeepContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderPeepLayer(generation, BobbleheadType.PEEP.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.PEEP.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPeepLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        renderPart(generation, "PeepTail", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(1.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(1.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, "PeepHat", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderMellowContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderMellowLayer(generation, BobbleheadType.MELLOW.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderMellowLayer(generation, BobbleheadType.MELLOW.glowTexture(), state, poseStack,
                bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, dynamicX, dynamicZ);
        renderGlowPart(generation, "Fluoro", MELLOW_LAMP, state, poseStack,
                bufferSource, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        renderGlowPart(generation, "Glow", GLOW, state, poseStack,
                bufferSource, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.MELLOW.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderMellowLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(3.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(2.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-5.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderAbelContent(GeometryGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             float dynamicX, float dynamicZ) {
        renderAbelLayer(generation, BobbleheadType.ABEL.texture(), state, poseStack,
                bufferSource, packedLight, packedOverlay, dynamicX, dynamicZ);
        renderAbelLayer(generation, BobbleheadType.ABEL.glowTexture(), state, poseStack,
                bufferSource, LightTexture.FULL_BRIGHT, packedOverlay, dynamicX, dynamicZ);
        renderPart(generation, "Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = BobbleheadType.ABEL.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderAbelLayer(GeometryGeneration generation, ResourceLocation texture,
                                            BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                            float dynamicX, float dynamicZ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, 0.125F);
        renderPart(generation, "LL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.0F, -0.125F);
        renderPart(generation, "RL", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(80.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -1.625F, 0.25F);
        renderPart(generation, "LA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-80.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, -1.625F, -0.25F);
        renderPart(generation, "RA", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(generation, "Body", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(dynamicX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(dynamicZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart(generation, "Head", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPart(GeometryGeneration generation, String group,
                                   ResourceLocation texture, BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculledUv(model(generation, MODELS.get(group)), poseStack,
                bufferSource, state, packedLight, packedOverlay, texture, 0.0F, 0.0F);
    }

    private static void renderGlowPart(GeometryGeneration generation, String group,
                                       ResourceLocation texture, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedOverlay,
                                        float red, float green, float blue, float alpha) {
        MachineModelRenderer.renderUnculledUvEyes(model(generation, MODELS.get(group)), poseStack, bufferSource,
                state, packedOverlay, texture, red, green, blue, alpha);
    }

    private static void renderAccessory(GeometryGeneration generation, String name,
                                        ResourceLocation texture, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculledUv(model(generation, ACCESSORY_MODELS.get(name)), poseStack,
                bufferSource, state, packedLight, packedOverlay, texture, 0.0F, 0.0F);
    }

    private static void renderLegacyItem(GeometryGeneration generation, String id,
                                         PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (generation != null) {
            renderCapturedItem(generation.items().get(id), poseStack, bufferSource, packedOverlay);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
        }
    }

    private static void renderLegacyVariantItem(GeometryGeneration generation, String id, String variant,
                                                PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedOverlay) {
        if (generation != null) {
            renderCapturedItem(generation.items().get(id + "/" + variant), poseStack, bufferSource, packedOverlay);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            ItemStack stack = LegacyVariantItem.stackFor(item, variant);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
        }
    }

    private static void renderCapturedItem(CapturedItem captured, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedOverlay) {
        if (captured != null) {
            Minecraft.getInstance().getItemRenderer().render(captured.stack(), ItemDisplayContext.FIXED,
                    false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay,
                    captured.model());
        }
    }

    @Override
    public AABB getRenderBoundingBox(BobbleheadBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(BobbleheadBlockEntity bobble) {
        CLIENT_INSTANCES.track(bobble);
        GeometryGeneration generation = geometryGeneration();
        BlockState state = bobble.getBlockState();
        BobbleheadType type = bobble.type();
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        switch (type) {
            case NONE -> renderNoneWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case STRENGTH -> renderStrengthWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case PERCEPTION -> renderPerceptionWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case ENDURANCE -> renderEnduranceWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case CHARISMA -> renderCharismaWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case INTELLIGENCE -> renderIntelligenceWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case AGILITY -> renderAgilityWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case LUCK -> renderLuckWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case BOB -> renderBobWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case FRIZZLE -> renderFrizzleWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case PU238 -> renderPu238World(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case VT -> renderVtWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case DOC -> renderDocWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case BLUEHAT -> renderBluehatWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case PHEO -> renderPheoWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case ADAM29 -> renderAdam29World(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case UFFR -> renderUffrWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case VAER -> renderVaerWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case NOS -> renderNosWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case DRILLGON -> renderDrillgonWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case CIRNO -> renderCirnoWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case MICROWAVE -> renderMicrowaveWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case PEEP -> renderPeepWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case MELLOW -> renderMellowWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
            case ABEL -> renderAbelWorld(generation, state, new PoseStack(), capture,
                    LightTexture.FULL_BRIGHT, 0, 0.0F, 0.0F);
        }
        AABB neutralBounds = capture.bounds(bobble.getBlockPos());
        double motionMargin = switch (type) {
            case PU238 -> 0.0D;
            case DRILLGON -> 0.0D;
            case NONE -> noneMotionMargin(state, capture);
            case STRENGTH -> strengthMotionMargin(state, capture);
            case PERCEPTION -> perceptionMotionMargin(state, capture);
            case ENDURANCE -> enduranceMotionMargin(state, capture);
            case CHARISMA -> charismaMotionMargin(state, capture);
            case INTELLIGENCE -> intelligenceMotionMargin(state, capture);
            case AGILITY -> agilityMotionMargin(state, capture);
            case LUCK -> luckMotionMargin(state, capture);
            case BOB -> bobMotionMargin(state, capture);
            case FRIZZLE -> frizzleMotionMargin(state, capture);
            case VT -> vtMotionMargin(state, capture);
            case DOC -> docMotionMargin(state, capture);
            case BLUEHAT -> bluehatMotionMargin(state, capture);
            case PHEO -> pheoMotionMargin(state, capture);
            case ADAM29 -> adam29MotionMargin(state, capture);
            case UFFR -> uffrMotionMargin(state, capture);
            case VAER -> vaerMotionMargin(state, capture);
            case NOS -> nosMotionMargin(state, capture);
            case CIRNO -> cirnoMotionMargin(state, capture);
            case MICROWAVE -> microwaveMotionMargin(state, capture);
            case PEEP -> peepMotionMargin(state, capture);
            case MELLOW -> mellowMotionMargin(state, capture);
            case ABEL -> abelMotionMargin(state, capture);
        };
        AABB bounds = motionMargin == 0.0D ? neutralBounds : new AABB(
                neutralBounds.minX - motionMargin, neutralBounds.minY - motionMargin,
                neutralBounds.minZ - motionMargin, neutralBounds.maxX + motionMargin,
                neutralBounds.maxY + motionMargin, neutralBounds.maxZ + motionMargin);
        bobble.publishClientRenderSnapshot(new BobbleheadBlockEntity.RenderSnapshot(
                state, type, generation, bounds, BobbleheadBlockEntity.clientSnapshotRevision()));
    }

    private static double noneMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double strengthMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double perceptionMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double enduranceMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(45.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double charismaMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(45.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double intelligenceMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double agilityMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double luckMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double bobMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double frizzleMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double vtMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double docMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double bluehatMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double pheoMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double adam29MotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double uffrMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.translate(0.0F, 0.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double vaerMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double nosMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double cirnoMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double microwaveMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double peepMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double mellowMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static double abelMotionMargin(BlockState state, GeometryCaptureBufferSource capture) {
        PoseStack pivotPose = new PoseStack();
        pivotPose.translate(0.5F, 0.0F, 0.5F);
        pivotPose.scale(0.25F, 0.25F, 0.25F);
        pivotPose.mulPose(Axis.YN.rotationDegrees(
                22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        pivotPose.mulPose(Axis.YP.rotationDegrees(0.0F));
        pivotPose.translate(0.0F, 1.75F, 0.0F);
        Vector3f pivot = pivotPose.last().pose().transformPosition(new Vector3f());
        double radius = capture.maximumDistance(pivot);
        return Math.nextUp(4.0D * radius * Math.sin(Math.toRadians(0.5D)));
    }

    private static GeometryGeneration geometryGeneration() {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (BobbleheadBlockEntityRenderer.class) {
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
        BakedModelGeneration models = BakedModelGeneration.capture(ALL_MODELS);
        Map<String, CapturedItem> items = new LinkedHashMap<>();
        captureItem(items, "cigarette", new ItemStack(
                BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("cigarette"))));
        captureItem(items, "can_redbomb", new ItemStack(
                BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("can_redbomb"))));
        Item variantItem = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("weapon_mod_special"));
        if (variantItem != Items.AIR) {
            captureItem(items, "weapon_mod_special/doubloons",
                    LegacyVariantItem.stackFor(variantItem, "doubloons"));
        }
        return new GeometryGeneration(models, Map.copyOf(items));
    }

    private static void captureItem(Map<String, CapturedItem> target, String key, ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.AIR)) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        if (!model.isCustomRenderer()) {
            target.put(key, new CapturedItem(stack.copyWithCount(1), model));
        }
    }

    private static BakedModel model(GeometryGeneration generation, ModelResourceLocation location) {
        return generation == null ? MachineModelRenderer.model(location) : generation.models().model(location);
    }

    private static List<ModelResourceLocation> allModels() {
        ArrayList<ModelResourceLocation> result = new ArrayList<>(MODELS.size() + ACCESSORY_MODELS.size());
        result.addAll(MODELS.values());
        result.addAll(ACCESSORY_MODELS.values());
        return List.copyOf(result);
    }

    private static Map<String, ModelResourceLocation> models() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        for (String group : GROUPS) {
            models.put(group, MachineModelRenderer.standalone("block/bobble_" + group.toLowerCase()));
        }
        return Map.copyOf(models);
    }

    private static Map<String, ModelResourceLocation> accessoryModels() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        for (String name : new String[]{"hev", "hat", "revolver", "shimmer_axe", "fatman", "sacred_dragon"}) {
            models.put(name, MachineModelRenderer.standalone("block/bobble_accessory_" + name));
        }
        return Map.copyOf(models);
    }

    private record GeometryGeneration(BakedModelGeneration models, Map<String, CapturedItem> items) {
    }

    private record CapturedItem(ItemStack stack, BakedModel model) {
        private CapturedItem {
            stack = stack.copy();
        }
    }

}
