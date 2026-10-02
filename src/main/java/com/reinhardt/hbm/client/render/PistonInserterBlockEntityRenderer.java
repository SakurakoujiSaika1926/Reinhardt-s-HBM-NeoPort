package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PistonInserterBlock;
import com.reinhardt.hbm.blockentity.PistonInserterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Exact 1.7.10 RenderPistonInserter model parts, direction table and item pose. */
public final class PistonInserterBlockEntityRenderer implements LongRangeBlockEntityRenderer<PistonInserterBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation FRAME = part("piston_inserter_frame");
    private static final ModelResourceLocation PISTON = part("piston_inserter_piston");
    private static final List<ModelResourceLocation> MODELS = List.of(FRAME, PISTON);
    private static final ClientRenderSnapshotInstances<PistonInserterBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile BakedModelGeneration geometryGeneration;

    public PistonInserterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        PistonInserterBlockEntity.setClientRenderSnapshotUpdater(
                PistonInserterBlockEntityRenderer::updateRenderSnapshot);
    }

    /** Invalidates loaded inserters when resource reload replaces baked item or machine models. */
    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(MODELS);
        PistonInserterBlockEntity.setClientRenderSnapshotUpdater(
                PistonInserterBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(PistonInserterBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(PISTON);
    }

    @Override
    public void render(PistonInserterBlockEntity inserter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PistonInserterBlockEntity.RenderSnapshot snapshot = inserter.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        BlockState state = snapshot.state();
        Direction facing = state.getValue(PistonInserterBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotateLegacy(poseStack, facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        STATIC_MESHES.render(
                generation.model(FRAME), poseStack, state, packedLight, packedOverlay);

        float extension = snapshot.extension(partialTick) / PistonInserterBlockEntity.MAX_EXTEND;
        poseStack.pushPose();
        poseStack.translate(0.0D, extension * 0.9375D, 0.0D);
        STATIC_MESHES.render(
                generation.model(PISTON), poseStack, state, packedLight, packedOverlay);

        ItemStack stack = snapshot.renderedItem();
        if (!stack.isEmpty() && snapshot.modelToken() instanceof BakedModel itemModel) {
            poseStack.pushPose();
            if (stack.getItem() instanceof BlockItem) {
                poseStack.translate(0.0D, 1.125D, 0.0D);
            } else {
                poseStack.translate(0.0D, 1.0625D, 0.1D);
                poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
            }
            Minecraft.getInstance().getItemRenderer().render(
                    stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource,
                    packedLight, packedOverlay, itemModel);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PistonInserterBlockEntity inserter) {
        return inserter.renderSnapshot().bounds();
    }

    /** Runs on the client thread; SectionCompiler only reads the immutable published record. */
    private static void updateRenderSnapshot(PistonInserterBlockEntity inserter) {
        CLIENT_INSTANCES.track(inserter);
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(inserter::refreshClientRenderSnapshot);
            return;
        }

        BlockState state = inserter.getBlockState();
        BakedModelGeneration generation = geometryGeneration();
        ItemStack sourceItem = inserter.copyHeldItemForRenderSnapshot();
        PistonInserterBlockEntity.RenderSnapshot previous = inserter.renderSnapshot();
        long revision = PistonInserterBlockEntity.clientRenderSnapshotRevision();
        boolean sameGeometry = previous.revision() == revision
                && previous.state() == state
                && previous.geometryToken() == generation
                && ItemStack.isSameItemSameComponents(previous.sourceItem(), sourceItem);

        ItemStack renderedItem = previous.renderedItem();
        Object modelToken = previous.modelToken();
        AABB bounds = previous.bounds();
        if (!sameGeometry) {
            renderedItem = sourceItem.copy();
            BakedModel itemModel = null;
            if (!renderedItem.isEmpty()) {
                try {
                    itemModel = minecraft.getItemRenderer().getModel(
                            renderedItem, inserter.getLevel(), null, 0);
                    if (itemModel.isCustomRenderer()) {
                        // A BEWLR exposes no finite geometry contract. Keep rendering and
                        // culling consistent by excluding unsupported custom item models.
                        renderedItem = ItemStack.EMPTY;
                        itemModel = null;
                    }
                } catch (RuntimeException malformedItemModel) {
                    renderedItem = ItemStack.EMPTY;
                    itemModel = null;
                }
            }

            RenderBounds.FloatBuilder builder = new RenderBounds.FloatBuilder(inserter.getBlockPos());
            PoseStack basePose = machinePose(state.getValue(PistonInserterBlock.FACING));
            includeBlockModel(builder, generation.model(FRAME), state, basePose);
            for (float travel : new float[] {0.0F, 0.9375F}) {
                basePose.pushPose();
                basePose.translate(0.0F, travel, 0.0F);
                includeBlockModel(builder, generation.model(PISTON), state, basePose);
                basePose.popPose();
            }

            if (itemModel != null) {
                try {
                    for (Vector3f vertex : sweptItemVertices(itemModel, renderedItem, basePose)) {
                        builder.include(vertex);
                    }
                } catch (RuntimeException malformedItemGeometry) {
                    // No partially inspected item is rendered. Machine vertices already
                    // accumulated remain the exact frame/piston envelope.
                    renderedItem = ItemStack.EMPTY;
                    itemModel = null;
                }
            }
            bounds = builder.build();
            modelToken = itemModel;
        }

        inserter.publishClientRenderSnapshot(new PistonInserterBlockEntity.RenderSnapshot(
                state, sourceItem, renderedItem, modelToken, generation,
                previousScalar(inserter), currentScalar(inserter), bounds, revision));
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (PistonInserterBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(MODELS);
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static float previousScalar(PistonInserterBlockEntity inserter) {
        // The public interpolation endpoints are carried by the previous snapshot until
        // the client tick publishes the next pair. extension(0/1) exposes those exact values.
        return inserter.extension(0.0F);
    }

    private static float currentScalar(PistonInserterBlockEntity inserter) {
        return inserter.extension(1.0F);
    }

    private static PoseStack machinePose(Direction facing) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        rotateLegacy(poseStack, facing);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        return poseStack;
    }

    private static List<Vector3f> sweptItemVertices(BakedModel itemModel, ItemStack stack,
                                                     PoseStack basePose) {
        List<Vector3f> vertices = new ArrayList<>();
        for (float travel : new float[] {0.0F, 0.9375F}) {
            basePose.pushPose();
            basePose.translate(0.0F, travel, 0.0F);
            if (stack.getItem() instanceof BlockItem) {
                basePose.translate(0.0F, 1.125F, 0.0F);
            } else {
                basePose.translate(0.0F, 1.0625F, 0.1F);
                basePose.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
            }
            BakedModel transformed = itemModel.applyTransform(ItemDisplayContext.FIXED, basePose, false);
            basePose.translate(-0.5F, -0.5F, -0.5F);
            includeItemModel(vertices, transformed, stack, basePose);
            basePose.popPose();
        }
        return vertices;
    }

    private static void includeBlockModel(RenderBounds.FloatBuilder bounds, BakedModel model,
                                          BlockState state, PoseStack poseStack) {
        RandomSource random = RandomSource.create(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            includeBlockQuads(bounds, model, state, null, renderType, poseStack, random);
            for (Direction side : Direction.values()) {
                includeBlockQuads(bounds, model, state, side, renderType, poseStack, random);
            }
        }
    }

    private static void includeBlockQuads(RenderBounds.FloatBuilder bounds, BakedModel model,
                                          BlockState state, Direction side, RenderType renderType,
                                          PoseStack poseStack, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
            includeQuad(bounds, quad, poseStack);
        }
    }

    private static void includeItemModel(List<Vector3f> vertices, BakedModel model,
                                         ItemStack stack, PoseStack poseStack) {
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

    private static void includeQuad(RenderBounds.FloatBuilder bounds, BakedQuad quad,
                                    PoseStack poseStack) {
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

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static void rotateLegacy(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(MachineModelRenderer.xQuaternion(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
                poseStack.mulPose(yQuaternion(180.0F));
            }
            case SOUTH -> poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
            case WEST -> {
                poseStack.mulPose(MachineModelRenderer.zQuaternion(90.0F));
                poseStack.mulPose(yQuaternion(-90.0F));
            }
            case EAST -> {
                poseStack.mulPose(MachineModelRenderer.zQuaternion(-90.0F));
                poseStack.mulPose(yQuaternion(90.0F));
            }
        }
    }

    private static Quaternionf yQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
