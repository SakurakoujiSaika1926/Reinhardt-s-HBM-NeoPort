package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.VendingMachineBlock;
import com.reinhardt.hbm.blockentity.VendingMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Literal RenderVendingMachine orientation mapping with separate legacy OBJ groups. */
public final class VendingMachineBlockEntityRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {
    private static final ModelResourceLocation SODA = MachineModelRenderer.standalone("block/vending_machine_soda");
    private static final ModelResourceLocation SNACKS = MachineModelRenderer.standalone("block/vending_machine_snacks");

    public VendingMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SODA);
        event.register(SNACKS);
    }

    @Override
    public void render(VendingMachineBlockEntity vending, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = vending.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(legacyYaw(state.getValue(VendingMachineBlock.FACING))));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(vending.snacks() ? SNACKS : SODA), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(VendingMachineBlockEntity vending) {
        return new AABB(vending.getBlockPos()).expandTowards(0.0D, 1.0D, 0.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 0.0F;
        };
    }
}
