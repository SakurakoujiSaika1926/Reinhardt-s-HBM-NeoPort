package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.reinhardt.hbm.entity.LegacyTaintCrabEntity;
import com.reinhardt.hbm.entity.LegacyTeslaCrabEntity;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 1.7.10 TileEntityTesla energy, targeting, and network synchronization. */
public final class TeslaCoilBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final long MAX_POWER = 100_000L;
    public static final long ENERGY_PER_TICK = 5_000L;
    public static final double RANGE = 10.0D;
    public static final double EMITTER_OFFSET = 1.75D;

    private long power;
    /** Immutable publication consumed by both the render and section-compile threads. */
    private volatile List<Vec3> targets = List.of();
    private volatile RenderSnapshot renderSnapshot;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.TESLA_COIL.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(state, emitter(), List.of(), 0L,
                new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0D, pos.getY() + 1.9375D, pos.getZ() + 1.0D));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeslaCoilBlockEntity tesla) {
        if (level.isClientSide) {
            if (!tesla.renderSnapshot.targets().equals(tesla.targets)
                    || tesla.renderSnapshot.blockState() != state) {
                tesla.publishRenderSnapshot(true);
            } else {
                tesla.publishClientTime(level.getGameTime());
            }
            return;
        }
        long powerBefore = tesla.power;
        PowerNetworkManager.tickFromEndpoint(level, tesla);
        if (level.getBlockState(pos.below()).is(HbmBlocks.METEOR_BATTERY.get())) {
            tesla.power = MAX_POWER;
        }

        List<Vec3> nextTargets = List.of();
        if (tesla.power >= ENERGY_PER_TICK) {
            tesla.power -= ENERGY_PER_TICK;
            nextTargets = zap(level, tesla.emitter(), RANGE, null);
        }
        if (tesla.power != powerBefore || !tesla.targets.equals(nextTargets)) {
            tesla.targets = nextTargets;
            tesla.sync();
        }
    }

    public Vec3 emitter() {
        return new Vec3(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + EMITTER_OFFSET, this.worldPosition.getZ() + 0.5D);
    }

    public List<Vec3> targets() {
        return this.targets;
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    public static List<Vec3> zap(Level level, Vec3 origin, double range, @Nullable LivingEntity source) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(range));
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Vec3> result = new ArrayList<>();
        for (LivingEntity target : candidates) {
            if (!isValidTarget(target, source)) {
                continue;
            }
            Vec3 targetPoint = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            if (origin.distanceTo(targetPoint) > range || obstructed(level, origin, targetPoint, source)) {
                continue;
            }

            if (target instanceof LegacyTaintCrabEntity) {
                result.add(new Vec3(target.getX(), target.getY() + 1.25D, target.getZ()));
                target.heal(15.0F);
                continue;
            }
            if (target instanceof LegacyTeslaCrabEntity) {
                result.add(new Vec3(target.getX(), target.getY() + 1.0D, target.getZ()));
                target.heal(10.0F);
                continue;
            }
            if (target instanceof LegacyCyberCrabEntity) {
                result.add(targetPoint);
                continue;
            }
            if (target instanceof Creeper creeper) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                        && net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel) instanceof net.minecraft.world.entity.LightningBolt lightning) {
                    lightning.moveTo(target.position());
                    creeper.thunderHit(serverLevel, lightning);
                }
                result.add(targetPoint);
                continue;
            }

            float damage = (float) Math.clamp(target.getMaxHealth() * 0.5D, 3.0D, 20.0D) / candidates.size();
            if (!(target instanceof Player && HbmArmorProtection.hasFaradayProtection(target))
                    && target.hurt(level.damageSources().source(HbmDamageTypes.ELECTRICITY), damage)) {
                level.playSound(null, target.blockPosition(), com.reinhardt.hbm.registry.HbmSoundEvents.WEAPON_TESLA.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            result.add(targetPoint);
        }
        return List.copyOf(result);
    }

    /** Client-side target collection for the entity arc renderers. */
    public static List<Vec3> targetPoints(Level level, Vec3 origin, double range, @Nullable LivingEntity source) {
        List<Vec3> result = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(range))) {
            if (!isValidTarget(target, source)) {
                continue;
            }
            Vec3 targetPoint = target instanceof LegacyTaintCrabEntity
                    ? new Vec3(target.getX(), target.getY() + 1.25D, target.getZ())
                    : target instanceof LegacyTeslaCrabEntity
                    ? new Vec3(target.getX(), target.getY() + 1.0D, target.getZ())
                    : new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            if (origin.distanceTo(targetPoint) <= range && !obstructed(level, origin, targetPoint, source)) {
                result.add(targetPoint);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isValidTarget(LivingEntity target, @Nullable LivingEntity source) {
        return target != source && !(target instanceof Cat) && target.isAlive();
    }

    private static boolean obstructed(Level level, Vec3 origin, Vec3 target, @Nullable LivingEntity source) {
        HitResult result = level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return result.getType() != HitResult.Type.MISS;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(6);
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            connectors.add(this.worldPosition.relative(direction));
        }
        return List.copyOf(connectors);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("block.reinhardtshbm.tesla", this.power, MAX_POWER);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", this.power);
        tag.put("Targets", saveTargets());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = Math.clamp(tag.getLong("Power"), 0L, MAX_POWER);
        this.targets = loadTargets(tag.getList("Targets", Tag.TAG_COMPOUND));
        if (this.level != null && this.level.isClientSide) {
            publishRenderSnapshot(true);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private ListTag saveTargets() {
        ListTag tag = new ListTag();
        for (Vec3 target : this.targets) {
            CompoundTag point = new CompoundTag();
            point.putDouble("X", target.x);
            point.putDouble("Y", target.y);
            point.putDouble("Z", target.z);
            tag.add(point);
        }
        return tag;
    }

    private static List<Vec3> loadTargets(ListTag tag) {
        List<Vec3> loaded = new ArrayList<>(tag.size());
        for (int index = 0; index < tag.size(); index++) {
            CompoundTag point = tag.getCompound(index);
            loaded.add(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        return List.copyOf(loaded);
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void publishClientTime(long gameTime) {
        RenderSnapshot previous = this.renderSnapshot;
        if (previous.gameTime() != gameTime) {
            this.renderSnapshot = new RenderSnapshot(previous.blockState(), previous.emitter(),
                    previous.targets(), gameTime, previous.bounds());
        }
    }

    private void publishRenderSnapshot(boolean rebuildClientSection) {
        RenderSnapshot previous = this.renderSnapshot;
        BlockState state = getBlockState();
        Vec3 emitter = emitter();
        List<Vec3> immutableTargets = List.copyOf(this.targets);
        AABB bounds = calculateRenderBounds(emitter, immutableTargets);
        RenderSnapshot next = new RenderSnapshot(state, emitter, immutableTargets,
                this.level == null ? 0L : this.level.getGameTime(), bounds);
        this.renderSnapshot = next;
        if (rebuildClientSection && this.level != null && this.level.isClientSide
                && previous != null && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private AABB calculateRenderBounds(Vec3 origin, List<Vec3> targetPoints) {
        // tesla.obj after (+0.5, 0, +0.5) * rotateY(180); face-used baked
        // vertices remain within this exact float envelope.
        double[] bounds = {
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1.0D, worldPosition.getY() + 1.9375D, worldPosition.getZ() + 1.0D
        };
        for (Vec3 target : targetPoints) {
            includeBeamTimeEnvelope(bounds, target.subtract(origin));
        }
        return new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }

    /** Exact all-phase envelope for the random beam's authored offsets. */
    private void includeBeamTimeEnvelope(double[] bounds, Vec3 end) {
        double length = end.length();
        if (length <= 1.0E-5D) {
            return;
        }
        Vec3 axis = end.scale(1.0D / length);
        Vec3 side = new Vec3(axis.z, 0.0D, -axis.x);
        side = side.lengthSqr() < 1.0E-5D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        Vec3 vertical = side.cross(axis).normalize();
        double quad = 0.03125D;
        includeRenderedLocal(bounds, side.scale(quad).add(vertical.scale(quad)));
        includeRenderedLocal(bounds, side.scale(-quad).add(vertical.scale(quad)));
        includeRenderedLocal(bounds, vertical.scale(quad));
        includeRenderedLocal(bounds, vertical.scale(-quad));
        includeRenderedLocal(bounds, end.subtract(side.scale(quad)).subtract(vertical.scale(quad)));
        includeRenderedLocal(bounds, end.add(side.scale(quad)).subtract(vertical.scale(quad)));
        includeRenderedLocal(bounds, end.subtract(vertical.scale(quad)));
        includeRenderedLocal(bounds, end.add(vertical.scale(quad)));
        int segments = Math.max(1, (int) Math.ceil(length * 5.0D));
        double interiorRadius = 0.15625D;
        for (int index = 1; index < segments; index++) {
            Vec3 center = end.scale(index / (double) segments);
            double dx = interiorRadius * (Math.abs(side.x) + Math.abs(vertical.x));
            double dy = interiorRadius * (Math.abs(side.y) + Math.abs(vertical.y));
            double dz = interiorRadius * (Math.abs(side.z) + Math.abs(vertical.z));
            includeRenderedLocal(bounds, center.x - dx, center.y - dy, center.z - dz);
            includeRenderedLocal(bounds, center.x + dx, center.y + dy, center.z + dz);
        }
    }

    private void includeRenderedLocal(double[] bounds, Vec3 point) {
        includeRenderedLocal(bounds, point.x, point.y, point.z);
    }

    /** Mirrors vertex(float,float,float) followed by the PoseStack translation. */
    private void includeRenderedLocal(double[] bounds, double x, double y, double z) {
        float renderedX = (float) x + 0.5F;
        float renderedY = (float) y + (float) EMITTER_OFFSET;
        float renderedZ = (float) z + 0.5F;
        double worldX = worldPosition.getX() + renderedX;
        double worldY = worldPosition.getY() + renderedY;
        double worldZ = worldPosition.getZ() + renderedZ;
        bounds[0] = Math.min(bounds[0], worldX);
        bounds[1] = Math.min(bounds[1], worldY);
        bounds[2] = Math.min(bounds[2], worldZ);
        bounds[3] = Math.max(bounds[3], worldX);
        bounds[4] = Math.max(bounds[4], worldY);
        bounds[5] = Math.max(bounds[5], worldZ);
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return section(first.minX) == section(second.minX)
                && section(inclusiveMaximum(first.minX, first.maxX)) == section(inclusiveMaximum(second.minX, second.maxX))
                && section(first.minY) == section(second.minY)
                && section(inclusiveMaximum(first.minY, first.maxY)) == section(inclusiveMaximum(second.minY, second.maxY))
                && section(first.minZ) == section(second.minZ)
                && section(inclusiveMaximum(first.minZ, first.maxZ)) == section(inclusiveMaximum(second.minZ, second.maxZ));
    }

    private static int section(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }

    public record RenderSnapshot(BlockState blockState, Vec3 emitter, List<Vec3> targets,
                                 long gameTime, AABB bounds) {
        public RenderSnapshot {
            targets = List.copyOf(targets);
        }
    }
}
