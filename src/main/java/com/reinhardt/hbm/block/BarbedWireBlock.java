package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BarbedWireBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private static final VoxelShape SHAPE_EAST_WEST = SHAPE_NORTH_SOUTH;

    private final Kind kind;

    public BarbedWireBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        if (level.isClientSide) {
            return;
        }

        DamageSources sources = level.damageSources();
        switch (this.kind) {
            case NORMAL -> entity.hurt(sources.cactus(), 2.0F);
            case FIRE -> {
                entity.hurt(sources.cactus(), 2.0F);
                entity.igniteForSeconds(1.0F);
            }
            case POISON -> {
                entity.hurt(sources.cactus(), 2.0F);
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 2));
                }
            }
            case ACID -> {
                entity.hurt(sources.cactus(), 2.0F);
                if (entity instanceof Player player) {
                    damageArmor(player);
                }
            }
            case WITHER -> {
                entity.hurt(sources.cactus(), 3.0F);
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 5 * 20, 4));
                }
            }
            case ULTRADEATH -> {
                entity.hurt(sources.source(HbmDamageTypes.RADIATION), 5.0F);
                if (entity instanceof LivingEntity living) {
                    HbmLivingRadiation data = HbmLivingRadiation.get(living);
                    data.addRadiation(10.0F);
                    HbmLivingRadiation.set(living, data);
                }
            }
        }
    }

    private static void damageArmor(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(1, serverLevel, serverPlayer, item -> serverPlayer.onEquippedItemBroken(item, slot));
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public enum Kind {
        NORMAL,
        FIRE,
        POISON,
        ACID,
        WITHER,
        ULTRADEATH
    }
}
