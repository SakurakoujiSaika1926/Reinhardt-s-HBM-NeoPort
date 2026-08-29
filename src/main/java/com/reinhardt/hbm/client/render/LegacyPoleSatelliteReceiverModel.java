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

/** Exact ModelSatelliteReceiver geometry and UV layout from HBM 1.7.10. */
public final class LegacyPoleSatelliteReceiverModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("pole_satellite_receiver"), "main");

    private final ModelPart root;

    public LegacyPoleSatelliteReceiverModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        cube(root, "base", 0, 0, 0, 0, 0, 12, 16, 12, -6, 8, -6, 0, 0, 0);
        cube(root, "frame_top", 10, 28, 3, 9, -8, 8, 8, 2, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "frame_front", 0, 39, 3, 7, -10, 8, 2, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "frame_left", 0, 28, 1, 9, -10, 2, 8, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "frame_right", 0, 28, 11, 9, -10, 2, 8, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "frame_bottom", 0, 39, 3, 17, -10, 8, 2, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "mount", 0, 44, 6, 12, -11, 2, 2, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "pin", 0, 49, 6.5F, 12.5F, -14, 1, 1, 3, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        cube(root, "receiver", 0, 53, 6, 12, -16, 2, 2, 2, -3, 6, 0, -0.2617994F, -0.4363323F, 0);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void cube(PartDefinition root, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
