package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the complete legacy pylon OBJ with stable item-space bounds. */
public final class PowerPylonItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation RED_CONNECTOR =
            MachineModelRenderer.standalone("block/red_connector_world");
    private static final ModelResourceLocation CONNECTOR_RED_SUPER =
            MachineModelRenderer.standalone("block/connector_red_super_world");
    private static final ModelResourceLocation MEDIUM_WOOD =
            MachineModelRenderer.standalone("block/red_pylon_medium_wood_world");
    private static final ModelResourceLocation MEDIUM_WOOD_TRANSFORMER =
            MachineModelRenderer.standalone("block/red_pylon_medium_transformer_world");
    private static final ModelResourceLocation MEDIUM_STEEL =
            MachineModelRenderer.standalone("block/red_pylon_steel_world");
    private static final ModelResourceLocation MEDIUM_STEEL_TRANSFORMER =
            MachineModelRenderer.standalone("block/red_pylon_steel_transformer_world");
    private static final ModelResourceLocation LARGE =
            MachineModelRenderer.standalone("block/red_pylon_large_world");
    private static final ModelResourceLocation SUBSTATION =
            MachineModelRenderer.standalone("block/substation_world");
    private final PowerPylonBlock.Kind kind;

    public PowerPylonItemRenderer(PowerPylonBlock.Kind kind) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.kind = kind;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (this.kind == PowerPylonBlock.Kind.RED_CONNECTOR
                || this.kind == PowerPylonBlock.Kind.CONNECTOR_RED_SUPER) {
            renderConnectorLegacy(context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (isMedium()) {
            renderMediumLegacy(context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (this.kind == PowerPylonBlock.Kind.RED_PYLON_LARGE) {
            renderLargeLegacy(context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (this.kind == PowerPylonBlock.Kind.SUBSTATION) {
            renderSubstationLegacy(context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        RenderSpec spec = spec();
        BlockState state = state();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
        } else if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(spec.scale(), spec.scale(), spec.scale());
        poseStack.translate(-spec.centerX(), -spec.centerY(), -spec.centerZ());
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(spec.model()), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderLargeLegacy(ItemDisplayContext context, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            // ItemRenderLibrary: renderInventory() translates by -5 and scales by 2.25.
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(2.25F, 2.25F, 2.25F);
        }
        // ItemRenderLibrary: renderCommon() runs after renderInventory() for GUI items too.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LARGE), poseStack,
                bufferSource, state(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderSubstationLegacy(ItemDisplayContext context, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            // ItemRenderLibrary: renderInventory() translates by -2.5 and scales by 4.5.
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        }
        // ItemRenderLibrary: renderCommon() runs after renderInventory() for GUI items too.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SUBSTATION), poseStack,
                bufferSource, state(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderMediumLegacy(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // RenderPylonMedium's 1.7.10 ItemRenderBase transform, expressed in PoseStack units.
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(1.0F, -5.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        }
        // RenderPylonMedium#renderCommonWithStack runs after renderInventory() for GUI items too.
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.75F, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model()), poseStack,
                bufferSource, state(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderConnectorLegacy(ItemDisplayContext context, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, this.kind == PowerPylonBlock.Kind.RED_CONNECTOR ? -3.5F : -5.0F, 0.0F);
            poseStack.scale(7.0F, 7.0F, 7.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model()), poseStack,
                bufferSource, state(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private boolean isMedium() {
        return switch (this.kind) {
            case RED_PYLON_MEDIUM_WOOD,
                 RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                 RED_PYLON_MEDIUM_STEEL,
                 RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> true;
            default -> false;
        };
    }

    private ModelResourceLocation model() {
        return switch (this.kind) {
            case RED_CONNECTOR -> RED_CONNECTOR;
            case CONNECTOR_RED_SUPER -> CONNECTOR_RED_SUPER;
            case RED_PYLON -> throw new IllegalStateException("Legacy red pylon uses ModelPylon geometry");
            case RED_PYLON_MEDIUM_WOOD -> MEDIUM_WOOD;
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> MEDIUM_WOOD_TRANSFORMER;
            case RED_PYLON_MEDIUM_STEEL -> MEDIUM_STEEL;
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> MEDIUM_STEEL_TRANSFORMER;
            case RED_PYLON_LARGE -> LARGE;
            case SUBSTATION -> SUBSTATION;
        };
    }

    private BlockState state() {
        return switch (this.kind) {
            case RED_CONNECTOR -> HbmBlocks.RED_CONNECTOR.get().defaultBlockState();
            case CONNECTOR_RED_SUPER -> HbmBlocks.CONNECTOR_RED_SUPER.get().defaultBlockState();
            case RED_PYLON -> HbmBlocks.RED_PYLON.get().defaultBlockState();
            case RED_PYLON_MEDIUM_WOOD -> HbmBlocks.RED_PYLON_MEDIUM_WOOD.get().defaultBlockState();
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> HbmBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get().defaultBlockState();
            case RED_PYLON_MEDIUM_STEEL -> HbmBlocks.RED_PYLON_MEDIUM_STEEL.get().defaultBlockState();
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> HbmBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get().defaultBlockState();
            case RED_PYLON_LARGE -> HbmBlocks.RED_PYLON_LARGE.get().defaultBlockState();
            case SUBSTATION -> HbmBlocks.SUBSTATION.get().defaultBlockState();
        };
    }

    private RenderSpec spec() {
        return switch (this.kind) {
            case RED_CONNECTOR -> new RenderSpec(model(), 2.0F, 0.0F, 0.28125F, 0.0F);
            case CONNECTOR_RED_SUPER -> new RenderSpec(model(), 1.35F, 0.0F, 0.5F, 0.0F);
            case RED_PYLON -> throw new IllegalStateException("Legacy red pylon uses ModelPylon geometry");
            case RED_PYLON_LARGE, SUBSTATION -> throw new IllegalStateException("Legacy item transforms handle this pylon");
            default -> throw new IllegalStateException("Medium pylons use the legacy item transform");
        };
    }

    private record RenderSpec(ModelResourceLocation model, float scale, float centerX, float centerY, float centerZ) {
    }
}
