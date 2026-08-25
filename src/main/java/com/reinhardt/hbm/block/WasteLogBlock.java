package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class WasteLogBlock extends RotatedPillarBlock {
    private static final MapCodec<WasteLogBlock> CODEC = simpleCodec(WasteLogBlock::new);
    private final boolean frozen;

    public WasteLogBlock(Properties properties) {
        this(properties, false);
    }

    public WasteLogBlock(Properties properties, boolean frozen) {
        super(properties);
        this.frozen = frozen;
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        ServerLevel level = params.getLevel();
        RandomSource random = level.random;
        if (frozen) {
            return List.of(new ItemStack(Items.SNOWBALL, 2 + random.nextInt(3)));
        }
        ItemStack drop = random.nextInt(1000) == 0
                ? new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.reinhardt.hbm.ReinhardtsHBM.id("burnt_bark")))
                : new ItemStack(Items.CHARCOAL, 2 + random.nextInt(3));
        return List.of(drop);
    }
}
