package com.reinhardt.hbm.item;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialShapes;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class FoundryMoldItem extends Item {
    private static final String MOLD_ID = "mold_id";
    private static final List<Mold> MOLDS = new ArrayList<>();

    public FoundryMoldItem(Properties properties) {
        super(properties);
        bootstrap();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.mold")
                .append(Component.literal(" - "))
                .append(mold(stack).title());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Mold mold = mold(stack);
        tooltip.add(mold.size() == 0
                ? Component.translatable("block.reinhardtshbm.foundry_mold").withStyle(ChatFormatting.GOLD)
                : Component.translatable("block.reinhardtshbm.foundry_basin").withStyle(ChatFormatting.RED));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        bootstrap();
        MOLDS.stream()
                .sorted(Comparator.comparingInt(Mold::order))
                .map(mold -> stackFor(this, mold.id()))
                .forEach(output::accept);
    }

    public static Mold mold(ItemStack stack) {
        bootstrap();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int id = tag.contains(MOLD_ID) ? tag.getInt(MOLD_ID) : modelData(stack);
        for (Mold mold : MOLDS) {
            if (mold.id() == id) {
                return mold;
            }
        }
        return MOLDS.getFirst();
    }

    public static ItemStack stackFor(Item item, int moldId) {
        bootstrap();
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(MOLD_ID, moldId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(moldId));
        return stack;
    }

    public static List<Mold> molds() {
        bootstrap();
        return List.copyOf(MOLDS);
    }

    private static int modelData(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null ? 0 : data.value();
    }

    private static void bootstrap() {
        if (!MOLDS.isEmpty()) {
            return;
        }
        shape(0, 0, "nugget", FoundryShape.NUGGET, 1);
        shape(1, 0, "billet", FoundryShape.BILLET, 1);
        shape(2, 0, "ingot", FoundryShape.INGOT, 1);
        shape(3, 0, "plate", FoundryShape.PLATE, 1);
        shape(4, 0, "wire", FoundryShape.WIRE, 8);
        multi(5, 0, "blade", FoundryShape.INGOT.q(3), (material, count) -> switch (material.name()) {
            case "titanium" -> item("blade_titanium", 1);
            case "tungsten" -> item("blade_tungsten", 1);
            default -> ItemStack.EMPTY;
        });
        multi(6, 0, "blades", FoundryShape.INGOT.q(4), (material, count) -> switch (material.name()) {
            case "steel" -> new ItemStack(HbmItems.BLADES_STEEL.get());
            case "titanium" -> new ItemStack(HbmItems.BLADES_TITANIUM.get());
            case "advanced_alloy" -> new ItemStack(HbmItems.BLADES_ADVANCED_ALLOY.get());
            default -> ItemStack.EMPTY;
        });
        multi(7, 0, "stamp", FoundryShape.INGOT.q(4), (material, count) -> switch (material.name()) {
            case "stone" -> item("stamp_stone_flat", 1);
            case "iron" -> item("stamp_iron_flat", 1);
            case "steel" -> item("stamp_steel_flat", 1);
            case "titanium" -> item("stamp_titanium_flat", 1);
            case "obsidian" -> item("stamp_obsidian_flat", 1);
            default -> ItemStack.EMPTY;
        });
        shape(8, 0, "shell", FoundryShape.SHELL, 1);
        shape(9, 0, "pipe", FoundryShape.PIPE, 1);
        shape(10, 1, "ingots", FoundryShape.INGOT, 9);
        shape(11, 1, "plates", FoundryShape.PLATE, 9);
        shape(12, 1, "block", FoundryShape.BLOCK, 1);
        multi(13, 1, "pipes", FoundryShape.BLOCK.q(3), (material, count) -> material.name().equals("steel") ? item("pipes_steel", 1) : ItemStack.EMPTY);
        shape(15, 1, "plates_cast", FoundryShape.CAST_PLATE, 3);
        shape(19, 0, "plate_cast", FoundryShape.CAST_PLATE, 1);
        shape(20, 0, "wire_dense", FoundryShape.DENSE_WIRE, 1);
        shape(21, 1, "wires_dense", FoundryShape.DENSE_WIRE, 9);
        shape(26, 0, "mechanism", FoundryShape.MECHANISM, 1);
    }

    private static void shape(int id, int size, String name, FoundryShape shape, int amount) {
        MOLDS.add(new Mold(id, size, name, shape.q(amount), amount, (material, count) -> outputFor(shape, material, count)));
    }

    private static void multi(int id, int size, String name, int cost, BiFunction<FoundryMaterial, Integer, ItemStack> output) {
        MOLDS.add(new Mold(id, size, name, cost, 1, output));
    }

    private static ItemStack outputFor(FoundryShape shape, FoundryMaterial material, int count) {
        if (material.name().equals("sodium") && shape == FoundryShape.INGOT) {
            return item("powder_sodium", count);
        }
        if (!FoundryMaterialShapes.supports(shape, material)) {
            return ItemStack.EMPTY;
        }
        if (isIndependentFoundryShape(shape)) {
            return item(HbmItems.independentFoundryItemPath(shape, material), count);
        }
        String prefix = switch (shape) {
            case NUGGET -> "nugget";
            case BILLET -> "billet";
            case INGOT -> "ingot";
            case PIPE -> "pipe";
            case SHELL -> "shell";
            case BLOCK -> "block";
            default -> shape.key();
        };
        return item(prefix + "_" + material.itemSuffix(), count);
    }

    private static boolean isIndependentFoundryShape(FoundryShape shape) {
        return shape == FoundryShape.PLATE
                || shape == FoundryShape.CAST_PLATE
                || shape == FoundryShape.WELDED_PLATE
                || shape == FoundryShape.WIRE
                || shape == FoundryShape.DENSE_WIRE
                || shape == FoundryShape.PIPE
                || shape == FoundryShape.BOLT
                || shape == FoundryShape.SHELL
                || shape == FoundryShape.MECHANISM;
    }

    private static ItemStack item(String path, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    public record Mold(int id, int size, String name, int cost, int count, BiFunction<FoundryMaterial, Integer, ItemStack> output) {
        public int order() {
            return MOLDS.indexOf(this);
        }

        public Component title() {
            return Component.translatable("shape.reinhardtshbm." + this.name).append(Component.literal(" x" + this.count));
        }

        public Optional<ItemStack> outputFor(FoundryMaterial material) {
            ItemStack stack = this.output.apply(material, this.count);
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        }
    }
}
