package com.reinhardt.hbm.radiation;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class HbmLivingRadiation {
    public static final float MAX_RADIATION = 2_500.0F;
    public static final float MAX_DIGAMMA = 10.0F;
    private static final ResourceLocation DIGAMMA_HEALTH_MODIFIER = ReinhardtsHBM.id("digamma_health");

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmLivingRadiation> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeNbt(data.save()),
            buffer -> {
                HbmLivingRadiation data = new HbmLivingRadiation();
                data.load(buffer.readNbt());
                return data;
            }
    );

    public static final Codec<HbmLivingRadiation> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmLivingRadiation data = new HbmLivingRadiation();
                data.load(tag);
                return data;
            },
            HbmLivingRadiation::save
    );

    private float radiation;
    private float environmentRadiation;
    private float radiationBuffer;
    private float chunkRadiation;
    private float neutron;
    private float digamma;

    public static HbmLivingRadiation get(LivingEntity entity) {
        return entity.getData(HbmDataAttachments.LIVING_RADIATION);
    }

    public static void set(LivingEntity entity, HbmLivingRadiation data) {
        entity.setData(HbmDataAttachments.LIVING_RADIATION, data);
        applyDigammaModifier(entity, data.getDigamma());
    }

    public float getRadiation() {
        return radiation;
    }

    public void setRadiation(float radiation) {
        this.radiation = clamp(radiation, 0.0F, MAX_RADIATION);
    }

    public void addRadiation(float amount) {
        setRadiation(this.radiation + amount);
    }

    public float getEnvironmentRadiation() {
        return environmentRadiation;
    }

    public void setEnvironmentRadiation(float environmentRadiation) {
        this.environmentRadiation = Math.max(0.0F, environmentRadiation);
    }

    public void addEnvironmentRadiation(float amount) {
        setEnvironmentRadiation(this.environmentRadiation + amount);
    }

    public float getRadiationBuffer() {
        return radiationBuffer;
    }

    public void setRadiationBuffer(float radiationBuffer) {
        this.radiationBuffer = Math.max(0.0F, radiationBuffer);
    }

    public float getChunkRadiation() {
        return chunkRadiation;
    }

    public void setChunkRadiation(float chunkRadiation) {
        this.chunkRadiation = Math.max(0.0F, chunkRadiation);
    }

    public float getNeutron() {
        return neutron;
    }

    public void setNeutron(float neutron) {
        this.neutron = Math.max(0.0F, neutron);
    }

    public float getDigamma() {
        return digamma;
    }

    public void setDigamma(float digamma) {
        this.digamma = clamp(digamma, 0.0F, MAX_DIGAMMA);
    }

    public void addDigamma(float amount) {
        setDigamma(this.digamma + amount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("radiation", radiation);
        tag.putFloat("environment_radiation", environmentRadiation);
        tag.putFloat("radiation_buffer", radiationBuffer);
        tag.putFloat("chunk_radiation", chunkRadiation);
        tag.putFloat("neutron", neutron);
        tag.putFloat("digamma", digamma);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }

        setRadiation(tag.getFloat("radiation"));
        setEnvironmentRadiation(tag.getFloat("environment_radiation"));
        setRadiationBuffer(tag.getFloat("radiation_buffer"));
        setChunkRadiation(tag.getFloat("chunk_radiation"));
        setNeutron(tag.getFloat("neutron"));
        setDigamma(tag.getFloat("digamma"));
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void applyDigammaModifier(LivingEntity entity, float digamma) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        maxHealth.removeModifier(DIGAMMA_HEALTH_MODIFIER);
        if (digamma > 0.0F) {
            double healthModifier = Math.pow(0.5D, digamma) - 1.0D;
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    DIGAMMA_HEALTH_MODIFIER,
                    healthModifier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }
}
