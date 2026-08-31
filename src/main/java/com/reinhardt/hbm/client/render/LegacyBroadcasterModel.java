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

/** Exact ModelBroadcaster geometry and UV layout from HBM 1.7.10. */
public final class LegacyBroadcasterModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("broadcaster"), "main");

    private final ModelPart root;

    public LegacyBroadcasterModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, "body", 0, 0, 14.0F, 10.0F, 8.0F, -7.0F, 14.0F, -4.0F);
        part(root, "antenna_base", 4, 21, 2.0F, 3.0F, 2.0F, -5.0F, 11.0F, -1.0F);
        part(root, "antenna", 0, 18, 1.0F, 11.0F, 1.0F, -4.5F, 0.0F, -0.5F);
        part(root, "switch", 4, 18, 3.0F, 2.0F, 1.0F, 2.0F, 12.0F, -0.5F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void part(PartDefinition root, String name, int u, int v,
                             float width, float height, float depth,
                             float x, float y, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror()
                        .addBox(0.0F, 0.0F, 0.0F, width, height, depth),
                PartPose.offset(x, y, z));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
