package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyMinecartEntity;
import com.reinhardt.hbm.item.LegacyMinecartItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Renders the original cart body and the distinct powder/destroyer top assemblies. */
public final class LegacyMinecartEntityRenderer extends EntityRenderer<LegacyMinecartEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("entity/cart");
    private static final ModelResourceLocation DESTROYER = MachineModelRenderer.standalone("entity/cart_destroyer");
    private static final ModelResourceLocation POWDER = MachineModelRenderer.standalone("entity/cart_powder");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyMinecartEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.7F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(DESTROYER);
        event.register(POWDER);
    }

    @Override
    public void render(LegacyMinecartEntity cart, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0F - entityYaw), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(BODY), poseStack, bufferSource, RENDER_STATE,
                packedLight, 0, bodyTexture(cart.base()), 0.0F, 0.0F);
        if (cart.variant() == LegacyMinecartItem.Type.DESTROYER) {
            MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(DESTROYER), poseStack, bufferSource, RENDER_STATE,
                    packedLight, 0, ReinhardtsHBM.id("textures/entity/cart_destroyer.png"), 0.0F, 0.0F);
        } else if (cart.variant() == LegacyMinecartItem.Type.POWDER || cart.variant() == LegacyMinecartItem.Type.SEMTEX) {
            MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(POWDER), poseStack, bufferSource, RENDER_STATE,
                    packedLight, 0, powderTexture(cart.variant()), 0.0F, 0.0F);
        }
        poseStack.popPose();
        super.render(cart, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static ResourceLocation bodyTexture(LegacyMinecartItem.Base base) {
        return switch (base) {
            case VANILLA -> ReinhardtsHBM.id("textures/entity/cart.png");
            case WOOD -> ReinhardtsHBM.id("textures/entity/cart_wood.png");
            case STEEL -> ReinhardtsHBM.id("textures/entity/cart_metal.png");
            case PAINTED -> ReinhardtsHBM.id("textures/entity/cart_metal_naked.png");
        };
    }

    private static ResourceLocation powderTexture(LegacyMinecartItem.Type type) {
        return type == LegacyMinecartItem.Type.SEMTEX
                ? ReinhardtsHBM.id("textures/block/semtex_side.png")
                : ReinhardtsHBM.id("textures/block/block_gunpowder.png");
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyMinecartEntity entity) {
        return bodyTexture(entity.base());
    }
}
