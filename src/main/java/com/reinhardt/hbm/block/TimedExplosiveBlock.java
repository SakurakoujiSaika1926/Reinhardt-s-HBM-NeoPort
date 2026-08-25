package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Shared exact 1.7.10 BlockTNTBase behavior for Semtex and C-4. */
public class TimedExplosiveBlock extends Block {
    public static final BooleanProperty IGNITE_ON_BREAK = BooleanProperty.create("ignite_on_break");
    private static final int NORMAL_FUSE = 80;
    private static final int POP_FUSE = 20;

    private final TimedExplosiveEntity.Kind kind;

    public TimedExplosiveBlock(Properties properties, TimedExplosiveEntity.Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(IGNITE_ON_BREAK, false));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock()) && !level.isClientSide) {
            checkForIgnition(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            checkForIgnition(level, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(IGNITE_ON_BREAK)) {
            prime(level, pos, player, NORMAL_FUSE);
            level.removeBlock(pos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (!level.isClientSide) {
            prime(level, pos, explosion.getIndirectSourceEntity(), POP_FUSE / 2 + level.random.nextInt(POP_FUSE));
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide) {
                prime(level, pos, player, NORMAL_FUSE);
                level.removeBlock(pos, false);
                damageFlintAndSteel(stack, level, player, hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                level.removeBlock(pos, false);
                popResource(level, pos, new ItemStack(this));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (ScrewdriverItem.isScrewdriver(stack)) {
            if (!level.isClientSide) {
                boolean armed = !state.getValue(IGNITE_ON_BREAK);
                level.setBlock(pos, state.setValue(IGNITE_ON_BREAK, armed), Block.UPDATE_ALL);
                player.displayClientMessage(
                        Component.translatable(armed
                                        ? "block.reinhardtshbm.timed_explosive.ignite_on_break.enabled"
                                        : "block.reinhardtshbm.timed_explosive.ignite_on_break.disabled")
                                .withStyle(armed ? ChatFormatting.RED : ChatFormatting.GOLD),
                        false
                );
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof AbstractArrow arrow && arrow.isOnFire()) {
            prime(level, pos, arrow.getOwner(), NORMAL_FUSE);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 15;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return state.getValue(IGNITE_ON_BREAK) ? List.of() : super.getDrops(state, params);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IGNITE_ON_BREAK);
    }

    private void checkForIgnition(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos) || isAdjacentToFire(level, pos)) {
            prime(level, pos, null, NORMAL_FUSE);
            level.removeBlock(pos, false);
        }
    }

    private static boolean isAdjacentToFire(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remote detonation entry point used by the 1.7.10 detonator family.
     * Unlike a redstone ignition it always uses the normal fuse length.
     */
    public void detonate(Level level, BlockPos pos, Entity owner) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        prime(level, pos, owner, NORMAL_FUSE);
        level.removeBlock(pos, false);
    }

    private void prime(Level level, BlockPos pos, Entity owner, int fuse) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TimedExplosiveEntity entity = new TimedExplosiveEntity(
                serverLevel,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                owner,
                fuse,
                kind
        );
        serverLevel.addFreshEntity(entity);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void damageFlintAndSteel(ItemStack stack, Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || player.getAbilities().instabuild) {
            return;
        }
        net.minecraft.world.entity.EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, serverLevel, serverPlayer, item -> serverPlayer.onEquippedItemBroken(item, slot));
    }
}
