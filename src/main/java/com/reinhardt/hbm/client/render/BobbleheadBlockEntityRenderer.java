package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.blockentity.BobbleheadBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

import java.util.LinkedHashMap;
import java.util.Map;

import com.reinhardt.hbm.item.LegacyVariantItem;

/** Direct OBJ-group port of RenderBobble, including its old pose and head bobbing transforms. */
public final class BobbleheadBlockEntityRenderer implements BlockEntityRenderer<BobbleheadBlockEntity> {
    private static final String[] GROUPS = {
            "Body", "Body17", "Drillgon", "Fumo", "FumoHead", "Head", "Head17", "Horn",
            "LA", "LA17", "LL", "LL17", "PeepHat", "PeepTail", "Pellet", "PelletShine",
            "RA", "RA17", "RL", "RL17", "Socket", "Fluoro", "Glow"
    };
    private static final Map<String, ModelResourceLocation> MODELS = models();
    private static final Map<String, ModelResourceLocation> ACCESSORY_MODELS = accessoryModels();
    private static final ResourceLocation SOCKET = ReinhardtsHBM.id("textures/models/trinkets/socket.png");
    private static final ResourceLocation GLOW = ReinhardtsHBM.id("textures/models/trinkets/glow.png");
    private static final ResourceLocation MELLOW_LAMP = ReinhardtsHBM.id("textures/models/trinkets/accessory/fluorescent_lamp.png");
    private static final ResourceLocation HEV_HELMET = ReinhardtsHBM.id("textures/models/trinkets/accessory/hev_helmet.png");
    private static final ResourceLocation HAT = ReinhardtsHBM.id("textures/models/trinkets/accessory/hat.png");
    private static final ResourceLocation REVOLVER = ReinhardtsHBM.id("textures/models/trinkets/accessory/revolver.png");
    private static final ResourceLocation SHIMMER_AXE = ReinhardtsHBM.id("textures/models/trinkets/accessory/shimmer_axe.png");
    private static final ResourceLocation MINI_NUKE = ReinhardtsHBM.id("textures/models/trinkets/accessory/fatman_mininuke.png");
    private static final ResourceLocation SACRED_DRAGON = ReinhardtsHBM.id("textures/models/trinkets/accessory/sacred_dragon.png");

