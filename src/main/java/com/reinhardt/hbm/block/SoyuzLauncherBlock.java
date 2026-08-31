package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
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
        super(properties, FOOTPRINT, Shapes.empty());
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
            removeDummies(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeDummies(Level level, BlockPos corePos) {
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
        return level.isClientSide ? null : (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof SoyuzLauncherBlockEntity soyuz) {
                SoyuzLauncherBlockEntity.tick(tickerLevel, tickerPos, tickerState, soyuz);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
