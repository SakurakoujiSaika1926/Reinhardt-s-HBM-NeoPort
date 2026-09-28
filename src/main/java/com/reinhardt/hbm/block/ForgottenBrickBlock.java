package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Exact state and wall-hole interaction port of {@code BlockForgottenBrick}. */
public final class ForgottenBrickBlock extends LegacyPillarBlockBase {
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    public ForgottenBrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Variant.DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (state.getValue(VARIANT) != Variant.HOLE || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(HbmItems.COAL_ETERNAL.get()));
            level.setBlock(pos, state.setValue(VARIANT, Variant.HOLE_EMPTY), UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public enum Variant implements StringRepresentable {
        DEFAULT("default"),
        BLACK_AND_WHITE("black_and_white"),
        NULLSTONE("nullstone"),
        HOLE("hole"),
        HOLE_EMPTY("hole_empty"),
        NULLROOM_WOOD("nullroom_wood"),
        NULLROOM_STONE("nullroom_stone");

        private final String serializedName;

        Variant(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
