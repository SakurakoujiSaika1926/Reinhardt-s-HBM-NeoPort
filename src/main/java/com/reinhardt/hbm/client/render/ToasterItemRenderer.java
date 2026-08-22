package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.ToasterBlock;
import com.reinhardt.hbm.item.ToasterBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class ToasterItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation IRON = MachineModelRenderer.standalone("item/deco_toaster_iron");
    private static final ModelResourceLocation STEEL = MachineModelRenderer.standalone("item/deco_toaster_steel");
    private static final ModelResourceLocation WOOD = MachineModelRenderer.standalone("item/deco_toaster_wood");
    private static final float SCALE = 0.95F / 0.5625F;

    public ToasterItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(IRON);
        event.register(STEEL);
        event.register(WOOD);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int variant = ToasterBlockItem.variant(stack);
        ModelResourceLocation model = variant == 1 ? STEEL : variant == 2 ? WOOD : IRON;
        BlockState state = HbmBlocks.DECO_TOASTER.get().defaultBlockState()
                .setValue(ToasterBlock.VARIANT, variant);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-90.0F), 0.0F, 1.0F, 0.0F)));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(0.0F, -0.15625F, 0.03125F);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
