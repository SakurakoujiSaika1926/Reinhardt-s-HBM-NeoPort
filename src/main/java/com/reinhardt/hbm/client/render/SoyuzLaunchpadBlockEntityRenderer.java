package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SoyuzLaunchpadBlock;
import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Dedicated latest-Soyuz grouped renderer; every orientation is machine-specific. */
public final class SoyuzLaunchpadBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<SoyuzLaunchpadBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ResourceLocation PAD_OBJ =
            ReinhardtsHBM.id("models/obj/machine/launchpad_soyuz.obj");
    private static final ResourceLocation PAD_TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/launchpad_soyuz.png");
    private static final LegacyEntityObjMesh PAD = LegacyEntityObjMesh.load(PAD_OBJ);
    private static final ModelResourceLocation SOYUZ = MachineModelRenderer.standalone("entity/soyuz");

    public SoyuzLaunchpadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOYUZ);
    }

    @Override
    public void render(SoyuzLaunchpadBlockEntity launchpad, float partialTick,
                       PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Direction facing = launchpad.getBlockState().getValue(LargeMachineBlock.FACING);
        float rotation = SoyuzLaunchpadBlock.legacyRotation(facing);
        float rotor = clamp(launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_ROTOR, partialTick)
                * -180.0F + 180.0F, 0.0F, 180.0F);
        float carriage = clamp(launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_CARRIAGE, partialTick)
                * -19.5F + 19.5F, 0.0F, 19.5F);
        float wheels = (float) (carriage * 360.0D / Math.PI);
        float tilt = launchpad.renderPosition(SoyuzLaunchpadBlockEntity.INDEX_TILT, partialTick);
        boolean renderSoyuz = launchpad.renderLoadedType() >= 0
                && launchpad.renderStatus() != SoyuzLaunchpadBlockEntity.SoyuzStatus.ABSENT;
        boolean lockSoyuz = launchpad.renderStatus() == SoyuzLaunchpadBlockEntity.SoyuzStatus.LAUNCHING;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(PAD_TEXTURE));
        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-4.0D, 0.0D, -4.0D);

        if (renderSoyuz && lockSoyuz) {
            renderSoyuz(pose, buffers, packedLight, packedOverlay, rotation);
        }

        group("Launchpad", pose, consumer, packedLight, packedOverlay);
        for (int i = 1; i <= 5; i++) {
            pose.pushPose();
            float extension = i == 5 ? 3.0F : 4.5F;
            float strut = clamp(launchpad.renderPosition(i - 1, partialTick)
                    * -extension + extension, 0.0F, extension);
            pose.translate(0.0D, 0.0D, strut);
            group("Strut" + i, pose, consumer, packedLight, packedOverlay);
            pose.popPose();
        }

        pose.translate(0.0D, 0.0D, -carriage);
        pose.translate(0.0D, 1.5D, -32.0D);
        pose.mulPose(Axis.XP.rotationDegrees(-tilt));
        pose.translate(0.0D, -1.5D, 32.0D);
        group("Carriage", pose, consumer, packedLight, packedOverlay);

        double[] wheelsForward = {17.0D, 19.0D, 29.0D, 31.0D};
        double[] wheelsSide = {6.75D, 5.25D, -5.25D, -6.75D};
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                pose.pushPose();
                double forward = wheelsForward[i - 1];
                double side = wheelsSide[j - 1];
                pose.translate(side, 0.0D, -forward);
                pose.mulPose(Axis.YP.rotationDegrees(wheels * (j % 2 == 0 ? -1.0F : 1.0F)));
                pose.translate(-side, 0.0D, forward);
                group("Wheel_" + i + "_" + j, pose, consumer, packedLight, packedOverlay);
                pose.popPose();
            }
        }

        pose.translate(0.0D, 24.5D, -18.0D);
        pose.mulPose(Axis.XP.rotationDegrees(-rotor));
        pose.translate(0.0D, -24.5D, 18.0D);
        group("Rotor", pose, consumer, packedLight, packedOverlay);

        pose.translate(0.0D, 24.5D, -6.0D);
        pose.mulPose(Axis.XP.rotationDegrees(rotor));
        pose.translate(0.0D, -24.5D, 6.0D);
        group("Mount", pose, consumer, packedLight, packedOverlay);

        if (renderSoyuz && !lockSoyuz) {
            renderSoyuz(pose, buffers, packedLight, packedOverlay, rotation);
        }
        pose.popPose();
    }

    private static void renderSoyuz(PoseStack pose, MultiBufferSource buffers,
                                    int light, int overlay, float rotation) {
        pose.pushPose();
        pose.translate(0.0D, 4.0D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-rotation));
        STATIC_MESHES.render(SOYUZ, pose, Blocks.IRON_BLOCK.defaultBlockState(), light, overlay);
        pose.popPose();
    }

    private static void group(String name, PoseStack pose, VertexConsumer consumer,
                              int light, int overlay) {
        PAD.renderGroup(name, pose, consumer, light, overlay, 0xFFFFFFFF);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzLaunchpadBlockEntity launchpad) {
        var pos = launchpad.getBlockPos();
        Direction facing = launchpad.getBlockState().getValue(LargeMachineBlock.FACING);
        return switch (facing) {
            case EAST -> new AABB(pos.getX() - 10.0D, pos.getY(), pos.getZ() - 58.0D,
                    pos.getX() + 4.0D, pos.getY() + 54.0D, pos.getZ() + 8.0D);
            case WEST -> new AABB(pos.getX() - 3.0D, pos.getY(), pos.getZ() - 7.0D,
                    pos.getX() + 11.0D, pos.getY() + 54.0D, pos.getZ() + 59.0D);
            case SOUTH -> new AABB(pos.getX() - 7.0D, pos.getY(), pos.getZ() - 10.0D,
                    pos.getX() + 59.0D, pos.getY() + 54.0D, pos.getZ() + 4.0D);
            case NORTH -> new AABB(pos.getX() - 58.0D, pos.getY(), pos.getZ() - 3.0D,
                    pos.getX() + 8.0D, pos.getY() + 54.0D, pos.getZ() + 11.0D);
            case UP, DOWN -> throw new IllegalStateException("Soyuz launch pad facing is vertical");
        };
    }
}
