package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.RbmkFuelRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface NeutronFluxProvider {
    NeutronFlux neutronFluxSpectrum(Level level, BlockPos requesterPos);

    static NeutronFlux sumHorizontalSpectrum(Level level, BlockPos pos) {
        NeutronFlux flux = NeutronFlux.ZERO;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof NeutronFluxProvider provider) {
                flux = flux.add(provider.neutronFluxSpectrum(level, pos));
            }
        }
        return flux;
    }

    static int sumHorizontalFlux(Level level, BlockPos pos) {
        return (int) Math.round(sumHorizontalSpectrum(level, pos).total());
    }

    record NeutronFlux(double slow, double fast) {
        public static final NeutronFlux ZERO = new NeutronFlux(0.0D, 0.0D);

        public NeutronFlux {
            slow = Math.max(0.0D, slow);
            fast = Math.max(0.0D, fast);
        }

        public static NeutronFlux slow(double value) {
            return new NeutronFlux(value, 0.0D);
        }

        public static NeutronFlux fast(double value) {
            return new NeutronFlux(0.0D, value);
        }

        public static NeutronFlux fromRatio(double total, double fastRatio) {
            double clampedTotal = Math.max(0.0D, total);
            double clampedRatio = Math.max(0.0D, Math.min(1.0D, fastRatio));
            return new NeutronFlux(clampedTotal * (1.0D - clampedRatio), clampedTotal * clampedRatio);
        }

        public NeutronFlux add(NeutronFlux other) {
            return new NeutronFlux(this.slow + other.slow, this.fast + other.fast);
        }

        public double total() {
            return slow + fast;
        }

        public double fastRatio() {
            double total = total();
            return total <= 0.0D ? 0.0D : fast / total;
        }

        public double effectiveFor(RbmkFuelRodItem.NeutronType type) {
            return switch (type) {
                case SLOW -> slow + fast * 0.5D;
                case FAST -> fast + slow * 0.3D;
                case ANY -> total();
            };
        }
    }
}