    public BobbleheadBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.values().forEach(event::register);
        ACCESSORY_MODELS.values().forEach(event::register);
    }

    @Override
    public void render(BobbleheadBlockEntity bobble, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bobble.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(BobbleheadBlock.ROTATION) + 90.0F));
        renderBobble(bobble.type(), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderItem(BobbleheadType type, BlockState state, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderBobble(type, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderBobble(BobbleheadType type, BlockState state, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (type) {
            case PU238 -> renderPellet(type, state, poseStack, bufferSource, packedOverlay);
            case UFFR -> renderFumo(type, state, poseStack, bufferSource, packedLight, packedOverlay);
            case DRILLGON -> renderPart("Drillgon", type.texture(), state, poseStack, bufferSource, packedLight, packedOverlay);
            default -> renderGuy(type, type.texture(), state, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (type == BobbleheadType.MELLOW || type == BobbleheadType.ABEL) {
            renderGuy(type, type.glowTexture(), state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
            if (type == BobbleheadType.MELLOW) {
                renderGlowPart("Fluoro", MELLOW_LAMP, state, poseStack, bufferSource, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                renderGlowPart("Glow", GLOW, state, poseStack, bufferSource, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        renderPost(type, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart("Socket", SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderSocketLabel(type, poseStack, bufferSource, packedLight);
    }

    private static void renderGuy(BobbleheadType type, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Pose pose = Pose.forType(type);
        String suffix = type.skinLayers() ? "" : "17";

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.bodyY));
        if (type == BobbleheadType.PEEP) renderPart("PeepTail", texture, state, poseStack, bufferSource, packedLight, packedOverlay);

        renderPivoted("LL" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay,
                0.0F, 1.0F, -0.125F, pose.leftLeg);
        renderPivoted("RL" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay,
                0.0F, 1.0F, 0.125F, pose.rightLeg);
        renderPivoted("LA" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay,
                0.0F, 1.625F, -0.25F, pose.leftArm);
        renderPivoted("RA" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay,
                0.0F, 1.625F, 0.25F, pose.rightArm);
        renderPart("Body" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay);

        float time = System.currentTimeMillis() * 0.005F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(time)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time + Math.PI * 0.5D)));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.head.x));
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.head.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.head.z));
        poseStack.translate(0.0F, -1.75F, 0.0F);
        renderPart("Head" + suffix, texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        if (type == BobbleheadType.VT) renderPart("Horn", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        if (type == BobbleheadType.PEEP) renderPart("PeepHat", texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        if (type == BobbleheadType.VAER) {
            poseStack.pushPose();
            poseStack.translate(0.25F, 1.9F, 0.075F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-60.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            renderLegacyItem("cigarette", poseStack, bufferSource, packedOverlay);
            poseStack.popPose();
        }
        if (type == BobbleheadType.NOS) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.75F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(0.095F, 0.095F, 0.095F);
            renderAccessory("hat", HAT, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderPivoted(String group, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                      float x, float y, float z, Rotation3 rotation) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z));
        poseStack.translate(-x, -y, -z);
        renderPart(group, texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPellet(BobbleheadType type, BlockState state, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedOverlay) {
        renderPart("Pellet", type.texture(), state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        float alpha = 0.1F + (float) Math.sin(System.currentTimeMillis() * 0.001D) * 0.05F;
        MachineModelRenderer.renderUnculledTintedLightning(MachineModelRenderer.model(MODELS.get("PelletShine")), poseStack,
                bufferSource, state, packedOverlay, (Math.round(alpha * 255.0F) << 24) | 0x00FFFF00);
    }

    private static void renderFumo(BobbleheadType type, BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderPart("Fumo", type.texture(), state, poseStack, bufferSource, packedLight, packedOverlay);
        float time = System.currentTimeMillis() * 0.005F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.75F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(time)));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time + Math.PI * 0.5D)));
        poseStack.translate(0.0F, -0.75F, 0.0F);
        renderPart("FumoHead", type.texture(), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPart(String group, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel model = MachineModelRenderer.model(MODELS.get(group));
        MachineModelRenderer.renderUnculledUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, texture, 0.0F, 0.0F);
    }

    private static void renderGlowPart(String group, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedOverlay,
                                        float red, float green, float blue, float alpha) {
        MachineModelRenderer.renderUnculledUvEyes(MachineModelRenderer.model(MODELS.get(group)), poseStack, bufferSource,
                state, packedOverlay, texture, red, green, blue, alpha);
    }

    private static void renderAccessory(String name, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(ACCESSORY_MODELS.get(name)), poseStack,
                bufferSource, state, packedLight, packedOverlay, texture, 0.0F, 0.0F);
    }

    private static void renderPost(BobbleheadType type, BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (type) {
            case BLUEHAT -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.875F, -0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-160.0F));
                poseStack.scale(0.0625F, 0.0625F, 0.0625F);
                renderAccessory("hev", HEV_HELMET, state, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
            case FRIZZLE -> {
                poseStack.pushPose();
                poseStack.translate(0.8F, 1.6F, 0.4F);
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
                renderAccessory("revolver", REVOLVER, state, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(0.3F, 1.4F, -0.2F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
                renderLegacyVariantItem("weapon_mod_special", "doubloons", poseStack, bufferSource, packedOverlay);
                poseStack.popPose();
            }
            case ADAM29 -> {
                poseStack.pushPose();
                poseStack.translate(0.4F, 1.15F, 0.4F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                renderLegacyItem("can_redbomb", poseStack, bufferSource, packedOverlay);
                poseStack.popPose();
            }
            case PHEO -> {
                poseStack.pushPose();
                poseStack.translate(0.5F, 1.15F, 0.45F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F));
                poseStack.scale(2.0F, 2.0F, 2.0F);
                renderAccessory("shimmer_axe", SHIMMER_AXE, state, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
            case BOB -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.6875F, 0.625F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(0.125F, 0.125F, 0.125F);
                poseStack.translate(-6.0F, 0.0F, 0.0F);
                for (int i = -1; i <= 1; i++) {
                    poseStack.translate(3.0F, 0.0F, 0.0F);
                    renderAccessory("fatman", MINI_NUKE, state, poseStack, bufferSource, packedLight, packedOverlay);
                }
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(0.25F, 0.3125F, -0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.scale(0.1F, 0.1F, 0.1F);
                renderAccessory("sacred_dragon", SACRED_DRAGON, state, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
            default -> {
            }
        }
    }

    private static void renderLegacyItem(String id, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
        }
    }

    private static void renderLegacyVariantItem(String id, String variant, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedOverlay) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item != Items.AIR) {
            ItemStack stack = LegacyVariantItem.stackFor(item, variant);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
        }
    }

    private static void renderSocketLabel(BobbleheadType type, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Font font = Minecraft.getInstance().font;
        String label = type.label();
        poseStack.pushPose();
        poseStack.translate(0.63F, 0.175F, 0.0F);
        poseStack.scale(0.01F, -0.01F, 0.01F);
        poseStack.translate(0.0F, 0.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(Component.literal(label), 0.0F, 0.0F, type == BobbleheadType.VT ? 0xFFFF0000 : 0xFFFFFFFF,
                true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BobbleheadBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    private static Map<String, ModelResourceLocation> models() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        for (String group : GROUPS) {
            models.put(group, MachineModelRenderer.standalone("block/bobble_" + group.toLowerCase()));
        }
        return Map.copyOf(models);
    }

    private static Map<String, ModelResourceLocation> accessoryModels() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        for (String name : new String[]{"hev", "hat", "revolver", "shimmer_axe", "fatman", "sacred_dragon"}) {
            models.put(name, MachineModelRenderer.standalone("block/bobble_accessory_" + name));
        }
        return Map.copyOf(models);
    }

    private record Rotation3(float x, float y, float z) {
        private static final Rotation3 ZERO = new Rotation3(0.0F, 0.0F, 0.0F);
    }

    private record Pose(float bodyY, Rotation3 leftArm, Rotation3 rightArm, Rotation3 leftLeg,
                        Rotation3 rightLeg, Rotation3 head) {
        private static Pose forType(BobbleheadType type) {
            Rotation3 leftArm = Rotation3.ZERO;
            Rotation3 rightArm = Rotation3.ZERO;
            Rotation3 leftLeg = Rotation3.ZERO;
            Rotation3 rightLeg = Rotation3.ZERO;
            Rotation3 head = Rotation3.ZERO;
            float bodyY = 0.0F;
            switch (type) {
                case STRENGTH -> { leftArm = r(0, 25, 135); rightArm = r(0, -45, 135); leftLeg = r(0, 0, -5); rightLeg = r(0, 0, 5); head = r(15, 0, 0); }
                case PERCEPTION -> { leftArm = r(0, -15, 135); rightArm = r(-5, 0, 0); }
                case ENDURANCE -> { bodyY = 45; leftArm = r(0, -25, 30); rightArm = r(0, 45, 30); head = r(0, -45, 0); }
                case CHARISMA -> { bodyY = 45; rightArm = r(0, -45, 90); leftLeg = r(0, 0, -5); rightLeg = r(0, 0, 5); head = r(-5, -45, 0); }
                case INTELLIGENCE -> { head = r(0, 30, 0); leftArm = r(5, 0, 0); rightArm = r(15, 0, 170); }
                case AGILITY -> { leftArm = r(0, 0, 60); rightArm = r(0, 0, -45); leftLeg = r(0, 0, -15); rightLeg = r(0, 0, 45); }
                case LUCK -> { leftArm = r(135, 45, 0); rightArm = r(-135, -45, 0); rightLeg = r(-5, 0, 0); }
                case VT -> { leftArm = r(0, -45, 60); rightArm = r(0, 0, 45); leftLeg = r(2, 0, 0); rightLeg = r(-2, 0, 0); }
                case BLUEHAT -> leftArm = r(0, 90, 60);
                case FRIZZLE -> { leftArm = r(0, 15, 45); rightArm = r(0, 0, 80); leftLeg = r(0, 0, 2); rightLeg = r(0, 0, -2); }
                case ADAM29 -> rightArm = r(0, 0, 60);
                case PHEO -> { leftArm = r(0, 0, 80); rightArm = r(0, 0, 45); }
                case VAER -> { leftArm = r(0, -5, 45); rightArm = r(0, 15, 45); }
                case PEEP -> { leftArm = r(0, 0, 1); rightArm = r(0, 0, 1); }
                case MELLOW -> { leftArm = r(0, 10, 0); rightArm = r(0, -10, 0); leftLeg = r(3, 5, 2); rightLeg = r(-3, -5, 0); }
                case ABEL -> { leftArm = r(0, 80, 90); rightArm = r(0, -80, 90); }
                default -> { }
            }
            return new Pose(bodyY, leftArm, rightArm, leftLeg, rightLeg, head);
        }

        private static Rotation3 r(float x, float y, float z) { return new Rotation3(x, y, z); }
    }
}
