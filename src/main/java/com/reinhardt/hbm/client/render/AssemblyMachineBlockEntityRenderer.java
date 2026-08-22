package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class AssemblyMachineBlockEntityRenderer implements BlockEntityRenderer<AssemblyMachineBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_assembly_machine_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_assembly_machine_frame");
    private static final ModelResourceLocation RING = MachineModelRenderer.standalone("block/machine_assembly_machine_ring");
    private static final ModelResourceLocation RING2 = MachineModelRenderer.standalone("block/machine_assembly_machine_ring2");
    private static final ModelResourceLocation ARM_LOWER_1 = MachineModelRenderer.standalone("block/machine_assembly_machine_arm_lower1");
    private static final ModelResourceLocation ARM_LOWER_2 = MachineModelRenderer.standalone("block/machine_assembly_machine_arm_lower2");
    private static final ModelResourceLocation ARM_UPPER_1 = MachineModelRenderer.standalone("block/machine_assembly_machine_arm_upper1");
    private static final ModelResourceLocation ARM_UPPER_2 = MachineModelRenderer.standalone("block/machine_assembly_machine_arm_upper2");
    private static final ModelResourceLocation HEAD_1 = MachineModelRenderer.standalone("block/machine_assembly_machine_head1");
    private static final ModelResourceLocation HEAD_2 = MachineModelRenderer.standalone("block/machine_assembly_machine_head2");
    private static final ModelResourceLocation SPIKE_1 = MachineModelRenderer.standalone("block/machine_assembly_machine_spike1");
    private static final ModelResourceLocation SPIKE_2 = MachineModelRenderer.standalone("block/machine_assembly_machine_spike2");

    public AssemblyMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(RING);
        event.register(RING2);
        event.register(ARM_LOWER_1);
        event.register(ARM_LOWER_2);
        event.register(ARM_UPPER_1);
        event.register(ARM_UPPER_2);
        event.register(HEAD_1);
        event.register(HEAD_2);
        event.register(SPIKE_1);
        event.register(SPIKE_2);
    }

    @Override
    public void render(AssemblyMachineBlockEntity assemblyMachine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        assemblyMachine.updateClientAnimation();
        BlockState state = assemblyMachine.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyAssemblyYaw(facing));
        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (assemblyMachine.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        rotateY(poseStack, (float) assemblyMachine.clientRing(partialTick));
        renderPart(RING, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(RING2, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderArmOne(assemblyMachine.clientArmPositions(0, partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderArmTwo(assemblyMachine.clientArmPositions(1, partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderRecipeIcon(assemblyMachine, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AssemblyMachineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 4.0D, 3.0D);
    }

    private static float legacyAssemblyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static void renderArmOne(
            double[] arm,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, 0.9375F);
        rotateX(poseStack, (float) arm[0]);
        poseStack.translate(0.0F, -1.625F, -0.9375F);
        renderPart(ARM_LOWER_1, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, 0.9375F);
        rotateX(poseStack, (float) arm[1]);
        poseStack.translate(0.0F, -2.375F, -0.9375F);
        renderPart(ARM_UPPER_1, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, 0.4375F);
        rotateX(poseStack, (float) arm[2]);
        poseStack.translate(0.0F, -2.375F, -0.4375F);
        renderPart(HEAD_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, (float) arm[3], 0.0F);
        renderPart(SPIKE_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderArmTwo(
            double[] arm,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.625F, -0.9375F);
        rotateX(poseStack, (float) -arm[0]);
        poseStack.translate(0.0F, -1.625F, 0.9375F);
        renderPart(ARM_LOWER_2, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, -0.9375F);
        rotateX(poseStack, (float) -arm[1]);
        poseStack.translate(0.0F, -2.375F, 0.9375F);
        renderPart(ARM_UPPER_2, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, -0.4375F);
        rotateX(poseStack, (float) -arm[2]);
        poseStack.translate(0.0F, -2.375F, 0.4375F);
        renderPart(HEAD_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, (float) arm[3], 0.0F);
        renderPart(SPIKE_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderRecipeIcon(
            AssemblyMachineBlockEntity assemblyMachine,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = assemblyMachine.getLevel();
        if (minecraft.player == null || level == null) {
            return;
        }
        double distance = minecraft.player.distanceToSqr(
                assemblyMachine.getBlockPos().getX() + 0.5D,
                assemblyMachine.getBlockPos().getY() + 1.0D,
                assemblyMachine.getBlockPos().getZ() + 0.5D
        );
        if (distance >= 35.0D * 35.0D) {
            return;
        }

        RecipeHolder<AssemblyMachineRecipe> holder = assemblyMachine.selectedRecipe(level).orElse(null);
        if (holder == null) {
            return;
        }
        ItemStack stack = holder.value().result().copy();
        if (stack.isEmpty()) {
            return;
        }
        stack.setCount(1);

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0625F, 0.0F);
        rotateX(poseStack, -90.0F);
        rotateZ(poseStack, -90.0F);
        poseStack.scale(0.85F, 0.85F, 0.85F);
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                null,
                0
        );
        poseStack.popPose();
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
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
