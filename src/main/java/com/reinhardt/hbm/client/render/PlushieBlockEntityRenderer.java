package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PlushieBlock;
import com.reinhardt.hbm.block.PlushieType;
import com.reinhardt.hbm.blockentity.PlushieBlockEntity;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlushieBlockEntityRenderer implements BlockEntityRenderer<PlushieBlockEntity> {
    private static final ModelResourceLocation YOMI = model("yomi", "Plane");
    private static final ModelResourceLocation HUNDUN = model("hundun", "goober_posed");
    private static final ModelResourceLocation DERG = model("derg", "Derg");
    private static final ModelResourceLocation DERG_BLEP = model("derg_blep", "Blep");
    private static final ModelResourceLocation DERG_FACE = model("derg_colonthree", "ColonThree");
    private static final Map<String, ModelResourceLocation> HORSE = horseModels();
    private static final ModelResourceLocation NO9_HELMET = no9Model("Helmet");
    private static final ModelResourceLocation NO9_INSIGNIA = no9Model("Insignia");
    private static final ResourceLocation YOMI_TEXTURE = texture("yomi.png");
    private static final ResourceLocation HUNDUN_TEXTURE = texture("hundun.png");
    private static final ResourceLocation DERG_TEXTURE = texture("derg.png");
    private static final ResourceLocation NUMBER_NINE_TEXTURE = ReinhardtsHBM.id("textures/models/horse/numbernine.png");
    private static final ResourceLocation NO9_TEXTURE = ReinhardtsHBM.id("textures/armor/no9.png");
    private static final ResourceLocation NO9_INSIGNIA_TEXTURE = ReinhardtsHBM.id("textures/armor/no9_insignia.png");

    public PlushieBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(YOMI);
        event.register(HUNDUN);
        event.register(DERG);
        event.register(DERG_BLEP);
        event.register(DERG_FACE);
        HORSE.values().forEach(event::register);
        event.register(NO9_HELMET);
        event.register(NO9_INSIGNIA);
    }

    @Override
    public void render(PlushieBlockEntity plushie, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = plushie.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F * state.getValue(PlushieBlock.ROTATION) + 90.0F));
        if (plushie.squishTimer(partialTick) > 0.0F) {
            float squish = plushie.squishTimer(partialTick);
            poseStack.scale(1.0F, 1.0F + (-(float) Math.sin(squish) * squish) * 0.025F, 1.0F);
        }
        applyWorldScale(plushie.type(), poseStack);
        renderPlushie(plushie.type(), plushie.squishTimer(partialTick) > 0.0F,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static void renderItem(PlushieType type, BlockState state, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderPlushie(type, false, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderPlushie(PlushieType type, boolean squish, BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (type) {
            case YOMI -> render(YOMI, YOMI_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
            case NUMBER_NINE -> renderNumberNine(state, poseStack, bufferSource, packedLight, packedOverlay);
            case HUNDUN -> render(HUNDUN, HUNDUN_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
            case DERG -> {
                render(DERG, DERG_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
                render(squish ? DERG_BLEP : DERG_FACE, DERG_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
            }
            case NONE -> {
            }
        }
    }

    private static void renderNumberNine(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        poseStack.translate(0.0F, -0.25F, 0.75F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        horsePart("body", NUMBER_NINE_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("tail", 0.0F, 1.125F, -0.4375F, 0.0F, 60.0F, 90.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("left_back_leg", 0.125F, 0.75F, -0.25F, 0.0F, -30.0F, 35.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("right_back_leg", -0.125F, 0.75F, -0.25F, 0.0F, -30.0F, -35.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("left_front_leg", 0.125F, 0.75F, 0.3125F, 0.0F, 20.0F, 5.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("right_front_leg", -0.125F, 0.75F, 0.3125F, 0.0F, 20.0F, -5.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        horsePivot("head", 0.0F, 1.125F, 0.375F, 0.0F, 60.0F, 0.0F, NUMBER_NINE_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, -0.6875F);
        poseStack.scale(0.0703125F, 0.0703125F, 0.0703125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        render(NO9_HELMET, NO9_TEXTURE, state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        render(NO9_INSIGNIA, NO9_INSIGNIA_TEXTURE, state, poseStack, bufferSource, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.06F, 1.13F, -0.42F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(60.0F));
        Item cigarette = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("cigarette"));
        if (cigarette != Items.AIR) {
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(cigarette), ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT, packedOverlay, poseStack, bufferSource, null, 0);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void horsePivot(String part, float x, float y, float z, float yaw, float pitch, float roll,
                                   ResourceLocation texture, BlockState state, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-x, -y, -z);
        horsePart(part, texture, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void horsePart(String part, ResourceLocation texture, BlockState state, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        render(HORSE.get(part), texture, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void render(ModelResourceLocation model, ResourceLocation texture, BlockState state,
                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel baked = MachineModelRenderer.model(model);
        MachineModelRenderer.renderUnculledUv(baked, poseStack, bufferSource, state, packedLight, packedOverlay,
                texture, 0.0F, 0.0F);
    }

    private static void applyWorldScale(PlushieType type, PoseStack poseStack) {
        switch (type) {
            case YOMI -> poseStack.scale(0.5F, 0.5F, 0.5F);
            case NUMBER_NINE -> poseStack.scale(0.75F, 0.75F, 0.75F);
            case HUNDUN, DERG, NONE -> {
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(PlushieBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    private static Map<String, ModelResourceLocation> horseModels() {
        Map<String, ModelResourceLocation> models = new LinkedHashMap<>();
        models.put("body", model("horse_body", "Body"));
        models.put("head", model("horse_head", "Head", "Mane", "NoseFemale"));
        models.put("left_front_leg", model("horse_left_front_leg", "LeftFrontLeg"));
        models.put("right_front_leg", model("horse_right_front_leg", "RightFrontLeg"));
        models.put("left_back_leg", model("horse_left_back_leg", "LeftBackLeg"));
        models.put("right_back_leg", model("horse_right_back_leg", "RightBackLeg"));
        models.put("tail", model("horse_tail", "Tail"));
        return Map.copyOf(models);
    }

    private static ModelResourceLocation model(String name, String... groups) {
        return MachineModelRenderer.standalone("block/trinkets/" + name);
    }

    private static ModelResourceLocation no9Model(String group) {
        return MachineModelRenderer.standalone("block/trinkets/no9_" + group.toLowerCase());
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/models/trinkets/" + name);
    }
}
