package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum LegacyTurretType {
    FRIENDLY("turret_friendly", Layout.NT, "textures/gui/weapon/gui_turret_friendly.png", AmmoKind.R556, false, true,
            10_000L, 100L, 1.5D, 3.5D, 32D, 3D, 4.5D, 3D, 45D, 30D, 15D, 5, 10),
    FRITZ("turret_fritz", Layout.NT, "textures/gui/weapon/gui_turret_fritz.png", AmmoKind.FLUID, false, true,
            10_000L, 100L, 1.5D, 2.25D, 48D, 2D, 4.5D, 3D, 45D, 30D, 15D, 1, 10),
    HOWARD("turret_howard", Layout.NT, "textures/gui/weapon/gui_turret_howard.png", AmmoKind.DGK, false, true,
            50_000L, 500L, 2.25D, 3.25D, 250D, 3D, 12D, 8D, 90D, 50D, 5D, 1, 10),
    HOWARD_DAMAGED("turret_howard_damaged", Layout.NT, null, AmmoKind.NONE, true, false,
            50_000L, 500L, 2.25D, 3.25D, 16D, 5D, 3D, 2D, 90D, 50D, 5D, 4, 10),
    MAXWELL("turret_maxwell", Layout.NT, "textures/gui/weapon/gui_turret_maxwell.png", AmmoKind.UPGRADE, false, true,
            10_000_000L, 10_000L, 2.0D, 2.125D, 64D, 5D, 9D, 6D, 40D, 35D, 2D, 1, 10),
    RICHARD("turret_richard", Layout.NT, "textures/gui/weapon/gui_turret_richard.png", AmmoKind.ROCKET_ML, false, true,
            10_000L, 100L, 1.5D, 1.25D, 64D, 8D, 4.5D, 3D, 25D, 25D, 5D, 10, 10),
    TAUON("turret_tauon", Layout.NT, "textures/gui/weapon/gui_turret_tau.png", AmmoKind.TAU, false, true,
            100_000L, 1_000L, 1.5D, 1.9375D, 128D, 3D, 9D, 6D, 35D, 35D, 5D, 5, 10),
    ARTY("turret_arty", Layout.ARTILLERY, "textures/gui/weapon/gui_turret_arty.png", AmmoKind.ARTY, false, true,
            100_000L, 100L, 3.0D, 9.0D, 3000D, 250D, 1D, 0.5D, 90D, 30D, 0D, 300, 200),
    HIMARS("turret_himars", Layout.ARTILLERY, "textures/gui/weapon/gui_turret_himars.png", AmmoKind.HIMARS, false, true,
            1_000_000L, 100L, 5.0D, 0.5D, 5000D, 250D, 1D, 0.5D, 30D, 30D, 5D, 40, 10),
    SENTRY("turret_sentry", Layout.SENTRY, "textures/gui/weapon/gui_turret_sentry.png", AmmoKind.P9, false, true,
            1_000L, 5L, 1.5D, 1.25D, 24D, 2D, 4.5D, 3D, 20D, 20D, 15D, 10, 10),
    SENTRY_DAMAGED("turret_sentry_damaged", Layout.SENTRY, null, AmmoKind.NONE, true, false,
            1_000L, 5L, 1.5D, 1.25D, 24D, 2D, 3D, 2D, 20D, 20D, 15D, 10, 10);

    private final String id;
    private final Layout layout;
    private final String guiTexture;
    private final AmmoKind ammoKind;
    private final boolean damaged;
    private final boolean hasMenu;
    private final long maxPower;
    private final long consumption;
    private final double heightOffset;
    private final double barrelLength;
    private final double detectorRange;
    private final double detectorGrace;
    private final double yawSpeed;
    private final double pitchSpeed;
    private final double elevation;
    private final double depression;
    private final double acceptableInaccuracy;
    private final int fireDelay;
    private final int detectorInterval;

    LegacyTurretType(
            String id,
            Layout layout,
            String guiTexture,
            AmmoKind ammoKind,
            boolean damaged,
            boolean hasMenu,
            long maxPower,
            long consumption,
            double heightOffset,
            double barrelLength,
            double detectorRange,
            double detectorGrace,
            double yawSpeed,
            double pitchSpeed,
            double elevation,
            double depression,
            double acceptableInaccuracy,
            int fireDelay,
            int detectorInterval
    ) {
        this.id = id;
        this.layout = layout;
        this.guiTexture = guiTexture;
        this.ammoKind = ammoKind;
        this.damaged = damaged;
        this.hasMenu = hasMenu;
        this.maxPower = maxPower;
        this.consumption = consumption;
        this.heightOffset = heightOffset;
        this.barrelLength = barrelLength;
        this.detectorRange = detectorRange;
        this.detectorGrace = detectorGrace;
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
        this.elevation = elevation;
        this.depression = depression;
        this.acceptableInaccuracy = acceptableInaccuracy;
        this.fireDelay = fireDelay;
        this.detectorInterval = detectorInterval;
    }

    public String id() {
        return id;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Layout layout() {
        return layout;
    }

    public String guiTexture() {
        return guiTexture;
    }

    public AmmoKind ammoKind() {
        return ammoKind;
    }

    public boolean damaged() {
        return damaged;
    }

    public boolean hasMenu() {
        return hasMenu;
    }

    public int placeOffset() {
        return layout == Layout.ARTILLERY ? 1 : 0;
    }

    public boolean hasDummies() {
        return layout != Layout.SENTRY;
    }

    public long maxPower() {
        return maxPower;
    }

    public long consumption() {
        return consumption;
    }

    public double heightOffset() {
        return heightOffset;
    }

    public double barrelLength() {
        return barrelLength;
    }

    public double detectorRange(int greenUpgradeLevel, int mode) {
        if (this == ARTY) {
            return mode == 1 ? 250.0D : 3000.0D;
        }
        return this == MAXWELL ? detectorRange + greenUpgradeLevel * 3.0D : detectorRange;
    }

    public double detectorGrace(int mode) {
        return this == ARTY && mode == 1 ? 32.0D : detectorGrace;
    }

    public double yawSpeed() {
        return yawSpeed;
    }

    public double pitchSpeed() {
        return pitchSpeed;
    }

    public double elevation() {
        return elevation;
    }

    public double depression() {
        return depression;
    }

    public double acceptableInaccuracy() {
        return acceptableInaccuracy;
    }

    public int fireDelay(int mode) {
        if (this == ARTY) {
            return mode == 0 ? 300 : 40;
        }
        return fireDelay;
    }

    public int detectorInterval(int mode) {
        if (this == ARTY) {
            return mode == 1 ? 20 : 200;
        }
        return detectorInterval;
    }

    public boolean acceptsAmmo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (ammoKind) {
            case R556 -> com.reinhardt.hbm.item.StandardAmmoItem.standardType(stack).family() == com.reinhardt.hbm.item.StandardAmmoItem.AmmoFamily.R556;
            case P9 -> com.reinhardt.hbm.item.StandardAmmoItem.standardType(stack).family() == com.reinhardt.hbm.item.StandardAmmoItem.AmmoFamily.P9;
            case TAU -> com.reinhardt.hbm.item.StandardAmmoItem.standardType(stack) == com.reinhardt.hbm.item.StandardAmmoItem.StandardAmmoType.TAU_URANIUM;
            case ROCKET_ML -> com.reinhardt.hbm.item.StandardAmmoItem.standardType(stack).family() == com.reinhardt.hbm.item.StandardAmmoItem.AmmoFamily.ROCKET_ML;
            case DGK -> stack.is(HbmItems.AMMO_DGK.get());
            case ARTY -> stack.is(HbmItems.AMMO_ARTY.get());
            case HIMARS -> stack.is(HbmItems.AMMO_HIMARS.get());
            case FLUID -> stack.is(HbmItems.AMMO_STANDARD.get()) || com.reinhardt.hbm.util.HbmFluidContainerTransfer.canDrainIntoTank(stack, new com.reinhardt.hbm.fluid.HbmFluidTank(1), fluid -> true, ignored -> true);
            case UPGRADE -> {
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                yield key != null && key.getNamespace().equals(com.reinhardt.hbm.ReinhardtsHBM.MOD_ID) && isMaxwellUpgrade(key.getPath());
            }
            case NONE -> false;
        };
    }

    private static boolean isMaxwellUpgrade(String path) {
        return path.equals("upgrade_5g")
                || path.equals("upgrade_screm")
                || path.matches("upgrade_(speed|effect|power|afterburn|overdrive)_[123]");
    }

    public enum Layout {
        NT,
        ARTILLERY,
        SENTRY
    }

    public enum AmmoKind {
        NONE,
        R556,
        P9,
        TAU,
        ROCKET_ML,
        DGK,
        ARTY,
        HIMARS,
        FLUID,
        UPGRADE
    }
}
