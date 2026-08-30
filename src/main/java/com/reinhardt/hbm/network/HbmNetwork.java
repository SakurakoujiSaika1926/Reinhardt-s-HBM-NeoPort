package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HbmNetwork {
    private HbmNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AnnihilatorControlPayload.TYPE, AnnihilatorControlPayload.STREAM_CODEC, AnnihilatorControlPayload::handle);
        registrar.playToServer(SetFluidIdentifierPayload.TYPE, SetFluidIdentifierPayload.STREAM_CODEC, SetFluidIdentifierPayload::handle);
        registrar.playToServer(SettingsToolKeysPayload.TYPE, SettingsToolKeysPayload.STREAM_CODEC, SettingsToolKeysPayload::handle);
        registrar.playToServer(ToggleMagnetPayload.TYPE, ToggleMagnetPayload.STREAM_CODEC, ToggleMagnetPayload::handle);
        registrar.playToServer(ToggleHeaterPayload.TYPE, ToggleHeaterPayload.STREAM_CODEC, ToggleHeaterPayload::handle);
        registrar.playToServer(HeatExchangerControlPayload.TYPE, HeatExchangerControlPayload.STREAM_CODEC, HeatExchangerControlPayload::handle);
        registrar.playToServer(GasTurbineControlPayload.TYPE, GasTurbineControlPayload.STREAM_CODEC, GasTurbineControlPayload::handle);
        registrar.playToServer(WoodBurnerControlPayload.TYPE, WoodBurnerControlPayload.STREAM_CODEC, WoodBurnerControlPayload::handle);
        registrar.playToServer(MicrowaveControlPayload.TYPE, MicrowaveControlPayload.STREAM_CODEC, MicrowaveControlPayload::handle);
        registrar.playToServer(SoyuzLauncherControlPayload.TYPE, SoyuzLauncherControlPayload.STREAM_CODEC, SoyuzLauncherControlPayload::handle);
        registrar.playToServer(CompressorControlPayload.TYPE, CompressorControlPayload.STREAM_CODEC, CompressorControlPayload::handle);
        registrar.playToServer(FunnelControlPayload.TYPE, FunnelControlPayload.STREAM_CODEC, FunnelControlPayload::handle);
        registrar.playToServer(MissileAssemblyControlPayload.TYPE, MissileAssemblyControlPayload.STREAM_CODEC, MissileAssemblyControlPayload::handle);
        registrar.playToServer(RadarControlPayload.TYPE, RadarControlPayload.STREAM_CODEC, RadarControlPayload::handle);
        registrar.playToServer(RadarCommandPayload.TYPE, RadarCommandPayload.STREAM_CODEC, RadarCommandPayload::handle);
        registrar.playToServer(FusionMachineControlPayload.TYPE, FusionMachineControlPayload.STREAM_CODEC, FusionMachineControlPayload::handle);
        registrar.playToServer(MixerControlPayload.TYPE, MixerControlPayload.STREAM_CODEC, MixerControlPayload::handle);
        registrar.playToServer(ResearchReactorControlPayload.TYPE, ResearchReactorControlPayload.STREAM_CODEC, ResearchReactorControlPayload::handle);
        registrar.playToServer(ReactorControlPayload.TYPE, ReactorControlPayload.STREAM_CODEC, ReactorControlPayload::handle);
        registrar.playToServer(RbmkConsoleControlPayload.TYPE, RbmkConsoleControlPayload.STREAM_CODEC, RbmkConsoleControlPayload::handle);
        registrar.playToServer(RbmkControlRodPayload.TYPE, RbmkControlRodPayload.STREAM_CODEC, RbmkControlRodPayload::handle);
        registrar.playToServer(RbmkAutoControlPayload.TYPE, RbmkAutoControlPayload.STREAM_CODEC, RbmkAutoControlPayload::handle);
        registrar.playToServer(RbmkCraneControlPayload.TYPE, RbmkCraneControlPayload.STREAM_CODEC, RbmkCraneControlPayload::handle);
        registrar.playToServer(ZirnoxControlPayload.TYPE, ZirnoxControlPayload.STREAM_CODEC, ZirnoxControlPayload::handle);
        registrar.playToServer(PwrControlPayload.TYPE, PwrControlPayload.STREAM_CODEC, PwrControlPayload::handle);
        registrar.playToServer(ParticleAcceleratorControlPayload.TYPE, ParticleAcceleratorControlPayload.STREAM_CODEC, ParticleAcceleratorControlPayload::handle);
        registrar.playToServer(BatteryReddControlPayload.TYPE, BatteryReddControlPayload.STREAM_CODEC, BatteryReddControlPayload::handle);
        registrar.playToServer(BatterySocketControlPayload.TYPE, BatterySocketControlPayload.STREAM_CODEC, BatterySocketControlPayload::handle);
        registrar.playToServer(DfcControlPayload.TYPE, DfcControlPayload.STREAM_CODEC, DfcControlPayload::handle);
        registrar.playToServer(TurretJeremyControlPayload.TYPE, TurretJeremyControlPayload.STREAM_CODEC, TurretJeremyControlPayload::handle);
        registrar.playToServer(TurretChekhovControlPayload.TYPE, TurretChekhovControlPayload.STREAM_CODEC, TurretChekhovControlPayload::handle);
        registrar.playToServer(LegacyTurretControlPayload.TYPE, LegacyTurretControlPayload.STREAM_CODEC, LegacyTurretControlPayload::handle);
        registrar.playToServer(WandConfigPayload.TYPE, WandConfigPayload.STREAM_CODEC, WandConfigPayload::handle);
        registrar.playToServer(SetRttyPagerChannelPayload.TYPE, SetRttyPagerChannelPayload.STREAM_CODEC, SetRttyPagerChannelPayload::handle);
        registrar.playToServer(RadioRecControlPayload.TYPE, RadioRecControlPayload.STREAM_CODEC, RadioRecControlPayload::handle);
        registrar.playToServer(AutocalControlPayload.TYPE, AutocalControlPayload.STREAM_CODEC, AutocalControlPayload::handle);
        registrar.playToServer(BobmazonOrderPayload.TYPE, BobmazonOrderPayload.STREAM_CODEC, BobmazonOrderPayload::handle);
        registrar.playToClient(PlayerInformPayload.TYPE, PlayerInformPayload.STREAM_CODEC, PlayerInformPayload::handle);
        registrar.playToClient(PollutionSyncPayload.TYPE, PollutionSyncPayload.STREAM_CODEC, PollutionSyncPayload::handle);
        registrar.playToClient(BatteryReddSyncPayload.TYPE, BatteryReddSyncPayload.STREAM_CODEC, BatteryReddSyncPayload::handle);
        registrar.playToClient(LegacyExplosionEffectPayload.TYPE, LegacyExplosionEffectPayload.STREAM_CODEC, LegacyExplosionEffectPayload::handle);
        registrar.playToClient(LegacySmallExplosionEffectPayload.TYPE, LegacySmallExplosionEffectPayload.STREAM_CODEC, LegacySmallExplosionEffectPayload::handle);
        registrar.playToClient(TurretCasingEffectPayload.TYPE, TurretCasingEffectPayload.STREAM_CODEC, TurretCasingEffectPayload::handle);
        registrar.playToClient(TurretMuzzleFlashPayload.TYPE, TurretMuzzleFlashPayload.STREAM_CODEC, TurretMuzzleFlashPayload::handle);
        registrar.playToClient(MaxwellGibEffectPayload.TYPE, MaxwellGibEffectPayload.STREAM_CODEC, MaxwellGibEffectPayload::handle);
        registrar.playToClient(LandmineEffectPayload.TYPE, LandmineEffectPayload.STREAM_CODEC, LandmineEffectPayload::handle);
        registrar.playToClient(SirenSoundPayload.TYPE, SirenSoundPayload.STREAM_CODEC, SirenSoundPayload::handle);
    }
}
