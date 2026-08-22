package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SoyuzLauncherBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SoyuzStructCoreBlockEntity extends BlockEntity {
    private int age;

    public SoyuzStructCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SOYUZ_STRUCT_CORE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SoyuzStructCoreBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        entity.age++;
        if (entity.age < 20) {
            return;
        }
        entity.age = 0;
        if (!matchesStructure(level, pos)) {
            return;
        }
        buildLauncher(level, pos);
    }

    private static boolean matchesStructure(Level level, BlockPos pos) {
        for (int x = -6; x <= 6; x++) {
            for (int y = 3; y <= 4; y++) {
                for (int z = -6; z <= 6; z++) {
                    if (!level.getBlockState(pos.offset(x, y, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                        return false;
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 3; y <= 4; y++) {
                for (int z = -8; z <= -7; z++) {
                    if (!level.getBlockState(pos.offset(x, y, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                        return false;
                    }
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 3; y <= 4; y++) {
                for (int z = 7; z <= 9; z++) {
                    if (!level.getBlockState(pos.offset(x, y, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                        return false;
                    }
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = 5; z <= 9; z++) {
                if (!level.getBlockState(pos.offset(x, 51, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                    return false;
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -8; z <= -6; z++) {
                if (!level.getBlockState(pos.offset(x, 38, z)).is(HbmBlocks.STRUCT_LAUNCHER.get())) {
                    return false;
                }
            }
        }
        return hasConcreteLegs(level, pos) && hasScaffold(level, pos);
    }

    private static boolean hasConcreteLegs(Level level, BlockPos pos) {
        for (int x = 3; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 3; z <= 6; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int x = -6; x <= -3; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 3; z <= 6; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int x = -6; x <= -3; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -6; z <= -3; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int x = 3; x <= 6; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -6; z <= -3; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -8; z <= -6; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 5; z <= 9; z++) {
                    if (!isConcrete(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasScaffold(Level level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 5; y <= 50; y++) {
                for (int z = 6; z <= 8; z++) {
                    if (!isStructScaffold(level.getBlockState(pos.offset(x, y, z)))) {
                        return false;
                    }
                }
            }
        }
        for (int y = 5; y <= 37; y++) {
            if (!isStructScaffold(level.getBlockState(pos.offset(0, y, -7)))) {
                return false;
            }
        }
        return true;
    }

    private static void buildLauncher(Level level, BlockPos pos) {
        removeTemporaryStructure(level, pos);
        BlockPos corePos = pos.above(SoyuzLauncherBlock.HEIGHT_OFFSET);
        BlockState launcher = HbmBlocks.SOYUZ_LAUNCHER.get()
                .defaultBlockState()
                .setValue(LargeMachineBlock.FACING, Direction.EAST);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(corePos, launcher, Block.UPDATE_ALL);
        SoyuzLauncherBlock.placeDummies(level, corePos);
    }

    private static void removeTemporaryStructure(Level level, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = 5; z <= 9; z++) {
                level.setBlock(pos.offset(x, 51, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -8; z <= -6; z++) {
                level.setBlock(pos.offset(x, 38, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 5; z <= 9; z++) {
                    level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 5; y <= 50; y++) {
                for (int z = 6; z <= 8; z++) {
                    level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        for (int y = 5; y <= 37; y++) {
            level.setBlock(pos.offset(0, y, -7), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean isConcrete(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "reinhardtshbm".equals(key.getNamespace())
                && ("concrete".equals(key.getPath()) || "concrete_smooth".equals(key.getPath()));
    }

    private static boolean isStructScaffold(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "reinhardtshbm".equals(key.getNamespace()) && "struct_scaffold".equals(key.getPath());
    }
}
