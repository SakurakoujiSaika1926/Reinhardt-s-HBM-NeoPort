package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.blockentity.DroneDockBlockEntity;
import com.reinhardt.hbm.blockentity.DroneProviderBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequesterBlockEntity;
import com.reinhardt.hbm.drone.DroneItemMatcher;
import com.reinhardt.hbm.item.LegacyDroneItem;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Programmed logistics drone from EntityRequestDrone. It flies the three
 * radio-network paths, pulls one matching provider stack, then returns the
 * unused drone to its dock exactly as the legacy entity did.
 */
public final class LegacyRequestDroneEntity extends LegacyDeliveryDroneEntity {
    private final List<Step> program = new ArrayList<>();
    private int nextActionTimer;

    public LegacyRequestDroneEntity(EntityType<? extends LegacyRequestDroneEntity> type, Level level) {
        super(type, level);
        configure(false, false);
    }

    public void addFlyStep(BlockPos point) {
        program.add(new Fly(point));
    }

    public void addLoadStep(BlockPos providerPoint, DroneItemMatcher matcher) {
        program.add(new Load(providerPoint, matcher));
    }

    public void addUnloadStep(BlockPos requesterPoint) {
        program.add(new Unload(requesterPoint));
    }

    public void addDockStep(BlockPos dockPoint) {
        program.add(new Dock(dockPoint));
    }

    public boolean isRequestDrone() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getDeltaMovement().lengthSqr() < 0.0001D) {
            executeNextAction();
        }
    }

    private void executeNextAction() {
        if (nextActionTimer > 0) {
            nextActionTimer--;
            return;
        }
        if (program.isEmpty()) {
            returnDroneAndCargo();
            return;
        }
        Step step = program.removeFirst();
        switch (step) {
            case Fly fly -> setTarget(fly.point().getX() + 0.5D, fly.point().getY() + 1.0D, fly.point().getZ() + 0.5D);
            case Load load -> load(load);
            case Unload unload -> unload(unload);
            case Dock dock -> dock(dock);
        }
    }

    private void load(Load load) {
        if (level().getBlockEntity(load.point().below()) instanceof DroneProviderBlockEntity provider) {
            ItemStack carried = provider.takeMatching(load.matcher());
            if (carried != null && !carried.isEmpty()) {
                setCargo(0, carried);
                setAppearance(1);
                level().playSound(null, blockPosition(), HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
            }
        }
        nextActionTimer = 5;
    }

    private void unload(Unload unload) {
        ItemStack carried = getCargo(0);
        if (!carried.isEmpty() && level().getBlockEntity(unload.point().below()) instanceof DroneRequesterBlockEntity requester) {
            ItemStack remaining = requester.insertRequested(carried);
            setCargo(0, remaining);
            if (remaining.isEmpty()) {
                setAppearance(0);
                level().playSound(null, blockPosition(), HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
            }
        }
        nextActionTimer = 5;
    }

    private void dock(Dock dock) {
        ItemStack carried = getCargo(0);
        if (level().getBlockEntity(dock.point().below()) instanceof DroneDockBlockEntity dockEntity) {
            ItemStack drone = LegacyDroneItem.stack(LegacyDroneItem.Type.REQUEST, 1);
            for (int slot = 0; slot < dockEntity.getContainerSize(); slot++) {
                ItemStack stack = dockEntity.getItem(slot);
                if (stack.isEmpty()) {
                    dockEntity.setItem(slot, drone);
                    returnCarriedToDock(dockEntity, carried, slot);
                    discard();
                    level().playSound(null, dockEntity.getBlockPos(), HbmSoundEvents.STORAGE_CLOSE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                    return;
                }
                if (ItemStack.isSameItemSameComponents(stack, drone) && stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    dockEntity.setChanged();
                    returnCarriedToDock(dockEntity, carried, slot);
                    discard();
                    level().playSound(null, dockEntity.getBlockPos(), HbmSoundEvents.STORAGE_CLOSE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                    return;
                }
            }
        }
        returnDroneAndCargo();
    }

    private void returnCarriedToDock(DroneDockBlockEntity dock, ItemStack carried, int preferredSlot) {
        if (carried.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < dock.getContainerSize(); slot++) {
            if (slot == preferredSlot) {
                continue;
            }
            if (dock.getItem(slot).isEmpty()) {
                dock.setItem(slot, carried);
                setCargo(0, ItemStack.EMPTY);
                return;
            }
        }
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), carried));
        setCargo(0, ItemStack.EMPTY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (level().isClientSide || !(attacker instanceof Player) || isRemoved()) {
            return false;
        }
        returnDroneAndCargo();
        return true;
    }

    private void returnDroneAndCargo() {
        if (!level().isClientSide && !isRemoved()) {
            ItemStack carried = getCargo(0);
            if (!carried.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), carried));
            }
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), LegacyDroneItem.stack(LegacyDroneItem.Type.REQUEST, 1)));
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("action_timer", nextActionTimer);
        tag.putInt("program_size", program.size());
        for (int index = 0; index < program.size(); index++) {
            tag.put("program_" + index, program.get(index).save(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nextActionTimer = tag.getInt("action_timer");
        program.clear();
        for (int index = 0; index < tag.getInt("program_size"); index++) {
            Step step = Step.load(tag.getCompound("program_" + index), registryAccess());
            if (step != null) {
                program.add(step);
            }
        }
    }

    private sealed interface Step permits Fly, Load, Unload, Dock {
        CompoundTag save(net.minecraft.core.HolderLookup.Provider registries);

        static Step load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            if (!tag.contains("kind")) {
                return null;
            }
            BlockPos point = BlockPos.of(tag.getLong("point"));
            return switch (tag.getString("kind")) {
                case "fly" -> new Fly(point);
                case "load" -> new Load(point, new DroneItemMatcher(
                        ItemStack.parseOptional(registries, tag.getCompound("pattern")), tag.getString("mode")));
                case "unload" -> new Unload(point);
                case "dock" -> new Dock(point);
                default -> null;
            };
        }
    }

    private record Fly(BlockPos point) implements Step {
        @Override public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) { return tag("fly", point); }
    }

    private record Load(BlockPos point, DroneItemMatcher matcher) implements Step {
        @Override public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) {
            CompoundTag tag = tag("load", point);
            tag.put("pattern", matcher.pattern().saveOptional(registries));
            tag.putString("mode", matcher.mode());
            return tag;
        }
    }

    private record Unload(BlockPos point) implements Step {
        @Override public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) { return tag("unload", point); }
    }

    private record Dock(BlockPos point) implements Step {
        @Override public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) { return tag("dock", point); }
    }

    private static CompoundTag tag(String kind, BlockPos point) {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", kind);
        tag.putLong("point", point.asLong());
        return tag;
    }
}
