package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.logic.EntityWaypoint;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.network.MaxwellGibEffectPayload;
import com.reinhardt.hbm.pollution.HbmPollutionData;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.worldgen.GlyphidHiveGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Base 1.7.10 glyphid entity. Variant-specific behavior is added by the formal subclasses. */
public class GlyphidEntity extends Monster {
    public enum Variant {
        NORMAL, INFECTED, RADIOACTIVE, BOMBARDIER, BRAWLER, DIGGER, BLASTER, BEHEMOTH, BRENDA, NUCLEAR, SCOUT
    }

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_INFECTED = 1;
    public static final int TYPE_RADIOACTIVE = 2;

    private static final EntityDataAccessor<Byte> WALL_CLIMBING =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ARMOR =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> SUBTYPE =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(GlyphidEntity.class, EntityDataSerializers.BYTE);
    private static volatile BlockPos guidanceTarget;

    private Entity lastSpecialTarget;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;
    private int specialTimer;
    private int behemothTimer = 120;
    private int behemothBreathTime;
    private int nuclearDeathTicks;
    private boolean nuclearDeathSignalSent;

    public static final int TASK_IDLE = 0;
    public static final int TASK_RETREAT_FOR_REINFORCEMENTS = 1;
    public static final int TASK_BUILD_HIVE = 2;
    public static final int TASK_INITIATE_RETREAT = 3;
    public static final int TASK_FOLLOW = 4;
    public static final int TASK_TERRAFORM = 5;
    public static final int TASK_DIG = 6;
    private int currentTask = TASK_IDLE;
    private boolean hasHome;
    private int homeX;
    private int homeY;
    private int homeZ;
    private boolean hasWaypoint;
    private EntityWaypoint taskWaypoint;
    private int taskX;
    private int taskY;
    private int taskZ;
    private int previousTask = TASK_IDLE;
    private EntityWaypoint previousWaypoint;
    private boolean shouldDig;
    private boolean scoutHasTarget;
    private int scoutTimer;
    private int scoutRange = 45;
    private int scoutMinimumHiveDistance = 8;
    private boolean scoutLargeHive;
    private float scoutLargeHiveChance;

