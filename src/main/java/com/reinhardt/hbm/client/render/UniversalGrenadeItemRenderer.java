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
        applyLegacyTransform(UniversalGrenadeItem.shell(stack), context, poseStack);
        renderGrenade(stack, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLegacyTransform(UniversalGrenadeItem.Shell shell,
                                             ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                // Exact ItemRenderGrenade#INVENTORY matrix. The old GUI used
                // a 16-pixel coordinate space, hence the 1/16 conversion.
                poseStack.translate(0.5F, 0.5F, 0.0F);
                poseStack.scale(-1.0F / 16.0F, -1.0F / 16.0F, -1.0F / 16.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                applyShellTransform(shell, LegacyView.INVENTORY, poseStack);
            }
            case GROUND -> {
                poseStack.scale(0.125F, 0.125F, 0.125F);
                applyShellTransform(shell, LegacyView.ENTITY, poseStack);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderGrenade#EQUIPPED: scale first, then translate in
                // authored OBJ units. PoseStack follows the same matrix order.
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.translate(3.0F, 1.0F, -0.5F);
                applyShellTransform(shell, LegacyView.EQUIPPED, poseStack);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // Static equivalent of renderFirstPerson before its optional
                // BODY/RING animation channels.
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.translate(3.0F, 1.0F, -3.0F);
                poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
            }
            case FIXED -> {
                poseStack.scale(0.125F, 0.125F, 0.125F);
                applyShellTransform(shell, LegacyView.ENTITY, poseStack);
            }
            case NONE, HEAD -> {
                poseStack.scale(0.125F, 0.125F, 0.125F);
                applyShellTransform(shell, LegacyView.ENTITY, poseStack);
            }
        }
    }

    /** Shell-specific transforms from ItemRenderGrenade#renderGrenade. */
    private static void applyShellTransform(UniversalGrenadeItem.Shell shell,
                                            LegacyView view, PoseStack poseStack) {
        switch (view) {
            case INVENTORY -> {
                switch (shell) {
                    case FRAG -> {
                        poseStack.scale(3.0F, 3.0F, 3.0F);
                        poseStack.translate(0.0F, -2.0F, 0.0F);
                    }
                    case STICK -> {
                        poseStack.scale(2.0F, 2.0F, 2.0F);
                        poseStack.translate(0.0F, -4.5F, 0.0F);
                    }
                    case TECH -> {
                        poseStack.scale(3.5F, 3.5F, 3.5F);
                        poseStack.translate(0.0F, -1.75F, 0.0F);
                    }
                    case NUKE -> {
                        poseStack.scale(2.5F, 2.5F, 2.5F);
                        poseStack.translate(0.0F, -2.75F, 0.0F);
                    }
                }
            }
            case EQUIPPED -> {
                switch (shell) {
                    case FRAG -> {
                    }
                    case STICK -> poseStack.translate(0.0F, -2.0F, 0.0F);
                    case TECH -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.5F, -1.0F, 0.5F);
                    }
                    case NUKE -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.5F, -3.0F, 0.5F);
                    }
                }
            }
            case ENTITY -> {
                switch (shell) {
                    case FRAG, STICK -> poseStack.translate(0.0F, -2.0F, 0.0F);
                    case TECH -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.0F, -1.0F, 0.0F);
                    }
                    case NUKE -> {
                        poseStack.scale(1.5F, 1.5F, 1.5F);
                        poseStack.translate(0.0F, -3.0F, 0.0F);
                    }
                }
            }
        }
    }

    private enum LegacyView {
        INVENTORY,
        EQUIPPED,
        ENTITY
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
