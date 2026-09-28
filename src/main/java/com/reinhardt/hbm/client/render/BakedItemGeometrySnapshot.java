package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Main-thread extraction of the exact baked-float vertices submitted by an
 * ordinary {@link net.minecraft.client.renderer.entity.ItemRenderer} FIXED
 * pass. The published result contains no model-manager references and is safe
 * for SectionCompiler threads to read.
 */
final class BakedItemGeometrySnapshot {
    private static volatile long revision;

    private BakedItemGeometrySnapshot() {
    }

    static void invalidateAfterBake() {
        revision++;
    }

    static long revision() {
        return revision;
    }

    static Result build(ItemStack source, PoseStack poseStack, BlockPos worldPos, Level level) {
        return build(source, poseStack, worldPos, level, ItemDisplayContext.FIXED);
    }

    static Result build(ItemStack source, PoseStack poseStack, BlockPos worldPos, Level level,
                        ItemDisplayContext displayContext) {
        ItemStack sourceCopy = source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1);
        if (sourceCopy.isEmpty()) {
            return empty(sourceCopy, worldPos);
        }

        try {
            BakedModel model = Minecraft.getInstance().getItemRenderer()
                    .getModel(sourceCopy, level, null, 0);
            if (model.isCustomRenderer()) {
                // A BEWLR has no finite vertex contract. The owning renderer must use
                // renderedItem(), so unsupported external/custom geometry is never
                // emitted with an unknowable culling envelope.
                return empty(sourceCopy, worldPos);
            }

            BakedModel transformed = model.applyTransform(displayContext, poseStack, false);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            Bounds bounds = new Bounds(worldPos);
            boolean solidRender = !(sourceCopy.getItem() instanceof BlockItem blockItem)
                    || (!(blockItem.getBlock() instanceof HalfTransparentBlock)
                    && !(blockItem.getBlock() instanceof StainedGlassPaneBlock));
            RandomSource random = RandomSource.create(42L);
            for (BakedModel pass : transformed.getRenderPasses(sourceCopy, solidRender)) {
                for (Direction side : Direction.values()) {
                    random.setSeed(42L);
                    include(bounds, pass.getQuads(null, side, random), poseStack);
                }
                random.setSeed(42L);
                include(bounds, pass.getQuads(null, null, random), poseStack);
            }
            return new Result(sourceCopy, sourceCopy, model,
                    bounds.build(), bounds.maximumRadius());
        } catch (RuntimeException malformedModel) {
            return empty(sourceCopy, worldPos);
        }
    }

    private static Result empty(ItemStack source, BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        return new Result(source, ItemStack.EMPTY, null,
                new AABB(x, y, z, x, y, z), 0.0D);
    }

    private static void include(Bounds bounds, Iterable<BakedQuad> quads, PoseStack poseStack) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2]))));
            }
        }
    }

    record Result(ItemStack sourceItem, ItemStack renderedItem, Object modelToken,
                  AABB bounds, double maximumRadius) {
        Result {
            sourceItem = sourceItem.copy();
            renderedItem = renderedItem.copy();
        }

        boolean matches(ItemStack candidate) {
            return ItemStack.isSameItemSameComponents(sourceItem, candidate);
        }
    }

    private static final class Bounds {
        private final BlockPos pos;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private double maximumRadiusSquared;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private void include(Vector3f point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
            maximumRadiusSquared = Math.max(maximumRadiusSquared,
                    (double) point.x * point.x
                            + (double) point.y * point.y
                            + (double) point.z * point.z);
        }

        private double maximumRadius() {
            return Math.nextUp(Math.sqrt(maximumRadiusSquared));
        }

        private AABB build() {
            if (!Float.isFinite(minX)) {
                double x = pos.getX();
                double y = pos.getY();
                double z = pos.getZ();
                return new AABB(x, y, z, x, y, z);
            }
            return new AABB(
                    pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ
            );
        }
    }
}
