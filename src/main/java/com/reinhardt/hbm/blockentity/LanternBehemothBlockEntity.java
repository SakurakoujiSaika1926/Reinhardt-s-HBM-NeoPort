package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.item.BlowtorchItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.entity.LegacyBobmazonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Exact persistent state and timer behavior of TileEntityLanternBehemoth. */
public final class LanternBehemothBlockEntity extends BlockEntity {
    private static final TagKey<net.minecraft.world.item.Item> STEEL_PLATES = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/steel"));

    private boolean broken;
    private int communicationTimer = -1;

    public LanternBehemothBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LANTERN_BEHEMOTH.get(), pos, state);
    }

    public boolean isBroken() {
        return broken;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LanternBehemothBlockEntity lantern) {
        if (level.isClientSide) {
            return;
        }

        if (lantern.communicationTimer == 360) {
            level.playSound(null, pos, HbmSoundEvents.HORN_NEAR_SINGLE.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
        }
        if (lantern.communicationTimer == 280) {
            level.playSound(null, pos, HbmSoundEvents.HORN_FAR_SINGLE.get(), SoundSource.BLOCKS, 10_000.0F, 1.0F);
        }
        if (lantern.communicationTimer == 220) {
            level.playSound(null, pos, HbmSoundEvents.HORN_NEAR_DUAL.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
        }
        if (lantern.communicationTimer == 100) {
            level.playSound(null, pos, HbmSoundEvents.HORN_FAR_DUAL.get(), SoundSource.BLOCKS, 10_000.0F, 1.0F);
        }
        if (lantern.communicationTimer == 0) {
            lantern.deliverSupplies(level);
        }
        if (lantern.communicationTimer >= 0) {
            lantern.communicationTimer--;
            lantern.setChanged();
        }
    }

    private void deliverSupplies(Level level) {
        ItemStack basic = new ItemStack(HbmItems.CIRCUIT_BASIC.get());
        basic.setCount(4 + level.random.nextInt(4));
        ItemStack advanced = new ItemStack(HbmItems.CIRCUIT_ADVANCED.get());
        advanced.setCount(4 + level.random.nextInt(2));

        ItemStack kit = new ItemStack(HbmItems.KIT_CUSTOM.get());
        com.reinhardt.hbm.item.LegacyCustomKitItem.create(
                kit, level.registryAccess(), 0xFFFFFF, 0x008000,
                basic, advanced,
                new ItemStack(Items.DIAMOND, 6 + level.random.nextInt(6)),
                new ItemStack(Blocks.POPPY));

        LegacyBobmazonEntity courier = new LegacyBobmazonEntity(
                level,
                getBlockPos().getX() + 0.5D + level.random.nextGaussian() * 10.0D,
                getBlockPos().getZ() + 0.5D + level.random.nextGaussian() * 10.0D,
                kit);
        level.addFreshEntity(courier);
    }

    public static boolean tryRepair(ItemStack held, Level level, BlockPos pos, Player player) {
        if (!(held.getItem() instanceof BlowtorchItem blowtorch) || level.isClientSide
                || !(level.getBlockEntity(pos) instanceof LanternBehemothBlockEntity lantern)) {
            return false;
        }
        if (!lantern.broken || !blowtorch.canTorch(held)) {
            return false;
        }

        int plates = 0;
        boolean hasCircuit = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(STEEL_PLATES)) {
                plates += stack.getCount();
            } else if (isBasicCircuit(stack)) {
                hasCircuit = true;
            }
        }
        if (plates < 2 || !hasCircuit) {
            return false;
        }

        consume(player, STEEL_PLATES, 2);
        consumeBasicCircuit(player);
        lantern.broken = false;
        lantern.communicationTimer = 400;
        lantern.setChanged();
        level.sendBlockUpdated(pos, lantern.getBlockState(), lantern.getBlockState(), Block.UPDATE_CLIENTS);
        blowtorch.consumeTorchFuel(held);
        return true;
    }

    private static boolean isBasicCircuit(ItemStack stack) {
        return stack.is(HbmItems.CIRCUIT_BASIC.get());
    }

    private static void consume(Player player, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag, int count) {
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(tag)) {
                continue;
            }
            int used = Math.min(remaining, stack.getCount());
            stack.shrink(used);
            remaining -= used;
            if (remaining == 0) {
                return;
            }
        }
    }

    private static void consumeBasicCircuit(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isBasicCircuit(stack)) {
                stack.shrink(1);
                return;
            }
        }
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isBroken", broken);
        tag.putInt("comTimer", communicationTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        broken = tag.getBoolean("isBroken");
        communicationTimer = tag.getInt("comTimer");
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

    public AABB renderBounds() {
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 6.0D, pos.getZ() + 1.0D);
    }
}
