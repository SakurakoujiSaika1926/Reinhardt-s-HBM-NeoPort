package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/** Explicit components of the old MissileStruct; absent or invalid parts are not substituted. */
public record CustomMissileData(ItemStack chip, ItemStack warhead, ItemStack fuselage,
                                ItemStack fins, ItemStack thruster) {
    @Nullable public static CustomMissileData read(ItemStack stack) {
        if (!(stack.getItem() instanceof CustomMissileItem)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ItemStack chip = part(tag, "chip", MissilePartItem.Type.CHIP);
        ItemStack warhead = part(tag, "warhead", MissilePartItem.Type.WARHEAD);
        ItemStack fuselage = part(tag, "fuselage", MissilePartItem.Type.FUSELAGE);
        ItemStack thruster = part(tag, "thruster", MissilePartItem.Type.THRUSTER);
        ItemStack fins = part(tag, "stability", MissilePartItem.Type.FINS);
        if (chip.isEmpty() || warhead.isEmpty() || fuselage.isEmpty() || thruster.isEmpty()
                || (tag.contains("stability") && fins.isEmpty())) return null;
        return new CustomMissileData(chip, warhead, fuselage, fins, thruster);
    }
    private static ItemStack part(CompoundTag tag, String key, MissilePartItem.Type expected) {
        if (!tag.contains(key)) return ItemStack.EMPTY;
        String value = tag.getString(key);
        ResourceLocation id = ResourceLocation.tryParse(value.indexOf(':') < 0 ? ReinhardtsHBM.MOD_ID + ":" + value : value);
        if (id == null) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (!(item instanceof MissilePartItem)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        return MissilePartItem.definition(stack).type() == expected ? stack : ItemStack.EMPTY;
    }
    public MissilePartItem.Definition fuselageDefinition() { return MissilePartItem.definition(fuselage); }
    public int fuelRequired() { return (int) fuselageDefinition().primary(); }
    public float inaccuracy() { return MissilePartItem.definition(chip).primary()
            * (fins.isEmpty() ? 1F : MissilePartItem.definition(fins).primary()); }
}
