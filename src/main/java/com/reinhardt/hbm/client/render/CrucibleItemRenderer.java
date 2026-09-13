package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.item.LegacyCrucibleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 1.7.10 ItemRenderCrucible transforms, split into the original OBJ parts. */
public final class CrucibleItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation HILT = MachineModelRenderer.standalone("weapons/crucible_hilt");
    private static final ModelResourceLocation GUARD_LEFT = MachineModelRenderer.standalone("weapons/crucible_guard_left");
    private static final ModelResourceLocation GUARD_RIGHT = MachineModelRenderer.standalone("weapons/crucible_guard_right");
    private static final ModelResourceLocation BLADE = MachineModelRenderer.standalone("weapons/crucible_blade");

    public CrucibleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(HILT);
        event.register(GUARD_LEFT);
        event.register(GUARD_RIGHT);
        event.register(BLADE);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        boolean charged = LegacyCrucibleItem.isCharged(stack);
        boolean fullBrightBlade = switch (context) {
            case GUI -> {
                // ItemRenderCrucible#INVENTORY.  Forge 1.7.10 invokes this
                // non-block custom renderer in the 16-pixel GUI coordinate
                // space, so its authored (2, 14) offset and 1.5 scale must be
                // converted to the 1.21 item-unit pose space here.
                poseStack.translate(2.0F / 16.0F, 14.0F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(1.5F / 16.0F, 1.5F / 16.0F, 1.5F / 16.0F);
                yield false;
            }
            case GROUND -> {
                // ItemRenderCrucible#ENTITY, then its intentional fall-through
                // into EQUIPPED.
                poseStack.translate(-0.75F, 0.6F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.translate(0.75F, -0.4F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.15F, 0.15F, 0.15F);
                yield true;
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderCrucible#EQUIPPED. The 1.7.10 render type had no
                // handedness distinction; Minecraft supplies that outside this renderer.
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.translate(0.75F, -0.4F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.15F, 0.15F, 0.15F);
                yield true;
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ItemRenderer#renderItemInFirstPerson applied this fixed
                // 0.4 scale before Forge dispatched the custom renderer.
                // ForgeHooksClient#renderEquippedItem then used its literal
                // non-EQUIPPED_BLOCK path because ItemRenderCrucible returns
                // false for that helper in first person. Keep that call order
                // here; these are item-specific legacy values, not a fit.
                poseStack.scale(0.4F, 0.4F, 0.4F);
                poseStack.translate(0.0F, -0.3F, 0.0F);
                poseStack.scale(1.5F, 1.5F, 1.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
                poseStack.translate(-0.9375F, -0.0625F, 0.0F);

                // ItemRenderCrucible#EQUIPPED_FIRST_PERSON. Its animation
                // source (HbmAnimations) has no 1.21.1 counterpart, so no
                // substitute animation or transformed fallback is invented.
                poseStack.translate(1.5F, -0.3F, 0.0F);
                poseStack.scale(0.3F, 0.3F, 0.3F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                yield true;
            }
            case NONE, HEAD, FIXED -> throw new IllegalArgumentException(
                    "ItemRenderCrucible had no 1.7.10 ItemRenderType for " + context);
        };

        render(HILT, poseStack, bufferSource, packedLight, packedOverlay);
        renderGuard(GUARD_LEFT, poseStack, bufferSource, packedLight, packedOverlay, charged, true);
        renderGuard(GUARD_RIGHT, poseStack, bufferSource, packedLight, packedOverlay, charged, false);
        if (charged) {
            poseStack.pushPose();
            poseStack.translate(0.005F, 0.0F, 0.0F);
            if (fullBrightBlade) {
                MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(BLADE), poseStack, bufferSource,
                        Blocks.IRON_BLOCK.defaultBlockState(), packedOverlay);
            } else {
                render(BLADE, poseStack, bufferSource, packedLight, packedOverlay);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderGuard(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay, boolean charged, boolean left) {
        poseStack.pushPose();
        if (left) {
            poseStack.translate(0.0F, 3.0F, 0.5F);
            poseStack.mulPose(Axis.XN.rotationDegrees(charged ? 0.0F : 90.0F));
            poseStack.translate(0.0F, -3.0F, -0.5F);
        } else {
            poseStack.translate(0.0F, 3.0F, -0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(charged ? 0.0F : 90.0F));
            poseStack.translate(0.0F, -3.0F, 0.5F);
        }
        render(model, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
    }
}
