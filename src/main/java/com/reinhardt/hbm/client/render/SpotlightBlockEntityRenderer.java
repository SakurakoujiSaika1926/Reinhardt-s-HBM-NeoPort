package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SpotlightBlock;
import com.reinhardt.hbm.blockentity.SpotlightBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Renders the original 1.7.10 spotlight OBJ parts and orientation. */
public final class SpotlightBlockEntityRenderer implements BlockEntityRenderer<SpotlightBlockEntity> {
    private static final ModelResourceLocation INCANDESCENT_ON = model("spotlight_incandescent_world");
    private static final ModelResourceLocation INCANDESCENT_OFF = model("spotlight_incandescent_off_world");
    private static final ModelResourceLocation HALOGEN_ON = model("spotlight_halogen_world");
    private static final ModelResourceLocation HALOGEN_OFF = model("spotlight_halogen_off_world");
    private static final ModelResourceLocation FLUORO_SINGLE_ON = model("spotlight_fluoro_single_world");
    private static final ModelResourceLocation FLUORO_CAP_ON = model("spotlight_fluoro_cap_world");
    private static final ModelResourceLocation FLUORO_MID_ON = model("spotlight_fluoro_mid_world");
    private static final ModelResourceLocation FLUORO_SINGLE_OFF = model("spotlight_fluoro_single_off_world");
    private static final ModelResourceLocation FLUORO_CAP_OFF = model("spotlight_fluoro_cap_off_world");
    private static final ModelResourceLocation FLUORO_MID_OFF = model("spotlight_fluoro_mid_off_world");

    private static final List<ModelResourceLocation> MODELS = List.of(
            INCANDESCENT_ON, INCANDESCENT_OFF, HALOGEN_ON, HALOGEN_OFF,
            FLUORO_SINGLE_ON, FLUORO_CAP_ON, FLUORO_MID_ON,
            FLUORO_SINGLE_OFF, FLUORO_CAP_OFF, FLUORO_MID_OFF
    );

    public SpotlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.forEach(event::register);
    }

    @Override
    public void render(SpotlightBlockEntity spotlight, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = spotlight.getBlockState();
        SpotlightBlock block = (SpotlightBlock) state.getBlock();
        Direction facing = state.getValue(SpotlightBlock.FACING);
        Direction connectionDirection = null;
        int connectionCount = 0;
        if (block.kind().fluorescent() && spotlight.getLevel() != null) {
            for (Direction direction : Direction.values()) {
                if (direction == facing || direction == facing.getOpposite()) {
                    continue;
                }
                if (block.canConnectTo(spotlight.getLevel().getBlockState(spotlight.getBlockPos().relative(direction)))) {
                    connectionDirection = direction;
                    connectionCount++;
                    if (block.canConnectTo(spotlight.getLevel().getBlockState(
                            spotlight.getBlockPos().relative(direction.getOpposite())))) {
                        connectionCount++;
                    }
                    break;
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(
                0.5F - facing.getStepX() * 0.5F,
                0.5F - facing.getStepY() * 0.5F,
                0.5F - facing.getStepZ() * 0.5F
        );
        // RenderLight used Vec3.rotateAroundX/Z.  Those legacy methods have
        // the opposite sign from a positive modern PoseStack axis rotation;
        // Y rotation is already the same convention.  Preserve each old axis
        // operation explicitly rather than applying a shared orientation.
        poseStack.mulPose(Axis.XP.rotationDegrees(-modularRoll(connectionDirection, facing)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch(facing)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw(facing)));

        ModelResourceLocation model = switch (block.kind()) {
            case INCANDESCENT -> block.isLit() ? INCANDESCENT_ON : INCANDESCENT_OFF;
            case HALOGEN -> block.isLit() ? HALOGEN_ON : HALOGEN_OFF;
            case FLUORESCENT -> fluoroModel(connectionCount, block.isLit());
        };
        MachineModelRenderer.renderUnculledCutoutNoCull(
                MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay
        );
        poseStack.popPose();
    }

    private static ModelResourceLocation fluoroModel(int connections, boolean lit) {
        ModelResourceLocation single = lit ? FLUORO_SINGLE_ON : FLUORO_SINGLE_OFF;
        ModelResourceLocation cap = lit ? FLUORO_CAP_ON : FLUORO_CAP_OFF;
        ModelResourceLocation mid = lit ? FLUORO_MID_ON : FLUORO_MID_OFF;
        return connections == 0 ? single : connections == 1 ? cap : mid;
    }

    private static float pitch(Direction direction) {
        return switch (direction) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    private static float yaw(Direction direction) {
        return switch (direction) {
            case NORTH -> 90.0F;
            case SOUTH -> -90.0F;
            case WEST -> 180.0F;
            default -> 0.0F;
        };
    }

    private static float modularRoll(Direction connectionDirection, Direction axis) {
        if (connectionDirection == null) {
            return 0.0F;
        }
        float flipX = axis == Direction.DOWN || axis == Direction.NORTH || axis == Direction.WEST ? -0.5F : 0.5F;
        float addX = axis == Direction.NORTH || axis == Direction.SOUTH ? -0.5F : 0.0F;
        boolean flipNS = axis == Direction.WEST;
        return switch (connectionDirection) {
            case NORTH -> flipNS ? 180.0F : 0.0F;
            case SOUTH -> !flipNS ? 180.0F : 0.0F;
            case EAST -> 180.0F * (flipX + addX);
            case WEST -> 180.0F * (-flipX + addX);
            case UP -> -90.0F;
            case DOWN -> 90.0F;
        };
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    @Override
    public AABB getRenderBoundingBox(SpotlightBlockEntity spotlight) {
        return new AABB(spotlight.getBlockPos());
    }
}
