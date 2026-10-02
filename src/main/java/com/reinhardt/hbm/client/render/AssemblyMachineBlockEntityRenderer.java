package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.AssemblyMachineBlock;
import com.reinhardt.hbm.block.TaczAmmoAssemblerBlock;
import com.reinhardt.hbm.blockentity.AssemblyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.TaczAmmoAssemblerBlockEntity;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

public class AssemblyMachineBlockEntityRenderer implements LongRangeBlockEntityRenderer<AssemblyMachineBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
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
    private final double[] arm1 = new double[4];
    private final double[] arm2 = new double[4];

    public AssemblyMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        AssemblyMachineBlockEntity.setClientRecipeIconSnapshotUpdater(
                AssemblyMachineBlockEntityRenderer::updateRecipeIconSnapshot);
    }

    static void invalidateRecipeIconSnapshotsAfterBake() {
        AssemblyMachineBlockEntity.setClientRecipeIconSnapshotUpdater(
                AssemblyMachineBlockEntityRenderer::updateRecipeIconSnapshot);
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
        if (assemblyMachine instanceof TaczAmmoAssemblerBlockEntity ammoAssembler) {
            renderTaczAmmoAssembler(ammoAssembler, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        renderAssemblyMachine(assemblyMachine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderAssemblyMachine(
            AssemblyMachineBlockEntity assemblyMachine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        assemblyMachine.updateClientAnimation();
        AssemblyMachineBlockEntity.RecipeIconRenderSnapshot recipeIcon = assemblyMachine.recipeIconRenderSnapshot();
        BlockState state = recipeIcon.state();
        Direction facing = assemblyMachineFacing(state);

        poseStack.pushPose();
        applyAssemblyMachineWorldPose(poseStack, facing);
        renderAssemblerComponents(assemblyMachine, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderAssemblyMachineRecipeIcon(assemblyMachine, recipeIcon, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderTaczAmmoAssembler(
            TaczAmmoAssemblerBlockEntity ammoAssembler,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ammoAssembler.updateClientAnimation();
        AssemblyMachineBlockEntity.RecipeIconRenderSnapshot recipeIcon = ammoAssembler.recipeIconRenderSnapshot();
        BlockState state = recipeIcon.state();
        Direction facing = taczAmmoAssemblerFacing(state);

        poseStack.pushPose();
        applyTaczAmmoAssemblerWorldPose(poseStack, facing);
        renderAssemblerComponents(ammoAssembler, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderTaczAmmoAssemblerRecipeIcon(ammoAssembler, recipeIcon, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderAssemblerComponents(
            AssemblyMachineBlockEntity assemblyMachine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (assemblyMachine.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) assemblyMachine.clientRing(partialTick)));
        renderPart(RING, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(RING2, poseStack, bufferSource, state, packedLight, packedOverlay);
        assemblyMachine.writeClientArmPositions(0, partialTick, this.arm1);
        assemblyMachine.writeClientArmPositions(1, partialTick, this.arm2);
        renderArmOne(this.arm1, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderArmTwo(this.arm2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AssemblyMachineBlockEntity blockEntity) {
        if (blockEntity instanceof TaczAmmoAssemblerBlockEntity ammoAssembler) {
            return taczAmmoAssemblerBounds(ammoAssembler);
        }
        return assemblyMachineBounds(blockEntity);
    }

    private static AABB assemblyMachineBounds(AssemblyMachineBlockEntity assemblyMachine) {
        /*
         * Exhaustive arm-state envelope: all five target poses, every 15-degree
         * transition, stop/retract paths, -0.75 striker travel, and arbitrary
         * ring yaw. The exact extrema from the face-used vertices and legal
         * animation paths are retained at full double precision below.
         */
        AABB machineBounds = RenderBounds.localAnalytic(assemblyMachine.getBlockPos(),
                -1.2674355063806066D, 0.0D, -1.2674355063806066D,
                2.2674355063806066D, 3.2494602153443313D, 2.2674355063806066D);
        return machineBounds.minmax(assemblyMachine.recipeIconRenderSnapshot().bounds());
    }

    private static AABB taczAmmoAssemblerBounds(TaczAmmoAssemblerBlockEntity ammoAssembler) {
        /* Same authored geometry, retained as a TACZ-owned culling path. */
        AABB machineBounds = RenderBounds.localAnalytic(ammoAssembler.getBlockPos(),
                -1.2674355063806066D, 0.0D, -1.2674355063806066D,
                2.2674355063806066D, 3.2494602153443313D, 2.2674355063806066D);
        return machineBounds.minmax(ammoAssembler.recipeIconRenderSnapshot().bounds());
    }

    private static void applyAssemblyMachineWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Assembly machine facing must be horizontal: " + facing);
        }
    }

    private static void applyTaczAmmoAssemblerWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("TACZ ammunition assembler facing must be horizontal: " + facing);
        }
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
        poseStack.mulPose(Axis.XP.rotationDegrees((float) arm[0]));
        poseStack.translate(0.0F, -1.625F, -0.9375F);
        renderPart(ARM_LOWER_1, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, 0.9375F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) arm[1]));
        poseStack.translate(0.0F, -2.375F, -0.9375F);
        renderPart(ARM_UPPER_1, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, 0.4375F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) arm[2]));
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
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -arm[0]));
        poseStack.translate(0.0F, -1.625F, 0.9375F);
        renderPart(ARM_LOWER_2, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, -0.9375F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -arm[1]));
        poseStack.translate(0.0F, -2.375F, 0.9375F);
        renderPart(ARM_UPPER_2, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 2.375F, -0.4375F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -arm[2]));
        poseStack.translate(0.0F, -2.375F, 0.4375F);
        renderPart(HEAD_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, (float) arm[3], 0.0F);
        renderPart(SPIKE_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAssemblyMachineRecipeIcon(
            AssemblyMachineBlockEntity assemblyMachine,
            AssemblyMachineBlockEntity.RecipeIconRenderSnapshot snapshot,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || assemblyMachine.getLevel() == null) {
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

        ItemStack stack = snapshot.renderedItem();
        if (stack.isEmpty() || !(snapshot.modelToken() instanceof net.minecraft.client.resources.model.BakedModel itemModel)) {
            return;
        }
        stack.setCount(1);

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0625F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        minecraft.getItemRenderer().render(
                stack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                itemModel
        );
        poseStack.popPose();
    }

    private static void renderTaczAmmoAssemblerRecipeIcon(
            TaczAmmoAssemblerBlockEntity ammoAssembler,
            AssemblyMachineBlockEntity.RecipeIconRenderSnapshot snapshot,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || ammoAssembler.getLevel() == null) {
            return;
        }
        double distance = minecraft.player.distanceToSqr(
                ammoAssembler.getBlockPos().getX() + 0.5D,
                ammoAssembler.getBlockPos().getY() + 1.0D,
                ammoAssembler.getBlockPos().getZ() + 0.5D
        );
        if (distance >= 35.0D * 35.0D) {
            return;
        }

        ItemStack stack = snapshot.renderedItem();
        if (stack.isEmpty() || !(snapshot.modelToken() instanceof net.minecraft.client.resources.model.BakedModel itemModel)) {
            return;
        }
        stack.setCount(1);

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0625F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        minecraft.getItemRenderer().render(
                stack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                itemModel
        );
        poseStack.popPose();
    }

    /** Main-thread recipe/model resolution; the culling worker only consumes the published record. */
    private static void updateRecipeIconSnapshot(AssemblyMachineBlockEntity assemblyMachine) {
        if (assemblyMachine instanceof TaczAmmoAssemblerBlockEntity ammoAssembler) {
            updateTaczAmmoAssemblerRecipeIconSnapshot(ammoAssembler);
            return;
        }
        updateAssemblyMachineRecipeIconSnapshot(assemblyMachine);
    }

    private static void updateAssemblyMachineRecipeIconSnapshot(AssemblyMachineBlockEntity assemblyMachine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(assemblyMachine::refreshClientRecipeIconSnapshot);
            return;
        }
        Level level = assemblyMachine.getLevel();
        if (level == null) {
            return;
        }

        RecipeHolder<AssemblyMachineRecipe> holder = assemblyMachine.selectedRecipe(level).orElse(null);
        ItemStack source = holder == null ? ItemStack.EMPTY : holder.value().result().copyWithCount(1);
        BlockState state = assemblyMachine.getBlockState();
        long generation = com.reinhardt.hbm.recipe.MachineRecipeCache.generation();
        long revision = AssemblyMachineBlockEntity.clientRecipeIconSnapshotRevision();
        AssemblyMachineBlockEntity.RecipeIconRenderSnapshot previous = assemblyMachine.recipeIconRenderSnapshot();
        if (previous.revision() == revision
                && previous.recipeGeneration() == generation
                && previous.state() == state
                && java.util.Objects.equals(previous.recipeId(), holder == null ? null : holder.id())
                && ItemStack.isSameItemSameComponents(previous.sourceItem(), source)) {
            assemblyMachine.publishClientRecipeIconSnapshot(previous);
            return;
        }

        Direction facing = assemblyMachineFacing(state);
        PoseStack itemPose = new PoseStack();
        applyAssemblyMachineWorldPose(itemPose, facing);
        itemPose.translate(0.0F, 1.0625F, 0.0F);
        itemPose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        itemPose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        itemPose.scale(0.85F, 0.85F, 0.85F);
        BakedItemGeometrySnapshot.Result geometry = BakedItemGeometrySnapshot.build(
                source, itemPose, assemblyMachine.getBlockPos(), level);
        assemblyMachine.publishClientRecipeIconSnapshot(new AssemblyMachineBlockEntity.RecipeIconRenderSnapshot(
                state, holder == null ? null : holder.id(), geometry.sourceItem(), geometry.renderedItem(),
                geometry.modelToken(),
                geometry.bounds(), generation, revision));
    }

    private static void updateTaczAmmoAssemblerRecipeIconSnapshot(TaczAmmoAssemblerBlockEntity ammoAssembler) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(ammoAssembler::refreshClientRecipeIconSnapshot);
            return;
        }
        Level level = ammoAssembler.getLevel();
        if (level == null) {
            return;
        }

        RecipeHolder<AssemblyMachineRecipe> holder = ammoAssembler.selectedRecipe(level).orElse(null);
        ItemStack source = holder == null ? ItemStack.EMPTY : holder.value().result().copyWithCount(1);
        BlockState state = ammoAssembler.getBlockState();
        long generation = com.reinhardt.hbm.recipe.MachineRecipeCache.generation();
        long revision = AssemblyMachineBlockEntity.clientRecipeIconSnapshotRevision();
        AssemblyMachineBlockEntity.RecipeIconRenderSnapshot previous = ammoAssembler.recipeIconRenderSnapshot();
        if (previous.revision() == revision
                && previous.recipeGeneration() == generation
                && previous.state() == state
                && java.util.Objects.equals(previous.recipeId(), holder == null ? null : holder.id())
                && ItemStack.isSameItemSameComponents(previous.sourceItem(), source)) {
            ammoAssembler.publishClientRecipeIconSnapshot(previous);
            return;
        }

        Direction facing = taczAmmoAssemblerFacing(state);
        PoseStack itemPose = new PoseStack();
        applyTaczAmmoAssemblerWorldPose(itemPose, facing);
        itemPose.translate(0.0F, 1.0625F, 0.0F);
        itemPose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        itemPose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        itemPose.scale(0.85F, 0.85F, 0.85F);
        BakedItemGeometrySnapshot.Result geometry = BakedItemGeometrySnapshot.build(
                source, itemPose, ammoAssembler.getBlockPos(), level);
        ammoAssembler.publishClientRecipeIconSnapshot(new AssemblyMachineBlockEntity.RecipeIconRenderSnapshot(
                state, holder == null ? null : holder.id(), geometry.sourceItem(), geometry.renderedItem(),
                geometry.modelToken(),
                geometry.bounds(), generation, revision));
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static Direction assemblyMachineFacing(BlockState state) {
        if (!state.hasProperty(AssemblyMachineBlock.FACING)) {
            throw new IllegalStateException("Assembly machine state is missing its owned facing property: " + state);
        }
        return state.getValue(AssemblyMachineBlock.FACING);
    }

    private static Direction taczAmmoAssemblerFacing(BlockState state) {
        if (!state.hasProperty(TaczAmmoAssemblerBlock.FACING)) {
            throw new IllegalStateException("TACZ ammunition assembler state is missing its owned facing property: " + state);
        }
        return state.getValue(TaczAmmoAssemblerBlock.FACING);
    }
}
