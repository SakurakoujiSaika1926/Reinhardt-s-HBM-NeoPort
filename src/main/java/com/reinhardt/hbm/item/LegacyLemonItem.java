package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Direct port of the registered 1.7.10 ItemLemon entries. */
public final class LegacyLemonItem extends Item {
    private record Definition(int nutrition, float saturation, boolean meat, boolean alwaysEdible) {
    }

    private static final Map<String, Definition> DEFINITIONS = Map.ofEntries(
            Map.entry("ingot_semtex", new Definition(4, 5.0F, true, false)),
            Map.entry("ingot_c4", new Definition(4, 5.0F, true, false)),
            Map.entry("powder_cement", new Definition(2, 0.5F, false, false)),
            Map.entry("bio_wafer", new Definition(4, 2.0F, false, false)),
            Map.entry("lemon", new Definition(3, 0.5F, false, false)),
            Map.entry("definitelyfood", new Definition(3, 0.5F, false, false)),
            Map.entry("med_ipecac", new Definition(0, 0.0F, false, true)),
            Map.entry("med_ptsd", new Definition(0, 0.0F, false, true)),
            Map.entry("med_schizophrenia", new Definition(0, 0.0F, false, false)),
            Map.entry("loops", new Definition(4, 0.25F, false, false)),
            Map.entry("loop_stew", new Definition(10, 0.5F, false, false)),
            Map.entry("spongebob_macaroni", new Definition(5, 1.0F, false, false)),
            Map.entry("fooditem", new Definition(2, 5.0F, false, false)),
            Map.entry("twinkie", new Definition(3, 0.25F, false, false)),
            Map.entry("static_sandwich", new Definition(6, 1.0F, false, false)),
            Map.entry("pudding", new Definition(6, 1.0F, false, false)),
            Map.entry("nugget", new Definition(200, 1.0F, false, false)),
            Map.entry("cheese", new Definition(5, 0.75F, false, false)),
            Map.entry("cheese_quesadilla", new Definition(8, 1.0F, false, false)),
            Map.entry("glyphid_meat", new Definition(3, 0.5F, true, false)),
            Map.entry("glyphid_meat_grilled", new Definition(8, 0.75F, true, false))
    );

    private final String id;

    private LegacyLemonItem(String id, Properties properties) {
        super(properties.food(food(id)));
        this.id = id;
    }

    public static LegacyLemonItem fromLegacyId(String id) {
        if (!DEFINITIONS.containsKey(id)) {
            throw new IllegalArgumentException("Unknown 1.7.10 ItemLemon id: " + id);
        }
        Properties properties = new Properties();
        if (id.equals("loop_stew")) {
            properties.stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BOWL);
        }
        return new LegacyLemonItem(id, properties);
    }

    public static boolean isLegacyLemon(String id) {
        return DEFINITIONS.containsKey(id);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player) || level.isClientSide) {
            return result;
        }

        if (id.equals("med_ipecac") || id.equals("med_ptsd")) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 50, 49));
            spawnVomit((ServerLevel) level, player, HbmParticleTypes.VOMIT.get(), 1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (id.equals("loop_stew")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 2));
        } else if (id.equals("glyphid_meat_grilled")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 180, 1));
        }
        return result;
    }

    private static net.minecraft.world.food.FoodProperties food(String id) {
        Definition definition = DEFINITIONS.get(id);
        net.minecraft.world.food.FoodProperties.Builder builder = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(definition.nutrition())
                .saturationModifier(definition.saturation());
        if (definition.alwaysEdible()) {
            builder.alwaysEdible();
        }
        return builder.build();
    }

    private static void spawnVomit(ServerLevel level, LivingEntity entity, ParticleOptions particle, int count) {
        Vec3 look = entity.getLookAngle();
        double y = entity.getY() + entity.getEyeHeight() - (entity instanceof Player ? 0.5D : 0.0D);
        for (int index = 0; index < count; index++) {
            level.sendParticles(particle, entity.getX(), y, entity.getZ(), 0,
                    (look.x + level.random.nextGaussian() * 0.2D) * 0.2D,
                    (look.y + level.random.nextGaussian() * 0.2D) * 0.2D,
                    (look.z + level.random.nextGaussian() * 0.2D) * 0.2D, 1.0D);
        }
    }
}
