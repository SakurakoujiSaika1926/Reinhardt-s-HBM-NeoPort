package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SoyuzCapsuleEntity extends Entity {
    private final ItemStack[] payload = new ItemStack[18];
    private int soyuzSkin;

    public SoyuzCapsuleEntity(EntityType<? extends SoyuzCapsuleEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        setNoGravity(true);
        for (int i = 0; i < this.payload.length; i++) {
            this.payload[i] = ItemStack.EMPTY;
        }
    }

    public SoyuzCapsuleEntity(Level level, int soyuzSkin) {
        this(HbmEntityTypes.SOYUZ_CAPSULE.get(), level);
        this.soyuzSkin = soyuzSkin;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        if (motion.y > -0.2D) {
            motion = new Vec3(motion.x, motion.y - 0.02D, motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        if (getY() > 600.0D) {
            setPos(getX(), 600.0D, getZ());
        }

        if (!level().isClientSide && hitBlock()) {
            land();
        }
    }

    public void setPayload(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.payload.length) {
            this.payload[slot] = stack.copy();
        }
    }

    private boolean hitBlock() {
        BlockPos pos = BlockPos.containing(getX(), getY(), getZ());
        return !level().getBlockState(pos).isAir();
    }

    private void land() {
        BlockPos hit = BlockPos.containing(getX(), getY(), getZ());
        BlockPos placePos = hit.above();
        BlockState targetState = level().getBlockState(placePos);
        if (!targetState.canBeReplaced()) {
            placePos = hit;
        }
        if (!level().getBlockState(placePos).canBeReplaced()) {
            discard();
            return;
        }

        level().setBlock(placePos, HbmBlocks.SOYUZ_CAPSULE.get().defaultBlockState(), 3);
        if (level().getBlockEntity(placePos) instanceof SoyuzCapsuleBlockEntity capsule) {
            for (int i = 0; i < this.payload.length; i++) {
                capsule.setItem(i, this.payload[i]);
            }
            capsule.setItem(SoyuzCapsuleBlockEntity.SLOT_ROCKET, new ItemStack(soyuzItem()));
        }
        discard();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Soyuz", this.soyuzSkin);
        for (int i = 0; i < this.payload.length; i++) {
            if (!this.payload[i].isEmpty()) {
                tag.put("Slot" + i, this.payload[i].saveOptional(registryAccess()));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.soyuzSkin = tag.getInt("Soyuz");
        for (int i = 0; i < this.payload.length; i++) {
            this.payload[i] = ItemStack.parseOptional(registryAccess(), tag.getCompound("Slot" + i));
        }
    }

    private static Item soyuzItem() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", "missile_soyuz"));
        return item == null ? Blocks.AIR.asItem() : item;
    }
}
