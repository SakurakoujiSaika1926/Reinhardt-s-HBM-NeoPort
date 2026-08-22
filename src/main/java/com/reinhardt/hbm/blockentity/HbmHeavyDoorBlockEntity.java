package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.block.HbmHeavyDoorPartBlock;
import com.reinhardt.hbm.door.HbmDoorDecl;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

    public HbmHeavyDoorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.HEAVY_DOOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HbmHeavyDoorBlockEntity door) {
        if (!(state.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return;
        }
        HbmDoorDecl decl = doorBlock.decl();
        if (level.isClientSide) {
            return;
        }

        boolean changed = false;
        if (door.state == STATE_OPENING) {
            door.openTicks = Math.min(decl.timeToOpen(), door.openTicks + 1);
            door.applyOpenRangeBlocks(level, decl, true);
            if (door.openTicks >= decl.timeToOpen()) {
                door.state = STATE_OPEN;
            }
            changed = true;
        } else if (door.state == STATE_CLOSING) {
            door.openTicks = Math.max(0, door.openTicks - 1);
            door.applyOpenRangeBlocks(level, decl, false);
            if (door.openTicks <= 0) {
                door.state = STATE_CLOSED;
            }
            changed = true;
        }

        boolean powered = HbmHeavyDoorBlock.hasNeighborSignalAnywhere(level, pos, doorBlock, state.getValue(HbmHeavyDoorBlock.FACING));
        if (powered && door.state == STATE_CLOSED) {
            door.state = STATE_OPENING;
            changed = true;
        } else if (!powered && door.state == STATE_OPEN) {
            door.state = STATE_CLOSING;
            changed = true;
        }

        if (changed) {
            door.sync();
        }
    }

    public boolean toggle() {
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
        boolean powered = HbmHeavyDoorBlock.hasNeighborSignalAnywhere(
                this.level,
                this.worldPosition,
                doorBlock,
                this.getBlockState().getValue(HbmHeavyDoorBlock.FACING)
        );
        if (powered && this.state == STATE_CLOSED) {
            this.state = STATE_OPENING;
            sync();
        } else if (!powered && this.state == STATE_OPEN) {
            this.state = STATE_CLOSING;
            sync();
        }
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
            if (length == 0) {
                continue;
            }
            if (opening) {
                for (int j = 0; j < length; j++) {
                    if ((float) j / Math.max(1, length - 1) > time) {
                        break;
                    }
                    setRangeColumn(level, facing, range, j, true);
                }
            } else {
                for (int j = length - 1; j >= 0; j--) {
                    if ((float) j / Math.max(1, length - 1) < time) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.state = tag.getByte("State");
        this.openTicks = tag.getInt("OpenTicks");
        this.skinIndex = tag.getByte("Skin");
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
