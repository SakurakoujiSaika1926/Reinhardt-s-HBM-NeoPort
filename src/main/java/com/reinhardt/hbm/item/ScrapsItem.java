package com.reinhardt.hbm.item;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScrapsItem extends Item {
    private static final String AMOUNT = "amount";
    private static final String LIQUID = "liquid";
    /**
     * The 1.7.10 autogen renderer fell back to the shared scraps sprite for
     * these materials. All other obtainable scrap materials have a baked,
     * material-specific solid sprite in the modern resource pack.
     */
    private static final Set<String> BASE_SOLID_TEXTURE_MATERIALS = Set.of(
            "actinium",
            "am_mix",
            "am241",
            "am242",
            "au198",
            "co60",
            "ghiorsium",
            "meteoric_iron",
            "neptunium",
            "obsidian",
            "pb209",
            "pig_iron",
            "pu_mix",
            "pu238",
            "pu239",
            "pu240",
            "pu241",
            "schraranium",
            "solinium",
            "u233",
            "u235",
            "wrought_iron"
    );

    private final FoundryMaterial material;

    public ScrapsItem(Properties properties, FoundryMaterial material) {
        super(properties);
        this.material = Objects.requireNonNull(material, "material");
    }

    @Override
    public Component getName(ItemStack stack) {
        FoundryMaterialStack contents = contents(stack);
        if (contents != null && isLiquid(stack)) {
            return Component.translatable(
                    "item.reinhardtshbm.scraps.liquid",
                    Component.translatable(contents.material().translationKey())
            );
        }
        if (contents != null) {
            return Component.translatable(
                    "item.reinhardtshbm.scraps.solid",
                    Component.translatable(contents.material().translationKey())
            );
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FoundryMaterialStack contents = contents(stack);
        if (contents == null) {
            return;
        }
        if (!isLiquid(stack)) {
            tooltip.add(Component.translatable(contents.material().translationKey())
                    .append(Component.literal(", "))
                    .append(formatAmountComponent(contents.amount()))
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(formatAmountComponent(contents.amount()).withStyle(ChatFormatting.YELLOW));
        }
        if (contents.material().behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.foundry_additive").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static ItemStack create(FoundryMaterialStack contents, boolean liquid) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.scrapsItem(contents.material()).get());
        CompoundTag tag = new CompoundTag();
        tag.putInt(AMOUNT, contents.amount());
        tag.putBoolean(LIQUID, liquid);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        int modelData = liquid
                ? (contents.material().behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE ? 2 : 1)
                : 0;
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        return stack;
    }

    public static FoundryMaterialStack contents(ItemStack stack) {
        if (!(stack.getItem() instanceof ScrapsItem scraps)) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int amount = tag.contains(AMOUNT) ? tag.getInt(AMOUNT) : FoundryShape.INGOT.q(1);
        return new FoundryMaterialStack(scraps.material, amount);
    }

    public static boolean isLiquid(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(LIQUID);
    }

    public FoundryMaterial material() {
        return this.material;
    }

    public boolean usesDedicatedSolidTexture() {
        return !BASE_SOLID_TEXTURE_MATERIALS.contains(this.material.name());
    }

    public static boolean usesBaseSolidTexture(FoundryMaterial material) {
        return BASE_SOLID_TEXTURE_MATERIALS.contains(material.name());
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof ScrapsItem scraps)) {
            return 0xFFFFFFFF;
        }
        if (!isLiquid(stack) && scraps.usesDedicatedSolidTexture()) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | scraps.material.moltenColor();
    }

    public static String formatAmount(int amount) {
        int blocks = amount / FoundryShape.BLOCK.q(1);
        amount -= FoundryShape.BLOCK.q(blocks);
        int ingots = amount / FoundryShape.INGOT.q(1);
        amount -= FoundryShape.INGOT.q(ingots);
        int nuggets = amount / FoundryShape.NUGGET.q(1);
        amount -= FoundryShape.NUGGET.q(nuggets);

        StringBuilder builder = new StringBuilder();
        if (blocks > 0) {
            builder.append(blocks).append(blocks == 1 ? " block " : " blocks ");
        }
        if (ingots > 0) {
            builder.append(ingots).append(ingots == 1 ? " ingot " : " ingots ");
        }
        if (nuggets > 0) {
            builder.append(nuggets).append(nuggets == 1 ? " nugget " : " nuggets ");
        }
        if (amount > 0) {
            builder.append(amount).append(amount == 1 ? " quantum " : " quanta ");
        }
        return builder.isEmpty() ? "0 quanta" : builder.toString().trim();
    }

    public static MutableComponent formatAmountComponent(int amount) {
        int blocks = amount / FoundryShape.BLOCK.q(1);
        amount -= FoundryShape.BLOCK.q(blocks);
        int ingots = amount / FoundryShape.INGOT.q(1);
        amount -= FoundryShape.INGOT.q(ingots);
        int nuggets = amount / FoundryShape.NUGGET.q(1);
        amount -= FoundryShape.NUGGET.q(nuggets);

        MutableComponent result = Component.empty();
        boolean hasPart = false;
        if (blocks > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(blocks == 1 ? "matshape.reinhardtshbm.block" : "matshape.reinhardtshbm.blocks", blocks));
        }
        if (ingots > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(ingots == 1 ? "matshape.reinhardtshbm.ingot" : "matshape.reinhardtshbm.ingots", ingots));
        }
        if (nuggets > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(nuggets == 1 ? "matshape.reinhardtshbm.nugget" : "matshape.reinhardtshbm.nuggets", nuggets));
        }
        if (amount > 0) {
            hasPart = appendPart(result, hasPart, Component.translatable(amount == 1 ? "matshape.reinhardtshbm.quantum" : "matshape.reinhardtshbm.quanta", amount));
        }
        if (!hasPart) {
            result.append(Component.translatable("matshape.reinhardtshbm.quanta", 0));
        }
        return result;
    }

    private static boolean appendPart(MutableComponent result, boolean hasPart, Component part) {
        if (hasPart) {
            result.append(Component.literal(" "));
        }
        result.append(part);
        return true;
    }
}
