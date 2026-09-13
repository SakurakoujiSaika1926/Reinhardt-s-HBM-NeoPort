package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class SoyuzLauncherBlock extends LargeMachineBlock implements EntityBlock {
    public static final int HEIGHT_OFFSET = 4;
    public static final Footprint FOOTPRINT = footprint();

    public SoyuzLauncherBlock(Properties properties) {
        // The fixed-east footprint below is transcribed from the 1.7.10
        // MultiblockHandlerXR dimension boxes rather than authored in the
        // modern north-facing basis.
        super(properties, FOOTPRINT, Shapes.empty(), RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            placeDummies(level, pos);
            pushEntitiesOutOfPositions(level, pos, Direction.EAST,
                    FOOTPRINT.offsets().stream().map(pos::offset).toList(), placer);
        }
    }

    public boolean canPlaceAt(BlockPlaceContext context, BlockPos corePos) {
        for (BlockPos offset : FOOTPRINT.offsets()) {
            BlockPos target = corePos.offset(offset);
            if (!target.equals(corePos) && !context.getLevel().getBlockState(target).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static void placeDummies(Level level, BlockPos corePos) {
        for (BlockPos offset : FOOTPRINT.offsets()) {
            if (offset.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos pos = corePos.offset(offset);
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof SoyuzLauncherBlockEntity launcher) {
                launcher.dropContents(level, pos);
                // SoyuzLauncher.breakBlock: the completed multiblock returns its construction materials.
                dropMaterial(level, pos, HbmBlocks.STRUCT_LAUNCHER.get(), 414);
                dropMaterial(level, pos, HbmBlocks.CONCRETE_SMOOTH.get(), 294);
                dropMaterial(level, pos, HbmBlocks.STRUCT_SCAFFOLD.get(), 447);
                dropMaterial(level, pos, HbmBlocks.STRUCT_SOYUZ_CORE.get(), 1);
            }
            removeDummies(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void dropMaterial(Level level, BlockPos pos, Block block, int count) {
        while (count > 0) {
            int size = Math.min(count, 64);
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new net.minecraft.world.item.ItemStack(block, size)));
            count -= size;
        }
    }

    private static void removeDummies(Level level, BlockPos corePos) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : FOOTPRINT.offsets()) {
                BlockPos pos = corePos.offset(offset);
                if (!pos.equals(corePos) && level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzLauncherBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != com.reinhardt.hbm.registry.HbmBlockEntities.SOYUZ_LAUNCHER.get()) return null;
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof SoyuzLauncherBlockEntity soyuz) {
                SoyuzLauncherBlockEntity.tick(tickerLevel, tickerPos, tickerState, soyuz);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isCrouching()) return InteractionResult.PASS;
        if (!level.isClientSide && !player.isCrouching() && player instanceof ServerPlayer serverPlayer) {
            LegacyMobSpawnEvents.markFbi(serverPlayer);
        }
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof MenuProvider menuProvider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    private static Footprint footprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addOldEastDimBox(offsets, new int[]{0, 1, 6, 6, 6, 6});
        addOldEastDimBox(offsets, new int[]{-2, 4, -3, 6, -3, 6});
        addOldEastDimBox(offsets, new int[]{-2, 4, 6, -3, -3, 6});
        addOldEastDimBox(offsets, new int[]{-2, 4, 6, -3, 6, -3});
        addOldEastDimBox(offsets, new int[]{-2, 4, -3, 6, 6, -3});
        addOldEastDimBox(offsets, new int[]{0, 4, 1, 1, -6, 8});
        addOldEastDimBox(offsets, new int[]{0, 4, 2, 2, 9, -5});
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x == 6 || x == -6 || z == 6 || z == -6) {
                    offsets.add(new BlockPos(x, 0, z));
                    offsets.add(new BlockPos(x, 1, z));
                }
            }
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(offsets.stream().toList());
    }

    private static void addOldEastDimBox(Set<BlockPos> offsets, int[] dim) {
        int minX = -dim[2];
        int maxX = dim[3];
        int minY = -dim[1];
        int maxY = dim[0];
        int minZ = -dim[5];
        int maxZ = dim[4];
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}
