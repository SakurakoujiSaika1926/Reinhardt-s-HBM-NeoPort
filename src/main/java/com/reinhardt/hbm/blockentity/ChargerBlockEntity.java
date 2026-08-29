package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Direct port of TileEntityCharger: power is consumed by nearby players' equipment batteries. */
public final class ChargerBlockEntity extends BlockEntity implements PowerEndpoint {
    private static final int DELAY = 20;
    private static final List<EquipmentSlot> CHARGE_SLOTS = List.of(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    );

    private final List<Player> players = new ArrayList<>();
    private long charge;
    private int lastOp;
    private boolean particles;
    private int usingTicks;
    private int lastUsingTicks;
    private long syncedCharge = Long.MIN_VALUE;
    private boolean syncedParticles;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CHARGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity charger) {
        if (level.isClientSide) {
            charger.tickAnimation(level, pos);
            return;
        }

        charger.players.clear();
        charger.players.addAll(level.getEntitiesOfClass(Player.class,
                new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0D, pos.getY() + 0.5D, pos.getZ() + 1.0D)));
        charger.charge = charger.calculateDemand();
        charger.particles = charger.lastOp > 0;
        if (charger.particles) {
            charger.lastOp--;
            if (level.getGameTime() % 20L == 0L) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        charger.updateAnimation(level, pos);
        PowerNetworkManager.tickFromEndpoint(level, charger);
        if (charger.charge != charger.syncedCharge || charger.particles != charger.syncedParticles
                || level.getGameTime() % 5L == 0L) {
            charger.syncedCharge = charger.charge;
            charger.syncedParticles = charger.particles;
            charger.sync();
        }
    }

    private void tickAnimation(Level level, BlockPos pos) {
        updateAnimation(level, pos);
        if (particles) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT,
                    pos.getX() + 0.5D + level.random.nextDouble() * 0.0625D + facing().getStepX() * 0.75D,
                    pos.getY() + 0.1D,
                    pos.getZ() + 0.5D + level.random.nextDouble() * 0.0625D + facing().getStepZ() * 0.75D,
                    -facing().getStepX() + level.random.nextGaussian() * 0.1D,
                    0.0D,
                    -facing().getStepZ() + level.random.nextGaussian() * 0.1D);
        }
    }

    private void updateAnimation(Level level, BlockPos pos) {
        lastUsingTicks = usingTicks;
        if ((charge > 0L || particles) && usingTicks < DELAY) {
            usingTicks++;
            if (!level.isClientSide && usingTicks == 2) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if (charge <= 0L && !particles && usingTicks > 0) {
            usingTicks--;
            if (!level.isClientSide && usingTicks == 4) {
                level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
    }

    private long calculateDemand() {
        long demand = 0L;
        for (Player player : players) {
            for (EquipmentSlot slot : CHARGE_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (BatteryPackItem.isBattery(stack)) {
                    demand = saturatedAdd(demand, Math.min(
                            Math.max(0L, BatteryPackItem.capacity(stack) - BatteryPackItem.charge(stack)),
                            BatteryPackItem.chargeRate(stack)));
                }
            }
        }
        return demand;
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(worldPosition.relative(facing()));
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(worldPosition.relative(facing()));
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return charge;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long remaining = receivedInput;
        for (Player player : players) {
            for (EquipmentSlot slot : CHARGE_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!BatteryPackItem.isBattery(stack) || remaining <= 0L) {
                    continue;
                }
                long target = Math.min(
                        Math.max(0L, BatteryPackItem.capacity(stack) - BatteryPackItem.charge(stack)),
                        BatteryPackItem.chargeRate(stack));
                long toCharge = Math.min(target, Math.max(remaining / 5L, 1L));
                if (toCharge <= 0L) {
                    continue;
                }
                BatteryPackItem.setStoredCharge(stack, BatteryPackItem.charge(stack) + toCharge);
                remaining -= toCharge;
                lastOp = 4;
            }
        }
        if (receivedInput != remaining) {
            setChanged();
        }
    }

    @Override
    public PowerEndpoint.ConnectionPriority getPowerPriority() {
        return PowerEndpoint.ConnectionPriority.NORMAL;
    }

    @Override
    public net.minecraft.network.chat.Component getPowerStatus() {
        return net.minecraft.network.chat.Component.translatable("message.reinhardtshbm.charger", charge);
    }

    public int usingTicks() {
        return usingTicks;
    }

    public int lastUsingTicks() {
        return lastUsingTicks;
    }

    public int delay() {
        return DELAY;
    }

    public boolean particles() {
        return particles;
    }

    private Direction facing() {
        return getBlockState().getValue(com.reinhardt.hbm.block.ChargerBlock.FACING);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Charge", charge);
        tag.putBoolean("Particles", particles);
        tag.putInt("UsingTicks", usingTicks);
        tag.putInt("LastUsingTicks", lastUsingTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = Math.max(0L, tag.getLong("Charge"));
        particles = tag.getBoolean("Particles");
        usingTicks = Math.max(0, Math.min(DELAY, tag.getInt("UsingTicks")));
        lastUsingTicks = Math.max(0, Math.min(DELAY, tag.getInt("LastUsingTicks")));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private static long saturatedAdd(long left, long right) {
        return left >= Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
