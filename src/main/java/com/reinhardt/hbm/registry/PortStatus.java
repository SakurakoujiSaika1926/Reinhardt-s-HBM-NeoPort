package com.reinhardt.hbm.registry;

import org.slf4j.Logger;

public final class PortStatus {
    private PortStatus() {
    }

    public static void logBootstrapSummary(Logger logger) {
        logger.info("Reinhardt's HBM NeoForge port bootstrap started.");
        logger.info("Initial 1.12.2 content slice: {} items, {} blocks, {} creative tabs.",
                HbmItems.ITEMS.getEntries().size(),
                HbmBlocks.BLOCKS.getEntries().size(),
                HbmCreativeTabs.TABS.getEntries().size());
    }
}
