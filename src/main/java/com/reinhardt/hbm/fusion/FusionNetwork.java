package com.reinhardt.hbm.fusion;

import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class FusionNetwork {
    private FusionNetwork() {
    }

    public static boolean provideKlystron(Level level, FusionMachineBlockEntity provider, long output) {
        if (output <= 0L) {
            return false;
        }
        boolean connected = false;
        for (Node node : provider.klystronProviderNodes()) {
            FusionMachineBlockEntity receiver = connectedMachine(level, node);
            if (receiver != null && receiver.acceptsKlystronNode(node)) {
                receiver.receiveKlystronEnergy(output);
                connected = true;
                break;
            }
        }
        return connected;
    }

    public static List<FusionMachineBlockEntity> connectedPlasmaReceivers(Level level, FusionMachineBlockEntity provider) {
        ArrayList<FusionMachineBlockEntity> receivers = new ArrayList<>();
        for (Node node : provider.plasmaProviderNodes()) {
            FusionMachineBlockEntity receiver = connectedMachine(level, node);
            if (receiver != null && receiver.acceptsPlasmaNode(node)) {
                receivers.add(receiver);
            }
        }
        return List.copyOf(receivers);
    }

    public static boolean providePlasma(Level level, FusionMachineBlockEntity provider, long fusionPower, double neutronPower, float r, float g, float b) {
        boolean connected = false;
        for (FusionMachineBlockEntity receiver : connectedPlasmaReceivers(level, provider)) {
            receiver.receiveFusionPower(fusionPower, neutronPower, r, g, b);
            connected = true;
        }
        return connected;
    }

    public static boolean hasConnectedKlystronProvider(Level level, FusionMachineBlockEntity receiver, Node receiverNode) {
        FusionMachineBlockEntity provider = connectedMachineAtConnector(level, receiverNode);
        return provider != null
                && provider != receiver
                && provider.klystronProviderNodes().stream().anyMatch(receiverNode::connectsTo);
    }

    public static boolean hasConnectedPlasmaReceiver(Level level, FusionMachineBlockEntity provider, Node providerNode) {
        FusionMachineBlockEntity receiver = connectedMachine(level, providerNode);
        return receiver != null
                && receiver != provider
                && receiver.acceptsPlasmaNode(providerNode);
    }

    private static FusionMachineBlockEntity connectedMachine(Level level, Node providerNode) {
        BlockPos receiverNodePos = providerNode.connectorPos();
        BlockEntity nodeEntity = level.getBlockEntity(receiverNodePos);
        if (nodeEntity instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof FusionMachineBlockEntity fusion) {
            return fusion;
        }
        if (nodeEntity instanceof FusionMachineBlockEntity fusion) {
            return fusion;
        }
        return null;
    }

    private static FusionMachineBlockEntity connectedMachineAtConnector(Level level, Node receiverNode) {
        BlockEntity nodeEntity = level.getBlockEntity(receiverNode.connectorPos());
        if (nodeEntity instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof FusionMachineBlockEntity fusion) {
            return fusion;
        }
        if (nodeEntity instanceof FusionMachineBlockEntity fusion) {
            return fusion;
        }
        return null;
    }

    public record Node(BlockPos nodePos, Direction face) {
        public BlockPos connectorPos() {
            return this.nodePos.relative(this.face);
        }

        public boolean connectsTo(Node provider) {
            return this.face == provider.face().getOpposite()
                    && this.nodePos.equals(provider.connectorPos())
                    && this.connectorPos().equals(provider.nodePos());
        }
    }
}
