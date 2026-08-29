package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/** Exact 1.7.10 BlockToolConversion table, evaluated against the main inventory. */
final class ToolConversion {
    enum Tool {
        TORCH,
        BOLT
    }

    private static final TagKey<net.minecraft.world.item.Item> DURA_STEEL_BOLTS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "bolts/dura_steel"));
    private static final Requirement STEEL_CAST_PLATE = material(30);
    private static final Requirement BISMOID_BRONZE_CAST_PLATE = material(46, 47);
    private static final Requirement DURA_STEEL_BOLT = new Requirement(stack -> stack.is(DURA_STEEL_BOLTS), 4);

    private ToolConversion() {
    }

    static boolean isConvertible(Level level, BlockPos pos, Tool tool) {
        return conversion(level.getBlockState(pos), tool) != null;
    }

    static boolean convert(Level level, BlockPos pos, Player player, Tool tool) {
        Conversion conversion = conversion(level.getBlockState(pos), tool);
        if (conversion == null || !consume(player, conversion.requirements())) {
            return false;
        }
        return level.setBlock(pos, conversion.output(), net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    private static Conversion conversion(BlockState state, Tool tool) {
        if (tool == Tool.TORCH && state.is(HbmBlocks.FUSION_COMPONENT.get()) && variant(state) == 0) {
            return new Conversion(state.setValue(LegacyVariantBlock.VARIANT, 1), List.of(STEEL_CAST_PLATE));
        }
        if (tool == Tool.TORCH && state.is(HbmBlocks.ICF_COMPONENT.get()) && variant(state) == 1) {
            return new Conversion(state.setValue(LegacyVariantBlock.VARIANT, 2), List.of(BISMOID_BRONZE_CAST_PLATE));
        }
        if (tool == Tool.BOLT && state.is(HbmBlocks.WATZ_END.get()) && variant(state) == 0) {
            return new Conversion(state.setValue(LegacyVariantBlock.VARIANT, 1), List.of(DURA_STEEL_BOLT));
        }
        if (tool == Tool.BOLT && state.is(HbmBlocks.ICF_COMPONENT.get()) && variant(state) == 3) {
            return new Conversion(state.setValue(LegacyVariantBlock.VARIANT, 4), List.of(STEEL_CAST_PLATE, DURA_STEEL_BOLT));
        }
        return null;
    }

    private static int variant(BlockState state) {
        return state.hasProperty(LegacyVariantBlock.VARIANT) ? state.getValue(LegacyVariantBlock.VARIANT) : 0;
    }

    private static Requirement material(int... materialIds) {
        return new Requirement(stack -> {
            if (!stack.is(HbmItems.PLATE_CAST.get())) {
                return false;
            }
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int materialId = data.getInt("material_id");
            for (int accepted : materialIds) {
                if (materialId == accepted) {
                    return true;
                }
            }
            return false;
        }, 1);
    }

    private static boolean consume(Player player, List<Requirement> requirements) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        List<ItemStack> inventory = player.getInventory().items;
        int[] remaining = inventory.stream().mapToInt(ItemStack::getCount).toArray();
        for (Requirement requirement : requirements) {
            int needed = requirement.count();
            for (int slot = 0; slot < inventory.size() && needed > 0; slot++) {
                ItemStack stack = inventory.get(slot);
                if (!requirement.matches().test(stack)) {
                    continue;
                }
                int used = Math.min(needed, remaining[slot]);
                remaining[slot] -= used;
                needed -= used;
            }
            if (needed > 0) {
                return false;
            }
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            int consumed = inventory.get(slot).getCount() - remaining[slot];
            if (consumed > 0) {
                inventory.get(slot).shrink(consumed);
            }
        }
        return true;
    }

    private record Requirement(Predicate<ItemStack> matches, int count) {
    }

    private record Conversion(BlockState output, List<Requirement> requirements) {
    }
}
