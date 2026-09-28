package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Renders the selected concrete from the bottom upward while the frame cures. */
public final class RebarBlockEntityRenderer implements LongRangeBlockEntityRenderer<RebarBlockEntity> {
    public RebarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        RebarBlockEntity.setClientRenderSnapshotUpdater(RebarBlockEntityRenderer::updateRenderSnapshot);
    }

    static void invalidateRenderSnapshotsAfterBake() {
        RebarBlockEntity.setClientRenderSnapshotUpdater(RebarBlockEntityRenderer::updateRenderSnapshot);
    }

    @Override
    public void render(RebarBlockEntity rebar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float fill = Math.min(1.0F, rebar.progress() / (float) RebarBlockEntity.CONCRETE_REQUIRED);
        if (fill <= 0.0F) return;
        RebarBlockEntity.RenderSnapshot snapshot = rebar.renderSnapshot();
        poseStack.pushPose();
        poseStack.scale(0.998F, fill, 0.998F);
        BakedBlockGeometrySnapshot.render(snapshot.modelToken(), poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RebarBlockEntity rebar) {
        return rebar.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(RebarBlockEntity rebar) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> updateRenderSnapshot(rebar));
            return;
        }
        RebarBlockEntity.RenderSnapshot previous = rebar.renderSnapshot();
        long revision = rebar.clientRenderSnapshotRevision();
        if (previous.revision() == revision && previous.targetState().equals(rebar.targetState())) {
            rebar.publishRenderSnapshot(previous);
            return;
        }

        PoseStack empty = new PoseStack();
        empty.scale(0.998F, 0.0F, 0.998F);
        PoseStack full = new PoseStack();
        full.scale(0.998F, 1.0F, 0.998F);
        BakedBlockGeometrySnapshot.Result geometry = BakedBlockGeometrySnapshot.build(
                rebar.targetState(), List.of(empty, full), rebar.getBlockPos());
        rebar.publishRenderSnapshot(new RebarBlockEntity.RenderSnapshot(
                geometry.state(), geometry.modelToken(), geometry.bounds(), revision));
    }
}
