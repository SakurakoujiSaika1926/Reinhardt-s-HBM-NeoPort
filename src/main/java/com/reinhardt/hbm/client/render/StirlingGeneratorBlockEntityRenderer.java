package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.StirlingGeneratorBlock;
import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class StirlingGeneratorBlockEntityRenderer implements BlockEntityRenderer<StirlingGeneratorBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_stirling_world");
    private static final ModelResourceLocation COG = MachineModelRenderer.standalone("block/machine_stirling_cog");
    private static final ModelResourceLocation COG_SMALL = MachineModelRenderer.standalone("block/machine_stirling_cog_small");
    private static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_stirling_piston");
    private static final ModelResourceLocation WORLD_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_world");
    private static final ModelResourceLocation COG_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog");
    private static final ModelResourceLocation COG_SMALL_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog_small");
    private static final ModelResourceLocation PISTON_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_piston");
    private static final ModelResourceLocation WORLD_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_world");
    private static final ModelResourceLocation COG_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog");
    private static final ModelResourceLocation COG_SMALL_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog_small");
    private static final ModelResourceLocation PISTON_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_piston");

    public StirlingGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
        event.register(COG);
        event.register(COG_SMALL);
        event.register(PISTON);
        event.register(WORLD_STEEL);
        event.register(COG_STEEL);
        event.register(COG_SMALL_STEEL);
        event.register(PISTON_STEEL);
        event.register(WORLD_CREATIVE);
        event.register(COG_CREATIVE);
        event.register(COG_SMALL_CREATIVE);
        event.register(PISTON_CREATIVE);
    }

    @Override
    public void render(StirlingGeneratorBlockEntity stirling, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = stirling.getBlockState();
        Direction facing = state.hasProperty(StirlingGeneratorBlock.FACING) ? state.getValue(StirlingGeneratorBlock.FACING) : Direction.NORTH;
        float rot = stirling.getSpin(partialTick);
        PartSet parts = PartSet.forState(state);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyStirlingParts(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.world), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (stirling.hasCog()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.375F, 0.0F);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rot), 0.0F, 0.0F, 1.0F)));
            poseStack.translate(0.0F, -1.375F, 0.0F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cog), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.375F, 0.25F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot * 2.0F + 3.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -1.375F, -0.25F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cogSmall), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rot * Math.PI / 90.0D) * 0.25D + 0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.piston), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StirlingGeneratorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 3.0D, 3.0D);
    }

    private record PartSet(ModelResourceLocation world, ModelResourceLocation cog, ModelResourceLocation cogSmall, ModelResourceLocation piston) {
        private static PartSet forState(BlockState state) {
            if (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get())) {
                return new PartSet(WORLD_CREATIVE, COG_CREATIVE, COG_SMALL_CREATIVE, PISTON_CREATIVE);
            }
            if (state.is(HbmBlocks.MACHINE_STIRLING_STEEL.get())) {
                return new PartSet(WORLD_STEEL, COG_STEEL, COG_SMALL_STEEL, PISTON_STEEL);
            }
            return new PartSet(WORLD, COG, COG_SMALL, PISTON);
        }
    }
}
