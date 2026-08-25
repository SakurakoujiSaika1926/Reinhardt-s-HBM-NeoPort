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

/** Exact ModelPylon box geometry and UV layout from 1.7.10. */
public final class LegacyRedPylonModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("red_pylon"), "main");

    private final ModelPart root;

    public LegacyRedPylonModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 96).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(-8.0F, -6.0F, -8.0F));
        root.addOrReplaceChild("pole",
                CubeListBuilder.create().texOffs(1, 1).addBox(0.0F, 0.0F, 0.0F, 4.0F, 73.0F, 4.0F),
                PartPose.offset(-2.0F, -79.0F, -2.0F));
        root.addOrReplaceChild("top_block",
                CubeListBuilder.create().texOffs(24, 1).addBox(0.0F, 0.0F, 0.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(-3.0F, -74.0F, -3.0F));
        root.addOrReplaceChild("cap",
                CubeListBuilder.create().texOffs(25, 17).addBox(0.0F, 0.0F, 0.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(-3.0F, -78.0F, -3.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
