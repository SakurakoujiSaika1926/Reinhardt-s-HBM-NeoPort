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

/** Pixel-for-pixel ModelHunterChopper geometry converted from its 1.7.10 Techne source. */
public final class LegacyChopperModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ReinhardtsHBM.id("hunter_chopper"), "main");
    private final ModelPart root;
    private final ModelPart rotorBlades;
    private final ModelPart torsoRotorBlades;
    private final ModelPart tailRotorBlades;

    public LegacyChopperModel(ModelPart root) {
        this.root = root;
        rotorBlades = root.getChild("RotorBlades");
        torsoRotorBlades = root.getChild("TorsoRotorBlades");
        tailRotorBlades = root.getChild("TailRotorBlades");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        part(root, "RotorPivotStem", 40, 22, 0, 0, 0, 1, 4, 1, -0.5F, 0, -0.5F, 0, 0, 0);
        part(root, "RotorPivotTop", 40, 27, 0, 0, 0, 3, 1, 3, -1.5F, -1, -1.5F, 0, 0, 0);
        part(root, "RotorPivotPlate", 40, 31, 0, 0, 0, 6, 1, 6, -3, 1.5F, -3, 0, 0, 0);
        part(root, "TorsoBaseCenter", 70, 0, 0, 0, 0, 14, 4, 2, -8, 4, -1, 0, 0, 0);
        part(root, "TorsoPlateLeft", 70, 6, 0, -4, 0, 14, 4, 1, -8, 8, -2, -0.2268928F, 0, 0);
        part(root, "TorsoBaseBottom", 70, 11, 0, 0, 0, 7, 2, 4, -4, 8, -2, 0, 0, 0);
        part(root, "TorsoPlateRight", 70, 17, 0, -4, -1, 14, 4, 1, -8, 8, 2, 0.2268928F, 0, 0);
        part(root, "TorsoPlateBottom", 70, 22, -5, -2, 0, 5, 2, 4, -4, 10, -2, 0, 0, 0.2094395F);
        part(root, "WingLeftPlate", 110, 0, 0, -3, 0, 9, 3, 1, -8, 9, -3, -0.2268928F, 0, 0);
        part(root, "WingRightPlate", 130, 0, 0, -3, 0, 9, 3, 1, -8, 9, 2, 0.2268928F, 0, 0);
        part(root, "WingLeft", 110, 4, 0, 0, 0, 3, 1, 6, -3, 10, -8, 0.3490659F, 0, 0);
        part(root, "WingLeftFront", 110, 11, 0, 0, 0, 2, 1, 7, -3, 10, -8, 0.3490659F, -0.3490659F, -0.1745329F);
        part(root, "WingLeftTip", 110, 19, 0, 0, 0, 5, 2, 1, -4, 9, -8, 0, 0, 0);
        part(root, "WingRight", 130, 4, 0, 0, -6, 3, 1, 6, -3, 10, 8, -0.3490659F, 0, 0);
        part(root, "WingRightFront", 130, 11, 0, 0, -7, 2, 1, 7, -3, 10, 8, -0.3490659F, 0.3490659F, -0.1745329F);
        part(root, "WingRightTip", 130, 19, 0, 0, 0, 5, 2, 1, -4, 9, 7, 0, 0, 0);
        part(root, "TorsoBaseBack", 70, 28, 0, 0, 0, 3, 2, 3, 3, 7.5F, -1.5F, 0, 0, 0);
        part(root, "TorsoBoxBottom", 70, 33, 0, -2, 0, 7, 2, 2, -3, 10, -1, 0, 0, 0.1570796F);
        part(root, "TorsoPlateBack", 70, 37, 0, 0, 0, 3, 1, 2, 6, 4, -1, 0, 0, 0.2268928F);
        part(root, "TorsoBoxBack", 70, 40, 0, 0, 0, 2, 4, 2, 6, 5, -1, 0, 0, 0);
        part(root, "TorsoPlateLeftBack", 70, 46, 0, -4, -1, 3, 4, 1, 6, 8.5F, -1, -0.2268928F, 0, 0);
        part(root, "TorsoPlateRightBack", 70, 51, 0, -4, 0, 3, 4, 1, 6, 8.5F, 1, 0.2268928F, 0, 0);
        part(root, "TailFrontBase", 24, 54, 0, 0, 0, 5, 2, 2, 8, 6, -1, 0, 0, 0);
        part(root, "TailFrontPlate", 24, 58, -5, 0, 0, 5, 1, 2, 13, 6, -1, 0, 0, 0.2268928F);
        part(root, "TailBackBase", 24, 61, 0, 0, 0, 4, 2, 1, 13, 6, -0.5F, 0, 0, 0);
        part(root, "TailRotorFront", 24, 64, 0, 0, 0, 1, 3, 1, 15.5F, 8, -0.5F, 0, 0, -0.2268928F);
        part(root, "TailRotorTop", 24, 68, 0, 0, 0, 3, 1, 1, 17, 6, -0.5F, 0, 0, 0);
        part(root, "TailRotorBack", 24, 70, 0, 0, 0, 1, 4, 1, 20, 6, -0.5F, 0, 0, 0);
        part(root, "TailRotorBottom", 24, 75, 0, 0, 0, 3, 1, 1, 18, 10, -0.5F, 0, 0, 0);
        part(root, "TailRotorBlades", 120, 120, -1.5F, -1.5F, 0, 3, 3, 0, 18.5F, 8.5F, 0, 0, 0, 0);
        part(root, "TailRotorPivot", 24, 77, 0, 0, 0, 1, 2, 1, 18, 8, -0.5F, 0, 0, 0);
        part(root, "HeadNeck", 0, 40, -1, 0, 0, 1, 6, 3, -7, 4, -1.5F, 0, 0, 0.2268928F);
        part(root, "HeadBack", 0, 49, 0, 0, 0, 1, 7, 4, -8.5F, 3.5F, -2, 0, 0, 0.2268928F);
        part(root, "HeadBase", 0, 60, -2, 1, 0, 2, 6, 4, -8.5F, 3.5F, -2, 0, 0, 0.2268928F);
        part(root, "HeadTop", 0, 70, -2, 0, 0, 2, 2, 4, -8.5F, 3.5F, -2, 0, 0, -0.2268928F);
        part(root, "HeadFront", 0, 76, 0, 0, 0, 2, 4, 2, -13, 5, -1, 0, 0, 0);
        part(root, "HeadLeft", 0, 82, -3, 0, 0, 3, 4, 1, -10, 5, -2, 0, 0.3490659F, 0);
        part(root, "HeadRight", 0, 87, -3, 0, -1, 3, 4, 1, -10, 5, 2, 0, -0.3490659F, 0);
        part(root, "HeadFrontTop", 0, 92, -3, 0, 0, 3, 1, 2, -10.5F, 4, -1, 0, 0, -0.3490659F);
        part(root, "TorsoRotorBottom", 0, 0, 0, 0, 0, 3, 1, 1, -7, 11.5F, -0.5F, 0, 0, 0);
        part(root, "TorsoRotorFront", 0, 2, 0, 0, 0, 1, 3, 1, -8, 9, -0.5F, 0, 0, 0);
        part(root, "TorsoRotorBack", 0, 6, 0, 0, 0, 1, 2, 1, -4, 10, -0.5F, 0, 0, 0);
        part(root, "TorsoRotorBlades", 112, 120, -1.5F, -1.5F, 0, 3, 3, 0, -5.5F, 10, 0, 0, 0, 0);
        part(root, "TorsoRotorPivot", 0, 9, 0, 0, 0, 1, 2, 1, -6, 8.5F, -0.5F, 0, 0, 0);
        part(root, "RotorBlades", 76, 68, -30, 0, -30, 60, 0, 60, 0, 1.5F, 0, 0, 0, 0);
        part(root, "Antenna1", 0, 95, 0, 0, 0, 4, 1, 1, -14, 4, 0.5F, 0, 0, 0);
        part(root, "Antenna2", 0, 97, 0, 0, 0, 2, 1, 1, -15, 7, 0, 0, 0, 0);
        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void part(PartDefinition root, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        float time = (System.currentTimeMillis() % 360_000L) / (1000.0F / 60.0F);
        rotorBlades.yRot = time * 0.5F;
        torsoRotorBlades.zRot = time * 0.5F;
        tailRotorBlades.zRot = time * 0.5F;
        root.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
