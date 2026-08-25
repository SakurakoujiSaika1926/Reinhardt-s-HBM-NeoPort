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

/** Original ModelChopperMine's single eight-pixel cube. */
public final class LegacyChopperMineModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ReinhardtsHBM.id("chopper_mine"), "main");
    private final ModelPart root;

    public LegacyChopperMineModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("mine", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 0, 0, 8, 8, 8), PartPose.offset(-4, -4, -4));
        return LayerDefinition.create(mesh, 32, 16);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
