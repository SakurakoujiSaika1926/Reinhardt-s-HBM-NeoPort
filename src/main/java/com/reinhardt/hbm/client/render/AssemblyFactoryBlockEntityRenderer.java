package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.AssemblyFactoryBlockEntity;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class AssemblyFactoryBlockEntityRenderer implements LongRangeBlockEntityRenderer<AssemblyFactoryBlockEntity> {
    private static final ResourceLocation SPARKS = ReinhardtsHBM.id("textures/models/machines/assembly_factory_sparks.png");
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_assembly_factory_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_assembly_factory_frame");
    private static final ModelResourceLocation SLIDER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider1");
    private static final ModelResourceLocation SLIDER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider2");
    private static final ModelResourceLocation SLIDER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider3");
    private static final ModelResourceLocation SLIDER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider4");
    private static final ModelResourceLocation ARM_LOWER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower1");
    private static final ModelResourceLocation ARM_LOWER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower2");
    private static final ModelResourceLocation ARM_LOWER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower3");
    private static final ModelResourceLocation ARM_LOWER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower4");
    private static final ModelResourceLocation ARM_UPPER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper1");
    private static final ModelResourceLocation ARM_UPPER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper2");
    private static final ModelResourceLocation ARM_UPPER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper3");
    private static final ModelResourceLocation ARM_UPPER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper4");
    private static final ModelResourceLocation HEAD_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_head1");
    private static final ModelResourceLocation HEAD_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_head2");
    private static final ModelResourceLocation HEAD_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_head3");
    private static final ModelResourceLocation HEAD_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_head4");
    private static final ModelResourceLocation STRIKER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker1");
    private static final ModelResourceLocation STRIKER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker2");
    private static final ModelResourceLocation STRIKER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker3");
    private static final ModelResourceLocation STRIKER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker4");
    private static final ModelResourceLocation BLADE_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade2");
    private static final ModelResourceLocation BLADE_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade4");

    public AssemblyFactoryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(SLIDER_1);
        event.register(SLIDER_2);
        event.register(SLIDER_3);
        event.register(SLIDER_4);
        event.register(ARM_LOWER_1);
        event.register(ARM_LOWER_2);
        event.register(ARM_LOWER_3);
        event.register(ARM_LOWER_4);
        event.register(ARM_UPPER_1);
        event.register(ARM_UPPER_2);
        event.register(ARM_UPPER_3);
        event.register(ARM_UPPER_4);
        event.register(HEAD_1);
        event.register(HEAD_2);
        event.register(HEAD_3);
        event.register(HEAD_4);
        event.register(STRIKER_1);
        event.register(STRIKER_2);
        event.register(STRIKER_3);
        event.register(STRIKER_4);
        event.register(BLADE_2);
        event.register(BLADE_4);
    }

    @Override
    public void render(AssemblyFactoryBlockEntity factory, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        factory.updateClientAnimation();
        BlockState state = factory.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyAssemblyYaw(facing));

        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (factory.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        double slide1 = factory.clientSlider(0, partialTick);
        double slide2 = factory.clientSlider(1, partialTick);
        double[] arm1 = factory.clientArmPositions(0, false, partialTick);
        double[] arm2 = factory.clientArmPositions(0, true, partialTick);
        double[] arm3 = factory.clientArmPositions(1, false, partialTick);
        double[] arm4 = factory.clientArmPositions(1, true, partialTick);

        renderStrikerOne(slide1, arm1, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderSawTwo(slide1, arm2, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderStrikerThree(slide2, arm3, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderSawFour(slide2, arm4, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderRecipeIcons(factory, poseStack, bufferSource, packedLight, packedOverlay);
        renderSparks(factory, partialTick, slide1, slide2, arm2, arm4, poseStack, bufferSource, packedOverlay);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AssemblyFactoryBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 5.0D, 5.0D);
    }

    private static void renderStrikerOne(double slide, double[] arm, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D - slide, 0.0D, 0.0D);
        renderPart(SLIDER_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderHingedArm(arm, -1.0F, -0.9375F, ARM_LOWER_1, ARM_UPPER_1, HEAD_1, STRIKER_1, null, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSawTwo(double slide, double[] arm, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(-0.5D + slide, 0.0D, 0.0D);
        renderPart(SLIDER_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderHingedArm(arm, 1.0F, 0.9375F, ARM_LOWER_2, ARM_UPPER_2, HEAD_2, STRIKER_2, BLADE_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderStrikerThree(double slide, double[] arm, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(-0.5D + slide, 0.0D, 0.0D);
        renderPart(SLIDER_3, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderHingedArm(arm, 1.0F, 0.9375F, ARM_LOWER_3, ARM_UPPER_3, HEAD_3, STRIKER_3, null, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSawFour(double slide, double[] arm, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D - slide, 0.0D, 0.0D);
        renderPart(SLIDER_4, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderHingedArm(arm, -1.0F, -0.9375F, ARM_LOWER_4, ARM_UPPER_4, HEAD_4, STRIKER_4, BLADE_4, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHingedArm(
            double[] arm,
            float sign,
            float zPivot,
            ModelResourceLocation lower,
            ModelResourceLocation upper,
            ModelResourceLocation head,
            ModelResourceLocation striker,
            ModelResourceLocation blade,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.translate(0.0D, 1.625D, zPivot);
        rotateX(poseStack, (float) (sign * arm[0]));
        poseStack.translate(0.0D, -1.625D, -zPivot);
        renderPart(lower, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, 2.375D, zPivot);
        rotateX(poseStack, (float) (sign * arm[1]));
        poseStack.translate(0.0D, -2.375D, -zPivot);
        renderPart(upper, poseStack, bufferSource, state, packedLight, packedOverlay);

        float headPivot = zPivot > 0.0F ? 0.4375F : -0.4375F;
        poseStack.translate(0.0D, 2.375D, headPivot);
        rotateX(poseStack, (float) (sign * arm[2]));
        poseStack.translate(0.0D, -2.375D, -headPivot);
        renderPart(head, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, arm[3], 0.0D);
        renderPart(striker, poseStack, bufferSource, state, packedLight, packedOverlay);

        if (blade != null) {
            poseStack.translate(0.0D, 1.625D, zPivot > 0.0F ? 0.3125D : -0.3125D);
            rotateX(poseStack, (float) (-sign * arm[4]));
            poseStack.translate(0.0D, -1.625D, zPivot > 0.0F ? -0.3125D : 0.3125D);
            renderPart(blade, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderRecipeIcons(AssemblyFactoryBlockEntity factory, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = factory.getLevel();
        if (minecraft.player == null || level == null) {
            return;
        }
        double distance = minecraft.player.distanceToSqr(
                factory.getBlockPos().getX() + 0.5D,
                factory.getBlockPos().getY() + 1.0D,
                factory.getBlockPos().getZ() + 0.5D
        );
        if (distance >= 35.0D * 35.0D) {
            return;
        }

        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            RecipeHolder<AssemblyMachineRecipe> holder = factory.selectedRecipe(module, level).orElse(null);
            if (holder == null) {
                continue;
            }
            ItemStack stack = holder.value().result().copy();
            if (stack.isEmpty()) {
                continue;
            }
            stack.setCount(1);

            poseStack.pushPose();
            poseStack.translate(1.5D - module, 1.0625D, 0.0D);
            rotateY(poseStack, 90.0F);
            rotateX(poseStack, -90.0F);
            rotateZ(poseStack, -90.0F);
            poseStack.scale(0.85F, 0.85F, 0.85F);
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, null, 0);
            poseStack.popPose();
        }
    }

    private static void renderSparks(
            AssemblyFactoryBlockEntity factory,
            float partialTick,
            double slide1,
            double slide2,
            double[] arm2,
            double[] arm4,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = factory.getLevel();
        if (minecraft.player == null || level == null) {
            return;
        }
        double distance = minecraft.player.distanceToSqr(
                factory.getBlockPos().getX() + 0.5D,
                factory.getBlockPos().getY() + 1.0D,
                factory.getBlockPos().getZ() + 0.5D
        );
        if (distance >= 35.0D * 35.0D) {
            return;
        }

        double uMin = ((level.getGameTime() / 10.0D) + partialTick) % 1.0D;
        double uMax = uMin + 1.0D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(SPARKS));
        if (arm2[3] <= -0.375D) {
            poseStack.pushPose();
            poseStack.translate(0.5D + slide1, 1.0625D, -arm2[2] / 45.0D);
            drawSparkBeam(poseStack, consumer, packedOverlay, 1.0D, uMin, uMax);
            poseStack.popPose();
        }
        if (arm4[3] <= -0.375D) {
            poseStack.pushPose();
            poseStack.translate(-0.5D - slide2, 1.0625D, arm4[2] / 45.0D);
            drawSparkBeam(poseStack, consumer, packedOverlay, -1.0D, uMin, uMax);
            poseStack.popPose();
        }
    }

    private static void drawSparkBeam(PoseStack poseStack, VertexConsumer consumer, int packedOverlay, double zSign, double uMin, double uMax) {
        PoseStack.Pose pose = poseStack.last();
        double wide = 0.1875D;
        double narrow = 0.0D;
        double length = 1.25D * zSign;
        double epsilon = 0.01D;
        sparkVertex(consumer, pose, -epsilon, -wide, length, uMin + 0.5D, 0.0D, 0);
        sparkVertex(consumer, pose, -epsilon, wide, length, uMin + 0.5D, 1.0D, 0);
        sparkVertex(consumer, pose, -epsilon, narrow, 0.0D, uMax + 0.5D, 1.0D, 255);
        sparkVertex(consumer, pose, -epsilon, -narrow, 0.0D, uMax + 0.5D, 0.0D, 255);

        sparkVertex(consumer, pose, epsilon, -wide, length, uMin, 1.0D, 0);
        sparkVertex(consumer, pose, epsilon, wide, length, uMin, 0.0D, 0);
        sparkVertex(consumer, pose, epsilon, narrow, 0.0D, uMax, 0.0D, 255);
        sparkVertex(consumer, pose, epsilon, -narrow, 0.0D, uMax, 1.0D, 255);
    }

    private static void sparkVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, double u, double v, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, alpha)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float legacyAssemblyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static void rotateX(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }

    private static void rotateZ(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F)));
    }
}
