package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.SellafieldBlockItem;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Radioactive Sellafield material with the six-stage 1.7.10 decay state. */
public final class SellafieldBlock extends Block {
    public static final int LEVELS = 6;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, LEVELS - 1);

    public SellafieldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(LEVEL);
        ChunkRadiationData.get(level).incrementRadiation(pos, 0.5D * (stage + 1));
        if (random.nextInt(stage == 0 ? 25 : 15) != 0) {
            return;
        }
        if (stage == 0) {
            level.setBlock(pos, HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, state.setValue(LEVEL, stage - 1), Block.UPDATE_ALL);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        int stage = state.getValue(LEVEL);
        float dose = stage < 5 ? stage + 1.0F : 10.0F;
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        data.addEnvironmentRadiation(dose);
        data.addRadiation(dose);
        HbmLivingRadiation.set(living, data);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (asItem() instanceof SellafieldBlockItem item) {
            return List.of(SellafieldBlockItem.stackFor(item, state.getValue(LEVEL)));
        }
        return super.getDrops(state, params);
    }
}
