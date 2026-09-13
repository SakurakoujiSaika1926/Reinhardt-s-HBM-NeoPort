package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyLauncherMissileEntity;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import java.util.List;

public final class LauncherMissileRenderer extends EntityRenderer<LegacyLauncherMissileEntity> {
    static final List<String> IDS = List.of("missile_test","missile_micro","missile_taint","missile_bhole",
            "missile_schrabidium","missile_emp","missile_generic","missile_incendiary","missile_cluster",
            "missile_buster","missile_decoy","missile_anti_ballistic","missile_stealth","missile_strong",
            "missile_incendiary_strong","missile_cluster_strong","missile_buster_strong","missile_emp_strong",
            "missile_burst","missile_inferno","missile_rain","missile_drill","missile_shuttle","missile_nuclear",
            "missile_nuclear_cluster","missile_volcano","missile_doomsday","missile_doomsday_rusted");
    public LauncherMissileRenderer(EntityRendererProvider.Context context) { super(context); }
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (String id : IDS) event.register(MachineModelRenderer.standalone("block/launcher_port/" + id));
    }
    public static float yaw(Direction facing) {
        return switch (facing) { case NORTH -> 90; case WEST -> 180; case SOUTH -> 270; case EAST -> 0;
            default -> throw new IllegalArgumentException("Vertical launcher orientation"); };
    }
    @Override public void render(LegacyLauncherMissileEntity entity, float yaw, float partial,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float heading = Mth.lerp(partial, entity.yRotO, entity.getYRot()) - 90;
        pose.mulPose(Axis.YP.rotationDegrees(heading));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.YN.rotationDegrees(heading));
        if (!(entity.missileItem().getItem() instanceof CustomMissileItem) && !entity.isAntiBallistic())
            pose.mulPose(Axis.YP.rotationDegrees(yaw(entity.launchFacing())));
        renderAssembly(entity.missileItem(), Blocks.AIR.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    public static void renderAssembly(ItemStack item, BlockState state, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        if (item.getItem() instanceof CustomMissileItem) {
            CustomMissileData m = CustomMissileData.read(item);
            if (m == null) throw new IllegalStateException("Invalid assembled missile render stack: " + item);
            MissileMultipartRenderer.render(state, pose, buffers, light, overlay, m.warhead(), m.fuselage(), m.fins(), m.thruster());
        } else if (item.getItem() instanceof LegacyMissileItem missile) {
            String id = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
            if (!IDS.contains(id)) throw new IllegalStateException("No legacy missile renderer: " + id);
            // ItemRenderMissileGeneric.generateLarge applies only to the five Tier 2 models.
            if (missile.tier() == LegacyMissileItem.Tier.TIER2) pose.scale(1.5F, 1.5F, 1.5F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MachineModelRenderer.standalone("block/launcher_port/" + id)),
                    pose, buffers, state, light, overlay);
        }
        pose.popPose();
    }
    public static void renderGui(GuiGraphics graphics, int left, int top, ItemStack item) {
        PoseStack pose = graphics.pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        pose.pushPose();
        if (item.getItem() instanceof CustomMissileItem) {
            CustomMissileData m = CustomMissileData.read(item);
            if (m == null) { pose.popPose(); return; }
            float height = MissileMultipartRenderer.height(m.warhead(), m.fuselage(), m.thruster());
            float scale = 90F / Math.max(height, 6F); // GUIMachine[CompactLauncher/LaunchTable], not auto-fit.
            pose.translate(left + 88, top + 115, 100);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.translate(height / 2 * scale, 0, 0);
            pose.scale(-scale, -scale, -scale);
        } else if (item.getItem() instanceof LegacyMissileItem missile) {
            float scale = switch (missile.formFactor()) {
                case ABM -> 1.45F; case MICRO -> 2.5F; case V2 -> 1.75F; case STRONG -> 1.375F;
                case HUGE -> .925F; case ATLAS -> .875F; case OTHER -> 1F;
            };
            if (BuiltInRegistries.ITEM.getKey(item.getItem()).equals(ReinhardtsHBM.id("missile_stealth"))) scale = 1.125F;
            pose.translate(left + 70, top + 120, 100);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(-8 * scale, -8 * scale, -8 * scale);
        } else { pose.popPose(); return; }
        renderAssembly(item, Blocks.AIR.defaultBlockState(), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        buffers.endBatch();
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(LegacyLauncherMissileEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
