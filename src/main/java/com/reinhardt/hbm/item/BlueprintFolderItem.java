package com.reinhardt.hbm.item;

import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 1.7.10 ItemBlueprintFolder: a consumed booklet that rolls one blueprint pool. */
public final class BlueprintFolderItem extends Item {
    private static final int DISCOVER_MODEL_DATA = 1;
    private static final int SECRET_MODEL_DATA = 2;

    public BlueprintFolderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack folder = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(folder, true);
        }

        String prefix = prefix(folder);
        List<String> pools = selectablePools(level, prefix);
        if (pools.isEmpty()) {
            return InteractionResultHolder.pass(folder);
        }

        // The legacy item consumes itself even from the creative inventory.
        folder.shrink(1);
        ItemStack blueprint = BlueprintItem.stackFor(pools.get(player.getRandom().nextInt(pools.size())));
        if (!player.getInventory().add(blueprint)) {
            player.drop(blueprint, false);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(folder, false);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));
        output.accept(stackFor(Variant.DISCOVER));
    }

    public static ItemStack stackFor(Variant variant) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.BLUEPRINT_FOLDER.get());
        if (variant.modelData != 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variant.modelData));
        }
        return stack;
    }

    private static String prefix(ItemStack stack) {
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (modelData != null && modelData.value() == DISCOVER_MODEL_DATA) {
            return BlueprintItem.POOL_PREFIX_DISCOVER;
        }
        if (modelData != null && modelData.value() == SECRET_MODEL_DATA) {
            return BlueprintItem.POOL_PREFIX_SECRET;
        }
        return BlueprintItem.POOL_PREFIX_ALT;
    }

    private static List<String> selectablePools(Level level, String prefix) {
        List<String> pools = new ArrayList<>();
        level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
                .forEach(recipe -> pools.addAll(recipe.value().blueprintPools()));
        level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get())
                .forEach(recipe -> pools.addAll(recipe.value().blueprintPools()));
        level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get())
                .forEach(recipe -> pools.addAll(recipe.value().blueprintPools()));
        level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRECISION_ASSEMBLER.get())
                .forEach(recipe -> pools.addAll(recipe.value().blueprintPools()));
        return pools.stream()
                .filter(pool -> pool.startsWith(prefix))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public enum Variant {
        STANDARD(0),
        DISCOVER(DISCOVER_MODEL_DATA),
        SECRET(SECRET_MODEL_DATA);

        private final int modelData;

        Variant(int modelData) {
            this.modelData = modelData;
        }
    }
}
