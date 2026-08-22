package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FoundrySlagBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class FoundrySlagBlock extends Block implements EntityBlock {
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public FoundrySlagBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundrySlagBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FoundrySlagBlockEntity slag) || slag.isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (tryFlowDown(level, pos, slag)) {
            return;
        }
        spreadSideways(level, pos, slag);
        if (!slag.isEmpty()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(level, pos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FoundrySlagBlockEntity slag) {
            FoundryMaterialStack stack = slag.stack();
            return stack == null ? List.of() : List.of(ScrapsItem.create(stack, false));
        }
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof FoundrySlagBlockEntity slag) {
            FoundryMaterialStack stack = slag.stack();
            if (stack != null) {
                return ScrapsItem.create(stack, false);
            }
        }
        return new ItemStack(this);
    }

    private static VoxelShape shape(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FoundrySlagBlockEntity slag && !slag.isEmpty()) {
            double height = Math.max(1.0D / 16.0D, Math.min(1.0D, (double) slag.amount() / FoundrySlagBlockEntity.MAX_AMOUNT));
            return Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, height, 1.0D);
        }
        return Shapes.block();
    }

    private static boolean tryFlowDown(ServerLevel level, BlockPos pos, FoundrySlagBlockEntity slag) {
        BlockPos below = pos.below();
        if (below.getY() < level.getMinBuildHeight()) {
            return false;
        }
        BlockState belowState = level.getBlockState(below);
        FoundryMaterial material = slag.material();
        if (material == null) {
            return false;
        }
        if (belowState.is(HbmBlocks.SLAG.get()) && level.getBlockEntity(below) instanceof FoundrySlagBlockEntity belowSlag
                && belowSlag.canAccept(material) && belowSlag.remainingCapacity() > 0) {
            int transferred = belowSlag.add(material, slag.amount());
            slag.shrink(transferred);
            if (slag.isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.scheduleTick(pos, HbmBlocks.SLAG.get(), 1);
            }
            level.scheduleTick(below, HbmBlocks.SLAG.get(), 1);
            return true;
        }
        if (belowState.canBeReplaced()) {
            int amount = slag.amount();
            level.setBlock(below, HbmBlocks.SLAG.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(below) instanceof FoundrySlagBlockEntity belowSlag) {
                belowSlag.setContents(material, amount);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.scheduleTick(below, HbmBlocks.SLAG.get(), 1);
            return true;
        }
        return false;
    }

    private static void spreadSideways(ServerLevel level, BlockPos pos, FoundrySlagBlockEntity slag) {
        if (slag.amount() < FoundrySlagBlockEntity.MAX_AMOUNT / 5) {
            return;
        }
        int openings = 0;
        for (Direction direction : HORIZONTALS) {
            if (level.getBlockState(pos.relative(direction)).canBeReplaced()) {
                openings++;
            }
        }
        if (openings <= 0) {
            return;
        }

        FoundryMaterial material = slag.material();
        if (material == null) {
            return;
        }
        int toSpread = Math.max(slag.amount() / (openings * 2), 1);
        for (Direction direction : HORIZONTALS) {
            if (slag.isEmpty()) {
                break;
            }
            BlockPos target = pos.relative(direction);
            if (!level.getBlockState(target).canBeReplaced()) {
                continue;
            }
            int transferred = Math.min(toSpread, slag.amount());
            level.setBlock(target, HbmBlocks.SLAG.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(target) instanceof FoundrySlagBlockEntity targetSlag) {
                targetSlag.setContents(material, transferred);
            }
            slag.shrink(transferred);
            level.scheduleTick(target, HbmBlocks.SLAG.get(), 1);
        }
        if (slag.isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
