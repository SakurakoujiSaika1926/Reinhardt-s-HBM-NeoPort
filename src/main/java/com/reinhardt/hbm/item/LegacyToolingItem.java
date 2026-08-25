package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Direct 1.7.10 ItemTooling / ItemToolingWeapon equivalents. */
public final class LegacyToolingItem extends Item {
    private LegacyToolingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static LegacyToolingItem handDrill(int durability) {
        Properties properties = new Properties();
        if (durability > 0) {
            properties.durability(durability);
        }
        return new LegacyToolingItem(properties);
    }

    public static LegacyToolingItem archineerWrench() {
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ReinhardtsHBM.id("archineer_wrench_damage"), 12.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
        return new LegacyToolingItem(new Properties().durability(1_000).component(DataComponents.ATTRIBUTE_MODIFIERS, attributes));
    }
}
