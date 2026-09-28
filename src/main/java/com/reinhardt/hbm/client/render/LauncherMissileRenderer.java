package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyLauncherMissileEntity;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LauncherMissileRenderer extends EntityRenderer<LegacyLauncherMissileEntity> {
    private static final Map<String, Double> VISUAL_RADII = new ConcurrentHashMap<>();
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
    private static void applyLegacyMissileEntityPose(PoseStack pose, Direction facing) {
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Legacy launcher missile requires a horizontal launch facing: " + facing);
        }
    }
    @Override public void render(LegacyLauncherMissileEntity entity, float yaw, float partial,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float heading = Mth.lerp(partial, entity.yRotO, entity.getYRot()) - 90;
        pose.mulPose(Axis.YP.rotationDegrees(heading));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.YN.rotationDegrees(heading));
        if (!(entity.missileItem().getItem() instanceof CustomMissileItem) && !entity.isAntiBallistic()) {
            applyLegacyMissileEntityPose(pose, entity.launchFacing());
        }
        renderAssembly(entity.missileItem(), Blocks.AIR.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    @Override public boolean shouldRender(LegacyLauncherMissileEntity entity, Frustum frustum,
                                          double cameraX, double cameraY, double cameraZ) {
        ItemStack missile = entity.missileItem();
        String signature = visualSignature(missile);
        double radius = VISUAL_RADII.computeIfAbsent(signature,
                ignored -> captureVisualRadius(missile));
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), radius);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
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

    static void renderAssembly(LauncherBlockEntity.MissileRenderData missile, BlockState state, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay) {
        if (!missile.present()) {
            return;
        }
        pose.pushPose();
        if (missile.custom()) {
            MissileMultipartRenderer.render(state, pose, buffers, light, overlay, missile);
        } else if (IDS.contains(missile.legacyId())) {
            if (missile.legacyTierTwo()) {
                pose.scale(1.5F, 1.5F, 1.5F);
            }
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(MachineModelRenderer.standalone(
                            "block/launcher_port/" + missile.legacyId())),
                    pose, buffers, state, light, overlay);
        }
        pose.popPose();
    }
    static void invalidateEntityBounds() { VISUAL_RADII.clear(); }
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

    private static String visualSignature(ItemStack missile) {
        if (missile.getItem() instanceof CustomMissileItem) {
            CustomMissileData data = CustomMissileData.read(missile);
            if (data == null) return "invalid-custom";
            return "custom|" + partId(data.warhead()) + '|' + partId(data.fuselage())
                    + '|' + partId(data.fins()) + '|' + partId(data.thruster());
        }
        return BuiltInRegistries.ITEM.getKey(missile.getItem()).toString();
    }

    private static double captureVisualRadius(ItemStack missile) {
        AABB bounds = null;
        if (missile.getItem() instanceof CustomMissileItem) {
            CustomMissileData data = CustomMissileData.read(missile);
            if (data == null) return 0.0D;
            float thrusterHeight = MissileMultipartRenderer.partHeight(partId(data.thruster()));
            float fuselageHeight = MissileMultipartRenderer.partHeight(partId(data.fuselage()));
            bounds = includePart(bounds, data.thruster(), 0.0F);
            bounds = includePart(bounds, data.fins(), thrusterHeight);
            bounds = includePart(bounds, data.fuselage(), thrusterHeight);
            bounds = includePart(bounds, data.warhead(), thrusterHeight + fuselageHeight);
        } else if (missile.getItem() instanceof LegacyMissileItem legacy) {
            String id = BuiltInRegistries.ITEM.getKey(missile.getItem()).getPath();
            if (!IDS.contains(id)) return 0.0D;
            PoseStack poseStack = new PoseStack();
            if (legacy.tier() == LegacyMissileItem.Tier.TIER2) {
                poseStack.scale(1.5F, 1.5F, 1.5F);
            }
            bounds = captureModelBounds(
                    MachineModelRenderer.standalone("block/launcher_port/" + id), poseStack);
        }
        return bounds == null ? 0.0D : rotationSafeRadius(boundsRadius(bounds));
    }

    private static AABB includePart(AABB accumulated, ItemStack part, float yOffset) {
        if (part.isEmpty()) return accumulated;
        String id = partId(part);
        if (!MissileMultipartRenderer.partIds().contains(id)) return accumulated;
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0F, yOffset, 0.0F);
        AABB partBounds = captureModelBounds(MissileMultipartRenderer.model(id), poseStack);
        return accumulated == null ? partBounds : accumulated.minmax(partBounds);
    }

    private static AABB captureModelBounds(net.minecraft.client.resources.model.ModelResourceLocation model,
                                           PoseStack poseStack) {
        return BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(model), Blocks.AIR.defaultBlockState(),
                List.of(poseStack), BlockPos.ZERO).bounds();
    }

    private static String partId(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static double boundsRadius(AABB bounds) {
        double maximumSquared = 0.0D;
        for (double x : new double[] {bounds.minX, bounds.maxX}) {
            for (double y : new double[] {bounds.minY, bounds.maxY}) {
                for (double z : new double[] {bounds.minZ, bounds.maxZ}) {
                    maximumSquared = Math.max(maximumSquared, x * x + y * y + z * z);
                }
            }
        }
        return Math.sqrt(maximumSquared);
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
    @Override public ResourceLocation getTextureLocation(LegacyLauncherMissileEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
