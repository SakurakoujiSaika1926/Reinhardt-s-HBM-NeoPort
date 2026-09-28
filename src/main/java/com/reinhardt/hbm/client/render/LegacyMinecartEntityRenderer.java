package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyMinecartEntity;
import com.reinhardt.hbm.item.LegacyMinecartItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Renders the original cart body and the distinct powder/destroyer top assemblies. */
public final class LegacyMinecartEntityRenderer extends EntityRenderer<LegacyMinecartEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("entity/cart");
    private static final ModelResourceLocation DESTROYER = MachineModelRenderer.standalone("entity/cart_destroyer");
    private static final ModelResourceLocation POWDER = MachineModelRenderer.standalone("entity/cart_powder");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final LegacyEntityObjMesh BODY_BOUNDS = mesh("cart");
    private static final LegacyEntityObjMesh DESTROYER_BOUNDS = mesh("cart_destroyer");
    private static final LegacyEntityObjMesh POWDER_BOUNDS = mesh("cart_powder");
    private static final double BODY_RADIUS = visualRadius(null);
    private static final double DESTROYER_RADIUS = visualRadius(DESTROYER_BOUNDS);
    private static final double POWDER_RADIUS = visualRadius(POWDER_BOUNDS);

    public LegacyMinecartEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // RenderNeoCart never overrides Render's default shadow size.
        shadowRadius = 0.0F;
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

    @Override
    public boolean shouldRender(LegacyMinecartEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = switch (entity.variant()) {
            case DESTROYER -> DESTROYER_RADIUS;
            case POWDER, SEMTEX -> POWDER_RADIUS;
            default -> BODY_RADIUS;
        };
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                frustum, cameraX, cameraY, cameraZ);
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

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(ReinhardtsHBM.id(
                "models/obj/entity/" + name + ".obj"));
    }

    private static double visualRadius(LegacyEntityObjMesh top) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, 0.25D, 0.0D);
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        BODY_BOUNDS.includeAllFacePositions(poseStack, bounds);
        if (top != null) top.includeAllFacePositions(poseStack, bounds);
        // The only remaining transform is arbitrary yaw, which preserves
        // this exact face-vertex radius.
        return bounds.maximumRadius();
    }
}
