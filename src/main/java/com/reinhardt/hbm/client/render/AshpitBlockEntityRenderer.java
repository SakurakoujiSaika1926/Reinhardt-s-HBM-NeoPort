package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct part-by-part port of 1.7.10 RenderAshpit. */
public final class AshpitBlockEntityRenderer implements BlockEntityRenderer<AshpitBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_ashpit_world");
    private static final ModelResourceLocation DOOR = MachineModelRenderer.standalone("block/machine_ashpit_door");
    private static final ModelResourceLocation INNER = MachineModelRenderer.standalone("block/machine_ashpit_inner");
    private static final ModelResourceLocation INNER_BURNING = MachineModelRenderer.standalone("block/machine_ashpit_inner_burning");

    public AshpitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(DOOR);
        event.register(INNER);
        event.register(INNER_BURNING);
    }

    @Override
    public void render(AshpitBlockEntity ashpit, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState state = ashpit.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        int worldLight = ashpit.getLevel() == null ? packedLight
                : LevelRenderer.getLightColor(ashpit.getLevel(), ashpit.getBlockPos());

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyHeaterParts(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, worldLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, ashpit.doorAngle(partialTick) * 0.75F / 135.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DOOR), poseStack, bufferSource, state, worldLight, packedOverlay);
        poseStack.popPose();

        BakedModel inner = MachineModelRenderer.model(ashpit.isFull() ? INNER_BURNING : INNER);
        if (ashpit.isFull()) {
            MachineModelRenderer.renderUnculledFullBright(inner, poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(inner, poseStack, bufferSource, state, worldLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AshpitBlockEntity ashpit) {
        return new AABB(ashpit.getBlockPos().getX() - 1, ashpit.getBlockPos().getY(), ashpit.getBlockPos().getZ() - 1,
                ashpit.getBlockPos().getX() + 2, ashpit.getBlockPos().getY() + 1, ashpit.getBlockPos().getZ() + 2);
    }
}
