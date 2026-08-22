package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockOreBlockEntity extends BlockEntity {
    private ItemStack resource = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
    private HbmFluidStack acidRequirement = HbmFluidStack.EMPTY;
    private int tier = 1;
    private int color = 0xD78A16;
    private int shape;

    public BedrockOreBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BEDROCK_ORE.get(), pos, blockState);
    }

    public ItemStack resource() {
        return this.resource;
    }

    public HbmFluidStack acidRequirement() {
        return this.acidRequirement;
    }

    public int tier() {
        return this.tier;
    }

    public int color() {
        return this.color;
    }

    public int shape() {
        return this.shape;
    }

    public void configure(ItemStack resource, HbmFluidStack acidRequirement, int color, int tier, int shape) {
        this.resource = resource.isEmpty() ? this.resource : resource.copy();
        this.acidRequirement = acidRequirement == null ? HbmFluidStack.EMPTY : acidRequirement;
        this.color = color == 0 ? this.color : color;
        this.tier = Math.max(1, tier);
        this.shape = Math.floorMod(shape, 10);
        setChanged();
    }

    public void setResource(ItemStack resource) {
        configure(resource, this.acidRequirement, this.color, this.tier, this.shape + 1);
    }

    public void setAcidRequirement(HbmFluidStack acidRequirement) {
        configure(this.resource, acidRequirement, this.color, this.tier, this.shape);
    }

    public void setTier(int tier) {
        configure(this.resource, this.acidRequirement, this.color, tier, this.shape);
    }

    public Component statusLine() {
        Component fluid = this.acidRequirement.isEmpty()
                ? Component.translatable("message.reinhardtshbm.bedrock_ore.no_fluid")
                : Component.translatable(
                        "message.reinhardtshbm.bedrock_ore.fluid_requirement",
                        this.acidRequirement.amount(),
                        Component.translatable(this.acidRequirement.type().translationKey())
                );
        return Component.translatable("message.reinhardtshbm.bedrock_ore.status", this.resource.getHoverName(), this.tier, fluid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Resource", this.resource.saveOptional(registries));
        tag.put("Acid", this.acidRequirement.save());
        tag.putInt("Tier", this.tier);
        tag.putInt("Color", this.color);
        tag.putInt("Shape", this.shape);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.resource = ItemStack.parseOptional(registries, tag.getCompound("Resource"));
        if (this.resource.isEmpty()) {
            this.resource = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
        }
        this.acidRequirement = HbmFluidStack.load(tag.getCompound("Acid"));
        this.tier = Math.max(1, tag.getInt("Tier"));
        this.color = tag.contains("Color") ? tag.getInt("Color") : 0xD78A16;
        this.shape = Math.floorMod(tag.getInt("Shape"), 10);
    }
}
