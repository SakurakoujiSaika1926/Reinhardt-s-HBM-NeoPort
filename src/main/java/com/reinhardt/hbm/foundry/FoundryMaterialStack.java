package com.reinhardt.hbm.foundry;

import net.minecraft.nbt.CompoundTag;

public record FoundryMaterialStack(FoundryMaterial material, int amount) {
    public FoundryMaterialStack {
        amount = Math.max(0, amount);
    }

    public FoundryMaterialStack copy() {
        return new FoundryMaterialStack(this.material, this.amount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("material_id", this.material.id());
        tag.putString("material", this.material.name());
        tag.putInt("amount", this.amount);
        return tag;
    }

    public static FoundryMaterialStack load(CompoundTag tag) {
        FoundryMaterial material = FoundryMaterial.byId(tag.getInt("material_id"))
                .or(() -> FoundryMaterial.byName(tag.getString("material")))
                .orElse(null);
        if (material == null) {
            return null;
        }
        return new FoundryMaterialStack(material, tag.getInt("amount"));
    }
}
