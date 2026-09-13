package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.RubberBoatEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Predicate;

/** Direct modern equivalent of 1.7.10 ItemBoatRubber's water placement flow. */
public final class LegacyRubberBoatItem extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    public LegacyRubberBoatItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 view = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(
                player,
                player.getBoundingBox().expandTowards(view.scale(5.0D)).inflate(1.0D),
                ENTITY_PREDICATE
        );
        if (!entities.isEmpty()) {
            Vec3 eye = player.getEyePosition();
            for (Entity entity : entities) {
                AABB bounds = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (bounds.contains(eye)) {
                    return InteractionResultHolder.pass(stack);
                }
            }
        }

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
        // ItemBoatRubber lowered the placement block by one when the ray hit
        // a snow layer, then spawned at the block centre one block above it.
        if (level.getBlockState(hitPos).is(Blocks.SNOW)) {
            hitPos = hitPos.below();
        }
        Vec3 position = Vec3.atBottomCenterOf(hitPos).add(0.0D, 1.3D, 0.0D);
        RubberBoatEntity boat = new RubberBoatEntity(level, position.x, position.y, position.z);
        // Legacy boat yaw was quantized to the four cardinal directions.
        boat.setYRot(((Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3) - 1) * 90.0F);
        // The legacy item tested a bounding box contracted by 0.1 on every
        // side, not the full boat box.
        if (!level.noCollision(boat, boat.getBoundingBox().deflate(0.1D))) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                EntityType.<RubberBoatEntity>createDefaultStackConfig(serverLevel, stack, player).accept(boat);
            }
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, position);
            stack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
