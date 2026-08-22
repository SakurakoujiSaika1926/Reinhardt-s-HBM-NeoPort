package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class IronFurnaceBlockEntityRenderer implements BlockEntityRenderer<IronFurnaceBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/furnace_iron_main");
    private static final ModelResourceLocation ON = MachineModelRenderer.standalone("block/furnace_iron_on");
    private static final ModelResourceLocation OFF = MachineModelRenderer.standalone("block/furnace_iron_off");

    public IronFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(ON);
        event.register(OFF);
    }

    @Override
    public void render(IronFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        boolean lit = state.hasProperty(LegacyFurnaceBlock.LIT) && state.getValue(LegacyFurnaceBlock.LIT);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyIronYaw(facing));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (lit) {
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(ON), poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(OFF), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IronFurnaceBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 3.0D, 2.0D);
    }

    private static float legacyIronYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            default -> 270.0F;
        };
    }
}
