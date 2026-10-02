package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PressMachineBlock;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import java.util.List;

public class PressBlockEntityRenderer implements LongRangeBlockEntityRenderer<PressBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation PRESS_BODY = MachineModelRenderer.standalone("block/machine_press_body_world");
    private static final ModelResourceLocation PRESS_HEAD = MachineModelRenderer.standalone("block/machine_press_head_world");
    private static final ModelResourceLocation EPRESS_BODY = MachineModelRenderer.standalone("block/machine_epress_body_world");
    private static final ModelResourceLocation EPRESS_HEAD = MachineModelRenderer.standalone("block/machine_epress_head_world");
    private static final List<ModelResourceLocation> MODELS = List.of(
            PRESS_BODY, PRESS_HEAD, EPRESS_BODY, EPRESS_HEAD);
    private static final ClientRenderSnapshotInstances<PressBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile BakedModelGeneration geometryGeneration;

    public PressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        PressBlockEntity.setClientRenderSnapshotUpdater(PressBlockEntityRenderer::updateRenderSnapshot);
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(MODELS);
        PressBlockEntity.setClientRenderSnapshotUpdater(PressBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(PressBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PRESS_BODY);
        event.register(PRESS_HEAD);
        event.register(EPRESS_BODY);
        event.register(EPRESS_HEAD);
    }

    @Override
    public void render(PressBlockEntity press, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PressBlockEntity.ItemRenderSnapshot snapshot = press.itemRenderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        Direction facing = pressFacing(state);
        float headTravel = press.clientHeadTravel(partialTick);

        switch (press.kind()) {
            case FIRE -> renderFirePress(generation, state, headTravel, snapshot.renderedInput(),
                    snapshot.itemModelToken(), poseStack, bufferSource, packedLight, packedOverlay);
            case ELECTRIC -> renderElectricPress(generation, state, facing, headTravel,
                    snapshot.renderedInput(), snapshot.itemModelToken(), poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(PressBlockEntity blockEntity) {
        return blockEntity.itemRenderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(PressBlockEntity press) {
        CLIENT_INSTANCES.track(press);
        BakedModelGeneration generation = geometryGeneration();
        BlockState state = press.getBlockState();
        Direction facing = pressFacing(state);
        ItemStack source = press.visualInput();
        ItemStack rendered = ItemStack.EMPTY;
        BakedModel itemModel = null;
        if (!source.isEmpty() && !(source.getItem() instanceof BlockItem)) {
            ItemStack candidate = source.copyWithCount(1);
            try {
                BakedModel candidateModel = Minecraft.getInstance().getItemRenderer()
                        .getModel(candidate, press.getLevel(), null, 0);
                if (!candidateModel.isCustomRenderer()) {
                    rendered = candidate;
                    itemModel = candidateModel;
                }
            } catch (RuntimeException ignoredMalformedModel) {
                rendered = ItemStack.EMPTY;
            }
        }

        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        switch (press.kind()) {
            case FIRE -> captureFirePressGeometry(generation, state, rendered, itemModel, capture);
            case ELECTRIC -> captureElectricPressGeometry(
                    generation, state, facing, rendered, itemModel, capture);
        }
        press.publishClientRenderSnapshot(new PressBlockEntity.ItemRenderSnapshot(
                state, rendered, itemModel, generation, capture.bounds(press.getBlockPos()),
                PressBlockEntity.clientRenderSnapshotRevision()));
    }

    private static void renderFirePress(BakedModelGeneration generation, BlockState state, float headTravel,
                                        ItemStack input, Object itemModelToken, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyFirePressBodyPose(poseStack);
        STATIC_MESHES.render(generation.model(PRESS_BODY),
                poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        applyFirePressHeadPose(poseStack, headTravel);
        STATIC_MESHES.render(generation.model(PRESS_HEAD),
                poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderFirePressInput(input, itemModelToken, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderElectricPress(BakedModelGeneration generation, BlockState state, Direction facing,
                                            float headTravel, ItemStack input, Object itemModelToken,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyElectricPressBodyPose(poseStack, facing);
        STATIC_MESHES.render(generation.model(EPRESS_BODY),
                poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        applyElectricPressHeadPose(poseStack, facing, headTravel);
        STATIC_MESHES.render(generation.model(EPRESS_HEAD),
                poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderElectricPressInput(input, itemModelToken, facing, poseStack, bufferSource,
                packedLight, packedOverlay);
    }

    private static void captureFirePressGeometry(BakedModelGeneration generation, BlockState state,
                                                 ItemStack input, BakedModel itemModel,
                                                 MultiBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        applyFirePressBodyPose(poseStack);
        MachineModelRenderer.renderUnculled(generation.model(PRESS_BODY), poseStack, capture, state, 0, 0);
        poseStack.popPose();

        poseStack.pushPose();
        applyFirePressHeadPose(poseStack, 0.0F);
        MachineModelRenderer.renderUnculled(generation.model(PRESS_HEAD), poseStack, capture, state, 0, 0);
        poseStack.popPose();
        poseStack.pushPose();
        applyFirePressHeadPose(poseStack, 0.875F);
        MachineModelRenderer.renderUnculled(generation.model(PRESS_HEAD), poseStack, capture, state, 0, 0);
        poseStack.popPose();

        renderFirePressInput(input, itemModel, poseStack, capture, 0, 0);
    }

    private static void captureElectricPressGeometry(BakedModelGeneration generation, BlockState state,
                                                     Direction facing, ItemStack input, BakedModel itemModel,
                                                     MultiBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        applyElectricPressBodyPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(generation.model(EPRESS_BODY), poseStack, capture, state, 0, 0);
        poseStack.popPose();

        poseStack.pushPose();
        applyElectricPressHeadPose(poseStack, facing, 0.0F);
        MachineModelRenderer.renderUnculled(generation.model(EPRESS_HEAD), poseStack, capture, state, 0, 0);
        poseStack.popPose();
        poseStack.pushPose();
        applyElectricPressHeadPose(poseStack, facing, 0.875F);
        MachineModelRenderer.renderUnculled(generation.model(EPRESS_HEAD), poseStack, capture, state, 0, 0);
        poseStack.popPose();

        renderElectricPressInput(input, itemModel, facing, poseStack, capture, 0, 0);
    }

    private static void applyFirePressBodyPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static void applyFirePressHeadPose(PoseStack poseStack, float headTravel) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.99F, 1.0F, 0.99F);
        poseStack.translate(0.0F, 0.875F - headTravel, 0.0F);
    }

    private static void applyFirePressInputPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.0F, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(0.0F, 1.0F - 0.0625F * 165.0F / 100.0F, 0.0F);
    }

    private static void applyElectricPressBodyPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electric press body requires a horizontal facing: " + facing);
        }
    }

    private static void applyElectricPressHeadPose(PoseStack poseStack, Direction facing, float headTravel) {
        poseStack.translate(0.5F, 1.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electric press head requires a horizontal facing: " + facing);
        }
        poseStack.translate(0.0F, 0.875F - headTravel, 0.0F);
    }

    private static void applyElectricPressInputPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 1.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Electric press input requires a horizontal facing: " + facing);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(1.0F, 1.0F - 0.0625F * 165.0F / 100.0F, 0.0F);
        poseStack.translate(-1.0F, -1.15F, 0.0F);
    }

    private static Direction pressFacing(BlockState state) {
        if (!state.hasProperty(PressMachineBlock.FACING)) {
            throw new IllegalStateException("Press block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PressMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Press block state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void renderElectricPressInput(
            ItemStack stack,
            Object itemModelToken,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem
                || !(itemModelToken instanceof BakedModel itemModel)) {
            return;
        }

        poseStack.pushPose();
        applyElectricPressInputPose(poseStack, facing);
        poseStack.translate(0.0F, 0.125F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().render(
                stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource,
                packedLight, packedOverlay, itemModel);
        poseStack.popPose();
    }

    private static void renderFirePressInput(
            ItemStack stack,
            Object itemModelToken,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem
                || !(itemModelToken instanceof BakedModel itemModel)) {
            return;
        }

        poseStack.pushPose();
        applyFirePressInputPose(poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().render(
                stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource,
                packedLight, packedOverlay, itemModel);
        poseStack.popPose();
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (PressBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(MODELS);
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

}
