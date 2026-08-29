package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Six metadata variants from 1.7.10 BlockResourceStone. */
public final class ResourceStoneBlock extends LegacyVariantBlock {
    public static final int SULFUR = 0;
    public static final int ASBESTOS = 1;
    public static final int HEMATITE = 2;
    public static final int MALACHITE = 3;
    public static final int LIMESTONE = 4;
    public static final int BAUXITE = 5;

    public ResourceStoneBlock(Properties properties) {
        super(properties, 5);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(VARIANT) == MALACHITE) {
            ItemStack tool = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
            int fortune = tool == null || tool.isEmpty()
                    ? 0
                    : EnchantmentHelper.getItemEnchantmentLevel(
                            params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.FORTUNE), tool);
            ItemStack stack = com.reinhardt.hbm.item.LegacyVariantItem.stackFor(HbmItems.CHUNK_ORE.get(), "malachite");
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
        if (!level.isClientSide && state.getValue(VARIANT) == ASBESTOS) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }
}
