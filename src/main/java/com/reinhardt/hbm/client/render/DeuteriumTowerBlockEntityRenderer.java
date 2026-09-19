package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.DeuteriumTowerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class DeuteriumTowerBlockEntityRenderer implements LongRangeBlockEntityRenderer<DeuteriumTowerBlockEntity> {
    private static final ModelResourceLocation TOWER = MachineModelRenderer.standalone("block/machine_deuterium_tower_world");

    public DeuteriumTowerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TOWER);
    }

    @Override
    public void render(DeuteriumTowerBlockEntity extractor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = extractor.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_DEUTERIUM_TOWER.get())) {
            return;
        }

        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion(180.0F));
        applyLegacyTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Literal ItemRenderLibrary transform for machine_deuterium_tower. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_DEUTERIUM_TOWER.get().defaultBlockState();
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(3.0F, 3.0F, 3.0F);
        }
        poseStack.mulPose(yawQuaternion(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TOWER), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DeuteriumTowerBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 1.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 1.0D,
                blockEntity.getBlockPos().getX() + 2.0D,
                blockEntity.getBlockPos().getY() + 10.0D,
                blockEntity.getBlockPos().getZ() + 2.0D
        );
    }

    private static void applyLegacyTransform(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case NORTH -> poseStack.translate(0.0F, 0.0F, -1.0F);
            case SOUTH -> {
                poseStack.mulPose(yawQuaternion(180.0F));
                poseStack.translate(1.0F, 0.0F, 0.0F);
            }
            case WEST -> {
                poseStack.mulPose(yawQuaternion(90.0F));
                poseStack.translate(1.0F, 0.0F, -1.0F);
            }
            case EAST -> poseStack.mulPose(yawQuaternion(270.0F));
            default -> {
            }
        }
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
