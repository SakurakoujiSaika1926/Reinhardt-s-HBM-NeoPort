package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact OBJ grouping and transforms from MissilePart, MissilePronter and GUIMachineMissileAssembly. */
public final class MissileMultipartRenderer {
    private static final Map<String, Geometry> PARTS = new LinkedHashMap<>();

    static {
        part(1.0F, 1.0F, "mp_thruster_10_kerosene");
        part(0.5F, 1.0F, "mp_thruster_10_solid", "mp_thruster_10_xenon", "mp_thruster_15_solid", "mp_thruster_15_solid_hexdecuple", "mp_thruster_20_solid_multi", "mp_thruster_20_solid_multier");
        part(1.5F, 1.5F, "mp_thruster_15_kerosene", "mp_thruster_15_hydrogen");
        part(1.0F, 1.5F, "mp_thruster_15_kerosene_dual", "mp_thruster_15_kerosene_triple", "mp_thruster_15_hydrogen_dual");
        part(2.0F, 2.0F, "mp_thruster_15_balefire_short", "mp_thruster_20_kerosene_dual", "mp_thruster_20_kerosene_triple");
        part(3.0F, 2.5F, "mp_thruster_15_balefire", "mp_thruster_15_balefire_large", "mp_thruster_15_balefire_large_rad", "mp_thruster_20_kerosene");
        part(1.0F, 1.75F, "mp_thruster_20_solid");

        part(0.0F, 2.0F, "mp_stability_10_flat", "mp_stability_10_space");
        part(0.0F, 3.0F, "mp_stability_10_cruise", "mp_stability_15_flat", "mp_stability_15_thin", "mp_stability_15_soyuz", "mp_s_20");

        part(4.0F, 3.0F,
                "mp_fuselage_10_kerosene", "mp_fuselage_10_kerosene_camo", "mp_fuselage_10_kerosene_desert", "mp_fuselage_10_kerosene_sky", "mp_fuselage_10_kerosene_insulation", "mp_fuselage_10_kerosene_flames", "mp_fuselage_10_kerosene_sleek", "mp_fuselage_10_kerosene_metal", "mp_fuselage_10_kerosene_taint",
                "mp_fuselage_10_solid", "mp_fuselage_10_solid_flames", "mp_fuselage_10_solid_insulation", "mp_fuselage_10_solid_sleek", "mp_fuselage_10_solid_soviet_glory", "mp_fuselage_10_solid_cathedral", "mp_fuselage_10_solid_moonlit", "mp_fuselage_10_solid_battery", "mp_fuselage_10_solid_duracell", "mp_fuselage_10_xenon", "mp_fuselage_10_xenon_bhole");
        part(7.0F, 5.0F,
                "mp_fuselage_10_long_kerosene", "mp_fuselage_10_long_kerosene_camo", "mp_fuselage_10_long_kerosene_desert", "mp_fuselage_10_long_kerosene_sky", "mp_fuselage_10_long_kerosene_flames", "mp_fuselage_10_long_kerosene_insulation", "mp_fuselage_10_long_kerosene_sleek", "mp_fuselage_10_long_kerosene_metal", "mp_fuselage_10_long_kerosene_dash", "mp_fuselage_10_long_kerosene_taint", "mp_fuselage_10_long_kerosene_vap",
                "mp_fuselage_10_long_solid", "mp_fuselage_10_long_solid_flames", "mp_fuselage_10_long_solid_insulation", "mp_fuselage_10_long_solid_sleek", "mp_fuselage_10_long_solid_soviet_glory", "mp_fuselage_10_long_solid_bullet", "mp_fuselage_10_long_solid_silvermoonlight");
        part(9.0F, 5.5F, "mp_fuselage_10_15_kerosene", "mp_fuselage_10_15_solid", "mp_fuselage_10_15_hydrogen", "mp_fuselage_10_15_balefire");
        part(10.0F, 6.0F,
                "mp_fuselage_15_kerosene", "mp_fuselage_15_kerosene_camo", "mp_fuselage_15_kerosene_desert", "mp_fuselage_15_kerosene_sky", "mp_fuselage_15_kerosene_insulation", "mp_fuselage_15_kerosene_metal", "mp_fuselage_15_kerosene_decorated", "mp_fuselage_15_kerosene_steampunk", "mp_fuselage_15_kerosene_polite", "mp_fuselage_15_kerosene_blackjack", "mp_fuselage_15_kerosene_lambda", "mp_fuselage_15_kerosene_minuteman", "mp_fuselage_15_kerosene_pip", "mp_fuselage_15_kerosene_taint", "mp_fuselage_15_kerosene_yuck",
                "mp_fuselage_15_solid", "mp_fuselage_15_solid_insulation", "mp_fuselage_15_solid_desh", "mp_fuselage_15_solid_soviet_glory", "mp_fuselage_15_solid_soviet_stank", "mp_fuselage_15_solid_faust", "mp_fuselage_15_solid_silvermoonlight", "mp_fuselage_15_solid_snowy", "mp_fuselage_15_solid_panorama", "mp_fuselage_15_solid_roses", "mp_fuselage_15_solid_mimi", "mp_fuselage_15_hydrogen", "mp_fuselage_15_hydrogen_cathedral", "mp_fuselage_15_balefire");
        part(16.0F, 10.0F, "mp_fuselage_15_20_kerosene", "mp_fuselage_15_20_kerosene_magnusson", "mp_fuselage_15_20_solid");

        part(2.0F, 1.5F, "mp_warhead_10_he", "mp_warhead_10_nuclear", "mp_warhead_15_he", "mp_warhead_15_incendiary");
        part(2.5F, 2.0F, "mp_warhead_10_incendiary", "mp_warhead_10_nuclear_large");
        part(0.5F, 1.0F, "mp_warhead_10_buster");
        part(2.25F, 1.5F, "mp_warhead_10_taint", "mp_warhead_10_cloud");
        part(3.5F, 2.0F, "mp_warhead_15_nuclear", "mp_warhead_15_nuclear_shark", "mp_warhead_15_nuclear_mimi");
        part(2.25F, 7.5F, "mp_warhead_15_boxcar");
        part(3.0F, 2.0F, "mp_warhead_15_n2");
        part(2.75F, 2.0F, "mp_warhead_15_balefire");
        part(2.25F, 2.0F, "mp_warhead_15_turbine");
    }

