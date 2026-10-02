package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.BatterySocketBlock;
import com.reinhardt.hbm.blockentity.BatterySocketBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

public class BatterySocketBlockEntityRenderer implements LongRangeBlockEntityRenderer<BatterySocketBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation SOCKET = MachineModelRenderer.standalone("block/machine_battery_socket_socket");
    private static final ModelResourceLocation SUPPORTS = MachineModelRenderer.standalone("block/machine_battery_socket_supports");
    private static final ModelResourceLocation BATTERY_REDSTONE = MachineModelRenderer.standalone("block/machine_battery_socket_battery_redstone");
    private static final ModelResourceLocation BATTERY_LEAD = MachineModelRenderer.standalone("block/machine_battery_socket_battery_lead");
    private static final ModelResourceLocation BATTERY_LITHIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_lithium");
    private static final ModelResourceLocation BATTERY_SODIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_sodium");
    private static final ModelResourceLocation BATTERY_SCHRABIDIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_schrabidium");
    private static final ModelResourceLocation BATTERY_SC = MachineModelRenderer.standalone("block/machine_battery_socket_battery_sc");
    private static final ModelResourceLocation BATTERY_QUANTUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_quantum");
    private static final ModelResourceLocation CAPACITOR_COPPER = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_copper");
    private static final ModelResourceLocation CAPACITOR_GOLD = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_gold");
    private static final ModelResourceLocation CAPACITOR_NIOBIUM = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_niobium");
    private static final ModelResourceLocation CAPACITOR_TANTALUM = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_tantalum");
    private static final ModelResourceLocation CAPACITOR_BISMUTH = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_bismuth");
    private static final ModelResourceLocation CAPACITOR_SPARK = MachineModelRenderer.standalone("block/machine_battery_socket_capacitor_spark");
    private static final Map<String, ModelResourceLocation> INSERT_MODELS = Map.ofEntries(
            Map.entry("battery_redstone", BATTERY_REDSTONE),
            Map.entry("battery_lead", BATTERY_LEAD),
            Map.entry("battery_lithium", BATTERY_LITHIUM),
            Map.entry("battery_sodium", BATTERY_SODIUM),
            Map.entry("battery_schrabidium", BATTERY_SCHRABIDIUM),
            Map.entry("battery_sc", BATTERY_SC),
            Map.entry("battery_quantum", BATTERY_QUANTUM),
            Map.entry("battery_creative", BATTERY_QUANTUM),
            Map.entry("capacitor_copper", CAPACITOR_COPPER),
            Map.entry("capacitor_gold", CAPACITOR_GOLD),
            Map.entry("capacitor_niobium", CAPACITOR_NIOBIUM),
            Map.entry("capacitor_tantalum", CAPACITOR_TANTALUM),
            Map.entry("capacitor_bismuth", CAPACITOR_BISMUTH),
            Map.entry("capacitor_spark", CAPACITOR_SPARK)
    );
    private static final AABB[] LOCAL_BOUNDS = buildBatterySocketBounds();

    public BatterySocketBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOCKET);
        event.register(SUPPORTS);
        for (ModelResourceLocation location : INSERT_MODELS.values()) {
            event.register(location);
        }
    }

    @Override
    public void render(BatterySocketBlockEntity socket, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = socket.getBlockState();
        Direction facing = batterySocketFacing(state);
        poseStack.pushPose();
        applyBatterySocketTransform(poseStack, facing);
        STATIC_MESHES.render(SOCKET, poseStack, state, packedLight, packedOverlay);
        if (socket.renderNeighborSnapshot().supportsVisible()) {
            STATIC_MESHES.render(SUPPORTS, poseStack, state, packedLight, packedOverlay);
        }
        ItemStack stack = socket.batteryStack();
        ModelResourceLocation insert = INSERT_MODELS.get(BatteryPackItem.variantId(stack));
        if (insert != null) {
            STATIC_MESHES.render(insert, poseStack, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BatterySocketBlockEntity blockEntity) {
        Direction facing = batterySocketFacing(blockEntity.getBlockState());
        // Socket/support face bounds contain every insert variant.
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildBatterySocketBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyBatterySocketTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F);
        }
        return bounds;
    }

    private static void applyBatterySocketTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Battery socket facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, 0.5F);
    }

    private static Direction batterySocketFacing(BlockState state) {
        if (!state.hasProperty(BatterySocketBlock.FACING)) {
            throw new IllegalStateException("Battery socket state is missing its owned facing property: " + state);
        }
        return state.getValue(BatterySocketBlock.FACING);
    }
}
