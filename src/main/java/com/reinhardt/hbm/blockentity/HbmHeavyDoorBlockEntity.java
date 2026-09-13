package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.client.sound.HbmDoorClientSounds;
import com.reinhardt.hbm.door.HbmDoorDecl;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HbmHeavyDoorBlockEntity extends BlockEntity {
    public static final byte STATE_CLOSED = 0;
    public static final byte STATE_OPEN = 1;
    public static final byte STATE_CLOSING = 2;
    public static final byte STATE_OPENING = 3;

    private byte state = STATE_CLOSED;
    private int openTicks;
    private byte skinIndex;
    /** Equivalent to legacy redstonePower > 0: at least one door part is powered. */
    private boolean redstonePowered;
    /** Equivalent to the legacy one-tick redstonePower == -1 falling-edge state. */
    private boolean redstoneReleasePending;

    public HbmHeavyDoorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.HEAVY_DOOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HbmHeavyDoorBlockEntity door) {
        if (!(state.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return;
        }
        HbmDoorDecl decl = doorBlock.decl();
        if (level.isClientSide) {
            HbmDoorClientSounds.tick(door);
            return;
        }

        boolean changed = false;
        if (door.state == STATE_OPENING) {
            door.playVaultAnimationSound(decl, door.openTicks);
            door.openTicks = Math.min(decl.timeToOpen(), door.openTicks + 1);
            door.applyOpenRangeBlocks(level, decl, true);
            if (door.openTicks >= decl.timeToOpen()) {
                door.state = STATE_OPEN;
            }
            changed = true;
        } else if (door.state == STATE_CLOSING) {
            door.playVaultAnimationSound(decl, door.openTicks);
            door.openTicks = Math.max(0, door.openTicks - 1);
            door.applyOpenRangeBlocks(level, decl, false);
            if (door.openTicks <= 0) {
                door.state = STATE_CLOSED;
            }
            changed = true;
        }

        // TileEntityDoorGeneric maintained a persistent positive power latch
        // plus a one-tick -1 state when its final signal vanished.  Do the
        // same in explicit fields: this avoids treating ordinary unpowered
        // manual doors as a continuous close command.
        if (door.updateRedstoneState(level, pos, doorBlock,
                state.getValue(HbmHeavyDoorBlock.FACING))) {
            changed = true;
        }
        if (door.applyRedstoneLatch()) {
            changed = true;
        }

        if (changed) {
            door.sync();
        }
    }

    public boolean toggle() {
        // Legacy tryToggle treats an energized closed door as a redstone lock.
        if (this.state == STATE_CLOSED && this.redstonePowered) {
            return false;
        }
        if (this.state == STATE_CLOSED) {
            this.state = STATE_OPENING;
            sync();
            return true;
        }
        if (this.state == STATE_OPEN) {
            this.state = STATE_CLOSING;
            sync();
            return true;
        }
        return false;
    }

    public void checkRedstoneNow() {
        if (this.level == null || this.level.isClientSide || !(this.getBlockState().getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return;
        }
        if (this.updateRedstoneState(this.level, this.worldPosition, doorBlock,
                this.getBlockState().getValue(HbmHeavyDoorBlock.FACING))) {
            sync();
        }
    }

    private boolean updateRedstoneState(Level level, BlockPos pos, HbmHeavyDoorBlock doorBlock, Direction facing) {
        boolean powered = HbmHeavyDoorBlock.hasNeighborSignalAnywhere(level, pos, doorBlock, facing);
        if (powered == this.redstonePowered) {
            return false;
        }
        this.redstonePowered = powered;
        if (powered) {
            // A new signal after a falling edge cancels old redstonePower == -1.
            this.redstoneReleasePending = false;
        } else {
            this.redstoneReleasePending = true;
        }
        return true;
    }

    private boolean applyRedstoneLatch() {
        boolean changed = false;
        if (this.redstoneReleasePending) {
            if (this.state == STATE_OPEN) {
                this.state = STATE_CLOSING;
                changed = true;
            }
            // TileEntityDoorGeneric reset redstonePower from -1 to 0 after
            // evaluating it once, including while a door was still moving.
            this.redstoneReleasePending = false;
            return true;
        }
        if (this.redstonePowered && this.state == STATE_CLOSED) {
            this.state = STATE_OPENING;
            changed = true;
        }
        return changed;
    }

    public byte state() {
        return this.state;
    }

    public int openTicks() {
        return this.openTicks;
    }

    public byte skinIndex() {
        return this.skinIndex;
    }

    public boolean isOpenForCollision() {
        return this.state != STATE_CLOSED;
    }

    public void cycleSkin(HbmDoorDecl decl) {
        if (decl.skinCount() <= 0) {
            return;
        }
        this.skinIndex = (byte) ((this.skinIndex + 1) % decl.skinCount());
        sync();
    }

    private void applyOpenRangeBlocks(Level level, HbmDoorDecl decl, boolean opening) {
        Direction facing = this.getBlockState().getValue(HbmHeavyDoorBlock.FACING);
        for (int i = 0; i < decl.openRanges().length; i++) {
            int[] range = decl.openRanges()[i];
            float time = decl.rangeOpenProgress(this.openTicks, i);
            int length = Math.abs(range[3]);
            int legacyDenominator = Math.abs(range[3] - 1);
            if (length == 0) {
                continue;
            }
            if (opening) {
                for (int j = 0; j < length; j++) {
                    if ((float) j / legacyDenominator > time) {
                        break;
                    }
                    setRangeColumn(level, facing, range, j, true);
                }
            } else {
                for (int j = length - 1; j >= 0; j--) {
                    if ((float) j / legacyDenominator < time) {
                        break;
                    }
                    setRangeColumn(level, facing, range, j, false);
                }
            }
        }
    }

    private void setRangeColumn(Level level, Direction facing, int[] range, int j, boolean extra) {
        int sign = Integer.signum(range[3]);
        for (int k = 0; k < range[4]; k++) {
            BlockPos local = switch (range[5]) {
                case 0 -> new BlockPos(0, k, sign * j);
                case 1 -> new BlockPos(k, sign * j, 0);
                case 2 -> new BlockPos(sign * j, k, 0);
                default -> BlockPos.ZERO;
            };
            BlockPos rangeStart = new BlockPos(range[0], range[1], range[2]);
            BlockPos finalPos = this.worldPosition.offset(HbmHeavyDoorBlock.rotateDoorOpening(rangeStart.offset(local), facing));
            if (finalPos.equals(this.worldPosition)) {
                continue;
            }
            if (level.getBlockState(finalPos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                    && level.getBlockEntity(finalPos) instanceof HbmHeavyDoorPartBlockEntity part
                    && part.corePos().equals(this.worldPosition)) {
                level.setBlock(finalPos, level.getBlockState(finalPos).setValue(HbmHeavyDoorPartBlock.EXTRA, extra), Block.UPDATE_ALL);
            }
        }
    }

    private void playVaultAnimationSound(HbmDoorDecl decl, int ticks) {
        if (decl != HbmDoorDecl.VAULT_DOOR || this.level == null || this.level.isClientSide) {
            return;
        }
        if ((this.state == STATE_OPENING && ticks == 0) || (this.state == STATE_CLOSING && ticks == 30)) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.VAULT_SCRAPE_NEW.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (ticks >= 45 && ticks <= 115 && (ticks - 45) % 10 == 0) {
            this.level.playSound(null, this.worldPosition, HbmSoundEvents.VAULT_THUD_NEW.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            if (!this.level.isClientSide && this.getBlockState().getBlock() instanceof HbmHeavyDoorBlock doorBlock) {
                Direction facing = this.getBlockState().getValue(HbmHeavyDoorBlock.FACING);
                for (BlockPos partPos : HbmHeavyDoorBlock.worldOffsets(doorBlock.decl(), facing, this.worldPosition).keySet()) {
                    BlockState state = this.level.getBlockState(partPos);
                    this.level.sendBlockUpdated(partPos, state, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("State", this.state);
        tag.putInt("OpenTicks", this.openTicks);
        tag.putByte("Skin", this.skinIndex);
        tag.putBoolean("RedstonePowered", this.redstonePowered);
        tag.putBoolean("RedstoneReleasePending", this.redstoneReleasePending);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.state = tag.getByte("State");
        this.openTicks = tag.getInt("OpenTicks");
        this.skinIndex = tag.getByte("Skin");
        this.redstonePowered = tag.getBoolean("RedstonePowered");
        this.redstoneReleasePending = tag.getBoolean("RedstoneReleasePending");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
