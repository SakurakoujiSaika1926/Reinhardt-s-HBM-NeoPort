package com.reinhardt.hbm.integration.create;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BlastDoorDummyBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.FloodlightDummyBlock;
import com.reinhardt.hbm.block.SatelliteDockDummyBlock;
import com.reinhardt.hbm.blockentity.BlastDoorDummyBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.FloodlightDummyBlockEntity;
import com.reinhardt.hbm.blockentity.SatelliteDockDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create integration for HBM's 1.7.10 {@code BlockDummyable} descendants.
 *
 * <p>The old implementation resolves a proxy by walking its actual adjacent
 * dummy chain back to the core.  The modern port stores that resolved core
 * position on their proxy block entities; Create needs the same exact
 * adjacency information while building a train contraption.  This class only
 * recognises HBM's explicit proxy block families.  It does not turn arbitrary
 * HBM blocks into a globally sticky or movable group.</p>
 */
public final class HbmCreateMultiblockCompat {
    private static boolean installed;

    private HbmCreateMultiblockCompat() {
    }

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;

        BlockMovementChecks.registerMovementNecessaryCheck(HbmCreateMultiblockCompat::isLegacyDummyNecessary);
        BlockMovementChecks.registerMovementAllowedCheck(HbmCreateMultiblockCompat::isCrashedBombMovementAllowed);
        BlockMovementChecks.registerAttachedCheck(HbmCreateMultiblockCompat::isLegacyDummyAttached);
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                HbmBlockEntities.MACHINE_DUMMY.get(),
                HbmCreateMultiblockCompat::transformDummyCoreLink
        );
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                HbmBlockEntities.BLAST_DOOR_DUMMY.get(),
                HbmCreateMultiblockCompat::transformDummyCoreLink
        );
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                HbmBlockEntities.HEAVY_DOOR_PART.get(),
                HbmCreateMultiblockCompat::transformDummyCoreLink
        );
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                HbmBlockEntities.SAT_DOCK_DUMMY.get(),
                HbmCreateMultiblockCompat::transformDummyCoreLink
        );
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                HbmBlockEntities.FLOODLIGHT_DUMMY.get(),
                HbmCreateMultiblockCompat::transformDummyCoreLink
        );
        ReinhardtsHBM.LOGGER.info("Installed Create support for HBM BlockDummyable proxy chains and movable dud bombs");
    }

    /**
     * High-version gameplay exception: crashed bombs stay unbreakable in the
     * world, but Create-style movers should still be able to capture and place
     * them.  The block itself clamps player mining progress to zero; this
     * callback keeps Create from treating the legacy dud as a hard blacklist.
     */
    private static BlockMovementChecks.CheckResult isCrashedBombMovementAllowed(
            BlockState state, Level level, BlockPos pos) {
        return state.is(HbmBlocks.CRASHED_BOMB.get())
                ? BlockMovementChecks.CheckResult.SUCCESS
                : BlockMovementChecks.CheckResult.PASS;
    }

    /**
     * Some legacy proxy cells intentionally render with an empty collision
     * shape (for example an RBMK lid slot when no lid is installed).  Create's
     * default movement test would stop the breadth-first search at such a
     * cell before it can capture or traverse the rest of the machine.
     */
    private static BlockMovementChecks.CheckResult isLegacyDummyNecessary(
            BlockState state, Level level, BlockPos pos) {
        return isProxyState(state)
                ? BlockMovementChecks.CheckResult.SUCCESS
                : BlockMovementChecks.CheckResult.PASS;
    }

    /**
     * Create asks whether {@code candidatePos} is attached toward its one
     * adjacent current block.  The check deliberately uses the persisted
     * dummy link and block states instead of requiring the core block entity
     * to be present.  During train assembly Create can inspect a neighbour
     * while another part of the machine is already queued for capture; the
     * core BE is still in the original world, but is not a safe prerequisite
     * for walking the complete proxy chain.
     */
    private static BlockMovementChecks.CheckResult isLegacyDummyAttached(
            BlockState candidateState,
            Level level,
            BlockPos candidatePos,
            net.minecraft.core.Direction towardCurrent
    ) {
        BlockPos currentPos = candidatePos.relative(towardCurrent);
        BlockState currentState = level.getBlockState(currentPos);
        BlockEntity candidate = level.getBlockEntity(candidatePos);
        BlockEntity current = level.getBlockEntity(currentPos);

        if (linksDirectlyTo(candidate, currentPos)
                    && isMachineCoreState(currentState)) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }
        if (shareCoreLink(candidate, current)) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }
        if (linksDirectlyTo(current, candidatePos)
                && isMachineCoreState(candidateState)) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }

        // A BE can be unavailable briefly while a multiblock is being
        // assembled or synchronised.  The block state still identifies the
        // proxy, so preserve the legacy chain edge for adjacent proxy cells.
        // This is also what keeps a whole proxy footprint together when the
        // first cell is inspected before its neighbours have finished loading.
        if (sameProxyFamily(candidateState, currentState)) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }

        // Let Create and other integrations decide every non-HBM edge.
        return BlockMovementChecks.CheckResult.PASS;
    }

    private static boolean linksDirectlyTo(BlockEntity dummy, BlockPos expectedCore) {
        BlockPos corePos = corePos(dummy);
        return corePos != null
                && !corePos.equals(BlockPos.ZERO)
                && !corePos.equals(dummy.getBlockPos())
                && corePos.equals(expectedCore);
    }

    private static boolean shareCoreLink(BlockEntity first, BlockEntity second) {
        BlockPos firstCore = corePos(first);
        BlockPos secondCore = corePos(second);
        return firstCore != null
                && secondCore != null
                && !firstCore.equals(BlockPos.ZERO)
                && !firstCore.equals(first.getBlockPos())
                && !firstCore.equals(second.getBlockPos())
                && firstCore.equals(secondCore);
    }

    private static boolean isMachineCoreState(BlockState state) {
        return !isProxyState(state) && !state.isAir();
    }

    private static boolean isProxyState(BlockState state) {
        return state.getBlock() instanceof MachineDummyBlock
                || state.getBlock() instanceof BlastDoorDummyBlock
                || state.getBlock() instanceof HbmHeavyDoorPartBlock
                || state.getBlock() instanceof SatelliteDockDummyBlock
                || state.getBlock() instanceof FloodlightDummyBlock;
    }

    /**
     * Keeps a temporarily unloaded proxy chain connected, but never joins two
     * unrelated proxy systems simply because they happen to touch.
     */
    private static boolean sameProxyFamily(BlockState first, BlockState second) {
        if (first.getBlock() instanceof MachineDummyBlock
                && second.getBlock() instanceof MachineDummyBlock) {
            return true;
        }
        return first.getBlock().getClass() == second.getBlock().getClass()
                && isProxyState(first);
    }

    private static BlockPos corePos(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        if (blockEntity instanceof BlastDoorDummyBlockEntity dummy) {
            return dummy.corePos();
        }
        if (blockEntity instanceof HbmHeavyDoorPartBlockEntity dummy) {
            return dummy.corePos();
        }
        if (blockEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            return dummy.corePos();
        }
        if (blockEntity instanceof FloodlightDummyBlockEntity dummy) {
            return dummy.corePos();
        }
        return null;
    }

    private static BlockPos coreOffset(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCoreOffset();
        }
        if (blockEntity instanceof BlastDoorDummyBlockEntity dummy) {
            return dummy.coreOffset();
        }
        if (blockEntity instanceof HbmHeavyDoorPartBlockEntity dummy) {
            return dummy.coreOffset();
        }
        if (blockEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            return dummy.coreOffset();
        }
        if (blockEntity instanceof FloodlightDummyBlockEntity dummy) {
            return dummy.coreOffset();
        }
        return null;
    }

    private static void setCoreOffset(BlockEntity blockEntity, BlockPos coreOffset) {
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            dummy.setCoreOffset(coreOffset);
        } else if (blockEntity instanceof BlastDoorDummyBlockEntity dummy) {
            dummy.setCoreOffset(coreOffset);
        } else if (blockEntity instanceof HbmHeavyDoorPartBlockEntity dummy) {
            dummy.setCoreOffset(coreOffset);
        } else if (blockEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            dummy.setCoreOffset(coreOffset);
        } else if (blockEntity instanceof FloodlightDummyBlockEntity dummy) {
            dummy.setCoreOffset(coreOffset);
        }
    }

    /**
     * On disassembly, use Create's own affine transform for the one exact
     * proxy-to-core vector.  Subtracting the transformed origin cancels only
     * Create's translation; rotation and mirroring remain entirely Create's
     * authoritative transform rather than a hand-written global rotation.
     */
    private static void transformDummyCoreLink(BlockEntity blockEntity, StructureTransform transform) {
        BlockPos coreOffset = coreOffset(blockEntity);
        if (coreOffset == null) {
            return;
        }

        BlockPos transformedOffset = transform.apply(coreOffset).subtract(transform.apply(BlockPos.ZERO));
        if (!transformedOffset.equals(coreOffset)) {
            setCoreOffset(blockEntity, transformedOffset);
        }
    }
}
