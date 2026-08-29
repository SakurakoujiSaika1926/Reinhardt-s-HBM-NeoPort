package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.SnowglobeType;
import com.reinhardt.hbm.blockentity.SnowglobeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.math.Axis;

import java.util.EnumMap;
import java.util.Map;

public final class SnowglobeBlockEntityRenderer implements BlockEntityRenderer<SnowglobeBlockEntity> {
    private static final ModelResourceLocation SOCKET = model("snowglobe_socket");
    private static final ModelResourceLocation GLASS = model("snowglobe_glass");
    private static final Map<SnowglobeType, ModelResourceLocation> FEATURES = featureModels();
    private static final ResourceLocation SOCKET_TEXTURE = texture("snowglobe.png");
    private static final ResourceLocation GLASS_TEXTURE = texture("snowglobe_glass.png");
    private static final ResourceLocation FEATURES_TEXTURE = texture("snowglobe_features.png");

    public SnowglobeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOCKET);
        event.register(GLASS);
        FEATURES.values().forEach(event::register);
    }

    @Override
    public void render(SnowglobeBlockEntity globe, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = globe.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderSnowglobe(globe.type(), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static void renderItem(SnowglobeType type, BlockState state, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderSnowglobe(type, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderSnowglobe(SnowglobeType type, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        if (type != SnowglobeType.NONE) {
            renderPart(FEATURES.get(type), FEATURES_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        }

        Font font = Minecraft.getInstance().font;
        String label = type.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPart(ModelResourceLocation model, ResourceLocation texture, BlockState state,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel baked = MachineModelRenderer.model(model);
        MachineModelRenderer.renderUnculledUv(baked, poseStack, bufferSource, state, packedLight, packedOverlay,
                texture, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(SnowglobeBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    private static Map<SnowglobeType, ModelResourceLocation> featureModels() {
        Map<SnowglobeType, ModelResourceLocation> models = new EnumMap<>(SnowglobeType.class);
        for (SnowglobeType type : SnowglobeType.values()) {
            if (type != SnowglobeType.NONE) {
                models.put(type, model("snowglobe_" + type.modelGroup().toLowerCase()));
            }
        }
        return Map.copyOf(models);
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/trinkets/" + name);
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/models/trinkets/" + name);
    }
}
