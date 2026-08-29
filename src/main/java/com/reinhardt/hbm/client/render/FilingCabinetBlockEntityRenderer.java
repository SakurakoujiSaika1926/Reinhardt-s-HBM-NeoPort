package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.blockentity.FilingCabinetBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class FilingCabinetBlockEntityRenderer implements BlockEntityRenderer<FilingCabinetBlockEntity> {
    private static final ModelResourceLocation GREEN_BASE = MachineModelRenderer.standalone("block/filing_cabinet_green_base");
    private static final ModelResourceLocation GREEN_LOWER = MachineModelRenderer.standalone("block/filing_cabinet_green_lower");
    private static final ModelResourceLocation GREEN_UPPER = MachineModelRenderer.standalone("block/filing_cabinet_green_upper");
    private static final ModelResourceLocation STEEL_BASE = MachineModelRenderer.standalone("block/filing_cabinet_steel_base");
    private static final ModelResourceLocation STEEL_LOWER = MachineModelRenderer.standalone("block/filing_cabinet_steel_lower");
    private static final ModelResourceLocation STEEL_UPPER = MachineModelRenderer.standalone("block/filing_cabinet_steel_upper");

    public FilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GREEN_BASE);
        event.register(GREEN_LOWER);
        event.register(GREEN_UPPER);
        event.register(STEEL_BASE);
        event.register(STEEL_LOWER);
        event.register(STEEL_UPPER);
    }

    @Override
    public void render(FilingCabinetBlockEntity cabinet, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = cabinet.getBlockState();
        boolean steel = state.getValue(FilingCabinetBlock.MATERIAL) == 1;
        ModelResourceLocation base = steel ? STEEL_BASE : GREEN_BASE;
        ModelResourceLocation lower = steel ? STEEL_LOWER : GREEN_LOWER;
        ModelResourceLocation upper = steel ? STEEL_UPPER : GREEN_UPPER;
        float rotation = switch (FilingCabinetBlock.legacyRotation(state.getValue(FilingCabinetBlock.FACING))) {
            case 0 -> 180.0F;
            case 1 -> 0.0F;
            case 2 -> 270.0F;
            default -> 90.0F;
        };
        poseStack.pushPose();
        poseStack.translate(.5F, 0.0F, .5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(base), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, .6875F * cabinet.lower(partialTick));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(lower), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, .6875F * (cabinet.upper(partialTick) - cabinet.lower(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(upper), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.popPose();
    }
}
