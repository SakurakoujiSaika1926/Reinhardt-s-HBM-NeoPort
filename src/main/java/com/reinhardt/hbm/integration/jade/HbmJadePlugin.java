package com.reinhardt.hbm.integration.jade;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ConcreteColoredBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.item.ConcreteColoredBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class HbmJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineDummyProvider.INSTANCE, MachineDummyBlock.class);
        registration.registerBlockComponent(ConcreteVariantProvider.INSTANCE, ConcreteColoredBlock.class);
    }

    private enum ConcreteVariantProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            if (!(state.getBlock().asItem() instanceof ConcreteColoredBlockItem item)
                    || !state.hasProperty(ConcreteColoredBlock.META)) {
                return;
            }

            tooltip.replace(JadeIds.CORE_OBJECT_NAME, item.variantName(state.getValue(ConcreteColoredBlock.META)));
        }

        @Override
        public ResourceLocation getUid() {
            return ReinhardtsHBM.id("jade_concrete_variant");
        }

        @Override
        public int getDefaultPriority() {
            return -10_090;
        }

        @Override
        public boolean isRequired() {
            return true;
        }
    }

    private enum MachineDummyProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof MachineDummyBlockEntity dummy)) {
                return;
            }

            BlockEntity core = dummy.core();
            if (core == null) {
                return;
            }

            BlockState coreState = accessor.getLevel().getBlockState(core.getBlockPos());
            if (coreState.isAir()) {
                return;
            }

            // Jade already renders the core object name and all storage components.
            // Replace only that name so dummy blocks identify the actual machine.
            tooltip.replace(JadeIds.CORE_OBJECT_NAME, coreState.getBlock().getName());
        }

        @Override
        public ResourceLocation getUid() {
            return ReinhardtsHBM.id("jade_machine_dummy");
        }

        @Override
        public int getDefaultPriority() {
            return -10_090;
        }

        @Override
        public boolean isRequired() {
            return true;
        }
    }
}
