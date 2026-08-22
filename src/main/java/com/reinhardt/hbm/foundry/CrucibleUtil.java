package com.reinhardt.hbm.foundry;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public final class CrucibleUtil {
    private CrucibleUtil() {
    }

    @Nullable
    public static FoundryMaterialStack pourFullStack(
            Level level,
            double x,
            double y,
            double z,
            double range,
            boolean safe,
            List<FoundryMaterialStack> stacks,
            int quanta,
            Predicate<FoundryMaterial> materialFilter
    ) {
        if (stacks.isEmpty() || quanta <= 0) {
            return null;
        }

        PourTarget target = findPouringTarget(level, x, y, z, range);
        if (target == null) {
            return spill(safe, stacks, quanta, materialFilter);
        }

        for (int index = 0; index < stacks.size(); index++) {
            FoundryMaterialStack stack = stacks.get(index);
            if (stack.material().behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE || !materialFilter.test(stack.material())) {
                continue;
            }

            int amountToPour = Math.min(stack.amount(), quanta);
            FoundryMaterialStack toPour = new FoundryMaterialStack(stack.material(), amountToPour);
            if (!target.acceptor().canAcceptPartialPour(level, target.pos(), x, y, z, Direction.UP, toPour)) {
                continue;
            }

            FoundryMaterialStack left = target.acceptor().pour(level, target.pos(), x, y, z, Direction.UP, toPour);
            int leftover = left == null ? 0 : left.amount();
            int transferred = amountToPour - leftover;
            if (transferred <= 0) {
                continue;
            }

            int remaining = stack.amount() - transferred;
            if (remaining <= 0) {
                stacks.remove(index);
            } else {
                stacks.set(index, new FoundryMaterialStack(stack.material(), remaining));
            }
            return new FoundryMaterialStack(stack.material(), transferred);
        }

        return spill(safe, stacks, quanta, materialFilter);
    }

    @Nullable
    private static PourTarget findPouringTarget(Level level, double x, double y, double z, double range) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int top = (int) Math.floor(y);
        int bottom = (int) Math.floor(y - range);
        for (int blockY = top; blockY >= bottom; blockY--) {
            cursor.set(blockX, blockY, blockZ);
            CrucibleAcceptor acceptor = acceptorAt(level, cursor);
            if (acceptor != null) {
                return new PourTarget(cursor.immutable(), acceptor);
            }
        }
        return null;
    }

    @Nullable
    private static CrucibleAcceptor acceptorAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrucibleAcceptor acceptor) {
            return acceptor;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CrucibleAcceptor acceptor) {
            return acceptor;
        }
        return null;
    }

    @Nullable
    private static FoundryMaterialStack spill(boolean safe, List<FoundryMaterialStack> stacks, int quanta, Predicate<FoundryMaterial> materialFilter) {
        if (safe) {
            return null;
        }

        for (int index = 0; index < stacks.size(); index++) {
            FoundryMaterialStack stack = stacks.get(index);
            if (!materialFilter.test(stack.material())) {
                continue;
            }
            int amount = Math.min(stack.amount(), quanta);
            int remaining = stack.amount() - amount;
            if (remaining <= 0) {
                stacks.remove(index);
            } else {
                stacks.set(index, new FoundryMaterialStack(stack.material(), remaining));
            }
            return new FoundryMaterialStack(stack.material(), amount);
        }
        return null;
    }

    private record PourTarget(BlockPos pos, CrucibleAcceptor acceptor) {
    }
}
