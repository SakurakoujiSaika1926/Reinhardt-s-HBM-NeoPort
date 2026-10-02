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

/** Exact cuboids and pivots from the current 1.7.10 {@code ModelGun}. */
public final class LegacyStatueGunModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("statue_elb_f"), "gun");

    private final ModelPart root;

    public LegacyStatueGunModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        cube(root, "shape1", 0, 0, 0, 0, 0, 6, 12, 4, 0, -4, -1, 0);
        cube(root, "shape2", 52, 0, 0, 0, 0, 3, 3, 3, 4, -7, -0.5F, 0);
        cube(root, "shape3", 28, 58, 0, 0, 0, 15, 3, 3, -15, -7, -0.5F, 0);
        cube(root, "shape4", 0, 61, 0, 0, 0, 1, 2, 1, 2, -3, 0.5F, 0.715585F);
        cube(root, "shape5", 0, 57, 0, 0, 0, 2, 2, 1, -13.5F, -8, 0.5F, 0.6108652F);
        cube(root, "shape6", 52, 7, 0, 0, 0, 4, 3, 2, 0, -6.5F, 0, 0);
        cube(root, "shape7", 46, 49, 0, 0, 0, 6, 5, 3, -15, -3, -0.5F, 0);
        cube(root, "shape8", 22, 0, 0, 0, 0, 12, 1, 2, -15, -4, 0, 0);
        cube(root, "shape9", 52, 13, 0, 0, 0, 3, 3, 3, -3, -4, -0.5F, 0);
        cube(root, "shape10", 11, 60, 0, 0, 0, 6, 2, 2, -9, -3, 0, 0);
        cube(root, "shape11", 35, 50, 0, 0, 0, 2, 4, 3, -9, -1, -0.5F, 0);
        cube(root, "shape12", 12, 57, 0, 0, 0, 7, 1, 1, -7, 2, 0.5F, 0);
        cube(root, "shape13", 0, 51, 0, 0, 0, 4, 1, 4, 0, -5, -1, 0);
        cube(root, "shape14", 0, 43, 0, 0, 0, 3, 5, 2, 7, -7, 0, 0.7853982F);
        cube(root, "shape15", 0, 38, 0, 0, 0, 3, 1, 3, -9, 3, -0.5F, -2.792527F);
        cube(root, "shape16", 0, 17, 0, 0, 0, 2, 3, 1, -1, -2, 0.5F, 0.2617994F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void cube(PartDefinition root, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz, float zRot) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(px, py, pz, 0.0F, 0.0F, zRot));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
