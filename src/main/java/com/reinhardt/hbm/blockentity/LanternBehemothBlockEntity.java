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
    private static final AABB NORMAL_LOCAL_BOUNDS = new AABB(
            0.1308D, -0.25D, 0.125D,
            0.875D, 5.5D, 0.875D);
    private static final AABB BROKEN_LOCAL_BOUNDS = new AABB(
            -0.7101258039474487D, -0.27778297662734985D, 0.35212594270706177D,
            0.6665130257606506D, 5.41259241104126D, 1.297677755355835D);

    private boolean broken;
    private int communicationTimer = -1;
    private volatile RenderSnapshot renderSnapshot;

    public LanternBehemothBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LANTERN_BEHEMOTH.get(), pos, state);
        this.renderSnapshot = buildRenderSnapshot(state, false);
    }

    public boolean isBroken() {
        return broken;
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
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
        lantern.setBroken(false);
        lantern.communicationTimer = 400;
        lantern.setChanged();
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
        if (this.broken == broken) {
            return;
        }
        this.broken = broken;
        publishRenderSnapshot();
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
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
        publishRenderSnapshot();
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

    private void publishRenderSnapshot() {
        RenderSnapshot previous = this.renderSnapshot;
        RenderSnapshot next = buildRenderSnapshot(this.getBlockState(), this.broken);
        this.renderSnapshot = next;
        if (this.level != null && this.level.isClientSide && previous != null
                && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            this.level.sendBlockUpdated(
                    this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private RenderSnapshot buildRenderSnapshot(BlockState state, boolean isBroken) {
        AABB local = isBroken ? BROKEN_LOCAL_BOUNDS : NORMAL_LOCAL_BOUNDS;
        return new RenderSnapshot(state, isBroken, local.move(this.worldPosition));
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return sectionMin(first.minX) == sectionMin(second.minX)
                && sectionMin(first.minY) == sectionMin(second.minY)
                && sectionMin(first.minZ) == sectionMin(second.minZ)
                && sectionMax(first.maxX) == sectionMax(second.maxX)
                && sectionMax(first.maxY) == sectionMax(second.maxY)
                && sectionMax(first.maxZ) == sectionMax(second.maxZ);
    }

    private static int sectionMin(double value) {
        return net.minecraft.util.Mth.floor(value) >> 4;
    }

    private static int sectionMax(double value) {
        return net.minecraft.util.Mth.floor(Math.nextDown(value)) >> 4;
    }

    public record RenderSnapshot(BlockState blockState, boolean broken, AABB bounds) {
    }
}
