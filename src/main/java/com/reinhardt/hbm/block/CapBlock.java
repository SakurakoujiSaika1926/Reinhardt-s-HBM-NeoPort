package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.CapBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Exact 1.7.10 bottle-cap block variants and their 128-cap recovery. */
public final class CapBlock extends Block {
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    public CapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, Type.NUKA));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item cap = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(state.getValue(TYPE).capItemId()));
        return cap == net.minecraft.world.item.Items.AIR ? List.of() : List.of(new ItemStack(cap, 128));
    }

    private ItemStack stackForState(BlockState state) {
        if (this.asItem() instanceof CapBlockItem item) {
            return CapBlockItem.stackFor(item, state.getValue(TYPE));
        }
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public enum Type implements StringRepresentable {
        NUKA("nuka", "cap_nuka"),
        QUANTUM("quantum", "cap_quantum"),
        SPARKLE("sparkle", "cap_sparkle"),
        RAD("rad", "cap_rad"),
        KORL("korl", "cap_korl"),
        FRITZ("fritz", "cap_fritz");

        private final String id;
        private final String capItemId;

        Type(String id, String capItemId) {
            this.id = id;
            this.capItemId = capItemId;
        }

        public String id() {
            return this.id;
        }

        public String capItemId() {
            return this.capItemId;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public static Type byId(String id) {
            for (Type value : values()) {
                if (value.id.equalsIgnoreCase(id)) {
                    return value;
                }
            }
            return NUKA;
        }

        public static Type byModelData(int value) {
            Type[] values = values();
            return value >= 0 && value < values.length ? values[value] : NUKA;
        }
    }
}
