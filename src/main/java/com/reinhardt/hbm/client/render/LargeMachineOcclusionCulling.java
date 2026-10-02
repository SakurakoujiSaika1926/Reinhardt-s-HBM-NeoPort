package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmClientConfig;
import com.reinhardt.hbm.mixin.client.LevelRendererAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Restores vanilla section occlusion for cross-section HBM block entities.
 *
 * <p>Those renderers must live in LevelRenderer's global block-entity set so
 * geometry outside the owner's section does not disappear. Global entries do
 * not otherwise participate in the section occlusion graph. The snapshot is
 * active only during the normal block-entity render pass and is conservative:
 * a machine is rejected only if none of the sections touched by its complete,
 * audited render AABB survived vanilla's visibility traversal.</p>
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class LargeMachineOcclusionCulling {
    private static final LongOpenHashSet VISIBLE_SECTIONS = new LongOpenHashSet();
    private static ClientLevel snapshotLevel;
    private static boolean active;

    private LargeMachineOcclusionCulling() {
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            captureVisibleSections(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            active = false;
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    static boolean isPotentiallyVisible(BlockEntity blockEntity, AABB bounds, Vec3 cameraPosition) {
        if (!active
                || blockEntity.getLevel() != snapshotLevel
                || VISIBLE_SECTIONS.isEmpty()) {
            return true;
        }
        if (bounds.contains(cameraPosition)) {
            return true;
        }

        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minX, bounds.maxX)));
        int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minY, bounds.maxY)));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minZ, bounds.maxZ)));

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    if (VISIBLE_SECTIONS.contains(SectionPos.asLong(sectionX, sectionY, sectionZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void captureVisibleSections(RenderLevelStageEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(event.getLevelRenderer() instanceof LevelRendererAccessor accessor)) {
            reset();
            return;
        }

        VISIBLE_SECTIONS.clear();
        for (SectionRenderDispatcher.RenderSection section : accessor.hbm$getVisibleSections()) {
            BlockPos origin = section.getOrigin();
            VISIBLE_SECTIONS.add(SectionPos.asLong(origin));
        }
        snapshotLevel = level;
        active = HbmClientConfig.ENABLE_LARGE_MACHINE_OCCLUSION_CULLING.get();
    }

    private static void reset() {
        VISIBLE_SECTIONS.clear();
        snapshotLevel = null;
        active = false;
    }

    /** AABB maxima are exclusive; keep an exact section boundary in the section below it. */
    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }
}
