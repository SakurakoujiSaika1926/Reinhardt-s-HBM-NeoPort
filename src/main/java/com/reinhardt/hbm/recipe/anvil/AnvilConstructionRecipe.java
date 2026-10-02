package com.reinhardt.hbm.recipe.anvil;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AnvilConstructionRecipe {
    private final List<AnvilIngredient> inputs;
    private final List<AnvilOutput> outputs;
    private final int tierLower;
    private final int tierUpper;
    private final OverlayType overlay;

    public AnvilConstructionRecipe(List<AnvilIngredient> inputs, List<AnvilOutput> outputs, int tierLower, int tierUpper, OverlayType overlay) {
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.tierLower = tierLower;
        this.tierUpper = tierUpper;
        this.overlay = overlay;
    }

    public static AnvilConstructionRecipe oneToOne(AnvilIngredient input, ItemStack output, int tier) {
        return new AnvilConstructionRecipe(
                List.of(input),
                List.of(new AnvilOutput(output)),
                tier,
                -1,
                OverlayType.SMITHING
        );
    }

    public static AnvilConstructionRecipe construction(List<AnvilIngredient> inputs, ItemStack output, int tier) {
        return new AnvilConstructionRecipe(
                inputs,
                List.of(new AnvilOutput(output)),
                tier,
                -1,
                OverlayType.CONSTRUCTION
        );
    }

    public List<AnvilIngredient> inputs() {
        return this.inputs;
    }

    public List<AnvilOutput> outputs() {
        return this.outputs;
    }

    public int tierLower() {
        return HbmConfig.lbsmUnlockAnvil() ? 1 : this.tierLower;
    }

    public int tierUpper() {
        return HbmConfig.lbsmUnlockAnvil() && this.tierUpper != -1 ? 1 : this.tierUpper;
    }

    public OverlayType overlay() {
        return this.overlay;
    }

    public boolean isTierValid(int tier) {
        if (HbmConfig.lbsmUnlockAnvil()) {
            return this.tierUpper == -1 ? tier >= 1 : tier == 1;
        }
        if (this.tierUpper == -1) {
            return tier >= this.tierLower;
        }
        return tier >= this.tierLower && tier <= this.tierUpper;
    }

    public ItemStack getDisplay() {
        if (this.overlay == OverlayType.RECYCLING && !this.inputs.isEmpty()) {
            return this.inputs.getFirst().displayStack();
        }
        if (this.outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return this.outputs.getFirst().stack().copy();
    }

    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("info.reinhardtshbm.template_in"));
        for (AnvilIngredient input : this.inputs) {
            lines.add(input.describeLine());
        }

        lines.add(Component.empty());
        lines.add(Component.translatable("info.reinhardtshbm.template_out"));
        for (AnvilOutput output : this.outputs) {
            ItemStack stack = output.stack();
            Component line = Component.literal(">" + stack.getCount() + "x ").append(stack.getHoverName());
            if (output.chance() != 1.0F) {
                line = line.copy().append(Component.literal(" (" + (int) (output.chance() * 100.0F) + "%)"));
            }
            lines.add(line);
        }
        return lines;
    }

    public boolean craft(Player player) {
        if (!consumeInputs(player.getInventory().items)) {
            return false;
        }

        boolean firedAchievement = false;
        for (ItemStack stack : rollOutputs(player.getRandom())) {
            if (!firedAchievement) {
                HbmAdvancements.awardForCraftedStack(player, stack);
                firedAchievement = true;
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        return true;
    }

    /**
     * Tests the exact construction/recycling input contract against an arbitrary
     * inventory without mutating it. This is the machine-neutral counterpart to
     * the player-inventory anvil UI.
     */
    public boolean canConsumeInputs(List<ItemStack> stacks) {
        return consumeInputsUnchecked(copyInventory(stacks));
    }

    /**
     * Atomically consumes this recipe from an arbitrary inventory. A failed
     * match leaves every supplied stack untouched, including fluid containers.
     */
    public boolean consumeInputs(List<ItemStack> stacks) {
        List<ItemStack> result = copyInventory(stacks);
        if (!consumeInputsUnchecked(result)) {
            return false;
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack original = stacks.get(slot);
            int consumed = original.getCount() - result.get(slot).getCount();
            if (consumed > 0) {
                original.shrink(consumed);
            }
        }
        return true;
    }

    /** Rolls every probabilistic output independently, matching the manual anvil. */
    public List<ItemStack> rollOutputs(RandomSource random) {
        List<ItemStack> rolled = new ArrayList<>();
        for (AnvilOutput output : this.outputs) {
            if (output.chance() < 1.0F && random.nextFloat() > output.chance()) {
                continue;
            }
            rolled.add(output.stack().copy());
        }
        return rolled;
    }

    private boolean consumeInputsUnchecked(List<ItemStack> stacks) {
        for (AnvilIngredient input : this.inputs) {
            if (!input.consumeFrom(stacks)) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }
        return copy;
    }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }
}
