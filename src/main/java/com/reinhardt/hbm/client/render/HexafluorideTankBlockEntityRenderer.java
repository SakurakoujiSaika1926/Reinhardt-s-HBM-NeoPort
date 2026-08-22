package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.HexafluorideTankBlock;
import com.reinhardt.hbm.blockentity.HexafluorideTankBlockEntity;
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

/** Exact RenderUF6Tank / RenderPuF6Tank world transforms using the original tank.obj mesh. */
public final class HexafluorideTankBlockEntityRenderer implements BlockEntityRenderer<HexafluorideTankBlockEntity> {
    private static final ModelResourceLocation UF6_MODEL = MachineModelRenderer.standalone("block/machine_uf6_tank_world");
    private static final ModelResourceLocation PUF6_MODEL = MachineModelRenderer.standalone("block/machine_puf6_tank_world");

    public HexafluorideTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(UF6_MODEL);
        event.register(PUF6_MODEL);
    }

    @Override
    public void render(HexafluorideTankBlockEntity tank, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderModel(tank.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        boolean plutonium = state.is(HbmBlocks.MACHINE_PUF6_TANK.get());
        Direction facing = state.getValue(HexafluorideTankBlock.FACING);

        poseStack.pushPose();
        // Render*F6Tank: translate(x + .5, y, z + .5), then apply its
        // metadata table. The OBJ itself is centred on the old render origin.
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(legacyYaw(facing)));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(plutonium ? PUF6_MODEL : UF6_MODEL),
                poseStack, bufferSource, state, packedLight, packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexafluorideTankBlockEntity tank) {
        // tank.obj occupies y=0..2 but does not create a second collision block.
        return new AABB(tank.getBlockPos()).expandTowards(0.0D, 1.0D, 0.0D);
    }

    static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }
}
