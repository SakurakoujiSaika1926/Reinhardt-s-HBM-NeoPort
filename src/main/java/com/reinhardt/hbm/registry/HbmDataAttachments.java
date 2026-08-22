package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
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
                    .copyOnDeath()
                    .build()
    );
    public static final Supplier<AttachmentType<HbmLivingHazards>> LIVING_HAZARDS = ATTACHMENTS.register(
            "living_hazards",
            () -> AttachmentType.builder(HbmLivingHazards::new)
                    .serialize(HbmLivingHazards.CODEC)
                    .sync(HbmLivingHazards.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    private HbmDataAttachments() {
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENTS.register(eventBus);
    }
}
