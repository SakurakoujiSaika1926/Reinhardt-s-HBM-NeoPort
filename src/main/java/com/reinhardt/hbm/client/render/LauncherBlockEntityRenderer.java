package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class LauncherBlockEntityRenderer implements LongRangeBlockEntityRenderer<LauncherBlockEntity> {
    /** Keep ground-level OBJ faces off the exact depth plane used by terrain. */
    private static final double DEPTH_EPSILON = 1.0D / 1024.0D;
    public static final Set<String> ITEM_IDS = Set.of("compact_launcher", "launch_pad", "launch_pad_rusted", "launch_pad_large", "launch_table");
    private static final BlockState ITEM_RENDER_STATE = Blocks.AIR.defaultBlockState();
    private static final Set<LauncherBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeometryGeneration;

    /** Identity keys keep equal coordinates in separate client levels from sharing GPU state. */
    private final Map<LauncherBlockEntity, TableBuffers> tableBuffers = new IdentityHashMap<>();

    public LauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        LauncherBlockEntity.setClientRenderSnapshotUpdater(LauncherBlockEntityRenderer::updateRenderSnapshot);
        LauncherBlockEntity.setClientGpuCacheDisposer(LauncherBlockEntityRenderer::discardMachineCache);
    }

    /** Invalidates snapshots before the global model cache can expose the next bake. */
    static void beginGeometryReload() {
        geometryGeneration = null;
        closeAllGpuCaches();
        LauncherBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Captures render layers, quads and face-used float vertices from one completed bake. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(LauncherBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        Map<BlockState, Map<ModelResourceLocation, CapturedModel>> byState = new IdentityHashMap<>();
        Map<List<CapturedLayer>, CapturedModel> canonicalModels = new HashMap<>();
        for (BlockState state : allRenderStates()) {
            Map<ModelResourceLocation, CapturedModel> models = new HashMap<>();
            for (ModelResourceLocation location : allModels()) {
                models.put(location, captureModel(location, state, canonicalModels));
            }
            byState.put(state, Map.copyOf(models));
        }
        geometryGeneration = new GeometryGeneration(++nextGeometryGeneration,
                Collections.unmodifiableMap(byState));
        closeAllGpuCaches();
        LauncherBlockEntity.setClientRenderSnapshotUpdater(LauncherBlockEntityRenderer::updateRenderSnapshot);
        LauncherBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    static void closeGpuCachesOnLogout() {
        closeAllGpuCaches();
    }
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (String model : List.of("launch_pad_world", "launch_pad_rusted_world", "compact_launcher_world", "launch_table_base",
                "launch_table_small_pad", "launch_table_large_pad", "launch_table_small_scaffold_base", "launch_table_small_scaffold_connector",
                "launch_table_small_scaffold_empty", "launch_table_large_scaffold_base", "launch_table_large_scaffold_connector", "launch_table_large_scaffold_empty"))
            event.register(MachineModelRenderer.standalone("block/" + model));
        event.register(MachineModelRenderer.standalone("block/launcher_port/pad_base"));
        for (String factor : List.of("abm", "micro", "v2", "strong", "huge", "atlas")) for (String part : List.of("pad", "erector", "pivot", "rope"))
            event.register(MachineModelRenderer.standalone("block/launcher_port/" + factor + "_" + part));
        LauncherMissileRenderer.registerAdditionalModels(event);
    }
    private static void draw(GeometryGeneration generation, String path, BlockState state, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        CapturedModel model = generation.model(model(path), state);
        if (model != null) {
            model.render(pose, buffers, light, overlay);
        }
    }
    @Override public void render(LauncherBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        LauncherBlockEntity.RenderSnapshot snapshot = be.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        pose.pushPose();
        switch (snapshot.kind()) {
            case PAD_SMALL -> {
                applySmallLaunchPadPose(pose, snapshot.facing());
                draw(generation, "launch_pad_world", state, pose, buffers, light, overlay);
                pose.translate(0, 1, 0);
                renderMissile(generation, snapshot.missile(), state, pose, buffers, light, overlay);
            }
            case PAD_RUSTED -> {
                applyRustedLaunchPadPose(pose, snapshot.facing());
                draw(generation, "launch_pad_rusted_world", state, pose, buffers, light, overlay);
                pose.translate(0, 1, 0);
                renderMissile(generation, snapshot.missile(), state, pose, buffers, light, overlay);
            }
            case COMPACT -> {
                applyCompactLauncherPose(pose);
                pose.translate(0, DEPTH_EPSILON, 0);
                draw(generation, "compact_launcher_world", state, pose, buffers, light, overlay);
                pose.translate(0, 1.0625, 0);
                if (snapshot.validMissile()) renderMissile(generation, snapshot.missile(), state, pose, buffers, light, overlay);
            }
            case PAD_LARGE -> {
                applyLargeLaunchPadPose(pose, snapshot.facing());
                large(generation, snapshot, partial, state, pose, buffers, light, overlay);
            }
            case TABLE -> {
                applyLaunchTablePose(pose, snapshot.facing());
                table(be, generation, snapshot, state, pose, buffers, light, overlay);
            }
            case SOYUZ -> throw new IllegalStateException("Soyuz has its own block entity and renderer");
        }
        pose.popPose();
    }
    private static void large(GeometryGeneration generation, LauncherBlockEntity.RenderSnapshot snapshot,
                              float partial, BlockState state, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        pose.translate(0, DEPTH_EPSILON, 0);
        draw(generation, "launcher_port/pad_base", state, pose, buffers, light, overlay);
        if (snapshot.formFactor() < 0
                || snapshot.formFactor() >= LegacyMissileItem.FormFactor.values().length) return;
        var factor = LegacyMissileItem.FormFactor.values()[snapshot.formFactor()];
        String prefix = switch (factor) { case ABM, OTHER -> "abm"; case MICRO -> "micro"; case V2 -> "v2";
            case STRONG -> "strong"; case HUGE -> "huge"; case ATLAS -> "atlas"; };
        double z = switch (factor) { case ABM, MICRO, OTHER -> 1.5; case V2 -> 1.75; case STRONG, HUGE -> 3; case ATLAS -> 4; };
        double y = factor == LegacyMissileItem.FormFactor.ABM || factor == LegacyMissileItem.FormFactor.MICRO
                || factor == LegacyMissileItem.FormFactor.V2 || factor == LegacyMissileItem.FormFactor.OTHER ? 1.25 : 1.5;
        String path = "launcher_port/" + prefix + "_";
        pose.pushPose();
        pose.translate(0, DEPTH_EPSILON, 0);
        draw(generation, path + "pad", state, pose, buffers, light, overlay);
        pose.popPose();
        if (snapshot.missile().present() && snapshot.erected()) draw(generation, path + "rope", state, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0, y, -z);
        pose.mulPose(Axis.XP.rotationDegrees(-snapshot.erector(partial)));
        pose.translate(0, -y, z);
        draw(generation, path + "pivot", state, pose, buffers, light, overlay);
        pose.translate(0, snapshot.lift(partial), 0);
        draw(generation, path + "erector", state, pose, buffers, light, overlay);
        if (snapshot.erected()) { pose.popPose(); pose.pushPose(); }
        if (snapshot.missile().present() && (snapshot.erected() || snapshot.readyToLoad())) {
            pose.translate(0, 2, 0);
            renderMissile(generation, snapshot.missile(), state, pose, buffers, light, overlay);
        }
        pose.popPose();
    }
    private void table(LauncherBlockEntity be, GeometryGeneration generation,
                       LauncherBlockEntity.RenderSnapshot snapshot, BlockState state, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        int connector = connectorIndex(snapshot.scaffoldHeight());
        TableBuffers cached = tableBuffers.computeIfAbsent(be, ignored -> new TableBuffers());
        cached.prepare(generation, snapshot.tableSize(), snapshot.scaffoldHeight(), connector,
                snapshot.validMissile(), state, light, overlay);
        drawLayers(cached.layers(), pose);
        pose.translate(0, 2.0625, 0);
        if (snapshot.validMissile()) renderMissile(generation, snapshot.missile(), state, pose, buffers, light, overlay);
    }
    /** Exact 1.7.10 small launch-pad inventory pose. */
    static void renderSmallLaunchPadItem(BlockState state, ItemDisplayContext context, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        pose.pushPose();
        applySmallLaunchPadItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0, -1, 0);
            pose.scale(3, 3, 3);
        }
        draw(generation, "launch_pad_world", state, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** Exact 1.7.10 rusted launch-pad inventory pose, kept independent from the intact pad. */
    static void renderRustedLaunchPadItem(BlockState state, ItemDisplayContext context, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        pose.pushPose();
        applyRustedLaunchPadItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0, -1, 0);
            pose.scale(3, 3, 3);
        }
        draw(generation, "launch_pad_rusted_world", state, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** Exact compact-launcher inventory pose. */
    static void renderCompactLauncherItem(BlockState state, ItemDisplayContext context, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        pose.pushPose();
        applyCompactLauncherItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0, -4, 0);
            pose.scale(3.5F, 3.5F, 3.5F);
        }
        pose.scale(.5F, .5F, .5F);
        draw(generation, "compact_launcher_world", state, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** Exact large-launch-pad inventory pose. */
    static void renderLargeLaunchPadItem(BlockState state, ItemDisplayContext context, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        pose.pushPose();
        applyLargeLaunchPadItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0, -3.75, 0);
            pose.scale(1.625F, 1.625F, 1.625F);
        }
        pose.scale(.5F, .5F, .5F);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        for (String part : List.of("pad_base", "atlas_pad", "atlas_erector", "atlas_pivot")) {
            draw(generation, "launcher_port/" + part, state, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    /** Exact launch-table inventory pose. */
    static void renderLaunchTableItem(BlockState state, ItemDisplayContext context, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        pose.pushPose();
        applyLaunchTableItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0, -2, 0);
            pose.scale(2.5F, 2.5F, 2.5F);
        }
        pose.scale(.5F, .5F, .5F);
        draw(generation, "launch_table_base", state, pose, buffers, light, overlay);
        draw(generation, "launch_table_small_pad", state, pose, buffers, light, overlay);
        pose.translate(0, 0, 2.5);
        for (int i = 0; i < 8; i++) {
            pose.translate(0, 1, 0);
            draw(generation, "launch_table_small_scaffold_" + (i < 6 ? "base" : i == 6 ? "connector" : "empty"), state, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    private static void applySmallLaunchPadItemContext(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        pose.translate(0.5F, 0.25F, 0.0F);
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyRustedLaunchPadItemContext(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        pose.translate(0.5F, 0.25F, 0.0F);
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyCompactLauncherItemContext(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        pose.translate(0.5F, 0.25F, 0.0F);
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyLargeLaunchPadItemContext(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        pose.translate(0.5F, 0.25F, 0.0F);
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyLaunchTableItemContext(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        pose.translate(0.5F, 0.25F, 0.0F);
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }
    @Override public AABB getRenderBoundingBox(LauncherBlockEntity be) {
        return be.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(LauncherBlockEntity be) {
        GeometryGeneration generation = geometryGeneration;

        ItemStack stack = be.getItem(0);
        if (be.kind() == LauncherBlockEntity.Kind.PAD_RUSTED) {
            stack = be.missileLoaded()
                    ? new ItemStack(BuiltInRegistries.ITEM.getOptional(
                            ReinhardtsHBM.id("missile_doomsday_rusted")).orElseThrow())
                    : ItemStack.EMPTY;
        }
        LauncherBlockEntity.MissileRenderData missile = missileData(stack);
        CustomMissileData custom = CustomMissileData.read(stack);
        int scaffoldHeight = custom == null ? be.scaffoldHeight() : (int) missile.totalHeight();
        boolean validMissile = be.validMissile(stack);

        LauncherBlockEntity.RenderSnapshot state = new LauncherBlockEntity.RenderSnapshot(
                generation, be.getBlockState(), be.kind(), be.facing(), be.formFactor(),
                be.lift(0.0F), be.lift(1.0F), be.erector(0.0F), be.erector(1.0F),
                be.erected(), be.readyToLoad(), be.tableSize(), scaffoldHeight,
                validMissile, missile, zeroVolumeBounds(be.getBlockPos()),
                LauncherBlockEntity.clientRenderSnapshotRevision());
        LauncherBlockEntity.RenderSnapshot previous = be.renderSnapshot();
        AABB bounds = generation == null
                ? zeroVolumeBounds(be.getBlockPos())
                : sameGeometry(previous, state)
                ? previous.bounds()
                : buildBounds(be.getBlockPos(), state, generation);
        be.publishClientRenderSnapshot(new LauncherBlockEntity.RenderSnapshot(
                state.geometryToken(), state.blockState(), state.kind(), state.facing(), state.formFactor(),
                state.previousLift(), state.lift(), state.previousErector(), state.erector(),
                state.erected(), state.readyToLoad(), state.tableSize(), state.scaffoldHeight(),
                state.validMissile(), state.missile(), bounds, state.revision()));
    }

    private static boolean sameGeometry(LauncherBlockEntity.RenderSnapshot first,
                                        LauncherBlockEntity.RenderSnapshot second) {
        return first.geometryToken() == second.geometryToken()
                && first.revision() == second.revision()
                && first.blockState() == second.blockState()
                && first.kind() == second.kind()
                && first.facing() == second.facing()
                && first.formFactor() == second.formFactor()
                && first.tableSize() == second.tableSize()
                && first.scaffoldHeight() == second.scaffoldHeight()
                && first.validMissile() == second.validMissile()
                && first.missile().equals(second.missile());
    }

    private static LauncherBlockEntity.MissileRenderData missileData(ItemStack stack) {
        if (stack.isEmpty()) {
            return LauncherBlockEntity.MissileRenderData.EMPTY;
        }
        CustomMissileData custom = CustomMissileData.read(stack);
        if (custom != null) {
            String thruster = itemId(custom.thruster());
            String fins = itemId(custom.fins());
            String fuselage = itemId(custom.fuselage());
            String warhead = itemId(custom.warhead());
            float thrusterHeight = MissileMultipartRenderer.partHeight(thruster);
            float fuselageHeight = MissileMultipartRenderer.partHeight(fuselage);
            float totalHeight = thrusterHeight + fuselageHeight + MissileMultipartRenderer.partHeight(warhead);
            return new LauncherBlockEntity.MissileRenderData(true, true, false,
                    "", thruster, thrusterHeight, fins, fuselage, fuselageHeight,
                    warhead, totalHeight);
        }
        if (stack.getItem() instanceof LegacyMissileItem missile) {
            String id = itemId(stack);
            return new LauncherBlockEntity.MissileRenderData(
                    LauncherMissileRenderer.IDS.contains(id), false,
                    missile.tier() == LegacyMissileItem.Tier.TIER2,
                    id, "", 0.0F, "", "", 0.0F, "", 0.0F);
        }
        return LauncherBlockEntity.MissileRenderData.EMPTY;
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static void renderMissile(GeometryGeneration generation,
                                      LauncherBlockEntity.MissileRenderData missile,
                                      BlockState state, PoseStack pose, MultiBufferSource buffers,
                                      int light, int overlay) {
        if (!missile.present()) {
            return;
        }
        pose.pushPose();
        if (!missile.custom()) {
            if (missile.legacyTierTwo()) {
                pose.scale(1.5F, 1.5F, 1.5F);
            }
            draw(generation, "launcher_port/" + missile.legacyId(), state,
                    pose, buffers, light, overlay);
        } else {
            drawCustomPart(generation, state, pose, buffers, light, overlay, missile.thrusterId());
            pose.translate(0.0F, missile.thrusterHeight(), 0.0F);
            drawCustomPart(generation, state, pose, buffers, light, overlay, missile.finsId());
            drawCustomPart(generation, state, pose, buffers, light, overlay, missile.fuselageId());
            pose.translate(0.0F, missile.fuselageHeight(), 0.0F);
            drawCustomPart(generation, state, pose, buffers, light, overlay, missile.warheadId());
        }
        pose.popPose();
    }

    private static void drawCustomPart(GeometryGeneration generation, BlockState state,
                                       PoseStack pose, MultiBufferSource buffers,
                                       int light, int overlay, String id) {
        CapturedModel model = generation.model(MissileMultipartRenderer.model(id), state);
        if (model != null) {
            model.render(pose, buffers, light, overlay);
        }
    }

    /** Exact current configuration, with the legal pad motion retained as a stable temporal union. */
    private static AABB buildBounds(BlockPos pos, LauncherBlockEntity.RenderSnapshot state,
                                    GeometryGeneration generation) {
        Bounds bounds = new Bounds(pos);
        switch (state.kind()) {
            case PAD_SMALL -> {
                PoseStack root = new PoseStack();
                applySmallLaunchPadPose(root, state.facing());
                include(generation, state.blockState(), bounds, root, "launch_pad_world");
                PoseStack missilePose = copy(root);
                missilePose.translate(0.0F, 1.0F, 0.0F);
                includeMissile(generation, state.blockState(), bounds, missilePose, state.missile());
            }
            case PAD_RUSTED -> {
                PoseStack root = new PoseStack();
                applyRustedLaunchPadPose(root, state.facing());
                include(generation, state.blockState(), bounds, root, "launch_pad_rusted_world");
                PoseStack missilePose = copy(root);
                missilePose.translate(0.0F, 1.0F, 0.0F);
                includeMissile(generation, state.blockState(), bounds, missilePose, state.missile());
            }
            case COMPACT -> {
                PoseStack root = new PoseStack();
                applyCompactLauncherPose(root);
                root.translate(0.0D, DEPTH_EPSILON, 0.0D);
                include(generation, state.blockState(), bounds, root, "compact_launcher_world");
                if (state.validMissile()) {
                    root.translate(0.0F, 1.0625F, 0.0F);
                    includeMissile(generation, state.blockState(), bounds, root, state.missile());
                }
            }
            case PAD_LARGE -> {
                PoseStack root = new PoseStack();
                applyLargeLaunchPadPose(root, state.facing());
                includeLarge(generation, bounds, root, state);
            }
            case TABLE -> {
                PoseStack root = new PoseStack();
                applyLaunchTablePose(root, state.facing());
                includeTable(generation, bounds, root, state);
            }
            case SOYUZ -> { }
        }
        return bounds.toAabb();
    }

    private static void applySmallLaunchPadPose(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Small launch pad requires a horizontal facing: " + facing);
        }
    }

    private static void applyRustedLaunchPadPose(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Rusted launch pad requires a horizontal facing: " + facing);
        }
    }

    private static void applyCompactLauncherPose(PoseStack pose) {
        pose.translate(0.5F, 0.0F, 0.5F);
    }

    private static void applyLargeLaunchPadPose(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Large launch pad requires a horizontal facing: " + facing);
        }
    }

    private static void applyLaunchTablePose(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(0.0F));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Launch table requires a horizontal facing: " + facing);
        }
    }

    private static void includeLarge(GeometryGeneration generation, Bounds bounds,
                                     PoseStack root, LauncherBlockEntity.RenderSnapshot state) {
        BlockState blockState = state.blockState();
        PoseStack base = copy(root);
        base.translate(0.0D, DEPTH_EPSILON, 0.0D);
        include(generation, blockState, bounds, base, "launcher_port/pad_base");
        if (state.formFactor() < 0 || state.formFactor() >= LegacyMissileItem.FormFactor.values().length) {
            return;
        }
        LegacyMissileItem.FormFactor factor = LegacyMissileItem.FormFactor.values()[state.formFactor()];
        String prefix = switch (factor) {
            case ABM, OTHER -> "abm";
            case MICRO -> "micro";
            case V2 -> "v2";
            case STRONG -> "strong";
            case HUGE -> "huge";
            case ATLAS -> "atlas";
        };
        float pivotZ = switch (factor) {
            case ABM, MICRO, OTHER -> -1.5F;
            case V2 -> -1.75F;
            case STRONG, HUGE -> -3.0F;
            case ATLAS -> -4.0F;
        };
        float pivotY = factor == LegacyMissileItem.FormFactor.ABM
                || factor == LegacyMissileItem.FormFactor.MICRO
                || factor == LegacyMissileItem.FormFactor.V2
                || factor == LegacyMissileItem.FormFactor.OTHER ? 1.25F : 1.5F;

        PoseStack factorPad = copy(base);
        factorPad.translate(0.0D, DEPTH_EPSILON, 0.0D);
        include(generation, blockState, bounds, factorPad, "launcher_port/" + prefix + "_pad");
        if (state.missile().present()) {
            include(generation, blockState, bounds, base, "launcher_port/" + prefix + "_rope");
        }

        CapturedModel pivot = generation.model(model("launcher_port/" + prefix + "_pivot"), blockState);
        CapturedModel erector = generation.model(model("launcher_port/" + prefix + "_erector"), blockState);
        if (pivot != null) {
            pivot.geometry().includeSweptNegativeX(bounds, base, pivotY, pivotZ, 0.0F, 90.0F, 0.0F, 1.0F);
        }
        if (erector != null) {
            // Erector rotation occurs with lift=1; lift then travels 1..0 at angle zero.
            erector.geometry().includeSweptNegativeX(bounds, base, pivotY, pivotZ, 0.0F, 90.0F, 1.0F, 1.0F);
            erector.geometry().includeAtNegativeX(bounds, base, pivotY, pivotZ, 0.0F, 0.0F, 1.0F, 1.0F);
        }
        if (state.missile().present()) {
            includeMissileSwept(generation, blockState, bounds, base, state.missile(), pivotY, pivotZ, 3.0F);
            includeMissileAtNegativeX(generation, blockState, bounds, base, state.missile(), pivotY, pivotZ,
                    0.0F, 2.0F, 3.0F);
            PoseStack erected = copy(base);
            erected.translate(0.0F, 2.0F, 0.0F);
            includeMissile(generation, blockState, bounds, erected, state.missile());
        }
    }

    private static void includeTable(GeometryGeneration generation, Bounds bounds,
                                     PoseStack root, LauncherBlockEntity.RenderSnapshot state) {
        BlockState blockState = state.blockState();
        PoseStack tableLocal = new PoseStack();
        includeVbo(generation, blockState, bounds, tableLocal, root, "launch_table_base");
        includeVbo(generation, blockState, bounds, tableLocal, root,
                state.tableSize() == MissilePartItem.Size.SIZE_20
                ? "launch_table_large_pad" : "launch_table_small_pad");
        int connector = connectorIndex(state.scaffoldHeight());
        boolean small = state.tableSize() == MissilePartItem.Size.SIZE_10;
        PoseStack scaffold = new PoseStack();
        if (small) {
            scaffold.translate(0.0F, 0.0F, -1.0F);
        }
        scaffold.translate(0.0F, 1.0F, 3.5F);
        String prefix = small ? "launch_table_small_scaffold_" : "launch_table_large_scaffold_";
        for (int i = 0; i <= state.scaffoldHeight(); i++) {
            String part = i < connector ? "base" : i > connector ? "empty"
                    : state.validMissile() ? "connector" : "base";
            includeVbo(generation, blockState, bounds, scaffold, root, prefix + part);
            scaffold.translate(0.0F, 1.0F, 0.0F);
        }
        if (state.validMissile()) {
            PoseStack missile = copy(root);
            missile.translate(0.0F, 2.0625F, 0.0F);
            includeMissile(generation, blockState, bounds, missile, state.missile());
        }
    }

    private static void includeMissile(GeometryGeneration generation, BlockState state, Bounds bounds,
                                       PoseStack pose, LauncherBlockEntity.MissileRenderData missile) {
        if (!missile.present()) {
            return;
        }
        if (!missile.custom()) {
            PoseStack scaled = copy(pose);
            if (missile.legacyTierTwo()) {
                scaled.scale(1.5F, 1.5F, 1.5F);
            }
            include(generation, state, bounds, scaled, "launcher_port/" + missile.legacyId());
            return;
        }
        includeCustomPart(generation, state, bounds, pose, missile.thrusterId());
        PoseStack upper = copy(pose);
        upper.translate(0.0F, missile.thrusterHeight(), 0.0F);
        includeCustomPart(generation, state, bounds, upper, missile.finsId());
        includeCustomPart(generation, state, bounds, upper, missile.fuselageId());
        upper.translate(0.0F, missile.fuselageHeight(), 0.0F);
        includeCustomPart(generation, state, bounds, upper, missile.warheadId());
    }

    private static void includeMissileSwept(GeometryGeneration generation, BlockState state, Bounds bounds,
                                            PoseStack root, LauncherBlockEntity.MissileRenderData missile,
                                            float pivotY, float pivotZ, float translatedY) {
        if (!missile.custom()) {
            CapturedModel captured = generation.model(model("launcher_port/" + missile.legacyId()), state);
            if (captured != null) {
                captured.geometry().includeSweptNegativeX(bounds, root, pivotY, pivotZ, 0.0F, 90.0F,
                        translatedY, missile.legacyTierTwo() ? 1.5F : 1.0F);
            }
            return;
        }
        includeCustomPartSwept(generation, state, bounds, root, missile.thrusterId(), pivotY, pivotZ, translatedY);
        float upper = translatedY + missile.thrusterHeight();
        includeCustomPartSwept(generation, state, bounds, root, missile.finsId(), pivotY, pivotZ, upper);
        includeCustomPartSwept(generation, state, bounds, root, missile.fuselageId(), pivotY, pivotZ, upper);
        includeCustomPartSwept(generation, state, bounds, root, missile.warheadId(), pivotY, pivotZ,
                upper + missile.fuselageHeight());
    }

    private static void includeMissileAtNegativeX(GeometryGeneration generation, BlockState state,
                                                   Bounds bounds, PoseStack root,
                                                   LauncherBlockEntity.MissileRenderData missile,
                                                   float pivotY, float pivotZ, float angle,
                                                   float minTranslatedY, float maxTranslatedY) {
        if (!missile.custom()) {
            CapturedModel captured = generation.model(model("launcher_port/" + missile.legacyId()), state);
            if (captured != null) {
                captured.geometry().includeAtNegativeX(bounds, root, pivotY, pivotZ, angle,
                        minTranslatedY, maxTranslatedY, missile.legacyTierTwo() ? 1.5F : 1.0F);
            }
            return;
        }
        includeCustomPartAt(generation, state, bounds, root, missile.thrusterId(), pivotY, pivotZ, angle,
                minTranslatedY, maxTranslatedY);
        float upperMin = minTranslatedY + missile.thrusterHeight();
        float upperMax = maxTranslatedY + missile.thrusterHeight();
        includeCustomPartAt(generation, state, bounds, root, missile.finsId(), pivotY, pivotZ, angle,
                upperMin, upperMax);
        includeCustomPartAt(generation, state, bounds, root, missile.fuselageId(), pivotY, pivotZ, angle,
                upperMin, upperMax);
        includeCustomPartAt(generation, state, bounds, root, missile.warheadId(), pivotY, pivotZ, angle,
                upperMin + missile.fuselageHeight(), upperMax + missile.fuselageHeight());
    }

    private static void includeCustomPart(GeometryGeneration generation, BlockState state, Bounds bounds,
                                          PoseStack pose, String id) {
        CapturedModel captured = generation.model(MissileMultipartRenderer.model(id), state);
        if (captured != null) {
            captured.geometry().includeTransformed(bounds, pose);
        }
    }

    private static void includeCustomPartSwept(GeometryGeneration generation, BlockState state,
                                               Bounds bounds, PoseStack root, String id,
                                               float pivotY, float pivotZ, float translatedY) {
        CapturedModel captured = generation.model(MissileMultipartRenderer.model(id), state);
        if (captured != null) {
            captured.geometry().includeSweptNegativeX(bounds, root, pivotY, pivotZ,
                    0.0F, 90.0F, translatedY, 1.0F);
        }
    }

    private static void includeCustomPartAt(GeometryGeneration generation, BlockState state,
                                            Bounds bounds, PoseStack root, String id,
                                            float pivotY, float pivotZ, float angle,
                                            float minTranslatedY, float maxTranslatedY) {
        CapturedModel captured = generation.model(MissileMultipartRenderer.model(id), state);
        if (captured != null) {
            captured.geometry().includeAtNegativeX(bounds, root, pivotY, pivotZ, angle,
                    minTranslatedY, maxTranslatedY, 1.0F);
        }
    }

    private static void include(GeometryGeneration generation, BlockState state, Bounds bounds,
                                PoseStack pose, String path) {
        CapturedModel captured = generation.model(model(path), state);
        if (captured != null) {
            captured.geometry().includeTransformed(bounds, pose);
        }
    }

    private static void includeVbo(GeometryGeneration generation, BlockState state, Bounds bounds,
                                   PoseStack localPose, PoseStack runtimePose, String path) {
        CapturedModel captured = generation.model(model(path), state);
        if (captured != null) {
            captured.geometry().includeVboTransformed(bounds, localPose, runtimePose);
        }
    }

    private static PoseStack copy(PoseStack source) {
        PoseStack copy = new PoseStack();
        copy.mulPose(new Matrix4f(source.last().pose()));
        return copy;
    }

    private static int connectorIndex(int scaffoldHeight) {
        return (int) Math.floor(scaffoldHeight * 0.75D);
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone("block/" + path);
    }

    private static Set<ModelResourceLocation> allModels() {
        Set<ModelResourceLocation> models = new LinkedHashSet<>();
        for (String path : List.of("launch_pad_world", "launch_pad_rusted_world", "compact_launcher_world",
                "launch_table_base", "launch_table_small_pad", "launch_table_large_pad",
                "launch_table_small_scaffold_base", "launch_table_small_scaffold_connector",
                "launch_table_small_scaffold_empty", "launch_table_large_scaffold_base",
                "launch_table_large_scaffold_connector", "launch_table_large_scaffold_empty",
                "launcher_port/pad_base")) {
            models.add(model(path));
        }
        for (String factor : List.of("abm", "micro", "v2", "strong", "huge", "atlas")) {
            for (String part : List.of("pad", "erector", "pivot", "rope")) {
                models.add(model("launcher_port/" + factor + "_" + part));
            }
        }
        for (String id : LauncherMissileRenderer.IDS) {
            models.add(model("launcher_port/" + id));
        }
        for (String id : MissileMultipartRenderer.partIds()) {
            models.add(MissileMultipartRenderer.model(id));
        }
        return models;
    }

    private static Set<BlockState> allRenderStates() {
        Set<BlockState> states = Collections.newSetFromMap(new IdentityHashMap<>());
        states.add(ITEM_RENDER_STATE);
        for (var block : List.of(HbmBlocks.LAUNCH_PAD.get(), HbmBlocks.LAUNCH_PAD_RUSTED.get(),
                HbmBlocks.LAUNCH_PAD_LARGE.get(), HbmBlocks.COMPACT_LAUNCHER.get(),
                HbmBlocks.LAUNCH_TABLE.get())) {
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        return states;
    }

    /** Captures the exact immutable layers/quads and their face-used float vertices. */
    private static CapturedModel captureModel(ModelResourceLocation location, BlockState state,
                                              Map<List<CapturedLayer>, CapturedModel> canonicalModels) {
        BakedModel model = MachineModelRenderer.model(location);
        RandomSource random = RandomSource.create(42L);
        List<CapturedLayer> layers = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType sourceRenderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, model, state, null, sourceRenderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, model, state, side, sourceRenderType, random);
            }
            if (!quads.isEmpty()) {
                layers.add(new CapturedLayer(
                        RenderTypeHelper.getEntityRenderType(sourceRenderType, false),
                        List.copyOf(quads)));
            }
        }
        List<CapturedLayer> immutableLayers = List.copyOf(layers);
        CapturedModel canonical = canonicalModels.get(immutableLayers);
        if (canonical != null) {
            return canonical;
        }
        List<Float> coordinates = new ArrayList<>();
        for (CapturedLayer layer : immutableLayers) {
            collectVertices(coordinates, layer.quads());
        }
        float[] vertices = new float[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            vertices[i] = coordinates.get(i);
        }
        CapturedModel captured = new CapturedModel(immutableLayers, new GeometryTemplate(vertices));
        canonicalModels.put(immutableLayers, captured);
        return captured;
    }

    private static void collectQuads(List<BakedQuad> quads, BakedModel model, BlockState state, Direction side,
                                     RenderType renderType, RandomSource random) {
        random.setSeed(42L);
        quads.addAll(model.getQuads(state, side, random, ModelData.EMPTY, renderType));
    }

    private static void collectVertices(List<Float> coordinates, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                coordinates.add(Float.intBitsToFloat(data[offset]));
                coordinates.add(Float.intBitsToFloat(data[offset + 1]));
                coordinates.add(Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }

    private static List<GpuLayer> buildTableLayers(GeometryGeneration generation,
                                                    MissilePartItem.Size tableSize,
                                                    int scaffoldHeight, int connector,
                                                    boolean validMissile, BlockState state,
                                                    int packedLight, int packedOverlay) {
        List<Placement> placements = new ArrayList<>();
        addPlacement(placements, generation.model(model("launch_table_base"), state), new PoseStack());
        addPlacement(placements, generation.model(model(tableSize == MissilePartItem.Size.SIZE_20
                ? "launch_table_large_pad" : "launch_table_small_pad"), state), new PoseStack());

        boolean small = tableSize == MissilePartItem.Size.SIZE_10;
        String prefix = small ? "launch_table_small_scaffold_" : "launch_table_large_scaffold_";
        PoseStack scaffold = new PoseStack();
        if (small) {
            scaffold.translate(0.0D, 0.0D, -1.0D);
        }
        scaffold.translate(0.0D, 1.0D, 3.5D);
        for (int index = 0; index <= scaffoldHeight; index++) {
            String part = index < connector ? "base" : index > connector ? "empty"
                    : validMissile ? "connector" : "base";
            addPlacement(placements, generation.model(model(prefix + part), state), scaffold);
            scaffold.translate(0.0D, 1.0D, 0.0D);
        }

        Set<RenderType> renderTypes = new LinkedHashSet<>();
        for (Placement placement : placements) {
            for (CapturedLayer layer : placement.model().layers()) {
                renderTypes.add(layer.renderType());
            }
        }
        List<GpuLayer> built = new ArrayList<>(renderTypes.size());
        try {
            for (RenderType renderType : renderTypes) {
                BufferBuilder builder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
                for (Placement placement : placements) {
                    PoseStack placementPose = new PoseStack();
                    placementPose.mulPose(new Matrix4f(placement.pose()));
                    for (CapturedLayer layer : placement.model().layers()) {
                        if (layer.renderType().equals(renderType)) {
                            MachineModelRenderer.renderCapturedQuads(
                                    layer.quads(), placementPose, builder, packedLight, packedOverlay);
                        }
                    }
                }
                MeshData mesh = builder.buildOrThrow();
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                try {
                    buffer.bind();
                    buffer.upload(mesh);
                } catch (RuntimeException exception) {
                    buffer.close();
                    throw exception;
                } finally {
                    VertexBuffer.unbind();
                }
                built.add(new GpuLayer(renderType, buffer));
            }
            return List.copyOf(built);
        } catch (RuntimeException exception) {
            closeLayersNow(built);
            throw exception;
        }
    }

    private static void addPlacement(List<Placement> placements, CapturedModel model, PoseStack pose) {
        if (model != null && model.hasGeometry()) {
            placements.add(new Placement(model, new Matrix4f(pose.last().pose())));
        }
    }

    private static void drawLayers(List<GpuLayer> layers, PoseStack pose) {
        for (GpuLayer layer : layers) {
            if (layer.buffer().isInvalid()) {
                continue;
            }
            layer.renderType().setupRenderState();
            try {
                layer.buffer().bind();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    layer.buffer().drawWithShader(MachineModelRenderer.composeModelView(pose),
                            RenderSystem.getProjectionMatrix(), shader);
                }
            } finally {
                VertexBuffer.unbind();
                layer.renderType().clearRenderState();
            }
        }
    }

    private static void discardMachineCache(LauncherBlockEntity launcher) {
        runOnRenderThread(() -> {
            List<LauncherBlockEntityRenderer> renderers;
            synchronized (INSTANCES) {
                renderers = List.copyOf(INSTANCES);
            }
            for (LauncherBlockEntityRenderer renderer : renderers) {
                TableBuffers removed = renderer.tableBuffers.remove(launcher);
                if (removed != null) {
                    removed.closeNow();
                }
            }
        });
    }

    private static void closeAllGpuCaches() {
        runOnRenderThread(() -> {
            List<LauncherBlockEntityRenderer> renderers;
            synchronized (INSTANCES) {
                renderers = List.copyOf(INSTANCES);
            }
            for (LauncherBlockEntityRenderer renderer : renderers) {
                for (TableBuffers buffers : renderer.tableBuffers.values()) {
                    buffers.closeNow();
                }
                renderer.tableBuffers.clear();
            }
        });
    }

    private static void runOnRenderThread(Runnable action) {
        if (RenderSystem.isOnRenderThread()) {
            action.run();
        } else {
            RenderSystem.recordRenderCall(action::run);
        }
    }

    private static void closeLayersNow(List<GpuLayer> layers) {
        if (layers == null) {
            return;
        }
        for (GpuLayer layer : layers) {
            layer.buffer().close();
        }
    }

    private static AABB zeroVolumeBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    private record GeometryGeneration(int generation,
                                      Map<BlockState, Map<ModelResourceLocation, CapturedModel>> modelsByState) {
        private CapturedModel model(ModelResourceLocation location, BlockState state) {
            Map<ModelResourceLocation, CapturedModel> models = modelsByState.get(state);
            return models == null ? null : models.get(location);
        }
    }

    private record CapturedModel(List<CapturedLayer> layers, GeometryTemplate geometry) {
        private boolean hasGeometry() {
            return geometry.vertices().length != 0;
        }

        private void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            for (CapturedLayer layer : layers) {
                MachineModelRenderer.renderCapturedQuads(layer.quads(), pose,
                        buffers.getBuffer(layer.renderType()), light, overlay);
            }
        }
    }

    private record CapturedLayer(RenderType renderType, List<BakedQuad> quads) {
    }

    private record Placement(CapturedModel model, Matrix4f pose) {
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private static final class TableBuffers {
        private GeometryGeneration generation;
        private MissilePartItem.Size tableSize;
        private int scaffoldHeight;
        private int connector;
        private boolean validMissile;
        private BlockState state;
        private int packedLight;
        private int packedOverlay;
        private List<GpuLayer> layers;

        private void prepare(GeometryGeneration requestedGeneration,
                             MissilePartItem.Size requestedTableSize,
                             int requestedScaffoldHeight, int requestedConnector,
                             boolean requestedValidMissile, BlockState requestedState,
                             int requestedLight, int requestedOverlay) {
            if (layers != null
                    && generation == requestedGeneration
                    && tableSize == requestedTableSize
                    && scaffoldHeight == requestedScaffoldHeight
                    && connector == requestedConnector
                    && validMissile == requestedValidMissile
                    && state == requestedState
                    && packedLight == requestedLight
                    && packedOverlay == requestedOverlay) {
                return;
            }
            closeNow();
            generation = requestedGeneration;
            tableSize = requestedTableSize;
            scaffoldHeight = requestedScaffoldHeight;
            connector = requestedConnector;
            validMissile = requestedValidMissile;
            state = requestedState;
            packedLight = requestedLight;
            packedOverlay = requestedOverlay;
            layers = buildTableLayers(requestedGeneration, requestedTableSize,
                    requestedScaffoldHeight, requestedConnector, requestedValidMissile,
                    requestedState, requestedLight, requestedOverlay);
        }

        private List<GpuLayer> layers() {
            return layers == null ? List.of() : layers;
        }

        private void closeNow() {
            closeLayersNow(layers);
            layers = null;
            generation = null;
            tableSize = null;
            state = null;
        }
    }

    private record GeometryTemplate(float[] vertices) {
        private static final float DEG = 0.017453292F;
        private static final double DEG_ERROR = Math.abs((double) DEG - Math.PI / 180.0D);
        private static final double FLOAT_UNIT_ROUNDOFF = 0x1.0p-24;
        /*
         * Upper bound on the rounded single-precision operations on one
         * coordinate: degree/radian + half-angle and trig reconstruction (8),
         * quaternion coefficients (18), affine postmultiply (27), the two
         * pivot translations (24), and the final three-FMA position transform
         * (4), rounded upward to 96.  gamma(96) therefore also covers JOML's
         * implementation choices without an empirical epsilon.
         */
        private static final int FLOAT_PIPELINE_OPERATIONS = 96;
        private static final double FLOAT_PIPELINE_GAMMA =
                FLOAT_PIPELINE_OPERATIONS * FLOAT_UNIT_ROUNDOFF
                        / (1.0D - FLOAT_PIPELINE_OPERATIONS * FLOAT_UNIT_ROUNDOFF);

        private void includeTransformed(Bounds bounds, PoseStack pose) {
            Matrix4f matrix = pose.last().pose();
            for (int i = 0; i < vertices.length; i += 3) {
                bounds.include(matrix.transformPosition(
                        new Vector3f(vertices[i], vertices[i + 1], vertices[i + 2])));
            }
        }

        /** Mirrors table VBO upload first, followed by the per-frame model-view transform. */
        private void includeVboTransformed(Bounds bounds, PoseStack localPose, PoseStack runtimePose) {
            Matrix4f local = localPose.last().pose();
            Matrix4f runtime = runtimePose.last().pose();
            for (int i = 0; i < vertices.length; i += 3) {
                Vector3f uploaded = local.transformPosition(
                        new Vector3f(vertices[i], vertices[i + 1], vertices[i + 2]));
                bounds.include(runtime.transformPosition(uploaded));
            }
        }

        private void includeSweptNegativeX(Bounds bounds, PoseStack root,
                                           float pivotY, float pivotZ,
                                           float minAngle, float maxAngle,
                                           float translatedY, float scale) {
            for (int i = 0; i < vertices.length; i += 3) {
                float x = vertices[i] * scale;
                float y = vertices[i + 1] * scale;
                float z = vertices[i + 2] * scale;
                float[] candidates = criticalAngles(y + translatedY - pivotY, z - pivotZ,
                        minAngle, maxAngle);
                for (float angle : candidates) {
                    includeSweptVertex(bounds, root, x, y, z, pivotY, pivotZ, angle, translatedY);
                }
            }
        }

        private void includeAtNegativeX(Bounds bounds, PoseStack root,
                                        float pivotY, float pivotZ, float angle,
                                        float minTranslatedY, float maxTranslatedY, float scale) {
            for (int i = 0; i < vertices.length; i += 3) {
                float x = vertices[i] * scale;
                float y = vertices[i + 1] * scale;
                float z = vertices[i + 2] * scale;
                includeVertex(bounds, root, x, y, z, pivotY, pivotZ, angle, minTranslatedY);
                includeVertex(bounds, root, x, y, z, pivotY, pivotZ, angle, maxTranslatedY);
            }
        }

        private static void includeVertex(Bounds bounds, PoseStack root,
                                          float x, float y, float z,
                                          float pivotY, float pivotZ,
                                          float angle, float translatedY) {
            Matrix4f matrix = new Matrix4f(root.last().pose())
                    .translate(0.0F, pivotY, pivotZ)
                    .rotate(new Quaternionf().rotationX(-angle * DEG))
                    .translate(0.0F, -pivotY, -pivotZ)
                    .translate(0.0F, translatedY, 0.0F);
            bounds.include(matrix.transformPosition(new Vector3f(x, y, z)));
        }

        private static void includeSweptVertex(Bounds bounds, PoseStack root,
                                               float x, float y, float z,
                                               float pivotY, float pivotZ,
                                               float angle, float translatedY) {
            Matrix4f rootMatrix = root.last().pose();
            Matrix4f matrix = new Matrix4f(rootMatrix)
                    .translate(0.0F, pivotY, pivotZ)
                    .rotate(new Quaternionf().rotationX(-angle * DEG))
                    .translate(0.0F, -pivotY, -pivotZ)
                    .translate(0.0F, translatedY, 0.0F);
            Vector3f emitted = matrix.transformPosition(new Vector3f(x, y, z));

            double relativeY = (double) y + translatedY - pivotY;
            double relativeZ = (double) z - pivotZ;
            double radius = Math.hypot(relativeY, relativeZ);
            double localYMagnitude = Math.abs((double) pivotY) + radius;
            double localZMagnitude = Math.abs((double) pivotZ) + radius;
            double angleErrorRadians = Math.nextUp(
                    Math.ulp(angle) * Math.abs((double) DEG)
                            + Math.abs((double) angle) * DEG_ERROR);

            double errorX = sweptCoordinateError(rootMatrix.m00(), rootMatrix.m10(), rootMatrix.m20(),
                    rootMatrix.m30(), x, localYMagnitude, localZMagnitude, radius, angleErrorRadians);
            double errorY = sweptCoordinateError(rootMatrix.m01(), rootMatrix.m11(), rootMatrix.m21(),
                    rootMatrix.m31(), x, localYMagnitude, localZMagnitude, radius, angleErrorRadians);
            double errorZ = sweptCoordinateError(rootMatrix.m02(), rootMatrix.m12(), rootMatrix.m22(),
                    rootMatrix.m32(), x, localYMagnitude, localZMagnitude, radius, angleErrorRadians);
            bounds.includeOutward(emitted, errorX, errorY, errorZ);
        }

        private static double sweptCoordinateError(float coefficientX, float coefficientY, float coefficientZ,
                                                    float translation, float x,
                                                    double localYMagnitude, double localZMagnitude,
                                                    double radius, double angleErrorRadians) {
            double magnitude = Math.abs((double) coefficientX * x)
                    + Math.abs((double) coefficientY) * localYMagnitude
                    + Math.abs((double) coefficientZ) * localZMagnitude
                    + Math.abs((double) translation);
            double evaluationError = Math.nextUp(FLOAT_PIPELINE_GAMMA * magnitude);
            double criticalAngleError = Math.nextUp(angleErrorRadians * radius
                    * (Math.abs((double) coefficientY) + Math.abs((double) coefficientZ)));
            // One evaluation error for the unknown emitted maximum and one
            // for the candidate we evaluated around the ideal critical point.
            return Math.nextUp(2.0D * evaluationError + criticalAngleError);
        }

        private static float[] criticalAngles(float y, float z, float minAngle, float maxAngle) {
            // Rx(-a): Y=y*cos(a)+z*sin(a), Z=z*cos(a)-y*sin(a).
            List<Float> candidates = new ArrayList<>(10);
            candidates.add(minAngle);
            candidates.add(maxAngle);
            addCritical(candidates, y, z, minAngle, maxAngle);
            addCritical(candidates, z, -y, minAngle, maxAngle);
            float[] result = new float[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                result[i] = candidates.get(i);
            }
            return result;
        }

        private static void addCritical(List<Float> candidates, float cosine, float sine,
                                        float minAngle, float maxAngle) {
            double base = Math.toDegrees(Math.atan2(sine, cosine));
            for (double value : new double[] {base, base + 180.0D, base - 180.0D, base + 360.0D}) {
                if (value < minAngle || value > maxAngle) {
                    continue;
                }
                float angle = (float) value;
                candidates.add(angle);
                float down = Math.nextDown(angle);
                float up = Math.nextUp(angle);
                if (down >= minAngle) candidates.add(down);
                if (up <= maxAngle) candidates.add(up);
            }
        }
    }

    private static final class Bounds {
        private final BlockPos pos;
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private void include(Vector3f point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        private void includeOutward(Vector3f point, double errorX, double errorY, double errorZ) {
            minX = Math.min(minX, Math.nextDown((double) point.x - errorX));
            minY = Math.min(minY, Math.nextDown((double) point.y - errorY));
            minZ = Math.min(minZ, Math.nextDown((double) point.z - errorZ));
            maxX = Math.max(maxX, Math.nextUp((double) point.x + errorX));
            maxY = Math.max(maxY, Math.nextUp((double) point.y + errorY));
            maxZ = Math.max(maxZ, Math.nextUp((double) point.z + errorZ));
        }

        private AABB toAabb() {
            if (!Double.isFinite(minX)) {
                return zeroVolumeBounds(pos);
            }
            return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
        }
    }
}
