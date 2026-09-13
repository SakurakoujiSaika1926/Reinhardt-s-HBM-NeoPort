package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the legacy T-51 OBJ on humanoid entities.
 *
 * <p>The item is an ordinary {@code ArmorItem}, so vanilla's armor layer would
 * otherwise try to load a non-existent {@code t51_layer_1.png} and show the
 * purple missing-texture material.  The old model is split into the same
 * humanoid parts as the 1.7.10 renderer and follows the parent model's
 * animation transforms.</p>
 */
public final class T51ArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>>
        extends RenderLayer<T, M> {
    private static final LegacyEntityObjMesh MODEL =
            LegacyEntityObjMesh.load(ReinhardtsHBM.id("models/obj/armor/t51.obj"));
    private static final ResourceLocation HELMET_TEXTURE =
            ReinhardtsHBM.id("textures/armor/t51_helmet.png");
    private static final ResourceLocation CHEST_TEXTURE =
            ReinhardtsHBM.id("textures/armor/t51_chest.png");
    private static final ResourceLocation ARM_TEXTURE =
            ReinhardtsHBM.id("textures/armor/t51_arm.png");
    private static final ResourceLocation LEG_TEXTURE =
            ReinhardtsHBM.id("textures/armor/t51_leg.png");

    public T51ArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        M parentModel = this.getParentModel();
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.HEAD),
                EquipmentSlot.HEAD, parentModel.head, "Helmet", HELMET_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.CHEST),
                EquipmentSlot.CHEST, parentModel.body, "Chest", CHEST_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.CHEST),
                EquipmentSlot.CHEST, parentModel.leftArm, "LeftArm", ARM_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.CHEST),
                EquipmentSlot.CHEST, parentModel.rightArm, "RightArm", ARM_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.LEGS),
                EquipmentSlot.LEGS, parentModel.leftLeg, "LeftLeg", LEG_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.LEGS),
                EquipmentSlot.LEGS, parentModel.rightLeg, "RightLeg", LEG_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.FEET),
                EquipmentSlot.FEET, parentModel.leftLeg, "LeftBoot", LEG_TEXTURE);
        renderIfT51(poseStack, bufferSource, packedLight,
                livingEntity.getItemBySlot(EquipmentSlot.FEET),
                EquipmentSlot.FEET, parentModel.rightLeg, "RightBoot", LEG_TEXTURE);
    }

    private static void renderIfT51(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            ItemStack stack,
            EquipmentSlot slot,
            ModelPart anchor,
            String group,
            ResourceLocation texture
    ) {
        if (!isT51(stack, slot)) {
            return;
        }

        poseStack.pushPose();
        // The OBJ is authored in the old ModelRenderer pixel coordinate
        // system.  Reproduce ModelRendererObj's pivot transform before
        // converting pixels to modern model units.
        anchor.translateAndRotate(poseStack);
        poseStack.translate(-anchor.x / 16.0F, -anchor.y / 16.0F, -anchor.z / 16.0F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        MODEL.renderGroup(
                group,
                poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
        poseStack.popPose();
    }

    private static boolean isT51(ItemStack stack, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> stack.is(HbmItems.T51_HELMET.get());
            case CHEST -> stack.is(HbmItems.T51_PLATE.get());
            case LEGS -> stack.is(HbmItems.T51_LEGS.get());
            case FEET -> stack.is(HbmItems.T51_BOOTS.get());
            default -> false;
        };
    }
}
