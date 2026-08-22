package com.reinhardt.hbm.client;

import com.reinhardt.hbm.client.screen.WandConfigScreens;
import net.minecraft.core.BlockPos;

public final class WandClientHooks {
    private WandClientHooks() {
    }

    public static void openStructure(BlockPos pos, boolean load) {
        WandConfigScreens.openStructure(pos, load);
    }

    public static void openJigsaw(BlockPos pos) {
        WandConfigScreens.openJigsaw(pos);
    }

    public static void openTandem(BlockPos pos) {
        WandConfigScreens.openTandem(pos);
    }
}
