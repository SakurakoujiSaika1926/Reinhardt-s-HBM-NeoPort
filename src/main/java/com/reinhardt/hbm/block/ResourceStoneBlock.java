package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Shared behavior for the six independently registered legacy resource stones. */
public final class ResourceStoneBlock extends Block {
    public enum Kind {
        NORMAL,
        ASBESTOS,
        MALACHITE
    }

    private final Kind kind;

    public ResourceStoneBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.kind == Kind.MALACHITE) {
            ItemStack tool = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
            int fortune = tool == null || tool.isEmpty()
                    ? 0
                    : EnchantmentHelper.getItemEnchantmentLevel(
                            params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.FORTUNE), tool);
            ItemStack stack = HbmItems.variantStack(HbmItems.CHUNK_ORE_ITEMS, "malachite");
            stack.setCount(3 + fortune + params.getLevel().random.nextInt(fortune + 2));
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos,
                               BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                               ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && this.kind == Kind.ASBESTOS) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        boolean asbestos = this.kind == Kind.ASBESTOS;
        super.onBlockExploded(state, level, pos, explosion);
        if (asbestos && !level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }
}
