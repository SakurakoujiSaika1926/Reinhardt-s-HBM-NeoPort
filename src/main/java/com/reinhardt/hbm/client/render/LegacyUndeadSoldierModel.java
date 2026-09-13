package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * Direct conversion of HBM 1.7.10 ModelSkeletonNT.
 *
 * <p>The old model inherited the zombie body/head, then replaced all four
 * limbs with 2x12x2 cuboids.  This is deliberately a separate layer instead
 * of a shared scale or transform so the skeleton variant keeps the authored
 * legacy geometry.</p>
 */
public final class LegacyUndeadSoldierModel extends HumanoidModel<LegacyUndeadSoldierEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("undead_soldier"), "main");

    public LegacyUndeadSoldierModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F);
        var root = mesh.getRoot();

        // ModelSkeletonNT's exact limb geometry and rotation points.
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
