package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Exact ModelRadio box geometry and UV layout from HBM 1.7.10. */
public final class LegacyRadioboxModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("radiobox"), "main");

    private final ModelPart root;
    private final ModelPart lever;

    public LegacyRadioboxModel(ModelPart root) {
        this.root = root;
        this.lever = root.getChild("lever");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("box",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 8.0F, 14.0F, 4.0F),
                PartPose.offset(-4.0F, 9.0F, -12.0F));
        root.addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(0, 18).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 7.0F, 13.0F, 1.0F),
                PartPose.offset(-3.5F, 9.5F, -12.5F));
        root.addOrReplaceChild("lever",
                CubeListBuilder.create().texOffs(16, 18).mirror()
                        .addBox(0.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(4.0F, 16.0F, -10.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                       boolean active) {
        this.lever.xRot = (float) Math.toRadians(active ? -160.0F : -20.0F);
        this.root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
