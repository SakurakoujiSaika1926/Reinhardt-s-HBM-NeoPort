package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GasFlareBlockEntityRenderer implements BlockEntityRenderer<GasFlareBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_flare_world");

    public GasFlareBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(
            GasFlareBlockEntity flare,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = flare.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, 180.0F);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(WORLD),
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasFlareBlockEntity flare) {
        BlockPosBounds bounds = new BlockPosBounds(flare.getBlockPos().getX(), flare.getBlockPos().getY(), flare.getBlockPos().getZ());
        return bounds.asBox();
    }

    private record BlockPosBounds(int x, int y, int z) {
        private AABB asBox() {
            return new AABB(this.x - 2.0D, this.y, this.z - 2.0D, this.x + 3.0D, this.y + 13.0D, this.z + 3.0D);
        }
    }
}
