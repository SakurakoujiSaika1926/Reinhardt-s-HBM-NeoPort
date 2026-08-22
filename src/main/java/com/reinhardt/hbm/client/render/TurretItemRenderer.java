package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class TurretItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation CHEKHOV_BASE = model("turret_chekhov_base");
    private static final ModelResourceLocation CHEKHOV_CARRIAGE = model("turret_chekhov_carriage");
    private static final ModelResourceLocation CHEKHOV_BODY = model("turret_chekhov_body");
    private static final ModelResourceLocation CHEKHOV_BARRELS = model("turret_chekhov_barrels");
    private static final ModelResourceLocation FRIENDLY_BASE = model("turret_friendly_base");
    private static final ModelResourceLocation FRIENDLY_CARRIAGE = model("turret_friendly_carriage");
    private static final ModelResourceLocation JEREMY_GUN = model("turret_jeremy_gun");
    private static final ModelResourceLocation FRITZ_GUN = model("turret_fritz_gun");
    private static final ModelResourceLocation HOWARD_CARRIAGE = model("turret_howard_carriage");
    private static final ModelResourceLocation HOWARD_BODY = model("turret_howard_body");
    private static final ModelResourceLocation HOWARD_BARRELS_TOP = model("turret_howard_barrels_top");
    private static final ModelResourceLocation HOWARD_BARRELS_BOTTOM = model("turret_howard_barrels_bottom");
    private static final ModelResourceLocation HOWARD_DAMAGED_BASE = model("turret_howard_damaged_base");
    private static final ModelResourceLocation HOWARD_DAMAGED_ITEM_CARRIAGE = model("turret_howard_damaged_item_carriage");
    private static final ModelResourceLocation HOWARD_DAMAGED_BODY = model("turret_howard_damaged_body");
    private static final ModelResourceLocation HOWARD_DAMAGED_BARRELS_TOP = model("turret_howard_damaged_barrels_top");
    private static final ModelResourceLocation HOWARD_DAMAGED_BARRELS_BOTTOM = model("turret_howard_damaged_barrels_bottom");
    private static final ModelResourceLocation MAXWELL_MICROWAVE = model("turret_maxwell_microwave");
    private static final ModelResourceLocation RICHARD_LAUNCHER = model("turret_richard_launcher");
    private static final ModelResourceLocation TAUON_CANNON = model("turret_tauon_cannon");
    private static final ModelResourceLocation TAUON_ROTOR = model("turret_tauon_rotor");
    private static final ModelResourceLocation ARTY_BASE = model("turret_arty_base");
    private static final ModelResourceLocation ARTY_CARRIAGE = model("turret_arty_carriage");
    private static final ModelResourceLocation ARTY_CANNON = model("turret_arty_cannon");
    private static final ModelResourceLocation ARTY_BARREL = model("turret_arty_barrel");
    private static final ModelResourceLocation HIMARS_CARRIAGE = model("turret_himars_carriage");
    private static final ModelResourceLocation HIMARS_LAUNCHER = model("turret_himars_launcher");
    private static final ModelResourceLocation HIMARS_CRANE = model("turret_himars_crane");
    private static final ModelResourceLocation HIMARS_TUBE_STANDARD = model("turret_himars_tube_standard");
    private static final ModelResourceLocation SENTRY_BASE = model("turret_sentry_base");
    private static final ModelResourceLocation SENTRY_PIVOT = model("turret_sentry_pivot");
    private static final ModelResourceLocation SENTRY_BODY = model("turret_sentry_body");
    private static final ModelResourceLocation SENTRY_DRUM = model("turret_sentry_drum");
    private static final ModelResourceLocation SENTRY_BARREL_L = model("turret_sentry_barrel_l");
    private static final ModelResourceLocation SENTRY_BARREL_R = model("turret_sentry_barrel_r");
    private static final ModelResourceLocation SENTRY_DAMAGED_BASE = model("turret_sentry_damaged_base");
    private static final ModelResourceLocation SENTRY_DAMAGED_PIVOT = model("turret_sentry_damaged_pivot");
    private static final ModelResourceLocation SENTRY_DAMAGED_BODY = model("turret_sentry_damaged_body");
    private static final ModelResourceLocation SENTRY_DAMAGED_DRUM = model("turret_sentry_damaged_drum");
    private static final ModelResourceLocation SENTRY_DAMAGED_BARREL_L = model("turret_sentry_damaged_barrel_l");
    private static final ModelResourceLocation SENTRY_DAMAGED_BARREL_R = model("turret_sentry_damaged_barrel_r");

    public TurretItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(HOWARD_DAMAGED_ITEM_CARRIAGE);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        poseStack.pushPose();
        boolean inventory = applyLegacyItemContext(context, poseStack);
        if (block == HbmBlocks.TURRET_JEREMY.get()) {
            renderJeremy(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block == HbmBlocks.TURRET_CHEKHOV.get()) {
            renderChekhov(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (block instanceof LegacyTurretBlock turretBlock) {
            renderLegacy(turretBlock.type(), inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static boolean applyLegacyItemContext(ItemDisplayContext context, PoseStack poseStack) {
        if (context != ItemDisplayContext.GUI) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        }
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.0620F, 0.0620F, 0.0620F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                return true;
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.2F, 0.2F, 0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(135.0F));
            }
            case FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.2F, 0.2F, 0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            }
            case THIRD_PERSON_RIGHT_HAND, HEAD -> {
                poseStack.translate(0.0F, 0.25F, 0.0F);
                poseStack.scale(0.1875F, 0.1875F, 0.1875F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 0.25F, 0.0F);
                poseStack.scale(0.1875F, 0.1875F, 0.1875F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case FIXED -> {
                poseStack.translate(0.0F, 0.3F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            default -> {
            }
        }
        return false;
    }

    private static void renderJeremy(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
        }
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(JEREMY_GUN, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderChekhov(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.translate(-0.75F, 0.0F, 0.0F);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BARRELS, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderLegacy(LegacyTurretType type, boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        switch (type) {
            case FRIENDLY -> renderFriendly(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case FRITZ -> renderFritz(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case HOWARD -> renderHoward(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case HOWARD_DAMAGED -> renderHowardDamaged(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case MAXWELL -> renderMaxwell(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case RICHARD -> renderRichard(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case TAUON -> renderTauon(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case ARTY -> renderArty(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case HIMARS -> renderHimars(inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SENTRY -> renderSentry(false, inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SENTRY_DAMAGED -> renderSentry(true, inventory, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderFriendly(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.translate(-0.75F, 0.0F, 0.0F);
        renderPart(FRIENDLY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(FRIENDLY_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BARRELS, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderFritz(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(FRITZ_GUN, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderHoward(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -4.5F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.translate(-0.75F, 0.0F, 0.0F);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_BARRELS_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_BARRELS_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderHowardDamaged(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -4.5F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.translate(-0.75F, 0.0F, 0.0F);
        renderPart(HOWARD_DAMAGED_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_DAMAGED_ITEM_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_DAMAGED_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_DAMAGED_BARRELS_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_DAMAGED_BARRELS_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderMaxwell(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(-1.0F, -3.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HOWARD_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(MAXWELL_MICROWAVE, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderRichard(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(RICHARD_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderTauon(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(TAUON_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(TAUON_ROTOR, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderArty(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(-3.0F, -4.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderPart(ARTY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(ARTY_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 3.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.translate(0.0F, -3.0F, 0.0F);
        renderPart(ARTY_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(ARTY_BARREL, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderHimars(boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderPart(ARTY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HIMARS_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HIMARS_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HIMARS_CRANE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(HIMARS_TUBE_STANDARD, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderSentry(boolean damaged, boolean inventory, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (inventory) {
            poseStack.translate(0.0F, -4.0F, 0.0F);
            poseStack.scale(7.0F, 7.0F, 7.0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderPart(damaged ? SENTRY_DAMAGED_BASE : SENTRY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_PIVOT : SENTRY_PIVOT, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_BODY : SENTRY_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_DRUM : SENTRY_DRUM, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_BARREL_L : SENTRY_BARREL_L, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_BARREL_R : SENTRY_BARREL_R, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderPart(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
