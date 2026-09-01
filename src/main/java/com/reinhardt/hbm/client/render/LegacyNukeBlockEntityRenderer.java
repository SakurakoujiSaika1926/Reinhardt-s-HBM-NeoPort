package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LegacyNukeBlock;
import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.blockentity.LegacyNukeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Renders the ten legacy bomb assemblies with their individual 1.7.10 poses. */
public final class LegacyNukeBlockEntityRenderer implements BlockEntityRenderer<LegacyNukeBlockEntity> {
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
        Direction facing = state.hasProperty(LegacyNukeBlock.FACING)
                ? state.getValue(LegacyNukeBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(definition.worldYaw(facing)));
        if (definition.isCustom()) {
            poseStack.translate(-2.0D, 0.0D, 0.0D);
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
        return new AABB(nuke.getBlockPos()).inflate(3.0D, 2.0D, 3.0D);
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
