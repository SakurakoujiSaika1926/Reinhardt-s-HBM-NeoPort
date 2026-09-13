package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Pixel-for-pixel conversion of the 1.7.10 ModelM65Blaze render pass. */
public final class LegacyM65BlazeModel extends EntityModel<LegacyRadBeastEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("m65_blaze"), "main");

    private final ModelPart mask;

    public LegacyM65BlazeModel(ModelPart root) {
        this.mask = root.getChild("mask");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition mask = mesh.getRoot().addOrReplaceChild(
                "mask", CubeListBuilder.create(), PartPose.ZERO);

        part(mask, "shape1", 0, 0, 0, 0, 0, 8, 8, 8, -4, -4, -4, 0);
        part(mask, "shape2", 0, 16, 0, 0, 0, 3, 3, 1, -1.5F, 0.5F, -5, 0);
        part(mask, "shape3", 0, 20, 0, -2, 0, 2, 2, 1, -1, 0.5F, -5, -0.4799655F);
        part(mask, "shape4", 8, 16, 0, 0, -2, 3, 2, 2, -1.5F, 2, -4, 0.6108652F);
        part(mask, "shape5", 0, 23, 0, 0, 0, 3, 3, 0, -3.5F, -2, -4.2F, 0);
        part(mask, "shape6", 0, 26, 0, 0, 0, 3, 3, 0, 0.5F, -2, -4.2F, 0);
        part(mask, "shape7", 6, 20, 0, 0, 0, 2, 2, 1, -1, 0.8F, -6, 0);
        part(mask, "shape8", 6, 23, 0, 0, -3, 2, 2, 1, -1, 2, -4, 0.6108652F);
        part(mask, "shape9", 18, 21, 0, -1, -5, 3, 4, 2, -1.5F, 2, -4, 0.6108652F);
        part(mask, "shape10", 18, 16, 0, -0.5F, -5, 4, 3, 2, -2, 2, -4, 0.6108652F);

        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void part(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float pivotX, float pivotY, float pivotZ, float xRot) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, xRot, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(LegacyRadBeastEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        mask.yRot = (float) Math.toRadians(netHeadYaw);
        mask.xRot = (float) Math.toRadians(headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int color) {
        mask.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
