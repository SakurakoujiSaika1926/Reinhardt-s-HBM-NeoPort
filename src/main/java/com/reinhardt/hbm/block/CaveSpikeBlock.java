package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.CaveSpikeBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.function.Supplier;

/** Direct port of BlockStalagmite: two material variants, no collision and support-dependent survival. */
public final class CaveSpikeBlock extends Block {
    public static final IntegerProperty MATERIAL = IntegerProperty.create("material", 0, 1);
    public static final String MATERIAL_TAG = "material";
    private static final VoxelShape EMPTY = Shapes.empty();
    private static final ResourceLocation ASBESTOS_POWDER = ResourceLocation.fromNamespaceAndPath("reinhardtshbm", "powder_asbestos");

    private final boolean hanging;
    private final Supplier<? extends Item> sulfur;

    public CaveSpikeBlock(Properties properties, boolean hanging, DeferredItem<Item> sulfur) {
        super(properties);
        this.hanging = hanging;
        this.sulfur = sulfur;
        this.registerDefaultState(this.stateDefinition.any().setValue(MATERIAL, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(MATERIAL, material(context.getItemInHand()));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = this.hanging ? Direction.UP : Direction.DOWN;
        BlockPos supportPos = pos.relative(supportDirection);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, supportDirection.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return EMPTY;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item item = state.getValue(MATERIAL) == 0
                ? sulfur.get()
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ASBESTOS_POWDER);
        return item == net.minecraft.world.item.Items.AIR ? List.of() : List.of(new ItemStack(item));
    }

    public static int material(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mth.clamp(tag.getInt(MATERIAL_TAG), 0, 1);
    }

    private ItemStack stackForState(BlockState state) {
        return this.asItem() instanceof CaveSpikeBlockItem item
                ? CaveSpikeBlockItem.stackFor(item, state.getValue(MATERIAL))
                : new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MATERIAL);
    }
}
