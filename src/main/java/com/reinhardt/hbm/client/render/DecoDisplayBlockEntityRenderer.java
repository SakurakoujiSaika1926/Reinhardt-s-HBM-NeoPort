package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.block.DecoCrtBlock;
import com.reinhardt.hbm.block.DecoModelBlock;
import com.reinhardt.hbm.block.StatueElbFBlock;
import com.reinhardt.hbm.blockentity.DecoDisplayBlockEntity;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact world-space transforms from RenderBlockDecoModel and RenderCRT. */
public final class DecoDisplayBlockEntityRenderer implements BlockEntityRenderer<DecoDisplayBlockEntity> {
    private static final ResourceLocation STATUE_TEXTURE =
            ReinhardtsHBM.id("textures/models/model_statue.png");
    private static final ResourceLocation GUN_TEXTURE =
            ReinhardtsHBM.id("textures/models/model_gun.png");
    private static final AABB[] STATUE_BOUNDS = buildStatueBounds();
    private static final ModelResourceLocation COMPUTER = model("block/deco_computer");
    private static final ModelResourceLocation[] CRT_MONITOR = {
            model("block/deco_crt_clean_monitor"),
            model("block/deco_crt_broken_monitor"),
            model("block/deco_crt_blinking_monitor"),
            model("block/deco_crt_bsod_monitor")
    };
    private static final ModelResourceLocation[] CRT_SCREEN = {
            model("block/deco_crt_clean_screen"),
            model("block/deco_crt_broken_screen"),
            model("block/deco_crt_blinking_screen"),
            model("block/deco_crt_bsod_screen")
    };

    private final LegacyStatueModel statue;
    private final LegacyStatueGunModel statueGun;

