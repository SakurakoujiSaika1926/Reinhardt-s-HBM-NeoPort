package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.api.entity.LegacyRadarDetectable;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class LegacyArtilleryShellEntity extends Entity implements LegacyRadarDetectable {
    private static final EntityDataAccessor<Integer> SHELL_TYPE =
            SynchedEntityData.defineId(LegacyArtilleryShellEntity.class, EntityDataSerializers.INT);
    private static final double GRAVITY = 9.81D * 0.05D;

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean shouldWhistle;
    private boolean didWhistle;
    private ItemStack cargo = ItemStack.EMPTY;
    private boolean inGround;
    private int ticksInGround;
    private BlockPos stuckBlockPos = BlockPos.ZERO;
    private BlockState stuckBlockState;
    private Direction stuckSide = Direction.UP;
    private int forcedChunkX = Integer.MIN_VALUE;
    private int forcedChunkZ = Integer.MIN_VALUE;
    private double syncPosX = Double.NaN;
    private double syncPosY = Double.NaN;
    private double syncPosZ = Double.NaN;
    private double syncYaw;
    private double syncPitch;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private int turnProgress;

    public LegacyArtilleryShellEntity(EntityType<? extends LegacyArtilleryShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LegacyArtilleryShellEntity(Level level, Vec3 position, Vec3 velocity, int shellType, Vec3 target, boolean whistle) {
        this(HbmEntityTypes.LEGACY_ARTILLERY_SHELL.get(), level);
        setPos(position.x, position.y, position.z);
        setShellType(shellType);
        setDeltaMovement(velocity);
        setTarget(target.x, target.y, target.z);
        this.shouldWhistle = whistle;
        updateRotationFromMotion(velocity);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHELL_TYPE, 0);
    }

    public LegacyProjectileUtil.ArtyType shellType() {
        return LegacyProjectileUtil.ArtyType.byModelData(this.entityData.get(SHELL_TYPE));
    }

    public void setShellType(int shellType) {
        this.entityData.set(SHELL_TYPE, shellType);
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void setCargo(ItemStack stack) {
        this.cargo = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        if (level().isClientSide) {
            tickClientInterpolation();
            return;
        }
        if (this.inGround) {
            tickInGround();
            return;
        }

        Vec3 motion = getDeltaMovement();
        updateRotationFromMotion(motion);
        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);
        HitResult hit = entityHit != null ? entityHit : blockHit;
        if (hit.getType() != HitResult.Type.MISS) {
            if (impact(hit)) {
                return;
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        if (isInWater()) {
            motion = motion.scale(0.8D);
        }
        // EntityThrowableNT applies its air drag every tick (the legacy
        // default is 0.99), including artillery shells that are not in water.
        motion = motion.scale(0.99D);
        setDeltaMovement(motion.add(0.0D, -GRAVITY, 0.0D));
        maybeWhistle();
        updateForcedChunk();
        maybeClusterSplit();
    }

    private void tickClientInterpolation() {
        if (this.turnProgress > 0) {
            double x = getX() + (this.syncPosX - getX()) / this.turnProgress;
            double y = getY() + (this.syncPosY - getY()) / this.turnProgress;
            double z = getZ() + (this.syncPosZ - getZ()) / this.turnProgress;
            double yawDelta = Mth.wrapDegrees(this.syncYaw - getYRot());
            setYRot((float) (getYRot() + yawDelta / this.turnProgress));
            setXRot((float) (getXRot() + (this.syncPitch - getXRot()) / this.turnProgress));
            this.turnProgress--;
            setPos(x, y, z);
        } else {
            setPos(getX(), getY(), getZ());
        }
        if (Double.isFinite(this.syncPosX)
                && new Vec3(this.syncPosX - getX(), this.syncPosY - getY(), this.syncPosZ - getZ()).length() < 0.2D) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    getX(), getY() + 0.5D, getZ(), 0.0D, 0.1D, 0.0D);
        }
    }

    private void tickInGround() {
        if (level().isClientSide) {
            return;
        }
        if (this.stuckBlockState != null && level().getBlockState(this.stuckBlockPos).is(this.stuckBlockState.getBlock())) {
            this.ticksInGround++;
            int despawn = groundDespawn();
            if (despawn > 0 && this.ticksInGround >= despawn) {
                discard();
            }
            return;
        }
        this.inGround = false;
        this.ticksInGround = 0;
        this.stuckBlockState = null;
        setDeltaMovement(
                getDeltaMovement().x * (level().random.nextFloat() * 0.2F),
                getDeltaMovement().y * (level().random.nextFloat() * 0.2F),
                getDeltaMovement().z * (level().random.nextFloat() * 0.2F)
        );
    }

    private void maybeWhistle() {
        if (this.didWhistle || !this.shouldWhistle) {
            return;
        }
        Vec3 motion = getDeltaMovement();
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double dist = Math.sqrt((getX() - this.targetX) * (getX() - this.targetX) + (getZ() - this.targetZ) * (getZ() - this.targetZ));
        if (horizontalSpeed * 18.0D > dist) {
            level().playSound(null, this.targetX, this.targetY, this.targetZ, HbmSoundEvents.TURRET_MORTAR_WHISTLE.get(), SoundSource.HOSTILE, 15.0F, 0.9F + level().random.nextFloat() * 0.2F);
            this.didWhistle = true;
        }
    }

    private void maybeClusterSplit() {
        LegacyProjectileUtil.ArtyType type = shellType();
        if (type != LegacyProjectileUtil.ArtyType.PHOSPHORUS_MULTI && type != LegacyProjectileUtil.ArtyType.MINI_NUKE_MULTI) {
            return;
        }
        if (!this.shouldWhistle || getDeltaMovement().y > 0.0D || this.targetY + 300.0D < getY()) {
            return;
        }
        int childType = type == LegacyProjectileUtil.ArtyType.PHOSPHORUS_MULTI
                ? LegacyProjectileUtil.ArtyType.PHOSPHORUS.modelData()
                : LegacyProjectileUtil.ArtyType.MINI_NUKE.modelData();
        int amount = type == LegacyProjectileUtil.ArtyType.PHOSPHORUS_MULTI ? 10 : 5;
        double deviation = 5.0D;
        Vec3 motion = getDeltaMovement();
        for (int i = 0; i < amount; i++) {
            Vec3 childMotion = i == 0 ? motion : motion.add(level().random.nextGaussian() * deviation, 0.0D, level().random.nextGaussian() * deviation);
            LegacyArtilleryShellEntity child = new LegacyArtilleryShellEntity(level(), position(), childMotion, childType, new Vec3(this.targetX, this.targetY, this.targetZ), this.shouldWhistle && !this.didWhistle);
            level().addFreshEntity(child);
        }
        if (level() instanceof ServerLevel serverLevel) {
            double rangeSquared = 500.0D * 500.0D;
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(position()) <= rangeSquared) {
                    serverLevel.sendParticles(player, com.reinhardt.hbm.registry.HbmParticleTypes.LEGACY_PLASMA_BLAST.get(), true,
                            getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        discard();
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity closest = null;
        Vec3 closestHit = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable())) {
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(optionalHit.get());
            if (distance < closestDistance) {
                closest = entity;
                closestHit = optionalHit.get();
                closestDistance = distance;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestHit);
    }

    private boolean impact(HitResult hit) {
        if (level().isClientSide) {
            discard();
            return true;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LegacyArtilleryShellEntity) {
            return false;
        }
        if (shellType() == LegacyProjectileUtil.ArtyType.CARGO) {
            if (hit instanceof BlockHitResult blockHit) {
                stickInBlock(blockHit);
                return true;
            }
            return false;
        }
        Vec3 effectPos = hit.getLocation();
        Vec3 pos = effectPos;
        Vec3 backstep = getDeltaMovement().normalize();
        if (Double.isFinite(backstep.x) && Double.isFinite(backstep.y) && Double.isFinite(backstep.z)) {
            pos = pos.subtract(backstep);
        }
        switch (shellType()) {
            case NORMAL -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 10.0F, 3.0F, false, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        10, 2.0F, 0.5F, 25.0F, 5, 0, 20, 0.75F, 1.0F, -2.0F, 150.0F);
            }
            case CLASSIC -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 15.0F, 5.0F, false, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        15, 5.0F, 1.0F, 45.0F, 10, 0, 50, 1.0F, 3.0F, -2.0F, 200.0F);
            }
            case HE -> {
                LegacyProjectileUtil.standardExplosion(this, pos, 15.0F, 3.0F, true, true, false);
                LegacyProjectileUtil.composeExplosionEffect(level(), effectPos,
                        15, 5.0F, 1.0F, 45.0F, 10, 16, 50, 1.0F, 3.0F, -2.0F, 200.0F);
            }
            case MINI_NUKE, MINI_NUKE_MULTI -> LegacyProjectileUtil.promptNuke(this, pos, false);
            case NUKE -> LegacyProjectileUtil.promptNuke(this, effectPos, true);
            case PHOSPHORUS, PHOSPHORUS_MULTI -> LegacyProjectileUtil.phosphorus(
                    this, pos, effectPos, 15, 12, 10.0F, 15, 5, 10.0D, 10.0F);
            case CHLORINE -> LegacyProjectileUtil.gas(this, effectPos, LegacyMistEntity.MistType.CHLORINE);
            case PHOSGENE -> LegacyProjectileUtil.gas(this, effectPos, LegacyMistEntity.MistType.PHOSGENE);
            case MUSTARD -> LegacyProjectileUtil.gas(this, effectPos, LegacyMistEntity.MistType.MUSTARD);
            case CARGO -> {}
        }
        discard();
        return true;
    }

    private void stickInBlock(BlockHitResult hit) {
        setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        setDeltaMovement(Vec3.ZERO);
        this.inGround = true;
        this.ticksInGround = 0;
        this.stuckBlockPos = hit.getBlockPos().immutable();
        this.stuckBlockState = level().getBlockState(this.stuckBlockPos);
        this.stuckSide = hit.getDirection();
    }

    private void updateRotationFromMotion(Vec3 motion) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < 1.0E-7D && Math.abs(motion.y) < 1.0E-7D) {
            return;
        }
        setYRot((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(motion.x, motion.z))));
        setXRot((float) Mth.wrapDegrees(-Math.toDegrees(Math.atan2(motion.y, horizontal))));
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.syncYaw = yRot;
        this.syncPitch = xRot;
        this.turnProgress = steps;
        setDeltaMovement(this.velocityX, this.velocityY, this.velocityZ);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.velocityX = x;
        this.velocityY = y;
        this.velocityZ = z;
        setDeltaMovement(x, y, z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("shell_type", this.entityData.get(SHELL_TYPE));
        tag.putDouble("target_x", this.targetX);
        tag.putDouble("target_y", this.targetY);
        tag.putDouble("target_z", this.targetZ);
        tag.putBoolean("should_whistle", this.shouldWhistle);
        tag.putBoolean("did_whistle", this.didWhistle);
        tag.putBoolean("in_ground", this.inGround);
        tag.putInt("ticks_in_ground", this.ticksInGround);
        tag.putInt("stuck_x", this.stuckBlockPos.getX());
        tag.putInt("stuck_y", this.stuckBlockPos.getY());
        tag.putInt("stuck_z", this.stuckBlockPos.getZ());
        tag.putInt("stuck_side", this.stuckSide.get3DDataValue());
        if (!this.cargo.isEmpty()) {
            tag.put("cargo", this.cargo.saveOptional(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(SHELL_TYPE, tag.getInt("shell_type"));
        this.targetX = tag.getDouble("target_x");
        this.targetY = tag.getDouble("target_y");
        this.targetZ = tag.getDouble("target_z");
        this.shouldWhistle = tag.getBoolean("should_whistle");
        this.didWhistle = tag.getBoolean("did_whistle");
        this.inGround = tag.getBoolean("in_ground");
        this.ticksInGround = tag.getInt("ticks_in_ground");
        this.stuckBlockPos = new BlockPos(tag.getInt("stuck_x"), tag.getInt("stuck_y"), tag.getInt("stuck_z"));
        this.stuckSide = Direction.from3DDataValue(tag.getInt("stuck_side"));
        if (this.inGround) {
            this.stuckBlockState = level().getBlockState(this.stuckBlockPos);
        }
        if (tag.contains("cargo")) {
            this.cargo = ItemStack.parseOptional(registryAccess(), tag.getCompound("cargo"));
        } else {
            this.cargo = ItemStack.EMPTY;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            if (!this.cargo.isEmpty()) {
                player.getInventory().add(this.cargo.copy());
                player.inventoryMenu.broadcastChanges();
            }
            discard();
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return false;
    }

    private int groundDespawn() {
        return this.cargo.isEmpty() ? 1200 : 0;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunk();
        super.remove(reason);
    }

    private void updateForcedChunk() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int chunkX = Mth.floor(getX()) >> 4;
        int chunkZ = Mth.floor(getZ()) >> 4;
        if (chunkX == this.forcedChunkX && chunkZ == this.forcedChunkZ) {
            return;
        }
        releaseForcedChunk(serverLevel);
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(serverLevel, this, chunkX, chunkZ, true, true);
        this.forcedChunkX = chunkX;
        this.forcedChunkZ = chunkZ;
    }

    private void releaseForcedChunk() {
        if (level() instanceof ServerLevel serverLevel) {
            releaseForcedChunk(serverLevel);
        }
    }

    private void releaseForcedChunk(ServerLevel serverLevel) {
        if (this.forcedChunkX == Integer.MIN_VALUE) {
            return;
        }
        HbmChunkTickets.ARTILLERY_PROJECTILES.forceChunk(
                serverLevel, this, this.forcedChunkX, this.forcedChunkZ, false, true);
        this.forcedChunkX = Integer.MIN_VALUE;
        this.forcedChunkZ = Integer.MIN_VALUE;
    }

    @Override
    public String translationKey() {
        return "gui.reinhardtshbm.radar.target.artillery";
    }

    @Override
    public int blipLevel() {
        return ARTY;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        // TileEntityMachineRadarNT's legacy converter used scanMissiles for shells.
        return params.scanMissiles();
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return getDeltaMovement().y < 0.0D;
    }
}
