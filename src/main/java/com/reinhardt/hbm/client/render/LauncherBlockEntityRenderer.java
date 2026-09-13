package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import java.util.List;
import java.util.Set;

public class LauncherBlockEntityRenderer implements BlockEntityRenderer<LauncherBlockEntity> {
    public static final Set<String> ITEM_IDS = Set.of("compact_launcher", "launch_pad", "launch_pad_rusted", "launch_pad_large", "launch_table");
    public LauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (String model : List.of("launch_pad_world", "launch_pad_rusted_world", "compact_launcher_world", "launch_table_base",
                "launch_table_small_pad", "launch_table_large_pad", "launch_table_small_scaffold_base", "launch_table_small_scaffold_connector",
                "launch_table_small_scaffold_empty", "launch_table_large_scaffold_base", "launch_table_large_scaffold_connector", "launch_table_large_scaffold_empty"))
            event.register(MachineModelRenderer.standalone("block/" + model));
        event.register(MachineModelRenderer.standalone("block/launcher_port/pad_base"));
        for (String factor : List.of("abm", "micro", "v2", "strong", "huge", "atlas")) for (String part : List.of("pad", "erector", "pivot", "rope"))
            event.register(MachineModelRenderer.standalone("block/launcher_port/" + factor + "_" + part));
        LauncherMissileRenderer.registerAdditionalModels(event);
    }
    private static void draw(String path, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MachineModelRenderer.standalone("block/" + path)), pose, buffers, state, light, overlay);
    }
    @Override public void render(LauncherBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        pose.pushPose();
        pose.translate(.5, 0, .5);
        if (be.kind() != LauncherBlockEntity.Kind.COMPACT) pose.mulPose(Axis.YP.rotationDegrees(LauncherMissileRenderer.yaw(be.facing())));
        switch (be.kind()) {
            case PAD_SMALL, PAD_RUSTED -> {
                draw(be.kind() == LauncherBlockEntity.Kind.PAD_SMALL ? "launch_pad_world" : "launch_pad_rusted_world", state, pose, buffers, light, overlay);
                ItemStack missile = be.getItem(0);
                if (be.kind() == LauncherBlockEntity.Kind.PAD_RUSTED)
                    missile = be.missileLoaded() ? new ItemStack(BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id("missile_doomsday_rusted")).orElseThrow()) : ItemStack.EMPTY;
                pose.translate(0, 1, 0);
                LauncherMissileRenderer.renderAssembly(missile, state, pose, buffers, light, overlay);
            }
            case COMPACT -> {
                draw("compact_launcher_world", state, pose, buffers, light, overlay);
                pose.translate(0, 1.0625, 0);
                if (be.validMissile(be.getItem(0))) LauncherMissileRenderer.renderAssembly(be.getItem(0), state, pose, buffers, light, overlay);
            }
            case PAD_LARGE -> large(be, partial, state, pose, buffers, light, overlay);
            case TABLE -> table(be, state, pose, buffers, light, overlay);
            case SOYUZ -> throw new IllegalStateException("Soyuz has its own block entity and renderer");
        }
        pose.popPose();
    }
    private static void large(LauncherBlockEntity be, float partial, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw("launcher_port/pad_base", state, pose, buffers, light, overlay);
        if (be.formFactor() < 0) return;
        var factor = LegacyMissileItem.FormFactor.values()[be.formFactor()];
        String prefix = switch (factor) { case ABM, OTHER -> "abm"; case MICRO -> "micro"; case V2 -> "v2";
            case STRONG -> "strong"; case HUGE -> "huge"; case ATLAS -> "atlas"; };
        double z = switch (factor) { case ABM, MICRO, OTHER -> 1.5; case V2 -> 1.75; case STRONG, HUGE -> 3; case ATLAS -> 4; };
        double y = factor == LegacyMissileItem.FormFactor.ABM || factor == LegacyMissileItem.FormFactor.MICRO
                || factor == LegacyMissileItem.FormFactor.V2 || factor == LegacyMissileItem.FormFactor.OTHER ? 1.25 : 1.5;
        String path = "launcher_port/" + prefix + "_";
        draw(path + "pad", state, pose, buffers, light, overlay);
        if (!be.getItem(0).isEmpty() && be.erected()) draw(path + "rope", state, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0, y, -z);
        pose.mulPose(Axis.XP.rotationDegrees(-be.erector(partial)));
        pose.translate(0, -y, z);
        draw(path + "pivot", state, pose, buffers, light, overlay);
        pose.translate(0, be.lift(partial), 0);
        draw(path + "erector", state, pose, buffers, light, overlay);
        if (be.erected()) { pose.popPose(); pose.pushPose(); }
        if (!be.getItem(0).isEmpty() && (be.erected() || be.readyToLoad())) {
            pose.translate(0, 2, 0);
            LauncherMissileRenderer.renderAssembly(be.getItem(0), state, pose, buffers, light, overlay);
        }
        pose.popPose();
    }
    private static void table(LauncherBlockEntity be, BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw("launch_table_base", state, pose, buffers, light, overlay);
        draw(be.tableSize() == MissilePartItem.Size.SIZE_20 ? "launch_table_large_pad" : "launch_table_small_pad", state, pose, buffers, light, overlay);
        var missile = CustomMissileData.read(be.getItem(0));
        if (missile != null) be.setScaffoldHeight((int)MissileMultipartRenderer.height(missile.warhead(), missile.fuselage(), missile.thruster()));
        int connector = (int)(be.scaffoldHeight() * .75);
        boolean small = be.tableSize() == MissilePartItem.Size.SIZE_10;
        String prefix = small ? "launch_table_small_scaffold_" : "launch_table_large_scaffold_";
        pose.pushPose();
        if (small) pose.translate(0, 0, -1);
        pose.translate(0, 1, 3.5);
        for (int i = 0; i <= be.scaffoldHeight(); i++) {
            String part = i < connector ? "base" : i > connector ? "empty" : be.validMissile(be.getItem(0)) ? "connector" : "base";
            draw(prefix + part, state, pose, buffers, light, overlay);
            pose.translate(0, 1, 0);
        }
        pose.popPose();
        pose.translate(0, 2.0625, 0);
        if (be.validMissile(be.getItem(0))) LauncherMissileRenderer.renderAssembly(be.getItem(0), state, pose, buffers, light, overlay);
    }
    /** Match the legacy 1.7.10 inventory pose, then apply each launcher's own matrices. */
    static void renderItem(String id, BlockState state, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, pose);
        if (context == ItemDisplayContext.GUI) {
            switch (id) {
                case "launch_pad", "launch_pad_rusted" -> { pose.translate(0, -1, 0); pose.scale(3, 3, 3); }
                case "compact_launcher" -> { pose.translate(0, -4, 0); pose.scale(3.5F, 3.5F, 3.5F); }
                case "launch_pad_large" -> { pose.translate(0, -3.75, 0); pose.scale(1.625F, 1.625F, 1.625F); }
                case "launch_table" -> { pose.translate(0, -2, 0); pose.scale(2.5F, 2.5F, 2.5F); }
                default -> throw new IllegalArgumentException(id);
            }
        }
        switch (id) {
            case "launch_pad", "launch_pad_rusted" -> draw(id + "_world", state, pose, buffers, light, overlay);
            case "compact_launcher" -> { pose.scale(.5F, .5F, .5F); draw("compact_launcher_world", state, pose, buffers, light, overlay); }
            case "launch_pad_large" -> {
                pose.scale(.5F, .5F, .5F); pose.mulPose(Axis.YP.rotationDegrees(90));
                for (String part : List.of("pad_base", "atlas_pad", "atlas_erector", "atlas_pivot"))
                    draw("launcher_port/" + part, state, pose, buffers, light, overlay);
            }
            case "launch_table" -> {
                pose.scale(.5F, .5F, .5F);
                draw("launch_table_base", state, pose, buffers, light, overlay);
                draw("launch_table_small_pad", state, pose, buffers, light, overlay);
                pose.translate(0, 0, 2.5);
                for (int i = 0; i < 8; i++) {
                    pose.translate(0, 1, 0);
                    draw("launch_table_small_scaffold_" + (i < 6 ? "base" : i == 6 ? "connector" : "empty"), state, pose, buffers, light, overlay);
                }
            }
            default -> throw new IllegalArgumentException(id);
        }
        pose.popPose();
    }
    @Override public int getViewDistance() { return 256; }
    @Override public AABB getRenderBoundingBox(LauncherBlockEntity be) {
        var pos = be.getBlockPos();
        return switch (be.kind()) {
            case PAD_LARGE -> new AABB(pos.getX()-10, pos.getY(), pos.getZ()-10, pos.getX()+11, pos.getY()+15, pos.getZ()+11);
            case TABLE -> new AABB(pos.getX()-5, pos.getY(), pos.getZ()-5, pos.getX()+6, pos.getY()+Math.max(15,be.scaffoldHeight()+2), pos.getZ()+6);
            default -> new AABB(pos.getX()-2, pos.getY(), pos.getZ()-2, pos.getX()+3, pos.getY()+15, pos.getZ()+3);
        };
    }
}
