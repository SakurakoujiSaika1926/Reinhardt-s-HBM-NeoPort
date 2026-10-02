package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LegacyDisplayStandBlock;
import com.reinhardt.hbm.blockentity.LegacyDisplayStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Exact world transforms from RenderPedestalTile and RenderSkeletonHolder. */
public final class LegacyDisplayStandBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<LegacyDisplayStandBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation SKELETON_HOLDER =
            MachineModelRenderer.standalone("block/skeleton_holder_world");
    private static final List<ModelResourceLocation> MODELS = List.of(SKELETON_HOLDER);
    private static final ClientRenderSnapshotInstances<LegacyDisplayStandBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile BakedModelGeneration geometryGeneration;

    public LegacyDisplayStandBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        LegacyDisplayStandBlockEntity.setClientRenderSnapshotUpdater(
                LegacyDisplayStandBlockEntityRenderer::updateRenderSnapshot);
    }

    /** Invalidates every loaded stand after baked item/holder models are replaced. */
    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(MODELS);
        LegacyDisplayStandBlockEntity.setClientRenderSnapshotUpdater(
                LegacyDisplayStandBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(LegacyDisplayStandBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SKELETON_HOLDER);
    }

    @Override
    public void render(LegacyDisplayStandBlockEntity stand, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LegacyDisplayStandBlockEntity.RenderSnapshot snapshot = stand.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        BlockState state = snapshot.state();
        if (!(state.getBlock() instanceof LegacyDisplayStandBlock block)) {
            return;
        }
        BakedModel itemModel = snapshot.modelToken() instanceof BakedModel bakedModel ? bakedModel : null;

        if (block.kind() == LegacyDisplayStandBlock.Kind.SKELETON_HOLDER) {
            poseStack.pushPose();
            applySkeletonHolderPose(poseStack, skeletonHolderFacing(state));
            STATIC_MESHES.render(generation.model(SKELETON_HOLDER), poseStack,
                    state, packedLight, packedOverlay);
            renderSkeletonItem(snapshot.displayedItem(), itemModel, snapshot.threeDimensional(), stand,
                    poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        } else {
            renderPedestalItem(snapshot.displayedItem(), itemModel, snapshot.threeDimensional(), stand, partialTick,
                    poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    private static void renderPedestalItem(ItemStack stack, BakedModel model, boolean threeDimensional,
                                           LegacyDisplayStandBlockEntity stand, float partialTick,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        if (stack.isEmpty() || model == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        float bob = (float) Math.sin((stand.getLevel().getGameTime() + partialTick) * 0.1F) * 0.0625F;
        poseStack.translate(0.0F, bob + (threeDimensional ? 0.0625F : 0.125F), 0.0F);
        if (!threeDimensional && minecraft.player != null) {
            poseStack.mulPose(Axis.YN.rotationDegrees(minecraft.player.getYRot() + 180.0F));
        }
        int itemLight = stand.getLevel() == null
                ? packedLight
                : LevelRenderer.getLightColor(stand.getLevel(), stand.getBlockPos().above());
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false,
                poseStack, bufferSource, itemLight, packedOverlay, model);
        poseStack.popPose();
    }

    private static void renderSkeletonItem(ItemStack stack, BakedModel model, boolean threeDimensional,
                                           LegacyDisplayStandBlockEntity stand,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        if (stack.isEmpty() || model == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (!threeDimensional) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }
        poseStack.translate(0.0F, 0.125F, 0.0F);
        minecraft.getItemRenderer().render(stack, ItemDisplayContext.FIXED, false,
                poseStack, bufferSource, packedLight, packedOverlay, model);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LegacyDisplayStandBlockEntity stand) {
        return stand.renderSnapshot().bounds();
    }

    /** Runs only on Minecraft's client thread; SectionCompiler only reads the published record. */
    private static void updateRenderSnapshot(LegacyDisplayStandBlockEntity stand) {
        CLIENT_INSTANCES.track(stand);
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(stand::refreshClientRenderSnapshot);
            return;
        }

        BlockState state = stand.getBlockState();
        BakedModelGeneration generation = geometryGeneration();
        ItemStack stack = stand.copyDisplayedItemForRenderSnapshot();
        BakedModel itemModel = null;
        boolean threeDimensional = false;
        if (!stack.isEmpty()) {
            try {
                itemModel = minecraft.getItemRenderer().getModel(stack, stand.getLevel(), null, 0);
                if (itemModel.isCustomRenderer()) {
                    // BEWLRs expose no geometry contract.  Exclude unsupported
                    // custom-renderer stacks from both this immutable render
                    // view and its bounds instead of rendering an unbounded
                    // fallback whose ordinary baked quads are usually empty.
                    stack = ItemStack.EMPTY;
                    itemModel = null;
                } else {
                    threeDimensional = itemModel.isGui3d();
                }
            } catch (RuntimeException malformedItemModel) {
                // Keep the immutable render and bounds views consistent: an item whose
                // baked fallback cannot be inspected is not submitted by this BER.
                stack = ItemStack.EMPTY;
            }
        }

        Bounds bounds = new Bounds(stand.getBlockPos());
        if (state.getBlock() instanceof LegacyDisplayStandBlock block) {
            if (block.kind() == LegacyDisplayStandBlock.Kind.SKELETON_HOLDER) {
                PoseStack holderPose = skeletonPose(state);
                try {
                    includeBlockModel(bounds, generation.model(SKELETON_HOLDER), state, holderPose);
                } catch (RuntimeException ignoredMalformedHolderModel) {
                    // The matching render call cannot emit this malformed baked model either.
                }
                if (itemModel != null && !stack.isEmpty()) {
                    Bounds itemBounds = new Bounds(stand.getBlockPos());
                    try {
                        includeSkeletonItem(itemBounds, itemModel, stack, threeDimensional, holderPose);
                        bounds.include(itemBounds);
                    } catch (RuntimeException malformedItemGeometry) {
                        stack = ItemStack.EMPTY;
                    }
                }
            } else if (itemModel != null && !stack.isEmpty()) {
                Bounds itemBounds = new Bounds(stand.getBlockPos());
                try {
                    includePedestalItem(itemBounds, itemModel, stack, threeDimensional);
                    bounds.include(itemBounds);
                } catch (RuntimeException malformedItemGeometry) {
                    stack = ItemStack.EMPTY;
                }
            }
        }

        stand.publishClientRenderSnapshot(new LegacyDisplayStandBlockEntity.RenderSnapshot(
                state, stack, threeDimensional, itemModel, generation, bounds.toAabb(),
                LegacyDisplayStandBlockEntity.clientRenderSnapshotRevision()));
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (LegacyDisplayStandBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(MODELS);
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static PoseStack skeletonPose(BlockState state) {
        PoseStack poseStack = new PoseStack();
        applySkeletonHolderPose(poseStack, skeletonHolderFacing(state));
        return poseStack;
    }

    private static void includeSkeletonItem(Bounds bounds, BakedModel model, ItemStack stack,
                                            boolean threeDimensional, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (!threeDimensional) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }
        poseStack.translate(0.0F, 0.125F, 0.0F);
        includeTransformedItem(bounds, model, stack, poseStack);
        poseStack.popPose();
    }

    private static void includePedestalItem(Bounds bounds, BakedModel model, ItemStack stack,
                                            boolean threeDimensional) {
        if (threeDimensional) {
            // The sinusoidal bob is an affine Y translation; its exact full-time
            // extrema therefore occur at the two endpoint values.
            for (float bob : new float[] {-0.0625F, 0.0625F}) {
                PoseStack poseStack = pedestalPose(bob, 0.0625F);
                includeTransformedItem(bounds, model, stack, poseStack);
            }
            return;
        }

        PoseStack itemPose = new PoseStack();
        BakedModel transformedModel = model.applyTransform(ItemDisplayContext.FIXED, itemPose, false);
        itemPose.translate(-0.5F, -0.5F, -0.5F);
        List<Vector3f> vertices = transformedItemVertices(transformedModel, stack, itemPose);
        if (vertices.isEmpty()) {
            return;
        }

        double radius = 0.0D;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Vector3f vertex : vertices) {
            radius = Math.max(radius, 1.5D * Math.hypot((double) vertex.x, (double) vertex.z));
            for (float bob : new float[] {-0.0625F, 0.0625F}) {
                Vector3f point = pedestalPose(bob, 0.125F).last().pose()
                        .transformPosition(new Vector3f(vertex));
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
            }
        }

        /*
         * Player yaw is continuous.  In exact arithmetic its support is the
         * horizontal circle above.  gamma(32) bounds the float error from the
         * sin/cos quaternion, quaternion-to-matrix expansion, dot product,
         * 1.5 scale, and 0.5 translation; nextDown/nextUp perform the final
         * outward double rounding.  This is a numerical proof allowance, not
         * an authored geometry margin.
         */
        double unitRoundoff = 0x1.0p-24;
        double gamma32 = 32.0D * unitRoundoff / (1.0D - 32.0D * unitRoundoff);
        double floatError = (0.5D + radius) * gamma32;
        double minHorizontal = Math.nextDown(0.5D - radius - floatError);
        double maxHorizontal = Math.nextUp(0.5D + radius + floatError);
        bounds.include(minHorizontal, minY, minHorizontal);
        bounds.include(maxHorizontal, maxY, maxHorizontal);
    }

    private static PoseStack pedestalPose(float bob, float baseHeight) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.translate(0.0F, bob + baseHeight, 0.0F);
        return poseStack;
    }

    private static void includeTransformedItem(Bounds bounds, BakedModel model, ItemStack stack,
                                               PoseStack poseStack) {
        BakedModel transformedModel = model.applyTransform(ItemDisplayContext.FIXED, poseStack, false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        for (Vector3f vertex : transformedItemVertices(transformedModel, stack, poseStack)) {
            bounds.include(vertex);
        }
    }

    private static List<Vector3f> transformedItemVertices(BakedModel model, ItemStack stack,
                                                           PoseStack poseStack) {
        List<Vector3f> vertices = new ArrayList<>();
        boolean solidRender = !(stack.getItem() instanceof BlockItem blockItem)
                || (!(blockItem.getBlock() instanceof HalfTransparentBlock)
                && !(blockItem.getBlock() instanceof StainedGlassPaneBlock));
        RandomSource random = RandomSource.create(42L);
        for (BakedModel pass : model.getRenderPasses(stack, solidRender)) {
            for (Direction side : Direction.values()) {
                random.setSeed(42L);
                includeItemQuads(vertices, pass.getQuads(null, side, random), poseStack);
            }
            random.setSeed(42L);
            includeItemQuads(vertices, pass.getQuads(null, null, random), poseStack);
        }
        return vertices;
    }

    private static void includeBlockModel(Bounds bounds, BakedModel model, BlockState state,
                                          PoseStack poseStack) {
        RandomSource random = RandomSource.create(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            includeBlockQuads(bounds, model, state, null, renderType, poseStack, random);
            for (Direction side : Direction.values()) {
                includeBlockQuads(bounds, model, state, side, renderType, poseStack, random);
            }
        }
    }

    private static void includeBlockQuads(Bounds bounds, BakedModel model, BlockState state,
                                          Direction side, RenderType renderType,
                                          PoseStack poseStack, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
            includeQuad(bounds, quad, poseStack);
        }
    }

    private static void includeItemQuads(List<Vector3f> vertices, Iterable<BakedQuad> quads,
                                         PoseStack poseStack) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                vertices.add(poseStack.last().pose().transformPosition(new Vector3f(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2]))));
            }
        }
    }

    private static void includeQuad(Bounds bounds, BakedQuad quad, PoseStack poseStack) {
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

    private static final class Bounds {
        private final BlockPos pos;
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private void include(Vector3f point) {
            include(point.x, point.y, point.z);
        }

        private void include(double x, double y, double z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        private void include(Bounds other) {
            if (!Double.isFinite(other.minX)) {
                return;
            }
            include(other.minX, other.minY, other.minZ);
            include(other.maxX, other.maxY, other.maxZ);
        }

        private AABB toAabb() {
            if (!Double.isFinite(minX)) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY() + 0.5D;
                double z = pos.getZ() + 0.5D;
                return new AABB(x, y, z, x, y, z);
            }
            return new AABB(
                    pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ
            );
        }
    }

    private static net.minecraft.core.Direction skeletonHolderFacing(BlockState state) {
        if (!state.hasProperty(LegacyDisplayStandBlock.FACING)) {
            throw new IllegalStateException("Skeleton holder state is missing its owned facing property: " + state);
        }
        net.minecraft.core.Direction facing = state.getValue(LegacyDisplayStandBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Skeleton holder state has an invalid vertical facing: " + state);
        }
        return facing;
    }

    private static void applySkeletonHolderPose(PoseStack poseStack, net.minecraft.core.Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Skeleton holder facing must be horizontal: " + facing);
        }
    }
}
