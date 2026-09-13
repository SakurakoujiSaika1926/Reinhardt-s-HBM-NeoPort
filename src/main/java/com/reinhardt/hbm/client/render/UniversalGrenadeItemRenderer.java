package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.item.UniversalGrenadeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Port of 1.7.10's ItemRenderGrenade.
 *
 * <p>The legacy renderer used one OBJ with ten named groups and rendered the
 * shell, filling, label and fuze as successive texture/tint passes.  Keeping
 * those passes separate is important: all 475 completed grenade combinations
 * share the same geometry, but the filling and fuze colours are part of the
 * old visual identity.</p>
 */
public final class UniversalGrenadeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation FRAG = model("grenade_frag");
    private static final ModelResourceLocation FRAG_BODY = model("grenade_frag_body");
    private static final ModelResourceLocation FRAG_LABEL = model("grenade_frag_label");
    private static final ModelResourceLocation FRAG_FUZE = model("grenade_frag_fuze");
    private static final ModelResourceLocation FRAG_SPOON = model("grenade_frag_spoon");
    private static final ModelResourceLocation FRAG_RING = model("grenade_frag_ring");

    private static final ModelResourceLocation STICK = model("grenade_stick");
    private static final ModelResourceLocation STICK_BODY = model("grenade_stick_body");
    private static final ModelResourceLocation STICK_CAP_BODY = model("grenade_stick_cap_body");
    private static final ModelResourceLocation STICK_LABEL = model("grenade_stick_label");
    private static final ModelResourceLocation STICK_FUZE = model("grenade_stick_fuze");

    private static final ModelResourceLocation TECH = model("grenade_tech");
    private static final ModelResourceLocation TECH_BODY = model("grenade_tech_body");
    private static final ModelResourceLocation TECH_LIGHTS = model("grenade_tech_lights");
    private static final ModelResourceLocation TECH_FUZE = model("grenade_tech_fuze");
    private static final ModelResourceLocation TECH_RING = model("grenade_tech_ring");

    private static final ModelResourceLocation NUKA = model("grenade_nuka");
    private static final ModelResourceLocation NUKA_BODY = model("grenade_nuka_body");
    private static final ModelResourceLocation NUKA_LABEL = model("grenade_nuka_label");
    private static final ModelResourceLocation NUKA_FUZE = model("grenade_nuka_fuze");
    private static final ModelResourceLocation NUKA_SPOON = model("grenade_nuka_spoon");
    private static final ModelResourceLocation NUKA_RING = model("grenade_nuka_ring");

    public UniversalGrenadeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("weapons/" + name);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAG);
        event.register(FRAG_BODY);
        event.register(FRAG_LABEL);
        event.register(FRAG_FUZE);
        event.register(FRAG_SPOON);
        event.register(FRAG_RING);
        event.register(STICK);
        event.register(STICK_BODY);
        event.register(STICK_CAP_BODY);
        event.register(STICK_LABEL);
        event.register(STICK_FUZE);
        event.register(TECH);
        event.register(TECH_BODY);
        event.register(TECH_LIGHTS);
        event.register(TECH_FUZE);
        event.register(TECH_RING);
        event.register(NUKA);
        event.register(NUKA_BODY);
        event.register(NUKA_LABEL);
        event.register(NUKA_FUZE);
        event.register(NUKA_SPOON);
        event.register(NUKA_RING);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyLegacyTransform(context, poseStack);
        renderGrenade(stack, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLegacyTransform(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                // ItemRenderGrenade#INVENTORY: 45° Z, 150° Y, 15° X.
                poseStack.translate(0.5F, 0.58F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                poseStack.scale(0.105F, 0.105F, 0.105F);
                poseStack.translate(0.0F, -5.0F, 0.0F);
            }
            case GROUND -> {
                // ItemRenderGrenade#ENTITY: authored OBJ units were scaled by
                // 0.125 in the legacy renderer.
                poseStack.translate(0.0F, -0.58F, 0.0F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderGrenade#EQUIPPED.
                poseStack.translate(0.19F, -0.30F, -0.03F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ItemRenderGrenade#EQUIPPED_FIRST_PERSON's fixed scale and
                // orientation. HbmAnimations is not present in 1.21.1, so
                // the static pose is retained while the mesh/tint passes stay
                // identical to the old renderer.
                poseStack.translate(0.25F, -0.25F, -0.30F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
            case FIXED -> {
                poseStack.translate(0.0F, -0.58F, 0.0F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
            case NONE, HEAD -> {
                poseStack.translate(0.0F, -0.58F, 0.0F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
            }
        }
    }

    private static void renderGrenade(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        UniversalGrenadeItem.Shell shell = UniversalGrenadeItem.shell(stack);
        UniversalGrenadeItem.Filling filling = UniversalGrenadeItem.filling(stack);
        UniversalGrenadeItem.Fuze fuze = UniversalGrenadeItem.fuze(stack);
        var state = Blocks.IRON_BLOCK.defaultBlockState();

        switch (shell) {
            case FRAG -> {
                render(FRAG, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(FRAG_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                render(FRAG_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                render(FRAG_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case STICK -> {
                render(STICK, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_CAP_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(STICK_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                // The old renderer used the body overlay for the removable
                // cap, so this is deliberately not a separate base texture.
                renderTinted(STICK_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case TECH -> {
                render(TECH, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(TECH_LIGHTS, filling.labelColor(), poseStack, bufferSource, state,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay);
                render(TECH_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case NUKE -> {
                render(NUKA, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_BODY, filling.bodyColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_LABEL, filling.labelColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTinted(NUKA_FUZE, fuze.bandColor(), poseStack, bufferSource, state, packedLight, packedOverlay);
                render(NUKA_SPOON, poseStack, bufferSource, state, packedLight, packedOverlay);
                render(NUKA_RING, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
    }

    private static void render(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource,
                                net.minecraft.world.level.block.state.BlockState state, int packedLight, int packedOverlay) {
        BakedModel model = MachineModelRenderer.model(location);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderTinted(ModelResourceLocation location, int color, PoseStack poseStack,
                                     MultiBufferSource bufferSource,
                                     net.minecraft.world.level.block.state.BlockState state,
                                     int packedLight, int packedOverlay) {
        BakedModel model = MachineModelRenderer.model(location);
        MachineModelRenderer.renderUnculledTinted(model, poseStack, bufferSource, state, packedLight, packedOverlay,
                0xFF000000 | color);
    }
}
