package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.item.HbmPlayerShield;
import com.reinhardt.hbm.player.HbmPlayerArmorState;
import com.reinhardt.hbm.player.HbmLegacyMobSpawnState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class HbmDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ReinhardtsHBM.MOD_ID);

    public static final Supplier<AttachmentType<HbmLivingRadiation>> LIVING_RADIATION = ATTACHMENTS.register(
            "living_radiation",
            () -> AttachmentType.builder(HbmLivingRadiation::new)
                    .serialize(HbmLivingRadiation.CODEC)
                    .sync(HbmLivingRadiation.STREAM_CODEC)
                    // Deliberately not copyOnDeath: the 1.7.10 death hook
                    // reset radiation, and carrying it into a respawn would
                    // re-enter the lethal threshold immediately.
                    .build()
    );
    public static final Supplier<AttachmentType<HbmLivingHazards>> LIVING_HAZARDS = ATTACHMENTS.register(
            "living_hazards",
            () -> AttachmentType.builder(HbmLivingHazards::new)
                    .serialize(HbmLivingHazards.CODEC)
                    .sync(HbmLivingHazards.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<HbmPlayerShield>> PLAYER_SHIELD = ATTACHMENTS.register(
            "player_shield",
            () -> AttachmentType.builder(HbmPlayerShield::new)
                    .serialize(HbmPlayerShield.CODEC)
                    .sync(HbmPlayerShield.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );
    public static final Supplier<AttachmentType<HbmPlayerArmorState>> PLAYER_ARMOR_STATE = ATTACHMENTS.register(
            "player_armor_state",
            () -> AttachmentType.builder(HbmPlayerArmorState::new)
                    .serialize(HbmPlayerArmorState.CODEC)
                    .sync(HbmPlayerArmorState.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );
    public static final Supplier<AttachmentType<HbmLegacyMobSpawnState>> LEGACY_MOB_SPAWN_STATE = ATTACHMENTS.register(
            "legacy_mob_spawn_state",
            () -> AttachmentType.builder(() -> HbmLegacyMobSpawnState.EMPTY)
                    .serialize(HbmLegacyMobSpawnState.CODEC)
                    .copyOnDeath()
                    .build()
    );

    private HbmDataAttachments() {
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
