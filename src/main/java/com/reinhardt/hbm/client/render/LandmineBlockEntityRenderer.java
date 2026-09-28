package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.LandmineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class LandmineBlockEntityRenderer implements LongRangeBlockEntityRenderer<LandmineBlockEntity> {
    private static final ModelResourceLocation AP_GRASS = model("block/mine_ap_grass_world");
    private static final ModelResourceLocation AP_DESERT = model("block/mine_ap_desert_world");
    private static final ModelResourceLocation AP_SNOW = model("block/mine_ap_snow_world");
    private static final ModelResourceLocation AP_STONE = model("block/mine_ap_stone_world");
    private static final ModelResourceLocation HE = model("block/mine_he_world");
    private static final ModelResourceLocation SHRAPNEL = model("block/mine_shrap_world");
    private static final ModelResourceLocation NUCLEAR = model("block/mine_fat_world");
    private static final ModelResourceLocation NAVAL = model("block/mine_naval_world");

    public LandmineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(AP_GRASS);
        event.register(AP_DESERT);
        event.register(AP_SNOW);
        event.register(AP_STONE);
        event.register(HE);
        event.register(SHRAPNEL);
        event.register(NUCLEAR);
        event.register(NAVAL);
    }

    @Override
    public void render(LandmineBlockEntity mine, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LandmineBlockEntity.RenderSnapshot snapshot = mine.renderSnapshot();
        BlockState state = snapshot.blockState();

        switch (snapshot.type()) {
            case AP -> renderApMine(snapshot.apSurface(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case HE -> renderHeMine(poseStack, bufferSource, state, packedLight, packedOverlay);
            case SHRAPNEL -> renderShrapnelMine(poseStack, bufferSource, state, packedLight, packedOverlay);
            case NUCLEAR -> renderNuclearMine(poseStack, bufferSource, state, packedLight, packedOverlay);
            case NAVAL -> renderNavalMine(poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderApMine(LandmineBlockEntity.ApSurface surface, PoseStack poseStack,
                                     MultiBufferSource bufferSource, BlockState state,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.375F, 0.375F, 0.375F);
        poseStack.translate(0.0D, -0.21875D, 0.0D);
        render(apModel(surface), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHeMine(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        render(HE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderShrapnelMine(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                           int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.375F, 0.375F, 0.375F);
        poseStack.translate(0.0D, -0.21875D, 0.0D);
        render(SHRAPNEL, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNuclearMine(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                          int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.25F, 0.25F, 0.25F);
        render(NUCLEAR, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNavalMine(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, 0.5D, 0.0D);
        render(NAVAL, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LandmineBlockEntity mine) {
        var pos = mine.getBlockPos();
        // Face-used OBJ vertices after each branch's complete scale,
        // translation, and fixed rotation.  AP variants share one mesh.
        return switch (mine.renderSnapshot().type()) {
            case AP -> new AABB(
                    pos.getX() + 0.2890625D, pos.getY() - 0.12890625D, pos.getZ() + 0.2890625D,
                    pos.getX() + 0.7109375D, pos.getY() + 0.05859375D, pos.getZ() + 0.7109375D);
            case SHRAPNEL -> new AABB(
                    pos.getX() + 0.2890625D, pos.getY() - 0.12890625D, pos.getZ() + 0.2890625D,
                    pos.getX() + 0.7109375D, pos.getY() + 0.05859375D, pos.getZ() + 0.7109375D);
            case HE -> new AABB(
                    pos.getX() + 0.187922D, pos.getY(), pos.getZ() + 0.25D,
                    pos.getX() + 0.75D, pos.getY() + 0.15625D, pos.getZ() + 0.75D);
            case NUCLEAR -> new AABB(
                    pos.getX() + 0.375D, pos.getY(), pos.getZ() + 0.191817D,
                    pos.getX() + 0.625D, pos.getY() + 0.3925D, pos.getZ() + 0.775D);
            case NAVAL -> new AABB(
                    pos.getX() - 0.712592D, pos.getY() - 0.8125D, pos.getZ() - 0.625D,
                    pos.getX() + 1.712592D, pos.getY() + 1.712592D, pos.getZ() + 1.625D);
        };
    }

    private static ModelResourceLocation apModel(LandmineBlockEntity.ApSurface surface) {
        return switch (surface) {
            case GRASS -> AP_GRASS;
            case DESERT -> AP_DESERT;
            case SNOW -> AP_SNOW;
            case STONE -> AP_STONE;
        };
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                               BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                state, packedLight, packedOverlay);
    }
}
