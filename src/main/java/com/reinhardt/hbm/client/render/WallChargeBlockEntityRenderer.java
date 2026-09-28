package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.blockentity.WallChargeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Renders every charge kind with its own 1.7.10 pose and matching bounds path. */
public final class WallChargeBlockEntityRenderer implements BlockEntityRenderer<WallChargeBlockEntity> {
    private static final ModelResourceLocation DYNAMITE = model("block/charge_dynamite_world");
    private static final ModelResourceLocation MINER = model("block/charge_miner_world");
    private static final ModelResourceLocation C4 = model("block/charge_c4_world");
    private static final ModelResourceLocation SEMTEX = model("block/charge_semtex_world");

    public WallChargeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DYNAMITE);
        event.register(MINER);
        event.register(C4);
        event.register(SEMTEX);
    }

    @Override
    public void render(WallChargeBlockEntity charge, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = charge.getBlockState();
        Direction facing = state.getValue(WallChargeBlock.FACING);
        switch (charge.kind()) {
            case DYNAMITE -> renderDynamite(charge, state, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
            case MINER -> renderMiner(charge, state, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
            case C4 -> renderC4(charge, state, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
            case SEMTEX -> renderSemtex(charge, state, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    private static void renderDynamite(WallChargeBlockEntity charge, BlockState state, Direction facing,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyDynamitePose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DYNAMITE), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderDynamiteTimer(charge, facing, poseStack, bufferSource);
    }

    private static void renderMiner(WallChargeBlockEntity charge, BlockState state, Direction facing,
                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyMinerPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MINER), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderMinerTimer(charge, facing, poseStack, bufferSource);
    }

    private static void renderC4(WallChargeBlockEntity charge, BlockState state, Direction facing,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyC4Pose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(C4), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderC4Timer(charge, facing, poseStack, bufferSource);
    }

    private static void renderSemtex(WallChargeBlockEntity charge, BlockState state, Direction facing,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySemtexPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SEMTEX), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderSemtexTimer(charge, facing, poseStack, bufferSource);
    }

    private static void renderDynamiteTimer(WallChargeBlockEntity charge, Direction facing,
                                            PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        applyDynamitePose(poseStack, facing);
        applyDynamiteTimerFace(poseStack);
        drawTimer(charge, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderMinerTimer(WallChargeBlockEntity charge, Direction facing,
                                         PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        applyMinerPose(poseStack, facing);
        applyMinerTimerFace(poseStack);
        drawTimer(charge, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderC4Timer(WallChargeBlockEntity charge, Direction facing,
                                      PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        applyC4Pose(poseStack, facing);
        applyC4TimerFace(poseStack);
        drawTimer(charge, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void renderSemtexTimer(WallChargeBlockEntity charge, Direction facing,
                                          PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        applySemtexPose(poseStack, facing);
        applySemtexTimerFace(poseStack);
        drawTimer(charge, poseStack, bufferSource);
        poseStack.popPose();
    }

    private static void drawTimer(WallChargeBlockEntity charge, PoseStack poseStack,
                                  MultiBufferSource bufferSource) {
        String text = charge.minutes() + ":" + charge.seconds();
        Font font = Minecraft.getInstance().font;
        font.drawInBatch(text, 0.0F, 0.0F, 0x00FF00, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(WallChargeBlockEntity blockEntity) {
        Direction facing = blockEntity.getBlockState().getValue(WallChargeBlock.FACING);
        return switch (blockEntity.kind()) {
            case DYNAMITE -> dynamiteBounds(blockEntity.getBlockPos(), facing);
            case MINER -> minerBounds(blockEntity.getBlockPos(), facing);
            case C4 -> c4Bounds(blockEntity.getBlockPos(), facing);
            case SEMTEX -> semtexBounds(blockEntity.getBlockPos(), facing);
        };
    }

    private static AABB dynamiteBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyDynamitePose(poseStack, facing);
        AABB geometry = RenderBounds.transformedBox(pos, poseStack,
                -0.4375F, -0.5F, -0.4375F, 0.4375F, -0.1875F, 0.4375F);
        return geometry.minmax(new AABB(pos));
    }

    private static AABB minerBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyMinerPose(poseStack, facing);
        AABB geometry = RenderBounds.transformedBox(pos, poseStack,
                -0.4375F, -0.5F, -0.4375F, 0.4375F, -0.1875F, 0.4375F);
        return geometry.minmax(new AABB(pos));
    }

    private static AABB c4Bounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyC4Pose(poseStack, facing);
        AABB geometry = RenderBounds.transformedBox(pos, poseStack,
                -0.46875F, -0.5F, -0.46875F, 0.46875F, -0.1875F, 0.46875F);
        return geometry.minmax(new AABB(pos));
    }

    private static AABB semtexBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySemtexPose(poseStack, facing);
        AABB geometry = RenderBounds.transformedBox(pos, poseStack,
                -0.46875F, -0.5F, -0.46875F, 0.46875F, -0.1875F, 0.46875F);
        return geometry.minmax(new AABB(pos));
    }

    private static void applyDynamiteTimerFace(PoseStack poseStack) {
        poseStack.translate(-0.05F, -0.185F, 0.15F);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    private static void applyMinerTimerFace(PoseStack poseStack) {
        poseStack.translate(-0.05F, -0.185F, 0.15F);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    private static void applyC4TimerFace(PoseStack poseStack) {
        poseStack.translate(-0.05F, -0.185F, 0.15F);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    private static void applySemtexTimerFace(PoseStack poseStack) {
        poseStack.translate(-0.05F, -0.185F, 0.15F);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }

    private static void applyDynamitePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyMinerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyC4Pose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applySemtexPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }
}
