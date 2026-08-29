package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.block.PistonInserterBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PistonInserterBlockEntity extends BlockEntity implements WorldlyContainer, MachineInventory {
    public static final int MAX_EXTEND = 25;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int extend;
    private int delay;
    private boolean retracting = true;
    private boolean lastPowered;
    private int clientTarget;
    private float renderExtend;
    private float lastRenderExtend;
    private boolean clientInitialized;

    public PistonInserterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PISTON_INSERTER.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T entity) {
        if (!(entity instanceof PistonInserterBlockEntity piston)) {
            return;
        }
        if (level.isClientSide) {
            piston.tickClient();
        } else {
            piston.tickServer(level, state);
        }
    }

    public float extension(float partialTick) {
        return lastRenderExtend + (renderExtend - lastRenderExtend) * partialTick;
    }

    public int extend() {
        return extend;
    }

    public Direction facing() {
        return getBlockState().getValue(PistonInserterBlock.FACING);
    }

    public void insertFromPlayer(Player player, InteractionHand hand) {
        if (!items.get(0).isEmpty()) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return;
        }
        items.set(0, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        sync();
    }

    public void ejectHeldItem() {
        if (!retracting || items.get(0).isEmpty() || level == null) {
            return;
        }
        Direction direction = facing();
        ItemStack stack = removeItemNoUpdate(0);
        ItemEntity item = new ItemEntity(level,
                worldPosition.getX() + 0.5D + direction.getStepX() * 0.75D,
                worldPosition.getY() + 0.5D + direction.getStepY() * 0.75D,
                worldPosition.getZ() + 0.5D + direction.getStepZ() * 0.75D,
                stack);
        item.setDeltaMovement(direction.getStepX() * 0.25D, direction.getStepY() * 0.25D, direction.getStepZ() * 0.25D);
        level.addFreshEntity(item);
        sync();
    }

    private void tickServer(Level level, BlockState state) {
        boolean powered = level.hasNeighborSignal(worldPosition);
        BlockPos front = worldPosition.relative(facing());
        if (!level.getBlockState(front).isCollisionShapeFullBlock(level, front)) {
            if (powered && !lastPowered && extend <= 0) {
                retracting = false;
            }
            lastPowered = powered;
        }

        if (delay <= 0) {
            if (retracting && extend > 0) {
                extend--;
            } else if (!retracting) {
                extend++;
                if (extend >= MAX_EXTEND) {
                    level.playSound(null, worldPosition, HbmSoundEvents.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
                    BlockPos target = worldPosition.relative(facing(), 2);
                    ItemStack stack = items.get(0);
                    if (!stack.isEmpty() && PileGraphiteBlock.insertAutomatedRod(level, target, facing(), stack, false)) {
                        PileGraphiteBlock.insertAutomatedRod(level, target, facing(), stack, true);
                        removeItemNoUpdate(0);
                    }
                    retracting = true;
                    delay = 5;
                }
            }
        } else {
            delay--;
        }
        sync();
    }

    private void tickClient() {
        lastRenderExtend = renderExtend;
        if (!clientInitialized) {
            renderExtend = clientTarget;
            clientInitialized = true;
        } else {
            renderExtend += (clientTarget - renderExtend) / 2.0F;
        }
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
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Extend", extend);
        tag.putInt("Delay", delay);
        tag.putBoolean("Retracting", retracting);
        tag.putBoolean("LastPowered", lastPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        extend = Math.max(0, Math.min(MAX_EXTEND, tag.getInt("Extend")));
        delay = Math.max(0, tag.getInt("Delay"));
        retracting = !tag.contains("Retracting") || tag.getBoolean("Retracting");
        lastPowered = tag.getBoolean("LastPowered");
        clientTarget = extend;
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

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return slot == 0 ? ContainerHelper.removeItem(items, 0, amount) : ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? ContainerHelper.takeItem(items, 0) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) { items.set(0, stack.copyWithCount(Math.min(1, stack.getCount()))); setChanged(); } }
    @Override public int getMaxStackSize() { return 1; }
    @Override public void setChanged() { super.setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.set(0, ItemStack.EMPTY); setChanged(); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) { return slot == 0 && !stack.isEmpty(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return false; }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (!items.get(0).isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, items.get(0).copy()));
            clearContent();
        }
    }

}
