package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.LegacyDroneItem;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;

/**
 * The 1.7.10 delivery drone's autonomous movement, 18-slot cargo hold and
 * optional chunk-loading mode. Route instructions are supplied by drone
 * network nodes, while the entity itself owns the carried contents so that
 * breaking it cannot duplicate cargo.
 */
public class LegacyDeliveryDroneEntity extends Entity {
    private static final EntityDataAccessor<Byte> APPEARANCE =
            SynchedEntityData.defineId(LegacyDeliveryDroneEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> EXPRESS =
            SynchedEntityData.defineId(LegacyDeliveryDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHUNK_LOADING =
            SynchedEntityData.defineId(LegacyDeliveryDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> cargo = NonNullList.withSize(18, ItemStack.EMPTY);
    // EntityDroneBase used -1 as the unset sentinel for all three target
    // coordinates; in particular targetY == -1 deliberately disables flight.
    private double targetX = -1.0D;
    private double targetY = -1.0D;
    private double targetZ = -1.0D;
    private HbmFluidDefinition fluidType = HbmFluids.none();
    private int fluidAmount;
    private final Set<ChunkPos> forcedChunks = new HashSet<>();

    public LegacyDeliveryDroneEntity(EntityType<? extends LegacyDeliveryDroneEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(APPEARANCE, (byte) 0);
        builder.define(EXPRESS, false);
        builder.define(CHUNK_LOADING, false);
    }

    public void configure(boolean express, boolean chunkLoading) {
        entityData.set(EXPRESS, express);
        entityData.set(CHUNK_LOADING, chunkLoading);
    }

    public boolean express() {
        return entityData.get(EXPRESS);
    }

    public boolean chunkLoading() {
        return entityData.get(CHUNK_LOADING);
    }

    public int appearance() {
        return entityData.get(APPEARANCE);
    }

    public void setAppearance(int appearance) {
        entityData.set(APPEARANCE, (byte) Math.clamp(appearance, 0, 2));
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public boolean hasTarget() {
        return targetY != -1.0D;
    }

    public boolean isAtTarget() {
        return hasTarget() && position().distanceToSqr(targetX, targetY, targetZ) < 0.02D;
    }

    public ItemStack getCargo(int slot) {
        return slot >= 0 && slot < cargo.size() ? cargo.get(slot) : ItemStack.EMPTY;
    }

    public void setCargo(int slot, ItemStack stack) {
        if (slot >= 0 && slot < cargo.size()) {
            cargo.set(slot, stack.copy());
        }
    }

    public NonNullList<ItemStack> cargo() {
        return cargo;
    }

    public HbmFluidDefinition fluidType() {
        return fluidType;
    }

    public int fluidAmount() {
        return fluidAmount;
    }

    public void setFluidPayload(HbmFluidDefinition type, int amount) {
        fluidType = type == null ? HbmFluids.none() : type;
        fluidAmount = Math.max(0, amount);
        if (fluidAmount == 0 || fluidType.isNone()) {
            clearFluidPayload();
        }
    }

    public void clearFluidPayload() {
        fluidType = HbmFluids.none();
        fluidAmount = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickFlight();
            tickChunkLoading();
        } else {
            spawnExhaust();
        }
    }

    private void tickFlight() {
        if (!hasTarget()) {
            return;
        }
        Vec3 toward = new Vec3(targetX - getX(), targetY - getY(), targetZ - getZ());
        double distance = toward.length();
        if (distance < 0.02D) {
            setPos(targetX, targetY, targetZ);
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double speed = express() ? 1.125D : 0.375D;
        Vec3 movement = toward.scale(Math.min(speed, distance) / distance);
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        if (horizontalCollision) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 1.0D, 0.0D));
            move(MoverType.SELF, getDeltaMovement());
        }
        if (movement.lengthSqr() > 1.0E-8D) {
            yRotO = getYRot();
            setYRot((float) Math.toDegrees(Math.atan2(movement.x, movement.z)));
        }
    }

    private void tickChunkLoading() {
        if (!chunkLoading() || !(level() instanceof ServerLevel serverLevel)) {
            releaseForcedChunks();
            return;
        }
        releaseForcedChunks();
        Vec3 motion = getDeltaMovement();
        int x1 = Mth.floor(getX());
        int z1 = Mth.floor(getZ());
        int x0 = Mth.floor(getX() - motion.x);
        int z0 = Mth.floor(getZ() - motion.z);
        for (ChunkPos chunk : chunksAlongLineSegment(x0, z0, x1, z1, 8.0D)) {
            HbmChunkTickets.DELIVERY_DRONES.forceChunk(serverLevel, this, chunk.x, chunk.z, true, true);
            forcedChunks.add(chunk);
        }
    }

    private void releaseForcedChunks() {
        if (!(level() instanceof ServerLevel serverLevel) || forcedChunks.isEmpty()) {
            return;
        }
        for (ChunkPos chunk : forcedChunks) {
            HbmChunkTickets.DELIVERY_DRONES.forceChunk(serverLevel, this, chunk.x, chunk.z, false, true);
        }
        forcedChunks.clear();
    }

    /**
     * Direct port of 1.7.10 ChunkShapeHelper.getChunksAlongLineSegment.
     * The eight-block padding is measured against chunk-box corners, not a
     * generic square around the drone.
     */
    private static Set<ChunkPos> chunksAlongLineSegment(int x0, int z0, int x1, int z1, double padding) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dz = -Math.abs(z1 - z0);
        int sz = z0 < z1 ? 1 : -1;
        int error = dx + dz;
        int originalX = x0;
        int originalZ = z0;
        Set<ChunkPos> out = new HashSet<>();
        Set<ChunkPos> checked = new HashSet<>();
        while (true) {
            ChunkPos current = new ChunkPos(x0 >> 4, z0 >> 4);
            out.add(current);
            int[][] neighbors = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
            for (int[] neighbor : neighbors) {
                ChunkPos candidate = new ChunkPos(current.x + neighbor[0], current.z + neighbor[1]);
                if (!checked.add(candidate) || out.contains(candidate)) {
                    continue;
                }
                if (boxLineDistance(originalX, originalZ, x1, z1,
                        candidate.x * 16, candidate.z * 16) < padding) {
                    out.add(candidate);
                }
            }
            int e2 = 2 * error;
            if (e2 >= dz) {
                if (x0 == x1) {
                    break;
                }
                error += dz;
                x0 += sx;
            }
            if (e2 <= dx) {
                if (z0 == z1) {
                    break;
                }
                error += dx;
                z0 += sz;
            }
        }
        return out;
    }

