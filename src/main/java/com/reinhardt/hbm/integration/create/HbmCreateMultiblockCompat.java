package com.reinhardt.hbm.integration.create;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.BlastDoorBlock;
import com.reinhardt.hbm.block.BlastDoorDummyBlock;
import com.reinhardt.hbm.block.ElectrolyzerBlock;
import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.block.FloodlightDummyBlock;
import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.block.LargeFluidTankBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.MiningLaserBlock;
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.block.SatelliteDockBlock;
import com.reinhardt.hbm.block.SatelliteDockDummyBlock;
import com.reinhardt.hbm.block.StirlingGeneratorBlock;
import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.block.TurretJeremyBlock;
import com.reinhardt.hbm.block.WatzBlock;
import com.reinhardt.hbm.block.WatzPumpBlock;
import com.reinhardt.hbm.block.WoodBurnerBlock;
import com.reinhardt.hbm.blockentity.IcfAssembledLaserBlockEntity;
import com.reinhardt.hbm.blockentity.IcfControllerBlockEntity;
import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stops Create contraptions from capturing an already formed HBM multiblock.
 * Unassembled construction blocks remain movable; only cores, their proxy
 * cells, and parts carrying an active assembly link are rejected.
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

        BlockMovementChecks.registerMovementAllowedCheck(HbmCreateMultiblockCompat::movementAllowed);
        ReinhardtsHBM.LOGGER.info("Installed Create movement guard for formed HBM multiblocks and movable dud bombs");
    }

    private static BlockMovementChecks.CheckResult movementAllowed(
            BlockState state, Level level, BlockPos pos) {
        // Preserve the existing high-version gameplay exception for dud bombs.
        if (state.is(HbmBlocks.CRASHED_BOMB.get())) {
            return BlockMovementChecks.CheckResult.SUCCESS;
        }
        return isFormedMultiblock(state, level, pos)
                ? BlockMovementChecks.CheckResult.FAIL
                : BlockMovementChecks.CheckResult.PASS;
    }

    private static boolean isFormedMultiblock(BlockState state, Level level, BlockPos pos) {
        Block block = state.getBlock();
        if (isProxyBlock(block) || isPlacedMultiblockCore(block)) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PwrBlockEntity part) {
            return part.isAssembledPart();
        }
        if (blockEntity instanceof PwrControllerBlockEntity controller) {
            return controller.assembled();
        }
        if (blockEntity instanceof IcfAssembledLaserBlockEntity) {
            return true;
        }
        return blockEntity instanceof IcfControllerBlockEntity controller
                && controller.assembled();
    }

    private static boolean isProxyBlock(Block block) {
        return block instanceof MachineDummyBlock
                || block instanceof BlastDoorDummyBlock
                || block instanceof HbmHeavyDoorPartBlock
                || block instanceof SatelliteDockDummyBlock
                || block instanceof FloodlightDummyBlock;
    }

    private static boolean isPlacedMultiblockCore(Block block) {
        return block instanceof LargeMachineBlock
                || block instanceof BlastDoorBlock
                || block instanceof ElectrolyzerBlock
                || block instanceof FloodlightBlock
                || block instanceof GasTurbineBlock
                || block instanceof HbmHeavyDoorBlock
                || block instanceof LargeFluidTankBlock
                || block instanceof LegacyTurretBlock
                || block instanceof MiningLaserBlock
                || block instanceof PowerPylonBlock
                || block instanceof RbmkComponentBlock
                || block instanceof SatelliteDockBlock
                || block instanceof StirlingGeneratorBlock
                || block instanceof TurretChekhovBlock
                || block instanceof TurretJeremyBlock
                || block instanceof WatzBlock
                || block instanceof WatzPumpBlock
                || block instanceof WoodBurnerBlock;
    }
}
