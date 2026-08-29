package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LanternBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct port of RenderLantern, including separate opaque and light OBJ groups. */
public final class LanternBlockEntityRenderer implements BlockEntityRenderer<LanternBlockEntity> {
    static final ModelResourceLocation BODY = model("block/trinkets/lantern");
    static final ModelResourceLocation LIGHT = model("block/trinkets/lantern_light");
    private static final net.minecraft.resources.ResourceLocation BODY_TEXTURE = texture("lantern.png");

    public LanternBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(LIGHT);
    }

    @Override
    public void render(LanternBlockEntity lantern, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderAssembly(lantern.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay,
                BODY_TEXTURE, ordinaryLightColor());
        poseStack.popPose();
    }

    static void renderAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                               int packedLight, int packedOverlay,
                               net.minecraft.resources.ResourceLocation texture, int lightColor) {
        BakedModel body = MachineModelRenderer.model(BODY);
        BakedModel light = MachineModelRenderer.model(LIGHT);
        MachineModelRenderer.renderUnculledUv(body, poseStack, bufferSource, state,
                packedLight, packedOverlay, texture, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculledTintedLightning(light, poseStack, bufferSource, state,
                packedOverlay, lightColor);
    }

    static void renderItem(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                           int packedLight, int packedOverlay) {
        renderAssembly(state, poseStack, bufferSource, packedLight, packedOverlay,
                BODY_TEXTURE, ordinaryLightColor());
    }

    private static int ordinaryLightColor() {
        float wave = (float) (Math.sin(System.currentTimeMillis() / 200.0D) / 2.0D + 0.5D);
        float multiplier = 0.9F + wave * 0.1F;
        int redGreen = Math.round(255.0F * multiplier);
        int blue = Math.round(255.0F * 0.7F * multiplier);
        return 0xFF000000 | (redGreen << 16) | (redGreen << 8) | blue;
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone(name);
    }

    private static net.minecraft.resources.ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/models/trinkets/" + name);
    }

    @Override
    public AABB getRenderBoundingBox(LanternBlockEntity lantern) {
        return new AABB(lantern.getBlockPos()).expandTowards(0.0D, 5.0D, 0.0D);
    }
}
