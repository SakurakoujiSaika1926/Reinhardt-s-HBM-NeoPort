package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadarScreenBlock;
import com.reinhardt.hbm.blockentity.RadarScreenBlockEntity;
import com.reinhardt.hbm.blockentity.RadarTarget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct visual port of RenderRadarScreen, with its own fixed legacy rotation table. */
public final class RadarScreenBlockEntityRenderer implements LongRangeBlockEntityRenderer<RadarScreenBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/radar_screen_world");
    private static final ResourceLocation RADAR_GUI = ReinhardtsHBM.id("textures/gui/gui_radar_nt.png");
    private static final AABB NORTH_BOUNDS = createRadarLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = createRadarLocalBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = createRadarLocalBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = createRadarLocalBounds(Direction.EAST);

    public RadarScreenBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RadarScreenBlockEntity screen, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = screen.getBlockState();
        Direction facing = radarScreenFacing(state);

        poseStack.pushPose();
        applyRadarPose(poseStack, facing);
        renderAssembly(state, poseStack, bufferSource, packedLight, packedOverlay);
        renderDisplay(screen, partialTick, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    static void renderAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                               int packedLight, int packedOverlay) {
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(RadarScreenBlockEntity screen) {
        BlockState state = screen.getBlockState();
        Direction facing = radarScreenFacing(state);
        // Model face bounds are X -0.5..0.5, Y 0..2, Z -0.5..1.5.
        // Noise, scan and in-range blip quads all lie inside this envelope.
        AABB local = switch (facing) {
            case NORTH -> NORTH_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case EAST -> EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Radar screen facing must be horizontal: " + facing);
        };
        return local.move(screen.getBlockPos());
    }

    private static void renderDisplay(RadarScreenBlockEntity screen, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedOverlay) {
        RadarScreenBlockEntity.RenderSnapshot snapshot = screen.renderSnapshot();
        if (!snapshot.linked()) {
            int noiseRow = 118 + Math.floorMod((int) (screen.getBlockPos().asLong() ^ System.nanoTime() / 20_000_000L), 81);
            VertexConsumer noise = bufferSource.getBuffer(RenderType.entityCutoutNoCull(RADAR_GUI));
            emitQuad(noise, poseStack.last(), 0.38D, 1.875D, 1.375D, 0.125D, -0.375D,
                    216.0F / 256.0F, (noiseRow + 40.0F) / 256.0F, 1.0F, noiseRow / 256.0F,
                    255, 255, 255, 255, packedOverlay);
            return;
        }

        long gameTime = screen.getLevel() == null ? 0L : screen.getLevel().getGameTime();
        float sweep = ((gameTime % 56L) + partialTick) / 30.0F;
        VertexConsumer scan = bufferSource.getBuffer(RenderType.lightning());
        emitQuad(scan, poseStack.last(), 0.38D, 2.0D - sweep, 1.375D, 1.875D - sweep, -0.375D,
                0.0F, 0.0F, 1.0F, 1.0F, 0, 255, 0, 50, packedOverlay);

        if (snapshot.entries().isEmpty() || snapshot.range() <= 0) {
            return;
        }
        VertexConsumer blips = bufferSource.getBuffer(RenderType.entityCutoutNoCull(RADAR_GUI));
        for (RadarTarget entry : snapshot.entries()) {
            double x = (entry.x() - snapshot.refX()) / ((double) snapshot.range() + 1.0D) * 0.875D;
            double z = (entry.z() - snapshot.refZ()) / ((double) snapshot.range() + 1.0D) * 0.875D;
            int type = Math.max(0, Math.min(31, entry.blipLevel()));
            double size = 0.0625D;
            emitQuad(blips, poseStack.last(), 0.38D, 1.0D - z + size, 0.5D - x + size,
                    1.0D - z - size, 0.5D - x - size,
                    216.0F / 256.0F, (type * 8.0F + 8.0F) / 256.0F,
                    224.0F / 256.0F, type * 8.0F / 256.0F,
                    255, 255, 255, 255, packedOverlay);
        }
    }

    /** Exact 1.7.10 radar-screen transform, deliberately private to this machine. */
    private static void applyRadarPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Radar screen facing must be horizontal: " + facing);
        }
    }

    private static AABB createRadarLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyRadarPose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.5F, 0.0F, -0.5F, 0.5F, 2.0F, 1.5F);
    }

    private static Direction radarScreenFacing(BlockState state) {
        if (!state.hasProperty(RadarScreenBlock.FACING)) {
            throw new IllegalStateException("Radar screen block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RadarScreenBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Radar screen block state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, double x, double topY, double topZ,
                                 double bottomY, double bottomZ, float uMin, float vMax, float uMax, float vMin,
                                 int red, int green, int blue, int alpha, int packedOverlay) {
        vertex(consumer, pose, x, topY, topZ, uMin, vMax, red, green, blue, alpha, packedOverlay);
        vertex(consumer, pose, x, topY, bottomZ, uMax, vMax, red, green, blue, alpha, packedOverlay);
        vertex(consumer, pose, x, bottomY, bottomZ, uMax, vMin, red, green, blue, alpha, packedOverlay);
        vertex(consumer, pose, x, bottomY, topZ, uMin, vMin, red, green, blue, alpha, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float u, float v,
                               int red, int green, int blue, int alpha, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 1.0F, 0.0F, 0.0F);
    }
}
