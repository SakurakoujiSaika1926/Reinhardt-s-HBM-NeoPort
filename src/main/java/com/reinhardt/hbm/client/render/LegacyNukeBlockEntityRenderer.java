package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LegacyNukeBlock;
import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.blockentity.LegacyNukeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.EnumMap;
import java.util.Map;

/** Renders the ten legacy bomb assemblies with their individual 1.7.10 poses. */
public final class LegacyNukeBlockEntityRenderer implements LongRangeBlockEntityRenderer<LegacyNukeBlockEntity> {
    private static final ModelResourceLocation GADGET = model("block/nuke_gadget_world");
    private static final ModelResourceLocation GADGET_BODY = model("block/nuke_gadget_body");
    private static final ModelResourceLocation GADGET_WIRES = model("block/nuke_gadget_wires");
    private static final ModelResourceLocation MAN = model("block/nuke_man_world");
    private static final ModelResourceLocation MIKE = model("block/nuke_mike_world");
    private static final ModelResourceLocation TSAR = model("block/nuke_tsar_world");
    private static final ModelResourceLocation FLEIJA = model("block/nuke_fleija_world");
    private static final ModelResourceLocation PROTOTYPE = model("block/nuke_prototype_world");
    private static final ModelResourceLocation SOLINIUM = model("block/nuke_solinium_world");
    private static final ModelResourceLocation N2 = model("block/nuke_n2_world");
    private static final ModelResourceLocation CUSTOM = model("block/nuke_custom_world");
    private static final ModelResourceLocation BALEFIRE = model("block/nuke_fstbmb_world");
    private static final Map<LegacyNukeDefinition, AABB[]> LOCAL_BOUNDS = buildNukeBounds();

