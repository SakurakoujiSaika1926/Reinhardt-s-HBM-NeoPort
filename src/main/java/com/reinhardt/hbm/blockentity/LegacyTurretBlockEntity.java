package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.api.machine.RadarCommandReceiver;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.LegacyArtilleryShellEntity;
import com.reinhardt.hbm.entity.LegacyBulletEntity;
import com.reinhardt.hbm.entity.LegacyHimarsRocketEntity;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.entity.LegacyTurretTargeting;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.item.AmmoArtyItem;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.item.TurretBiometryItem;
import com.reinhardt.hbm.menu.LegacyTurretMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyTurretBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, RadarCommandReceiver {
    public static final int CHIP_SLOT = 0;
    public static final int AMMO_START = 1;
    public static final int AMMO_END = 10;
    public static final int BATTERY_SLOT = 10;
    public static final int SLOT_COUNT = 11;
    public static final int DATA_COUNT = 19;
    private static final int[] AMMO_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] FRITZ_INPUT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] NO_SLOTS = {};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank tank = new HbmFluidTank(HbmFluids.byName("diesel").orElse(HbmFluids.none()), 16_000);
    private long power;
    private long lastInput;
    private boolean on;
    private boolean aligned;
    private boolean targetPlayers;
    private boolean targetAnimals;
    private boolean targetMobs = true;
    private boolean targetMachines = true;
    private boolean trackingTarget;
    private int searchTimer;
    private int timer;
    private int reload;
    private int casingDelay;
    private int loaded;
    private int stattrak;
    private int mode;
    private int spareAmmo = -1;
    private float spin;
    private float lastSpin;
    private float spinAccel;
    private float crane;
    private float lastCrane;
    private double barrelLeftPos;
    private double lastBarrelLeftPos;
    private double barrelRightPos;
    private double lastBarrelRightPos;
    private double artilleryBarrelPos;
    private double lastArtilleryBarrelPos;
    private boolean retractingLeft;
    private boolean retractingRight;
    private boolean retractingArtillery;
    private boolean shotSide;
    private boolean didShootLeft;
    private boolean didShootRight;
    private boolean didShootArtillery;
    private boolean didShootBeam;
    private int beamTicks;
    private double beamDistance;
    private int maxwellUpgradeCheckDelay;
    private int maxwellRedLevel;
    private int maxwellGreenLevel;
    private int maxwellBlueLevel;
    private int maxwellBlackLevel;
    private int maxwellPinkLevel;
    private boolean maxwell5g;
    private boolean maxwellScrem;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double rotationYaw;
    private double rotationPitch;
    private double prevRotationYaw;
    private double prevRotationPitch;
    @Nullable
    private TurretCasingEffects.CasingKind pendingCasing;
    @Nullable
    private Entity target;
    private final List<Vec3> targetQueue = new ArrayList<>();

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) LegacyTurretBlockEntity.this.power;
                case 1 -> (int) LegacyTurretBlockEntity.this.lastInput;
                case 2 -> LegacyTurretBlockEntity.this.isOn() ? 1 : 0;
                case 3 -> LegacyTurretBlockEntity.this.targetPlayers ? 1 : 0;
                case 4 -> LegacyTurretBlockEntity.this.targetAnimals ? 1 : 0;
                case 5 -> LegacyTurretBlockEntity.this.targetMobs ? 1 : 0;
                case 6 -> LegacyTurretBlockEntity.this.targetMachines ? 1 : 0;
                case 7 -> LegacyTurretBlockEntity.this.stattrak;
                case 8 -> (int) Math.round(Math.toDegrees(LegacyTurretBlockEntity.this.rotationYaw) * 100.0D);
                case 9 -> (int) Math.round(Math.toDegrees(LegacyTurretBlockEntity.this.rotationPitch) * 100.0D);
                case 10 -> (int) Math.round(Math.toDegrees(LegacyTurretBlockEntity.this.prevRotationYaw) * 100.0D);
                case 11 -> (int) Math.round(Math.toDegrees(LegacyTurretBlockEntity.this.prevRotationPitch) * 100.0D);
                case 12 -> LegacyTurretBlockEntity.this.mode;
                case 13 -> LegacyTurretBlockEntity.this.loaded;
                case 14 -> LegacyTurretBlockEntity.this.spareAmmo;
                case 15 -> Math.round(LegacyTurretBlockEntity.this.crane * 10_000.0F);
                case 16 -> LegacyTurretBlockEntity.this.tank.type().oldId();
                case 17 -> LegacyTurretBlockEntity.this.tank.amount();
                case 18 -> LegacyTurretBlockEntity.this.tank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> LegacyTurretBlockEntity.this.power = value;
                case 1 -> LegacyTurretBlockEntity.this.lastInput = value;
                case 2 -> LegacyTurretBlockEntity.this.on = value != 0;
                case 3 -> LegacyTurretBlockEntity.this.targetPlayers = value != 0;
                case 4 -> LegacyTurretBlockEntity.this.targetAnimals = value != 0;
                case 5 -> LegacyTurretBlockEntity.this.targetMobs = value != 0;
                case 6 -> LegacyTurretBlockEntity.this.targetMachines = value != 0;
                case 7 -> LegacyTurretBlockEntity.this.stattrak = value;
                case 8 -> LegacyTurretBlockEntity.this.rotationYaw = Math.toRadians(value / 100.0D);
                case 9 -> LegacyTurretBlockEntity.this.rotationPitch = Math.toRadians(value / 100.0D);
                case 10 -> LegacyTurretBlockEntity.this.prevRotationYaw = Math.toRadians(value / 100.0D);
                case 11 -> LegacyTurretBlockEntity.this.prevRotationPitch = Math.toRadians(value / 100.0D);
                case 12 -> LegacyTurretBlockEntity.this.mode = value;
                case 13 -> LegacyTurretBlockEntity.this.loaded = value;
                case 14 -> LegacyTurretBlockEntity.this.spareAmmo = value;
                case 15 -> LegacyTurretBlockEntity.this.crane = value / 10_000.0F;
                case 16 -> LegacyTurretBlockEntity.this.tank.setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 17 -> LegacyTurretBlockEntity.this.tank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LegacyTurretBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.LEGACY_TURRET.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyTurretBlockEntity turret) {
        if (level.isClientSide) {
            turret.tickClient();
            return;
        }
        turret.updateMaxwellUpgradeCache();
        PowerNetworkManager.tickFromEndpoint(level, turret);
        turret.tickServer(level);
    }

    public LegacyTurretType type() {
        return getBlockState().getBlock() instanceof LegacyTurretBlock block ? block.type() : LegacyTurretType.FRIENDLY;
    }

    private void tickClient() {
        if (Math.abs(this.prevRotationYaw - this.rotationYaw) > Math.PI) {
            if (this.prevRotationYaw < this.rotationYaw) {
                this.prevRotationYaw += Math.PI * 2.0D;
            } else {
                this.prevRotationYaw -= Math.PI * 2.0D;
            }
        }
        LegacyTurretType type = type();
        this.lastSpin = this.spin;
        if (type == LegacyTurretType.FRIENDLY) {
            this.spinAccel = this.trackingTarget ? Math.min(45.0F, this.spinAccel + 2.0F) : Math.max(0.0F, this.spinAccel - 2.0F);
            this.spin += this.spinAccel;
        } else if (this.trackingTarget && (type == LegacyTurretType.TAUON || type == LegacyTurretType.HOWARD || type == LegacyTurretType.HOWARD_DAMAGED)) {
            this.spin += 45.0F;
        }
        if (this.spin >= 360.0F) {
            this.spin -= 360.0F;
            this.lastSpin -= 360.0F;
        }
        if (this.trackingTarget) {
            this.beamDistance = new Vec3(this.targetX, this.targetY, this.targetZ).distanceTo(turretPos());
        }
        if (this.beamTicks > 0) {
            this.beamTicks--;
        }
        this.lastCrane = this.crane;
        this.lastBarrelLeftPos = this.barrelLeftPos;
        this.lastBarrelRightPos = this.barrelRightPos;
        if (this.retractingLeft) {
            this.barrelLeftPos += 0.5D;
            if (this.barrelLeftPos >= 1.0D) {
                this.retractingLeft = false;
            }
        } else {
            this.barrelLeftPos = Math.max(0.0D, this.barrelLeftPos - 0.25D);
        }
        if (this.retractingRight) {
            this.barrelRightPos += 0.5D;
            if (this.barrelRightPos >= 1.0D) {
                this.retractingRight = false;
            }
        } else {
            this.barrelRightPos = Math.max(0.0D, this.barrelRightPos - 0.25D);
        }
        this.lastArtilleryBarrelPos = this.artilleryBarrelPos;
        if (this.retractingArtillery) {
            this.artilleryBarrelPos += 0.5D;
            if (this.artilleryBarrelPos >= 1.0D) {
                this.retractingArtillery = false;
            }
        } else {
            this.artilleryBarrelPos = Math.max(0.0D, this.artilleryBarrelPos - 0.05D);
        }
    }

    private void tickServer(Level level) {
        LegacyTurretType type = type();
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        this.aligned = false;
        updateSpecialReloads(type);
        if (type == LegacyTurretType.FRITZ) {
            handleFritzFluidSlots();
        }
        if (!isManualArtilleryMode(type)) {
            this.targetQueue.clear();
        }
        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
            this.stattrak++;
        }
        if (this.target != null && !isManualArtilleryMode(type) && !entityInLOS(this.target)) {
            this.target = null;
        }
        if (isOn() && hasPower()) {
            Vec3 aimTarget = activeTargetPos(type);
            if (type == LegacyTurretType.HIMARS && (!himarsHasAmmo() || this.crane > 0.0F)) {
                turnTowardsAngle(0.0D, this.rotationYaw);
                if (this.aligned) {
                    updateHimarsLoading();
                }
            } else if (aimTarget != null) {
                turnTowards(aimTarget);
            }
            this.searchTimer--;
            this.power -= currentConsumption(type);
            if (this.searchTimer <= 0) {
                this.searchTimer = type.detectorInterval(this.mode);
                if (this.target == null && !isManualArtilleryMode(type)) {
                    seekTarget(level);
                }
            }
        } else {
            this.target = null;
            this.searchTimer = 0;
        }
        if (!isOn()) {
            this.targetQueue.clear();
        }
        if (activeTargetPos(type) == null && type == LegacyTurretType.FRIENDLY) {
            this.timer = Mth.clamp(this.timer - 1, 0, 20);
        }
        if (this.aligned) {
            updateFiring(level, type);
        }
        if (type == LegacyTurretType.RICHARD) {
            updateRichardReloadAfterFiring();
        }
        Vec3 activeTarget = activeTargetPos(type);
        if (activeTarget != null) {
            Vec3 targetPos = activeTarget;
            this.targetX = targetPos.x;
            this.targetY = targetPos.y;
            this.targetZ = targetPos.z;
            this.trackingTarget = true;
        } else {
            this.trackingTarget = false;
        }
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, type.maxPower());
        tickCasingDelay(level);
        setChanged();
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void updateSpecialReloads(LegacyTurretType type) {
        if ((type == LegacyTurretType.HOWARD || type == LegacyTurretType.HOWARD_DAMAGED)
                && this.loaded <= 0 && consumeFirstMatching(stack -> stack.is(HbmItems.AMMO_DGK.get()))) {
            this.loaded = 200;
            play(HbmSoundEvents.TURRET_HOWARD_RELOAD.get(), 4.0F, 1.0F);
        }
    }

    private void updateRichardReloadAfterFiring() {
        if (this.reload > 0 && --this.reload == 0) {
            this.loaded = 17;
        }
        if (this.loaded <= 0 && this.reload <= 0
                && firstStandardAmmo(StandardAmmoItem.AmmoFamily.ROCKET_ML) != StandardAmmoItem.StandardAmmoType.NONE) {
            this.reload = 100;
        }
        if (firstStandardAmmo(StandardAmmoItem.AmmoFamily.ROCKET_ML) == StandardAmmoItem.StandardAmmoType.NONE) {
            this.loaded = 0;
        }
    }

    private void updateHimarsLoading() {
        if (himarsHasAmmo()) {
            this.crane = Math.max(0.0F, this.crane - 0.0125F);
            return;
        }
        this.crane = Math.min(1.0F, this.crane + 0.0125F);
        if (this.crane < 1.0F) {
            return;
        }
        int slot = firstSlot(stack -> stack.is(HbmItems.AMMO_HIMARS.get()));
        if (slot < 0) {
            this.loaded = 0;
            return;
        }
        ItemStack stack = this.items.get(slot);
        LegacyProjectileUtil.HimarsType type = LegacyProjectileUtil.himarsType(stack);
        this.spareAmmo = type.modelData();
        this.loaded = type.amount();
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    private boolean himarsHasAmmo() {
        return this.spareAmmo >= 0 && this.loaded > 0;
    }

    private boolean isManualArtilleryMode(LegacyTurretType type) {
        return (type == LegacyTurretType.ARTY && this.mode == 2) || (type == LegacyTurretType.HIMARS && this.mode == 1);
    }

    @Nullable
    private Vec3 activeTargetPos(LegacyTurretType type) {
        if (isManualArtilleryMode(type) && !this.targetQueue.isEmpty()) {
            return this.targetQueue.getFirst();
        }
        return this.target == null ? null : entityPos(this.target);
    }

    private void updateFiring(Level level, LegacyTurretType type) {
        if ((type == LegacyTurretType.RICHARD && this.reload > 0)
                || (type == LegacyTurretType.HIMARS && this.crane > 0.0F)) {
            return;
        }
        this.timer++;
        if (this.timer % Math.max(1, type.fireDelay(this.mode)) != 0 || activeTargetPos(type) == null) {
            return;
        }
        if (type == LegacyTurretType.FRIENDLY && this.timer <= 20) {
            return;
        }
        switch (type) {
            case FRIENDLY -> fireStandard(level, StandardAmmoItem.AmmoFamily.R556, 10.0F, HbmSoundEvents.TURRET_CHEKHOV_FIRE.get(), 2.0F);
            case FRITZ -> fireFritz(level);
            case HOWARD -> fireHoward(level, false);
            case HOWARD_DAMAGED -> fireHoward(level, true);
            case MAXWELL -> fireMaxwell(level);
            case RICHARD -> fireRichard(level);
            case TAUON -> fireTauon(level);
            case ARTY -> fireArty(level);
            case HIMARS -> fireHimars(level);
            case SENTRY -> fireStandard(level, StandardAmmoItem.AmmoFamily.P9, 5.0F, HbmSoundEvents.TURRET_SENTRY_FIRE.get(), 2.0F);
            case SENTRY_DAMAGED -> fireDamagedSentry(level);
        }
    }

    private void fireStandard(Level level, StandardAmmoItem.AmmoFamily family, float baseDamage, net.minecraft.sounds.SoundEvent sound, float volume) {
        StandardAmmoItem.StandardAmmoType ammo = firstStandardAmmo(family);
        if (ammo == StandardAmmoItem.StandardAmmoType.NONE) {
            return;
        }
        spawnLegacyBullet(level, ammo, baseDamage, null);
        consumeFirstMatching(stack -> StandardAmmoItem.standardType(stack) == ammo);
        play(sound, volume, 1.0F);
        if (type() == LegacyTurretType.SENTRY) {
            spawnSentryMuzzleParticles(level, this.shotSide, 1);
        } else {
            spawnMuzzleParticles(level, 1);
        }
        if (type() == LegacyTurretType.SENTRY) {
            spawnSentryCasing(level, ammo);
            if (this.shotSide) {
                this.didShootLeft = true;
            } else {
                this.didShootRight = true;
            }
            this.shotSide = !this.shotSide;
        } else if (type() == LegacyTurretType.FRIENDLY) {
            spawnFriendlyCasing(level, ammo);
        }
    }

    private void fireFritz(Level level) {
        HbmFluidDefinition fluid = this.tank.type();
        if (fluid.isNone() || !fluid.hasTrait(HbmFluidTrait.FLAMMABLE) || !fluid.hasTrait(HbmFluidTrait.LIQUID) || this.tank.amount() < 2) {
            return;
        }
        this.tank.setAmount(this.tank.amount() - 2);
        LegacyBulletEntity flame = spawnLegacyBullet(
                level,
                StandardAmmoItem.StandardAmmoType.FLAME_DIESEL,
                Math.min(fluid.flammableHeatPerMillibucket() / 500_000.0F, 20.0F),
                null,
                0.05D
        );
        if (flame != null) {
            flame.setBalefire(fluid == HbmFluids.byName("balefire").orElse(HbmFluids.none()));
        }
        play(HbmSoundEvents.FLAMETHROWER_SHOOT.get(), 2.0F, 1.0F + level.random.nextFloat() * 0.5F);
    }

    private void fireHoward(Level level, boolean damaged) {
        if (!damaged && this.loaded <= 0) {
            return;
        }
        if (damaged) {
            play(HbmSoundEvents.TURRET_HOWARD_FIRE.get(), 4.0F, 0.7F + level.random.nextFloat() * 0.3F);
            spawnHowardCasing(level);
            if (level.random.nextInt(100) + 1 <= HbmConfig.CIWS_ACCURACY.get() * 0.5F) {
                damageTarget(level.damageSources().source(HbmDamageTypes.SHRAPNEL), 2.0F + level.random.nextInt(2));
            }
            spawnHowardMuzzleParticles(level, true);
            return;
        }
        play(HbmSoundEvents.TURRET_HOWARD_FIRE.get(), 4.0F, 0.9F + level.random.nextFloat() * 0.3F);
        play(HbmSoundEvents.TURRET_HOWARD_FIRE.get(), 4.0F, 1.0F + level.random.nextFloat() * 0.3F);
        spawnHowardCasing(level);
        spawnHowardCasing(level);
        if (this.timer % 2 == 0) {
            this.loaded--;
            if (level.random.nextInt(100) + 1 <= HbmConfig.CIWS_ACCURACY.get()) {
                damageTarget(level.damageSources().source(HbmDamageTypes.SHRAPNEL), 2.0F + level.random.nextInt(2));
            }
            spawnHowardMuzzleParticles(level, false);
        }
    }

    private void fireMaxwell(Level level) {
        long demand = currentConsumption(LegacyTurretType.MAXWELL) * 10L;
        if (this.power < demand) {
            return;
        }
        this.power -= demand;
        LivingEntity livingTarget = this.target instanceof LivingEntity living ? living : null;
        boolean wasAlive = livingTarget != null && livingTarget.isAlive();
        if (this.maxwell5g && this.target instanceof Player player) {
            player.addEffect(new MobEffectInstance(HbmMobEffects.DEATH, 30 * 60 * 20, 0, true, true));
        } else {
            damageTarget(level.damageSources().source(HbmDamageTypes.MICROWAVE), (this.maxwellBlackLevel * 10.0F + this.maxwellRedLevel + 1.0F) * 0.25F);
        }
        if (this.maxwellPinkLevel > 0 && this.target != null) {
            this.target.igniteForSeconds(this.maxwellPinkLevel * 3.0F);
        }
        if (wasAlive && livingTarget != null && !livingTarget.isAlive()) {
            TurretCasingEffects.spawnMaxwellGib(level, livingTarget, this.maxwellScrem);
        }
        this.didShootBeam = true;
    }

    private void fireRichard(Level level) {
        if (this.reload > 0) {
            return;
        }
        StandardAmmoItem.StandardAmmoType ammo = firstStandardAmmo(StandardAmmoItem.AmmoFamily.ROCKET_ML);
        if (ammo == StandardAmmoItem.StandardAmmoType.NONE) {
            this.loaded = 0;
            return;
        }
        LegacyBulletEntity rocket = spawnLegacyBullet(level, ammo, 30.0F, this.target);
        if (rocket == null) {
            return;
        }
        consumeFirstMatching(stack -> StandardAmmoItem.standardType(stack) == ammo);
        this.loaded--;
        play(HbmSoundEvents.TURRET_RICHARD_FIRE.get(), 2.0F, 1.0F);
    }

    private void fireTauon(Level level) {
        if (!consumeFirstMatching(stack -> StandardAmmoItem.standardType(stack) == StandardAmmoItem.StandardAmmoType.TAU_URANIUM)) {
            return;
        }
        if (this.target != null) {
            this.target.hurt(level.damageSources().source(HbmDamageTypes.ELECTRICITY), 30.0F + level.random.nextInt(11));
        }
        play(HbmSoundEvents.WEAPON_TAU_SHOOT.get(), 4.0F, 0.9F + level.random.nextFloat() * 0.3F);
        spawnTauMuzzleParticles(level);
        this.didShootBeam = true;
    }

    private void fireArty(Level level) {
        Vec3 targetPos = activeTargetPos(LegacyTurretType.ARTY);
        if (targetPos == null) {
            return;
        }
        int slot = firstSlot(stack -> stack.is(HbmItems.AMMO_ARTY.get()));
        if (slot >= 0) {
            ItemStack shellStack = this.items.get(slot);
            TurretCasingEffects.CasingKind casingKind = artilleryCasing(shellStack);
            spawnArtilleryShell(level, shellStack, targetPos);
            shellStack.shrink(1);
            if (shellStack.isEmpty()) {
                this.items.set(slot, ItemStack.EMPTY);
            }
            play(HbmSoundEvents.TURRET_JEREMY_FIRE.get(), 25.0F, 1.0F);
            this.didShootArtillery = true;
            scheduleCasing(casingKind, 7);
            spawnParticleAt(level, turretPos().add(barrelVector(type().barrelLength())), 0.0F, 5, 150.0D);
        }
        if (this.mode == 2 && !this.targetQueue.isEmpty()) {
            this.targetQueue.removeFirst();
        }
    }

    private void fireHimars(Level level) {
        if (!himarsHasAmmo() || this.crane > 0.0F) {
            return;
        }
        Vec3 targetPos = activeTargetPos(LegacyTurretType.HIMARS);
        if (targetPos == null) {
            return;
        }
        spawnHimarsRocket(level, targetPos);
        this.loaded--;
        play(HbmSoundEvents.WEAPON_ROCKET_FLAME.get(), 25.0F, 1.0F);
        if (this.mode == 1 && !this.targetQueue.isEmpty()) {
            this.targetQueue.removeFirst();
        }
    }

    private void fireDamagedSentry(Level level) {
        play(HbmSoundEvents.TURRET_SENTRY_FIRE.get(), 2.0F, this.shotSide ? 1.0F : 0.75F);
        if (this.shotSide) {
            spawnLegacyBullet(level, StandardAmmoItem.StandardAmmoType.P9_FMJ, 5.0F, null);
        }
        if (this.shotSide) {
            this.didShootLeft = true;
            spawnSentryMuzzleParticles(level, true, 1);
        } else {
            this.didShootRight = true;
            spawnSentryCasing(level);
            spawnParticleAt(level, turretPos(), 1.0F, 1, 50.0D);
        }
        this.shotSide = !this.shotSide;
    }

    private void scheduleCasing(TurretCasingEffects.CasingKind kind, int delay) {
        this.pendingCasing = kind;
        this.casingDelay = Math.max(0, delay);
        if (this.casingDelay == 0 && this.level != null) {
            spawnPendingCasing(this.level);
        }
    }

    private void tickCasingDelay(Level level) {
        if (this.pendingCasing == null) {
            return;
        }
        if (this.casingDelay > 0) {
            this.casingDelay--;
            return;
        }
        spawnPendingCasing(level);
    }

    private void spawnPendingCasing(Level level) {
        TurretCasingEffects.CasingKind kind = this.pendingCasing;
        this.pendingCasing = null;
        if (kind == null) {
            return;
        }
        if (kind == TurretCasingEffects.CasingKind.ARTY_16
                || kind == TurretCasingEffects.CasingKind.ARTY_16_PHOS
                || kind == TurretCasingEffects.CasingKind.ARTY_16_NUKE) {
            Vec3 spawn = turretPos();
            TurretCasingEffects.spawnDirectCasing(level, spawn, this.rotationYaw, this.rotationPitch, kind,
                    -0.6D, 0.3D, 0.0D, 0.01D,
                    level.random.nextFloat() * 20.0F - 10.0F, 0.0F,
                    200, 1.0D, 20);
        }
    }

    private void spawnFriendlyCasing(Level level, StandardAmmoItem.StandardAmmoType ammo) {
        Vec3 spawn = turretPos().add(TurretCasingEffects.localVector(-1.125D, 0.125D, 0.25D, this.rotationYaw, this.rotationPitch));
        TurretCasingEffects.CasingKind casing = ammo == StandardAmmoItem.StandardAmmoType.R556_AP
                ? TurretCasingEffects.CasingKind.FRIENDLY_STEEL
                : TurretCasingEffects.CasingKind.FRIENDLY;
        TurretCasingEffects.spawnEjectorCasing(level, spawn, this.rotationYaw, this.rotationPitch,
                casing, -0.3D, 0.6D, 0.0D, 0.02D, 0.05D, 1);
    }

    private void spawnSentryCasing(Level level) {
        spawnSentryCasing(level, StandardAmmoItem.StandardAmmoType.P9_FMJ);
    }

    private void spawnSentryCasing(Level level, StandardAmmoItem.StandardAmmoType ammo) {
        Vec3 spawn = turretPos().add(TurretCasingEffects.localVector(0.0D, 0.25D, -0.125D, this.rotationYaw, this.rotationPitch));
        TurretCasingEffects.CasingKind casing = ammo == StandardAmmoItem.StandardAmmoType.P9_AP
                ? TurretCasingEffects.CasingKind.P9_STEEL
                : TurretCasingEffects.CasingKind.P9;
        TurretCasingEffects.spawnEjectorCasing(level, spawn, this.rotationYaw, this.rotationPitch,
                casing, 0.2D, 0.2D, 0.0D, 0.01D, 0.01D, 1);
    }

    private void spawnHowardCasing(Level level) {
        Vec3 spawn = turretPos().add(TurretCasingEffects.localVector(-0.875D, 0.2D, -0.125D, this.rotationYaw, this.rotationPitch));
        TurretCasingEffects.spawnEjectorCasing(level, spawn, this.rotationYaw, this.rotationPitch,
                TurretCasingEffects.CasingKind.HOWARD, 0.4D, 0.0D, 0.0D, 0.02D, 0.03D, 1);
    }

    @Nullable
    private LegacyBulletEntity spawnLegacyBullet(Level level, StandardAmmoItem.StandardAmmoType ammo, float baseDamage, @Nullable Entity lockon) {
        return spawnLegacyBullet(level, ammo, baseDamage, lockon, 0.0D);
    }

    @Nullable
    private LegacyBulletEntity spawnLegacyBullet(Level level, StandardAmmoItem.StandardAmmoType ammo, float baseDamage, @Nullable Entity lockon, double spread) {
        Vec3 pos = turretPos();
        Vec3 dir = barrelVector(1.0D).normalize();
        if (spread > 0.0D) {
            dir = dir.add(
                    level.random.nextGaussian() * spread,
                    level.random.nextGaussian() * spread,
                    level.random.nextGaussian() * spread
            ).normalize();
        }
        Vec3 muzzle = pos.add(barrelVector(type().barrelLength()));
        LegacyBulletEntity bullet = new LegacyBulletEntity(level, muzzle.x, muzzle.y, muzzle.z, dir, ammo, baseDamage);
        if (lockon != null) {
            bullet.setLockonTarget(lockon);
        }
        level.addFreshEntity(bullet);
        return bullet;
    }

    private void spawnArtilleryShell(Level level, ItemStack shellStack, Vec3 targetPos) {
        Vec3 pos = turretPos();
        Vec3 vec = barrelVector(type().barrelLength());
        Vec3 muzzle = pos.add(vec);
        Vec3 velocity = vec.normalize().scale(this.mode == 1 ? 20.0D : 50.0D);
        int shellType = LegacyProjectileUtil.artyType(shellStack).modelData();
        Vec3 legacyTarget = new Vec3((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);
        LegacyArtilleryShellEntity shell = new LegacyArtilleryShellEntity(level, muzzle, velocity, shellType, legacyTarget, this.mode != 1);
        if (level instanceof ServerLevel serverLevel) {
            shell.setCargo(AmmoArtyItem.cargo(shellStack, serverLevel.registryAccess()));
        }
        level.addFreshEntity(shell);
    }

    private void spawnHimarsRocket(Level level, Vec3 targetPos) {
        Vec3 pos = turretPos();
        Vec3 vec = barrelVector(type().barrelLength());
        Vec3 muzzle = pos.add(vec);
        LegacyHimarsRocketEntity rocket = new LegacyHimarsRocketEntity(level, muzzle, vec.normalize(), this.spareAmmo, targetPos);
        if (this.target != null) {
            rocket.setTarget(this.target);
        }
        level.addFreshEntity(rocket);
    }

    private void damageTarget(DamageSource source, float damage) {
        if (this.target != null) {
            LegacyProjectileUtil.hurtNoIFrame(this.target, source, Math.max(0.0F, damage));
        }
    }

    private void explodeNearTarget(Level level, float radius) {
        if (this.target == null) {
            return;
        }
        Vec3 pos = entityPos(this.target);
        level.explode(null, pos.x, pos.y, pos.z, radius, Level.ExplosionInteraction.BLOCK);
    }

    private void spawnMuzzleParticles(Level level, int count) {
        Vec3 muzzle = turretPos().add(barrelVector(type().barrelLength()));
        spawnParticleAt(level, muzzle, 1.5F, count, 50.0D);
    }

    private void spawnSentryMuzzleParticles(Level level, boolean leftBarrel, int count) {
        double sideOffset = leftBarrel ? 0.125D : -0.125D;
        Vec3 side = TurretCasingEffects.localVector(0.0D, 0.0D, -sideOffset, this.rotationYaw, 0.0D);
        Vec3 muzzle = turretPos().add(barrelVector(type().barrelLength())).add(side);
        spawnParticleAt(level, muzzle, 1.0F, count, 50.0D);
    }

    private void spawnHowardMuzzleParticles(Level level, boolean damaged) {
        Vec3 muzzle = turretPos().add(barrelVector(type().barrelLength()));
        Vec3 offset = TurretCasingEffects.localVector(0.0D, 0.25D, 0.0D, this.rotationYaw, this.rotationPitch);
        spawnParticleAt(level, muzzle.add(offset), 1.5F, 1, 50.0D);
        if (!damaged) {
            spawnParticleAt(level, muzzle.subtract(offset), 1.5F, 1, 50.0D);
        }
    }

    private void spawnTauMuzzleParticles(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 muzzle = turretPos().add(barrelVector(type().barrelLength()));
        for (int i = 0; i < 5; i++) {
            serverLevel.sendParticles(
                    HbmParticleTypes.TAU_SPARK.get(),
                    muzzle.x,
                    muzzle.y,
                    muzzle.z,
                    0,
                    level.random.nextGaussian() * 0.05D,
                    0.05D,
                    level.random.nextGaussian() * 0.05D,
                    1.0D
            );
        }
        serverLevel.sendParticles(HbmParticleTypes.TAU_HADRON.get(), muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnParticleAt(Level level, Vec3 position, float size, int count, double range) {
        TurretCasingEffects.spawnMuzzleFlash(level, position, size, count, range);
    }

    private static TurretCasingEffects.CasingKind artilleryCasing(ItemStack stack) {
        if (!(stack.getItem() instanceof AmmoArtyItem item)) {
            return TurretCasingEffects.CasingKind.ARTY_16;
        }
        return switch (item.variant(stack).modelData()) {
            case 5, 7 -> TurretCasingEffects.CasingKind.ARTY_16_PHOS;
            case 3, 4, 6, 10, 11 -> TurretCasingEffects.CasingKind.ARTY_16_NUKE;
            default -> TurretCasingEffects.CasingKind.ARTY_16;
        };
    }

    private void play(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void handleFritzFluidSlots() {
        HbmFluidDefinition diesel = HbmFluids.byName("diesel").orElse(HbmFluids.none());
        ItemStack identifier = this.items.get(AMMO_END - 1);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition selected = FluidIdentifierItem.primary(identifier);
            if (this.tank.type() != selected) {
                this.tank.setType(selected);
            }
        }
        for (int slot = AMMO_START; slot < AMMO_END - 1; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (StandardAmmoItem.standardType(stack) == StandardAmmoItem.StandardAmmoType.FLAME_DIESEL
                    && this.tank.type() == diesel
                    && this.tank.amount() + 1000 <= this.tank.capacity()) {
                this.tank.setAmount(this.tank.amount() + 1000);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
                continue;
            }
            final int outputSlot = AMMO_END - 1;
            HbmFluidContainerTransfer.drainIntoTank(
                    stack,
                    this.tank,
                    ignored -> true,
                    output -> canMergeOutput(outputSlot, output),
                    output -> mergeOutput(outputSlot, output)
            );
        }
        ItemStack lastAmmoSlot = this.items.get(AMMO_END - 1);
        if (StandardAmmoItem.standardType(lastAmmoSlot) == StandardAmmoItem.StandardAmmoType.FLAME_DIESEL
                && this.tank.type() == diesel
                && this.tank.amount() + 1000 <= this.tank.capacity()) {
            this.tank.setAmount(this.tank.amount() + 1000);
            lastAmmoSlot.shrink(1);
            if (lastAmmoSlot.isEmpty()) {
                this.items.set(AMMO_END - 1, ItemStack.EMPTY);
            }
        }
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (type() != LegacyTurretType.FRITZ || side == null || side == Direction.UP || side == Direction.DOWN) {
            return null;
        }
        for (ConnectorSpec spec : connectorSpecs(this.worldPosition, facing(), type())) {
            BlockPos machinePort = spec.pos().relative(spec.machineSide().getOpposite());
            if (machinePort.equals(queriedPos) && spec.machineSide() == side) {
                return this.tank;
            }
        }
        return null;
    }

    private void seekTarget(Level level) {
        Vec3 pos = turretPos();
        int green = type() == LegacyTurretType.MAXWELL ? this.maxwellGreenLevel : 0;
        double range = type().detectorRange(green, this.mode);
        AABB area = new AABB(pos, pos).inflate(range);
        Entity best = null;
        double closest = range;
        for (Entity entity : level.getEntities((Entity) null, area, this::acceptableTarget)) {
            Vec3 entityPos = entityPos(entity);
            double distance = entityPos.distanceTo(pos);
            if (distance >= closest || distance < type().detectorGrace(this.mode) || distance > range) {
                continue;
            }
            if (!entityInLOS(entity)) {
                continue;
            }
            closest = distance;
            best = entity;
        }
        this.target = best;
        if ((type() == LegacyTurretType.SENTRY || type() == LegacyTurretType.SENTRY_DAMAGED) && best != null) {
            level.playSound(
                    null,
                    best.getX(), best.getY(), best.getZ(),
                    HbmSoundEvents.TURRET_SENTRY_LOCKON.get(),
                    SoundSource.BLOCKS,
                    2.0F,
                    1.5F
            );
        }
    }

    private boolean entityInLOS(Entity entity) {
        if (!hasThermalVision(type()) && entity instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY)) {
            return false;
        }
        Vec3 pos = turretPos();
        Vec3 entityPos = entityPos(entity);
        Vec3 delta = entityPos.subtract(pos);
        double distance = delta.length();
        int green = type() == LegacyTurretType.MAXWELL ? this.maxwellGreenLevel : 0;
        double range = type().detectorRange(green, this.mode);
        if (distance < type().detectorGrace(this.mode) || distance > range * 1.1D) {
            return false;
        }
        if (this.type() == LegacyTurretType.ARTY && this.mode != 1) {
            return artilleryHasSkyLine(entity);
        }
        if (this.type() == LegacyTurretType.HIMARS) {
            return artilleryHasSkyLine(entity);
        }
        double pitchDegrees = Math.toDegrees(Math.asin(delta.y / distance));
        if (pitchDegrees < -type().depression() || pitchDegrees > type().elevation()) {
            return false;
        }
        return this.level == null || this.level.clip(new ClipContext(
                entityPos,
                pos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        )).getType() == HitResult.Type.MISS;
    }

    private boolean artilleryHasSkyLine(Entity entity) {
        if (this.level == null) {
            return true;
        }
        int terrainHeight = this.level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(entity.getX()),
                Mth.floor(entity.getZ())
        );
        return terrainHeight < entity.getY() + entity.getBbHeight();
    }

    private static boolean hasThermalVision(LegacyTurretType type) {
        return type != LegacyTurretType.SENTRY && type != LegacyTurretType.SENTRY_DAMAGED && type != LegacyTurretType.HOWARD_DAMAGED;
    }

    private boolean acceptableTarget(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (type().damaged()) {
            return entity instanceof LivingEntity && !(entity instanceof Player player && player.getAbilities().instabuild);
        }
        if (LegacyTurretTargeting.blacklisted(entity)) {
            return false;
        }
        int conditionalResult = LegacyTurretTargeting.conditionalResult(entity, this);
        if (conditionalResult != 0) {
            return conditionalResult > 0;
        }
        List<String> whitelist = whitelist();
        if (entity instanceof Player player) {
            if (whitelist.contains(player.getDisplayName().getString())) {
                return false;
            }
        }
        if (entity instanceof Mob mob && mob.hasCustomName() && whitelist.contains(mob.getName().getString())) {
            return false;
        }
        if (this.targetAnimals && (passiveTarget(entity) || LegacyTurretTargeting.friendlyTarget(entity))) {
            return true;
        }
        if (this.targetMobs) {
            if (entity instanceof EnderDragon) {
                return false;
            }
            if (entity instanceof EnderDragonPart || entity instanceof Enemy || LegacyTurretTargeting.hostileTarget(entity)) {
                return true;
            }
        }
        if (this.targetMachines) {
            if (!LegacyTurretTargeting.radarVisible(entity, this)) {
                return false;
            }
            if (LegacyTurretTargeting.machineTarget(entity, this)) {
                return true;
            }
        }
        if (this.targetPlayers) {
            if (entity instanceof FakePlayer) {
                return false;
            }
            return entity instanceof Player || LegacyTurretTargeting.playerTarget(entity);
        }
        return false;
    }

    private static boolean passiveTarget(Entity entity) {
        return entity instanceof Animal
                || entity instanceof WaterAnimal
                || entity instanceof AmbientCreature
                || entity instanceof AbstractGolem
                || entity instanceof Npc;
    }

    private void turnTowards(Vec3 targetPos) {
        if (type() == LegacyTurretType.ARTY) {
            turnArtilleryTowards(targetPos);
            return;
        }
        Vec3 pos = turretPos();
        Vec3 delta = targetPos.subtract(pos);
        double targetPitch = Math.asin(delta.y / delta.length());
        double targetYaw = -Math.atan2(delta.x, delta.z);
        if (type() == LegacyTurretType.HIMARS) {
            targetPitch = Math.PI / 4.0D;
        }
        turnTowardsAngle(targetPitch, targetYaw);
    }

    private void turnArtilleryTowards(Vec3 targetPos) {
        Vec3 pos = turretPos().add(barrelVector(type().barrelLength()));
        Vec3 delta = targetPos.subtract(pos);
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double y = delta.y;
        double v0 = this.mode == 1 ? 20.0D : 50.0D;
        double v02 = v0 * v0;
        double g = 9.81D * 0.05D;
        double disc = v02 * v02 - g * (g * x * x + 2.0D * y * v02);
        double upperLower = this.mode == 1 ? -1.0D : 1.0D;
        double targetPitch = Math.atan((v02 + Math.sqrt(disc) * upperLower) / (g * x));
        turnTowardsAngle(targetPitch, targetYaw);
    }

    private void turnTowardsAngle(double targetPitch, double targetYaw) {
        double turnYaw = Math.toRadians(type().yawSpeed());
        double turnPitch = Math.toRadians(type().pitchSpeed());
        double twoPi = Math.PI * 2.0D;
        if (Math.abs(this.rotationPitch - targetPitch) < turnPitch || Math.abs(this.rotationPitch - targetPitch) > twoPi - turnPitch) {
            this.rotationPitch = targetPitch;
        } else if (targetPitch > this.rotationPitch) {
            this.rotationPitch += turnPitch;
        } else {
            this.rotationPitch -= turnPitch;
        }
        double deltaYaw = (targetYaw - this.rotationYaw) % twoPi;
        int dir = 0;
        if (deltaYaw < -Math.PI) {
            dir = 1;
        } else if (deltaYaw < 0.0D) {
            dir = -1;
        } else if (deltaYaw > Math.PI) {
            dir = -1;
        } else if (deltaYaw > 0.0D) {
            dir = 1;
        }
        if (Math.abs(this.rotationYaw - targetYaw) < turnYaw || Math.abs(this.rotationYaw - targetYaw) > twoPi - turnYaw) {
            this.rotationYaw = targetYaw;
        } else {
            this.rotationYaw += turnYaw * dir;
        }
        double dPitch = targetPitch - this.rotationPitch;
        double dYaw = targetYaw - this.rotationYaw;
        this.rotationYaw = this.rotationYaw % twoPi;
        this.rotationPitch = this.rotationPitch % twoPi;
        this.aligned = Math.sqrt(dYaw * dYaw + dPitch * dPitch) <= Math.toRadians(type().acceptableInaccuracy());
    }

    public Vec3 turretPos() {
        Vec3 offset = horizontalOffset();
        return Vec3.atLowerCornerOf(this.worldPosition).add(offset.x, type().heightOffset(), offset.z);
    }

    public Vec3 horizontalOffset() {
        LegacyTurretType type = type();
        if (type.layout() == LegacyTurretType.Layout.SENTRY) {
            return new Vec3(0.5D, 0.0D, 0.5D);
        }
        return horizontalOffset(facing());
    }

    public static Vec3 horizontalOffset(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(1.0D, 0.0D, 1.0D);
            case WEST -> new Vec3(1.0D, 0.0D, 0.0D);
            case EAST -> new Vec3(0.0D, 0.0D, 1.0D);
            default -> new Vec3(0.0D, 0.0D, 0.0D);
        };
    }

    public Direction facing() {
        return this.getBlockState().hasProperty(LegacyTurretBlock.FACING)
                ? this.getBlockState().getValue(LegacyTurretBlock.FACING)
                : Direction.NORTH;
    }

    public double renderYaw(float partialTick) {
        return Mth.lerp(partialTick, this.prevRotationYaw, this.rotationYaw);
    }

    public double renderPitch(float partialTick) {
        return Mth.lerp(partialTick, this.prevRotationPitch, this.rotationPitch);
    }

    public float renderSpin(float partialTick) {
        return Mth.lerp(partialTick, this.lastSpin, this.spin);
    }

    public float renderCrane(float partialTick) {
        return Mth.lerp(partialTick, this.lastCrane, this.crane);
    }

    public int loadedAmmo() {
        return this.loaded;
    }

    public int spareAmmoModelData() {
        return this.spareAmmo;
    }

    public double renderLeftBarrel(float partialTick) {
        return Mth.lerp(partialTick, this.lastBarrelLeftPos, this.barrelLeftPos);
    }

    public double renderRightBarrel(float partialTick) {
        return Mth.lerp(partialTick, this.lastBarrelRightPos, this.barrelRightPos);
    }

    public double renderArtilleryBarrel(float partialTick) {
        return Mth.lerp(partialTick, this.lastArtilleryBarrelPos, this.artilleryBarrelPos);
    }

    public int beamTicks() {
        return this.beamTicks;
    }

    public double beamDistance() {
        return this.beamDistance;
    }

    private Vec3 barrelVector(double length) {
        double x = length;
        double y = 0.0D;
        double z = 0.0D;
        double cosPitch = Math.cos(-this.rotationPitch);
        double sinPitch = Math.sin(-this.rotationPitch);
        double py = y * cosPitch - x * sinPitch;
        double px = y * sinPitch + x * cosPitch;
        double yaw = -(this.rotationYaw + Math.PI * 0.5D);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double rx = px * cosYaw + z * sinYaw;
        double rz = z * cosYaw - px * sinYaw;
        return new Vec3(rx, py, rz);
    }

    private static Vec3 entityPos(Entity entity) {
        return new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
    }

    private StandardAmmoItem.StandardAmmoType firstStandardAmmo(StandardAmmoItem.AmmoFamily family) {
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            StandardAmmoItem.StandardAmmoType type = StandardAmmoItem.standardType(this.items.get(slot));
            if (type.family() == family) {
                return type;
            }
        }
        return StandardAmmoItem.StandardAmmoType.NONE;
    }

    private boolean consumeFirstMatching(java.util.function.Predicate<ItemStack> predicate) {
        int slot = firstSlot(predicate);
        if (slot < 0) {
            return false;
        }
        ItemStack stack = this.items.get(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        return true;
    }

    private int firstSlot(java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private int upgradeLevel(String prefix) {
        int level = 0;
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this.items.get(slot).getItem());
            if (key != null && key.getNamespace().equals(ReinhardtsHBM.MOD_ID) && key.getPath().startsWith(prefix)) {
                String suffix = key.getPath().substring(prefix.length());
                try {
                    level += Integer.parseInt(suffix);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return level;
    }

    private boolean hasUpgrade(String id) {
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this.items.get(slot).getItem());
            if (key != null && key.getNamespace().equals(ReinhardtsHBM.MOD_ID) && key.getPath().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void updateMaxwellUpgradeCache() {
        if (type() != LegacyTurretType.MAXWELL) {
            return;
        }
        if (this.maxwellUpgradeCheckDelay <= 0) {
            this.maxwellUpgradeCheckDelay = 20;
            this.maxwellRedLevel = upgradeLevel("upgrade_speed_");
            this.maxwellGreenLevel = upgradeLevel("upgrade_effect_");
            this.maxwellBlueLevel = upgradeLevel("upgrade_power_");
            this.maxwellBlackLevel = upgradeLevel("upgrade_overdrive_");
            this.maxwellPinkLevel = upgradeLevel("upgrade_afterburn_");
            this.maxwell5g = hasUpgrade("upgrade_5g");
            this.maxwellScrem = hasUpgrade("upgrade_screm");
        }
        this.maxwellUpgradeCheckDelay--;
    }

    private boolean canMergeOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack existing = this.items.get(slot);
        return existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, output) && existing.getCount() + output.getCount() <= existing.getMaxStackSize());
    }

    private void mergeOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack existing = this.items.get(slot);
        if (existing.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(existing, output)) {
            existing.grow(output.getCount());
        }
    }

    public void toggle(int id) {
        switch (id) {
            case 0 -> this.on = !this.on;
            case 1 -> this.targetPlayers = !this.targetPlayers;
            case 2 -> this.targetAnimals = !this.targetAnimals;
            case 3 -> this.targetMobs = !this.targetMobs;
            case 4 -> this.targetMachines = !this.targetMachines;
            case 5 -> toggleMode();
            default -> {
            }
        }
        setChanged();
    }

    private void toggleMode() {
        if (type() == LegacyTurretType.ARTY) {
            this.mode = (this.mode + 1) % 3;
            this.target = null;
            this.targetQueue.clear();
        } else if (type() == LegacyTurretType.HIMARS) {
            this.mode = (this.mode + 1) % 2;
            this.target = null;
            this.targetQueue.clear();
        }
    }

    public boolean enqueueTarget(double x, double y, double z) {
        LegacyTurretType type = type();
        if (type != LegacyTurretType.ARTY && type != LegacyTurretType.HIMARS) {
            return false;
        }
        Vec3 targetPos = new Vec3(x, y, z);
        if (targetPos.distanceTo(turretPos()) > type.detectorRange(0, this.mode)) {
            return false;
        }
        this.targetQueue.add(targetPos);
        setChanged();
        return true;
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        // TileEntityTurretBaseArtillery queued block-center coordinates.
        return enqueueTarget(x + 0.5D, y, z + 0.5D);
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        return enqueueTarget(target.getX(), target.getY(), target.getZ());
    }

    public void addWhitelistName(String name) {
        if (name.isEmpty() || name.length() > 25 || !isChip(this.items.get(CHIP_SLOT))) {
            return;
        }
        List<String> names = whitelist();
        if (names.contains(name)) {
            return;
        }
        names.add(name);
        writeWhitelist(names);
    }

    public void removeWhitelistName(int index) {
        if (!isChip(this.items.get(CHIP_SLOT))) {
            return;
        }
        List<String> names = whitelist();
        if (index < 0 || index >= names.size()) {
            return;
        }
        names.remove(index);
        writeWhitelist(names);
    }

    public List<String> whitelist() {
        ItemStack stack = this.items.get(CHIP_SLOT);
        if (!isChip(stack)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(TurretBiometryItem.names(stack));
    }

    private void writeWhitelist(List<String> names) {
        ItemStack stack = this.items.get(CHIP_SLOT);
        TurretBiometryItem.writeNames(stack, names);
        setChanged();
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public boolean isOn() {
        return type().damaged() || this.on;
    }

    public boolean hasPower() {
        LegacyTurretType type = type();
        return type.damaged() || this.power >= currentConsumption(type);
    }

    private long currentConsumption(LegacyTurretType type) {
        if (type != LegacyTurretType.MAXWELL) {
            return type.consumption();
        }
        if (this.maxwell5g) {
            return 10L;
        }
        return Math.max(0L, 10_000L - this.maxwellBlueLevel * 300L);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorSpecs(this.worldPosition, facing(), type()).stream().map(ConnectorSpec::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        for (ConnectorSpec spec : connectorSpecs(this.worldPosition, facing(), type())) {
            if (spec.pos().equals(connectorPos) && spec.machineSide() == machineSide) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        LegacyTurretType type = type();
        return this.power >= type.maxPower() ? 0L : type.maxPower() - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        LegacyTurretType type = type();
        this.power = Math.min(type.maxPower(), this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        LegacyTurretType type = type();
        return Component.translatable("message.reinhardtshbm.power.legacy_turret", currentConsumption(type), this.power, type.maxPower());
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null
                && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D
        ) <= 128.0D;
    }

    @Override
    public void startOpen(Player player) {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.OPEN_C.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.CLOSE_C.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (type() != LegacyTurretType.SENTRY_DAMAGED) {
            Containers.dropContents(level, pos, this);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return type() == LegacyTurretType.FRITZ ? FRITZ_INPUT_SLOTS : AMMO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= AMMO_START && slot < AMMO_END;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm." + type().id());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return type().hasMenu() ? new LegacyTurretMenu(containerId, playerInventory, this, this.menuData) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putLong("power", this.power);
        tag.putLong("last_input", this.lastInput);
        tag.putBoolean("on", this.on);
        tag.putBoolean("target_players", this.targetPlayers);
        tag.putBoolean("target_animals", this.targetAnimals);
        tag.putBoolean("target_mobs", this.targetMobs);
        tag.putBoolean("target_machines", this.targetMachines);
        tag.putInt("search_timer", this.searchTimer);
        tag.putInt("timer", this.timer);
        tag.putInt("reload", this.reload);
        tag.putInt("loaded", this.loaded);
        tag.putInt("stattrak", this.stattrak);
        tag.putInt("mode", this.mode);
        tag.putInt("spare_ammo", this.spareAmmo);
        tag.putFloat("crane", this.crane);
        tag.putDouble("rotation_yaw", this.rotationYaw);
        tag.putDouble("rotation_pitch", this.rotationPitch);
        tag.put("tank", this.tank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, this.items, registries);
        this.power = tag.getLong("power");
        this.lastInput = tag.getLong("last_input");
        this.on = tag.getBoolean("on");
        this.targetPlayers = tag.getBoolean("target_players");
        this.targetAnimals = tag.getBoolean("target_animals");
        this.targetMobs = !tag.contains("target_mobs") || tag.getBoolean("target_mobs");
        this.targetMachines = !tag.contains("target_machines") || tag.getBoolean("target_machines");
        this.searchTimer = tag.getInt("search_timer");
        this.timer = tag.getInt("timer");
        this.reload = tag.getInt("reload");
        this.loaded = tag.getInt("loaded");
        this.stattrak = tag.getInt("stattrak");
        this.mode = tag.getInt("mode");
        this.spareAmmo = tag.getInt("spare_ammo");
        float oldCrane = this.crane;
        this.crane = tag.getFloat("crane");
        double oldYaw = this.rotationYaw;
        double oldPitch = this.rotationPitch;
        this.rotationYaw = tag.getDouble("rotation_yaw");
        this.rotationPitch = tag.getDouble("rotation_pitch");
        if (tag.contains("tracking_target")) {
            this.trackingTarget = tag.getBoolean("tracking_target");
            this.targetX = tag.getDouble("target_x");
            this.targetY = tag.getDouble("target_y");
            this.targetZ = tag.getDouble("target_z");
            if (this.trackingTarget) {
                this.beamDistance = new Vec3(this.targetX, this.targetY, this.targetZ).distanceTo(turretPos());
            }
        }
        if (this.level != null && this.level.isClientSide) {
            this.prevRotationYaw = oldYaw;
            this.prevRotationPitch = oldPitch;
            this.lastCrane = oldCrane;
            if (tag.getBoolean("did_shoot_left")) {
                this.retractingLeft = true;
            }
            if (tag.getBoolean("did_shoot_right")) {
                this.retractingRight = true;
            }
            if (tag.getBoolean("did_shoot_artillery")) {
                this.retractingArtillery = true;
            }
            if (tag.getBoolean("did_shoot_beam")) {
                this.beamTicks = type() == LegacyTurretType.MAXWELL ? 5 : 3;
            }
        } else {
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
            this.lastCrane = this.crane;
        }
        if (tag.contains("tank")) {
            CompoundTag tankTag = tag.getCompound("tank");
            this.tank.load(tankTag);
            if (type() == LegacyTurretType.FRITZ && this.tank.amount() == 0) {
                HbmFluidDefinition savedType = HbmFluids.byName(tankTag.getString("type"))
                        .orElse(HbmFluids.byName("diesel").orElse(HbmFluids.none()));
                this.tank.setType(savedType);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putBoolean("tracking_target", this.trackingTarget);
        tag.putDouble("target_x", this.targetX);
        tag.putDouble("target_y", this.targetY);
        tag.putDouble("target_z", this.targetZ);
        tag.putBoolean("did_shoot_left", this.didShootLeft);
        tag.putBoolean("did_shoot_right", this.didShootRight);
        tag.putBoolean("did_shoot_artillery", this.didShootArtillery);
        tag.putBoolean("did_shoot_beam", this.didShootBeam);
        if (this.level == null || !this.level.isClientSide) {
            this.didShootLeft = false;
            this.didShootRight = false;
            this.didShootArtillery = false;
            this.didShootBeam = false;
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static boolean isChip(ItemStack stack) {
        return stack.is(HbmItems.TURRET_CHIP.get());
    }

    public static List<ConnectorSpec> connectorSpecs(BlockPos corePos, Direction facing, LegacyTurretType type) {
        if (type.layout() == LegacyTurretType.Layout.SENTRY) {
            return List.of(new ConnectorSpec(corePos.below(), Direction.DOWN, 0.0F, 0.0F));
        }
        if (type.layout() == LegacyTurretType.Layout.ARTILLERY) {
            return artilleryConnectorSpecs(corePos, facing);
        }
        Vec3 offset = horizontalOffset(facing);
        BlockPos anchor = corePos.offset((int) offset.x, 0, (int) offset.z);
        return List.of(
                new ConnectorSpec(anchor.offset(-2, 0, 0), Direction.WEST, 0.0F, 0.0F),
                new ConnectorSpec(anchor.offset(-2, 0, -1), Direction.WEST, 0.0F, -1.0F),
                new ConnectorSpec(anchor.offset(-1, 0, 1), Direction.SOUTH, 0.0F, -1.0F),
                new ConnectorSpec(anchor.offset(0, 0, 1), Direction.SOUTH, 0.0F, 0.0F),
                new ConnectorSpec(anchor.offset(1, 0, 0), Direction.EAST, 0.0F, -1.0F),
                new ConnectorSpec(anchor.offset(1, 0, -1), Direction.EAST, 0.0F, 0.0F),
                new ConnectorSpec(anchor.offset(0, 0, -2), Direction.NORTH, 0.0F, -1.0F),
                new ConnectorSpec(anchor.offset(-1, 0, -2), Direction.NORTH, 0.0F, 0.0F)
        );
    }

    private static List<ConnectorSpec> artilleryConnectorSpecs(BlockPos corePos, Direction facing) {
        Direction dir = facing.getOpposite();
        Direction rot = dir.getClockWise();
        ArrayList<ConnectorSpec> specs = new ArrayList<>();
        for (int y = 0; y <= 1; y++) {
            for (int j = 0; j < 4; j++) {
                specs.add(new ConnectorSpec(corePos.relative(dir, -1 + j).relative(rot, -3).above(y), Direction.SOUTH, 0.0F, 0.0F));
                specs.add(new ConnectorSpec(corePos.relative(dir, -1 + j).relative(rot, 2).above(y), Direction.NORTH, 0.0F, 0.0F));
                specs.add(new ConnectorSpec(corePos.relative(dir, -2).relative(rot, 1 - j).above(y), Direction.EAST, 0.0F, 0.0F));
                specs.add(new ConnectorSpec(corePos.relative(dir, 3).relative(rot, 1 - j).above(y), Direction.WEST, 0.0F, 0.0F));
            }
        }
        return specs;
    }

    public record ConnectorSpec(BlockPos pos, Direction machineSide, float renderOffsetX, float renderOffsetZ) {
    }
}
