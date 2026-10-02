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

/** Exact cuboids and pivots from the current 1.7.10 {@code ModelStatue}. */
public final class LegacyStatueModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("statue_elb_f"), "statue");

    private final ModelPart root;

    public LegacyStatueModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        cube(root, "base", 0, 0, 0, 0, 0, 16, 8, 16, -8, 16, -8, 0, 0, 0);
        cube(root, "leg_left", 0, 24, 0, 0, 0, 4, 12, 4, -4, 4, -2, 0, 0, 0);
        cube(root, "leg_right", 16, 24, 0, 0, 0, 4, 12, 4, 0, 4, -2, 0, 0, 0);
        cube(root, "body", 32, 40, 0, 0, 0, 8, 12, 4, -4, -8, -2, 0, 0, 0);
        cube(root, "arm_right_upper", 0, 40, 0, 0, -2, 4, 8, 4, 4, -8, 0, 0.5235988F, 0, 0);
        cube(root, "arm_left_upper", 16, 40, -4, 0, -2, 4, 8, 4, -4, -8, 0, -0.0872665F, 0, 0);
        cube(root, "arm_right_lower", 0, 52, -2, 0, -2, 4, 8, 4, 6, -2, 3, 1.22173F, 0, 0);
        cube(root, "arm_left_lower", 16, 52, 0, 0, -2, 4, 8, 4, -8, -1, -0.5F, 0.2617994F, 0, 0);
        cube(root, "head", 32, 24, -4, -8, -4, 8, 8, 8, 0, -8, 0, -0.1745329F, 0, 0);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void cube(PartDefinition root, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