    private static double boxLineDistance(int lineX0, int lineZ0, int lineX1, int lineZ1,
                                          int boxX, int boxZ) {
        double min = Double.MAX_VALUE;
        int[][] corners = {{0, 0}, {0, 16}, {16, 0}, {16, 16}};
        for (int[] corner : corners) {
            min = Math.min(min, pointSegmentDistance(lineX0, lineZ0, lineX1, lineZ1,
                    boxX + corner[0], boxZ + corner[1]));
        }
        return min;
    }

    private static double pointSegmentDistance(int x1, int z1, int x2, int z2, int px, int pz) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        if (dx == 0 && dz == 0) {
            return Math.sqrt((double) (px - x1) * (px - x1) + (double) (pz - z1) * (pz - z1));
        }
        double t = ((px - x1) * (double) dx + (pz - z1) * (double) dz)
                / (double) (dx * dx + dz * dz);
        if (t < 0.0D) {
            return Math.sqrt((double) (px - x1) * (px - x1) + (double) (pz - z1) * (pz - z1));
        }
        if (t > 1.0D) {
            return Math.sqrt((double) (px - x2) * (px - x2) + (double) (pz - z2) * (pz - z2));
        }
        double projX = x1 + t * dx;
        double projZ = z1 + t * dz;
        return Math.sqrt((px - projX) * (px - projX) + (pz - projZ) * (pz - projZ));
    }

    private void spawnExhaust() {
        for (int direction = 0; direction < 4; direction++) {
            double x = direction < 2 ? (direction == 0 ? 1.125D : -1.125D) : 0.0D;
            double z = direction >= 2 ? (direction == 2 ? 1.125D : -1.125D) : 0.0D;
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    getX() + x, getY() + 0.75D, getZ() + z, 0.0D, -0.2D, 0.0D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (level().isClientSide || !(attacker instanceof Player) || isRemoved()) {
            return false;
        }
        dropContentsAndDrone();
        discard();
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunks();
        super.remove(reason);
    }

    private void dropContentsAndDrone() {
        for (ItemStack stack : cargo) {
            if (!stack.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack.copy()));
            }
        }
        LegacyDroneItem.Type type = express()
                ? (chunkLoading() ? LegacyDroneItem.Type.PATROL_EXPRESS_CHUNKLOADING : LegacyDroneItem.Type.PATROL_EXPRESS)
                : (chunkLoading() ? LegacyDroneItem.Type.PATROL_CHUNKLOADING : LegacyDroneItem.Type.PATROL);
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), LegacyDroneItem.stack(type, 1)));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("express", express());
        tag.putBoolean("chunk_loading", chunkLoading());
        tag.putByte("appearance", entityData.get(APPEARANCE));
        if (fluidAmount > 0 && !fluidType.isNone()) {
            tag.putString("fluid_type", fluidType.name());
            tag.putInt("fluid_amount", fluidAmount);
        }
        if (hasTarget()) {
            tag.putDouble("target_x", targetX);
            tag.putDouble("target_y", targetY);
            tag.putDouble("target_z", targetZ);
        }
        for (int slot = 0; slot < cargo.size(); slot++) {
            if (!cargo.get(slot).isEmpty()) {
                tag.put("cargo_" + slot, cargo.get(slot).saveOptional(registryAccess()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        configure(tag.getBoolean("express"), tag.getBoolean("chunk_loading"));
        entityData.set(APPEARANCE, tag.getByte("appearance"));
        setFluidPayload(HbmFluids.byName(tag.getString("fluid_type")).orElse(HbmFluids.none()), tag.getInt("fluid_amount"));
        if (tag.contains("target_y")) {
            setTarget(tag.getDouble("target_x"), tag.getDouble("target_y"), tag.getDouble("target_z"));
        }
        for (int slot = 0; slot < cargo.size(); slot++) {
            cargo.set(slot, tag.contains("cargo_" + slot)
                    ? ItemStack.parseOptional(registryAccess(), tag.getCompound("cargo_" + slot))
                    : ItemStack.EMPTY);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    /**
     * EntityDroneBase#canTriggerWalking returned false in 1.7.10.
     * This is the modern block-trigger equivalent; the drone must not
     * activate pressure plates or other walking-trigger blocks.
     */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 2304.0D;
    }
}
