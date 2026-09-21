package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.integration.tacz.TaczAmmoAssemblyRecipes;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TaczAmmoAssemblerBlockEntity extends AssemblyMachineBlockEntity {
    public TaczAmmoAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.tacz_ammo_assembler");
    }

    @Override
    protected List<RecipeHolder<AssemblyMachineRecipe>> activeVisibleRecipes(Level level) {
        return TaczAmmoAssemblyRecipes.available(level);
    }
}
