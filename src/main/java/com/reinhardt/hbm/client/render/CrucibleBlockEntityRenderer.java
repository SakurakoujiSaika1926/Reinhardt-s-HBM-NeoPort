package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class CrucibleBlockEntityRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_crucible_world");
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava.png");

    public CrucibleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(CrucibleBlockEntity crucible, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = crucible.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderMoltenSurface(crucible, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrucibleBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
    }

    private static void renderMoltenSurface(CrucibleBlockEntity crucible, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        int totalMass = 0;
        int color = 0xFFB65434;
        for (FoundryMaterialStack stack : crucible.recipeStack()) {
            totalMass += stack.amount();
            color = 0xFF000000 | stack.material().moltenColor();
        }
        for (FoundryMaterialStack stack : crucible.wasteStack()) {
            totalMass += stack.amount();
            if (color == 0xFFB65434) {
                color = 0xFF000000 | stack.material().moltenColor();
            }
        }
        if (totalMass <= 0) {
            return;
        }

        double level = 0.5D + ((double) totalMass / (double) (CrucibleBlockEntity.RECIPE_CAPACITY + CrucibleBlockEntity.WASTE_CAPACITY)) * 0.875D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, new Vec3(-1.0D, level, -1.0D), 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(-1.0D, level, 1.0D), 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(1.0D, level, 1.0D), 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(1.0D, level, -1.0D), 1.0F, 0.0F, r, g, b, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 220)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
