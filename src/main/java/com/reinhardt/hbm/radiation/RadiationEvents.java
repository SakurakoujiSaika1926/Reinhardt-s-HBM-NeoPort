package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.command.RhbmCommand;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.item.ArmorInsertItem;
import com.reinhardt.hbm.item.LegacyReviveArmorModItem;
import com.reinhardt.hbm.item.LegacyInjectorKnifeArmorModItem;
import com.reinhardt.hbm.item.BlockBlastResistanceTooltip;
import com.reinhardt.hbm.item.HbmPlayerShield;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RadiationEvents {
    private static final net.minecraft.resources.ResourceLocation ARMOR_HEALTH_MODIFIER = ReinhardtsHBM.id("armor_mod_health");
    private static final net.minecraft.resources.ResourceLocation ARMOR_INSERT_SPEED_MODIFIER = ReinhardtsHBM.id("armor_insert_speed");
    private static final net.minecraft.resources.ResourceLocation ARMOR_MOD_SPEED_MODIFIER = ReinhardtsHBM.id("armor_mod_speed");
    private RadiationEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RhbmCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.tick(level);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.invalidateResistance(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.invalidateResistance(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.invalidateResistance(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isSimulated() && event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.invalidateResistance(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof ItemEntity itemEntity) {
            tickItemEntity(itemEntity);
            return;
        }

        if (entity instanceof LivingEntity living) {
            tickLiving(living);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        BlockBlastResistanceTooltip.append(event.getItemStack(), event.getToolTip());
        HbmHazardSystem.appendTooltip(event.getItemStack(), event.getToolTip());
    }

    /**
     * 1.7.10 rewarded a lodestone on one out of every 64 iron-smelting
     * operations. It is the only source for the magnet armor-mod chain.
     */
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !event.getSmelting().is(net.minecraft.world.item.Items.IRON_INGOT)
                || player.getRandom().nextInt(64) != 0) {
            return;
        }

        ItemStack lodestone = new ItemStack(com.reinhardt.hbm.registry.HbmItems.LODESTONE.get());
        if (!player.getInventory().add(lodestone)) {
            player.drop(lodestone, false);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player) {
            HbmPlayerShield.absorb(player, event);
            applyArmorModDamage(player, event);
            applyArmorInsertDamage(player, event);
        }
    }

    /** Preserves ItemModRevive's pre-death armor-slot rescue behavior. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onLivingDeathFirst(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player
                && LegacyReviveArmorModItem.tryRevive(player)) {
            event.setCanceled(true);
        }
    }

    private static void tickLiving(LivingEntity living) {
        if (!(living.level() instanceof ServerLevel level)) {
            return;
        }

        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        tickLegacyBurning(level, living);
        if (living.tickCount % 20 == 0) {
            data.setRadiationBuffer(data.getEnvironmentRadiation());
            data.setEnvironmentRadiation(0.0F);
            data.setNeutron(0.0F);
        }

        double chunkRadiation = HbmRadiationWorlds.getRadiation(level, living.blockPosition());
        data.setChunkRadiation((float) chunkRadiation);
        if (chunkRadiation > 0.0D) {
            contaminateRadiation(living, data, chunkRadiation / 20.0D);
        }

        if (living instanceof Player player && living.tickCount % HbmRadiationConstants.HAZARD_RATE_TICKS == 0) {
            applyInventoryHazards(player, data);
        }
        if (living instanceof Player player) {
            ArmorFSBItem.tickFullSet(player);
            applyArmorHealthModifier(player);
            tickArmorInsert(player, data);
            tickArmorMods(player);
            HbmPlayerShield.tick(player);
        }

        applyRadiationVomiting(living, data.getRadiation());
        applyRadiationEffects(living, data);
        applyDigammaEffects(living, data);
        HbmLivingRadiation.set(living, data);
    }

    private static void applyArmorHealthModifier(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(ARMOR_HEALTH_MODIFIER);
        double bonus = ArmorModHandler.healthBonus(player.getArmorSlots(), player.registryAccess());
        if (bonus > 0.0D) {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    ARMOR_HEALTH_MODIFIER,
                    bonus,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyArmorInsertDamage(Player player, LivingIncomingDamageEvent event) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack insert = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.KEVLAR];
        if (!(insert.getItem() instanceof ArmorInsertItem armorInsert)) {
            return;
        }

        boolean projectile = event.getSource().is(DamageTypeTags.IS_PROJECTILE);
        boolean explosion = event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        event.setAmount(armorInsert.modifyDamage(event.getAmount(), projectile, explosion));

        insert.setDamageValue(insert.getDamageValue() + 1);
        if (armorInsert.reactive()) {
            player.level().explode(player, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    0.05F, Level.ExplosionInteraction.NONE);
        }
        if (insert.getDamageValue() >= insert.getMaxDamage()) {
            ArmorModHandler.removeMod(chestplate, ArmorModHandler.KEVLAR);
        } else {
            ArmorModHandler.applyMod(chestplate, insert, player.registryAccess());
        }
    }

    private static void applyArmorModDamage(Player player, LivingIncomingDamageEvent event) {
        float amount = event.getAmount();
        for (ItemStack armor : player.getArmorSlots()) {
            for (ItemStack mod : ArmorModHandler.pryMods(armor, player.registryAccess())) {
                if (mod.getItem() instanceof ArmorModItem armorMod) {
                    amount = armorMod.modifyArmorDamage(player, armor, event.getSource(), amount);
                }
            }
        }
        event.setAmount(amount);
    }

    private static void tickArmorMods(Player player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(ARMOR_MOD_SPEED_MODIFIER);
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);

        double movementMultiplier = 1.0D;
        boolean injectorKnifeInstalled = false;
        for (ItemStack armor : player.getArmorSlots()) {
            for (ItemStack mod : ArmorModHandler.pryMods(armor, player.registryAccess())) {
                if (mod.getItem() instanceof ArmorModItem armorMod) {
                    injectorKnifeInstalled |= armorMod instanceof LegacyInjectorKnifeArmorModItem;
                    armorMod.tickArmor(player, armor);
                    movementMultiplier *= armorMod.movementMultiplier();
                }
            }
        }
        if (!injectorKnifeInstalled && maxHealth != null) {
            maxHealth.removeModifier(LegacyInjectorKnifeArmorModItem.HEALTH_MODIFIER);
        }
        if (movement != null && movementMultiplier != 1.0D) {
            movement.addOrUpdateTransientModifier(new AttributeModifier(
                    ARMOR_MOD_SPEED_MODIFIER,
                    movementMultiplier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void tickArmorInsert(Player player, HbmLivingRadiation data) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(ARMOR_INSERT_SPEED_MODIFIER);
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack insert = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.KEVLAR];
        if (!(insert.getItem() instanceof ArmorInsertItem armorInsert)) {
            return;
        }

        if (armorInsert.radioactive()) {
            data.addRadiation(100.0F);
        }
        if (movement != null && armorInsert.speedMultiplier() != 1.0F) {
            movement.addOrUpdateTransientModifier(new AttributeModifier(
                    ARMOR_INSERT_SPEED_MODIFIER,
                    armorInsert.speedMultiplier() - 1.0F,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void tickLegacyBurning(ServerLevel level, LivingEntity living) {
        if (!living.isAlive()) {
            return;
        }

        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        if (living.fireImmune() || living.isInWaterOrRain()) {
            hazards.clearFire();
        }

        int phase = living.tickCount + living.getId();
        if (hazards.getFire() > 0) {
            hazards.tickFire();
            if (phase % 15 == 0) {
                level.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        1.0F, 1.5F + living.getRandom().nextFloat() * 0.5F);
            }
            if (phase % 40 == 0) {
                living.hurt(living.damageSources().onFire(), 2.0F);
            }
            spawnAttachedFlame(level, living, HbmParticleTypes.FLAMETHROWER_FIRE.get());
        }

        if (hazards.getBalefire() > 0) {
            hazards.tickBalefire();
            if (phase % 15 == 0) {
                level.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        1.0F, 1.5F + living.getRandom().nextFloat() * 0.5F);
            }
            HbmLivingRadiation.get(living).addRadiation(5.0F);
            if (phase % 20 == 0) {
                living.hurt(living.damageSources().onFire(), 5.0F);
            }
            spawnAttachedFlame(level, living, HbmParticleTypes.FLAMETHROWER_BALEFIRE.get());
        }
    }

    private static void spawnAttachedFlame(ServerLevel level, LivingEntity living,
                                            net.minecraft.core.particles.ParticleOptions particle) {
        double x = living.getX() - living.getBbWidth() * 0.5D + living.getBbWidth() * living.getRandom().nextDouble();
        double y = living.getY() + living.getRandom().nextDouble() * living.getBbHeight();
        double z = living.getZ() - living.getBbWidth() * 0.5D + living.getBbWidth() * living.getRandom().nextDouble();
        Vec3 position = new Vec3(x, y, z);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position) <= 50.0D * 50.0D) {
                level.sendParticles(player, particle, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void applyInventoryHazards(Player player, HbmLivingRadiation data) {
        for (ItemStack stack : player.getInventory().items) {
            applyStackHazards(player, data, stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            applyStackHazards(player, data, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            applyStackHazards(player, data, stack);
        }
    }

    private static void applyStackHazards(Player player, HbmLivingRadiation data, ItemStack stack) {
        HbmHazardData hazards = HbmHazardSystem.hazards(stack);
        if (hazards.isEmpty()) {
            return;
        }

        if (hazards.radiation() > 0.0D) {
            contaminateRadiation(player, data, hazards.radiation() / 20.0D * HbmRadiationConstants.HAZARD_RATE_TICKS);
        }
        if (hazards.digamma() > 0.0D && canReceiveDose(player) && !player.hasEffect(HbmMobEffects.STABILITY)) {
            data.addDigamma((float) (hazards.digamma() / 20.0D * HbmRadiationConstants.HAZARD_RATE_TICKS));
        }
        if (hazards.hot() > 0.0D && canReceiveDose(player) && !player.isInWaterOrRain()) {
            player.igniteForSeconds((float) Math.ceil(hazards.hot()) * HbmRadiationConstants.HAZARD_RATE_TICKS);
        }
        if (hazards.blinding() > 0.0D && canReceiveDose(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    Math.max(1, (int) Math.ceil(hazards.blinding()) * HbmRadiationConstants.HAZARD_RATE_TICKS),
                    0,
                    false,
                    true
            ));
        }
    }

    private static void tickItemEntity(ItemEntity itemEntity) {
        if (!itemEntity.onGround() || itemEntity.tickCount % 20 != 0 || !(itemEntity.level() instanceof ServerLevel level)) {
            return;
        }

        HbmHazardData hazards = HbmHazardSystem.hazards(itemEntity.getItem());
        if (hazards.contaminating() <= 0.0D) {
            return;
        }

        int radius = (int) Math.min(Math.sqrt(hazards.contaminating()) + 0.5D, 500.0D);
        if (radius > 1) {
            ChunkRadiationData.get(level).incrementRadiation(itemEntity.blockPosition(), hazards.contaminating());
        }
        itemEntity.discard();
    }

    private static void contaminateRadiation(LivingEntity living, HbmLivingRadiation data, double amount) {
        if (living.hasEffect(HbmMobEffects.RADX)) {
            // 1.7.10 Rad-X added 0.2 radiation resistance in HazmatRegistry.
            amount *= 0.8D;
        }
        data.addEnvironmentRadiation((float) amount);
        if (canReceiveDose(living)) {
            data.addRadiation((float) amount);
        }
    }

    private static boolean canReceiveDose(LivingEntity living) {
        return !(living instanceof Player player && (player.isCreative() || player.isSpectator() || player.tickCount < 200));
    }

    private static void applyRadiationVomiting(LivingEntity living, float radiation) {
        if (!(living.level() instanceof ServerLevel level) || !canReceiveDose(living) || !canVomit(living)) {
            return;
        }

        java.util.Random random = new java.util.Random(living.getId());
        int offset600 = random.nextInt(600);
        int offset1200 = random.nextInt(1200);
        long time = level.getGameTime();
        long bloodCycle = (time + offset600) % 600L;
        long normalCycle = (time + offset1200) % 1200L;

        if (radiation > 600.0F && bloodCycle < 20L) {
            spawnVomit(level, living, true, 25);
            if (bloodCycle == 1L) {
                playVomit(level, living);
            }
        } else if (radiation > 200.0F && normalCycle < 20L) {
            spawnVomit(level, living, false, 15);
            if (normalCycle == 1L) {
                playVomit(level, living);
            }
        }
    }

    private static void spawnVomit(ServerLevel level, LivingEntity living, boolean blood, int count) {
        Vec3 look = living.getLookAngle();
        double x = living.getX();
        double y = living.getY() + living.getEyeHeight() - (living instanceof Player ? 0.5D : 0.0D);
        double z = living.getZ();

        for (int i = 0; i < count; i++) {
            double xSpeed = (look.x + level.random.nextGaussian() * 0.2D) * 0.2D;
            double ySpeed = (look.y + level.random.nextGaussian() * 0.2D) * 0.2D;
            double zSpeed = (look.z + level.random.nextGaussian() * 0.2D) * 0.2D;
            level.sendParticles(
                    blood ? HbmParticleTypes.BLOOD_VOMIT.get() : HbmParticleTypes.VOMIT.get(),
                    x,
                    y,
                    z,
                    0,
                    xSpeed,
                    ySpeed,
                    zSpeed,
                    1.0D
            );
        }
    }

    private static void playVomit(ServerLevel level, LivingEntity living) {
        level.playSound(null, living.getX(), living.getY(), living.getZ(), HbmSoundEvents.VOMIT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
    }

    private static boolean canVomit(LivingEntity living) {
        return living.getType().getCategory() != MobCategory.WATER_CREATURE;
    }

    private static void applyRadiationEffects(LivingEntity living, HbmLivingRadiation data) {
        if (!canReceiveDose(living)) {
            return;
        }

        float radiation = data.getRadiation();
        if (radiation < 200.0F) {
            return;
        }
        if (radiation > HbmLivingRadiation.MAX_RADIATION) {
            data.setRadiation(HbmLivingRadiation.MAX_RADIATION);
            radiation = data.getRadiation();
        }

        int rng = living.level().random.nextInt(21_000);
        if (radiation >= 1000.0F) {
            living.hurt(living.damageSources().source(HbmDamageTypes.RADIATION), 1000.0F);
            data.setRadiation(0.0F);
            if (living.isAlive()) {
                living.setHealth(0.0F);
            }
            return;
        }
        if (radiation >= 800.0F) {
            if (rng % 300 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (rng % 300 == 50) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 2));
            }
            if (rng % 300 == 100) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 2));
            }
            if (rng % 500 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20, 2));
            }
            if (rng % 700 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 3 * 20, 1));
            }
            if (rng % 300 == 150) {
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 5 * 20, 3));
            }
            if (rng % 300 == 200) {
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 20, 3));
            }
        } else if (radiation >= 600.0F) {
            if (rng % 300 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (rng % 300 == 50) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 2));
            }
            if (rng % 300 == 100) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 2));
            }
            if (rng % 500 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 3 * 20, 1));
            }
            if (rng % 300 == 150) {
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 20, 3));
            }
            if (rng % 400 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6 * 20, 2));
            }
        } else if (radiation >= 400.0F) {
            if (rng % 300 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 30, 0));
            }
            if (rng % 500 == 50) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 0));
            }
            if (rng % 300 == 100) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 1));
            }
            if (rng % 500 == 150) {
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 20, 2));
            }
            if (rng % 600 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 4 * 20, 1));
            }
        } else {
            if (rng % 300 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0));
            }
            if (rng % 500 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 0));
            }
            if (rng % 700 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 20, 2));
            }
            if (rng % 800 == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 4 * 20, 0));
            }
        }
    }

    private static void applyDigammaEffects(LivingEntity living, HbmLivingRadiation data) {
        if (!canReceiveDose(living)) {
            return;
        }

        float digamma = data.getDigamma();
        if (digamma < 0.01F) {
            return;
        }
        if (digamma >= HbmLivingRadiation.MAX_DIGAMMA || living.getMaxHealth() <= 0.0F) {
            living.hurt(living.damageSources().source(HbmDamageTypes.DIGAMMA), 5_000_000.0F);
            if (living.isAlive()) {
                living.setHealth(0.0F);
            }
        }
    }
}
