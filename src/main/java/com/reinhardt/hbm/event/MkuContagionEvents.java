package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.integration.touhoulittlemaid.HbmTouhouLittleMaidCompat;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Complete latest-1.7.10 MKU contagion state machine. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class MkuContagionEvents {
    public static final int DURATION = 3 * 60 * 60 * 20;
    private static final String CONTAMINATION_KEY = "ntmContagion";
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private MkuContagionEvents() {
    }

    public static void infect(LivingEntity living) {
        living.setData(HbmDataAttachments.MKU_CONTAGION, DURATION);
    }

    public static int contagion(LivingEntity living) {
        return living.getData(HbmDataAttachments.MKU_CONTAGION);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living)
                || living.level().isClientSide
                || !living.isAlive()) {
            return;
        }
        tick((ServerLevel) living.level(), living);
    }

    private static void tick(ServerLevel level, LivingEntity living) {
        int contagion = contagion(living);

        if (living instanceof Player player) {
            ItemStack selected = player.getInventory().getItem(living.getRandom().nextInt(36));
            if (living.getRandom().nextInt(100) == 0) {
                selected = player.getItemBySlot(ARMOR[living.getRandom().nextInt(ARMOR.length)]);
            }
            if (!selected.isEmpty() && selected.getMaxStackSize() == 1) {
                if (contagion > 0) {
                    markContaminated(selected);
                } else if (isContaminated(selected) && !hasBacterialIsolation(player)) {
                    infect(player);
                }
            }
        }

        if (contagion <= 0) {
            return;
        }

        living.setData(HbmDataAttachments.MKU_CONTAGION, contagion - 1);
        int minute = 60 * 20;
        int hour = 60 * minute;

        if (contagion < 2 * hour + 55 * minute && contagion % 20 == 0) {
            double range = living.isInWaterOrRain() ? 16.0D : 2.0D;
            for (Entity nearby : level.getEntities(living, living.getBoundingBox().inflate(range))) {
                if (nearby instanceof LivingEntity target
                        && contagion(target) <= 0
                        && !hasBacterialIsolation(target)) {
                    infect(target);
                }
                if (nearby instanceof ItemEntity item) {
                    ItemStack stack = item.getItem();
                    markContaminated(stack);
                    item.setItem(stack);
                }
            }
        }

        if (contagion < 2 * hour && living.getRandom().nextInt(1000) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0));
        }
        if (contagion < hour && living.getRandom().nextInt(100) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 4));
        }
        if (contagion < 30 * minute && living.getRandom().nextInt(400) == 0) {
            living.hurt(living.damageSources().source(HbmDamageTypes.MKU), 1.0F);
        }
        if (contagion < 5 * minute && living.getRandom().nextInt(100) == 0) {
            living.hurt(living.damageSources().source(HbmDamageTypes.MKU), 2.0F);
        }

        int vomitCycle = (contagion + living.getId()) % 200;
        if (contagion < 30 * minute && vomitCycle < 20 && canVomit(living)) {
            spawnBloodVomit(level, living, 25);
            if (vomitCycle == 19) {
                level.playSound(null, living.getX(), living.getY(), living.getZ(),
                        HbmSoundEvents.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }

        // The latest 1.7.10 source checks the pre-decrement local value here.
        // Keeping that observable order is intentional; terminal harm normally
        // comes from the increasingly frequent MKU damage in the last minutes.
        if (contagion == 0) {
            living.hurt(living.damageSources().source(HbmDamageTypes.MKU), 1000.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide
                && contagion(event.getEntity()) > 0
                && event.getAmount() < 100.0F) {
            event.setAmount(event.getAmount() * 2.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide || contagion(event.getEntity()) <= 0) {
            return;
        }
        for (ItemEntity item : event.getDrops()) {
            ItemStack stack = item.getItem();
            markContaminated(stack);
            item.setItem(stack);
        }
    }

    private static boolean hasBacterialIsolation(LivingEntity living) {
        return HbmArmorProtection.hasLegacyHazmatProtection(living)
                && HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.BACTERIA);
    }

    private static boolean canVomit(LivingEntity living) {
        return living instanceof Player || HbmTouhouLittleMaidCompat.isMaid(living);
    }

    private static void spawnBloodVomit(ServerLevel level, LivingEntity living, int count) {
        Vec3 look = living.getLookAngle();
        double x = living.getX();
        double y = living.getY() + living.getEyeHeight() - (living instanceof Player ? 0.5D : 0.0D);
        double z = living.getZ();
        for (int i = 0; i < count; i++) {
            level.sendParticles(HbmParticleTypes.BLOOD_VOMIT.get(), x, y, z, 0,
                    (look.x + level.random.nextGaussian() * 0.2D) * 0.2D,
                    (look.y + level.random.nextGaussian() * 0.2D) * 0.2D,
                    (look.z + level.random.nextGaussian() * 0.2D) * 0.2D,
                    1.0D);
        }
    }

    private static boolean isContaminated(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(CONTAMINATION_KEY);
    }

    private static void markContaminated(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putBoolean(CONTAMINATION_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
