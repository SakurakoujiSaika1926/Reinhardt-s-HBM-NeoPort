package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/** Exact non-firearm portions of 1.7.10 EntityFBI. */
public final class LegacyFbiEntity extends Monster {
    private static final Set<ResourceLocation> LEGACY_BREAKABLE = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_door"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "iron_door"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "trapdoor"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chest"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "trapped_chest"),
            ReinhardtsHBM.id("machine_press"), ReinhardtsHBM.id("machine_epress"),
            ReinhardtsHBM.id("machine_chemical_plant"), ReinhardtsHBM.id("machine_chemical_factory"),
            ReinhardtsHBM.id("machine_crystallizer"), ReinhardtsHBM.id("machine_turbine"),
            ReinhardtsHBM.id("machine_industrial_turbine"), ReinhardtsHBM.id("machine_chungus"),
            ReinhardtsHBM.id("machine_purex"), ReinhardtsHBM.id("crate_iron"), ReinhardtsHBM.id("crate_steel"),
            ReinhardtsHBM.id("machine_diesel"), ReinhardtsHBM.id("machine_rtg_grey"),
            ReinhardtsHBM.id("machine_minirtg"), ReinhardtsHBM.id("machine_powerrtg"),
            ReinhardtsHBM.id("machine_cyclotron"));

    public LegacyFbiEntity(EntityType<? extends LegacyFbiEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation groundNavigation) {
            groundNavigation.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ARMOR, 20.0D);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // EntityFBI ignored indirect damage caused by another FBI.
        if (!source.isDirect() && source.getEntity() instanceof LegacyFbiEntity) {
            return false;
        }
        // A glass-helmet FBI was also immune to the two legacy environmental
        // damage identifiers used by the old oxygen/thermal systems.
        if (getItemBySlot(EquipmentSlot.HEAD).is(Items.GLASS)
                && ("oxygenSuffocation".equals(source.getMsgId())
                || "thermal".equals(source.getMsgId()))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LegacyFbiBreakingGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        // HBM's two firearm choices were retired with the old firearm system;
        // retain the security armour branch and no replacement weapon.
        if (level.getLevel().dimension() != Level.OVERWORLD) {
            // The old FBI used a glass helmet plus PAA suit outside the
            // Overworld; this branch is independent of the 1/5 security-gear
            // roll used on the Overworld.
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GLASS));
            setItemSlot(EquipmentSlot.CHEST, legacyItem("paa_plate"));
            setItemSlot(EquipmentSlot.LEGS, legacyItem("paa_legs"));
            setItemSlot(EquipmentSlot.FEET, legacyItem("paa_boots"));
        } else if (random.nextInt(5) == 0) {
            setItemSlot(EquipmentSlot.HEAD, legacyItem("security_helmet"));
            setItemSlot(EquipmentSlot.CHEST, legacyItem("security_plate"));
            setItemSlot(EquipmentSlot.LEGS, legacyItem("security_legs"));
            setItemSlot(EquipmentSlot.FEET, legacyItem("security_boots"));
        }
        setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0.0F);
        return result;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        // EntityFBI#isPotionApplicable always returned false.
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            setItemSlot(EquipmentSlot.HEAD, legacyItem("gas_mask_m65"));
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // EntityFBI.canDespawn() was hard-disabled in 1.7.10.
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        // MobConfig.raidAttackDelay / raidAttackReach are preserved as
        // explicit modern config values with the same 1.7.10 defaults.
        if (tickCount % HbmConfig.FBI_RAID_ATTACK_DELAY.get() == 0) breakLegacyBlock();
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.5D))) {
            item.setRemainingFireTicks(200);
        }
    }

    private void breakLegacyBlock() {
        Vec3 origin = position().add(0.0D, 0.5D + random.nextFloat(), 0.0D);
        double angle = random.nextFloat() * Math.PI * 2.0D;
        Vec3 direction = new Vec3(Math.cos(angle) * HbmConfig.FBI_RAID_ATTACK_REACH.get(),
                0.0D, Math.sin(angle) * HbmConfig.FBI_RAID_ATTACK_REACH.get());
        BlockHitResult hit = level().clip(new ClipContext(origin, origin.add(direction),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (LEGACY_BREAKABLE.contains(BuiltInRegistries.BLOCK.getKey(level().getBlockState(pos).getBlock()))) {
            level().destroyBlock(pos, false, this);
        }
    }

    private static ItemStack legacyItem(String id) {
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * Port of EntityAIBreaking.  This is deliberately a separate goal from
     * the 40-tick random raid shot below: the old FBI used both behaviours.
     */
    private final class LegacyFbiBreakingGoal extends Goal {
        private BlockPos markedPos;
        private int digTick;
        private int scanTick;
        private LivingEntity target;

        private LegacyFbiBreakingGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = getTarget();
            if (target == null || !getNavigation().isDone() || distanceTo(target) <= 1.0D
                    || (!target.onGround() && hasLineOfSight(target))) {
                return false;
            }
            BlockHitResult hit = nextObstacle(2.0D);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            BlockState state = level().getBlockState(hit.getBlockPos());
            if (state.getDestroySpeed(level(), hit.getBlockPos()) < 0.0F) {
                return false;
            }
            markedPos = hit.getBlockPos();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (markedPos == null) {
                return false;
            }
            Vec3 vector = new Vec3(
                    markedPos.getX() - getX(),
                    markedPos.getY() - (getY() + getEyeHeight()),
                    markedPos.getZ() - getZ());
            return isAlive() && vector.length() <= 4.0D;
        }

        @Override
        public void tick() {
            BlockHitResult hit = null;
            if (tickCount % 10 == 0) {
                hit = nextObstacle(2.0D);
            }
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                markedPos = hit.getBlockPos();
            }
            if (markedPos == null || level().getBlockState(markedPos).isAir()) {
                digTick = 0;
                return;
            }

            BlockState state = level().getBlockState(markedPos);
            int health = (int) (state.getDestroySpeed(level(), markedPos) / 3.0F);
            if (health < 0) {
                markedPos = null;
                return;
            }

            digTick++;
            float progress = (digTick * 0.05F) / (float) health;
            if (progress >= 1.0F) {
                digTick = 0;
                level().destroyBlock(markedPos, false, LegacyFbiEntity.this);
                if (target != null && target.isAlive()) {
                    getNavigation().moveTo(target, 1.0D);
                }
                markedPos = null;
                return;
            }

            if (digTick % 5 == 0) {
                var sound = state.getSoundType(level(), markedPos, LegacyFbiEntity.this);
                level().playSound(null, markedPos, sound.getBreakSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                        sound.getVolume() + 1.0F, sound.getPitch());
                swing(InteractionHand.MAIN_HAND);
                level().destroyBlockProgress(getId(), markedPos, Mth.clamp((int) (progress * 10.0F), 0, 9));
            }
        }

        @Override
        public void stop() {
            BlockPos oldMarkedPos = markedPos;
            markedPos = null;
            digTick = 0;
            if (oldMarkedPos != null) {
                level().destroyBlockProgress(getId(), oldMarkedPos, -1);
            }
        }

        private BlockHitResult nextObstacle(double distance) {
            int digWidth = Mth.ceil(getBbWidth());
            int digHeight = Mth.ceil(getBbHeight());
            int passMax = Math.max(1, digWidth * digWidth * digHeight);
            int x = scanTick % digWidth - (digWidth / 2);
            int y = scanTick / (digWidth * digWidth);
            int z = (scanTick % (digWidth * digWidth)) / digWidth - (digWidth / 2);
            float yaw = getYRot();
            float pitch = getXRot();
            Vec3 origin = new Vec3(getX() + x, getY() + y, getZ() + z);
            Vec3 direction = viewVector(yaw, pitch).scale(distance);
            BlockHitResult hit = (BlockHitResult) level().clip(new ClipContext(
                    origin, origin.add(direction), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, LegacyFbiEntity.this));
            if (hit.getType() != HitResult.Type.BLOCK) {
                scanTick = (scanTick + 1) % passMax;
                return null;
            }
            BlockState state = level().getBlockState(hit.getBlockPos());
            if (state.getDestroySpeed(level(), hit.getBlockPos()) >= 0.0F) {
                scanTick = 0;
                return hit;
            }
            scanTick = (scanTick + 1) % passMax;
            return null;
        }

        private Vec3 viewVector(float yaw, float pitch) {
            float yawRad = -yaw * ((float) Math.PI / 180.0F) - (float) Math.PI;
            float pitchRad = -pitch * ((float) Math.PI / 180.0F);
            float cosPitch = Mth.cos(pitchRad);
            return new Vec3(Mth.sin(yawRad) * cosPitch, Mth.sin(pitchRad), Mth.cos(yawRad) * cosPitch);
        }
    }
}
