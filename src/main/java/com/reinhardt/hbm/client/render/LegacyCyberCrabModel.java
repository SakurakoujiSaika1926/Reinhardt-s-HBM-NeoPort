package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact twenty-part ModelCrab geometry from the 1.7.10 renderer. */
public final class LegacyCyberCrabModel extends EntityModel<LegacyCyberCrabEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("cyber_crab"), "main");
    private final ModelPart root;
    private final ModelPart[] parts = new ModelPart[20];

    public LegacyCyberCrabModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < parts.length; i++) {
            parts[i] = root.getChild("part" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, 0, 1, 1, 0, 0, 0, 4, 1, 4, -2, -3, -2, 0, 0, 0);
        part(root, 1, 17, 1, 0, 0, 0, 4, 1, 6, -2, -4, -3, 0, 0, 0);
        part(root, 2, 33, 1, 0, 0, 0, 3, 1, 3, -1.5F, -5, -1.5F, 0, 0, 0);
        part(root, 3, 49, 1, 0, 0, 0, 4, 1, 2, -2, -4.5F, -1, 0, 0, 0);
        part(root, 4, 1, 9, 0, 0, 0, 6, 1, 4, -3, -4, -2, 0, 0, 0);
        part(root, 5, 25, 9, -.5F, 0, 2, 1, 1, 3, 0, -3, 0, -.17453293F, .78539816F, 0);
        part(root, 6, 41, 9, -.5F, 0, 2, 1, 1, 3, 0, -3, 0, -.17453293F, -.78539816F, 0);
        part(root, 7, 1, 17, -.5F, 0, 2, 1, 1, 3, 0, -3, 0, -.17453293F, -2.35619449F, 0);
        part(root, 8, 17, 17, -.5F, 0, 2, 1, 1, 3, 0, -3, 0, -.17453293F, 2.35619449F, 0);
        part(root, 9, 57, 9, -.5F, 1, 4, 1, 3, 1, 0, -3, 0, .17453293F, -.78539816F, 0);
        part(root, 10, 33, 17, -.5F, 1, 4, 1, 3, 1, 0, -3, 0, .17453293F, .78539816F, 0);
        part(root, 11, 41, 17, -.5F, 1, 4, 1, 3, 1, 0, -3, 0, .17453293F, -2.35619449F, 0);
        part(root, 12, 49, 17, -.5F, 1, 4, 1, 3, 1, 0, -3, 0, .17453293F, 2.35619449F, 0);
        part(root, 13, 17, 1, -.5F, 0, 1.5F, 1, 1, 1, 0, -3, 0, -.43633231F, -.6981317F, 0);
        part(root, 14, 33, 9, -.5F, 0, 1.5F, 1, 1, 1, 0, -3, 0, -.43633231F, .87266463F, 0);
        part(root, 15, 49, 9, -.5F, 0, 1.5F, 1, 1, 1, 0, -3, 0, -.43633231F, -2.26892803F, 0);
        part(root, 16, 9, 17, -.5F, 0, 1.5F, 1, 1, 1, 0, -3, 0, -.43633231F, 2.44346095F, 0);
        part(root, 17, 1, 25, 0, 0, 0, 2, 1, 4, -1, -4.5F, -2, 0, 0, 0);
        part(root, 18, 17, 25, 0, 0, 0, 5, 1, 3, -2.5F, -3.5F, -1.5F, 0, 0, 0);
        part(root, 19, 33, 25, 0, 0, 0, 3, 1, 5, -1.5F, -3.5F, -2.5F, 0, 0, 0);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void part(PartDefinition root, int id, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild("part" + id,
                CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    @Override
    public void setupAnim(LegacyCyberCrabEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        parts[9].yRot = -.78539816F;
        parts[10].yRot = .78539816F;
        parts[11].yRot = -2.35619449F;
        parts[12].yRot = 2.35619449F;
        float swing = (float) (-(Math.cos(limbSwing * .6662F * 2.0F) * .4F)
                * limbSwingAmount * 1.5F);
        parts[9].yRot -= swing;
        parts[10].yRot += swing;
        parts[11].yRot -= swing;
        parts[12].yRot += swing;
        parts[5].yRot = parts[10].yRot;
        parts[6].yRot = parts[9].yRot;
        parts[7].yRot = parts[11].yRot;
        parts[8].yRot = parts[12].yRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int color) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
