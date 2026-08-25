package com.reinhardt.hbm.item;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public class GasMaskItem extends ArmorItem implements FilterableGasMask {
    public static final String FILTER_KEY = "hfrFilter";

    private final Kind kind;

    public GasMaskItem(Holder<ArmorMaterial> material, Kind kind, Properties properties) {
        super(material, Type.HELMET, properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    public Set<HbmArmorProtection.HazardClass> blacklist() {
        return this.kind.blacklist();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack filter = getInstalledFilter(stack, player.registryAccess());
        if (filter.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        removeFilter(stack);
        if (!level.isClientSide) {
            if (!player.getInventory().add(filter)) {
                player.drop(filter, false);
            }
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack filter = getInstalledFilter(stack, context.registries());
        if (filter.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.no_filter").withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.translatable("tooltip.reinhardtshbm.gas_mask.installed_filter").withStyle(ChatFormatting.GOLD));
        int maxDamage = filter.getMaxDamage();
        if (maxDamage > 0) {
            int percent = Math.max(0, (maxDamage - filter.getDamageValue()) * 100 / maxDamage);
            tooltip.add(Component.literal("  ")
                    .append(filter.getHoverName())
                    .append(" (" + percent + "%)")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("  ").append(filter.getHoverName()).withStyle(ChatFormatting.YELLOW));
        }
    }

    public static boolean installFilter(ItemStack mask, ItemStack filter, LivingEntity entity) {
        if (!(mask.getItem() instanceof FilterableGasMask gasMask) || !(filter.getItem() instanceof GasMaskFilterItem)) {
            return false;
        }
        if (!gasMask.isFilterApplicable(filter)) {
            return false;
        }

        CompoundTag root = mask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(FILTER_KEY, filter.save(entity.registryAccess()));
        mask.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return true;
    }

    public static ItemStack getInstalledFilter(ItemStack mask, net.minecraft.core.HolderLookup.Provider registries) {
        CustomData data = mask.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(FILTER_KEY)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, root.getCompound(FILTER_KEY));
    }

    public static void removeFilter(ItemStack mask) {
        CompoundTag root = mask.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.remove(FILTER_KEY);
        if (root.isEmpty()) {
            mask.remove(DataComponents.CUSTOM_DATA);
        } else {
            mask.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    public static void damageInstalledFilter(ItemStack mask, LivingEntity entity, int damage) {
        ItemStack filter = getInstalledFilter(mask, entity.registryAccess());
        if (filter.isEmpty() || filter.getMaxDamage() <= 0) {
            return;
        }
        filter.setDamageValue(filter.getDamageValue() + damage);
        if (filter.getDamageValue() > filter.getMaxDamage()) {
            removeFilter(mask);
        } else {
            installFilter(mask, filter, entity);
        }
    }

    public boolean isFilterApplicable(ItemStack filter) {
        return filter.getItem() instanceof GasMaskFilterItem;
    }

    public static boolean isWornBy(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem;
    }

    public enum Kind {
        STANDARD(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING)),
        M65(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING)),
        OLDE(Set.of(HbmArmorProtection.HazardClass.GAS_BLISTERING)),
        MONO(Set.of(
                HbmArmorProtection.HazardClass.GAS_LUNG,
                HbmArmorProtection.HazardClass.GAS_BLISTERING,
                HbmArmorProtection.HazardClass.BACTERIA
        ));

        private final Set<HbmArmorProtection.HazardClass> blacklist;

        Kind(Set<HbmArmorProtection.HazardClass> blacklist) {
            this.blacklist = Set.copyOf(blacklist);
        }

        public Set<HbmArmorProtection.HazardClass> blacklist() {
            return this.blacklist;
        }
    }
}
