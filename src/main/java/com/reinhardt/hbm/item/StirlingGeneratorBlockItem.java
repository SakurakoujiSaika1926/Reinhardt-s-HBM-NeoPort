package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.StirlingGeneratorItemRenderer;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.StirlingGeneratorBlock;
import com.reinhardt.hbm.blockentity.HeatSourceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class StirlingGeneratorBlockItem extends BlockItem {
    public StirlingGeneratorBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final StirlingGeneratorItemRenderer renderer = new StirlingGeneratorItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() == Direction.UP) {
            BlockPos heatCore = heatCorePos(level, context.getClickedPos());
            if (heatCore != null) {
                return placeCenteredOnHeatSource(context, heatCore);
            }
        }
        return super.useOn(context);
    }

    private InteractionResult placeCenteredOnHeatSource(UseOnContext context, BlockPos heatCore) {
        Level level = context.getLevel();
        Direction facing = context.getPlayer() == null ? Direction.NORTH : context.getPlayer().getDirection().getOpposite();
        BlockPos placePos = new BlockPos(heatCore.getX(), topOfHeatMachine(level, heatCore).getY() + 1, heatCore.getZ());
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        LargeMachineBlock.Footprint footprint = LargeMachineBlock.Footprint.centered(1, 2, 1);
        if (!level.getBlockState(placePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }
        if (!LargeMachineBlock.canPlaceFootprintAt(level, placePos, facing, footprint, placeContext)) {
            return InteractionResult.FAIL;
        }

        BlockState state = getBlock().defaultBlockState().setValue(StirlingGeneratorBlock.FACING, facing);
        if (!level.setBlock(placePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }
        getBlock().setPlacedBy(level, placePos, state, context.getPlayer(), context.getItemInHand());
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static BlockPos heatCorePos(Level level, BlockPos clickedPos) {
        BlockEntity clickedEntity = level.getBlockEntity(clickedPos);
        if (clickedEntity instanceof HeatSourceBlockEntity) {
            return clickedPos;
        }
        if (clickedEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity) {
            return dummy.getCorePos();
        }
        if (level.getBlockState(clickedPos).getBlock() instanceof MachineDummyBlock
                && clickedEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatSourceBlockEntity) {
            return dummy.getCorePos();
        }
        return null;
    }

    private static BlockPos topOfHeatMachine(Level level, BlockPos corePos) {
        BlockPos top = corePos;
        for (int y = corePos.getY(); y <= corePos.getY() + 4; y++) {
            for (int x = corePos.getX() - 2; x <= corePos.getX() + 2; x++) {
                for (int z = corePos.getZ() - 2; z <= corePos.getZ() + 2; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(corePos) && level.getBlockEntity(pos) instanceof HeatSourceBlockEntity) {
                        top = top.getY() < y ? pos : top;
                    } else if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy && dummy.getCorePos().equals(corePos)) {
                        top = top.getY() < y ? pos : top;
                    }
                }
            }
        }
        return top;
    }
}