    public DecoDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.statue = new LegacyStatueModel(context.bakeLayer(LegacyStatueModel.LAYER));
        this.statueGun = new LegacyStatueGunModel(context.bakeLayer(LegacyStatueGunModel.LAYER));
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LegacyStatueModel.LAYER, LegacyStatueModel::createLayer);
        event.registerLayerDefinition(LegacyStatueGunModel.LAYER, LegacyStatueGunModel::createLayer);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(COMPUTER);
        for (ModelResourceLocation model : CRT_MONITOR) {
            event.register(model);
        }
        for (ModelResourceLocation model : CRT_SCREEN) {
            event.register(model);
        }
    }

    @Override
    public void render(DecoDisplayBlockEntity display, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = display.getBlockState();
        if (state.is(HbmBlocks.DECO_COMPUTER.get())) {
            poseStack.pushPose();
            applyDecoComputerPose(poseStack, decoComputerFacing(state));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPUTER), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (state.is(HbmBlocks.DECO_CRT.get())) {
            poseStack.pushPose();
            applyDecoCrtPose(poseStack, decoCrtFacing(state));
            int variant = Math.max(0, Math.min(CRT_MONITOR.length - 1, state.getValue(DecoCrtBlock.VARIANT)));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRT_MONITOR[variant]), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            if (state.getValue(DecoCrtBlock.LIT)) {
                MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(CRT_SCREEN[variant]), poseStack,
                        bufferSource, state, packedOverlay);
            } else {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRT_SCREEN[variant]), poseStack,
                        bufferSource, state, packedLight, packedOverlay);
            }
            poseStack.popPose();
            return;
        }
        if (state.is(HbmBlocks.STATUE_ELB_F.get())) {
            renderStatue(state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderStatue(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyStatuePose(poseStack, state.getValue(StatueElbFBlock.FACING));

        VertexConsumer statueConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(STATUE_TEXTURE));
        statue.render(poseStack, statueConsumer, packedLight, packedOverlay);

        poseStack.pushPose();
        applyWatchPose(poseStack);
        Item watch = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("watch"));
        if (watch != Items.AIR) {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    new ItemStack(watch), ItemDisplayContext.FIXED,
                    packedLight, packedOverlay, poseStack, bufferSource, null, 0
            );
        }
        poseStack.popPose();

        poseStack.pushPose();
        applyGunPose(poseStack);
        VertexConsumer gunConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(GUN_TEXTURE));
        statueGun.render(poseStack, gunConsumer, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DecoDisplayBlockEntity display) {
        BlockState state = display.getBlockState();
        if (state.is(HbmBlocks.STATUE_ELB_F.get())) {
            return STATUE_BOUNDS[state.getValue(StatueElbFBlock.FACING).ordinal()].move(display.getBlockPos());
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(display);
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static void applyDecoComputerPose(PoseStack poseStack, net.minecraft.core.Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Decorative computer facing must be horizontal: " + facing);
        }
    }

    private static void applyDecoCrtPose(PoseStack poseStack, net.minecraft.core.Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // RenderCRT metadata 0/1/2/3 maps to NORTH/EAST/WEST/SOUTH;
        // its inventory/world rotations are 90/0/270/180 degrees.
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Decorative CRT facing must be horizontal: " + facing);
        }
    }

    private static void applyStatuePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (facing) {
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("ELB statue requires a horizontal facing: " + facing);
        }
    }

    private static void applyWatchPose(PoseStack poseStack) {
        float scale = 1.0F / 16.0F;
        poseStack.translate(0.0F, -2.0F * scale, 2.0F * scale + scale / 3.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }

    private static void applyGunPose(PoseStack poseStack) {
        float scale = 1.0F / 16.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(-20.0F * scale, 4.0F * scale, 11.0F * scale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F));
    }

    private static AABB[] buildStatueBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack poseStack = new PoseStack();
            applyStatuePose(poseStack, facing);
            RenderBounds.FloatBuilder builder = new RenderBounds.FloatBuilder(net.minecraft.core.BlockPos.ZERO);
            includeStatue(builder, poseStack);

            poseStack.pushPose();
            applyWatchPose(poseStack);
            // The generated watch item occupies the canonical fixed-item unit cube.
            builder.includeBox(poseStack, -0.5F, -0.5F, -0.0625F, 0.5F, 0.5F, 0.0625F);
            poseStack.popPose();

            poseStack.pushPose();
            applyGunPose(poseStack);
            includeGun(builder, poseStack);
            poseStack.popPose();
            bounds[facing.ordinal()] = builder.build();
        }
        return bounds;
    }

    private static void includeStatue(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
        includePart(bounds, poseStack, -8, 16, -8, 0, 0, 0, 0, 0, 0, 16, 8, 16);
        includePart(bounds, poseStack, -4, 4, -2, 0, 0, 0, 0, 0, 0, 4, 12, 4);
        includePart(bounds, poseStack, 0, 4, -2, 0, 0, 0, 0, 0, 0, 4, 12, 4);
        includePart(bounds, poseStack, -4, -8, -2, 0, 0, 0, 0, 0, 0, 8, 12, 4);
        includePart(bounds, poseStack, 4, -8, 0, 0.5235988F, 0, 0, 0, 0, -2, 4, 8, 2);
        includePart(bounds, poseStack, -4, -8, 0, -0.0872665F, 0, 0, -4, 0, -2, 0, 8, 2);
        includePart(bounds, poseStack, 6, -2, 3, 1.22173F, 0, 0, -2, 0, -2, 2, 8, 2);
        includePart(bounds, poseStack, -8, -1, -0.5F, 0.2617994F, 0, 0, 0, 0, -2, 4, 8, 2);
        includePart(bounds, poseStack, 0, -8, 0, -0.1745329F, 0, 0, -4, -8, -4, 4, 0, 4);
    }

    private static void includeGun(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
        includePart(bounds, poseStack, 0, -4, -1, 0, 0, 0, 0, 0, 0, 6, 12, 4);
        includePart(bounds, poseStack, 4, -7, -0.5F, 0, 0, 0, 0, 0, 0, 3, 3, 3);
        includePart(bounds, poseStack, -15, -7, -0.5F, 0, 0, 0, 0, 0, 0, 15, 3, 3);
        includePart(bounds, poseStack, 2, -3, 0.5F, 0, 0, 0.715585F, 0, 0, 0, 1, 2, 1);
        includePart(bounds, poseStack, -13.5F, -8, 0.5F, 0, 0, 0.6108652F, 0, 0, 0, 2, 2, 1);
        includePart(bounds, poseStack, 0, -6.5F, 0, 0, 0, 0, 0, 0, 0, 4, 3, 2);
        includePart(bounds, poseStack, -15, -3, -0.5F, 0, 0, 0, 0, 0, 0, 6, 5, 3);
        includePart(bounds, poseStack, -15, -4, 0, 0, 0, 0, 0, 0, 0, 12, 1, 2);
        includePart(bounds, poseStack, -3, -4, -0.5F, 0, 0, 0, 0, 0, 0, 3, 3, 3);
        includePart(bounds, poseStack, -9, -3, 0, 0, 0, 0, 0, 0, 0, 6, 2, 2);
        includePart(bounds, poseStack, -9, -1, -0.5F, 0, 0, 0, 0, 0, 0, 2, 4, 3);
        includePart(bounds, poseStack, -7, 2, 0.5F, 0, 0, 0, 0, 0, 0, 7, 1, 1);
        includePart(bounds, poseStack, 0, -5, -1, 0, 0, 0, 0, 0, 0, 4, 1, 4);
        includePart(bounds, poseStack, 7, -7, 0, 0, 0, 0.7853982F, 0, 0, 0, 3, 5, 2);
        includePart(bounds, poseStack, -9, 3, -0.5F, 0, 0, -2.792527F, 0, 0, 0, 3, 1, 3);
        includePart(bounds, poseStack, -1, -2, 0.5F, 0, 0, 0.2617994F, 0, 0, 0, 2, 3, 1);
    }

    private static void includePart(RenderBounds.FloatBuilder bounds, PoseStack poseStack,
                                    float pivotX, float pivotY, float pivotZ,
                                    float xRot, float yRot, float zRot,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ) {
        poseStack.pushPose();
        poseStack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        if (xRot != 0.0F || yRot != 0.0F || zRot != 0.0F) {
            poseStack.mulPose(new org.joml.Quaternionf().rotationZYX(zRot, yRot, xRot));
        }
        bounds.includeBox(poseStack,
                minX / 16.0F, minY / 16.0F, minZ / 16.0F,
                maxX / 16.0F, maxY / 16.0F, maxZ / 16.0F);
        poseStack.popPose();
    }

    private static net.minecraft.core.Direction decoComputerFacing(BlockState state) {
        if (!state.hasProperty(DecoModelBlock.FACING)) {
            throw new IllegalStateException("Decorative computer state is missing its facing property: " + state);
        }
        net.minecraft.core.Direction facing = state.getValue(DecoModelBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Decorative computer state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static net.minecraft.core.Direction decoCrtFacing(BlockState state) {
        if (!state.hasProperty(DecoCrtBlock.FACING)) {
            throw new IllegalStateException("Decorative CRT state is missing its facing property: " + state);
        }
        net.minecraft.core.Direction facing = state.getValue(DecoCrtBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Decorative CRT state has a vertical facing: " + facing);
        }
        return facing;
    }
}
