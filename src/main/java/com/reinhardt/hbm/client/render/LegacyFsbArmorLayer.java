package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity renderer for the 1.7.10 powered-suit OBJ models.
 *
 * <p>These suits are registered as normal {@link net.minecraft.world.item.ArmorItem}
 * instances so that their inventory icons and equipment behaviour continue to
 * use the modern item system.  Their old entity models, however, are not
 * vanilla layer textures.  This layer keeps the old OBJ geometry and attaches
 * each group to the animated 1.21.1 humanoid part that used to drive the
 * corresponding ModelRendererObj.</p>
 */
public final class LegacyFsbArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>>
        extends RenderLayer<T, M> {
    private static final Map<String, LegacyEntityObjMesh> MESHES = Map.ofEntries(
            Map.entry("ajr", mesh("ajr")),
            Map.entry("bismuth", mesh("bismuth")),
            Map.entry("bj", mesh("bj")),
            Map.entry("bnuuy", mesh("bnuuy")),
            Map.entry("dnt", mesh("dnt")),
            Map.entry("envsuit", mesh("envsuit")),
            Map.entry("fau", mesh("fau")),
            Map.entry("goggles", mesh("goggles")),
            Map.entry("hat", mesh("hat")),
            Map.entry("hev", mesh("hev")),
            Map.entry("ncrpa", mesh("ncrpa")),
            Map.entry("no9", mesh("no9")),
            Map.entry("remnant", mesh("remnant")),
            Map.entry("steamsuit", mesh("steamsuit")),
            Map.entry("taurun", mesh("taurun")),
            Map.entry("trenchmaster", mesh("trenchmaster"))
    );

    private static final List<Part> PARTS = createParts();

    public LegacyFsbArmorLayer(RenderLayerParent<T, M> parent) {
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
        for (Part part : PARTS) {
            ItemStack stack = livingEntity.getItemBySlot(part.slot());
            if (!part.matches(stack)) {
                continue;
            }

            ModelPart anchor = part.anchor().resolve(parentModel);
            poseStack.pushPose();
            anchor.translateAndRotate(poseStack);
            poseStack.translate(-anchor.x / 16.0F, -anchor.y / 16.0F, -anchor.z / 16.0F);
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

            if (part.animatedFan()) {
                poseStack.translate(0.0F, 4.875F, 0.0F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-(livingEntity.tickCount + partialTick) * 9.0F));
                poseStack.translate(0.0F, -4.875F, 0.0F);
            }

            int light = part.emissive() ? LightTexture.FULL_BRIGHT : packedLight;
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(part.texture()));
            MESHES.get(part.mesh()).renderGroup(
                    part.group(),
                    poseStack,
                    consumer,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0xFFFFFFFF
            );
            poseStack.popPose();
        }
    }

    private static LegacyEntityObjMesh mesh(String name) {
        return LegacyEntityObjMesh.load(ReinhardtsHBM.id("models/obj/armor/" + name + ".obj"));
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/armor/" + name + ".png");
    }

    private static List<Part> createParts() {
        List<Part> parts = new ArrayList<>();

        addSuit(parts, "bismuth", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "bismuth", "bismuth", "bismuth", "bismuth",
                HbmItems.BISMUTH_HELMET, HbmItems.BISMUTH_PLATE, HbmItems.BISMUTH_LEGS, HbmItems.BISMUTH_BOOTS);
        addSuit(parts, "steamsuit", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "steamsuit_helmet", "steamsuit_chest", "steamsuit_arm", "steamsuit_leg",
                HbmItems.STEAMSUIT_HELMET, HbmItems.STEAMSUIT_PLATE, HbmItems.STEAMSUIT_LEGS, HbmItems.STEAMSUIT_BOOTS);
        addSuit(parts, "bnuuy", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "bnuuy_helmet", "bnuuy_chest", "bnuuy_arm", "bnuuy_leg",
                HbmItems.DIESELSUIT_HELMET, HbmItems.DIESELSUIT_PLATE, HbmItems.DIESELSUIT_LEGS, HbmItems.DIESELSUIT_BOOTS);
        addSuit(parts, "ajr", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ajr_helmet", "ajr_chest", "ajr_arm", "ajr_leg",
                HbmItems.AJR_HELMET, HbmItems.AJR_PLATE, HbmItems.AJR_LEGS, HbmItems.AJR_BOOTS);
        addSuit(parts, "ajr", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ajro_helmet", "ajro_chest", "ajro_arm", "ajro_leg",
                HbmItems.AJRO_HELMET, HbmItems.AJRO_PLATE, HbmItems.AJRO_LEGS, HbmItems.AJRO_BOOTS);
        addSuit(parts, "remnant", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "rpa_helmet", "rpa_chest", "rpa_arm", "rpa_leg",
                HbmItems.RPA_HELMET, HbmItems.RPA_PLATE, HbmItems.RPA_LEGS, HbmItems.RPA_BOOTS);
        addSuit(parts, "ncrpa", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "ncrpa_helmet", "ncrpa_chest", "ncrpa_arm", "ncrpa_leg",
                HbmItems.NCRPA_HELMET, HbmItems.NCRPA_PLATE, HbmItems.NCRPA_LEGS, HbmItems.NCRPA_BOOTS);
        addSuit(parts, "bj", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "bj_eyepatch", "bj_chest", "bj_arm", "bj_leg",
                HbmItems.BJ_HELMET, HbmItems.BJ_PLATE, HbmItems.BJ_LEGS, HbmItems.BJ_BOOTS);
        addSuit(parts, "envsuit", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "envsuit_helmet", "envsuit_chest", "envsuit_arm", "envsuit_leg",
                HbmItems.ENVSUIT_HELMET, HbmItems.ENVSUIT_PLATE, HbmItems.ENVSUIT_LEGS, HbmItems.ENVSUIT_BOOTS);
        addSuit(parts, "hev", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftFoot", "RightFoot",
                "hev_helmet", "hev_chest", "hev_arm", "hev_leg",
                HbmItems.HEV_HELMET, HbmItems.HEV_PLATE, HbmItems.HEV_LEGS, HbmItems.HEV_BOOTS);
        addSuit(parts, "fau", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "fau_helmet", "fau_chest", "fau_arm", "fau_leg",
                HbmItems.FAU_HELMET, HbmItems.FAU_PLATE, HbmItems.FAU_LEGS, HbmItems.FAU_BOOTS);
        addSuit(parts, "dnt", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "dnt_helmet", "dnt_chest", "dnt_arm", "dnt_leg",
                HbmItems.DNT_HELMET, HbmItems.DNT_PLATE, HbmItems.DNT_LEGS, HbmItems.DNT_BOOTS);
        addSuit(parts, "dnt", "Head", "Body", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "dnt_helmet", "dnt_chest", "dnt_arm", "dnt_leg",
                HbmItems.DNS_HELMET, HbmItems.DNS_PLATE, HbmItems.DNS_LEGS, HbmItems.DNS_BOOTS);
        addSuit(parts, "taurun", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "taurun_helmet", "taurun_chest", "taurun_arm", "taurun_leg",
                HbmItems.TAURUN_HELMET, HbmItems.TAURUN_PLATE, HbmItems.TAURUN_LEGS, HbmItems.TAURUN_BOOTS);
        addSuit(parts, "trenchmaster", "Helmet", "Chest", "LeftArm", "RightArm",
                "LeftLeg", "RightLeg", "LeftBoot", "RightBoot",
                "trenchmaster_helmet", "trenchmaster_chest", "trenchmaster_arm", "trenchmaster_leg",
                HbmItems.TRENCHMASTER_HELMET, HbmItems.TRENCHMASTER_PLATE, HbmItems.TRENCHMASTER_LEGS, HbmItems.TRENCHMASTER_BOOTS);

        addPart(parts, "bj", EquipmentSlot.CHEST, Anchor.BODY, "Jetpack", "bj_jetpack", false,
                HbmItems.BJ_PLATE_JETPACK);
        addPart(parts, "ncrpa", EquipmentSlot.HEAD, Anchor.HEAD, "Eyes", "ncrpa_helmet", true,
                HbmItems.NCRPA_HELMET);
        addPart(parts, "envsuit", EquipmentSlot.HEAD, Anchor.HEAD, "Lamps", "envsuit_helmet", true,
                HbmItems.ENVSUIT_HELMET);
        addPart(parts, "remnant", EquipmentSlot.CHEST, Anchor.BODY, "Glow", "rpa_chest", true,
                HbmItems.RPA_PLATE);
        addPart(parts, "remnant", EquipmentSlot.CHEST, Anchor.BODY, "Fan", "rpa_chest", false,
                HbmItems.RPA_PLATE);
        addPart(parts, "fau", EquipmentSlot.CHEST, Anchor.BODY, "Cassette", "fau_cassette", false,
                HbmItems.FAU_PLATE);
        addPart(parts, "trenchmaster", EquipmentSlot.HEAD, Anchor.HEAD, "Light", "trenchmaster_helmet", true,
                HbmItems.TRENCHMASTER_HELMET);

        addPart(parts, "goggles", EquipmentSlot.HEAD, Anchor.HEAD, "Cube", "goggles", false,
                HbmItems.GOGGLES, HbmItems.ASHGLASSES);
        addPart(parts, "hat", EquipmentSlot.HEAD, Anchor.HEAD, "Cube_Cube.001", "hat", false,
                HbmItems.HAT);
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Helmet", "no9", false,
                HbmItems.NO9);
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Insignia", "no9_insignia", false,
                HbmItems.NO9);
        addPart(parts, "no9", EquipmentSlot.HEAD, Anchor.HEAD, "Flame", "no9", true,
                HbmItems.NO9);

        return List.copyOf(parts);
    }

    private static void addSuit(
            List<Part> parts,
            String mesh,
            String headGroup,
            String bodyGroup,
            String leftArmGroup,
            String rightArmGroup,
            String leftLegGroup,
            String rightLegGroup,
            String leftBootGroup,
            String rightBootGroup,
            String helmetTexture,
            String chestTexture,
            String armTexture,
            String legTexture,
            ItemLike helmet,
            ItemLike chest,
            ItemLike legs,
            ItemLike boots
    ) {
        addPart(parts, mesh, EquipmentSlot.HEAD, Anchor.HEAD, headGroup, helmetTexture, false, helmet);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.BODY, bodyGroup, chestTexture, false, chest);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.LEFT_ARM, leftArmGroup, armTexture, false, chest);
        addPart(parts, mesh, EquipmentSlot.CHEST, Anchor.RIGHT_ARM, rightArmGroup, armTexture, false, chest);
        addPart(parts, mesh, EquipmentSlot.LEGS, Anchor.LEFT_LEG, leftLegGroup, legTexture, false, legs);
        addPart(parts, mesh, EquipmentSlot.LEGS, Anchor.RIGHT_LEG, rightLegGroup, legTexture, false, legs);
        addPart(parts, mesh, EquipmentSlot.FEET, Anchor.LEFT_LEG, leftBootGroup, legTexture, false, boots);
        addPart(parts, mesh, EquipmentSlot.FEET, Anchor.RIGHT_LEG, rightBootGroup, legTexture, false, boots);
    }

    private static void addPart(
            List<Part> parts,
            String mesh,
            EquipmentSlot slot,
            Anchor anchor,
            String group,
            String texture,
            boolean emissive,
            ItemLike... items
    ) {
        parts.add(new Part(mesh, slot, anchor, group, texture(texture), emissive, "Fan".equals(group), items));
    }

    private record Part(
            String mesh,
            EquipmentSlot slot,
            Anchor anchor,
            String group,
            ResourceLocation texture,
            boolean emissive,
            boolean animatedFan,
            ItemLike[] items
    ) {
        private boolean matches(ItemStack stack) {
            for (ItemLike item : items) {
                if (stack.is(item.asItem())) {
                    return true;
                }
            }
            return false;
        }
    }

    private enum Anchor {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG;

        private ModelPart resolve(HumanoidModel<?> model) {
            return switch (this) {
                case HEAD -> model.head;
                case BODY -> model.body;
                case LEFT_ARM -> model.leftArm;
                case RIGHT_ARM -> model.rightArm;
                case LEFT_LEG -> model.leftLeg;
                case RIGHT_LEG -> model.rightLeg;
            };
        }
    }
}
