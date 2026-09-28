package com.reinhardt.hbm.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable set of exact baked-model objects belonging to one resource generation. */
final class BakedModelGeneration {
    private final Map<ModelResourceLocation, BakedModel> models;

    private BakedModelGeneration(Map<ModelResourceLocation, BakedModel> models) {
        this.models = models;
    }

    static BakedModelGeneration capture(Iterable<ModelResourceLocation> locations) {
        Map<ModelResourceLocation, BakedModel> captured = new LinkedHashMap<>();
        for (ModelResourceLocation location : locations) {
            captured.put(location, MachineModelRenderer.model(location));
        }
        return new BakedModelGeneration(Map.copyOf(captured));
    }

    BakedModel model(ModelResourceLocation location) {
        BakedModel model = models.get(location);
        if (model == null) {
            throw new IllegalArgumentException("Model is not part of this baked generation: " + location);
        }
        return model;
    }
}
