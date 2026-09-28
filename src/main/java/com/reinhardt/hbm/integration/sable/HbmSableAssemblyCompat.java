package com.reinhardt.hbm.integration.sable;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.BlastDoorDummyBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightDummyBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.blockentity.IcfAssembledLaserBlockEntity;
import com.reinhardt.hbm.blockentity.IcfControllerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Repairs HBM block-entity links after Sable moves a complete block batch.
 *
 * <p>Sable copies block-entity NBT verbatim and only rewrites the NBT's own
 * {@code x/y/z}. HBM's assembled PWR/ICF machines additionally persist links
 * to their controller and footprint. Those links must be remapped with the
 * exact source-to-destination mapping or the copied parts will regard their
 * controller as missing and restore their raw construction blocks.</p>
 *
 * <p>The Sable API is deliberately reached through reflection so HBM remains
 * loadable when Sable and Create Aeronautics are absent.</p>
 */
public final class HbmSableAssemblyCompat {
    private static final String TRANSFORM_CLASS =
            "dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile Adapter adapter;

    private HbmSableAssemblyCompat() {
    }

    /** Called once, at the tail of Sable's complete {@code moveBlocks} batch. */
    public static void afterMoveBlocks(
            ServerLevel sourceLevel,
            Object assemblyTransform,
            Iterable<BlockPos> sourcePositions
    ) {
        Adapter current = adapter;
        if (current == null) {
            current = Adapter.create(assemblyTransform);
            if (current == null) {
                return;
            }
            adapter = current;
        }

        try {
            ServerLevel destinationLevel = current.destinationLevel(assemblyTransform);
            Map<BlockPos, BlockPos> movedPositions = new LinkedHashMap<>();
            for (BlockPos sourcePos : sourcePositions) {
                BlockPos immutableSource = sourcePos.immutable();
                movedPositions.put(immutableSource, current.apply(assemblyTransform, immutableSource));
            }

            int repaired = 0;
            for (Map.Entry<BlockPos, BlockPos> moved : movedPositions.entrySet()) {
                BlockEntity blockEntity = destinationLevel.getBlockEntity(moved.getValue());
                if (blockEntity == null) {
                    continue;
                }
                if (repairAssemblyLinks(blockEntity, movedPositions)
                        || repairProxyLink(blockEntity, moved.getKey(), movedPositions)) {
                    repaired++;
                }
            }

            if (repaired > 0) {
                HbmSablePowerCompat.invalidate(sourceLevel);
                HbmSablePowerCompat.invalidate(destinationLevel);
                ReinhardtsHBM.LOGGER.debug(
                        "Remapped {} HBM multiblock links after Sable moved {} blocks",
                        repaired,
                        movedPositions.size()
                );
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (WARNED.compareAndSet(false, true)) {
                ReinhardtsHBM.LOGGER.error(
                        "Failed to remap HBM multiblock links after a Sable assembly move",
                        unwrap(exception)
                );
            }
        }
    }

    private static boolean repairAssemblyLinks(
            BlockEntity blockEntity,
            Map<BlockPos, BlockPos> movedPositions
    ) {
        if (blockEntity instanceof PwrBlockEntity part) {
            BlockPos movedController = movedPositions.get(part.controllerPos());
            if (movedController != null) {
                part.transformAssemblyLink(ignored -> movedController);
                return true;
            }
            return false;
        }
        if (blockEntity instanceof PwrControllerBlockEntity controller) {
            controller.transformAssemblyLinks(pos -> movedPositions.getOrDefault(pos, pos));
            return true;
        }
        if (blockEntity instanceof IcfAssembledLaserBlockEntity part) {
            BlockPos movedController = movedPositions.get(part.controllerPos());
            if (movedController != null) {
                part.transformAssemblyLink(ignored -> movedController);
                return true;
            }
            return false;
        }
        if (blockEntity instanceof IcfControllerBlockEntity controller) {
            controller.transformAssemblyLinks(pos -> movedPositions.getOrDefault(pos, pos));
            return true;
        }
        return false;
    }

    private static boolean repairProxyLink(
            BlockEntity blockEntity,
            BlockPos sourcePos,
            Map<BlockPos, BlockPos> movedPositions
    ) {
        BlockPos sourceCore;
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            sourceCore = sourcePos.offset(dummy.getCoreOffset());
            BlockPos destinationCore = movedPositions.get(sourceCore);
            if (destinationCore != null) {
                dummy.setCorePos(destinationCore);
                return true;
            }
        } else if (blockEntity instanceof BlastDoorDummyBlockEntity dummy) {
            sourceCore = sourcePos.offset(dummy.coreOffset());
            BlockPos destinationCore = movedPositions.get(sourceCore);
            if (destinationCore != null) {
                dummy.setCorePos(destinationCore);
                return true;
            }
        } else if (blockEntity instanceof HbmHeavyDoorPartBlockEntity dummy) {
            sourceCore = sourcePos.offset(dummy.coreOffset());
            BlockPos destinationCore = movedPositions.get(sourceCore);
            if (destinationCore != null) {
                dummy.setCoreOffset(destinationCore.subtract(blockEntity.getBlockPos()));
                return true;
            }
        } else if (blockEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            sourceCore = sourcePos.offset(dummy.coreOffset());
            BlockPos destinationCore = movedPositions.get(sourceCore);
            if (destinationCore != null) {
                dummy.setCorePos(destinationCore);
                return true;
            }
        } else if (blockEntity instanceof FloodlightDummyBlockEntity dummy) {
            sourceCore = sourcePos.offset(dummy.coreOffset());
            BlockPos destinationCore = movedPositions.get(sourceCore);
            if (destinationCore != null) {
                dummy.setCorePos(destinationCore);
                return true;
            }
        }
        return false;
    }

    private static Throwable unwrap(Throwable throwable) {
        return throwable instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : throwable;
    }

    private record Adapter(Method apply, Method getLevel) {
        private static Adapter create(Object transform) {
            if (transform == null || !TRANSFORM_CLASS.equals(transform.getClass().getName())) {
                warnAdapterFailure(new IllegalArgumentException(
                        "Unexpected Sable assembly transform: "
                                + (transform == null ? "null" : transform.getClass().getName())
                ));
                return null;
            }
            try {
                Method apply = transform.getClass().getMethod("apply", BlockPos.class);
                Method getLevel = transform.getClass().getMethod("getLevel");
                return new Adapter(apply, getLevel);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                warnAdapterFailure(exception);
                return null;
            }
        }

        private BlockPos apply(Object transform, BlockPos sourcePos) throws ReflectiveOperationException {
            Object result = this.apply.invoke(transform, sourcePos);
            if (result instanceof BlockPos blockPos) {
                return blockPos.immutable();
            }
            throw new ReflectiveOperationException("Sable AssemblyTransform.apply(BlockPos) returned " + result);
        }

        private ServerLevel destinationLevel(Object transform) throws ReflectiveOperationException {
            Object result = this.getLevel.invoke(transform);
            if (result instanceof ServerLevel serverLevel) {
                return serverLevel;
            }
            throw new ReflectiveOperationException("Sable AssemblyTransform.getLevel() returned " + result);
        }

        private static void warnAdapterFailure(Throwable throwable) {
            if (WARNED.compareAndSet(false, true)) {
                ReinhardtsHBM.LOGGER.error(
                        "Sable is present but its assembly transform API is incompatible; "
                                + "HBM multiblock relocation protection could not be installed",
                        throwable
                );
            }
        }
    }
}