    public GlyphidEntity(EntityType<? extends GlyphidEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static void setGuidanceTarget(BlockPos pos) {
        guidanceTarget = pos == null ? null : pos.immutable();
    }

    public static BlockPos getGuidanceTarget() {
        return guidanceTarget;
    }

    public static AttributeSupplier.Builder createAttributes() {
        GlyphidStats.StatBundle stats = GlyphidStats.forVariant(Variant.NORMAL);
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.health())
                .add(Attributes.MOVEMENT_SPEED, stats.speed() * 0.1D)
                .add(Attributes.ATTACK_DAMAGE, stats.damage())
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return GlyphidEntity.this.getCurrentTask() == TASK_IDLE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return GlyphidEntity.this.getCurrentTask() == TASK_IDLE && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new GlyphidPlayerTargetGoal());
    }

    private final class GlyphidPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        private GlyphidPlayerTargetGoal() {
            super(GlyphidEntity.this, Player.class, true);
        }

        @Override
        protected double getFollowDistance() {
            if (GlyphidEntity.this.getVariant() == Variant.SCOUT) {
                return 10.0D;
            }
            return GlyphidEntity.this.useExtendedTargeting() ? 128.0D : 16.0D;
        }

        @Override
        public boolean canUse() {
            if (GlyphidEntity.this.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                return false;
            }
            targetConditions.range(getFollowDistance());
            return super.canUse();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALL_CLIMBING, (byte) 0);
        builder.define(ARMOR, (byte) 0b11111);
        builder.define(SUBTYPE, (byte) TYPE_NORMAL);
        builder.define(VARIANT, (byte) Variant.NORMAL.ordinal());
    }

    public Variant getVariant() {
        int id = entityData.get(VARIANT);
        return id < 0 || id >= Variant.values().length ? Variant.NORMAL : Variant.values()[id];
    }

    @Override
    public Component getName() {
        Component customName = getCustomName();
        if (customName != null) {
            return customName;
        }
        String suffix = switch (getVariant()) {
            case BEHEMOTH -> "_behemoth";
            case BLASTER -> "_blaster";
            case BOMBARDIER -> "_bombardier";
            case BRAWLER -> "_brawler";
            case BRENDA -> "_brenda";
            case DIGGER -> "_digger";
            case NUCLEAR -> "_nuclear";
            case SCOUT -> "_scout";
            default -> "";
        };
        return Component.translatable("entity.reinhardtshbm.glyphid" + suffix);
    }

    public void setVariant(Variant variant) {
        entityData.set(VARIANT, (byte) variant.ordinal());
        refreshAttributes();
        refreshDimensions();
    }

    public int getSubtype() {
        return entityData.get(SUBTYPE);
    }

    public void setSubtype(int subtype) {
        entityData.set(SUBTYPE, (byte) Math.max(TYPE_NORMAL, Math.min(TYPE_RADIOACTIVE, subtype)));
        refreshAttributes();
    }

    public int armorCount() {
        return Integer.bitCount(entityData.get(ARMOR) & 0x1F);
    }

    public byte armorMask() {
        return entityData.get(ARMOR);
    }

    public GlyphidStats.StatBundle stats() {
        return GlyphidStats.forVariant(getVariant());
    }

    /** Scale used by the 1.7.10 OBJ renderer for each concrete glyphid class. */
    public double modelScale() {
        return switch (getVariant()) {
            case BRAWLER, BLASTER -> 1.25D;
            case DIGGER -> 1.30D;
            case BEHEMOTH -> 1.50D;
            case BRENDA, NUCLEAR -> 2.0D;
            case SCOUT -> 0.75D;
            default -> 1.0D;
        };
    }

    public String textureName() {
        return switch (getVariant()) {
            case BOMBARDIER -> "glyphid_bombardier";
            case BRAWLER -> "glyphid_brawler";
            case DIGGER -> "glyphid_digger";
            case BLASTER -> "glyphid_blaster";
            case BEHEMOTH -> "glyphid_behemoth";
            case BRENDA -> "glyphid_brenda";
            case NUCLEAR -> "glyphid_nuclear";
            case SCOUT -> "glyphid_scout";
            default -> "glyphid";
        };
    }

    public boolean hasArmor(int segment) {
        return (entityData.get(ARMOR) & (1 << segment)) != 0;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (getVariant()) {
            case BRAWLER, BLASTER -> EntityDimensions.fixed(2.0F, 1.125F);
            case BEHEMOTH -> EntityDimensions.fixed(2.5F, 1.5F);
            case BRENDA, NUCLEAR -> EntityDimensions.fixed(2.5F, 1.75F);
            case SCOUT -> EntityDimensions.fixed(1.25F, 0.75F);
            default -> EntityDimensions.fixed(1.75F, 1.0F);
        };
    }

    private void refreshAttributes() {
        GlyphidStats.StatBundle stats = stats();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.health());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(stats.speed() * 0.1D
                * (getSubtype() == TYPE_RADIOACTIVE ? 2.0D : 1.0D));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stats.damage()
                * (getSubtype() == TYPE_RADIOACTIVE ? 5.0D : 1.0D));
        // EntityType construction applies attributes after the entity
        // constructor.  A fixed-variant glyphid calls setVariant from that
        // constructor, so its initial health is still zero at this point.
        // Vanilla Mob initialises newly-created entities to max health; keep
        // that same initialisation instead of preserving the pre-attribute
        // zero value.  Existing entities retain their current health when
        // their variant/subtype changes.
        setHealth(getHealth() <= 0.0F ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        refreshAttributes();
        setHealth(getMaxHealth());
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        tickLegacyTaskSystem();
        boolean climbing = horizontalCollision;
        entityData.set(WALL_CLIMBING, (byte) (climbing ? 1 : 0));
        if (climbing && !onGround()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.max(motion.y, 0.2D), motion.z);
        }
        runLegacyVariantBehavior();
    }

    /** Mirrors EntityGlyphid's server-side task state and waypoint navigation. */
    private void tickLegacyTaskSystem() {
        if (!hasHome) {
            homeX = blockPosition().getX();
            homeY = blockPosition().getY();
            homeZ = blockPosition().getZ();
            hasHome = true;
        }

        if (hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
            setTarget(null);
            getNavigation().stop();
            return;
        }

        if (currentTask == TASK_FOLLOW && isAtDestination() && !hasWaypoint) {
            setCurrentTask(TASK_IDLE, null);
        } else if (currentTask == TASK_DIG && tickCount % 20 == 0 && isAtDestination()) {
            swing(InteractionHand.MAIN_HAND);
            performLegacyDigExplosion();
            shouldDig = false;
            setCurrentTask(previousTask, previousWaypoint);
        }

        if (currentTask != TASK_IDLE && taskWaypoint != null && !isAtDestination()
                && tickCount % 10 == 0) {
            taskX = taskWaypoint.blockPosition().getX();
            taskY = taskWaypoint.blockPosition().getY();
            taskZ = taskWaypoint.blockPosition().getZ();
            if (taskWaypoint.isHighPriority()) {
                setTarget(null);
                getNavigation().stop();
            }
            if (canDig() && modelScale() >= 1.0D && currentTask != TASK_DIG) {
                BlockHitResult obstruction = findWaypointObstruction();
                if (obstruction != null) {
                    digToWaypoint(obstruction);
                    return;
                }
            }
            getNavigation().moveTo(taskX + 0.5D, taskY, taskZ + 0.5D, 1.0D);
        }
    }

    private boolean canDig() {
        return getVariant() == Variant.DIGGER || HbmConfig.glyphidDig();
    }

    /** Matches EntityGlyphid's 1.7.10 obstruction trace. */
    private BlockHitResult findWaypointObstruction() {
        Vec3 start = getEyePosition();
        Vec3 end = new Vec3(taskX, taskY, taskZ);
        BlockHitResult hit = (BlockHitResult) level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level().getBlockState(pos);
        return state.getBlock().getExplosionResistance() <= digResistance() ? hit : null;
    }

    /**
     * Creates the temporary five-block dig waypoint used by the old AI and
     * stores the task which must be restored after the blast.
     */
    private void digToWaypoint(BlockHitResult obstacle) {
        EntityWaypoint digWaypoint = new EntityWaypoint(
                HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
        BlockPos pos = obstacle.getBlockPos();
        digWaypoint.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        digWaypoint.setRadius(5);
        level().addFreshEntity(digWaypoint);

        previousTask = currentTask;
        previousWaypoint = taskWaypoint;
        setCurrentTask(TASK_DIG, digWaypoint);
        getNavigation().moveTo(taskX + 0.5D, taskY, taskZ + 0.5D, 1.0D);
        communicate(TASK_DIG, digWaypoint);
    }

    private int digBlastSize() {
        return Math.min((int) (3.0D * modelScale()) / 2, 5);
    }

    private int digResistance() {
        return Math.min((int) (50.0D * (modelScale() * 2.0D)), 150);
    }

    /**
     * Port of BlockAllocatorGlyphidDig plus BlockProcessorStandard.setNoDrop.
     * The old implementation did not create explosion effects or entity
     * damage for this task; only the allocated blocks are removed.
     */
    private void performLegacyDigExplosion() {
        double centerX = taskX;
        double centerY = taskY + 2.0D;
        double centerZ = taskZ;
        float size = digBlastSize();
        int resolution = 16;
        Set<BlockPos> affected = new HashSet<>();

        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != resolution - 1
                            && j != 0 && j != resolution - 1
                            && k != 0 && k != resolution - 1) {
                        continue;
                    }

                    double directionX = (double) ((float) i / (resolution - 1.0F) * 2.0F - 1.0F);
                    double directionY = (double) ((float) j / (resolution - 1.0F) * 2.0F - 1.0F);
                    double directionZ = (double) ((float) k / (resolution - 1.0F) * 2.0F - 1.0F);
                    double length = Math.sqrt(directionX * directionX
                            + directionY * directionY + directionZ * directionZ);
                    directionX /= length;
                    directionY /= length;
                    directionZ /= length;

                    double currentX = centerX;
                    double currentY = centerY;
                    double currentZ = centerZ;
                    double distance = 0.0D;
                    while (distance <= size) {
                        distance = Math.sqrt(
                                (currentX - centerX) * (currentX - centerX)
                                        + (currentY - centerY) * (currentY - centerY)
                                        + (currentZ - centerZ) * (currentZ - centerZ));
                        BlockPos pos = new BlockPos(
                                Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
                        BlockState state = level().getBlockState(pos);

                        if (!state.isAir()) {
                            if (state.getBlock().getExplosionResistance() > digResistance()
                                    || state.is(HbmBlocks.GLYPHID_SPAWNER.get())) {
                                break;
                            }
                        }
                        affected.add(pos);
                        currentX += directionX * 0.3D;
                        currentY += directionY * 0.3D;
                        currentZ += directionZ * 0.3D;
                    }
                }
            }
        }

        for (BlockPos pos : affected) {
            if (!level().getBlockState(pos).isAir()) {
                level().destroyBlock(pos, false);
            }
        }
    }

    private void runLegacyVariantBehavior() {
        LivingEntity target = getTarget();
        switch (getVariant()) {
            case SCOUT -> runScoutBehavior();
            case BOMBARDIER, BLASTER -> bombardierAttack(target);
            case BRAWLER -> brawlerLeap(target);
            case DIGGER -> diggerSlam(target);
            case BEHEMOTH -> behemothAttack(target);
            case NUCLEAR -> runNuclearBehavior();
            default -> {
            }
        }
    }

    private void runScoutBehavior() {
        if (getTarget() != null && distanceToSqr(getTarget()) > 100.0D) {
            setTarget(null);
        }
        if (getTarget() != null && tickCount % 60 == 0
                && (getTarget().isRemoved() || !getTarget().isAlive())) {
            setTarget(null);
        }

        // This condition intentionally follows EntityGlyphidScout's old
        // task gate: a Scout without a waypoint keeps looking for expansion
        // work even after a previous task has completed.
        if ((currentTask != TASK_BUILD_HIVE || currentTask != TASK_TERRAFORM) && taskWaypoint == null) {
            net.minecraft.core.BlockPos guidance = scoutGuidanceTarget();
            if (HbmConfig.glyphidGuidance() && guidance != null) {
                if (!scoutHasTarget) {
                    Vec3 awayFromBase = position().subtract(Vec3.atCenterOf(guidance));
                    if (awayFromBase.lengthSqr() > 1.0E-6D) {
                        Vec3 point = position().add(awayFromBase.normalize().scale(10.0D));
                        EntityWaypoint target = new EntityWaypoint(
                                HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                        target.moveTo(point.x, point.y, point.z, 0.0F, 0.0F);
                        target.setMaxAge(300);
                        target.setRadius(6);
                        target.setWaypointType(TASK_BUILD_HIVE);
                        level().addFreshEntity(target);
                        scoutHasTarget = true;
                        setCurrentTask(TASK_RETREAT_FOR_REINFORCEMENTS, target);
                    }
                }
                if (isAtDestination()) {
                    setCurrentTask(TASK_BUILD_HIVE, null);
                    scoutHasTarget = false;
                }
            } else {
                setCurrentTask(TASK_BUILD_HIVE, null);
            }
        }

        if (currentTask != TASK_BUILD_HIVE && currentTask != TASK_TERRAFORM) {
            return;
        }

        if (!level().isClientSide && !scoutHasTarget) {
            if (scoutRange != 60 && hasNuclearGlyphidNearby()) {
                setCurrentTask(TASK_TERRAFORM, null);
            }
            if (expandScoutHive()) {
                addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 180 * 20, 1));
                scoutHasTarget = true;
            }
        }
        if (taskWaypoint == null && scoutHasTarget) {
            scoutHasTarget = false;
        }

        if (currentTask == TASK_TERRAFORM && isAtDestination() && scoutCanBuildHiveHere()) {
            communicate(TASK_TERRAFORM, taskWaypoint);
        }

        if (tickCount % 10 != 0 || !isAtDestination() || level().isClientSide
                || !scoutCanBuildHiveHere()) {
            return;
        }
        scoutTimer++;
        if (scoutTimer == 1) {
            EntityWaypoint returnPoint = new EntityWaypoint(
                    HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
            returnPoint.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
            returnPoint.setWaypointType(TASK_IDLE);

            EntityWaypoint home = new EntityWaypoint(
                    HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
            home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
            home.setAdditionalWaypoint(returnPoint);
            home.setMaxAge(1200);
            home.setRadius(6);
            home.moveTo(homeX, homeY, homeZ, 0.0F, 0.0F);
            level().addFreshEntity(home);

            taskWaypoint = home;
            hasWaypoint = true;
            taskX = home.blockPosition().getX();
            taskY = home.blockPosition().getY();
            taskZ = home.blockPosition().getZ();
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40 * 20, 10));
            communicate(TASK_RETREAT_FOR_REINFORCEMENTS, home);
        } else if (scoutTimer >= 5) {
            level().explode(this, getX(), getY(), getZ(), 5.0F, false,
                    Level.ExplosionInteraction.NONE);
            GlyphidHiveGenerator.generateSmall(level(), blockPosition().getX(), blockPosition().getY(),
                    blockPosition().getZ(), random, getSubtype() != TYPE_NORMAL, false);
            discard();
        } else {
            communicate(TASK_FOLLOW, taskWaypoint);
        }
    }

    private net.minecraft.core.BlockPos scoutGuidanceTarget() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }
        Player player = serverLevel.getNearestPlayer(this, 300.0D);
        if (player == null) {
            return getGuidanceTarget();
        }
        return player.blockPosition();
    }

    private boolean scoutCanBuildHiveHere() {
        int length = scoutLargeHive ? 16 : 8;
        Vec3 start = new Vec3(getX(), getY() + 1.0D, getZ());
        for (int index = 0; index < 8; index++) {
            double angle = Math.toRadians(360.0D / 16.0D * index);
            Vec3 end = start.add(-Math.sin(angle) * length, 0.0D, Math.cos(angle) * length);
            net.minecraft.world.phys.HitResult hit = level().clip(new net.minecraft.world.level.ClipContext(
                    start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    && level().getBlockState(net.minecraft.core.BlockPos.containing(hit.getLocation()))
                    .is(HbmBlocks.GLYPHID_BASE.get())) {
                setCurrentTask(TASK_IDLE, null);
                scoutHasTarget = false;
                return false;
            }
        }
        return true;
    }

    private boolean hasNuclearGlyphidNearby() {
        return !level().getEntitiesOfClass(GlyphidEntity.class, getBoundingBox().inflate(8.0D),
                glyphid -> glyphid != this && glyphid.getVariant() == Variant.NUCLEAR).isEmpty();
    }

    private boolean expandScoutHive() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        scoutRange = currentTask == TASK_TERRAFORM ? 60 : scoutRange;
        scoutMinimumHiveDistance = currentTask == TASK_TERRAFORM ? 20 : scoutMinimumHiveDistance;
        int nestX = homeX - scoutRange + random.nextInt(scoutRange * 2);
        int nestZ = homeZ - scoutRange + random.nextInt(scoutRange * 2);
        int nestY = serverLevel.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nestX, nestZ);
        net.minecraft.core.BlockPos groundPos = new net.minecraft.core.BlockPos(nestX, nestY - 1, nestZ);
        net.minecraft.world.level.block.state.BlockState ground = level().getBlockState(groundPos);
        boolean farEnough = position().distanceToSqr(nestX, nestY, nestZ)
                > scoutMinimumHiveDistance * scoutMinimumHiveDistance;
        if (!farEnough || ground.isAir() || !ground.isCollisionShapeFullBlock(level(), groundPos)
                || ground.is(HbmBlocks.GLYPHID_BASE.get())) {
            return false;
        }

        if (ground.is(HbmBlocks.BASALT.get())) {
            scoutLargeHive = true;
            scoutLargeHiveChance = Math.max(1.0F, HbmConfig.GLYPHID_LARGE_HIVE_CHANCE.get() / 2.0F);
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60 * 20, 3));
        }

        EntityWaypoint nest = new EntityWaypoint(HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
        nest.setWaypointType(currentTask);
        nest.setRadius(5);
        if (scoutLargeHive) {
            nest.setHighPriority();
        }
        nest.moveTo(nestX, nestY, nestZ, 0.0F, 0.0F);
        serverLevel.addFreshEntity(nest);
        setCurrentTask(currentTask, nest);
        communicate(TASK_BUILD_HIVE, nest);
        return true;
    }

    private void bombardierAttack(LivingEntity target) {
        if (target == null) {
            return;
        }
        if (tickCount % 20 == 0) {
            lastSpecialTarget = target;
            lastTargetX = target.getX();
            lastTargetY = target.getY();
            lastTargetZ = target.getZ();
        }
        if (tickCount % 60 != 1) {
            return;
        }
        boolean topAttack = distanceTo(target) > 20.0F;
        double velocityX = target.getX() - lastTargetX;
        double velocityY = target.getY() - lastTargetY;
        double velocityZ = target.getZ() - lastTargetZ;
        if (lastSpecialTarget != target || new Vec3(velocityX, velocityY, velocityZ).length() > 30.0D) {
            velocityX = velocityY = velocityZ = 0.0D;
        }
        int prediction = topAttack ? 60 : 20;
        Vec3 delta = new Vec3(
                target.getX() - getX() + velocityX * prediction,
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D) + velocityY * prediction,
                target.getZ() - getZ() + velocityZ * prediction
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = getVariant() == Variant.BLASTER ? 1.25D : 1.0D;
        double speedSquared = speed * speed;
        double gravity = 0.04D;
        double discriminant = speedSquared * speedSquared
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speedSquared);
        if (discriminant < 0.0D) {
            return;
        }
        double targetPitch = Math.atan((speedSquared + Math.sqrt(discriminant) * (topAttack ? 1.0D : -1.0D))
                / (gravity * horizontal));
        if (Double.isNaN(targetPitch)) {
            return;
        }
        Vec3 fireVector = new Vec3(speed, 0.0D, 0.0D)
                .zRot((float) -targetPitch)
                .yRot((float) -(targetYaw + Math.PI * 0.5D));
        int bombCount = getVariant() == Variant.BLASTER ? 10 : 5;
        float spread = getVariant() == Variant.BLASTER ? 0.5F : 1.0F;
        float damage = getVariant() == Variant.BLASTER ? 15.0F : 5.0F;
        for (int index = 0; index < bombCount; index++) {
            level().addFreshEntity(new GlyphidAcidBombEntity(level(), this,
                    new Vec3(getX(), getY() + 1.0D, getZ()), fireVector,
                    (float) speed, index * spread, damage));
        }
        swing(InteractionHand.MAIN_HAND);
    }

    private void runNuclearBehavior() {
        if (tickCount % 20 != 0) {
            return;
        }
        if (currentTask == TASK_FOLLOW && isAtDestination()) {
            setCurrentTask(TASK_IDLE, null);
        }
        if (currentTask == TASK_BUILD_HIVE && getTarget() == null) {
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 10 * 20, 3));
        }
        if (currentTask == TASK_TERRAFORM) {
            setHealth(0.0F);
        }
    }

    private void brawlerLeap(LivingEntity target) {
        if (target == null || !isAlive()) {
            return;
        }
        lastTargetX = target.getX();
        lastTargetY = target.getY();
        lastTargetZ = target.getZ();
        if (--specialTimer > 0) {
            return;
        }
        specialTimer = 80 + random.nextInt(30);
        if (distanceTo(target) >= 20.0F) {
            return;
        }
        // The 1.7.10 brawler never assigned lastTarget, so its prediction velocity is zero.
        Vec3 delta = new Vec3(
                target.getX() - getX(),
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D),
                target.getZ() - getZ()
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double yaw = -Math.atan2(delta.x, delta.z);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = 1.5D;
        double gravity = 0.01D;
        double discriminant = speed * speed * speed * speed
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speed * speed);
        if (discriminant < 0.0D) {
            return;
        }
        double pitch = Math.atan((speed * speed + Math.sqrt(discriminant)) / (gravity * horizontal));
        Vec3 motion = new Vec3(speed, 0.0D, 0.0D)
                .zRot((float) (-pitch / 3.5D))
                .yRot((float) -(yaw + Math.PI * 0.5D));
        motion = motion.normalize().scale(speed).add(
                random.nextGaussian() * 0.0075D * random.nextFloat(),
                random.nextGaussian() * 0.0075D * random.nextFloat(),
                random.nextGaussian() * 0.0075D * random.nextFloat()
        );
        setDeltaMovement(motion);
    }

    private void diggerSlam(LivingEntity target) {
        if (target == null || !isAlive()) {
            return;
        }
        lastTargetX = target.getX();
        lastTargetY = target.getY();
        lastTargetZ = target.getZ();
        if (--specialTimer > 0) {
            return;
        }
        specialTimer = 120;
        if (distanceTo(target) >= 30.0F) {
            return;
        }
        int baseX = (int) getX();
        int baseY = (int) getY();
        int baseZ = (int) getZ();
        Vec3 direction = getLookAngle();
        java.util.List<net.minecraft.core.BlockPos> path = blockPath(baseX, baseY, baseZ, 6, direction);
        for (int index = 0; index < 8; index++) {
            direction = direction.yRot(-1.0F / 16.0F);
            path.addAll(blockPath(baseX, baseY - 1, baseZ, 6, direction));
        }

        boolean topAttack = distanceTo(target) > 20.0F;
        double velocityX = target.getX() - lastTargetX;
        double velocityY = target.getY() - lastTargetY;
        double velocityZ = target.getZ() - lastTargetZ;
        if (lastSpecialTarget != target) {
            velocityX = velocityY = velocityZ = 0.0D;
        }
        int prediction = 60;
        Vec3 delta = new Vec3(
                target.getX() - getX() + velocityX * prediction,
                target.getY() + target.getBbHeight() * 0.5D - (getY() + 1.0D) + velocityY * prediction,
                target.getZ() - getZ() + velocityZ * prediction
        );
        if (delta.length() < 3.0D) {
            return;
        }
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double speed = 1.2D;
        double gravity = 0.03D;
        double discriminant = speed * speed * speed * speed
                - gravity * (gravity * horizontal * horizontal + 2.0D * delta.y * speed * speed);
        Vec3 rubbleMotion = Vec3.ZERO;
        if (discriminant >= 0.0D) {
            double pitch = Math.atan((speed * speed + Math.sqrt(discriminant) * (topAttack ? 1.0D : -1.0D))
                    / (gravity * horizontal));
            rubbleMotion = new Vec3(speed, 0.0D, 0.0D)
                    .zRot((float) -pitch)
                    .yRot((float) -(-Math.atan2(delta.x, delta.z) + Math.PI * 0.5D));
        }
        for (net.minecraft.core.BlockPos pos : path) {
            net.minecraft.world.level.block.state.BlockState state = level().getBlockState(pos);
            if (state.isAir() || !state.isCollisionShapeFullBlock(level(), pos)
                    || state.getBlock() instanceof com.reinhardt.hbm.block.MachineDummyBlock
                    || level().getBlockEntity(pos) != null
                    || state.getBlock().getExplosionResistance() >= HbmBlocks.CONCRETE.get().getExplosionResistance()) {
                continue;
            }
            Vec3 motion = rubbleMotion.lengthSqr() == 0.0D ? new Vec3(0.0D, 0.0D, 0.0D) : rubbleMotion;
            level().addFreshEntity(new MineRubbleEntity(level(), pos.getX() + 0.5D, pos.getY() + 2.0D,
                    pos.getZ() + 0.5D, motion));
            level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        swing(InteractionHand.MAIN_HAND);
    }

    private static java.util.List<net.minecraft.core.BlockPos> blockPath(int x, int y, int z, int length, Vec3 direction) {
        java.util.List<net.minecraft.core.BlockPos> result = new java.util.ArrayList<>();
        for (int index = 0; index <= length; index++) {
            result.add(new net.minecraft.core.BlockPos((int) (x + direction.x * index), y,
                    (int) (z + direction.z * index)));
        }
        return result;
    }

    private void behemothAttack(LivingEntity target) {
        if (target == null) {
            behemothTimer = 120;
            behemothBreathTime = 0;
            return;
        }
        if (behemothBreathTime > 0) {
            if (attackAnim <= 0.0F) {
                swing(InteractionHand.MAIN_HAND);
            }
            if (distanceTo(target) < 20.0F) {
                addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 6));
                level().addFreshEntity(new GlyphidAcidSprayEntity(level(), this));
            }
            setYRot(yRotO);
            behemothBreathTime--;
        } else if (--behemothTimer <= 0) {
            behemothBreathTime = 120;
            behemothTimer = 120;
        }
    }

    public boolean isBesideClimbableBlock() {
        return entityData.get(WALL_CLIMBING) != 0;
    }

    public boolean useExtendedTargeting() {
        if (getVariant() == Variant.SCOUT) {
            return false;
        }
        if (HbmConfig.glyphidExtendedTargeting()) {
            return true;
        }
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        return HbmPollutionData.get(serverLevel).get(blockPosition(), HbmPollutionType.SOOT)
                >= HbmConfig.GLYPHID_TARGETING_THRESHOLD.get();
    }

    public int getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(int task) {
        setCurrentTask(task, null);
    }

    public EntityWaypoint getWaypoint() {
        return taskWaypoint;
    }

    /** Assigns the old task and its optional entity waypoint as one operation. */
    public void setCurrentTask(int task, EntityWaypoint waypoint) {
        currentTask = Math.max(TASK_IDLE, Math.min(TASK_DIG, task));
        taskWaypoint = waypoint;
        hasWaypoint = waypoint != null;
        if (waypoint != null) {
            taskX = waypoint.blockPosition().getX();
            taskY = waypoint.blockPosition().getY();
            taskZ = waypoint.blockPosition().getZ();
            if (waypoint.isHighPriority()) {
                setTarget(null);
                getNavigation().stop();
            }
        }
        carryOutTask();
    }

    /** Handles task transitions that are performed once when a task is assigned. */
    public void carryOutTask() {
        switch (currentTask) {
            case TASK_RETREAT_FOR_REINFORCEMENTS -> {
                if (taskWaypoint != null) {
                    communicate(TASK_FOLLOW, taskWaypoint);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
            }
            case TASK_INITIATE_RETREAT -> {
                if (!level().isClientSide && taskWaypoint == null) {
                    EntityWaypoint returnPoint = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    returnPoint.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
                    returnPoint.setWaypointType(TASK_IDLE);

                    EntityWaypoint home = new EntityWaypoint(
                            HbmEntityTypes.GLYPHID_WAYPOINT.get(), level());
                    home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                    home.setAdditionalWaypoint(returnPoint);
                    home.setHighPriority();
                    home.setRadius(6);
                    home.moveTo(homeX, homeY, homeZ, 0.0F, 0.0F);
                    level().addFreshEntity(home);

                    communicate(TASK_FOLLOW, home);
                    setCurrentTask(TASK_FOLLOW, home);
                }
            }
            case TASK_DIG -> shouldDig = true;
            default -> {
            }
        }
    }

    /** Copies tasks to nearby non-scout Glyphids, matching the old radius rule. */
    public void communicate(int task, EntityWaypoint waypoint) {
        int radius = waypoint == null ? 4 : waypoint.getRadius();
        AABB area = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ())
                .inflate(radius, radius, radius);
        for (GlyphidEntity glyphid : level().getEntitiesOfClass(GlyphidEntity.class, area,
                glyphid -> glyphid != this && glyphid.getVariant() != Variant.SCOUT)) {
            if (glyphid.getCurrentTask() != task) {
                glyphid.setCurrentTask(task, waypoint);
            }
        }
    }

    public boolean isAtDestination() {
        int destinationRadius = taskWaypoint == null ? 25 : taskWaypoint.getRadius() * taskWaypoint.getRadius();
        return distanceToSqr(taskX, taskY, taskZ) <= destinationRadius;
    }

    /** EntityGlyphidNuclear's old signal only reached scouts within four blocks. */
    private void communicateNuclearRetreat() {
        AABB area = getBoundingBox().inflate(4.0D);
        for (GlyphidEntity glyphid : level().getEntitiesOfClass(GlyphidEntity.class, area,
                glyphid -> glyphid != this && glyphid.getVariant() == Variant.SCOUT)) {
            if (glyphid.getCurrentTask() != TASK_INITIATE_RETREAT) {
                glyphid.setCurrentTask(TASK_INITIATE_RETREAT);
            }
        }
    }

    public boolean shouldBreakArmor(float amount) {
        float multiplier = GlyphidStats.armorBreakMultiplier(getVariant());
        double chanceValue = getVariant() == Variant.SCOUT ? amount : amount * multiplier;
        return random.nextInt(100) <= Math.min(Math.pow(chanceValue, 2.0D), 100.0D);
    }

    public void breakOffArmor() {
        int mask = entityData.get(ARMOR) & 0x1F;
        if (mask == 0) {
            return;
        }
        int bit;
        do {
            bit = 1 << random.nextInt(5);
        } while ((mask & bit) == 0);
        entityData.set(ARMOR, (byte) (mask & ~bit));
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.25F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof GlyphidEntity) {
            return false;
        }
        if (getVariant() == Variant.BRAWLER && source.is(DamageTypeTags.IS_FALL) && amount <= 10.0F) {
            return false;
        }
        return GlyphidStats.handleAttack(this, source, amount);
    }

    @Override
    public boolean fireImmune() {
        return getVariant() == Variant.BRENDA || getVariant() == Variant.NUCLEAR || super.fireImmune();
    }

    /** EntityMob in 1.7.10 did not inherit the modern daylight-burning rule. */
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    public boolean attackSuperclass(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && getVariant() == Variant.SCOUT && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 10 * 20, 3));
        }
        if (hit && getSubtype() == TYPE_INFECTED && target instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 100, 2));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.CONFUSION, 100, 0));
        }
        return hit;
    }

    @Override
    protected void tickDeath() {
        if (getVariant() != Variant.NUCLEAR) {
            super.tickDeath();
            return;
        }

        nuclearDeathTicks++;
        if (!nuclearDeathSignalSent) {
            communicateNuclearRetreat();
            nuclearDeathSignalSent = true;
        }

        if (nuclearDeathTicks == 90 && !level().isClientSide) {
            AABB area = getBoundingBox().inflate(8.0D);
            if (!level().getEntitiesOfClass(GlyphidEntity.class, area,
                    glyphid -> glyphid != this).isEmpty()) {
                addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20, 6));
                addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
            }
        }

        if (nuclearDeathTicks == 100) {
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                boolean infected = getSubtype() == TYPE_INFECTED;
                if (infected) {
                    int count = 15 + random.nextInt(6);
                    for (int index = 0; index < count; index++) {
                        double offsetX = ((index % 2) - 0.5D) * 0.5D;
                        double offsetZ = ((index / 2) - 0.5D) * 0.5D;
                        ParasiteMaggotEntity maggot = new ParasiteMaggotEntity(
                                HbmEntityTypes.PARASITE_MAGGOT.get(), serverLevel);
                        maggot.moveTo(getX() + offsetX, getY() + 0.5D, getZ() + offsetZ,
                                random.nextFloat() * 360.0F, 0.0F);
                        maggot.setDeltaMovement(offsetX, 0.0D, offsetZ);
                        serverLevel.addFreshEntity(maggot);
                    }
                } else {
                    LegacyProjectileUtil.detonateGlyphidNuclear(serverLevel, this, position(), !infected);
                }
            }
            discard();
            return;
        }

        if (!level().isClientSide && nuclearDeathTicks % 10 == 0) {
            level().playSound(null, blockPosition(), HbmSoundEvents.CHARGE_BEEP.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 5.0F, 1.0F);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && getSubtype() == TYPE_INFECTED
                && getVariant() != Variant.NUCLEAR) {
            spawnInfectedMaggots();
            sendInfectedDeathEffect();
        }
        if (!level().isClientSide && getVariant() == Variant.BRENDA) {
            level().addFreshEntity(new LegacyMistEntity(level(), getX(), getY(), getZ(),
                    LegacyMistEntity.MistType.PHEROMONE, 14.0F, 6.0F, 80));
            for (int index = 0; index < 12; index++) {
                GlyphidEntity glyphid = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level());
                glyphid.setVariant(Variant.NORMAL);
                glyphid.moveTo(getX(), getY() + 0.5D, getZ(),
                        random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(glyphid);
                glyphid.setDeltaMovement(random.nextGaussian(), 0.0D, random.nextGaussian());
            }
        }
        if (!level().isClientSide && getVariant() == Variant.BEHEMOTH) {
            level().addFreshEntity(new LegacyMistEntity(level(), getX(), getY(), getZ(),
                    LegacyMistEntity.MistType.SULFURIC_ACID, 10.0F, 4.0F, 120));
        }
    }

    private void spawnInfectedMaggots() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        int count = 2 + random.nextInt(3);
        for (int index = 0; index < count; index++) {
            double offsetX = (index % 2 - 0.5D) * 0.5D;
            double offsetZ = (index / 2 - 0.5D) * 0.5D;
            ParasiteMaggotEntity maggot = new ParasiteMaggotEntity(
                    HbmEntityTypes.PARASITE_MAGGOT.get(), serverLevel);
            maggot.moveTo(getX() + offsetX, getY() + 0.5D, getZ() + offsetZ,
                    random.nextFloat() * 360.0F, 0.0F);
            maggot.setDeltaMovement(offsetX, 0.0D, offsetZ);
            maggot.hurtMarked = true;
            serverLevel.addFreshEntity(maggot);
        }
        level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F,
                0.95F + random.nextFloat() * 0.2F);
    }

    private void sendInfectedDeathEffect() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        MaxwellGibEffectPayload payload = new MaxwellGibEffectPayload(
                getId(), getX(), getY() + getBbHeight() * 0.5D, getZ(), getBbWidth(), getBbHeight(), 0);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= 22_500.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source,
                                       boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack meat = new ItemStack(isOnFire() ? HbmItems.GLYPHID_MEAT_GRILLED.get() : HbmItems.GLYPHID_MEAT.get(),
                1 + random.nextInt(3) + (recentlyHit ? 1 : 0));
        spawnAtLocation(meat);
        if (getVariant() == Variant.BRENDA && random.nextInt(3) == 0) {
            HbmFluidDefinition pheromone = HbmFluids.byName("pheromone").orElse(HbmFluids.none());
            spawnAtLocation(HbmFluidContainerItem.makeFull(HbmItems.GLYPHID_GLAND::get, pheromone));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return getTarget() == null && currentTask == TASK_IDLE && tickCount > 100
                && distanceToClosestPlayer > 32.0D;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("armor", armorMask());
        tag.putByte("subtype", (byte) getSubtype());
        tag.putByte("variant", (byte) getVariant().ordinal());
        tag.putBoolean("hasHome", hasHome);
        tag.putInt("homeX", homeX);
        tag.putInt("homeY", homeY);
        tag.putInt("homeZ", homeZ);
        tag.putBoolean("hasWaypoint", hasWaypoint);
        tag.putInt("taskX", taskX);
        tag.putInt("taskY", taskY);
        tag.putInt("taskZ", taskZ);
        tag.putInt("task", currentTask);
        tag.putBoolean("shouldDig", shouldDig);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ARMOR, tag.getByte("armor"));
        entityData.set(SUBTYPE, tag.getByte("subtype"));
        entityData.set(VARIANT, tag.getByte("variant"));
        hasHome = tag.getBoolean("hasHome");
        homeX = tag.getInt("homeX");
        homeY = tag.getInt("homeY");
        homeZ = tag.getInt("homeZ");
        hasWaypoint = tag.getBoolean("hasWaypoint");
        taskX = tag.getInt("taskX");
        taskY = tag.getInt("taskY");
        taskZ = tag.getInt("taskZ");
        currentTask = Math.max(TASK_IDLE, Math.min(TASK_DIG, tag.getInt("task")));
        shouldDig = tag.getBoolean("shouldDig");
        refreshAttributes();
        refreshDimensions();
    }

    public float armorDamageThreshold() {
        return stats().thresholdPerArmor() * armorCount() / 5.0F;
    }
}
