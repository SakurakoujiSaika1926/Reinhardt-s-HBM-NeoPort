package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class HbmToolBehavior {
    private static final String AREA_KEY = "hbm_area";
    private static final String HARVEST_KEY = "hbm_harvest";
    private static final int MAX_EXTRA_BLOCKS = 192;
    private static final ThreadLocal<Boolean> BREAKING_EXTRA = ThreadLocal.withInitial(() -> false);

    private HbmToolBehavior() {
    }

    public enum AreaAbility {
        RECURSION("tool.ability.reinhardtshbm.recursion", new float[]{3, 4, 5, 6, 7, 9, 10}),
        HAMMER("tool.ability.reinhardtshbm.hammer", new float[]{1, 2, 3, 4}),
        HAMMER_FLAT("tool.ability.reinhardtshbm.hammer_flat", new float[]{1, 2, 3, 4}),
        EXPLOSION("tool.ability.reinhardtshbm.explosion", new float[]{2.5F, 5.0F, 10.0F, 15.0F});

        private final String key;
        private final float[] values;

        AreaAbility(String key, float[] values) {
            this.key = key;
            this.values = values;
        }

        private Component label(int level) {
            return Component.translatable(this.key, valueAt(this.values, level));
        }
    }

    public enum HarvestAbility {
        SILK("tool.ability.reinhardtshbm.silk", null),
        LUCK("tool.ability.reinhardtshbm.luck", new float[]{1, 2, 3, 4, 5, 9}),
        SMELTER("tool.ability.reinhardtshbm.smelter", null),
        SHREDDER("tool.ability.reinhardtshbm.shredder", null),
        CENTRIFUGE("tool.ability.reinhardtshbm.centrifuge", null),
        CRYSTALLIZER("tool.ability.reinhardtshbm.crystallizer", null),
        MERCURY("tool.ability.reinhardtshbm.mercury", null);

        private final String key;
        private final float[] values;

        HarvestAbility(String key, float[] values) {
            this.key = key;
            this.values = values;
        }

        private Component label(int level) {
            if (this.values == null) {
                return Component.translatable(this.key);
            }
            return Component.translatable(this.key, (int) valueAt(this.values, level));
        }
    }

    public enum WeaponAbility {
        RADIATION("weapon.ability.reinhardtshbm.radiation", new float[]{15.0F, 50.0F, 500.0F}),
        VAMPIRE("weapon.ability.reinhardtshbm.vampire", new float[]{2.0F, 3.0F, 5.0F, 10.0F, 50.0F}),
        STUN("weapon.ability.reinhardtshbm.stun", new float[]{2.0F, 3.0F, 5.0F, 10.0F, 15.0F}),
        PHOSPHORUS("weapon.ability.reinhardtshbm.phosphorus", new float[]{60.0F, 90.0F}),
        FIRE("weapon.ability.reinhardtshbm.fire", new float[]{5.0F, 10.0F}),
        CHAINSAW("weapon.ability.reinhardtshbm.chainsaw", new float[]{15.0F, 10.0F}),
        BEHEADER("weapon.ability.reinhardtshbm.beheader", null),
        BOBBLE("weapon.ability.reinhardtshbm.bobble", null);

        private final String key;
        private final float[] values;

        WeaponAbility(String key, float[] values) {
            this.key = key;
            this.values = values;
        }

        private Component label(int level) {
            if (this.values == null) {
                return Component.translatable(this.key);
            }
            float value = valueAt(this.values, level);
            if (value == (int) value) {
                return Component.translatable(this.key, (int) value);
            }
            return Component.translatable(this.key, value);
        }
    }

    public enum SpecialBehavior {
        NONE,
        REDSTONE_SWORD,
        MYSTERY_SHOVEL,
        SHIMMER_AXE
    }

    public static Item.Properties properties(HbmToolProfile profile) {
        Item.Properties properties = new Item.Properties()
                .stacksTo(1)
                .rarity(profile.rarity())
                .attributes(attributes(profile));
        return properties;
    }

    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, HbmToolProfile profile) {
        ItemStack stack = player.getItemInHand(hand);
        if (!profile.hasCyclableAbilities()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                cycleArea(stack, player, profile);
            } else {
                cycleHarvest(stack, player, profile);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static InteractionResult useOn(UseOnContext context, HbmToolProfile profile) {
        return switch (profile.specialBehavior()) {
            case REDSTONE_SWORD -> useRedstoneSword(context);
            case MYSTERY_SHOVEL -> useMysteryShovel(context);
            case SHIMMER_AXE -> useShimmerAxe(context);
            case NONE -> InteractionResult.PASS;
        };
    }

    public static void addTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag, HbmToolProfile profile) {
        AbilityLine area = activeArea(stack, profile);
        AbilityLine harvest = activeHarvest(stack, profile);
        if (area != null) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.active_area", area.label()).withStyle(ChatFormatting.GOLD));
        }
        if (harvest != null) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.active_harvest", harvest.label()).withStyle(ChatFormatting.GOLD));
        }

        for (HbmToolProfile.AbilityLevel<AreaAbility> ability : profile.areaAbilities()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.area", ability.ability().label(ability.level())).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (HbmToolProfile.AbilityLevel<HarvestAbility> ability : profile.harvestAbilities()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.harvest", ability.ability().label(ability.level())).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (HbmToolProfile.AbilityLevel<WeaponAbility> ability : profile.weaponAbilities()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.weapon", ability.ability().label(ability.level())).withStyle(ChatFormatting.GRAY));
        }
        if (profile.depthRockBreaker()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.depth_rock").withStyle(ChatFormatting.RED));
        }
        if (profile.hasCyclableAbilities()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.tool.cycle").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean isFoil(ItemStack stack, HbmToolProfile profile) {
        return activeArea(stack, profile) != null || activeHarvest(stack, profile) != null;
    }

    public static void postHurt(ItemStack stack, LivingEntity target, LivingEntity attacker, HbmToolProfile profile) {
        if (attacker.level().isClientSide) {
            return;
        }

        for (HbmToolProfile.AbilityLevel<WeaponAbility> ability : profile.weaponAbilities()) {
            applyWeaponAbility(ability.ability(), ability.level(), target, attacker);
        }

        if (profile.specialBehavior() == SpecialBehavior.SHIMMER_AXE && target.isAlive()) {
            target.setHealth(Math.max(0.0F, target.getHealth() * 0.5F));
        }
    }

    public static void afterMine(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner, HbmToolProfile profile) {
        if (level.isClientSide || BREAKING_EXTRA.get() || !(miner instanceof ServerPlayer player)) {
            return;
        }

        AbilityLine active = activeArea(stack, profile);
        if (active == null || state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return;
        }

        if (active.areaAbility() == AreaAbility.EXPLOSION) {
            float strength = valueAt(AreaAbility.EXPLOSION.values, active.level());
            level.explode(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, strength, false, Level.ExplosionInteraction.BLOCK);
            return;
        }

        BREAKING_EXTRA.set(true);
        try {
            if (active.areaAbility() == AreaAbility.RECURSION) {
                breakRecursive(stack, player, level, state, pos, (int) valueAt(AreaAbility.RECURSION.values, active.level()), profile);
            } else if (active.areaAbility() == AreaAbility.HAMMER) {
                breakCube(stack, player, level, state, pos, (int) valueAt(AreaAbility.HAMMER.values, active.level()), profile);
            } else if (active.areaAbility() == AreaAbility.HAMMER_FLAT) {
                breakFlat(stack, player, level, state, pos, (int) valueAt(AreaAbility.HAMMER_FLAT.values, active.level()), profile);
            }
        } finally {
            BREAKING_EXTRA.set(false);
        }
    }

    public static float minerDestroySpeed(ItemStack stack, BlockState state, HbmToolProfile profile) {
        if (!profile.miner()) {
            return 1.0F;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return profile.tier().getSpeed();
        }
        return 1.0F;
    }

    public static boolean isCorrectMinerTool(BlockState state, Tier tier, boolean miner) {
        if (!miner) {
            return false;
        }
        if (!(state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL))) {
            return false;
        }
        return !state.is(tier.getIncorrectBlocksForDrops());
    }

    private static ItemAttributeModifiers attributes(HbmToolProfile profile) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, profile.attackDamage(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, profile.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );
        if (profile.movementModifier() != 0.0D) {
            builder.add(
                    Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(ReinhardtsHBM.id("tool_movement"), profile.movementModifier(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    EquipmentSlotGroup.MAINHAND
            );
        }
        return builder.build();
    }

    private static void applyWeaponAbility(WeaponAbility ability, int level, LivingEntity target, LivingEntity attacker) {
        switch (ability) {
            case RADIATION -> {
                if (target.isAlive()) {
                    HbmLivingRadiation data = HbmLivingRadiation.get(target);
                    data.addRadiation(valueAt(WeaponAbility.RADIATION.values, level));
                    HbmLivingRadiation.set(target, data);
                }
            }
            case VAMPIRE -> {
                if (target.isAlive()) {
                    float amount = valueAt(WeaponAbility.VAMPIRE.values, level);
                    target.hurt(target.damageSources().magic(), amount);
                    attacker.heal(amount);
                }
            }
            case STUN -> {
                int duration = (int) valueAt(WeaponAbility.STUN.values, level) * 20;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 4));
            }
            case PHOSPHORUS -> {
                int duration = (int) valueAt(WeaponAbility.PHOSPHORUS.values, level) * 20;
                target.igniteForTicks(duration);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(duration, 20 * 20), 1));
            }
            case FIRE -> target.igniteForTicks((int) valueAt(WeaponAbility.FIRE.values, level) * 20);
            case CHAINSAW -> {
                if (!target.isAlive()) {
                    attacker.heal(1.0F);
                }
            }
            case BEHEADER, BOBBLE -> {
            }
        }
    }

    private static void breakRecursive(ItemStack stack, ServerPlayer player, Level level, BlockState originState, BlockPos origin, int radius, HbmToolProfile profile) {
        if (originState.is(Blocks.STONE) || originState.is(Blocks.NETHERRACK)) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        int broken = 0;

        while (!queue.isEmpty() && broken < MAX_EXTRA_BLOCKS) {
            BlockPos current = queue.removeFirst();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (!visited.add(next) || next.distSqr(origin) > radius * radius) {
                            continue;
                        }
                        BlockState nextState = level.getBlockState(next);
                        if (!nextState.is(originState.getBlock())) {
                            continue;
                        }
                        if (tryBreakExtra(stack, player, level, originState, origin, next, profile)) {
                            broken++;
                            queue.addLast(next);
                        }
                    }
                }
            }
        }
    }

    private static void breakCube(ItemStack stack, ServerPlayer player, Level level, BlockState originState, BlockPos origin, int range, HbmToolProfile profile) {
        int broken = 0;
        for (int dx = -range; dx <= range && broken < MAX_EXTRA_BLOCKS; dx++) {
            for (int dy = -range; dy <= range && broken < MAX_EXTRA_BLOCKS; dy++) {
                for (int dz = -range; dz <= range && broken < MAX_EXTRA_BLOCKS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    if (tryBreakExtra(stack, player, level, originState, origin, origin.offset(dx, dy, dz), profile)) {
                        broken++;
                    }
                }
            }
        }
    }

    private static void breakFlat(ItemStack stack, ServerPlayer player, Level level, BlockState originState, BlockPos origin, int range, HbmToolProfile profile) {
        Vec3 look = player.getLookAngle();
        Direction.Axis axis;
        if (Math.abs(look.y) > Math.max(Math.abs(look.x), Math.abs(look.z))) {
            axis = Direction.Axis.Y;
        } else {
            axis = player.getDirection().getAxis();
        }

        int broken = 0;
        for (int a = -range; a <= range && broken < MAX_EXTRA_BLOCKS; a++) {
            for (int b = -range; b <= range && broken < MAX_EXTRA_BLOCKS; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos target = switch (axis) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                };
                if (tryBreakExtra(stack, player, level, originState, origin, target, profile)) {
                    broken++;
                }
            }
        }
    }

    private static boolean tryBreakExtra(ItemStack stack, ServerPlayer player, Level level, BlockState originState, BlockPos origin, BlockPos target, HbmToolProfile profile) {
        if (target.equals(origin)) {
            return false;
        }

        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0.0F) {
            return false;
        }

        float originSpeed = Math.max(0.01F, originState.getDestroySpeed(level, origin));
        float targetSpeed = targetState.getDestroySpeed(level, target);
        if (targetSpeed / originSpeed > 10.0F) {
            return false;
        }

        boolean correctTool = stack.isCorrectToolForDrops(targetState)
                || isCorrectMinerTool(targetState, profile.tier(), profile.miner())
                || !targetState.requiresCorrectToolForDrops();
        if (!correctTool) {
            return false;
        }

        return player.gameMode.destroyBlock(target);
    }

    private static InteractionResult useRedstoneSword(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        BlockState wireState = Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.POWER, 0);
        if (!level.getBlockState(placePos).canBeReplaced() || !wireState.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.setBlock(placePos, wireState, 3);
            context.getItemInHand().hurtAndBreak(14, (ServerLevel) level, (ServerPlayer) player, item -> player.onEquippedItemBroken(item, context.getHand() == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult useMysteryShovel(UseOnContext context) {
        Level level = context.getLevel();
        Optional<net.minecraft.world.level.block.Block> ntmDirt = BuiltInRegistries.BLOCK.getOptional(ReinhardtsHBM.id("ntm_dirt"));
        if (ntmDirt.isEmpty() || !level.getBlockState(context.getClickedPos()).is(ntmDirt.get())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.destroyBlock(context.getClickedPos(), false, context.getPlayer(), 1);
            BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id("ingot_u238m2")).ifPresent(item -> {
                for (int i = 0; i < 3; i++) {
                    level.addFreshEntity(new ItemEntity(level, context.getClickedPos().getX() + 0.5D, context.getClickedPos().getY() + 0.5D, context.getClickedPos().getZ() + 0.5D, new ItemStack(item)));
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult useShimmerAxe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).isAir()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            destroyWeakNoDrop(level, pos);
            destroyWeakNoDrop(level, pos.above());
            destroyWeakNoDrop(level, pos.below());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void destroyWeakNoDrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000.0F) {
            level.destroyBlock(pos, false);
        }
    }

    private static void cycleArea(ItemStack stack, Player player, HbmToolProfile profile) {
        if (profile.areaAbilities().isEmpty()) {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.tool.no_area"), true);
            return;
        }
        int next = nextIndex(index(stack, AREA_KEY), profile.areaAbilities().size());
        setIndex(stack, AREA_KEY, next);
        Component label = next < 0 ? Component.translatable("tool.ability.reinhardtshbm.none") : profile.areaAbilities().get(next).ability().label(profile.areaAbilities().get(next).level());
        player.displayClientMessage(Component.translatable("chat.reinhardtshbm.tool.area", label), true);
    }

    private static void cycleHarvest(ItemStack stack, Player player, HbmToolProfile profile) {
        if (profile.harvestAbilities().isEmpty()) {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.tool.no_harvest"), true);
            return;
        }
        int next = nextIndex(index(stack, HARVEST_KEY), profile.harvestAbilities().size());
        setIndex(stack, HARVEST_KEY, next);
        Component label = next < 0 ? Component.translatable("tool.ability.reinhardtshbm.none") : profile.harvestAbilities().get(next).ability().label(profile.harvestAbilities().get(next).level());
        player.displayClientMessage(Component.translatable("chat.reinhardtshbm.tool.harvest", label), true);
    }

    private static AbilityLine activeArea(ItemStack stack, HbmToolProfile profile) {
        int index = index(stack, AREA_KEY);
        if (index < 0 || index >= profile.areaAbilities().size()) {
            return null;
        }
        HbmToolProfile.AbilityLevel<AreaAbility> ability = profile.areaAbilities().get(index);
        return new AbilityLine(ability.ability(), null, ability.level(), ability.ability().label(ability.level()));
    }

    private static AbilityLine activeHarvest(ItemStack stack, HbmToolProfile profile) {
        int index = index(stack, HARVEST_KEY);
        if (index < 0 || index >= profile.harvestAbilities().size()) {
            return null;
        }
        HbmToolProfile.AbilityLevel<HarvestAbility> ability = profile.harvestAbilities().get(index);
        return new AbilityLine(null, ability.ability(), ability.level(), ability.ability().label(ability.level()));
    }

    private static int nextIndex(int current, int size) {
        int next = current + 1;
        return next >= size ? -1 : next;
    }

    private static int index(ItemStack stack, String key) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : -1;
    }

    private static void setIndex(ItemStack stack, String key, int index) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (index < 0) {
            tag.remove(key);
        } else {
            tag.putInt(key, index);
        }
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    private static float valueAt(float[] values, int level) {
        return values[Math.max(0, Math.min(values.length - 1, level))];
    }

    private record AbilityLine(AreaAbility areaAbility, HarvestAbility harvestAbility, int level, Component label) {
    }
}
