package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LargeFactoryItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float ITEM_TARGET_SIZE = 0.95F;
    private static final float CHEMICAL_FACTORY_GUI_TARGET_SIZE = 1.35F;
    private static final float ITEM_YAW = 90.0F;
    private static final RandomSource FIT_RANDOM = RandomSource.create();
    private static final Map<Block, ItemFit> FIT_CACHE = new IdentityHashMap<>();

    private static final ModelResourceLocation ASSEMBLY_BASE = MachineModelRenderer.standalone("block/machine_assembly_factory_base");
    private static final ModelResourceLocation ASSEMBLY_FRAME = MachineModelRenderer.standalone("block/machine_assembly_factory_frame");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider1");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider2");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider3");
    private static final ModelResourceLocation ASSEMBLY_SLIDER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_slider4");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower1");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower2");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower3");
    private static final ModelResourceLocation ASSEMBLY_ARM_LOWER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armlower4");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper1");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper2");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper3");
    private static final ModelResourceLocation ASSEMBLY_ARM_UPPER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_armupper4");
    private static final ModelResourceLocation ASSEMBLY_HEAD_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_head1");
    private static final ModelResourceLocation ASSEMBLY_HEAD_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_head2");
    private static final ModelResourceLocation ASSEMBLY_HEAD_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_head3");
    private static final ModelResourceLocation ASSEMBLY_HEAD_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_head4");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_1 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker1");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker2");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_3 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker3");
    private static final ModelResourceLocation ASSEMBLY_STRIKER_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_striker4");
    private static final ModelResourceLocation ASSEMBLY_BLADE_2 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade2");
    private static final ModelResourceLocation ASSEMBLY_BLADE_4 = MachineModelRenderer.standalone("block/machine_assembly_factory_blade4");

    private static final ModelResourceLocation CHEMICAL_ITEM = MachineModelRenderer.standalone("block/machine_chemical_factory_item");

    private static final FactoryItemParts ASSEMBLY_FACTORY = new FactoryItemParts(
            HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get(),
            0.0F,
            1.5F,
            0.0F,
            5.0F,
            ITEM_TARGET_SIZE,
            List.of(
                    ASSEMBLY_BASE,
                    ASSEMBLY_FRAME,
                    ASSEMBLY_SLIDER_1,
                    ASSEMBLY_SLIDER_2,
                    ASSEMBLY_SLIDER_3,
                    ASSEMBLY_SLIDER_4,
                    ASSEMBLY_ARM_LOWER_1,
                    ASSEMBLY_ARM_LOWER_2,
                    ASSEMBLY_ARM_LOWER_3,
                    ASSEMBLY_ARM_LOWER_4,
                    ASSEMBLY_ARM_UPPER_1,
                    ASSEMBLY_ARM_UPPER_2,
                    ASSEMBLY_ARM_UPPER_3,
                    ASSEMBLY_ARM_UPPER_4,
                    ASSEMBLY_HEAD_1,
                    ASSEMBLY_HEAD_2,
                    ASSEMBLY_HEAD_3,
                    ASSEMBLY_HEAD_4,
                    ASSEMBLY_STRIKER_1,
                    ASSEMBLY_STRIKER_2,
                    ASSEMBLY_STRIKER_3,
                    ASSEMBLY_STRIKER_4,
                    ASSEMBLY_BLADE_2,
                    ASSEMBLY_BLADE_4
            )
    );
    private static final FactoryItemParts CHEMICAL_FACTORY = new FactoryItemParts(
            HbmBlocks.MACHINE_CHEMICAL_FACTORY.get(),
            7.0F,
            4.5F,
            2.0F,
            19.0F,
            CHEMICAL_FACTORY_GUI_TARGET_SIZE,
            List.of(
                    CHEMICAL_ITEM
            )
    );

    public LargeFactoryItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ASSEMBLY_FACTORY.models.forEach(event::register);
        CHEMICAL_FACTORY.models.forEach(event::register);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FactoryItemParts parts = FactoryItemParts.forStack(stack);
        BlockState state = parts.block.defaultBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(ITEM_YAW), 0.0F, 1.0F, 0.0F)));
        ItemFit fit = fit(parts, state);
        float targetSize = context == ItemDisplayContext.GUI ? parts.guiTargetSize : ITEM_TARGET_SIZE;
        float scale = targetSize / fit.longestSide;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-fit.centerX, -fit.centerY, -fit.centerZ);
        for (ModelResourceLocation model : parts.models) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static ItemFit fit(FactoryItemParts parts, BlockState state) {
        return FIT_CACHE.computeIfAbsent(parts.block, ignored -> ItemFit.from(parts, state));
    }

    private record FactoryItemParts(Block block, float fallbackCenterX, float fallbackCenterY, float fallbackCenterZ, float fallbackLongestSide, float guiTargetSize, List<ModelResourceLocation> models) {
        private static FactoryItemParts forStack(ItemStack stack) {
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block == HbmBlocks.MACHINE_CHEMICAL_FACTORY.get()) {
                    return CHEMICAL_FACTORY;
                }
            }
            return ASSEMBLY_FACTORY;
        }

        private ItemFit fallbackFit() {
            return new ItemFit(this.fallbackCenterX, this.fallbackCenterY, this.fallbackCenterZ, this.fallbackLongestSide);
        }
    }

    private record ItemFit(float centerX, float centerY, float centerZ, float longestSide) {
        private static ItemFit from(FactoryItemParts parts, BlockState state) {
            Bounds bounds = new Bounds();
            for (ModelResourceLocation location : parts.models) {
                addModelBounds(bounds, MachineModelRenderer.model(location), state);
            }
            if (!bounds.valid()) {
                return parts.fallbackFit();
            }
            float sizeX = bounds.maxX - bounds.minX;
            float sizeY = bounds.maxY - bounds.minY;
            float sizeZ = bounds.maxZ - bounds.minZ;
            float longestSide = Math.max(sizeX, Math.max(sizeY, sizeZ));
            if (longestSide <= 0.0001F) {
                return parts.fallbackFit();
            }
            return new ItemFit(
                    (bounds.minX + bounds.maxX) * 0.5F,
                    (bounds.minY + bounds.maxY) * 0.5F,
                    (bounds.minZ + bounds.maxZ) * 0.5F,
                    longestSide
            );
        }

        private static void addModelBounds(Bounds bounds, BakedModel model, BlockState state) {
            FIT_RANDOM.setSeed(42L);
            bounds.add(model.getQuads(state, null, FIT_RANDOM));
            for (Direction side : Direction.values()) {
                FIT_RANDOM.setSeed(42L);
                bounds.add(model.getQuads(state, side, FIT_RANDOM));
            }
        }
    }

    private static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void add(List<BakedQuad> quads) {
            for (BakedQuad quad : quads) {
                add(quad);
            }
        }

        private void add(BakedQuad quad) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                add(
                        Float.intBitsToFloat(vertices[offset]),
                        Float.intBitsToFloat(vertices[offset + 1]),
                        Float.intBitsToFloat(vertices[offset + 2])
                );
            }
        }

        private void add(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private boolean valid() {
            return Float.isFinite(this.minX) && Float.isFinite(this.minY) && Float.isFinite(this.minZ)
                    && Float.isFinite(this.maxX) && Float.isFinite(this.maxY) && Float.isFinite(this.maxZ);
        }
    }
}
