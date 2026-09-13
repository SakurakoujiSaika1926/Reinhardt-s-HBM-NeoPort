package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.LegacyMinecartItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The five old HBM minecart variants share rail physics. Their type and base
 * are synchronized instead of registering five visually identical entity IDs.
 */
public final class LegacyMinecartEntity extends Minecart {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(LegacyMinecartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BASE = SynchedEntityData.defineId(LegacyMinecartEntity.class, EntityDataSerializers.INT);
    private final NonNullList<ItemStack> cargo = NonNullList.withSize(54, ItemStack.EMPTY);

    public LegacyMinecartEntity(EntityType<? extends LegacyMinecartEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, LegacyMinecartItem.Type.EMPTY.ordinal());
        builder.define(BASE, LegacyMinecartItem.Base.VANILLA.ordinal());
    }

    public LegacyMinecartItem.Type variant() {
        return LegacyMinecartItem.Type.byId(entityData.get(VARIANT));
    }

    public void setVariant(LegacyMinecartItem.Type type) {
        entityData.set(VARIANT, type.ordinal());
    }

    public LegacyMinecartItem.Base base() {
        return LegacyMinecartItem.Base.byId(entityData.get(BASE));
    }

    public void setBase(LegacyMinecartItem.Base base) {
        entityData.set(BASE, base.ordinal());
    }

    @Override
    public Item getDropItem() {
        return HbmItems.CART.get();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (variant() == LegacyMinecartItem.Type.CRATE) {
            player.openMenu(menu("container.reinhardtshbm.cart_crate", 54));
            return InteractionResult.CONSUME;
        }
        if (variant() == LegacyMinecartItem.Type.DESTROYER) {
            player.openMenu(menu("container.reinhardtshbm.cart_destroyer", 18));
            return InteractionResult.CONSUME;
        }
        return super.interact(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && variant() == LegacyMinecartItem.Type.DESTROYER && tickCount % 5 == 0) {
            destroyMatchedItems();
        }
        if (level().isClientSide && variant() == LegacyMinecartItem.Type.DESTROYER && tickCount % 5 == 0) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, getX(), getY() + 0.75D, getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public void destroy(net.minecraft.world.damagesource.DamageSource source) {
        if (!level().isClientSide && !isRemoved()) {
            ItemStack drop = asItemStack();
            spawnAtLocation(drop);
            discard();
        }
    }

    public void restoreCargo(ItemStack stack, net.minecraft.core.RegistryAccess registries) {
        CompoundTag stored = LegacyMinecartItem.cargo(stack);
        for (int i = 0; i < cargo.size(); i++) {
            cargo.set(i, stored.contains("slot_" + i)
                    ? ItemStack.parseOptional(registries, stored.getCompound("slot_" + i))
                    : ItemStack.EMPTY);
        }
    }

    /** EntityMinecartModBase/EntityMinecartDeobf#canTriggerWalking returned false. */
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    private void destroyMatchedItems() {
        boolean destroyed = false;
        for (ItemEntity entity : level().getEntitiesOfClass(ItemEntity.class, new AABB(getX() - 2.5D, getY() - 1.5D, getZ() - 2.5D, getX() + 2.5D, getY() + 2.0D, getZ() + 2.5D))) {
            ItemStack incoming = entity.getItem();
            if (matches(incoming, 0, 9, true) || matches(incoming, 9, 18, false)) {
                entity.discard();
                destroyed = true;
            }
        }
        if (destroyed) {
            level().playSound(null, blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 0.5F, 0.5F + random.nextFloat() * 0.2F);
        }
    }

    private boolean matches(ItemStack incoming, int start, int end, boolean requireComponents) {
        for (int slot = start; slot < end; slot++) {
            ItemStack filter = cargo.get(slot);
            if (!filter.isEmpty() && ItemStack.isSameItem(filter, incoming)
                    && (!requireComponents || ItemStack.isSameItemSameComponents(filter, incoming))) {
                return true;
            }
        }
        return false;
    }

    private MenuProvider menu(String title, int slots) {
        net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(slots) {
            @Override
            public ItemStack getItem(int slot) {
                return cargo.get(slot);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                cargo.set(slot, stack);
                setChanged();
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return net.minecraft.world.ContainerHelper.removeItem(cargo, slot, amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return net.minecraft.world.ContainerHelper.takeItem(cargo, slot);
            }

            @Override
            public void setChanged() {
                // Cargo is persisted by the entity's next save pass.
            }
        };
        return new SimpleMenuProvider((id, playerInventory, ignored) -> new ChestMenu(
                slots == 54 ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x2,
                id,
                playerInventory,
                inventory,
                slots / 9),
                Component.translatable(title));
    }

    private ItemStack asItemStack() {
        ItemStack stack = LegacyMinecartItem.createCartItem(base(), variant());
        CompoundTag stored = new CompoundTag();
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack contained = cargo.get(i);
            if (!contained.isEmpty()) {
                stored.put("slot_" + i, contained.saveOptional(registryAccess()));
            }
        }
        LegacyMinecartItem.setCargo(stack, stored);
        if (hasCustomName()) {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
        }
        return stack;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("base", base().ordinal());
        tag.putInt("variant", variant().ordinal());
        for (int i = 0; i < cargo.size(); i++) {
            ItemStack contained = cargo.get(i);
            if (!contained.isEmpty()) {
                tag.put("slot_" + i, contained.saveOptional(registryAccess()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBase(LegacyMinecartItem.Base.byId(tag.getInt("base")));
        setVariant(LegacyMinecartItem.Type.byId(tag.getInt("variant")));
        for (int i = 0; i < cargo.size(); i++) {
            cargo.set(i, tag.contains("slot_" + i)
                    ? ItemStack.parseOptional(registryAccess(), tag.getCompound("slot_" + i))
                    : ItemStack.EMPTY);
        }
    }
}
