package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.LauncherStructCoreBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Exact 1.7.10 compact- and large-launcher construction projections. */
public final class LauncherStructCoreBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<LauncherStructCoreBlockEntity> {
    private static final int COMPACT_COMPONENT_COUNT = 8;
    private static final int LARGE_COMPONENT_COUNT = 91;
    private static final Set<LauncherStructCoreBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile ConstructionPreviewMesh.Geometry compactGeometry;
    private static volatile List<ConstructionPreviewMesh.Geometry> largeGeometries = List.of();
    private static volatile AABB largeLocalBounds;
    private static int nextGeneration;

    private final ConstructionPreviewMesh compactMesh = new ConstructionPreviewMesh();
    private final List<ConstructionPreviewMesh> largeMeshes = List.of(
            new ConstructionPreviewMesh(),
            new ConstructionPreviewMesh(),
            new ConstructionPreviewMesh(),
            new ConstructionPreviewMesh());

    public LauncherStructCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    @Override
    public void render(LauncherStructCoreBlockEntity core, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (core.isLarge()) {
            int phase = scaffoldPhase();
            List<ConstructionPreviewMesh.Geometry> geometries = largeGeometries;
            ConstructionPreviewMesh.Geometry geometry = geometries.size() == 4
                    ? geometries.get(phase) : null;
            this.largeMeshes.get(phase).render(geometry, poseStack, packedOverlay);
        } else {
            this.compactMesh.render(compactGeometry, poseStack, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(LauncherStructCoreBlockEntity core) {
        if (core.isLarge() && largeLocalBounds != null) {
            return largeLocalBounds.move(core.getBlockPos());
        }
        ConstructionPreviewMesh.Geometry compact = compactGeometry;
        if (!core.isLarge() && compact != null) {
            return compact.boundsAt(core.getBlockPos());
        }
        BlockPos pos = core.getBlockPos();
        return core.isLarge()
                ? RenderBounds.localAnalytic(pos, -4.0D, 0.0D, -4.0D, 5.0D, 12.0D, 5.0D)
                : RenderBounds.localAnalytic(pos, -1.0D, 0.0D, -1.0D, 2.0D, 1.0D, 2.0D);
    }

    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(LauncherStructCoreBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        int generation = ++nextGeneration;
        compactGeometry = captureCompact(generation);
        List<ConstructionPreviewMesh.Geometry> large = new ArrayList<>(4);
        AABB union = null;
        for (int phase = 0; phase < 4; phase++) {
            ConstructionPreviewMesh.Geometry geometry = captureLarge(generation, phase);
            large.add(geometry);
            union = union == null ? geometry.localBounds() : union.minmax(geometry.localBounds());
        }
        largeGeometries = List.copyOf(large);
        largeLocalBounds = union;
        closeAllBuffers();
    }

    static void invalidateModelCache() {
        closeAllBuffers();
    }

    private static ConstructionPreviewMesh.Geometry captureCompact(int generation) {
        BlockState launcher = HbmBlocks.STRUCT_LAUNCHER.get().defaultBlockState();
        List<ConstructionPreviewMesh.PlacementSpec> placements =
                new ArrayList<>(COMPACT_COMPONENT_COUNT);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    placements.add(ConstructionPreviewMesh.placement(launcher, x, 0, z));
                }
            }
        }
        requireCount("compact launcher", placements, COMPACT_COMPONENT_COUNT);
        return ConstructionPreviewMesh.capture(generation, placements);
    }

    private static ConstructionPreviewMesh.Geometry captureLarge(int generation, int phase) {
        BlockState launcher = HbmBlocks.STRUCT_LAUNCHER.get().defaultBlockState();
        BlockState scaffold = HbmBlocks.STRUCT_SCAFFOLD.get().defaultBlockState();
        List<ConstructionPreviewMesh.PlacementSpec> placements =
                new ArrayList<>(LARGE_COMPONENT_COUNT);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x != 0 || z != 0) {
                    placements.add(ConstructionPreviewMesh.placement(launcher, x, 0, z));
                }
            }
        }
        int scaffoldX = switch (phase) {
            case 0 -> 3;
            case 1, 3 -> 0;
            case 2 -> -3;
            default -> throw new IllegalArgumentException("Invalid launcher preview phase " + phase);
        };
        int scaffoldZ = switch (phase) {
            case 0, 2 -> 0;
            case 1 -> 3;
            case 3 -> -3;
            default -> throw new IllegalArgumentException("Invalid launcher preview phase " + phase);
        };
        for (int y = 1; y < 12; y++) {
            placements.add(ConstructionPreviewMesh.placement(scaffold, scaffoldX, y, scaffoldZ));
        }
        requireCount("large launcher", placements, LARGE_COMPONENT_COUNT);
        return ConstructionPreviewMesh.capture(generation, placements);
    }

    private static void requireCount(String name,
                                     List<ConstructionPreviewMesh.PlacementSpec> placements,
                                     int expected) {
        if (placements.size() != expected) {
            throw new IllegalStateException(
                    name + " preview component count changed: " + placements.size());
        }
    }

    private static void closeAllBuffers() {
        List<LauncherStructCoreBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (LauncherStructCoreBlockEntityRenderer renderer : renderers) {
            renderer.compactMesh.close();
            renderer.largeMeshes.forEach(ConstructionPreviewMesh::close);
        }
    }

    private static int scaffoldPhase() {
        return (int) (System.currentTimeMillis() % 4000L / 1000L);
    }
}
