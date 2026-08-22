package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.FoundryMoldItem;
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

public class StrandCasterBlockEntityRenderer implements BlockEntityRenderer<StrandCasterBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_strand_caster");
    private static final ModelResourceLocation PLATE = MachineModelRenderer.standalone("block/machine_strand_caster_plate");
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava_gray.png");

    public StrandCasterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PLATE);
    }

    @Override
    public void render(StrandCasterBlockEntity caster, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = caster.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0F), 0.0F, 1.0F, 0.0F)));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderDynamicMolten(caster, poseStack, bufferSource, state, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StrandCasterBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 8.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 8.0D,
                blockEntity.getBlockPos().getX() + 8.0D,
                blockEntity.getBlockPos().getY() + 3.0D,
                blockEntity.getBlockPos().getZ() + 8.0D
        );
    }

    private static void renderDynamicMolten(
            StrandCasterBlockEntity caster,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        FoundryMaterial material = caster.material();
        FoundryMoldItem.Mold mold = caster.getInstalledMold();
        if (material == null || mold == null || caster.amount() <= 0) {
            return;
        }
        int color = 0xFF000000 | material.moltenColor();
        double offset = ((double) caster.amount() / (double) mold.cost()) * 0.375D;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, Math.max(-offset + 3.4D, 0.0D));
        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(PLATE), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, color);
        poseStack.popPose();

        double level = ((double) caster.amount() / (double) caster.getCapacity()) * 0.675D;
        renderMoltenSurface(poseStack, bufferSource, packedOverlay, color, 2.3D + level);
    }

    private static void renderMoltenSurface(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay, int color, double y) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, new Vec3(-0.9D, y, -0.999D), 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(-0.9D, y, 0.999D), 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(0.9D, y, 0.999D), 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, new Vec3(0.9D, y, -0.999D), 1.0F, 0.0F, r, g, b, packedOverlay);
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
            case EAST -> 270.0F;
            case NORTH -> 0.0F;
            case WEST -> 90.0F;
            default -> 180.0F;
        };
    }
}
