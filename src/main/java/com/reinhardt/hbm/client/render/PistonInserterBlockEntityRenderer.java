package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PistonInserterBlock;
import com.reinhardt.hbm.blockentity.PistonInserterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Exact 1.7.10 RenderPistonInserter model parts, direction table and item pose. */
public final class PistonInserterBlockEntityRenderer implements BlockEntityRenderer<PistonInserterBlockEntity> {
    private static final ModelResourceLocation FRAME = part("piston_inserter_frame");
    private static final ModelResourceLocation PISTON = part("piston_inserter_piston");

    public PistonInserterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(PISTON);
    }

    @Override
    public void render(PistonInserterBlockEntity inserter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = inserter.getBlockState();
        Direction facing = state.getValue(PistonInserterBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotateLegacy(poseStack, facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);

        float extension = inserter.extension(partialTick) / PistonInserterBlockEntity.MAX_EXTEND;
        poseStack.pushPose();
        poseStack.translate(0.0D, extension * 0.9375D, 0.0D);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);

        ItemStack stack = inserter.getItem(0);
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            if (stack.getItem() instanceof BlockItem) {
                poseStack.translate(0.0D, 1.125D, 0.0D);
            } else {
                poseStack.translate(0.0D, 1.0625D, 0.1D);
                poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, bufferSource, null, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PistonInserterBlockEntity inserter) {
        Direction direction = inserter.getBlockState().getValue(PistonInserterBlock.FACING);
        int x = inserter.getBlockPos().getX();
        int y = inserter.getBlockPos().getY();
        int z = inserter.getBlockPos().getZ();
        return new AABB(
                x + Math.min(0, direction.getStepX()),
                y + Math.min(0, direction.getStepY()),
                z + Math.min(0, direction.getStepZ()),
                x + 1 + Math.max(0, direction.getStepX()),
                y + 1 + Math.max(0, direction.getStepY()),
                z + 1 + Math.max(0, direction.getStepZ())
        );
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static void rotateLegacy(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(MachineModelRenderer.xQuaternion(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
                poseStack.mulPose(yQuaternion(180.0F));
            }
            case SOUTH -> poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
            case WEST -> {
                poseStack.mulPose(MachineModelRenderer.zQuaternion(90.0F));
                poseStack.mulPose(yQuaternion(-90.0F));
            }
            case EAST -> {
                poseStack.mulPose(MachineModelRenderer.zQuaternion(-90.0F));
                poseStack.mulPose(yQuaternion(90.0F));
            }
        }
    }

    private static Quaternionf yQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
