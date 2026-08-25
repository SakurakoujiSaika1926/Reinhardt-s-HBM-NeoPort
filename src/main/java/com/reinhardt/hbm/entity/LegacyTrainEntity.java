package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.LegacyTrainItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cargo tram body with the 1.7.10 storage capacities and charge slot.  HBM
 * rails remain the only placement target; after placement vanilla rail physics
 * supplies the modernized equivalent of the old rail traversal code.
 */
public final class LegacyTrainEntity extends Minecart {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(LegacyTrainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ENERGY = SynchedEntityData.defineId(LegacyTrainEntity.class, EntityDataSerializers.LONG);
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(45, ItemStack.EMPTY);

    public LegacyTrainEntity(EntityType<? extends LegacyTrainEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, LegacyTrainItem.Type.CARGO_TRAM.ordinal());
        builder.define(ENERGY, 0L);
    }

    public LegacyTrainItem.Type variant() {
        return LegacyTrainItem.Type.byId(entityData.get(VARIANT));
    }

    public void setVariant(LegacyTrainItem.Type type) {
        entityData.set(VARIANT, type.ordinal());
    }

    public long energy() {
        return entityData.get(ENERGY);
    }

    @Override
    public Item getDropItem() {
        return HbmItems.TRAIN.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && variant().electric()) {
            entityData.set(ENERGY, BatteryPackItem.dischargeIntoMachine(inventory.get(28), energy(), 1_000L));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(menu());
        return InteractionResult.CONSUME;
    }

    @Override
    public void destroy(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide && !isRemoved()) {
            for (int i = 0; i < variant().slots(); i++) {
                if (!inventory.get(i).isEmpty()) {
                    spawnAtLocation(inventory.get(i));
                }
            }
            spawnAtLocation(LegacyTrainItem.stackFor(variant()));
            discard();
        }
    }

    private MenuProvider menu() {
        int slots = variant().slots();
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(54) {
            @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
            @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); }
            @Override public ItemStack removeItem(int slot, int amount) { return net.minecraft.world.ContainerHelper.removeItem(inventory, slot, amount); }
            @Override public ItemStack removeItemNoUpdate(int slot) { return net.minecraft.world.ContainerHelper.takeItem(inventory, slot); }
        };
        return new SimpleMenuProvider((id, playerInventory, ignored) -> new ChestMenu(
                MenuType.GENERIC_9x6, id, playerInventory, container, 6),
                Component.translatable("container.reinhardtshbm.train." + variant().id()));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("variant", variant().ordinal());
        tag.putLong("energy", energy());
        for (int i = 0; i < variant().slots(); i++) {
            if (!inventory.get(i).isEmpty()) {
                tag.put("slot_" + i, inventory.get(i).saveOptional(registryAccess()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(LegacyTrainItem.Type.byId(tag.getInt("variant")));
        entityData.set(ENERGY, Math.max(0L, Math.min(1_000L, tag.getLong("energy"))));
        for (int i = 0; i < inventory.size(); i++) {
            inventory.set(i, i < variant().slots() && tag.contains("slot_" + i)
                    ? ItemStack.parseOptional(registryAccess(), tag.getCompound("slot_" + i))
                    : ItemStack.EMPTY);
        }
    }
}
