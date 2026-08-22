package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.BlastDoorDummyBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlastDoorBlockEntity extends BlockEntity implements LockableBlockEntity {
    public static final int STATE_CLOSED = 0;
    public static final int STATE_MOVING = 1;
    public static final int STATE_OPEN = 2;
    private static final int MOVE_TICKS = 100;
    private static final int VISUAL_DURATION_MS = 5000;
    private static final double VISUAL_EXTEND = 5.0D;

    private boolean isOpening;
    private int state = STATE_CLOSED;
    private long sysTime;
    private int timer;
    private boolean redstoned;
    private int lock;
    private boolean locked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    public BlastDoorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BLAST_DOOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlastDoorBlockEntity door) {
        if (level.isClientSide) {
            return;
        }

        door.checkRedstoneNow();

        boolean changed = false;
        if (door.state != STATE_MOVING) {
            if (door.timer != 0) {
                door.timer = 0;
                changed = true;
            }
        } else {
            door.timer++;
            if (door.isOpening) {
                if (door.timer >= 0) {
                    door.removeDummy(pos.above(1));
                }
                if (door.timer >= 20) {
                    door.removeDummy(pos.above(2));
                }
                if (door.timer >= 40) {
                    door.removeDummy(pos.above(3));
                }
                if (door.timer >= 60) {
                    door.removeDummy(pos.above(4));
                }
                if (door.timer >= 80) {
                    door.removeDummy(pos.above(5));
                }
            } else {
                if (door.timer >= 20) {
                    door.placeDummy(pos.above(5));
                }
                if (door.timer >= 40) {
                    door.placeDummy(pos.above(4));
                }
                if (door.timer >= 60) {
                    door.placeDummy(pos.above(3));
                }
                if (door.timer >= 80) {
                    door.placeDummy(pos.above(2));
                }
                if (door.timer >= 100) {
                    door.placeDummy(pos.above(1));
                }
            }
            if (door.timer >= MOVE_TICKS) {
                if (door.isOpening) {
                    door.finishOpen();
                } else {
                    door.finishClose();
                }
            }
            changed = true;
        }

        if (changed) {
            door.sync();
        }
    }

    public boolean placeInitialDummies() {
        boolean placed = true;
        for (int y = 1; y <= 6; y++) {
            placed &= placeDummy(this.worldPosition.above(y));
        }
        return placed;
    }

    public void checkRedstoneNow() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        boolean powered = (!isLocked() && this.level.hasNeighborSignal(this.worldPosition))
                || this.level.hasNeighborSignal(this.worldPosition.above(6));
        if (powered) {
            if (!this.redstoned) {
                tryToggle(null);
            }
            this.redstoned = true;
        } else {
            this.redstoned = false;
        }
    }

    public void tryToggle(@Nullable Player player) {
        if (!canAccess(player)) {
            return;
        }
        if (canOpen()) {
            open();
            openNeigh();
        } else if (canClose()) {
            close();
            closeNeigh();
        }
    }

    public void open() {
        if (this.state == STATE_CLOSED) {
            this.isOpening = true;
            this.state = STATE_MOVING;
            this.timer = 0;
            this.sysTime = System.currentTimeMillis();
            playStartSound();
            sync();
        }
    }

    private void finishOpen() {
        this.state = STATE_OPEN;
        this.timer = 0;
        playStopSound();
        sync();
    }

    public void close() {
        if (this.state == STATE_OPEN) {
            this.isOpening = false;
            this.state = STATE_MOVING;
            this.timer = 0;
            this.sysTime = System.currentTimeMillis();
            playStartSound();
            sync();
        }
    }

    private void finishClose() {
        this.state = STATE_CLOSED;
        this.timer = 0;
        playStopSound();
        sync();
    }

    private void openNeigh() {
        if (this.level == null) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof BlastDoorBlockEntity other
                    && other.canOpen()
                    && other.canLinkWith(this)) {
                other.open();
                other.openNeigh();
            }
        }
    }

    private void closeNeigh() {
        if (this.level == null) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof BlastDoorBlockEntity other
                    && other.canClose()
                    && other.canLinkWith(this)) {
                other.close();
                other.closeNeigh();
            }
        }
    }

    private boolean canLinkWith(BlastDoorBlockEntity source) {
        return !this.isLocked() || this.lock == source.lock;
    }

    public boolean canOpen() {
        return this.state == STATE_CLOSED;
    }

    public boolean canClose() {
        return this.state == STATE_OPEN;
    }

    public boolean isOpening() {
        return this.isOpening;
    }

    public int doorState() {
        return this.state;
    }

    public double visualTimer() {
        if (this.state == STATE_CLOSED) {
            return getAnimationFromSysTime(VISUAL_DURATION_MS);
        }
        if (this.state == STATE_OPEN) {
            return 0.0D;
        }
        long now = System.currentTimeMillis();
        if (this.isOpening) {
            return getAnimationFromSysTime(this.sysTime + VISUAL_DURATION_MS - now);
        }
        return getAnimationFromSysTime(now - this.sysTime);
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public void lock() {
        this.locked = true;
        lockChanged();
    }

    @Override
    public void unlock() {
        this.locked = false;
        lockChanged();
    }

    @Override
    public int pins() {
        return this.lock;
    }

    @Override
    public void setPins(int pins) {
        this.lock = pins;
        lockChanged();
    }

    @Override
    public double lockMod() {
        return this.lockMod;
    }

    @Override
    public void setLockMod(double mod) {
        this.lockMod = mod;
        lockChanged();
    }

    @Override
    public boolean cheesable() {
        return this.cheesable;
    }

    @Override
    public void setCheesable(boolean cheesable) {
        this.cheesable = cheesable;
        lockChanged();
    }

    @Override
    public void lockChanged() {
        sync();
    }

    public static boolean isLockTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("padlock") || path.equals("key_kit");
    }

    private boolean placeDummy(BlockPos pos) {
        if (this.level == null) {
            return false;
        }
        BlockState present = this.level.getBlockState(pos);
        if (!present.isAir() && !present.is(HbmBlocks.DUMMY_BLOCK_BLAST.get())) {
            this.level.destroyBlock(pos, false);
        }
        if (!this.level.setBlock(pos, HbmBlocks.DUMMY_BLOCK_BLAST.get().defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        if (this.level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy) {
            dummy.setCorePos(this.worldPosition);
        }
        return true;
    }

    private void removeDummy(BlockPos pos) {
        if (this.level == null) {
            return;
        }
        if (this.level.getBlockState(pos).is(HbmBlocks.DUMMY_BLOCK_BLAST.get())) {
            BlastDoorDummyBlock.runWithoutCoreDestroy(() -> this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        }
    }

    public static void removeDummies(Level level, BlockPos corePos) {
        BlastDoorDummyBlock.runWithoutCoreDestroy(() -> {
            for (int y = 1; y <= 6; y++) {
                BlockPos pos = corePos.above(y);
                if (level.getBlockState(pos).is(HbmBlocks.DUMMY_BLOCK_BLAST.get())
                        && level.getBlockEntity(pos) instanceof BlastDoorDummyBlockEntity dummy
                        && dummy.corePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private void playStartSound() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.REACTOR_START.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
        }
    }

    private void playStopSound() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.REACTOR_STOP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    private static double getAnimationFromSysTime(long time) {
        return Math.max(Math.min(time, VISUAL_DURATION_MS) / (double) VISUAL_DURATION_MS * VISUAL_EXTEND, 0.0D);
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isOpening", this.isOpening);
        tag.putInt("state", this.state);
        tag.putLong("sysTime", this.sysTime);
        tag.putInt("timer", this.timer);
        tag.putBoolean("redstoned", this.redstoned);
        tag.putInt("lock", this.lock);
        tag.putBoolean("isLocked", this.locked);
        tag.putDouble("lockMod", this.lockMod);
        tag.putBoolean("cheesable", this.cheesable);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isOpening = tag.getBoolean("isOpening");
        this.state = tag.getInt("state");
        this.sysTime = tag.getLong("sysTime");
        this.timer = tag.getInt("timer");
        this.redstoned = tag.getBoolean("redstoned");
        this.lock = tag.getInt("lock");
        this.locked = tag.getBoolean("isLocked");
        this.lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
        this.cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
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
}
