package com.reinhardt.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings retained from HBM 1.7.10. */
public final class HbmClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_COOLING_TOWER_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_LARGE_MACHINE_OCCLUSION_CULLING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("particles");
        ENABLE_COOLING_TOWER_PARTICLES = builder
                .comment("是否显示冷却塔工作时的水蒸气粒子效果。默认值与 HBM 1.7.10 一致：true。")
                .define("enableCoolingTowerParticles", true);
        builder.pop();
        builder.push("rendering");
        ENABLE_LARGE_MACHINE_OCCLUSION_CULLING = builder
                .comment("使用原版区段遮挡图剔除被地形完全遮挡的大型设备；只在设备完整渲染包围盒覆盖的所有区段均不可见时生效。")
                .define("enableLargeMachineOcclusionCulling", true);
        builder.pop();
        SPEC = builder.build();
    }

    private HbmClientConfig() {
    }
}
