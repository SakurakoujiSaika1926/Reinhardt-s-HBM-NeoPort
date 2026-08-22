package com.reinhardt.hbm.item;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public class ScrapsItem extends Item {
    private static final String MATERIAL_ID = "material_id";
    private static final String MATERIAL = "material";
    private static final String AMOUNT = "amount";
    private static final String LIQUID = "liquid";

    public ScrapsItem(Properties properties) {
        super(properties);
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

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (material.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE
                    || material.behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE) {
                output.accept(create(new FoundryMaterialStack(material, FoundryShape.INGOT.q(1)), false));
            }
        }
    }

    public static ItemStack create(FoundryMaterialStack contents, boolean liquid) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.SCRAPS.get());
        CompoundTag tag = contents.save();
        tag.putBoolean(LIQUID, liquid);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        int modelData = liquid
                ? (contents.material().behavior() == FoundryMaterial.SmeltingBehavior.ADDITIVE ? 2 : 1)
                : 0;
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData));
        return stack;
    }

    public static FoundryMaterialStack contents(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        FoundryMaterial material = FoundryMaterial.byId(tag.getInt(MATERIAL_ID))
                .or(() -> FoundryMaterial.byName(tag.getString(MATERIAL)))
                .orElse(null);
        if (material == null) {
            return null;
        }
        int amount = tag.contains(AMOUNT) ? tag.getInt(AMOUNT) : FoundryShape.INGOT.q(1);
        return new FoundryMaterialStack(material, amount);
    }

    public static boolean isLiquid(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(LIQUID);
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
