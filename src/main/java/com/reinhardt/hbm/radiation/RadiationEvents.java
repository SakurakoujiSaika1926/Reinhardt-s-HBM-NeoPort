package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.command.RhbmCommand;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.item.ArmorModItem;
import com.reinhardt.hbm.item.ArmorInsertItem;
import com.reinhardt.hbm.item.LegacyReviveArmorModItem;
import com.reinhardt.hbm.item.LegacyInjectorKnifeArmorModItem;
import com.reinhardt.hbm.item.LegacyJetpackItem;
import com.reinhardt.hbm.item.BlockBlastResistanceTooltip;
import com.reinhardt.hbm.item.HbmPlayerShield;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RadiationEvents {
    private static final net.minecraft.resources.ResourceLocation ARMOR_HEALTH_MODIFIER = ReinhardtsHBM.id("armor_mod_health");
    private static final net.minecraft.resources.ResourceLocation ARMOR_INSERT_SPEED_MODIFIER = ReinhardtsHBM.id("armor_insert_speed");
    private static final net.minecraft.resources.ResourceLocation ARMOR_MOD_SPEED_MODIFIER = ReinhardtsHBM.id("armor_mod_speed");
    private static final net.minecraft.resources.ResourceLocation ARMOR_STEP_HEIGHT_MODIFIER = ReinhardtsHBM.id("armor_step_height");
    private static final net.minecraft.resources.ResourceLocation LEGACY_REACHER_ID = ReinhardtsHBM.id("reacher");
    private static final Set<StratumXpDrop> STRATUM_XP_DROPS = ConcurrentHashMap.newKeySet();
    private static final int UNKNOWN_INVENTORY_HAZARD_HASH = Integer.MIN_VALUE;
    private static final int INVENTORY_HAZARD_FALLBACK_CHECK_TICKS = 20;
    private static final ExecutorService INVENTORY_HAZARD_SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-InventoryHazards");
        thread.setDaemon(true);
        return thread;
    });
    private static final java.util.Map<UUID, InventoryHazardState> INVENTORY_HAZARD_STATES = new ConcurrentHashMap<>();
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
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.markChunkLoaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.markChunkUnloaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.unload(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        markPlayerRadiationInputsDirty(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearPlayerRadiationInputs(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        clearPlayerRadiationInputs(event.getOriginal());
        markPlayerRadiationInputsDirty(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        markPlayerRadiationInputsDirty(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        markPlayerRadiationInputsDirty(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerContainerClose(PlayerContainerEvent.Close event) {
        markPlayerRadiationInputsDirty(event.getEntity());
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        markPlayerRadiationInputsDirty(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            markPlayerRadiationInputsDirty(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HbmRadiationWorlds.invalidateResistance(level, event.getPos());
            if (event.getPlayer() instanceof ServerPlayer player
                    && event.getState().is(com.reinhardt.hbm.registry.HbmBlocks.STONE_GNEISS.get())
                    && !HbmAdvancements.has(player, "stratum")) {
                HbmAdvancements.award(player, "stratum");
                STRATUM_XP_DROPS.add(new StratumXpDrop(level.dimension(), event.getPos().immutable(), player.getUUID()));
            }
            emitLegacyCoalDust(level, event);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player
                && event.getState().is(com.reinhardt.hbm.registry.HbmBlocks.STONE_GNEISS.get())
                && STRATUM_XP_DROPS.remove(new StratumXpDrop(event.getLevel().dimension(), event.getPos().immutable(), player.getUUID()))) {
            event.setDroppedExperience(500);
        }
    }

    /**
     * 1.7.10's block-break hook released coal dust when coal ore, a coal
     * block, or lignite was mined.  This is intentionally separate from the
     * inventory hazard (which only applies to dust items): the six adjacent
     * air blocks each had an independent 1/2 roll for a temporary gas_coal
     * cloud.
     */
    private static void emitLegacyCoalDust(ServerLevel level, BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!isLegacyCoalDustSource(state)) {
            return;
        }

        BlockPos origin = event.getPos();
        for (Direction direction : Direction.values()) {
            BlockPos target = origin.relative(direction);
            if (level.isEmptyBlock(target) && level.random.nextBoolean()) {
                level.setBlock(target, com.reinhardt.hbm.registry.HbmBlocks.GAS_COAL.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static boolean isLegacyCoalDustSource(BlockState state) {
        Block block = state.getBlock();
        // Deepslate variants did not exist in 1.7.10, but are the modern
        // equivalent of the same coal/lignite ores and must retain the
        // legacy mining hazard.
        return block == net.minecraft.world.level.block.Blocks.COAL_ORE
                || block == net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE
                || block == net.minecraft.world.level.block.Blocks.COAL_BLOCK
                || block == com.reinhardt.hbm.registry.HbmBlocks.ORE_LIGNITE.get()
                || block == com.reinhardt.hbm.registry.HbmBlocks.ORE_DEEPSLATE_LIGNITE.get();
    }

    private record StratumXpDrop(ResourceKey<Level> dimension, BlockPos pos, UUID player) {
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
        appendArmorRadiationTooltip(event);
    }

    private static void appendArmorRadiationTooltip(ItemTooltipEvent event) {
        double resistance = HbmArmorProtection.itemRadiationTooltipResistance(event.getItemStack());
        if (resistance <= 0.0D) {
            return;
        }
        event.getToolTip().add(Component.translatable(
                "tooltip.reinhardtshbm.armor.radiation_resistance",
                formatResistance(resistance),
                formatReductionPercent(HbmArmorProtection.multiplierForResistance(resistance))
        ).withStyle(ChatFormatting.YELLOW));
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        markPlayerRadiationInputsDirty(event.getEntity());
        HbmAdvancements.awardForCraftedStack(event.getEntity(), event.getCrafting());
    }

    /**
     * 1.7.10 rewarded a lodestone on one out of every 64 iron-smelting
     * operations. It is the only source for the magnet armor-mod chain.
     */
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();
        markPlayerRadiationInputsDirty(player);
        HbmAdvancements.awardForCraftedStack(player, event.getSmelting());
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
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        markPlayerRadiationInputsDirty(event.getPlayer());
        if (event.getOriginalStack().is(net.minecraft.world.item.Items.SLIME_BALL)) {
            HbmAdvancements.award(event.getPlayer(), "slimeball");
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player) {
            HbmPlayerShield.absorb(player, event);
            applyArmorModDamage(player, event);
            applyArmorInsertDamage(player, event);
            applyLegacyArmorCombatHooks(player, event);
        }
    }

    /**
     * ArmorDNT and ArmorTrenchmaster used LivingAttack/Hurt hooks in 1.7.10.
     * NeoForge exposes the same point as LivingIncomingDamageEvent.
     */
    private static void applyLegacyArmorCombatHooks(Player player, LivingIncomingDamageEvent event) {
        if (!ArmorFSBItem.hasFSBArmor(player)) {
            return;
        }
        String group = ArmorFSBItem.fullSetGroup(player);
        if ("dns".equals(group)) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                event.setAmount(event.getAmount() * 0.001F);
            } else {
                event.setAmount(0.0F);
            }
        } else if ("trenchmaster".equals(group)
                && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && player.getRandom().nextInt(3) == 0) {
            event.setCanceled(true);
        }
    }

    /** Restores ArmorFSB's hard-landing shockwave without touching vanilla fall
     * damage when the suit is unpowered or incomplete. */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide
                || !ArmorFSBItem.hasFSBArmor(player)
                || !ArmorFSBItem.hasFeature(player, ArmorFSBItem.Feature.HARD_LANDING)
                || event.getDistance() <= 10.0F) {
            return;
        }

        var bounds = player.getBoundingBox().inflate(3.0D, 0.0D, 3.0D);
        for (Entity target : player.level().getEntities(player, bounds,
                candidate -> candidate instanceof LivingEntity living && living.isAlive())) {
            double dx = player.getX() - target.getX();
            double dz = player.getZ() - target.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= 3.0D) {
                continue;
            }
            double intensity = 3.0D - distance;
            target.push(-dx * intensity * 0.18D, 0.1D * intensity, -dz * intensity * 0.18D);
            target.hurt(player.damageSources().playerAttack(player), (float) (intensity * 10.0D));
        }
        event.setDistance(0.0F);
    }

    /** Preserves ItemModRevive's pre-death armor-slot rescue behavior. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onLivingDeathFirst(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Player player
                && LegacyReviveArmorModItem.tryRevive(player)) {
            clearReviveHazards(player);
            event.setCanceled(true);
        }
    }

    /**
     * Do not carry transient contamination through a real death.  The legacy
     * 1.7.10 death hook reset radiation explicitly; doing the same for the
     * attachment (including its environment/neutron/digamma fields) also
     * prevents a dead entity or a freshly revived player from re-entering a
     * lethal check every tick.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onLivingDeathCleanup(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            if (event.getEntity() instanceof com.reinhardt.hbm.entity.LegacyTaintedCreeperEntity
                    && event.getSource().is(HbmDamageTypes.BOXCAR)
                    && event.getEntity().level() instanceof ServerLevel level) {
                HbmAdvancements.awardNearby(level, event.getEntity().getBoundingBox().inflate(50.0D), "hidden");
            }
            // Radiation was reset unconditionally by the 1.7.10 death hook;
            // keep that guarantee even when another mod cancels the death.
            HbmLivingRadiation.clear(event.getEntity());
            // Clear respiratory hazards even when another handler cancels
            // the death (totem/custom revival).  The lethal setter resets the
            // value before hurt(), but a cancelled death can otherwise leave
            // a stale asbestos/black-lung value that immediately kills the
            // entity again on the next tick.
            HbmLivingHazards.clear(event.getEntity());
        }
    }

    private static void clearReviveHazards(Player player) {
        player.removeEffect(MobEffects.BLINDNESS);
        HbmLivingHazards.clear(player);
        HbmLivingRadiation.clear(player);
        markPlayerRadiationInputsDirty(player);
    }

    private static void markPlayerRadiationInputsDirty(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        InventoryHazardState state = INVENTORY_HAZARD_STATES.computeIfAbsent(player.getUUID(),
                unused -> new InventoryHazardState());
        state.dirty = true;
        state.cached = InventoryHazardResult.EMPTY;
        HbmArmorProtection.invalidateRadiationMultiplier(player);
    }

    private static void clearPlayerRadiationInputs(Player player) {
        if (player == null) {
            return;
        }
        InventoryHazardState state = INVENTORY_HAZARD_STATES.remove(player.getUUID());
        if (state != null && state.inFlight != null) {
            state.inFlight.cancel(false);
        }
        HbmArmorProtection.invalidateRadiationMultiplier(player);
    }

    private static void tickLiving(LivingEntity living) {
        if (!(living.level() instanceof ServerLevel level)) {
            return;
        }

        // A few vanilla/compatibility paths can leave a zero-health entity in
        // the tick list for a short death window.  The old handler returned as
        // soon as the entity was dead; clear the attachment here as a second
        // line of defence for deaths that did not go through LivingDeathEvent
        // (for example a direct setHealth(0) fallback).
        if (!living.isAlive()) {
            HbmLivingRadiation.clear(living);
            return;
        }

        tickLegacyBurning(level, living);
        // Fire/balefire damage can kill an entity during the legacy burn pass.
        // Do not continue scanning its inventory after the death hook has
        // cleared the transient attachments.
        if (!living.isAlive()) {
            HbmLivingRadiation.clear(living);
            HbmLivingHazards.clear(living);
            return;
        }

        // HazardSystem.updateLivingInventory ran for every non-player
        // EntityLivingBase in 1.7.10.  Keep the respiratory (asbestos/coal)
        // path at the same every-tick cadence for mobs as well as players.
        applyRespiratoryInventoryHazards(living);
        if (!living.isAlive()) {
            HbmLivingRadiation.clear(living);
            HbmLivingHazards.clear(living);
            return;
        }

        HbmLivingRadiation data = living.getExistingDataOrNull(HbmDataAttachments.LIVING_RADIATION);
        boolean radiationImmune = isLegacyRadiationImmune(living);
        double chunkRadiation = 0.0D;
        if (!radiationImmune) {
            HbmRadiationWorlds.queueExposure(level, living);
            chunkRadiation = HbmRadiationWorlds.getExposureRadiation(level, living);
        }
        if (data == null && chunkRadiation <= 0.0D && !(living instanceof Player)) {
            return;
        }
        if (data == null) {
            data = HbmLivingRadiation.get(living);
        }
        if (living.tickCount % 20 == 0) {
            data.setRadiationBuffer(data.getEnvironmentRadiation());
            data.setEnvironmentRadiation(0.0F);
            data.setNeutron(0.0F);
        }

        data.setChunkRadiation(radiationImmune ? 0.0F : (float) chunkRadiation);
        if (!radiationImmune && chunkRadiation > 0.0D) {
            contaminateRadiation(living, data, chunkRadiation / 20.0D);
        }

        // ContaminationUtil.isRadImmune excluded the legacy immune classes
        // from accumulated radiation and all radiation sickness effects.
        // Keep the environmental/respiratory pass above, but never let the
        // modern attachment turn those entities radioactive or apply digamma
        // health modifiers.
        if (radiationImmune) {
            data.setRadiation(0.0F);
            data.setDigamma(0.0F);
            data.setNeutron(0.0F);
            HbmLivingRadiation.set(living, data);
            return;
        }

        if (living instanceof Player player) {
            // 1.7.10 HazardSystem.updatePlayerInventory ran every tick. Keep
            // respiratory hazards at that cadence; radiation/hot/etc. remain
            // on the throttled five-tick path below.
            if (living.tickCount % HbmRadiationConstants.HAZARD_RATE_TICKS == 0) {
                applyInventoryHazards(player, data);
            }
        }
        if (living instanceof Player player) {
            ArmorFSBItem.tickFullSet(player);
            applyArmorHealthModifier(player);
            tickArmorInsert(player, data);
            tickArmorMods(player);
            tickArmorProfile(player, data);
            HbmPlayerShield.tick(player);
        }

        applyRadiationVomiting(living, data.getRadiation());
        if (!applyLegacyRadiationTransformations(living, data)) {
            applyRadiationEffects(living, data);
        }
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
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.removeModifier(ARMOR_STEP_HEIGHT_MODIFIER);
        }
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);

        double movementMultiplier = 1.0D;
        boolean injectorKnifeInstalled = false;
        for (ItemStack armor : player.getArmorSlots()) {
            for (ItemStack mod : ArmorModHandler.pryMods(armor, player.registryAccess())) {
                if (mod == null || mod.isEmpty()) {
                    continue;
                }
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
        ArmorFSBItem.FeatureProfile profile = ArmorFSBItem.features(player);
        if (stepHeight != null && profile.stepHeight() > 0 && ArmorFSBItem.hasFSBArmor(player)) {
            stepHeight.addOrUpdateTransientModifier(new AttributeModifier(
                    ARMOR_STEP_HEIGHT_MODIFIER,
                    profile.stepHeight(),
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        ArmorFSBItem.tickDash(player);
        LegacyJetpackItem.tickBuiltInJetpack(player);
    }

    private static void tickArmorProfile(Player player, HbmLivingRadiation data) {
        if (!ArmorFSBItem.hasFSBArmor(player)) {
            return;
        }
        ArmorFSBItem.FeatureProfile profile = ArmorFSBItem.features(player);
        if (profile.geigerSound() && data.getRadiation() > 0.01F && player.tickCount % 5 == 0) {
            int level = Math.min(6, Math.max(1, (int) Math.floor(Math.log10(data.getRadiation() + 1.0F)) + 1));
            var sound = switch (level) {
                case 1 -> HbmSoundEvents.GEIGER_1.get();
                case 2 -> HbmSoundEvents.GEIGER_2.get();
                case 3 -> HbmSoundEvents.GEIGER_3.get();
                case 4 -> HbmSoundEvents.GEIGER_4.get();
                case 5 -> HbmSoundEvents.GEIGER_5.get();
                default -> HbmSoundEvents.GEIGER_6.get();
            };
            player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.45F, 1.0F);
        }
    }

    private static void tickArmorInsert(Player player, HbmLivingRadiation data) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(ARMOR_INSERT_SPEED_MODIFIER);
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack insert = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.KEVLAR];
        if (insert == null || insert.isEmpty()) {
            return;
        }
        if (!(insert.getItem() instanceof ArmorInsertItem armorInsert)) {
            return;
        }

        if (armorInsert.radioactive()) {
            data.addRadiationWithReadout(100.0F);
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

        HbmLivingHazards hazards = living.getExistingDataOrNull(HbmDataAttachments.LIVING_HAZARDS);
        if (hazards == null) {
            return;
        }
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
            HbmLivingRadiation.get(living).addRadiationWithReadout(5.0F);
            if (phase % 20 == 0) {
                living.hurt(living.damageSources().onFire(), 5.0F);
            }
            spawnAttachedFlame(level, living, HbmParticleTypes.FLAMETHROWER_BALEFIRE.get());
        }

        if (hazards.getBlackFire() > 0) {
            hazards.tickBlackFire();
            if (phase % 10 == 0) {
                level.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        1.0F, 1.5F + living.getRandom().nextFloat() * 0.5F);
                living.hurt(living.damageSources().onFire(), 10.0F);
            }
            HbmLivingRadiation.get(living).addRadiationWithReadout(5.0F);
            spawnAttachedFlame(level, living, HbmParticleTypes.FLAMETHROWER_BLACK.get());
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
        InventoryHazardState state = updateInventoryHazardState(player);
        applyInventoryHazardResult(player, data, state.cached);
    }

    private static InventoryHazardState updateInventoryHazardState(Player player) {
        InventoryHazardState state = INVENTORY_HAZARD_STATES.computeIfAbsent(player.getUUID(),
                unused -> new InventoryHazardState());
        if (state.lastUpdateTick == player.tickCount) {
            return state;
        }
        state.lastUpdateTick = player.tickCount;

        CompletableFuture<InventoryHazardResult> inFlight = state.inFlight;
        if (inFlight != null && inFlight.isDone()) {
            state.inFlight = null;
            InventoryHazardResult result = joinInventoryHazards(inFlight);
            int currentHash = inventoryHazardHash(player);
            state.observedHash = currentHash;
            state.nextFallbackCheckTick = nextInventoryHazardFallbackTick(player);
            if (result != null && result.inventoryHash() == currentHash) {
                state.cached = result;
                state.dirty = false;
            } else {
                state.cached = InventoryHazardResult.EMPTY;
                state.dirty = true;
            }
        } else if (inFlight != null) {
            return state;
        } else if (!state.dirty && player.tickCount >= state.nextFallbackCheckTick) {
            int currentHash = inventoryHazardHash(player);
            state.nextFallbackCheckTick = nextInventoryHazardFallbackTick(player);
            if (state.observedHash != UNKNOWN_INVENTORY_HAZARD_HASH && state.observedHash != currentHash) {
                state.cached = InventoryHazardResult.EMPTY;
                state.dirty = true;
            }
            state.observedHash = currentHash;
        }

        if (state.dirty) {
            InventoryHazardSnapshot snapshot = snapshotInventoryHazards(player);
            state.observedHash = snapshot.inventoryHash();
            state.nextFallbackCheckTick = nextInventoryHazardFallbackTick(player);
            state.dirty = false;
            state.inFlight = CompletableFuture.supplyAsync(
                    () -> summarizeInventoryHazards(snapshot),
                    INVENTORY_HAZARD_SOLVER);
            return state;
        }

        return state;
    }

    private static InventoryHazardResult joinInventoryHazards(CompletableFuture<InventoryHazardResult> future) {
        try {
            return future.join();
        } catch (CancellationException exception) {
            return null;
        } catch (CompletionException exception) {
            ReinhardtsHBM.LOGGER.warn("Asynchronous inventory radiation hazard calculation failed", exception);
            return null;
        }
    }

    private static InventoryHazardSnapshot snapshotInventoryHazards(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        int hash = inventoryHazardInitialHash();
        hash = appendInventoryStacks(player.getInventory().items, stacks, hash);
        hash = appendInventoryStacks(player.getInventory().armor, stacks, hash);
        hash = appendInventoryStacks(player.getInventory().offhand, stacks, hash);
        return new InventoryHazardSnapshot(
                player.tickCount,
                hash,
                HbmConfig.ENABLE_528_MODE.get(),
                HbmConfig.ENABLE_ASBESTOS.get(),
                HbmConfig.ENABLE_COAL_DUST.get(),
                List.copyOf(stacks));
    }

    private static int inventoryHazardHash(Player player) {
        int hash = inventoryHazardInitialHash();
        hash = appendInventoryHash(player.getInventory().items, hash);
        hash = appendInventoryHash(player.getInventory().armor, hash);
        return appendInventoryHash(player.getInventory().offhand, hash);
    }

    private static int inventoryHazardInitialHash() {
        int hash = 31 + (HbmConfig.ENABLE_528_MODE.get() ? 1 : 0);
        hash = 31 * hash + (HbmConfig.ENABLE_ASBESTOS.get() ? 1 : 0);
        return 31 * hash + (HbmConfig.ENABLE_COAL_DUST.get() ? 1 : 0);
    }

    private static int nextInventoryHazardFallbackTick(Player player) {
        return player.tickCount + INVENTORY_HAZARD_FALLBACK_CHECK_TICKS
                + Math.floorMod(player.getId(), INVENTORY_HAZARD_FALLBACK_CHECK_TICKS);
    }

    private static int appendInventoryHash(Iterable<ItemStack> source, int hash) {
        for (ItemStack stack : source) {
            hash = 31 * hash + inventoryHazardStackHash(stack);
        }
        return hash;
    }

    private static int appendInventoryStacks(Iterable<ItemStack> source, List<ItemStack> target, int hash) {
        for (ItemStack stack : source) {
            hash = 31 * hash + inventoryHazardStackHash(stack);
            if (stack != null && !stack.isEmpty()) {
                target.add(stack.copy());
            }
        }
        return hash;
    }

    private static int inventoryHazardStackHash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int hash = System.identityHashCode(stack.getItem());
        hash = 31 * hash + stack.getCount();
        hash = 31 * hash + stack.getDamageValue();
        hash = 31 * hash + stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).hashCode();
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        hash = 31 * hash + (modelData == null ? 0 : modelData.hashCode());
        return hash;
    }

    private static InventoryHazardResult summarizeInventoryHazards(InventoryHazardSnapshot snapshot) {
        if (snapshot.stacks().isEmpty()) {
            return new InventoryHazardResult(snapshot.entityTick(), snapshot.inventoryHash(),
                    0.0D, 0.0D, 0, 0, List.of());
        }
        boolean hasReacher = false;
        for (ItemStack stack : snapshot.stacks()) {
            if (isLegacyReacher(stack)) {
                hasReacher = true;
                break;
            }
        }

        double radiationDose = 0.0D;
        double digammaDose = 0.0D;
        int burnSeconds = 0;
        int blindnessTicks = 0;
        List<RespiratoryHazardEntry> respiratoryHazards = new ArrayList<>();
        boolean reacherHotProtection = hasLegacyReacherHotProtection(hasReacher, snapshot.enable528Mode());
        for (ItemStack stack : snapshot.stacks()) {
            HbmHazardData hazards = HbmHazardSystem.hazards(stack);
            if (!hazards.isEmpty()) {
                if (hazards.radiation() > 0.0D) {
                    double radiation = hazards.radiation() / 20.0D;
                    if (hasReacher) {
                        radiation = snapshot.enable528Mode() ? radiation / 49.0D : squirt(radiation);
                    }
                    radiationDose += radiation * HbmRadiationConstants.HAZARD_RATE_TICKS;
                }
                if (hazards.digamma() > 0.0D) {
                    digammaDose += hazards.digamma() / 20.0D * HbmRadiationConstants.HAZARD_RATE_TICKS;
                }
                if (hazards.hot() > 0.0D && !reacherHotProtection) {
                    burnSeconds = Math.max(burnSeconds,
                            (int) Math.ceil(hazards.hot()) * HbmRadiationConstants.HAZARD_RATE_TICKS);
                }
                if (hazards.blinding() > 0.0D) {
                    blindnessTicks = Math.max(blindnessTicks,
                            Math.max(1, (int) Math.ceil(hazards.blinding()) * HbmRadiationConstants.HAZARD_RATE_TICKS));
                }
            }

            int asbestosAmount = 0;
            if (snapshot.enableAsbestos()) {
                double level = HbmHazardSystem.asbestosLevel(stack);
                if (level > 0.0D) {
                    asbestosAmount = (int) Math.min(level, 10.0D);
                }
            }

            int coalAmount = 0;
            int coalFilterRollBound = 0;
            int coalFilterDamage = 0;
            if (snapshot.enableCoalDust()) {
                double level = HbmHazardSystem.coalDustLevel(stack);
                if (level > 0.0D) {
                    coalAmount = (int) Math.min(level * stack.getCount(), 10.0D);
                    coalFilterRollBound = Math.max(65 - stack.getCount(), 1);
                    coalFilterDamage = (int) level;
                }
            }

            if (asbestosAmount > 0 || coalAmount > 0 || coalFilterDamage > 0) {
                respiratoryHazards.add(new RespiratoryHazardEntry(
                        asbestosAmount,
                        coalAmount,
                        coalFilterRollBound,
                        coalFilterDamage));
            }
        }
        return new InventoryHazardResult(snapshot.entityTick(), snapshot.inventoryHash(),
                radiationDose, digammaDose, burnSeconds, blindnessTicks, List.copyOf(respiratoryHazards));
    }

    private static void applyInventoryHazardResult(Player player, HbmLivingRadiation data,
                                                   InventoryHazardResult result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if (result.radiationDose() > 0.0D) {
            contaminateRadiation(player, data, result.radiationDose());
        }
        if (result.digammaDose() > 0.0D && canReceiveDose(player) && !player.hasEffect(HbmMobEffects.STABILITY)) {
            data.addDigamma((float) result.digammaDose());
        }
        if (result.burnSeconds() > 0 && canReceiveDose(player) && !player.isInWaterOrRain()) {
            player.igniteForSeconds(result.burnSeconds());
        }
        if (result.blindnessTicks() > 0 && canReceiveDose(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    result.blindnessTicks(),
                    0,
                    false,
                    true
            ));
        }
    }

    private record InventoryHazardSnapshot(int entityTick, int inventoryHash, boolean enable528Mode,
                                           boolean enableAsbestos, boolean enableCoalDust,
                                           List<ItemStack> stacks) {
    }

    private record InventoryHazardResult(int entityTick, int inventoryHash, double radiationDose,
                                         double digammaDose, int burnSeconds, int blindnessTicks,
                                         List<RespiratoryHazardEntry> respiratoryHazards) {
        private static final InventoryHazardResult EMPTY =
                new InventoryHazardResult(0, UNKNOWN_INVENTORY_HAZARD_HASH, 0.0D, 0.0D, 0, 0, List.of());

        boolean isEmpty() {
            return radiationDose <= 0.0D
                    && digammaDose <= 0.0D
                    && burnSeconds <= 0
                    && blindnessTicks <= 0
                    && respiratoryHazards.isEmpty();
        }
    }

    private record RespiratoryHazardEntry(int asbestosAmount, int coalAmount,
                                          int coalFilterRollBound, int coalFilterDamage) {
    }

    private static final class InventoryHazardState {
        private CompletableFuture<InventoryHazardResult> inFlight;
        private InventoryHazardResult cached = InventoryHazardResult.EMPTY;
        private int observedHash = UNKNOWN_INVENTORY_HAZARD_HASH;
        private int nextFallbackCheckTick;
        private int lastUpdateTick = Integer.MIN_VALUE;
        private boolean dirty = true;
    }

    /** Exact 1.7.10 HazardTypeAsbestos/HazardTypeCoal inventory behavior. */
    private static void applyRespiratoryInventoryHazards(LivingEntity living) {
        HbmLivingHazards hazards = HbmLivingHazards.get(living);
        if (living instanceof Player player) {
            InventoryHazardState state = updateInventoryHazardState(player);
            applyRespiratoryHazardEntries(living, hazards, state.cached.respiratoryHazards());
        } else {
            // The old getEquipmentInSlot(0..4) covered armor and the held
            // equipment of mobs.  These modern iterables are its equivalent.
            for (ItemStack stack : living.getArmorSlots()) {
                applyRespiratoryStackHazards(living, hazards, stack);
                if (!living.isAlive()) {
                    return;
                }
            }
            for (ItemStack stack : living.getHandSlots()) {
                applyRespiratoryStackHazards(living, hazards, stack);
                if (!living.isAlive()) {
                    return;
                }
            }
        }
        HbmLivingHazards.set(living, hazards);
    }

    private static void applyRespiratoryHazardEntries(LivingEntity living, HbmLivingHazards hazards,
                                                      List<RespiratoryHazardEntry> entries) {
        for (RespiratoryHazardEntry entry : entries) {
            if (entry.asbestosAmount() > 0
                    && !HbmArmorProtection.hasHeadProtection(living,
                    HbmArmorProtection.HazardClass.PARTICLE_FINE, entry.asbestosAmount())) {
                hazards.addAsbestos(living, entry.asbestosAmount());
                if (!living.isAlive()) {
                    return;
                }
            }

            if (entry.coalAmount() > 0 || entry.coalFilterDamage() > 0) {
                if (!HbmArmorProtection.hasHeadProtection(living,
                        HbmArmorProtection.HazardClass.PARTICLE_COARSE)) {
                    if (entry.coalAmount() > 0) {
                        hazards.addBlackLung(living, entry.coalAmount());
                    }
                } else if (entry.coalFilterDamage() > 0
                        && living.getRandom().nextInt(entry.coalFilterRollBound()) == 0) {
                    // HazardTypeCoal damages a protected filter only on this
                    // per-tick random roll, using the unstacked hazard level.
                    HbmArmorProtection.hasHeadProtection(living,
                            HbmArmorProtection.HazardClass.PARTICLE_COARSE, entry.coalFilterDamage());
                }
            }
        }
    }

    private static void applyRespiratoryStackHazards(LivingEntity living, HbmLivingHazards hazards, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (HbmConfig.ENABLE_ASBESTOS.get()) {
            double level = HbmHazardSystem.asbestosLevel(stack);
            if (level > 0.0D) {
                int amount = (int) Math.min(level, 10.0D);
                if (!HbmArmorProtection.hasHeadProtection(living,
                        HbmArmorProtection.HazardClass.PARTICLE_FINE, amount)) {
                    hazards.addAsbestos(living, amount);
                    if (!living.isAlive()) {
                        return;
                    }
                }
            }
        }

        if (HbmConfig.ENABLE_COAL_DUST.get()) {
            double level = HbmHazardSystem.coalDustLevel(stack);
            if (level > 0.0D) {
                if (!HbmArmorProtection.hasHeadProtection(living,
                        HbmArmorProtection.HazardClass.PARTICLE_COARSE)) {
                    int amount = (int) Math.min(level * stack.getCount(), 10.0D);
                    if (amount > 0) {
                        hazards.addBlackLung(living, amount);
                    }
                } else if (living.getRandom().nextInt(Math.max(65 - stack.getCount(), 1)) == 0) {
                    // HazardTypeCoal damages a protected filter only on this
                    // per-tick random roll, using the unstacked hazard level.
                    HbmArmorProtection.hasHeadProtection(living,
                            HbmArmorProtection.HazardClass.PARTICLE_COARSE, (int) level);
                }
            }
        }
    }

    private static boolean isLegacyReacher(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return LEGACY_REACHER_ID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean hasLegacyReacherHotProtection(boolean hasReacher, boolean enable528Mode) {
        return hasReacher && !enable528Mode;
    }

    private static double squirt(double x) {
        return Math.sqrt(x + 1.0D / ((x + 2.0D) * (x + 2.0D))) - 1.0D / (x + 2.0D);
    }

    private static String formatResistance(double resistance) {
        String value = String.format(Locale.ROOT, "%.4f", resistance);
        while (value.contains(".") && value.endsWith("0")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String formatReductionPercent(double multiplier) {
        double percent = Math.max(0.0D, 100.0D - multiplier * 100.0D);
        if (percent >= 99.999D) {
            return String.format(Locale.ROOT, "%.5f%%", percent);
        }
        if (percent >= 99.9D) {
            return String.format(Locale.ROOT, "%.3f%%", percent);
        }
        if (percent >= 99.0D) {
            return String.format(Locale.ROOT, "%.2f%%", percent);
        }
        if (percent >= 10.0D) {
            return String.format(Locale.ROOT, "%.1f%%", percent);
        }
        if (percent >= 1.0D) {
            return String.format(Locale.ROOT, "%.1f%%", percent);
        }
        return String.format(Locale.ROOT, "%.3f%%", percent);
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
        // Apply the per-piece HazmatRegistry resistance restored by the armor
        // port before updating both environment and accumulated dose.
        amount *= HbmArmorProtection.radiationMultiplier(living);
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

    /**
     * Exact 1.7.10 ContaminationUtil.isRadImmune membership.  The old
     * interface was marker-based; the modern port has no common interface, so
     * the concrete legacy classes are enumerated explicitly.
     */
    public static boolean isLegacyRadiationImmune(LivingEntity living) {
        return living instanceof com.reinhardt.hbm.entity.LegacyNuclearCreeperEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyTaintedCreeperEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyCyberCrabEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyMaskManEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyRadBeastEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyUfoEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyChopperEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyWormHeadEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyWormBodyEntity
                || living instanceof com.reinhardt.hbm.entity.LegacyQuackosEntity
                || living instanceof MushroomCow
                || living instanceof Zombie
                || living instanceof net.minecraft.world.entity.monster.Skeleton
                || living instanceof net.minecraft.world.entity.animal.Ocelot;
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
        return living instanceof ServerPlayer;
    }

    /** The four entity substitutions at the start of 1.7.10 EntityEffectHandler.handleRadiationEffect. */
    private static boolean applyLegacyRadiationTransformations(LivingEntity living, HbmLivingRadiation data) {
        if (!living.isAlive() || living instanceof Player player && player.isCreative()) {
            return false;
        }

        float radiation = data.getRadiation();
        ServerLevel level = (ServerLevel) living.level();
        if (living.getClass() == Creeper.class && radiation >= 200.0F && living.getHealth() > 0.0F) {
            if (level.random.nextInt(3) == 0) {
                com.reinhardt.hbm.entity.LegacyNuclearCreeperEntity nuclear =
                        new com.reinhardt.hbm.entity.LegacyNuclearCreeperEntity(HbmEntityTypes.NUCLEAR_CREEPER.get(), level);
                nuclear.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot());
                level.addFreshEntity(nuclear);
                living.discard();
            } else {
                living.hurt(living.damageSources().source(HbmDamageTypes.RADIATION), 100.0F);
            }
            return true;
        }
        if (living instanceof Cow && !(living instanceof MushroomCow) && radiation >= 50.0F) {
            MushroomCow mooshroom = new MushroomCow(net.minecraft.world.entity.EntityType.MOOSHROOM, level);
            mooshroom.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot());
            level.addFreshEntity(mooshroom);
            living.discard();
            return true;
        }
        if (living instanceof Villager && radiation >= 500.0F) {
            Zombie zombie = new Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
            zombie.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot());
            level.addFreshEntity(zombie);
            living.discard();
            return true;
        }
        if (living.getClass() == com.reinhardt.hbm.entity.LegacyDuckEntity.class && radiation >= 200.0F) {
            com.reinhardt.hbm.entity.LegacyQuackosEntity quackos =
                    new com.reinhardt.hbm.entity.LegacyQuackosEntity(HbmEntityTypes.QUACKOS.get(), level);
            quackos.moveTo(living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot());
            level.addFreshEntity(quackos);
            living.discard();
            return true;
        }
        return false;
    }

    private static void applyRadiationEffects(LivingEntity living, HbmLivingRadiation data) {
        if (!living.isAlive() || !canReceiveDose(living)) {
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
            // Let the normal hurt/death pipeline decide the outcome.  A
            // post-hurt setHealth(0) would incorrectly kill entities rescued
            // by a totem or the legacy revive armor hook.
            living.hurt(living.damageSources().source(HbmDamageTypes.RADIATION), 1000.0F);
            data.setRadiation(0.0F);
            if (living instanceof Player player) {
                HbmAdvancements.award(player, "rad_death");
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
            if (living instanceof Player player) {
                HbmAdvancements.award(player, "rad_poison");
            }
        }
    }

    private static void applyDigammaEffects(LivingEntity living, HbmLivingRadiation data) {
        if (!living.isAlive() || !canReceiveDose(living)) {
            return;
        }

        float digamma = data.getDigamma();
        if (digamma < 0.01F) {
            return;
        }
        if (living instanceof Player player) {
            HbmAdvancements.award(player, "digamma_see");
            if (digamma >= 2.0F) {
                HbmAdvancements.award(player, "digamma_feel");
            }
            if (digamma >= 10.0F) {
                HbmAdvancements.award(player, "digamma_know");
            }
        }
        if (digamma >= HbmLivingRadiation.MAX_DIGAMMA || living.getMaxHealth() <= 0.0F) {
            // Do not force a second death after hurt: the damage event may be
            // canceled by a revive item or consumed by a totem.
            living.hurt(living.damageSources().source(HbmDamageTypes.DIGAMMA), 5_000_000.0F);
            // Clear the trigger even when a totem prevents death; otherwise
            // the same entity would be killed again on the next tick.
            data.setDigamma(0.0F);
        }
    }
}
