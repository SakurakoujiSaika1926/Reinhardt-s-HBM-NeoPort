package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.blockentity.CrashedBombBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct RenderCrashedBomb transform, intentionally independent from machine facing transforms. */
public final class CrashedBombBlockEntityRenderer implements BlockEntityRenderer<CrashedBombBlockEntity> {
    private static final ModelResourceLocation[] MODELS = {
            model("crashed_bomb_balefire"), model("crashed_bomb_conventional"),
            model("crashed_bomb_nuke"), model("crashed_bomb_salted")
    };

    public CrashedBombBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : MODELS) {
            event.register(model);
        }
    }

    @Override
    public void render(CrashedBombBlockEntity bomb, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bomb.getBlockState();
        CrashedBombBlock.Type type = CrashedBombBlock.type(state);
        RandomSource random = RandomSource.create(bomb.getBlockPos().asLong());
        float yaw = random.nextFloat() * 360.0F;
        float pitch = random.nextFloat() * 45.0F + 45.0F;
        float roll = random.nextFloat() * 360.0F;
        float offset = random.nextFloat() * 2.0F - 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0.0D, 0.0D, -offset + worldOffset(type));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODELS[type.ordinal()]), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrashedBombBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D);
    }

    private static float worldOffset(CrashedBombBlock.Type type) {
        return switch (type) {
            case NUKE -> 1.25F;
            case SALTED -> 0.5F;
            default -> 0.0F;
        };
    }

    private static ModelResourceLocation model(String path) {
        return new ModelResourceLocation(ReinhardtsHBM.id("block/" + path), ModelResourceLocation.STANDALONE_VARIANT);
    }
}
