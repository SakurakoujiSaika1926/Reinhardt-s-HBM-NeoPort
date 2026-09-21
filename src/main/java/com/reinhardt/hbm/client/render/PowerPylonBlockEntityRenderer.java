package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

public class PowerPylonBlockEntityRenderer implements LongRangeBlockEntityRenderer<PowerPylonBlockEntity> {
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
    private static final ResourceLocation RED_PYLON_TEXTURE =
            ReinhardtsHBM.id("textures/models/network/modelpylon.png");
    private final LegacyRedPylonModel redPylonModel;

    public PowerPylonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.redPylonModel = new LegacyRedPylonModel(context.bakeLayer(LegacyRedPylonModel.LAYER));
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
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
        if (pylon.kind() == PowerPylonBlock.Kind.RED_PYLON) {
            renderLegacyRedPylon(poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        ModelResourceLocation model = switch (pylon.kind()) {
            case RED_CONNECTOR -> RED_CONNECTOR;
            case CONNECTOR_RED_SUPER -> CONNECTOR_RED_SUPER;
            case RED_PYLON -> throw new IllegalStateException("Legacy red pylon uses ModelPylon geometry");
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

    private void renderLegacyRedPylon(PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // RenderPylon: translate(x + 0.5, y + 0.625, z + 0.5), then rotate Z 180 degrees.
        poseStack.translate(0.5F, 0.625F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        this.redPylonModel.render(poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(RED_PYLON_TEXTURE)),
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderLines(PowerPylonBlockEntity pylon, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (pylon.getLevel() == null || pylon.connections().isEmpty()) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(pylon.color() == 0 ? WIRE_TEXTURE : WIRE_GREYSCALE_TEXTURE));
        Vec3[] mounts = pylon.mountPositions();
        for (BlockPos connection : pylon.connections()) {
            PowerPylonBlockEntity.ConnectionRenderInfo otherInfo = pylon.connectionRenderInfo(connection);
            if (otherInfo == null) {
                continue;
            }
            PowerPylonBlockEntity other = pylon.getLevel().getBlockEntity(connection) instanceof PowerPylonBlockEntity loaded
                    ? loaded : null;
            boolean splitAcrossLoadedEndpoints = other != null && other.hasConnection(pylon.getBlockPos());
            Vec3[] otherMounts = otherInfo.mountPositions();
            int lineCount = Math.min(mounts.length, otherMounts.length);
            for (int i = 0; i < lineCount; i++) {
                int otherIndex = adjustedOtherMountIndex(pylon, otherInfo, i, lineCount);
                Vec3 first = mounts[i % mounts.length];
                Vec3 secondAbsolute = otherMounts[otherIndex].add(connection.getX() - pylon.getBlockPos().getX(), connection.getY() - pylon.getBlockPos().getY(), connection.getZ() - pylon.getBlockPos().getZ());
                if (splitAcrossLoadedEndpoints) {
                    // Match 1.7.10 while both endpoint block entities are loaded:
                    // each pylon owns one half of the cable.
                    Vec3 midpoint = first.add(secondAbsolute.subtract(first).scale(0.5D));
                    renderSaggingWire(pylon, poseStack, consumer, first, midpoint, false, packedOverlay);
                } else {
                    // A 100-block high-voltage span can outlive the client chunk
                    // containing its far endpoint. Draw the whole cached span
                    // from the loaded end instead of letting the cable vanish.
                    renderSaggingWire(pylon, poseStack, consumer, first, secondAbsolute, true, packedOverlay);
                }
            }
        }
    }

    private int adjustedOtherMountIndex(PowerPylonBlockEntity first, PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int line, int lineCount) {
        int index = line % second.mountPositions().length;
        if (lineCount == 4 && (first.kind() == PowerPylonBlock.Kind.SUBSTATION || second.kind() == PowerPylonBlock.Kind.SUBSTATION)) {
            return matchedOtherMountIndex(first, second, line, lineCount);
        }
        if (lineCount == 4 && first.kind() == PowerPylonBlock.Kind.RED_PYLON_LARGE && second.kind() == PowerPylonBlock.Kind.RED_PYLON_LARGE) {
            Direction a = first.facing();
            Direction b = second.facing();
            if ((a == Direction.EAST && b == Direction.NORTH) || (a == Direction.NORTH && b == Direction.EAST)) {
                index = (index + 2) % second.mountPositions().length;
            }
        }
        return index;
    }

    private int matchedOtherMountIndex(PowerPylonBlockEntity first, PowerPylonBlockEntity.ConnectionRenderInfo second,
                                       int line, int lineCount) {
        boolean firstIsCanonical = compareBlockPos(first.getBlockPos(), second.pos()) <= 0;
        Vec3[] firstMounts = absoluteMounts(first.mountPositions(), first.getBlockPos(), lineCount);
        Vec3[] secondMounts = absoluteMounts(second.mountPositions(), second.pos(), lineCount);
        int[] mapping = firstIsCanonical
                ? bestMountMapping(firstMounts, secondMounts, lineCount)
                : bestMountMapping(secondMounts, firstMounts, lineCount);
        int normalizedLine = line % lineCount;

        if (firstIsCanonical) {
            return mapping[normalizedLine];
        }

        for (int i = 0; i < mapping.length; i++) {
            if (mapping[i] == normalizedLine) {
                return i;
            }
        }
        return normalizedLine % second.mountPositions().length;
    }

    private int[] bestMountMapping(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        chooseMountMapping(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private void chooseMountMapping(int depth, int[] current, int[] best, boolean[] used,
                                    Vec3[] firstMounts, Vec3[] secondMounts, double[] bestScore) {
        if (depth == current.length) {
            double score = mountMappingScore(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                chooseMountMapping(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private double mountMappingScore(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + projectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private int projectedCrossings(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        Vec3 firstCenter = average(firstMounts);
        Vec3 secondCenter = average(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (projectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private boolean projectedSegmentsCross(double ax, double ay, double bx, double by,
                                           double cx, double cy, double dx, double dy) {
        double o1 = orientation(ax, ay, bx, by, cx, cy);
        double o2 = orientation(ax, ay, bx, by, dx, dy);
        double o3 = orientation(cx, cy, dx, dy, ax, ay);
        double o4 = orientation(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private double orientation(double ax, double ay, double bx, double by, double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private Vec3 average(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private Vec3[] absoluteMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private int compareBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    private void renderSaggingWire(PowerPylonBlockEntity pylon, PoseStack poseStack, VertexConsumer consumer,
                                   Vec3 start, Vec3 end, boolean fullSpan, int packedOverlay) {
        Vec3 delta = end.subtract(start);
        double hang = Math.min(delta.length() / (fullSpan ? 30.0D : 15.0D), 2.5D);
        int color = pylon.color() == 0 ? 0xFFFFFF : pylon.color();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int segments = 10;

        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / (double) segments;
            double t1 = (double) (i + 1) / (double) segments;
            double sagArc = fullSpan ? Math.PI : Math.PI * 0.5D;
            double sag0 = Math.sin(t0 * sagArc) * hang;
            double sag1 = Math.sin(t1 * sagArc) * hang;
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
        // Keep the exact 1.7.10 wire girth; the previous 0.025 port value
        // made long high-voltage spans alias out at shallow viewing angles.
        horizontalNormal = horizontalNormal.normalize().scale(0.03125D);
        Vec3 verticalNormal = delta.cross(horizontalNormal).normalize().scale(0.03125D);
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
