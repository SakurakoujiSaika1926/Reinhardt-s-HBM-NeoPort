package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LandmineBlock;
import com.reinhardt.hbm.blockentity.LandmineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class LandmineBlockEntityRenderer implements BlockEntityRenderer<LandmineBlockEntity> {
    private static final ModelResourceLocation AP_GRASS = model("block/mine_ap_grass_world");
    private static final ModelResourceLocation AP_DESERT = model("block/mine_ap_desert_world");
    private static final ModelResourceLocation AP_SNOW = model("block/mine_ap_snow_world");
    private static final ModelResourceLocation AP_STONE = model("block/mine_ap_stone_world");
    private static final ModelResourceLocation HE = model("block/mine_he_world");
    private static final ModelResourceLocation SHRAPNEL = model("block/mine_shrap_world");
    private static final ModelResourceLocation NUCLEAR = model("block/mine_fat_world");
    private static final ModelResourceLocation NAVAL = model("block/mine_naval_world");

    public LandmineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(AP_GRASS);
        event.register(AP_DESERT);
        event.register(AP_SNOW);
        event.register(AP_STONE);
        event.register(HE);
        event.register(SHRAPNEL);
        event.register(NUCLEAR);
        event.register(NAVAL);
    }

    @Override
    public void render(LandmineBlockEntity mine, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = mine.getBlockState();
        if (!(state.getBlock() instanceof LandmineBlock landmine)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));

        switch (landmine.type()) {
            case AP -> {
                poseStack.scale(0.375F, 0.375F, 0.375F);
                poseStack.translate(0.0D, -0.21875D, 0.0D);
                render(apModel(mine.getLevel(), mine.getBlockPos()), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case HE -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                render(HE, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SHRAPNEL -> {
                poseStack.scale(0.375F, 0.375F, 0.375F);
                poseStack.translate(0.0D, -0.21875D, 0.0D);
                render(SHRAPNEL, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case NUCLEAR -> {
                poseStack.scale(0.25F, 0.25F, 0.25F);
                render(NUCLEAR, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case NAVAL -> {
                poseStack.translate(0.0D, 0.5D, 0.0D);
                render(NAVAL, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LandmineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 1.0D, 2.0D);
    }

    private static ModelResourceLocation apModel(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level == null) {
            return AP_GRASS;
        }
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) > pos.getY() + 2) {
            return AP_STONE;
        }
        Biome biome = level.getBiome(pos).value();
        if (biome.coldEnoughToSnow(pos)) {
            return AP_SNOW;
        }
        if (biome.getBaseTemperature() >= 1.5F && biome.getModifiedClimateSettings().downfall() <= 0.1F) {
            return AP_DESERT;
        }
        return AP_GRASS;
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                               BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                state, packedLight, packedOverlay);
    }
}
