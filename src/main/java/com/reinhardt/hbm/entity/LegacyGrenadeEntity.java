package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Direct port of EntityGrenadeBouncyGeneric. The old entity only collided with
 * blocks, reflected the impacted motion axis, and exploded after its item fuse.
 */
public final class LegacyGrenadeEntity extends Entity {
    private static final int FUSE_TICKS = 60;
    private static final float THROW_SPEED = 1.5F;
    private static final float BOUNCE_MODIFIER = 0.5F;
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(LegacyGrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE =
            SynchedEntityData.defineId(LegacyGrenadeEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;

    public LegacyGrenadeEntity(EntityType<? extends LegacyGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public LegacyGrenadeEntity(Level level, Player owner, Kind kind) {
        this(HbmEntityTypes.LEGACY_GRENADE.get(), level);
        setKind(kind);
        this.ownerId = owner.getUUID();
        setFuse(kind.fuseTicks());
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        Vec3 direction = owner.getLookAngle();
        setPos(getX() - direction.x * 0.16D, getY(), getZ() - direction.z * 0.16D);
        Vec3 motion = direction.scale(THROW_SPEED).add(
                random.nextGaussian() * 0.0075D,
                random.nextGaussian() * 0.0075D,
                random.nextGaussian() * 0.0075D
        );
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    public Kind kind() {
        return Kind.byOrdinal(entityData.get(KIND));
    }

    public ItemStack renderStack() {
        return new ItemStack(kind() == Kind.FISHING ? HbmItems.STICK_DYNAMITE_FISHING.get() : HbmItems.STICK_DYNAMITE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // The old entity synchronised its grenade ItemStack. Modern component
        // data carries the equivalent kind and fuse explicitly for both sides.
        builder.define(KIND, Kind.DYNAMITE.ordinal());
        builder.define(FUSE, FUSE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement();
        HitResult hit = level().clip(new ClipContext(position(), position().add(motion),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit instanceof BlockHitResult blockHit) {
            setPos(blockHit.getLocation());
            motion = reflect(motion, blockHit.getDirection()).scale(BOUNCE_MODIFIER);
            if (!level().isClientSide && motion.length() > 0.05D) {
                level().playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_HIT,
                        SoundSource.NEUTRAL, 2.0F, 1.0F);
            }
        } else {
            move(MoverType.SELF, motion);
        }

        updateRotation(motion);
        if (isInWater() && level().isClientSide) {
            for (int index = 0; index < 4; index++) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        getX() - motion.x * 0.25D, getY() - motion.y * 0.25D, getZ() - motion.z * 0.25D,
                        motion.x, motion.y, motion.z);
            }
        }
        setDeltaMovement(motion.scale(isInWater() ? 0.8D : 0.99D).add(0.0D, -0.03D, 0.0D));

        if (!level().isClientSide && setFuseAndCheck()) {
            detonate();
        }
    }

    private void detonate() {
        Vec3 detonation = position();
        if (kind() == Kind.DYNAMITE) {
            // ItemGrenadeDynamite: VNT radius 5, player range modifier 15, weapon SFX.
            LegacyProjectileUtil.standardExplosion(this, detonation, 5.0F, 15.0F, true, true, false);
            LegacyProjectileUtil.sendSmallExplosionEffect(level(), detonation, 10, 2.5F, 1.0F);
        } else {
            // ItemGrenadeFishing: vanilla radius 3, explicitly no fire or terrain damage.
            level().explode(this, getX(), getY() + 0.25D, getZ(), 3.0F, false, Level.ExplosionInteraction.NONE);
            if (level() instanceof ServerLevel serverLevel) {
                spawnFishingLoot(serverLevel);
            }
        }
        discard();
    }

    private void spawnFishingLoot(ServerLevel level) {
        for (int index = 0; index < 15; index++) {
            BlockPos pos = BlockPos.containing(
                    Mth.floor(getX()) + random.nextInt(15) - 7,
                    Mth.floor(getY()) + random.nextInt(15) - 7,
                    Mth.floor(getZ()) + random.nextInt(15) - 7
            );
            if (!level.getBlockState(pos).is(Blocks.WATER)) {
                continue;
            }
            LootParams.Builder parameters = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD));
            Player owner = owner();
            if (owner != null) {
                parameters.withOptionalParameter(LootContextParams.THIS_ENTITY, owner);
            }
            List<ItemStack> loot = level.getServer().reloadableRegistries()
                    .getLootTable(BuiltInLootTables.FISHING)
                    .getRandomItems(parameters.create(LootContextParamSets.FISHING));
            for (ItemStack stack : loot) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack, 1.0F).setDeltaMovement(0.0D, 1.0D, 0.0D);
                }
            }
        }
    }

    private Player owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerId);
    }

    private static Vec3 reflect(Vec3 motion, Direction side) {
        return switch (side.getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
    }

    private void updateRotation(Vec3 motion) {
        if (motion.lengthSqr() < 1.0E-8D) {
            return;
        }
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot(getXRot() - (float) (motion.length() * 25.0D));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("kind", kind().id);
        tag.putInt("fuse", fuse());
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(Kind.byId(tag.getString("kind")));
        setFuse(tag.getInt("fuse"));
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }

    private int fuse() {
        return entityData.get(FUSE);
    }

    private void setFuse(int value) {
        entityData.set(FUSE, value);
    }

    private boolean setFuseAndCheck() {
        int next = fuse() - 1;
        setFuse(next);
        return next <= 0;
    }

    private void setKind(Kind value) {
        entityData.set(KIND, value.ordinal());
    }

    public enum Kind {
        DYNAMITE("dynamite", FUSE_TICKS),
        FISHING("fishing", FUSE_TICKS);

        private final String id;
        private final int fuseTicks;

        Kind(String id, int fuseTicks) {
            this.id = id;
            this.fuseTicks = fuseTicks;
        }

        private int fuseTicks() {
            return fuseTicks;
        }

        private static Kind byId(String id) {
            for (Kind value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return DYNAMITE;
        }

        private static Kind byOrdinal(int ordinal) {
            Kind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DYNAMITE;
        }
    }
}
