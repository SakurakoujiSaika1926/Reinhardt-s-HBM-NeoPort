package com.reinhardt.hbm.radiation;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.network.PlayerInformPayload;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class HbmLivingHazards {
    public static final int MAX_ASBESTOS = 60 * 60 * 20;
    public static final int MAX_BLACK_LUNG = 2 * 60 * 60 * 20;

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmLivingHazards> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeNbt(data.save()),
            buffer -> {
                HbmLivingHazards data = new HbmLivingHazards();
                data.load(buffer.readNbt());
                return data;
            }
    );

    public static final Codec<HbmLivingHazards> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmLivingHazards data = new HbmLivingHazards();
                data.load(tag);
                return data;
            },
            HbmLivingHazards::save
    );

    private int asbestos;
    private int blackLung;
    private int fire;
    private int balefire;
    /** Legacy black-fire timer used by EntityFireLingering type BLACK. */
    private int blackFire;

    public static HbmLivingHazards get(LivingEntity entity) {
        return entity.getData(HbmDataAttachments.LIVING_HAZARDS);
    }

    public static void set(LivingEntity entity, HbmLivingHazards data) {
        entity.setData(HbmDataAttachments.LIVING_HAZARDS, data);
    }

    public static void clear(LivingEntity entity) {
        HbmLivingHazards data = get(entity);
        data.setAsbestos(entity, 0);
        data.setBlackLung(entity, 0);
        data.fire = 0;
        data.balefire = 0;
        data.blackFire = 0;
        set(entity, data);
    }

    public int getAsbestos() {
        return HbmConfig.ENABLE_ASBESTOS.get() ? asbestos : 0;
    }

    public void setAsbestos(LivingEntity entity, int asbestos) {
        if (!HbmConfig.ENABLE_ASBESTOS.get()) {
            this.asbestos = 0;
            return;
        }
        this.asbestos = Math.max(0, asbestos);
        if (this.asbestos >= MAX_ASBESTOS) {
            this.asbestos = 0;
            entity.hurt(entity.damageSources().source(HbmDamageTypes.ASBESTOS), 1000.0F);
        }
    }

    public void addAsbestos(LivingEntity entity, int amount) {
        if (!HbmConfig.ENABLE_ASBESTOS.get()) {
            return;
        }
        setAsbestos(entity, this.asbestos + amount);
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                    120,
                    "info.reinhardtshbm.asbestos",
                    0xFF5555,
                    3000
            ));
        }
    }

    public int getBlackLung() {
        return HbmConfig.ENABLE_COAL_DUST.get() ? blackLung : 0;
    }

    public void setBlackLung(LivingEntity entity, int blackLung) {
        if (!HbmConfig.ENABLE_COAL_DUST.get()) {
            this.blackLung = 0;
            return;
        }
        this.blackLung = Math.max(0, blackLung);
        if (this.blackLung >= MAX_BLACK_LUNG) {
            this.blackLung = 0;
            entity.hurt(entity.damageSources().source(HbmDamageTypes.BLACK_LUNG), 1000.0F);
        }
    }

    public void addBlackLung(LivingEntity entity, int amount) {
        if (!HbmConfig.ENABLE_COAL_DUST.get()) {
            return;
        }
        setBlackLung(entity, this.blackLung + amount);
        if (entity instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(
                    121,
                    "info.reinhardtshbm.coaldust",
                    0xFF5555,
                    3000
            ));
        }
    }

    public int getFire() {
        return fire;
    }

    public void extendFire(int duration) {
        this.fire = Math.max(this.fire, Math.max(0, duration));
    }

    public void clearFire() {
        this.fire = 0;
    }

    public void tickFire() {
        if (this.fire > 0) {
            this.fire--;
        }
    }

    public int getBalefire() {
        return balefire;
    }

    public void extendBalefire(int duration) {
        this.balefire = Math.max(this.balefire, Math.max(0, duration));
    }

    public void tickBalefire() {
        if (this.balefire > 0) {
            this.balefire--;
        }
    }

    public int getBlackFire() {
        return blackFire;
    }

    /** EntityFireLingering's BLACK type starts at 200 ticks, then adds five per hit. */
    public void extendBlackFire() {
        this.blackFire = this.blackFire < 200 ? 200 : this.blackFire + 5;
    }

    public void tickBlackFire() {
        if (this.blackFire > 0) {
            this.blackFire--;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("asbestos", asbestos);
        tag.putInt("black_lung", blackLung);
        tag.putInt("fire", fire);
        tag.putInt("balefire", balefire);
        tag.putInt("black_fire", blackFire);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        this.asbestos = Math.max(0, tag.getInt("asbestos"));
        this.blackLung = Math.max(0, tag.getInt("black_lung"));
        this.fire = Math.max(0, tag.getInt("fire"));
        this.balefire = Math.max(0, tag.getInt("balefire"));
        this.blackFire = Math.max(0, tag.getInt("black_fire"));
    }
}
