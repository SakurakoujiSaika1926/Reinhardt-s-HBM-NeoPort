package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SteelGrateBlock extends Block {
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 9);

    private static final VoxelShape[] SHAPES = new VoxelShape[10];
    private static final VoxelShape[] WIDE_SHAPES = new VoxelShape[10];

    static {
        for (int height = 0; height < 10; height++) {
            double y = getY(height);
            SHAPES[height] = Shapes.box(0.0D, y, 0.0D, 1.0D, y + 0.125D, 1.0D);
            WIDE_SHAPES[height] = Shapes.box(0.0D, y, 0.0D, 1.0D, y + 0.1249375D, 1.0D);
        }
    }

    private final boolean wide;

    public SteelGrateBlock(Properties properties, boolean wide) {
        super(properties);
        this.wide = wide;
        this.registerDefaultState(this.stateDefinition.any().setValue(HEIGHT, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) {
            return this.defaultBlockState().setValue(HEIGHT, 7);
        }
        if (face == Direction.UP) {
            return this.defaultBlockState().setValue(HEIGHT, 0);
        }

        double hitY = context.getClickLocation().y - context.getClickedPos().getY();
        return this.defaultBlockState().setValue(HEIGHT, Mth.clamp((int) Math.floor(hitY * 8.0D), 0, 7));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        if (placer == null || !placer.isShiftKeyDown()) {
            return;
        }

        int height = state.getValue(HEIGHT);
        if (height == 0 && hasTopRoom(level, pos.below())) {
            level.setBlock(pos, state.setValue(HEIGHT, 9), 3);
        } else if (height == 7 && hasBottomRoom(level, pos.above())) {
            level.setBlock(pos, state.setValue(HEIGHT, 8), 3);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        int height = state.getValue(HEIGHT);
        if ((height == 9 && !hasTopRoom(level, pos.below())) || (height == 8 && !hasBottomRoom(level, pos.above()))) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.wide && context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                return Shapes.empty();
            }
        }
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeFor(state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!this.wide || (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb))) {
            return;
        }

        if (entity.getY() < pos.getY() + getY(state.getValue(HEIGHT)) + 0.375D) {
            entity.setDeltaMovement(Vec3.ZERO.add(0.0D, -0.25D, 0.0D));
            entity.setPos(entity.getX(), entity.getY() - 0.125D, entity.getZ());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.wide) {
            tooltip.add(Component.translatable("block.reinhardtshbm.steel_grate_wide.desc").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static double getY(int height) {
        if (height == 9) {
            return -0.125D;
        }
        return height * 0.125D;
    }

    private VoxelShape shapeFor(BlockState state) {
        return this.wide ? WIDE_SHAPES[state.getValue(HEIGHT)] : SHAPES[state.getValue(HEIGHT)];
    }

    private static boolean hasTopRoom(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.bounds().maxY < 0.95D;
    }

    private static boolean hasBottomRoom(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.bounds().minY > 0.05D;
    }
}
