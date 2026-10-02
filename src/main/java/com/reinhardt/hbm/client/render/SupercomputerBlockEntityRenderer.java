package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SupercomputerBlock;
import com.reinhardt.hbm.blockentity.SupercomputerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Literal RenderSuperComputer body and scrolling full-bright scan pass. */
public final class SupercomputerBlockEntityRenderer implements LongRangeBlockEntityRenderer<SupercomputerBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation COMPUTER =
            MachineModelRenderer.standalone("block/machine_supercomputer_computer");
    private static final ModelResourceLocation LIGHTS =
            MachineModelRenderer.standalone("block/machine_supercomputer_lights");
    private static final ResourceLocation SCAN_TEXTURE =
            ReinhardtsHBM.id("textures/models/machines/supercomputer_scan.png");

    public SupercomputerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(COMPUTER);
        event.register(LIGHTS);
    }

    @Override
    public void render(SupercomputerBlockEntity computer, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = computer.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_SUPERCOMPUTER.get())
                || !state.hasProperty(LargeMachineBlock.FACING)) {
            return;
        }
        Direction facing = state.getValue(LargeMachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(SupercomputerBlock.legacyRotation(facing)));
        STATIC_MESHES.render(COMPUTER, poseStack, state, packedLight, packedOverlay);
        if (computer.processing()) {
            float scroll = ((computer.getLevel() == null ? 0L : computer.getLevel().getGameTime()) % 20L
                    + partialTick) / 20.0F;
            MachineModelRenderer.renderUnculledUv(
                    MachineModelRenderer.model(LIGHTS), poseStack, buffers, state,
                    LightTexture.FULL_BRIGHT, packedOverlay, SCAN_TEXTURE, -scroll, 0.0F);
        }
        poseStack.popPose();
    }

    /** Direct conversion of RenderSuperComputer's inventory/common transforms. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
                                  int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_SUPERCOMPUTER.get().defaultBlockState();
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(-2.0F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        STATIC_MESHES.render(COMPUTER, poseStack, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculledUv(
                MachineModelRenderer.model(LIGHTS), poseStack, buffers, state,
                LightTexture.FULL_BRIGHT, packedOverlay, SCAN_TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SupercomputerBlockEntity computer) {
        BlockState state = computer.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_SUPERCOMPUTER.get())
                || !state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Supercomputer renderer received an invalid block state: " + state);
        }
        Direction facing = state.getValue(LargeMachineBlock.FACING);
        AABB local = switch (facing) {
            case SOUTH -> new AABB(-3.0D, 0.0D, -3.0D, 4.0D, 8.25D, 9.0D);
            case NORTH -> new AABB(-3.0D, 0.0D, -8.0D, 4.0D, 8.25D, 4.0D);
            case EAST -> new AABB(-3.0D, 0.0D, -3.0D, 9.0D, 8.25D, 4.0D);
            case WEST -> new AABB(-8.0D, 0.0D, -3.0D, 4.0D, 8.25D, 4.0D);
            case UP, DOWN -> throw new IllegalStateException("Supercomputer facing is vertical: " + facing);
        };
        return local.move(computer.getBlockPos());
    }
}
