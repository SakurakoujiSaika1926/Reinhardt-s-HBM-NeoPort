package com.reinhardt.hbm.capability;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Forge Energy view of an HBM power endpoint. This is the 1.21 equivalent of
 * 1.12's NTMEnergyCapabilityWrapper: no converter block is involved.
 */
public final class NativeEnergyStorage implements IEnergyStorage {
    private final Level level;
    private final BlockPos hostPos;
    private final BlockPos connectorPos;
    @Nullable
    private final Direction side;

    public NativeEnergyStorage(Level level, BlockPos hostPos, @Nullable Direction side) {
        this.level = level;
        this.hostPos = hostPos.immutable();
        this.connectorPos = side == null ? this.hostPos : this.hostPos.relative(side).immutable();
        this.side = side;
    }

    @Nullable
    private PowerEndpoint endpoint() {
        return PowerNetworkManager.endpointAt(level, hostPos);
    }

    private boolean connected(PowerEndpoint endpoint) {
        return side == null || endpoint.canConnectPower(level, connectorPos, side.getOpposite());
    }

    private double rate() {
        return HbmConfig.HE_TO_FE_CONVERSION_RATE.get();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        PowerEndpoint endpoint = endpoint();
        double rate = rate();
        if (endpoint == null || !connected(endpoint) || maxReceive <= 0 || rate <= 0D
                || endpoint.getRequestedInput() <= 0L) {
            return 0;
        }
        long heBudget = Math.max(0L, (long) Math.floor(maxReceive / rate));
        if (heBudget <= 0L) {
            return simulate ? 1 : 0;
        }
        long accepted = Math.min(heBudget, endpoint.getRequestedInput());
        if (accepted > 0L && !simulate) {
            endpoint.applyPower(0L, accepted);
        }
        return toFe(accepted, maxReceive, rate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        PowerEndpoint endpoint = endpoint();
        double rate = rate();
        if (endpoint == null || !connected(endpoint) || maxExtract <= 0 || rate <= 0D
                || endpoint.getAvailableOutput() <= 0L) {
            return 0;
        }
        long heBudget = Math.max(0L, (long) Math.floor(maxExtract / rate));
        if (heBudget <= 0L) {
            return simulate ? 1 : 0;
        }
        long extracted = Math.min(heBudget, endpoint.getAvailableOutput());
        if (extracted > 0L && !simulate) {
            endpoint.applyPower(extracted, 0L);
        }
        return toFe(extracted, maxExtract, rate);
    }

    @Override
    public int getEnergyStored() {
        PowerEndpoint endpoint = endpoint();
        double rate = rate();
        return endpoint == null || rate <= 0D ? 0 : toFe(endpoint.getAvailableOutput(), Integer.MAX_VALUE, rate);
    }

    @Override
    public int getMaxEnergyStored() {
        PowerEndpoint endpoint = endpoint();
        double rate = rate();
        if (endpoint == null || rate <= 0D) {
            return 0;
        }
        long visible = Math.max(endpoint.getAvailableOutput(), endpoint.getRequestedInput());
        return toFe(visible, Integer.MAX_VALUE, rate);
    }

    @Override
    public boolean canExtract() {
        PowerEndpoint endpoint = endpoint();
        return endpoint != null && connected(endpoint) && endpoint.getAvailableOutput() > 0L;
    }

    @Override
    public boolean canReceive() {
        PowerEndpoint endpoint = endpoint();
        return endpoint != null && connected(endpoint) && endpoint.getRequestedInput() > 0L;
    }

    private static int toFe(long he, int limit, double rate) {
        if (he <= 0L) {
            return 0;
        }
        double converted = he * rate;
        if (!Double.isFinite(converted) || converted >= Integer.MAX_VALUE) {
            return Math.min(limit, Integer.MAX_VALUE);
        }
        return (int) Math.min(limit, Math.max(0L, Math.round(converted)));
    }
}
