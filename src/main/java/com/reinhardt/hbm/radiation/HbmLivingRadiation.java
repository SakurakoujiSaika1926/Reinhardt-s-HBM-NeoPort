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
    private static final int SYNC_INTERVAL_TICKS = 5;
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
    private boolean dirty;
    private int syncCooldown;
    private float appliedDigamma = Float.NaN;

    public static HbmLivingRadiation get(LivingEntity entity) {
        return entity.getData(HbmDataAttachments.LIVING_RADIATION);
    }

    /**
     * Clears all transient radiation state after a real death or a revive.
     *
     * The 1.7.10 implementation explicitly reset the accumulated radiation
     * from its LivingDeathEvent.  Keeping the reset in one place is important
     * here because the attachment also carries the environment buffer,
     * neutron dose and digamma health modifier; leaving any of those behind
     * can re-trigger a lethal check on a dead/revived player.
     */
    public static void clear(LivingEntity entity) {
        HbmLivingRadiation data = entity.getExistingDataOrNull(HbmDataAttachments.LIVING_RADIATION);
        if (data == null) {
            return;
        }
        data.setRadiation(0.0F);
        data.setEnvironmentRadiation(0.0F);
        data.setRadiationBuffer(0.0F);
        data.setChunkRadiation(0.0F);
        data.setNeutron(0.0F);
        data.setDigamma(0.0F);
        set(entity, data);
    }

    public static void set(LivingEntity entity, HbmLivingRadiation data) {
        HbmLivingRadiation existing = entity.getExistingDataOrNull(HbmDataAttachments.LIVING_RADIATION);
        if (existing != data) {
            entity.setData(HbmDataAttachments.LIVING_RADIATION, data);
            data.dirty = false;
            data.syncCooldown = SYNC_INTERVAL_TICKS;
        } else if (data.dirty) {
            if (--data.syncCooldown <= 0) {
                entity.syncData(HbmDataAttachments.LIVING_RADIATION);
                data.dirty = false;
                data.syncCooldown = SYNC_INTERVAL_TICKS;
            }
        } else if (data.syncCooldown > 0) {
            data.syncCooldown--;
        }
        if (Float.compare(data.appliedDigamma, data.getDigamma()) != 0) {
            applyDigammaModifier(entity, data.getDigamma());
            data.appliedDigamma = data.getDigamma();
        }
    }

    public float getRadiation() {
        return radiation;
    }

    public void setRadiation(float radiation) {
        float clamped = clamp(radiation, 0.0F, MAX_RADIATION);
        if (Float.compare(this.radiation, clamped) != 0) {
            this.radiation = clamped;
            dirty = true;
        }
    }

    public void addRadiation(float amount) {
        setRadiation(this.radiation + amount);
    }

    /**
     * Adds body dose and records the same positive amount in the short-lived
     * received-dose buffer used by Geiger/dosimeter readouts.
     *
     * <p>Some legacy contact sources directly mutate body radiation instead of
     * going through the central contamination helper.  Keep {@link
     * #addRadiation(float)} raw for command/cleanup/internal callers, and use
     * this method only for actual incoming dose so the UI cannot report
     * 0 RAD/s while the body counter is still rising.</p>
     */
    public void addRadiationWithReadout(float amount) {
        if (amount > 0.0F) {
            addEnvironmentRadiation(amount);
        }
        addRadiation(amount);
    }

    public float getEnvironmentRadiation() {
        return environmentRadiation;
    }

    public void setEnvironmentRadiation(float environmentRadiation) {
        float clamped = Math.max(0.0F, environmentRadiation);
        if (Float.compare(this.environmentRadiation, clamped) != 0) {
            this.environmentRadiation = clamped;
            dirty = true;
        }
    }

    public void addEnvironmentRadiation(float amount) {
        setEnvironmentRadiation(this.environmentRadiation + amount);
    }

    public float getRadiationBuffer() {
        return radiationBuffer;
    }

    public void setRadiationBuffer(float radiationBuffer) {
        float clamped = Math.max(0.0F, radiationBuffer);
        if (Float.compare(this.radiationBuffer, clamped) != 0) {
            this.radiationBuffer = clamped;
            dirty = true;
        }
    }

    public float getChunkRadiation() {
        return chunkRadiation;
    }

    public void setChunkRadiation(float chunkRadiation) {
        float clamped = Math.max(0.0F, chunkRadiation);
        if (Float.compare(this.chunkRadiation, clamped) != 0) {
            this.chunkRadiation = clamped;
            dirty = true;
        }
    }

    public float getNeutron() {
        return neutron;
    }

    public void setNeutron(float neutron) {
        float clamped = Math.max(0.0F, neutron);
        if (Float.compare(this.neutron, clamped) != 0) {
            this.neutron = clamped;
            dirty = true;
        }
    }

    public float getDigamma() {
        return digamma;
    }

    public void setDigamma(float digamma) {
        float clamped = clamp(digamma, 0.0F, MAX_DIGAMMA);
        if (Float.compare(this.digamma, clamped) != 0) {
            this.digamma = clamped;
            dirty = true;
        }
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
        dirty = false;
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

        AttributeModifier existing = maxHealth.getModifier(DIGAMMA_HEALTH_MODIFIER);
        if (digamma <= 0.0F) {
            if (existing != null) {
                maxHealth.removeModifier(DIGAMMA_HEALTH_MODIFIER);
            }
        } else {
            double healthModifier = Math.pow(0.5D, digamma) - 1.0D;
            if (existing == null || Double.compare(existing.amount(), healthModifier) != 0) {
                maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                        DIGAMMA_HEALTH_MODIFIER,
                        healthModifier,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
        }
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }
}
