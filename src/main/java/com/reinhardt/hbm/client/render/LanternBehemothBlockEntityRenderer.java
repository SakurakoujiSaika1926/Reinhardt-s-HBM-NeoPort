package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LanternBehemothBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.math.Axis;

/** Direct port of RenderLanternBehemoth's broken tilt and red/green beacon pulse. */
public final class LanternBehemothBlockEntityRenderer implements LongRangeBlockEntityRenderer<LanternBehemothBlockEntity> {
    private static final net.minecraft.resources.ResourceLocation RUSTY_TEXTURE =
            ReinhardtsHBM.id("textures/models/trinkets/lantern_rusty.png");

    public LanternBehemothBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        // The ordinary renderer owns and registers the shared OBJ groups.
    }

    @Override
    public void render(LanternBehemothBlockEntity lantern, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LanternBehemothBlockEntity.RenderSnapshot snapshot = lantern.renderSnapshot();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (snapshot.broken()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
        }
        int color = snapshot.broken() ? pulse(0xFF0000, 0.0F, 1.0F) : pulse(0x00FF00, 0.5F, 0.5F);
        LanternBlockEntityRenderer.renderAssembly(snapshot.blockState(), poseStack, bufferSource,
                packedLight, packedOverlay, RUSTY_TEXTURE, color);
        poseStack.popPose();
    }

    private static int pulse(int color, float base, float amplitude) {
        float wave = (float) (Math.sin(System.currentTimeMillis() / 200.0D) / 2.0D + 0.5D);
        float multiplier = base + wave * amplitude;
        int red = Math.round(((color >> 16) & 0xFF) * multiplier);
        int green = Math.round(((color >> 8) & 0xFF) * multiplier);
        int blue = Math.round((color & 0xFF) * multiplier);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @Override
    public AABB getRenderBoundingBox(LanternBehemothBlockEntity lantern) {
        return lantern.renderSnapshot().bounds();
    }
}
