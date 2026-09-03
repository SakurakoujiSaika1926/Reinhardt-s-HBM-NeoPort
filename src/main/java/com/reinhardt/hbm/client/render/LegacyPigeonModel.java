package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyPigeonEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Direct LayerDefinition conversion of the old ModelPigeon. */
public final class LegacyPigeonModel extends EntityModel<LegacyPigeonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("pigeon"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart body;
    private final ModelPart bodyFat;
    private final ModelPart ass;
    private final ModelPart feathers;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart bodyLeftWing;
    private final ModelPart bodyRightWing;
    private final ModelPart fatLeftWing;
    private final ModelPart fatRightWing;

    public LegacyPigeonModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        beak = root.getChild("beak");
        body = root.getChild("body");
        bodyFat = root.getChild("body_fat");
        ass = root.getChild("ass");
        feathers = root.getChild("feathers");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        bodyLeftWing = body.getChild("left_wing");
        bodyRightWing = body.getChild("right_wing");
        fatLeftWing = bodyFat.getChild("left_wing");
        fatRightWing = bodyFat.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", cube(0, 0, -2, -6, -2, 4, 6, 4), PartPose.offset(0, 16, -2));
        root.addOrReplaceChild("beak", cube(14, 0, -1, -4, -4, 2, 2, 2), PartPose.offset(0, 16, -2));
        PartDefinition body = root.addOrReplaceChild("body", cube(0, 10, -3, -3, -4, 6, 6, 8),
                PartPose.offset(0, 17, 0));
        body.addOrReplaceChild("left_wing", cube(28, 0, 0, 0, -3, 1, 4, 6),
                PartPose.offset(3, -2, 0));
        body.addOrReplaceChild("right_wing", cube(28, 10, -1, 0, -3, 1, 4, 6),
                PartPose.offset(-3, -2, 0));
        PartDefinition bodyFat = root.addOrReplaceChild("body_fat", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-3, -3, -4, 6, 6, 8, new CubeDeformation(1.0F)),
                PartPose.offset(0, 17, 0));
        bodyFat.addOrReplaceChild("left_wing", cube(28, 0, 0, 0, -3, 1, 4, 6),
                PartPose.offset(3, -2, 0));
        bodyFat.addOrReplaceChild("right_wing", cube(28, 10, -1, 0, -3, 1, 4, 6),
                PartPose.offset(-3, -2, 0));
        root.addOrReplaceChild("ass", cube(0, 24, -2, -2, -2, 4, 4, 4), PartPose.offset(0, 20, 4));
        root.addOrReplaceChild("feathers", cube(16, 24, -1, -.5F, -2, 2, 1, 4), PartPose.offset(0, 21.5F, 7.5F));
        root.addOrReplaceChild("left_leg", cube(20, 0, -1, 0, 0, 2, 4, 2), PartPose.offset(1, 20, -1));
        root.addOrReplaceChild("right_leg", cube(20, 0, -1, 0, 0, 2, 4, 2), PartPose.offset(-1, 20, -1));
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static CubeListBuilder cube(int u, int v, float x, float y, float z,
                                        float dx, float dy, float dz) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz);
    }

    @Override
    public void setupAnim(LegacyPigeonEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float headRotation = (float) Math.toRadians(headPitch);
        head.xRot = beak.xRot = headRotation;
        head.yRot = beak.yRot = (float) Math.toRadians(netHeadYaw);
        body.xRot = bodyFat.xRot = ass.xRot = -(float) Math.PI / 4.0F;
        feathers.xRot = -(float) Math.PI / 8.0F;
        rightLeg.xRot = (float) Math.cos(limbSwing * .6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot = (float) Math.cos(limbSwing * .6662F + Math.PI) * 1.4F * limbSwingAmount;
        boolean fat = entity.isFat();
        body.visible = !fat;
        bodyFat.visible = fat;
        float wingRotation = (float) ((Math.sin(entity.fallTime) + 1.0F) * entity.dest);
        ModelPart leftWing = fat ? fatLeftWing : bodyLeftWing;
        ModelPart rightWing = fat ? fatRightWing : bodyRightWing;
        rightWing.zRot = wingRotation;
        leftWing.zRot = -wingRotation;
        head.z = fat ? -4.0F : -2.0F;
        beak.z = fat ? -4.0F : -2.0F;
        ass.z = fat ? 5.0F : 4.0F;
        feathers.z = fat ? 8.5F : 7.5F;
        leftWing.x = fat ? 4.0F : 3.0F;
        rightWing.x = fat ? -4.0F : -3.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int color) {
        head.render(poseStack, consumer, packedLight, packedOverlay, color);
        beak.render(poseStack, consumer, packedLight, packedOverlay, color);
        (bodyFat.visible ? bodyFat : body).render(poseStack, consumer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, consumer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, consumer, packedLight, packedOverlay, color);
        ass.render(poseStack, consumer, packedLight, packedOverlay, color);
        feathers.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
