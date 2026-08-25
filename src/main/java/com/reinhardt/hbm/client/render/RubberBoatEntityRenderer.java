package com.reinhardt.hbm.client.render;

import com.mojang.datafixers.util.Pair;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/** Reuses vanilla boat animation and water masking with the legacy rubber texture. */
public final class RubberBoatEntityRenderer extends BoatRenderer {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/boat_rubber.png");

    public RubberBoatEntityRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return Pair.of(TEXTURE, super.getModelWithLocation(boat).getSecond());
    }
}