    private MissileMultipartRenderer() {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        PARTS.keySet().forEach(id -> event.register(model(id)));
    }

    public static float height(ItemStack warhead, ItemStack fuselage, ItemStack thruster) {
        return geometry(warhead).height() + geometry(fuselage).height() + geometry(thruster).height();
    }

    public static void render(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              int packedOverlay, ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        renderPart(state, poseStack, bufferSource, packedLight, packedOverlay, thruster);
        poseStack.translate(0.0F, geometry(thruster).height(), 0.0F);
        renderPart(state, poseStack, bufferSource, packedLight, packedOverlay, fins);
        renderPart(state, poseStack, bufferSource, packedLight, packedOverlay, fuselage);
        poseStack.translate(0.0F, geometry(fuselage).height(), 0.0F);
        renderPart(state, poseStack, bufferSource, packedLight, packedOverlay, warhead);
    }

    /** Direct matrix port of GUIMachineMissileAssembly's rotating OBJ preview. */
    public static void renderGui(GuiGraphics graphics, int centerX, int centerY, BlockState state,
                                 ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        float height = height(warhead, fuselage, thruster);
        if (height <= 0.0F) {
            return;
        }
        PoseStack poseStack = graphics.pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        float scale = 144.0F / Math.max(height, 6.0F);
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 100.0F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians((System.currentTimeMillis() / 10L) % 360L), 0.0F, -1.0F, 0.0F)));
        poseStack.translate(height * scale / 2.0F, 0.0F, 0.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-90.0F), 0.0F, 0.0F, 1.0F)));
        poseStack.scale(-1.0F, -1.0F, -1.0F);
        render(state, poseStack, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, warhead, fuselage, fins, thruster);
        buffers.endBatch();
        poseStack.popPose();
    }

    private static Geometry geometry(ItemStack stack) {
        if (stack.isEmpty()) {
            return Geometry.EMPTY;
        }
        return PARTS.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), Geometry.EMPTY);
    }

    private static void renderPart(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, ItemStack stack) {
        if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (PARTS.containsKey(id)) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model(id)), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
    }

    private static ModelResourceLocation model(String id) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("block/missile_parts/" + id));
    }

    private static void part(float height, float guiHeight, String... ids) {
        for (String id : ids) {
            PARTS.put(id, new Geometry(height, guiHeight));
        }
    }

    private record Geometry(float height, float guiHeight) {
        private static final Geometry EMPTY = new Geometry(0.0F, 0.0F);
    }
}
