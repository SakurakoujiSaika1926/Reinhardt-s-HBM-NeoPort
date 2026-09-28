package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.IcfStructBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.blockentity.IcfStructBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Exact 1.7.10 ICF construction projection with explicit per-facing poses. */
public final class IcfStructBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<IcfStructBlockEntity> {
    private static final int COMPONENT_COUNT = 407;
    private static final Set<IcfStructBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile ConstructionPreviewMesh.Geometry geometry;
    private static volatile Map<Direction, AABB> localBoundsByFacing = Map.of();
    private static int nextGeneration;

    private final ConstructionPreviewMesh mesh = new ConstructionPreviewMesh();

    public IcfStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    @Override
    public void render(IcfStructBlockEntity core, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        ConstructionPreviewMesh.Geometry current = geometry;
        if (current == null) {
            return;
        }
        Direction facing = facing(core);
        poseStack.pushPose();
        applyFacingPose(poseStack, facing);
        this.mesh.render(current, poseStack, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IcfStructBlockEntity core) {
        Direction facing = facing(core);
        AABB local = localBoundsByFacing.get(facing);
        if (local != null) {
            return local.move(core.getBlockPos());
        }
        BlockPos pos = core.getBlockPos();
        return switch (facing) {
            case SOUTH, NORTH -> RenderBounds.localAnalytic(
                    pos, -8.0D, 0.0D, -2.0D, 9.0D, 6.0D, 3.0D);
            case EAST, WEST -> RenderBounds.localAnalytic(
                    pos, -2.0D, 0.0D, -8.0D, 3.0D, 6.0D, 9.0D);
            case UP, DOWN -> throw new IllegalStateException(
                    "ICF preview has non-horizontal facing " + facing);
        };
    }

    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(IcfStructBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        ConstructionPreviewMesh.Geometry next = capture(++nextGeneration);
        EnumMap<Direction, AABB> bounds = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            bounds.put(facing, next.transformedLocalBounds(
                    poseStack -> applyFacingPose(poseStack, facing)));
        }
        geometry = next;
        localBoundsByFacing = Map.copyOf(bounds);
        closeAllBuffers();
    }

    static void invalidateModelCache() {
        closeAllBuffers();
    }

    private static ConstructionPreviewMesh.Geometry capture(int generation) {
        BlockState component = HbmBlocks.ICF_COMPONENT.get().defaultBlockState();
        BlockState scaffold = component.setValue(LegacyVariantBlock.VARIANT, 0);
        BlockState vessel = component.setValue(LegacyVariantBlock.VARIANT, 2);
        BlockState structure = component.setValue(LegacyVariantBlock.VARIANT, 4);
        List<ConstructionPreviewMesh.PlacementSpec> placements =
                new ArrayList<>(COMPONENT_COUNT);

        for (int length = -8; length <= 8; length++) {
            add(placements, scaffold, 1, 0, length);
            if (length != 0) {
                add(placements, scaffold, 0, 0, length);
            }
            add(placements, scaffold, -1, 0, length);
            add(placements, vessel, 0, 3, length);

            BlockState shell = Math.abs(length) <= 2 ? vessel : structure;
            for (int width = -1; width <= 1; width++) {
                add(placements, shell, width, 1, length);
            }
            for (int width = -2; width <= 2; width++) {
                add(placements, shell, width, 2, length);
                if (width != 0) {
                    add(placements, shell, width, 3, length);
                }
                add(placements, shell, width, 4, length);
            }
            for (int width = -1; width <= 1; width++) {
                add(placements, shell, width, 5, length);
            }
        }

        if (placements.size() != COMPONENT_COUNT) {
            throw new IllegalStateException(
                    "ICF preview component count changed: " + placements.size());
        }
        return ConstructionPreviewMesh.capture(generation, placements);
    }

    /** Mirrors IcfStructBlockEntity.component's authored (-length, y, width). */
    private static void add(List<ConstructionPreviewMesh.PlacementSpec> placements,
                            BlockState state, int width, int y, int length) {
        placements.add(ConstructionPreviewMesh.placement(state, -length, y, width));
    }

    private static Direction facing(IcfStructBlockEntity core) {
        BlockState state = core.getBlockState();
        if (!state.hasProperty(IcfStructBlock.FACING)) {
            throw new IllegalStateException(
                    "ICF structure core is missing its FACING property: " + state);
        }
        return state.getValue(IcfStructBlock.FACING);
    }

    /**
     * Exact counterpart of IcfStructBlockEntity.mapIcfStructureOffset:
     * SOUTH=(x,z), EAST=(z,-x), NORTH=(-x,-z), WEST=(-z,x).
     */
    private static void applyFacingPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalStateException(
                    "ICF preview has non-horizontal facing " + facing);
        }
        poseStack.translate(-0.5D, 0.0D, -0.5D);
    }

    private static void closeAllBuffers() {
        List<IcfStructBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (IcfStructBlockEntityRenderer renderer : renderers) {
            renderer.mesh.close();
        }
    }
}
