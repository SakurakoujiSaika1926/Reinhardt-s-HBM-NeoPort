package com.reinhardt.hbm.api.entity;

public interface LegacyTurretMachineTarget {
    TargetKind turretTargetKind();

    enum TargetKind {
        MISSILE,
        RAILCAR,
        BOMBER
    }
}
