package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exact gameplay port of the 1.7.10 custom-nuke drop upgrade. */
public final class LegacyFallingNukeEntity extends Entity {
    private static final EntityDataAccessor<Integer> FACING =
            SynchedEntityData.defineId(LegacyFallingNukeEntity.class, EntityDataSerializers.INT);

    private float tnt;
    private float nuke;
    private float hydro;
    private float amat;
    private float dirty;
    private float schrab;
    private float euph;

    public LegacyFallingNukeEntity(EntityType<? extends LegacyFallingNukeEntity> type, Level level) {
        super(type, level);
        setYRot(90.0F);
        setXRot(90.0F);
        yRotO = 90.0F;
        xRotO = 90.0F;
    }

    public static LegacyFallingNukeEntity create(ServerLevel level, Vec3 position, Direction facing,
                                                   float tnt, float nuke, float hydro, float amat,
                                                   float dirty, float schrab, float euph) {
        LegacyFallingNukeEntity entity = new LegacyFallingNukeEntity(HbmEntityTypes.LEGACY_FALLING_NUKE.get(), level);
        entity.setPos(position);
        entity.setFacing(facing);
        entity.tnt = tnt;
        entity.nuke = nuke;
        entity.hydro = hydro;
        entity.amat = amat;
        entity.dirty = dirty;
        entity.schrab = schrab;
        entity.euph = euph;
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FACING, Direction.NORTH.get2DDataValue());
    }

    public Direction facing() {
        return Direction.from2DDataValue(entityData.get(FACING));
    }

    public void setFacing(Direction facing) {
        if (!facing.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Falling custom nuke facing must be horizontal: " + facing);
        }
        entityData.set(FACING, facing.get2DDataValue());
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        xRotO = getXRot();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, Math.max(-1.0D, motion.y - 0.05D), motion.z * 0.99D);
        if (getXRot() > -75.0F) {
            setXRot(getXRot() - 2.0F);
        }

        if (!(level() instanceof ServerLevel serverLevel)) return;
        BlockPos impact = BlockPos.containing(getX(), getY(), getZ());
        if (!serverLevel.getBlockState(impact).isAir()) {
            NukeExplosionManager.scheduleCustomNuke(serverLevel, getX(), getY(), getZ(),
                    tnt, nuke, hydro, amat, dirty, schrab, euph);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Facing", (byte) facing().get2DDataValue());
        tag.putFloat("tnt", tnt);
        tag.putFloat("nuke", nuke);
        tag.putFloat("hydro", hydro);
        tag.putFloat("amat", amat);
        tag.putFloat("dirty", dirty);
        tag.putFloat("schrab", schrab);
        tag.putFloat("euph", euph);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFacing(Direction.from2DDataValue(tag.getByte("Facing")));
        tnt = tag.getFloat("tnt");
        nuke = tag.getFloat("nuke");
        hydro = tag.getFloat("hydro");
        amat = tag.getFloat("amat");
        dirty = tag.getFloat("dirty");
        schrab = tag.getFloat("schrab");
        euph = tag.getFloat("euph");
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 25_000.0D; }
}
