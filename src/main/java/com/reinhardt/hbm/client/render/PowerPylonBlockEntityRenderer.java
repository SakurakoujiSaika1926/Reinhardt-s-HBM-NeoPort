package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class PowerPylonBlockEntityRenderer implements BlockEntityRenderer<PowerPylonBlockEntity> {
    private static final ModelResourceLocation RED_PYLON = MachineModelRenderer.standalone("block/red_pylon_world");
    private static final ModelResourceLocation RED_CONNECTOR = MachineModelRenderer.standalone("block/red_connector_world");
    private static final ModelResourceLocation CONNECTOR_RED_SUPER = MachineModelRenderer.standalone("block/connector_red_super_world");
    private static final ModelResourceLocation RED_PYLON_LARGE = MachineModelRenderer.standalone("block/red_pylon_large_world");
    private static final ModelResourceLocation RED_PYLON_MEDIUM_WOOD = MachineModelRenderer.standalone("block/red_pylon_medium_wood_world");
    private static final ModelResourceLocation RED_PYLON_MEDIUM_WOOD_TRANSFORMER = MachineModelRenderer.standalone("block/red_pylon_medium_transformer_world");
    private static final ModelResourceLocation RED_PYLON_MEDIUM_STEEL = MachineModelRenderer.standalone("block/red_pylon_steel_world");
    private static final ModelResourceLocation RED_PYLON_MEDIUM_STEEL_TRANSFORMER = MachineModelRenderer.standalone("block/red_pylon_steel_transformer_world");
    private static final ModelResourceLocation SUBSTATION = MachineModelRenderer.standalone("block/substation_world");
    private static final ResourceLocation WIRE_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire.png");
    private static final ResourceLocation WIRE_GREYSCALE_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire_greyscale.png");

    public PowerPylonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RED_PYLON);
        event.register(RED_CONNECTOR);
        event.register(CONNECTOR_RED_SUPER);
        event.register(RED_PYLON_LARGE);
        event.register(RED_PYLON_MEDIUM_WOOD);
        event.register(RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
        event.register(RED_PYLON_MEDIUM_STEEL);
        event.register(RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
        event.register(SUBSTATION);
    }

    @Override
    public void render(PowerPylonBlockEntity pylon, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderModel(pylon, poseStack, bufferSource, packedLight, packedOverlay);
        renderLines(pylon, poseStack, bufferSource, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(PowerPylonBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }

    private void renderModel(PowerPylonBlockEntity pylon, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = pylon.getBlockState();
        Direction facing = state.hasProperty(PowerPylonBlock.FACING) ? state.getValue(PowerPylonBlock.FACING) : Direction.SOUTH;
        ModelResourceLocation model = switch (pylon.kind()) {
            case RED_CONNECTOR -> RED_CONNECTOR;
            case CONNECTOR_RED_SUPER -> CONNECTOR_RED_SUPER;
            case RED_PYLON -> RED_PYLON;
            case RED_PYLON_MEDIUM_WOOD -> RED_PYLON_MEDIUM_WOOD;
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> RED_PYLON_MEDIUM_WOOD_TRANSFORMER;
            case RED_PYLON_MEDIUM_STEEL -> RED_PYLON_MEDIUM_STEEL;
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> RED_PYLON_MEDIUM_STEEL_TRANSFORMER;
            case RED_PYLON_LARGE -> RED_PYLON_LARGE;
            case SUBSTATION -> SUBSTATION;
        };

        poseStack.pushPose();
        switch (pylon.kind()) {
            case RED_CONNECTOR, CONNECTOR_RED_SUPER -> orientConnector(poseStack, facing);
            case RED_PYLON_MEDIUM_WOOD,
                 RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                 RED_PYLON_MEDIUM_STEEL,
                 RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> orientLegacyCentered(poseStack, mediumPylonYaw(facing));
            case RED_PYLON_LARGE -> orientLegacyCentered(poseStack, largePylonYaw(facing));
            case SUBSTATION -> orientLegacyCentered(poseStack, substationYaw(facing));
            default -> {
            }
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderLines(PowerPylonBlockEntity pylon, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (pylon.getLevel() == null || pylon.connections().isEmpty()) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(pylon.color() == 0 ? WIRE_TEXTURE : WIRE_GREYSCALE_TEXTURE));
        Vec3[] mounts = pylon.mountPositions();
        for (BlockPos connection : pylon.connections()) {
            if (!(pylon.getLevel().getBlockEntity(connection) instanceof PowerPylonBlockEntity other)) {
                continue;
            }
            Vec3[] otherMounts = other.mountPositions();
            int lineCount = Math.min(mounts.length, otherMounts.length);
            for (int i = 0; i < lineCount; i++) {
                int otherIndex = adjustedOtherMountIndex(pylon, other, i, lineCount);
                Vec3 first = mounts[i % mounts.length];
                Vec3 secondAbsolute = otherMounts[otherIndex].add(connection.getX() - pylon.getBlockPos().getX(), connection.getY() - pylon.getBlockPos().getY(), connection.getZ() - pylon.getBlockPos().getZ());
                // 1.7.10 renders each half from its own pylon to the midpoint.
                // Rendering the full span here duplicates every cable because
                // the connected pylon renders its own half as well.
                Vec3 midpoint = first.add(secondAbsolute.subtract(first).scale(0.5D));
                renderSaggingWire(pylon, poseStack, consumer, first, midpoint, packedOverlay);
            }
        }
    }

    private int adjustedOtherMountIndex(PowerPylonBlockEntity first, PowerPylonBlockEntity second, int line, int lineCount) {
        int index = line % second.mountPositions().length;
        if (lineCount == 4 && first.kind() == PowerPylonBlock.Kind.RED_PYLON_LARGE && second.kind() == PowerPylonBlock.Kind.RED_PYLON_LARGE) {
            Direction a = first.getBlockState().getValue(PowerPylonBlock.FACING);
            Direction b = second.getBlockState().getValue(PowerPylonBlock.FACING);
            if ((a == Direction.EAST && b == Direction.NORTH) || (a == Direction.NORTH && b == Direction.EAST)) {
                index = (index + 2) % second.mountPositions().length;
            }
        }
        return index;
    }

    private void renderSaggingWire(PowerPylonBlockEntity pylon, PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, int packedOverlay) {
        Vec3 delta = end.subtract(start);
        double hang = Math.min(delta.length() / 15.0D, 2.5D);
        int color = pylon.color() == 0 ? 0xFFFFFF : pylon.color();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int segments = 10;

        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / (double) segments;
            double t1 = (double) (i + 1) / (double) segments;
            double sag0 = Math.sin(t0 * Math.PI * 0.5D) * hang;
            double sag1 = Math.sin(t1 * Math.PI * 0.5D) * hang;
            Vec3 p0 = start.add(delta.scale(t0)).subtract(0.0D, sag0, 0.0D);
            Vec3 p1 = start.add(delta.scale(t1)).subtract(0.0D, sag1, 0.0D);
            drawWireSegment(poseStack, consumer, p0, p1, r, g, b, packedOverlay);
        }
    }

    private void drawWireSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 start, Vec3 end, int r, int g, int b, int packedOverlay) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(delta.length(), 0.001D);
        Vec3 horizontalNormal = new Vec3(-delta.z, 0.0D, delta.x);
        if (horizontalNormal.lengthSqr() < 0.0001D) {
            horizontalNormal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        horizontalNormal = horizontalNormal.normalize().scale(0.025D);
        Vec3 verticalNormal = delta.cross(horizontalNormal).normalize().scale(0.025D);
        int wrap = Math.max(1, Mth.ceil(length * 8.0D));

        quad(poseStack, consumer, start.add(horizontalNormal), start.subtract(horizontalNormal), end.subtract(horizontalNormal), end.add(horizontalNormal), wrap, r, g, b, packedOverlay);
        quad(poseStack, consumer, start.add(verticalNormal), start.subtract(verticalNormal), end.subtract(verticalNormal), end.add(verticalNormal), wrap, r, g, b, packedOverlay);
    }

    private void quad(PoseStack poseStack, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int wrap, int r, int g, int bl, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, a, 0.0F, 0.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, b, 0.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, c, wrap, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, d, wrap, 0.0F, r, g, bl, packedOverlay);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private void orientConnector(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 1.0F, 0.0F, 0.0F)));
            case NORTH -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 0.0F, 1.0F)));
            }
            case SOUTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
            case WEST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 0.0F, 1.0F)));
            }
            case EAST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 0.0F, 1.0F)));
            }
            default -> {
            }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
    }

    private void orientLegacyCentered(PoseStack poseStack, float degrees) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }

    private float largePylonYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case EAST -> 45.0F;
            case SOUTH -> 0.0F;
            case WEST -> 135.0F;
            default -> 0.0F;
        };
    }

    private float mediumPylonYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private float substationYaw(Direction facing) {
        return (facing == Direction.NORTH || facing == Direction.SOUTH) ? 90.0F : 0.0F;
    }
}
