package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class FusionMachineBlockEntityRenderer implements BlockEntityRenderer<FusionMachineBlockEntity> {
    private static final ModelResourceLocation TORUS_TORUS = MachineModelRenderer.standalone("block/fusion_torus_torus");
    private static final ModelResourceLocation TORUS_MAGNET = MachineModelRenderer.standalone("block/fusion_torus_magnet");
    private static final ModelResourceLocation TORUS_BOLTS1 = MachineModelRenderer.standalone("block/fusion_torus_bolts1");
    private static final ModelResourceLocation TORUS_BOLTS2 = MachineModelRenderer.standalone("block/fusion_torus_bolts2");
    private static final ModelResourceLocation TORUS_BOLTS3 = MachineModelRenderer.standalone("block/fusion_torus_bolts3");
    private static final ModelResourceLocation TORUS_BOLTS4 = MachineModelRenderer.standalone("block/fusion_torus_bolts4");
    private static final ModelResourceLocation TORUS_PLASMA = MachineModelRenderer.standalone("block/fusion_torus_plasma");
    private static final ModelResourceLocation TORUS_PLASMA_GLOW = MachineModelRenderer.standalone("block/fusion_torus_plasma_glow");
    private static final ModelResourceLocation TORUS_PLASMA_SPARKLE = MachineModelRenderer.standalone("block/fusion_torus_plasma_sparkle");

    private static final ModelResourceLocation KLYSTRON_BODY = MachineModelRenderer.standalone("block/fusion_klystron_body");
    private static final ModelResourceLocation KLYSTRON_ROTOR = MachineModelRenderer.standalone("block/fusion_klystron_rotor");
    private static final ModelResourceLocation KLYSTRON_CREATIVE_BODY = MachineModelRenderer.standalone("block/fusion_klystron_creative_body");
    private static final ModelResourceLocation KLYSTRON_CREATIVE_ROTOR = MachineModelRenderer.standalone("block/fusion_klystron_creative_rotor");
    private static final ModelResourceLocation BOILER = MachineModelRenderer.standalone("block/fusion_boiler");
    private static final ModelResourceLocation BREEDER = MachineModelRenderer.standalone("block/fusion_breeder");
    private static final ModelResourceLocation COLLECTOR = MachineModelRenderer.standalone("block/fusion_collector");
    private static final ModelResourceLocation COUPLER = MachineModelRenderer.standalone("block/fusion_coupler");
    private static final ModelResourceLocation MHDT_TURBINE = MachineModelRenderer.standalone("block/fusion_mhdt_turbine");
    private static final ModelResourceLocation MHDT_COILS = MachineModelRenderer.standalone("block/fusion_mhdt_coils");
    private static final ModelResourceLocation PLASMA_FORGE_BODY = MachineModelRenderer.standalone("block/fusion_plasma_forge_body");
    private static final ModelResourceLocation PLASMA_FORGE_PLASMA = MachineModelRenderer.standalone("block/fusion_plasma_forge_plasma");
    private static final ModelResourceLocation PLASMA_FORGE_PLASMA_GLOW = MachineModelRenderer.standalone("block/fusion_plasma_forge_plasma_glow");
    private static final ModelResourceLocation PLASMA_FORGE_SLIDER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_slider_striker");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_LOWER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_lower_striker");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_UPPER_STRIKER = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_upper_striker");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_MOUNT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_mount");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_LEFT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_left");
    private static final ModelResourceLocation PLASMA_FORGE_STRIKER_RIGHT = MachineModelRenderer.standalone("block/fusion_plasma_forge_striker_right");
    private static final ModelResourceLocation PLASMA_FORGE_PISTON_LEFT = MachineModelRenderer.standalone("block/fusion_plasma_forge_piston_left");
    private static final ModelResourceLocation PLASMA_FORGE_PISTON_RIGHT = MachineModelRenderer.standalone("block/fusion_plasma_forge_piston_right");
    private static final ModelResourceLocation PLASMA_FORGE_SLIDER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_slider_jet");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_LOWER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_lower_jet");
    private static final ModelResourceLocation PLASMA_FORGE_ARM_UPPER_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_arm_upper_jet");
    private static final ModelResourceLocation PLASMA_FORGE_JET = MachineModelRenderer.standalone("block/fusion_plasma_forge_jet");

    public FusionMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TORUS_TORUS);
        event.register(TORUS_MAGNET);
        event.register(TORUS_BOLTS1);
        event.register(TORUS_BOLTS2);
        event.register(TORUS_BOLTS3);
        event.register(TORUS_BOLTS4);
        event.register(TORUS_PLASMA);
        event.register(TORUS_PLASMA_GLOW);
        event.register(TORUS_PLASMA_SPARKLE);
        event.register(KLYSTRON_BODY);
        event.register(KLYSTRON_ROTOR);
        event.register(KLYSTRON_CREATIVE_BODY);
        event.register(KLYSTRON_CREATIVE_ROTOR);
        event.register(BOILER);
        event.register(BREEDER);
        event.register(COLLECTOR);
        event.register(COUPLER);
        event.register(MHDT_TURBINE);
        event.register(MHDT_COILS);
        event.register(PLASMA_FORGE_BODY);
        event.register(PLASMA_FORGE_PLASMA);
        event.register(PLASMA_FORGE_PLASMA_GLOW);
        event.register(PLASMA_FORGE_SLIDER_STRIKER);
        event.register(PLASMA_FORGE_ARM_LOWER_STRIKER);
        event.register(PLASMA_FORGE_ARM_UPPER_STRIKER);
        event.register(PLASMA_FORGE_STRIKER_MOUNT);
        event.register(PLASMA_FORGE_STRIKER_LEFT);
        event.register(PLASMA_FORGE_STRIKER_RIGHT);
        event.register(PLASMA_FORGE_PISTON_LEFT);
        event.register(PLASMA_FORGE_PISTON_RIGHT);
        event.register(PLASMA_FORGE_SLIDER_JET);
        event.register(PLASMA_FORGE_ARM_LOWER_JET);
        event.register(PLASMA_FORGE_ARM_UPPER_JET);
        event.register(PLASMA_FORGE_JET);
    }

    @Override
    public void render(FusionMachineBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        switch (machine.kind()) {
            case TORUS -> renderTorus(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
            case KLYSTRON -> renderKlystron(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, facing, false);
            case KLYSTRON_CREATIVE -> renderKlystron(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, facing, true);
            case BOILER -> renderFacingModel(BOILER, poseStack, bufferSource, state, packedLight, packedOverlay, facing, 0.0F);
            case BREEDER -> renderFacingModel(BREEDER, poseStack, bufferSource, state, packedLight, packedOverlay, facing, 0.0F);
            case COLLECTOR -> renderFacingModel(COLLECTOR, poseStack, bufferSource, state, packedLight, packedOverlay, facing, 0.0F);
            case COUPLER -> renderFacingModel(COUPLER, poseStack, bufferSource, state, packedLight, packedOverlay, facing, 90.0F);
            case MHDT -> renderMhdt(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, facing);
            case PLASMA_FORGE -> renderPlasmaForge(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay, facing);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FusionMachineBlockEntity blockEntity) {
        return switch (blockEntity.kind()) {
            case TORUS -> new AABB(blockEntity.getBlockPos()).inflate(8.0D, 6.0D, 8.0D);
            case MHDT -> new AABB(blockEntity.getBlockPos()).inflate(8.0D, 5.0D, 8.0D);
            case PLASMA_FORGE -> new AABB(blockEntity.getBlockPos()).inflate(6.0D, 5.0D, 6.0D);
            default -> new AABB(blockEntity.getBlockPos()).inflate(5.0D, 5.0D, 5.0D);
        };
    }

    private static void renderTorus(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_TORUS), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion(machine.rotor(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_MAGNET), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (machine.torusConnection(0)) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_BOLTS2), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (machine.torusConnection(1)) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_BOLTS4), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (machine.torusConnection(2)) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_BOLTS3), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (machine.torusConnection(3)) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORUS_BOLTS1), poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        if (machine.plasmaEnergy() > 0L) {
            renderTorusPlasma(machine, poseStack, bufferSource, state, packedOverlay);
        }
    }

    private static void renderKlystron(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing,
            boolean creative
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(legacyFusionYaw(facing)));
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(creative ? KLYSTRON_CREATIVE_BODY : KLYSTRON_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 2.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(machine.rotor(partialTick)), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -2.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(creative ? KLYSTRON_CREATIVE_ROTOR : KLYSTRON_ROTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMhdt(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(legacyFusionYaw(facing)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MHDT_TURBINE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(machine.rotor(partialTick) % 15.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MHDT_COILS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPlasmaForge(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(90.0F + legacyFusionYaw(facing)));
        if (machine.connected()) {
            poseStack.pushPose();
            poseStack.translate(-2.0D, 0.0D, 0.0D);
            renderPart(TORUS_BOLTS1, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        renderPart(PLASMA_FORGE_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPlasmaForgePlasma(machine, poseStack, bufferSource, state, packedOverlay);
        renderPlasmaForgeRecipeItem(machine, partialTick, poseStack, bufferSource, packedOverlay);
        renderPlasmaForgeBeam(machine, partialTick, poseStack, bufferSource);
        renderPlasmaForgeArms(machine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderPlasmaForgeArms(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        double[] striker = machine.plasmaForgeStriker(partialTick);
        double[] jet = machine.plasmaForgeJet(partialTick);
        double rotor = machine.plasmaForgeRing(partialTick);

        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion((float) rotor));

        poseStack.pushPose();
        renderPart(PLASMA_FORGE_SLIDER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -2.75D, 2.5D, 0.0D, -striker[0], Axis.Z);
        renderPart(PLASMA_FORGE_ARM_LOWER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -2.75D, 3.75D, 0.0D, -striker[1], Axis.Z);
        renderPart(PLASMA_FORGE_ARM_UPPER_STRIKER, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, -1.5D, 3.75D, 0.0D, -striker[2], Axis.Z);
        renderPart(PLASMA_FORGE_STRIKER_MOUNT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        rotateAround(poseStack, 0.0D, 3.375D, 0.5D, striker[3], Axis.X);
        renderPart(PLASMA_FORGE_STRIKER_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, -striker[4], 0.0D);
        renderPart(PLASMA_FORGE_PISTON_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        rotateAround(poseStack, 0.0D, 3.375D, -0.5D, -striker[3], Axis.X);
        renderPart(PLASMA_FORGE_STRIKER_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, -striker[5], 0.0D);
        renderPart(PLASMA_FORGE_PISTON_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        renderPart(PLASMA_FORGE_SLIDER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 2.75D, 2.5D, 0.0D, jet[0], Axis.Z);
        renderPart(PLASMA_FORGE_ARM_LOWER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 2.75D, 3.75D, 0.0D, jet[1], Axis.Z);
        renderPart(PLASMA_FORGE_ARM_UPPER_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateAround(poseStack, 1.5D, 3.75D, 0.0D, jet[2], Axis.Z);
        renderPart(PLASMA_FORGE_JET, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (machine.plasmaForgeJetActive()) {
            renderPlasmaForgeJet(machine, poseStack, bufferSource);
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderFacingModel(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            Direction facing,
            float extraYaw
    ) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yawQuaternion(extraYaw + legacyFusionYaw(facing)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderTorusPlasma(
            FusionMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        long time = System.currentTimeMillis();
        float alpha = 0.35F + (float) (Math.sin(time / 1000.0D) * 0.25D);
        double mainOsc = sps(time / 1000.0D) % 1.0D;
        double glowOsc = Math.sin(time / 2000.0D) % 1.0D;
        double glowExtra = time / 10000.0D % 1.0D;
        double sparkleSpin = time / 500.0D * -1.0D % 1.0D;
        double sparkleOsc = Math.sin(time / 1000.0D) * 0.5D % 1.0D;

        renderPlasmaLayer(TORUS_PLASMA, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha, 1.0F), 0.0F, (float) mainOsc);
        if (closeEnough(machine, 100.0D)) {
            renderPlasmaLayer(TORUS_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha * 2.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
            renderPlasmaLayer(TORUS_PLASMA_SPARKLE, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 0.75F, 2.0F), (float) sparkleSpin, (float) sparkleOsc);
        }
    }

    private static void renderPlasmaForgePlasma(
            FusionMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        if (machine.plasmaEnergy() <= 0L) {
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(PLASMA_FORGE_PLASMA), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, 0xFF000000);
            return;
        }
        long time = System.currentTimeMillis();
        float alpha = 0.5F + (float) (Math.sin(time / 500.0D) * 0.25D);
        double mainOsc = sps(time / 750.0D) % 1.0D;
        double glowOsc = Math.sin(time / 1000.0D) % 1.0D;
        double glowExtra = time / 10000.0D % 1.0D;
        renderPlasmaLayer(PLASMA_FORGE_PLASMA, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, alpha, 1.0F), 0.0F, (float) mainOsc);
        renderPlasmaLayer(PLASMA_FORGE_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 1.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
        glowOsc = Math.sin(time / 600.0D + 2.0D) % 1.0D;
        glowExtra = time / 5000.0D % 1.0D;
        renderPlasmaLayer(PLASMA_FORGE_PLASMA_GLOW, poseStack, bufferSource, state, packedOverlay, plasmaColor(machine, 1.0F, 2.0F), 0.0F, (float) (glowOsc + glowExtra));
    }

    private static void renderPlasmaLayer(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay,
            int argb,
            float uOffset,
            float vOffset
    ) {
        MachineModelRenderer.renderUnculledTintedUvEyes(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedOverlay, argb, uOffset, vOffset);
    }

    private static void renderPlasmaForgeRecipeItem(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        if (!closeEnough(machine, 35.0D)) {
            return;
        }
        RecipeHolder<PlasmaForgeRecipe> holder = machine.selectedPlasmaForgeRecipe(machine.getLevel()).orElse(null);
        if (holder == null) {
            return;
        }
        ItemStack stack = holder.value().displayIcon();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(yawQuaternion(90.0F));
        poseStack.translate(0.0D, 1.75D + Math.sin((Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTick) * 0.1D) * 0.0625D, 0.0D);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, machine.getLevel(), 0);
        poseStack.popPose();
    }

    private static void renderPlasmaForgeBeam(
            FusionMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        if (!closeEnough(machine, 50.0D) || machine.selectedPlasmaForgeRecipe(machine.getLevel()).isEmpty()) {
            return;
        }
        double offset = ((Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTick) / 15.0D) % 1.0D;
        double in = 0.4375D;
        double bottom = 1.0D;
        double top = 2.5D + offset * 0.125D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        quad(pose, consumer, -in, bottom, in, -in, top, in, -in, top, -in, -in, bottom, -in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, top, in, in, bottom, in, in, bottom, -in, in, top, -in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, bottom, in, in, top, in, -in, top, in, -in, bottom, in, 255, 255, 255, 160, 0);
        quad(pose, consumer, in, top, -in, in, bottom, -in, -in, bottom, -in, -in, top, -in, 255, 255, 255, 160, 0);
    }

    private static void renderPlasmaForgeJet(FusionMachineBlockEntity machine, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        int r = Math.round(clamp(machine.plasmaRed() * 255.0F, 0.0F, 255.0F));
        int g = Math.round(clamp(machine.plasmaGreen() * 255.0F, 0.0F, 255.0F));
        int b = Math.round(clamp(machine.plasmaBlue() * 255.0F, 0.0F, 255.0F));
        double outerLen = 1.0D + (System.nanoTime() & 0xFFFF) / 65535.0D * 0.125D;
        renderJetLayer(pose, consumer, r, g, b, outerLen, 0.01D, 0.125D, 1.375D, 1.625D);
        renderJetLayer(pose, consumer, r, g, b, outerLen * 1.5D, 0.0625D * 1.5D, 0.125D, 1.375D, 1.625D);
    }

    private static void renderJetLayer(PoseStack.Pose pose, VertexConsumer consumer, int r, int g, int b, double outerLen, double narrow, double side, double near, double far) {
        quad(pose, consumer, near, 3.0D, side, far, 3.0D, side, far - narrow, 3.0D - outerLen, side - narrow, near + narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
        quad(pose, consumer, near, 3.0D, -side, far, 3.0D, -side, far - narrow, 3.0D - outerLen, -side + narrow, near + narrow, 3.0D - outerLen, -side + narrow, r, g, b, 255, 0);
        quad(pose, consumer, near, 3.0D, side, near, 3.0D, -side, near + narrow, 3.0D - outerLen, -side + narrow, near + narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
        quad(pose, consumer, far, 3.0D, side, far, 3.0D, -side, far - narrow, 3.0D - outerLen, -side + narrow, far - narrow, 3.0D - outerLen, side - narrow, r, g, b, 255, 0);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4,
            int r,
            int g,
            int b,
            int nearAlpha,
            int farAlpha
    ) {
        vertex(pose, consumer, x1, y1, z1, r, g, b, nearAlpha);
        vertex(pose, consumer, x2, y2, z2, r, g, b, nearAlpha);
        vertex(pose, consumer, x3, y3, z3, r, g, b, farAlpha);
        vertex(pose, consumer, x4, y4, z4, r, g, b, farAlpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, double x, double y, double z, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    private static float legacyFusionYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static void rotateAround(PoseStack poseStack, double x, double y, double z, double degrees, Axis axis) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(axis.quaternion((float) degrees));
        poseStack.translate(-x, -y, -z);
    }

    private static int plasmaColor(FusionMachineBlockEntity machine, float alpha, float multiplier) {
        int a = Math.round(clamp(alpha * 255.0F, 0.0F, 255.0F));
        int r = Math.round(clamp(machine.plasmaRed() * multiplier * 255.0F, 0.0F, 255.0F));
        int g = Math.round(clamp(machine.plasmaGreen() * multiplier * 255.0F, 0.0F, 255.0F));
        int b = Math.round(clamp(machine.plasmaBlue() * multiplier * 255.0F, 0.0F, 255.0F));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double sps(double value) {
        return Math.sin(Math.PI / 2.0D * Math.cos(value));
    }

    private static boolean closeEnough(FusionMachineBlockEntity machine, double distance) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null
                || minecraft.player.distanceToSqr(
                machine.getBlockPos().getX() + 0.5D,
                machine.getBlockPos().getY() + 2.5D,
                machine.getBlockPos().getZ() + 0.5D
        ) < distance * distance;
    }

    private enum Axis {
        X(1.0F, 0.0F, 0.0F),
        Z(0.0F, 0.0F, 1.0F);

        private final float x;
        private final float y;
        private final float z;

        Axis(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Quaternionf quaternion(float degrees) {
            return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), this.x, this.y, this.z));
        }
    }
}
