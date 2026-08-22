package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.BatterySocketBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

public class BatterySocketBlockEntityRenderer implements BlockEntityRenderer<BatterySocketBlockEntity> {
    private static final ModelResourceLocation SOCKET = MachineModelRenderer.standalone("block/machine_battery_socket_socket");
    private static final ModelResourceLocation SUPPORTS = MachineModelRenderer.standalone("block/machine_battery_socket_supports");
    private static final ModelResourceLocation BATTERY_REDSTONE = MachineModelRenderer.standalone("block/machine_battery_socket_battery_redstone");
    private static final ModelResourceLocation BATTERY_LEAD = MachineModelRenderer.standalone("block/machine_battery_socket_battery_lead");
    private static final ModelResourceLocation BATTERY_LITHIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_lithium");
    private static final ModelResourceLocation BATTERY_SODIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_sodium");
    private static final ModelResourceLocation BATTERY_SCHRABIDIUM = MachineModelRenderer.standalone("block/machine_battery_socket_battery_schrabidium");
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
            Map.entry("battery_quantum", BATTERY_QUANTUM),
            Map.entry("battery_creative", BATTERY_QUANTUM),
            Map.entry("capacitor_copper", CAPACITOR_COPPER),
            Map.entry("capacitor_gold", CAPACITOR_GOLD),
            Map.entry("capacitor_niobium", CAPACITOR_NIOBIUM),
            Map.entry("capacitor_tantalum", CAPACITOR_TANTALUM),
            Map.entry("capacitor_bismuth", CAPACITOR_BISMUTH),
            Map.entry("capacitor_spark", CAPACITOR_SPARK)
    );

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
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        poseStack.translate(-0.5F, 0.0F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOCKET), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (!socket.getLevel().getBlockState(socket.getBlockPos().above(2)).isAir()) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SUPPORTS), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        ItemStack stack = socket.batteryStack();
        ModelResourceLocation insert = INSERT_MODELS.get(BatteryPackItem.variantId(stack));
        if (insert != null) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(insert), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BatterySocketBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
