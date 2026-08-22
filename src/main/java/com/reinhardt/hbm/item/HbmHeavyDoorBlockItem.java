package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class HbmHeavyDoorBlockItem extends BlockItem {
    public HbmHeavyDoorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!(this.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return super.place(context);
        }
        BlockState state = doorBlock.getStateForPlacement(context);
        if (state == null) {
            return InteractionResult.FAIL;
        }
        BlockPos corePos = HbmHeavyDoorBlock.corePosForClicked(
                context.getClickedPos(),
                state.getValue(HbmHeavyDoorBlock.FACING),
                doorBlock.decl()
        );
        return super.place(new CoreContext(context, corePos, state.getValue(HbmHeavyDoorBlock.FACING)));
    }

    private static final class CoreContext extends BlockPlaceContext implements HbmHeavyDoorBlock.PrecomputedDoorPlacement {
        private final BlockPos corePos;
        private final Direction facing;

        private CoreContext(BlockPlaceContext original, BlockPos corePos, Direction facing) {
            super(original);
            this.corePos = corePos;
            this.facing = facing;
        }

        @Override
        public BlockPos getClickedPos() {
            return this.corePos;
        }

        @Override
        public Direction getHorizontalDirection() {
            return this.facing;
        }

        @Override
        public Direction hbmDoorFacing() {
            return this.facing;
        }

        @Override
        public boolean canPlace() {
            Level level = this.getLevel();
            return level.getBlockState(this.corePos).canBeReplaced(this);
        }
    }
}
