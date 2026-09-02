package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.client.WandClientHooks;
import com.reinhardt.hbm.item.WandStructureBlockItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import com.reinhardt.hbm.worldgen.structure.StructureWandBlockTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

public class WandStructureBlock extends Block implements EntityBlock {
    public static final BooleanProperty LOAD = BooleanProperty.create("load");

    public WandStructureBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LOAD, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean load = context.getItemInHand().has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                && context.getItemInHand().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getBoolean("load");
        return defaultBlockState().setValue(LOAD, load);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackForState(state));
    }

    private ItemStack stackForState(BlockState state) {
        if (this.asItem() instanceof WandStructureBlockItem item) {
            return item.stack(state.getValue(LOAD));
        }
        return new ItemStack(this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandStructureBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof BlockItem blockItem
                && !HbmStructureIO.isStructureBlock(blockItem.getBlock())
                && level.getBlockEntity(pos) instanceof WandStructureBlockEntity structure) {
            if (!level.isClientSide) {
                structure.toggleBlacklist(StructureWandBlockTarget.key(blockItem.getBlock(), stack));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            openClientScreen(pos, state.getValue(LOAD));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openClientScreen(BlockPos pos, boolean load) {
        if (FMLEnvironment.dist.isClient()) {
            WandClientHooks.openStructure(pos, load);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOAD);
    }
}
