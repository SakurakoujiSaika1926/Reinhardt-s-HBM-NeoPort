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

/** Exact ModelPoleTop geometry and UV layout from HBM 1.7.10. */
public final class LegacyPoleTopModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("pole_top"), "main");

    private final ModelPart root;

    public LegacyPoleTopModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        cube(root, "base", 0, 1, 0, 0, 0, 12, 8, 12, -6, 16, -6);
        cube(root, "leg_nw", 0, 23, 0, 0, 0, 4, 16, 4, -8, 4, -8);
        cube(root, "leg_ne", 0, 23, 0, 0, 0, 4, 16, 4, 4, 4, -8);
        cube(root, "leg_sw", 0, 23, 0, 0, 0, 4, 16, 4, -8, 4, 4);
        cube(root, "leg_se", 0, 23, 0, 0, 0, 4, 16, 4, 4, 4, 4);
        cube(root, "cap", 0, 47, 0, 0, 0, 4, 2, 4, -2, 14, -2);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void cube(PartDefinition root, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz),
                PartPose.offset(px, py, pz));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
