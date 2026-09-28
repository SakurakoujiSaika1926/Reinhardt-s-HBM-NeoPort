package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

public final class LargeFactoryItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation ASSEMBLY_BASE = MachineModelRenderer.standalone("block/machine_assembly_factory_base");
    private static final ModelResourceLocation ASSEMBLY_FRAME = MachineModelRenderer.standalone("block/machine_assembly_factory_frame");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider1");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider2");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider3");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider4");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower1");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower2");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower3");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower4");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper1");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper2");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper3");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper4");
    private static final ModelResourceLocation ASSEMBLY_HEAD_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_head1");
    private static final ModelResourceLocation ASSEMBLY_HEAD_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_head2");
    private static final ModelResourceLocation ASSEMBLY_HEAD_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_head3");
    private static final ModelResourceLocation ASSEMBLY_HEAD_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_head4");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker1");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker2");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker3");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker4");
    private static final ModelResourceLocation ASSEMBLY_BLADE_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade2");
    private static final ModelResourceLocation ASSEMBLY_BLADE_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade4");

    private static final ModelResourceLocation CHEMICAL_BASE = MachineModelRenderer.standalone("block/machine_chemical_factory_base");
    private static final ModelResourceLocation CHEMICAL_FRAME = MachineModelRenderer.standalone("block/machine_chemical_factory_frame");
    private static final ModelResourceLocation CHEMICAL_FAN_1 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan1");
    private static final ModelResourceLocation CHEMICAL_FAN_2 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan2");
    private static final List<ModelResourceLocation> CHEMICAL_MODELS = List.of(
            CHEMICAL_BASE,
            CHEMICAL_FRAME,
            CHEMICAL_FAN_1,
            CHEMICAL_FAN_2
    );

    private static final List<ModelResourceLocation> ASSEMBLY_MODELS = List.of(
            ASSEMBLY_BASE,
            ASSEMBLY_FRAME,
            ASSEMBLY_SLIDER_1,
            ASSEMBLY_SLIDER_2,
            ASSEMBLY_SLIDER_3,
            ASSEMBLY_SLIDER_4,
            ASSEMBLY_ARM_LOWER_1,
            ASSEMBLY_ARM_LOWER_2,
            ASSEMBLY_ARM_LOWER_3,
            ASSEMBLY_ARM_LOWER_4,
            ASSEMBLY_ARM_UPPER_1,
            ASSEMBLY_ARM_UPPER_2,
            ASSEMBLY_ARM_UPPER_3,
            ASSEMBLY_ARM_UPPER_4,
            ASSEMBLY_HEAD_1,
            ASSEMBLY_HEAD_2,
            ASSEMBLY_HEAD_3,
            ASSEMBLY_HEAD_4,
            ASSEMBLY_STRIKER_1,
            ASSEMBLY_STRIKER_2,
            ASSEMBLY_STRIKER_3,
            ASSEMBLY_STRIKER_4,
            ASSEMBLY_BLADE_2,
            ASSEMBLY_BLADE_4
    );

    public LargeFactoryItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ASSEMBLY_MODELS.forEach(event::register);
        CHEMICAL_MODELS.forEach(event::register);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == HbmBlocks.MACHINE_CHEMICAL_FACTORY.get()) {
            renderChemicalFactoryItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        renderAssemblyFactoryItem(context, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderAssemblyFactoryItem(ItemDisplayContext context, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get().defaultBlockState();
        poseStack.pushPose();
        applyAssemblyFactoryItemPose(context, poseStack);
        for (ModelResourceLocation model : ASSEMBLY_MODELS) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void applyAssemblyFactoryItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.19F, 0.19F, 0.19F);
        poseStack.translate(0.0F, -1.5F, 0.0F);
    }

    private static void renderChemicalFactoryItem(ItemDisplayContext context, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_CHEMICAL_FACTORY.get().defaultBlockState();
        poseStack.pushPose();
        applyChemicalFactoryItemPose(context, poseStack);
        for (ModelResourceLocation model : CHEMICAL_MODELS) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                    state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void applyChemicalFactoryItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        // The legacy chemical and assembly factories use the same inventory
        // transform.  Keeping this machine-owned copy avoids a shared pose
        // while preserving that exact parity now that the unrendered
        // Plane.001 export no longer distorts the item envelope.
        poseStack.scale(0.19F, 0.19F, 0.19F);
        poseStack.translate(0.0F, -1.5F, 0.0F);
    }
}