    public LegacyNukeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GADGET);
        event.register(GADGET_BODY);
        event.register(GADGET_WIRES);
        event.register(MAN);
        event.register(MIKE);
        event.register(TSAR);
        event.register(FLEIJA);
        event.register(PROTOTYPE);
        event.register(SOLINIUM);
        event.register(N2);
        event.register(CUSTOM);
        event.register(BALEFIRE);
    }

    @Override
    public void render(LegacyNukeBlockEntity nuke, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = nuke.getBlockState();
        LegacyNukeDefinition definition = nuke.definition();

        poseStack.pushPose();
        switch (definition) {
            case GADGET -> applyGadgetTransform(poseStack, gadgetFacing(state));
            case MAN -> applyManTransform(poseStack, manFacing(state));
            case MIKE -> applyMikeTransform(poseStack, mikeFacing(state));
            case TSAR -> applyTsarTransform(poseStack, tsarFacing(state));
            case FLEIJA -> applyFleijaTransform(poseStack, fleijaFacing(state));
            case PROTOTYPE -> applyPrototypeTransform(poseStack, prototypeFacing(state));
            case SOLINIUM -> applySoliniumTransform(poseStack, soliniumFacing(state));
            case N2 -> applyN2Transform(poseStack, n2Facing(state));
            case CUSTOM -> applyCustomTransform(poseStack, customFacing(state));
            case BALEFIRE -> applyBalefireTransform(poseStack, balefireFacing(state));
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(modelFor(definition)), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        if (definition == LegacyNukeDefinition.GADGET
                && Minecraft.getInstance().options.graphicsMode().get().getId() != 0) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GADGET_WIRES), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LegacyNukeBlockEntity nuke) {
        BlockState state = nuke.getBlockState();
        Direction facing = switch (nuke.definition()) {
            case GADGET -> gadgetFacing(state);
            case MAN -> manFacing(state);
            case MIKE -> mikeFacing(state);
            case TSAR -> tsarFacing(state);
            case FLEIJA -> fleijaFacing(state);
            case PROTOTYPE -> prototypeFacing(state);
            case SOLINIUM -> soliniumFacing(state);
            case N2 -> n2Facing(state);
            case CUSTOM -> customFacing(state);
            case BALEFIRE -> balefireFacing(state);
        };
        return LOCAL_BOUNDS.get(nuke.definition())[facing.ordinal()].move(nuke.getBlockPos());
    }

    private static Map<LegacyNukeDefinition, AABB[]> buildNukeBounds() {
        EnumMap<LegacyNukeDefinition, AABB[]> bounds = new EnumMap<>(LegacyNukeDefinition.class);
        for (LegacyNukeDefinition definition : LegacyNukeDefinition.values()) {
            float[] envelope = nukeEnvelope(definition);
            AABB[] facings = new AABB[Direction.values().length];
            for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                PoseStack poseStack = new PoseStack();
                applyLegacyNukeTransform(poseStack, definition, facing);
                facings[facing.ordinal()] = RenderBounds.transformedBox(
                        net.minecraft.core.BlockPos.ZERO, poseStack,
                        envelope[0], envelope[1], envelope[2], envelope[3], envelope[4], envelope[5]);
            }
            bounds.put(definition, facings);
        }
        return bounds;
    }

    private static float[] nukeEnvelope(LegacyNukeDefinition definition) {
        return switch (definition) {
            case GADGET -> new float[] {-1.301602F, 0.0F, -1.364865F, 1.0F, 2.494827F, 1.233917F};
            case MAN -> new float[] {-1.221398F, -0.086501F, -0.805217F, 2.033254F, 1.586501F, 0.805218F};
            case MIKE -> new float[] {-1.132443F, 0.0F, -1.132443F, 1.132443F, 6.0F, 1.132443F};
            case TSAR -> new float[] {-5.303239F, -0.03209F, -1.372628F, 2.836521F, 2.553267F, 1.372628F};
            case FLEIJA -> new float[] {-0.5F, 0.0F, -1.25F, 0.5F, 0.9375F, 1.25F};
            case PROTOTYPE -> new float[] {-0.625001F, 0.0F, -3.5F, 0.625001F, 1.0625F, 3.5F};
            case SOLINIUM -> new float[] {-0.5F, 0.0F, -1.8125F, 0.5F, 1.0F, 1.8125F};
            case N2 -> new float[] {-1.0F, 0.0F, -1.0F, 1.0F, 7.0F, 1.0F};
            case CUSTOM -> new float[] {-0.35F, 0.0F, -0.425F, 2.85F, 0.85F, 0.425F};
            case BALEFIRE -> new float[] {-0.928078F, -0.178078F, -4.5F, 1.105F, 1.678078F, 3.0F};
        };
    }

    private static void applyLegacyNukeTransform(PoseStack poseStack, LegacyNukeDefinition definition, Direction facing) {
        switch (definition) {
            case GADGET -> applyGadgetTransform(poseStack, facing);
            case MAN -> applyManTransform(poseStack, facing);
            case MIKE -> applyMikeTransform(poseStack, facing);
            case TSAR -> applyTsarTransform(poseStack, facing);
            case FLEIJA -> applyFleijaTransform(poseStack, facing);
            case PROTOTYPE -> applyPrototypeTransform(poseStack, facing);
            case SOLINIUM -> applySoliniumTransform(poseStack, facing);
            case N2 -> applyN2Transform(poseStack, facing);
            case CUSTOM -> applyCustomTransform(poseStack, facing);
            case BALEFIRE -> applyBalefireTransform(poseStack, facing);
        }
    }

    private static Direction gadgetFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Gadget state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction manFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Little Boy state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction mikeFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Ivy Mike state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction tsarFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Tsar Bomba state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction fleijaFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("F.L.E.I.J.A. state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction prototypeFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Prototype state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction soliniumFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Solinium bomb state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction n2Facing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("N2 bomb state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction customFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Custom nuke state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static Direction balefireFacing(BlockState state) {
        if (!state.hasProperty(LegacyNukeBlock.FACING)) {
            throw new IllegalStateException("Balefire bomb state is missing its dedicated facing property");
        }
        return state.getValue(LegacyNukeBlock.FACING);
    }

    private static void applyGadgetTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> { }
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Gadget facing must be horizontal: " + facing);
        }
    }

    private static void applyManTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Little Boy facing must be horizontal: " + facing);
        }
    }

    private static void applyMikeTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Ivy Mike facing must be horizontal: " + facing);
        }
    }

    private static void applyTsarTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Tsar Bomba facing must be horizontal: " + facing);
        }
    }

    private static void applyFleijaTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("F.L.E.I.J.A. facing must be horizontal: " + facing);
        }
    }

    private static void applyPrototypeTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Prototype facing must be horizontal: " + facing);
        }
    }

    private static void applySoliniumTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Solinium bomb facing must be horizontal: " + facing);
        }
    }

    private static void applyN2Transform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("N2 bomb facing must be horizontal: " + facing);
        }
    }

    private static void applyCustomTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Custom nuke facing must be horizontal: " + facing);
        }
        poseStack.translate(-2.0F, 0.0F, 0.0F);
    }

    private static void applyBalefireTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Balefire bomb facing must be horizontal: " + facing);
        }
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static ModelResourceLocation modelFor(LegacyNukeDefinition definition) {
        return switch (definition) {
            case GADGET -> GADGET_BODY;
            case MAN -> MAN;
            case MIKE -> MIKE;
            case TSAR -> TSAR;
            case FLEIJA -> FLEIJA;
            case PROTOTYPE -> PROTOTYPE;
            case SOLINIUM -> SOLINIUM;
            case N2 -> N2;
            case CUSTOM -> CUSTOM;
            case BALEFIRE -> BALEFIRE;
        };
    }
}
