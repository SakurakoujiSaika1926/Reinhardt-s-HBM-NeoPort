package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LegacyNukeBlock;
import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.explosion.BalefireExplosionManager;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class LegacyNukeBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    private static final int MAX_SLOTS = 27;
    private static final int[] NO_AUTOMATION = {};
    private final NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
    public float tnt;
    public float nuke;
    public float hydro;
    public float amat;
    public float dirty;
    public float schrab;
    public float euph;

    public LegacyNukeBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_NUKE.get(), pos, state);
    }

    public LegacyNukeDefinition definition() {
        return getBlockState().getBlock() instanceof LegacyNukeBlock block ? block.definition() : LegacyNukeDefinition.CUSTOM;
    }

    @Override public int getContainerSize() { return MAX_SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return valid(slot) ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (!valid(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = items.get(slot).split(amount);
        if (!result.isEmpty()) setChangedAndSync();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (!valid(slot)) return ItemStack.EMPTY;
        ItemStack result = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!valid(slot)) return;
        items.set(slot, stack.copy());
        refreshCustomPayload();
        setChangedAndSync();
    }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction side) { return NO_AUTOMATION; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    public boolean slotHasExpectedItem(int slot) {
        return valid(slot) && matches(items.get(slot), definition().expected(slot));
    }

    public boolean isReady() {
        LegacyNukeDefinition def = definition();
        if (def.isCustom()) return hasCustomPayload();
        int requiredSlots = def == LegacyNukeDefinition.TSAR ? 5 : def.slotCount();
        for (int slot = 0; slot < requiredSlots; slot++) {
            if (!slotHasExpectedItem(slot)) return false;
        }
        return true;
    }

    public boolean isFilled() {
        LegacyNukeDefinition def = definition();
        return def == LegacyNukeDefinition.TSAR ? isReady() && slotHasExpectedItem(5) : isReady();
    }

    public void detonate() {
        if (!(level instanceof ServerLevel serverLevel) || !isReady()) return;
        LegacyNukeDefinition def = definition();
        refreshCustomPayload();
        clearContent();
        serverLevel.removeBlock(worldPosition, false);
        if (def == LegacyNukeDefinition.BALEFIRE) {
            BalefireExplosionManager.schedule(serverLevel, worldPosition, 250);
        } else if (def.isCustom()) {
            NukeExplosionManager.scheduleCustomNuke(serverLevel, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    tnt, nuke, hydro, amat, dirty, schrab, euph);
        } else {
            NukeExplosionManager.scheduleLegacyNuke(serverLevel, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, def.radius());
        }
    }

    private boolean hasCustomPayload() {
        refreshCustomPayload();
        return tnt > 0.0F || nuke > 0.0F || hydro > 0.0F || amat > 0.0F || schrab > 0.0F || euph > 0.0F;
    }

    private void refreshCustomPayload() {
        float tntValue = 0.0F, nukeValue = 0.0F, hydroValue = 0.0F;
        float amatValue = 0.0F, dirtyValue = 0.0F, schrabValue = 0.0F, euphValue = 0.0F;
        float tntMultiplier = 1.0F, nukeMultiplier = 1.0F, hydroMultiplier = 1.0F;
        float amatMultiplier = 1.0F, dirtyMultiplier = 1.0F, schrabMultiplier = 1.0F;

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            int count = stack.getCount();
            switch (id) {
                case "gunpowder" -> tntValue += 0.8F * count;
                case "tnt" -> tntValue += 4.0F * count;
                case "det_cord" -> tntValue += 1.5F * count;
                case "ingot_semtex" -> tntValue += 8.0F * count;
                case "det_charge" -> tntValue += 15.0F * count;
                case "red_barrel" -> tntValue += 2.5F * count;
                case "pink_barrel" -> tntValue += 4.0F * count;
                case "custom_tnt" -> tntValue += 10.0F * count;
                case "ingot_u233", "ingot_u235" -> nukeValue += 15.0F * count;
                case "custom_nuke" -> nukeValue += 30.0F * count;
                case "ingot_pu239", "ingot_pu241" -> nukeValue += 25.0F * count;
                case "ingot_neptunium", "powder_neptunium" -> nukeValue += 30.0F * count;
                case "nugget_u233", "nugget_u235" -> nukeValue += 1.5F * count;
                case "nugget_pu239", "nugget_pu241" -> nukeValue += 2.5F * count;
                case "nugget_neptunium" -> nukeValue += 3.0F * count;
                case "cell_deuterium" -> hydroValue += 20.0F * count;
                case "cell_tritium" -> hydroValue += 30.0F * count;
                case "lithium" -> hydroValue += 20.0F * count;
                case "custom_hydro" -> hydroValue += 30.0F * count;
                case "cell_antimatter" -> amatValue += 5.0F * count;
                case "egg_balefire_shard" -> amatValue += 15.0F * count;
                case "custom_amat" -> amatValue += 15.0F * count;
                case "egg_balefire" -> amatValue += 150.0F * count;
                case "ingot_tungsten", "custom_dirty" -> dirtyValue += (id.equals("ingot_tungsten") ? 1.0F : 10.0F) * count;
                case "ingot_schrabidium", "powder_schrabidium" -> schrabValue += 5.0F * count;
                case "block_schrabidium" -> schrabValue += 50.0F * count;
                case "nugget_schrabidium" -> schrabValue += 0.5F * count;
                case "cell_sas3" -> schrabValue += 7.5F * count;
                case "cell_anti_schrabidium", "custom_schrab" -> schrabValue += 15.0F * count;
                case "nugget_euphemium", "ingot_euphemium" -> euphValue += 1.0F * count;
                case "redstone" -> tntMultiplier *= 1.05F * count;
                case "redstone_block" -> tntMultiplier *= 1.5F * count;
                case "ingot_uranium", "ingot_u238" -> nukeMultiplier *= (id.equals("ingot_uranium") ? 1.05F : 1.1F) * count;
                case "ingot_plutonium", "ingot_pu238" -> nukeMultiplier *= 1.15F * count;
                case "nugget_uranium", "nugget_u238" -> nukeMultiplier *= (id.equals("nugget_uranium") ? 1.005F : 1.01F) * count;
                case "nugget_plutonium", "nugget_pu238" -> nukeMultiplier *= (id.equals("nugget_plutonium") ? 1.15F : 1.015F) * count;
                case "powder_uranium" -> nukeMultiplier *= 1.05F * count;
                case "powder_plutonium" -> nukeMultiplier *= 1.15F * count;
                case "ingot_pu240", "nuclear_waste" -> dirtyMultiplier *= (id.equals("ingot_pu240") ? 1.05F : 1.025F) * count;
                case "block_waste" -> dirtyMultiplier *= 1.25F * count;
                case "yellow_barrel" -> dirtyMultiplier *= 1.2F * count;
                default -> { }
            }
        }
        tntValue *= tntMultiplier;
        nukeValue *= nukeMultiplier;
        hydroValue *= hydroMultiplier;
        amatValue *= amatMultiplier;
        dirtyValue *= dirtyMultiplier;
        schrabValue *= schrabMultiplier;
        if (tntValue < 16.0F) nukeValue = 0.0F;
        if (nukeValue < 100.0F) hydroValue = 0.0F;
        if (nukeValue < 50.0F) amatValue = 0.0F;
        if (nukeValue < 50.0F) schrabValue = 0.0F;
        if (schrabValue == 0.0F) euphValue = 0.0F;
        tnt = tntValue;
        nuke = nukeValue;
        hydro = hydroValue;
        amat = amatValue;
        dirty = dirtyValue;
        schrab = schrabValue;
        euph = euphValue;
    }

    private static boolean matches(ItemStack stack, String expected) {
        if (stack.isEmpty() || expected == null || expected.isEmpty()) return false;
        String[] alternatives = expected.split("\\|");
        String actual = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String option : alternatives) {
            String[] variant = option.split(":", 2);
            if (actual.equals(variant[0])) {
                if (variant.length == 1) return true;
                CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                return data.getString("variant").equals(variant[1])
                        || data.getString("type").equals(variant[1])
                        || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().endsWith(variant[1]);
            }
        }
        return false;
    }

    @Override public void clearContent() { for (int i = 0; i < MAX_SLOTS; i++) items.set(i, ItemStack.EMPTY); setChangedAndSync(); }

    @Override public Component getDisplayName() { return Component.translatable("container.reinhardtshbm." + definition().blockId()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new com.reinhardt.hbm.menu.LegacyNukeMenu(id, inventory, this); }
    @Override public void dropContents(Level level, BlockPos pos) { for (ItemStack stack : items) if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, stack.copy())); clearContent(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < MAX_SLOTS; i++) tag.put("Slot" + i, items.get(i).saveOptional(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < MAX_SLOTS; i++) items.set(i, ItemStack.parseOptional(registries, tag.getCompound("Slot" + i)));
        refreshCustomPayload();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
    private static boolean valid(int slot) { return slot >= 0 && slot < MAX_SLOTS; }
}
