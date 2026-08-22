package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class SirenTrackItem extends Item {
    public enum Playback { LOOP, PASS, SOUND }

    public enum Track {
        NULL("", null, Playback.SOUND, 0, 0),
        HATCH("Hatch Siren", HbmSoundEvents.HATCH_ALARM, Playback.LOOP, 0x3358839, 250),
        AUTOPILOT("Autopilot Disconnected", HbmSoundEvents.SIREN_AUTOPILOT, Playback.LOOP, 0xB5C4B5, 50),
        AMS("AMS Siren", HbmSoundEvents.SIREN_AMS, Playback.LOOP, 0xE5E0B2, 50),
        BLAST_DOOR("Blast Door Alarm", HbmSoundEvents.SIREN_BLAST_DOOR, Playback.LOOP, 0xB1F500, 50),
        APC_LOOP("APC Siren", HbmSoundEvents.SIREN_APC_LOOP, Playback.LOOP, 0x366520, 50),
        KLAXON("Klaxon", HbmSoundEvents.SIREN_KLAXON, Playback.LOOP, 0x808080, 50),
        FO_KLAXON_A("Vault Door Alarm", HbmSoundEvents.SIREN_FO_KLAXON_A, Playback.LOOP, 0x8C810B, 50),
        FO_KLAXON_B("Security Alert", HbmSoundEvents.SIREN_FO_KLAXON_B, Playback.LOOP, 0x76818E, 50),
        REGULAR("Standard Siren", HbmSoundEvents.SIREN_REGULAR, Playback.LOOP, 0x663300, 100),
        CLASSIC("Classic Siren", HbmSoundEvents.SIREN_CLASSIC, Playback.LOOP, 0xC0CFE8, 100),
        BANK("Bank Alarm", HbmSoundEvents.SIREN_BANK, Playback.LOOP, 0x368D62, 100),
        BEEP("Beep Siren", HbmSoundEvents.SIREN_BEEP, Playback.LOOP, 0xD3F3B3, 100),
        CONTAINER("Container Alarm", HbmSoundEvents.SIREN_CONTAINER, Playback.LOOP, 0xE0E2DF, 100),
        SWEEP("Sweep Siren", HbmSoundEvents.SIREN_SWEEP, Playback.LOOP, 0xEDCEFA, 500),
        STRIDER("Missile Silo Siren", HbmSoundEvents.SIREN_STRIDER, Playback.LOOP, 0xAB43AA, 500),
        AIR_RAID("Air Raid Siren", HbmSoundEvents.SIREN_AIR_RAID, Playback.LOOP, 0xDF3795, 500),
        NOSTROMO("Nostromo Self Destruct", HbmSoundEvents.SIREN_NOSTROMO, Playback.LOOP, 0x5DD800, 100),
        EAS("EAS Alarm Screech", HbmSoundEvents.SIREN_EAS, Playback.LOOP, 0xB3A8C1, 50),
        APC_PASS("APC Pass", HbmSoundEvents.SIREN_APC_PASS, Playback.PASS, 0x345F33, 50),
        RAZORTRAIN("Razortrain Horn", HbmSoundEvents.SIREN_RAZORTRAIN, Playback.SOUND, 0x775B6D, 250);

        private final String title;
        @Nullable
        private final net.neoforged.neoforge.registries.DeferredHolder<SoundEvent, SoundEvent> sound;
        private final Playback playback;
        private final int color;
        private final int volume;

        Track(String title, @Nullable net.neoforged.neoforge.registries.DeferredHolder<SoundEvent, SoundEvent> sound,
              Playback playback, int color, int volume) {
            this.title = title;
            this.sound = sound;
            this.playback = playback;
            this.color = color;
            this.volume = volume;
        }

        public static Track fromDamage(int damage) {
            return damage >= 0 && damage < values().length ? values()[damage] : NULL;
        }

        public String title() { return this.title; }
        public int id() { return ordinal(); }
        public boolean valid() { return this != NULL && this.sound != null; }
        public SoundEvent sound() { return this.sound == null ? null : this.sound.get(); }
        public Playback playback() { return this.playback; }
        public int color() { return this.color; }
        public int range() { return this.volume; }
        public float volume() { return this.volume; }
    }

    public SirenTrackItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Track track(ItemStack stack) {
        if (!(stack.getItem() instanceof SirenTrackItem)) {
            return null;
        }
        Track track = Track.fromDamage(stack.getDamageValue());
        return track.valid() ? track : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        Track track = track(stack);
        return Component.translatable("item.reinhardtshbm.siren_track").append(" - ").append(Component.literal(track == null ? "" : track.title()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Track track = track(stack);
        if (track != null) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.siren_track.type", track.playback().name()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.siren_track.volume", track.volume()).withStyle(ChatFormatting.GRAY));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (int damage = 1; damage < Track.values().length; damage++) {
            ItemStack track = new ItemStack(this);
            track.setDamageValue(damage);
            output.accept(track);
        }
    }
}
