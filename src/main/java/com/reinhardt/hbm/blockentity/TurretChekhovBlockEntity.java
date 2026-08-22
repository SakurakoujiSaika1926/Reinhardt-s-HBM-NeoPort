package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.entity.ChekhovBulletEntity;
import com.reinhardt.hbm.entity.LegacyTurretTargeting;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.item.TurretBiometryItem;
import com.reinhardt.hbm.menu.TurretChekhovMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
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
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TurretChekhovBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int CHIP_SLOT = 0;
    public static final int AMMO_START = 1;
    public static final int AMMO_END = 10;
    public static final int BATTERY_SLOT = 10;
    public static final int SLOT_COUNT = 11;
    public static final int DATA_COUNT = 12;
    public static final long MAX_POWER = 10_000L;
    public static final int CONSUMPTION = 100;
    public static final double DETECTOR_GRACE = 3.0D;
    public static final double DETECTOR_RANGE = 32.0D;
    public static final double DEPRESSION = 30.0D;
    public static final double ELEVATION = 45.0D;
    public static final double BARREL_LENGTH = 3.5D;
    private static final double ACCEPTABLE_INACCURACY = 15.0D;
    private static final int FIRE_DELAY = 2;
    private static final int[] AMMO_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
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
    private int stattrak;
    private double rotationYaw;
    private double rotationPitch;
    private double prevRotationYaw;
    private double prevRotationPitch;
    private float spin;
    private float prevSpin;
    private float spinAccel;
    @Nullable
    private Entity target;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) TurretChekhovBlockEntity.this.power;
                case 1 -> (int) TurretChekhovBlockEntity.this.lastInput;
                case 2 -> TurretChekhovBlockEntity.this.on ? 1 : 0;
                case 3 -> TurretChekhovBlockEntity.this.targetPlayers ? 1 : 0;
                case 4 -> TurretChekhovBlockEntity.this.targetAnimals ? 1 : 0;
                case 5 -> TurretChekhovBlockEntity.this.targetMobs ? 1 : 0;
                case 6 -> TurretChekhovBlockEntity.this.targetMachines ? 1 : 0;
                case 7 -> TurretChekhovBlockEntity.this.stattrak;
                case 8 -> (int) Math.round(Math.toDegrees(TurretChekhovBlockEntity.this.rotationYaw) * 100.0D);
                case 9 -> (int) Math.round(Math.toDegrees(TurretChekhovBlockEntity.this.rotationPitch) * 100.0D);
                case 10 -> (int) Math.round(Math.toDegrees(TurretChekhovBlockEntity.this.prevRotationYaw) * 100.0D);
                case 11 -> (int) Math.round(Math.toDegrees(TurretChekhovBlockEntity.this.prevRotationPitch) * 100.0D);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TurretChekhovBlockEntity.this.power = value;
                case 1 -> TurretChekhovBlockEntity.this.lastInput = value;
                case 2 -> TurretChekhovBlockEntity.this.on = value != 0;
                case 3 -> TurretChekhovBlockEntity.this.targetPlayers = value != 0;
                case 4 -> TurretChekhovBlockEntity.this.targetAnimals = value != 0;
                case 5 -> TurretChekhovBlockEntity.this.targetMobs = value != 0;
                case 6 -> TurretChekhovBlockEntity.this.targetMachines = value != 0;
                case 7 -> TurretChekhovBlockEntity.this.stattrak = value;
                case 8 -> TurretChekhovBlockEntity.this.rotationYaw = Math.toRadians(value / 100.0D);
                case 9 -> TurretChekhovBlockEntity.this.rotationPitch = Math.toRadians(value / 100.0D);
                case 10 -> TurretChekhovBlockEntity.this.prevRotationYaw = Math.toRadians(value / 100.0D);
                case 11 -> TurretChekhovBlockEntity.this.prevRotationPitch = Math.toRadians(value / 100.0D);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TurretChekhovBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.TURRET_CHEKHOV.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TurretChekhovBlockEntity turret) {
        if (level.isClientSide) {
            turret.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, turret);
        turret.tickServer(level);
    }

    private void tickClient() {
        if (Math.abs(this.prevRotationYaw - this.rotationYaw) > Math.PI) {
            if (this.prevRotationYaw < this.rotationYaw) {
                this.prevRotationYaw += Math.PI * 2.0D;
            } else {
                this.prevRotationYaw -= Math.PI * 2.0D;
            }
        }
        this.spinAccel = this.trackingTarget ? Math.min(45.0F, this.spinAccel + 2.0F) : Math.max(0.0F, this.spinAccel - 2.0F);
        this.prevSpin = this.spin;
        this.spin += this.spinAccel;
        if (this.spin >= 360.0F) {
            this.spin -= 360.0F;
            this.prevSpin -= 360.0F;
        }
    }

    private void tickServer(Level level) {
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
        this.aligned = false;
        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
            this.stattrak++;
        }
        if (this.target != null && !entityInLOS(this.target)) {
            this.target = null;
        }
        if (isOn() && hasPower()) {
            if (this.target != null) {
                turnTowards(entityPos(this.target));
            }
            this.searchTimer--;
            this.power = Math.max(0L, this.power - CONSUMPTION);
            if (this.searchTimer <= 0) {
                this.searchTimer = 10;
                if (this.target == null) {
                    seekTarget(level);
                }
            }
        } else {
            this.target = null;
            this.searchTimer = 0;
        }
        if (this.aligned) {
            updateFiring(level);
        }
        if (this.target == null) {
            this.timer = Mth.clamp(this.timer - 1, 0, 20);
        }
        this.trackingTarget = this.target != null;
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        setChanged();
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void updateFiring(Level level) {
        this.timer++;
        if (this.timer <= 20 || this.timer % FIRE_DELAY != 0) {
            return;
        }
        StandardAmmoItem.Bmg50Type ammo = firstAmmoLoaded();
        if (ammo == StandardAmmoItem.Bmg50Type.NONE) {
            return;
        }
        Vec3 pos = turretPos();
        Vec3 dir = barrelVector(1.0D).normalize();
        Vec3 muzzle = pos.add(barrelVector(BARREL_LENGTH));
        level.addFreshEntity(new ChekhovBulletEntity(level, muzzle.x, muzzle.y, muzzle.z, dir, ammo));
        consumeAmmo(ammo);
        level.playSound(null, this.worldPosition, HbmSoundEvents.TURRET_CHEKHOV_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        TurretCasingEffects.spawnMuzzleFlash(level, muzzle, 1.5F, 1, 50.0D);
        spawnCasing(level, ammo);
    }

    private void spawnCasing(Level level, StandardAmmoItem.Bmg50Type ammo) {
        Vec3 spawn = turretPos().add(TurretCasingEffects.localVector(-1.125D, 0.125D, 0.25D, this.rotationYaw, this.rotationPitch));
        TurretCasingEffects.CasingKind casing = switch (ammo) {
            case AP, DU -> TurretCasingEffects.CasingKind.BMG50_STEEL;
            default -> TurretCasingEffects.CasingKind.BMG50;
        };
        TurretCasingEffects.spawnEjectorCasing(level, spawn, this.rotationYaw, this.rotationPitch,
                casing, -0.8D, 0.8D, 0.0D, 0.1D, 0.1D, 1);
    }

    private void seekTarget(Level level) {
        Vec3 pos = turretPos();
        AABB area = new AABB(pos, pos).inflate(DETECTOR_RANGE);
        Entity best = null;
        double closest = DETECTOR_RANGE;
        for (Entity entity : level.getEntities((Entity) null, area, this::acceptableTarget)) {
            Vec3 entityPos = entityPos(entity);
            double distance = entityPos.distanceTo(pos);
            if (distance >= closest || distance < DETECTOR_GRACE || distance > DETECTOR_RANGE) {
                continue;
            }
            if (!entityInLOS(entity)) {
                continue;
            }
            closest = distance;
            best = entity;
        }
        this.target = best;
    }

    private boolean entityInLOS(Entity entity) {
        Vec3 pos = turretPos();
        Vec3 entityPos = entityPos(entity);
        Vec3 delta = entityPos.subtract(pos);
        double distance = delta.length();
        if (distance < DETECTOR_GRACE || distance > DETECTOR_RANGE * 1.1D) {
            return false;
        }
        double pitchDegrees = Math.toDegrees(Math.asin(delta.y / distance));
        if (pitchDegrees < -DEPRESSION || pitchDegrees > ELEVATION) {
            return false;
        }
        return this.level == null || this.level.clip(new net.minecraft.world.level.ClipContext(
                entityPos,
                pos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                entity
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private boolean acceptableTarget(Entity entity) {
        if (!entity.isAlive()) {
            return false;
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
        if (entity instanceof Mob mob && mob.hasCustomName()) {
            if (whitelist.contains(mob.getName().getString())) {
                return false;
            }
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
        Vec3 pos = turretPos();
        Vec3 delta = targetPos.subtract(pos);
        double targetPitch = Math.asin(delta.y / delta.length());
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double turnYaw = Math.toRadians(4.5D);
        double turnPitch = Math.toRadians(3.0D);
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
        this.aligned = Math.sqrt(dYaw * dYaw + dPitch * dPitch) <= Math.toRadians(ACCEPTABLE_INACCURACY);
    }

    private StandardAmmoItem.Bmg50Type firstAmmoLoaded() {
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            StandardAmmoItem.Bmg50Type type = StandardAmmoItem.bmg50Type(this.items.get(slot));
            if (type != StandardAmmoItem.Bmg50Type.NONE) {
                return type;
            }
        }
        return StandardAmmoItem.Bmg50Type.NONE;
    }

    private void consumeAmmo(StandardAmmoItem.Bmg50Type ammo) {
        for (int slot = AMMO_START; slot < AMMO_END; slot++) {
            if (StandardAmmoItem.bmg50Type(this.items.get(slot)) == ammo) {
                this.items.get(slot).shrink(1);
                if (this.items.get(slot).isEmpty()) {
                    this.items.set(slot, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    public void toggle(int id) {
        switch (id) {
            case 0 -> this.on = !this.on;
            case 1 -> this.targetPlayers = !this.targetPlayers;
            case 2 -> this.targetAnimals = !this.targetAnimals;
            case 3 -> this.targetMobs = !this.targetMobs;
            case 4 -> this.targetMachines = !this.targetMachines;
            default -> {
            }
        }
        setChanged();
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
            return List.of();
        }
        return TurretBiometryItem.names(stack);
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
        return this.on;
    }

    public boolean hasPower() {
        return this.power >= CONSUMPTION;
    }

    public double renderYaw(float partialTick) {
        return Mth.lerp(partialTick, this.prevRotationYaw, this.rotationYaw);
    }

    public double renderPitch(float partialTick) {
        return Mth.lerp(partialTick, this.prevRotationPitch, this.rotationPitch);
    }

    public float renderSpin(float partialTick) {
        return Mth.lerp(partialTick, this.prevSpin, this.spin);
    }

    public Vec3 turretPos() {
        Vec3 offset = horizontalOffset();
        return Vec3.atLowerCornerOf(this.worldPosition).add(offset.x, 1.5D, offset.z);
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

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction facing = this.getBlockState().hasProperty(TurretChekhovBlock.FACING)
                ? this.getBlockState().getValue(TurretChekhovBlock.FACING)
                : Direction.NORTH;
        return connectorSpecs(this.worldPosition, facing).stream().map(ConnectorSpec::pos).toList();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        Direction facing = this.getBlockState().hasProperty(TurretChekhovBlock.FACING)
                ? this.getBlockState().getValue(TurretChekhovBlock.FACING)
                : Direction.NORTH;
        for (ConnectorSpec spec : connectorSpecs(this.worldPosition, facing)) {
            if (spec.pos().equals(connectorPos) && spec.machineSide() == machineSide) {
                return true;
            }
        }
        return false;
    }

    public Vec3 horizontalOffset() {
        Direction facing = this.getBlockState().hasProperty(TurretChekhovBlock.FACING)
                ? this.getBlockState().getValue(TurretChekhovBlock.FACING)
                : Direction.NORTH;
        return horizontalOffset(facing);
    }

    public static Vec3 horizontalOffset(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(1.0D, 0.0D, 1.0D);
            case WEST -> new Vec3(1.0D, 0.0D, 0.0D);
            case EAST -> new Vec3(0.0D, 0.0D, 1.0D);
            default -> new Vec3(0.0D, 0.0D, 0.0D);
        };
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : MAX_POWER - this.power;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        this.lastInput = receivedInput;
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.turret_chekhov", CONSUMPTION, this.power, MAX_POWER);
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
        Containers.dropContents(level, pos, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AMMO_SLOTS;
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
        return Component.translatable("container.reinhardtshbm.turret_chekhov");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new TurretChekhovMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveItems(tag, registries);
        tag.putLong("power", this.power);
        tag.putLong("last_input", this.lastInput);
        tag.putBoolean("on", this.on);
        tag.putBoolean("target_players", this.targetPlayers);
        tag.putBoolean("target_animals", this.targetAnimals);
        tag.putBoolean("target_mobs", this.targetMobs);
        tag.putBoolean("target_machines", this.targetMachines);
        tag.putInt("search_timer", this.searchTimer);
        tag.putInt("timer", this.timer);
        tag.putInt("stattrak", this.stattrak);
        tag.putDouble("rotation_yaw", this.rotationYaw);
        tag.putDouble("rotation_pitch", this.rotationPitch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadItems(tag, registries);
        this.power = tag.getLong("power");
        this.lastInput = tag.getLong("last_input");
        this.on = tag.getBoolean("on");
        this.targetPlayers = tag.getBoolean("target_players");
        this.targetAnimals = tag.getBoolean("target_animals");
        this.targetMobs = !tag.contains("target_mobs") || tag.getBoolean("target_mobs");
        this.targetMachines = !tag.contains("target_machines") || tag.getBoolean("target_machines");
        this.searchTimer = tag.getInt("search_timer");
        this.timer = tag.getInt("timer");
        this.stattrak = tag.getInt("stattrak");
        double oldYaw = this.rotationYaw;
        double oldPitch = this.rotationPitch;
        this.rotationYaw = tag.getDouble("rotation_yaw");
        this.rotationPitch = tag.getDouble("rotation_pitch");
        if (tag.contains("tracking_target")) {
            this.trackingTarget = tag.getBoolean("tracking_target");
        }
        if (this.level != null && this.level.isClientSide) {
            this.prevRotationYaw = oldYaw;
            this.prevRotationPitch = oldPitch;
        } else {
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
        }
    }

    public void saveItems(CompoundTag tag, HolderLookup.Provider registries) {
        net.minecraft.world.ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    public void loadItems(CompoundTag tag, HolderLookup.Provider registries) {
        this.items.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putBoolean("tracking_target", this.trackingTarget);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static List<BlockPos> occupiedPositions(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> positions = new ArrayList<>(4);
        switch (facing) {
            case NORTH -> {
                positions.add(corePos);
                positions.add(corePos.offset(1, 0, 0));
                positions.add(corePos.offset(0, 0, 1));
                positions.add(corePos.offset(1, 0, 1));
            }
            case SOUTH -> {
                positions.add(corePos);
                positions.add(corePos.offset(-1, 0, 0));
                positions.add(corePos.offset(0, 0, -1));
                positions.add(corePos.offset(-1, 0, -1));
            }
            case EAST -> {
                positions.add(corePos);
                positions.add(corePos.offset(-1, 0, 0));
                positions.add(corePos.offset(0, 0, 1));
                positions.add(corePos.offset(-1, 0, 1));
            }
            default -> {
                positions.add(corePos);
                positions.add(corePos.offset(1, 0, 0));
                positions.add(corePos.offset(0, 0, -1));
                positions.add(corePos.offset(1, 0, -1));
            }
        }
        return positions;
    }

    private static boolean isChip(ItemStack stack) {
        return stack.is(HbmItems.TURRET_CHIP.get());
    }

    public static List<ConnectorSpec> connectorSpecs(BlockPos corePos, Direction facing) {
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

    public record ConnectorSpec(BlockPos pos, Direction machineSide, float renderOffsetX, float renderOffsetZ) {
    }
}
