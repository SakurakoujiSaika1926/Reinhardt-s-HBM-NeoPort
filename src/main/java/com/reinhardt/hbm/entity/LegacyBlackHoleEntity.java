package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Direct 1.7.10 EntityBlackHole port for missile impact use. */
public final class LegacyBlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(LegacyBlackHoleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RAGING =
            SynchedEntityData.defineId(LegacyBlackHoleEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean breaksBlocks = true;
    private int vortexTimer;

    public LegacyBlackHoleEntity(EntityType<? extends LegacyBlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void spawn(Level level, Vec3 position, float size, boolean noBreak) {
        if (level.isClientSide) {
            return;
        }
        LegacyBlackHoleEntity hole = new LegacyBlackHoleEntity(HbmEntityTypes.LEGACY_BLACK_HOLE.get(), level);
        hole.setPos(position.x, position.y, position.z);
        hole.entityData.set(SIZE, size);
        hole.breaksBlocks = !noBreak;
        level.addFreshEntity(hole);
    }

    /** EntityRagingVortex, produced when a singularity spark touches ground. */
    public static void spawnRaging(Level level, Vec3 position, float size) {
        if (level.isClientSide) {
            return;
        }
        LegacyBlackHoleEntity hole = new LegacyBlackHoleEntity(HbmEntityTypes.LEGACY_BLACK_HOLE.get(), level);
        hole.setPos(position.x, position.y, position.z);
        hole.entityData.set(SIZE, size);
        hole.entityData.set(RAGING, true);
        level.addFreshEntity(hole);
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public LegacyBlackHoleEntity noBreak() {
        this.breaksBlocks = false;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.5F);
        builder.define(RAGING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isRaging()) {
            vortexTimer++;
            if (vortexTimer <= 20) {
                vortexTimer -= 20;
            }
            float pulse = (float) (Math.sin(vortexTimer) * Math.PI / 20.0D) * 0.35F;
            float decay = 0.0F;
            if (random.nextInt(100) == 0) {
                decay = 0.1F;
                level().explode(null, getX(), getY(), getZ(), 10.0F, false, Level.ExplosionInteraction.TNT);
            }
            entityData.set(SIZE, size() - pulse - decay);
            if (size() <= 0.0F) {
                discard();
                return;
            }
        }
        if (!level().isClientSide && breaksBlocks) {
            consumeBlocks();
        }
        pullEntities();
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
        setDeltaMovement(getDeltaMovement().scale(0.99D));
    }

    private void consumeBlocks() {
        float size = size();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        for (int ray = 0; ray < size * 2.0F; ray++) {
            double phi = random.nextDouble() * Math.PI * 2.0D;
            double cosTheta = random.nextDouble() * 2.0D - 1.0D;
            double theta = Math.acos(cosTheta);
            Vec3 direction = new Vec3(
                    Math.sin(theta) * Math.cos(phi),
                    Math.sin(theta) * Math.sin(phi),
                    Math.cos(theta)
            );
            int length = (int) Math.ceil(size * 15.0F);
            for (int step = 0; step < length; step++) {
                BlockPos pos = BlockPos.containing(getX() + direction.x * step, getY() + direction.y * step, getZ() + direction.z * step);
                FluidState fluid = server.getFluidState(pos);
                if (!fluid.isEmpty()) {
                    server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    continue;
                }
                BlockState state = server.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                server.addFreshEntity(new MineRubbleEntity(server, pos.getX() + 0.5D, pos.getY(),
                        pos.getZ() + 0.5D, getDeltaMovement(), state));
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                break;
            }
        }
    }

    private void pullEntities() {
        float size = size();
        double range = size * 15.0D;
        AABB bounds = new AABB(getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range);
        List<Entity> entities = level().getEntities(this, bounds, Entity::isAlive);
        for (Entity target : entities) {
            if (target instanceof Player player && player.getAbilities().instabuild) {
                continue;
            }
            if (target instanceof LegacyBlackHoleEntity) {
                continue;
            }
            if (target instanceof FallingBlockEntity falling && !level().isClientSide && target.tickCount > 1) {
                BlockPos pos = falling.blockPosition();
                ((ServerLevel) level()).addFreshEntity(new MineRubbleEntity((ServerLevel) level(),
                        pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        target.getDeltaMovement(), falling.getBlockState()));
                target.discard();
                continue;
            }

            Vec3 delta = new Vec3(getX() - target.getX(), getY() - target.getY(), getZ() - target.getZ());
            double dist = delta.length();
            if (dist > range || dist < 1.0E-5D) {
                continue;
            }

            Vec3 pull = delta.normalize();
            if (!(target instanceof ItemEntity)) {
                pull = pull.yRot((float) Math.toRadians(15.0D));
            }
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x * 0.1D, pull.y * 0.2D, pull.z * 0.1D));
            target.hurtMarked = true;

            if (dist < size * 1.5D) {
                target.hurt(damageSources().source(HbmDamageTypes.BLACK_HOLE, this, null), 1000.0F);
                if (!(target instanceof LivingEntity)) {
                    target.discard();
                }
                if (!level().isClientSide && target instanceof ItemEntity item) {
                    var stack = item.getItem();
                    if (stack.isEmpty()) {
                        continue;
                    }
                    var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                    if ("pellet_antimatter".equals(itemId) || "flame_pony".equals(itemId)) {
                        discard();
                        level().explode(null, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.TNT);
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("size", size());
        tag.putBoolean("breaksBlocks", breaksBlocks);
        tag.putBoolean("raging", isRaging());
        tag.putInt("vortexTimer", vortexTimer);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SIZE, tag.getFloat("size"));
        breaksBlocks = !tag.contains("breaksBlocks") || tag.getBoolean("breaksBlocks");
        entityData.set(RAGING, tag.getBoolean("raging"));
        vortexTimer = tag.getInt("vortexTimer");
    }

    public boolean isRaging() {
        return entityData.get(RAGING);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000.0D;
    }
}
