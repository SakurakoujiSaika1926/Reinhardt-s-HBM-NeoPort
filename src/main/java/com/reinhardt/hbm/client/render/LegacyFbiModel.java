package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyFbiEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/** ModelFBI is the plain 1.7.10 ModelBiped with a 64x32 texture canvas. */
public final class LegacyFbiModel extends HumanoidModel<LegacyFbiEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ReinhardtsHBM.id("fbi"), "main");

    public LegacyFbiModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(0.0F), 0.0F), 64, 32);
    }
}
