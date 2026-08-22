package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CompactLauncherBlock;
import com.reinhardt.hbm.block.LaunchTableBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LauncherStructCoreBlockEntity extends BlockEntity {
    private final boolean large;

    public LauncherStructCoreBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, false);
    }

    public LauncherStructCoreBlockEntity(BlockPos pos, BlockState blockState, boolean large) {
        super(HbmBlockEntities.LAUNCHER_STRUCT_CORE.get(), pos, blockState);
        this.large = large;
    }

    public boolean isLarge() {
        return this.large;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LauncherStructCoreBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        if (entity.large) {
            Direction scaffold = tableScaffoldDirection(level, pos);
            if (scaffold != null) {
                buildTable(level, pos, scaffold);
            }
        } else if (isCompact(level, pos)) {
            buildCompact(level, pos);
        }
    }

    private static boolean isCompact(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (!level.getBlockState(pos.offset(x, 0, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Direction tableScaffoldDirection(Level level, BlockPos pos) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (!level.getBlockState(pos.offset(x, 0, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                    return null;
                }
            }
        }
        if (hasScaffoldColumn(level, pos.offset(3, 0, 0))) {
            return Direction.NORTH;
        }
        if (hasScaffoldColumn(level, pos.offset(-3, 0, 0))) {
            return Direction.SOUTH;
        }
        if (hasScaffoldColumn(level, pos.offset(0, 0, 3))) {
            return Direction.EAST;
        }
        if (hasScaffoldColumn(level, pos.offset(0, 0, -3))) {
            return Direction.WEST;
        }
        return null;
    }

    private static boolean hasScaffoldColumn(Level level, BlockPos base) {
        for (int y = 1; y < 12; y++) {
            if (!isStructScaffold(level.getBlockState(base.above(y)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStructScaffold(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "reinhardtshbm".equals(key.getNamespace()) && "struct_scaffold".equals(key.getPath());
    }

    private static void buildCompact(Level level, BlockPos pos) {
        level.setBlock(pos, HbmBlocks.COMPACT_LAUNCHER.get().defaultBlockState(), Block.UPDATE_ALL);
        CompactLauncherBlock.placeDummies(level, pos);
    }

    private static void buildTable(Level level, BlockPos pos, Direction facing) {
        BlockState state = HbmBlocks.LAUNCH_TABLE.get().defaultBlockState().setValue(LaunchTableBlock.FACING, facing);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        LaunchTableBlock.placeDummies(level, pos, facing);
        LaunchTableBlock.clearScaffoldColumn(level, pos, facing);
    }
}
