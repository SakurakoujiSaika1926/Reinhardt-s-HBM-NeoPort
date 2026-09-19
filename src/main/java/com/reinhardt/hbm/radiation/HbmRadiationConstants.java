package com.reinhardt.hbm.radiation;

final class HbmRadiationConstants {
    static final int HAZARD_RATE_TICKS = 5;
    /** Legacy radiation diffusion runs once per second (20 game ticks). */
    static final int RAD_SOLVE_INTERVAL_TICKS = 20;
    static final double RAD_EPSILON = 1.0E-5D;
    static final double CHUNK_RADIATION_MAX = 25_000_000.0D;

    static final float CO60 = 30.0F;
    static final float SR90 = 15.0F;
    static final float TC99 = 2.75F;
    static final float I131 = 150.0F;
    static final float XE135 = 1250.0F;
    static final float CS137 = 20.0F;
    static final float AU198 = 500.0F;
    static final float PB209 = 10000.0F;
    static final float AT209 = 7500.0F;
    static final float PO210 = 75.0F;
    static final float RA226 = 7.5F;
    static final float AC227 = 30.0F;
    static final float TH232 = 0.1F;
    static final float THF = 1.75F;
    static final float U = 0.35F;
    static final float U233 = 5.0F;
    static final float U235 = 1.0F;
    static final float U238 = 0.25F;
    static final float UF = 0.5F;
    static final float UZH = 0.125F;
    static final float NP237 = 2.5F;
    static final float NPF = 1.5F;
    static final float PU = 7.5F;
    static final float PURG = 6.25F;
    static final float PU238 = 10.0F;
    static final float PU239 = 5.0F;
    static final float PU240 = 7.5F;
    static final float PU241 = 25.0F;
    static final float PUF = 4.25F;
    static final float AM241 = 8.5F;
    static final float AM242 = 9.5F;
    static final float AMRG = 9.0F;
    static final float AMF = 4.75F;
    static final float MOX = 2.5F;
    static final float SA326 = 15.0F;
    static final float SA327 = 17.5F;
    static final float SRN = SA326 * 0.1F;
    static final float SBD = SA326 * 0.1F;
    static final float SAF = 5.85F;
    static final float SAS3 = 5.0F;
    static final float GH336 = 5.0F;
    static final float MUD = 1.0F;
    static final float WST = 15.0F;
    static final float WSTV = 7.5F;
    static final float YC = U;
    static final float TRX = 25.0F;
    static final float TRN = 0.1F;
    static final float FO = 10.0F;
    static final float BF = 300_000.0F;

    static final float NUGGET = 0.1F;
    static final float INGOT = 1.0F;
    static final float GEM = 1.0F;
    static final float POWDER = 3.0F;
    static final float POWDER_TINY = NUGGET * POWDER;
    static final float ORE = INGOT;
    static final float BLOCK = 10.0F;
    static final float CRYSTAL = BLOCK;
    static final float BILLET = 0.5F;
    static final float PLATE = INGOT;
    static final float WIRE = 9.0F / 72.0F;
    static final float BOLT = 9.0F / 72.0F;
    static final float DENSE_WIRE = INGOT;
    static final float PLATE_CAST = PLATE * 3.0F;
    static final float PLATE_WELDED = PLATE * 6.0F;
    static final float PIPE = PLATE * 3.0F;
    static final float SHELL = PLATE * 4.0F;
    static final float RTG = BILLET * 3.0F;

    private HbmRadiationConstants() {
    }
}
