package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.entity.LegacyMobEquipment;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class LogicBlockEntity extends BlockEntity {
    public int phase;
    public int timer;
    public String actionID = "FODDER_WAVE";
    public String conditionID = "PLAYER_CUBE_5";
    public String interactionID = "";
    public Direction direction = Direction.SOUTH;
    public String disguise = "";
    public int disguiseMeta;

    public LogicBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LOGIC_BLOCK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LogicBlockEntity logic) {
        if (level.isClientSide) {
            return;
        }
        logic.actionID = WandLogicBlockEntity.normalizeActionId(logic.actionID);
        logic.conditionID = WandLogicBlockEntity.normalizeConditionId(logic.conditionID);
        logic.interactionID = WandLogicBlockEntity.normalizeInteractionId(logic.interactionID);
        logic.runAction(level, pos);
        if (logic.checkCondition(level, pos)) {
            logic.phase++;
            logic.timer = 0;
        } else {
            logic.timer++;
        }
        logic.setChanged();
    }

    public boolean interact(Player player) {
        if (this.level == null || this.level.isClientSide) {
            return false;
        }
        String interaction = WandLogicBlockEntity.normalizeInteractionId(this.interactionID);
        if ("TEST".equals(interaction)) {
            if (this.phase > 1) {
                return true;
            }
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!held.isEmpty() && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
            this.phase++;
            sync();
            return true;
        }
        if ("RADAWAY_INJECTOR".equals(interaction)) {
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!isItem(held, "key")) {
                return true;
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            HbmLivingRadiation data = HbmLivingRadiation.get(player);
            data.setRadiation(0.0F);
            data.setRadiationBuffer(0.0F);
            data.setEnvironmentRadiation(0.0F);
            HbmLivingRadiation.set(player, data);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("[RAD CONTAINMENT SYSTEM] Radiation treatment administered"), false);
            this.phase = 2;
            this.timer = 0;
            sync();
            return true;
        }
        if ("POWER_LOCK".equals(interaction)) {
            Optional<PowerEndpoint> endpoint = adjacentPowerEndpoint();
            if (endpoint.isEmpty() || endpoint.get().getAvailableOutput() <= 500_000L) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("[POWER LOCK] Charge adjacent energy storage to at least 500KHE to release emergency lock"), false);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("[POWER LOCK] Power Restored! Safe Unlocked!"), false);
                this.phase++;
                this.timer = 0;
                sync();
            }
            return true;
        }
        return !interaction.isBlank();
    }

    private void runAction(Level level, BlockPos pos) {
        switch (WandLogicBlockEntity.normalizeActionId(this.actionID)) {
            case "FODDER_WAVE" -> actionFodderWave(level, pos);
            case "POWER_LOCK" -> actionPowerLock(level, pos);
            case "COLLAPSE_ROOF_RAD_5" -> actionCollapseRoof(level, pos, 4);
            case "COLLAPSE_ROOF_RAD_10" -> actionCollapseRoof(level, pos, 8);
            case "BOMB_TRAP" -> actionBombTrap(level, pos);
            case "BOMB_CRANE" -> actionBombCrane(level, pos);
            case "DEAD_GUY_CRANE" -> actionDeadGuyCrane(level, pos);
            case "SKELETON_GUN_TIER_1" -> actionSkeletons(level, pos, 1);
            case "SKELETON_GUN_TIER_2" -> actionSkeletons(level, pos, 2);
            case "SKELETON_GUN_TIER_3" -> actionSkeletons(level, pos, 3);
            case "ZOMBIE_TIER_1" -> actionZombies(level, pos, false);
            case "ZOMBIE_TIER_2" -> actionZombies(level, pos, true);
            case "ABERRATOR" -> actionAberrator(level, pos);
            case "PUZZLE_TEST" -> actionPuzzleTest(level, pos);
            case "MISSILE_STRIKE" -> actionMissileStrike(level, pos);
            case "IRRADIATE_ENTITIES_AOE" -> actionIrradiateEntities(level, pos);
            default -> {
            }
        }
    }

    private boolean checkCondition(Level level, BlockPos pos) {
        return switch (WandLogicBlockEntity.normalizeConditionId(this.conditionID)) {
            case "EMPTY" -> false;
            case "PLAYER_CUBE_3" -> hasPlayerCube(level, pos, 3);
            case "PLAYER_CUBE_5" -> hasPlayerCube(level, pos, 5);
            case "PLAYER_CUBE_25" -> hasPlayerCube(level, pos, 25);
            case "REDSTONE" -> level.hasNeighborSignal(pos);
            case "BOMB_CRANE" -> conditionBombCrane(level, pos);
            case "ABERRATOR" -> conditionAberrator(level, pos);
            case "PUZZLE_TEST" -> conditionPuzzleTest(level, pos);
            default -> false;
        };
    }

    private void actionFodderWave(Level level, BlockPos pos) {
        if (this.phase != 1) {
            return;
        }
        spawnFodderWave(level, pos);
        setLegacyBlock(level, pos, "block_steel", 0);
    }

    private void actionPowerLock(Level level, BlockPos pos) {
        if (this.phase == 0 && hasPlayerCube(level, pos, 3)) {
            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 300.0D, false);
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("[POWER LOCK] Low Power Warning! Locking Safe"), false);
            }
            this.phase++;
            this.timer = 0;
        }
    }

    private void actionCollapseRoof(Level level, BlockPos pos, int radius) {
        if (this.phase == 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int halfRadiusSquared = radius * radius / 2;
        for (int dx = -radius; dx < radius; dx++) {
            int xx = dx * dx;
            for (int dy = -radius; dy < radius; dy++) {
                int yy = xx + dy * dy;
                for (int dz = -radius; dz < radius; dz++) {
                    if (yy + dz * dz >= halfRadiusSquared) {
                        continue;
                    }
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0.0F || targetState.getExplosionResistance(level, target, null) > 70.0F) {
                        continue;
                    }
                    FallingBlockEntity falling = FallingBlockEntity.fall(serverLevel, target, targetState);
                    falling.time = 1;
                }
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void actionBombTrap(Level level, BlockPos pos) {
        if (this.phase != 1) {
            return;
        }
        Direction direction = this.direction.getOpposite();
        setLegacyBlock(level, pos.relative(direction), "charge_c4", 2);
        Block disguise = disguiseBlock();
        if (disguise == Blocks.AIR) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            level.setBlock(pos, HbmLegacyNbtTemplate.stateFromLegacyId(BuiltInRegistries.BLOCK.getKey(disguise).toString(), this.disguiseMeta), 3);
        }
    }

    private void actionBombCrane(Level level, BlockPos pos) {
        if (this.phase == 0) {
            setLegacyBlock(level, pos.above(), "charge_c4", Direction.UP.get3DDataValue());
        } else if (this.phase >= 1) {
            setLegacyBlock(level, pos, "block_steel", 0);
        }
    }

    private void actionDeadGuyCrane(Level level, BlockPos pos) {
        if (this.phase == 1) {
            setLegacyBlock(level, pos, "skeleton_holder", 0);
        }
    }

    private void actionSkeletons(Level level, BlockPos pos, int tier) {
        if (this.phase != 1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Skeleton skeleton = new Skeleton(EntityType.SKELETON, level);
            skeleton.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
            LegacyMobEquipment.assignLogicSkeletonTier(skeleton, tier);
            level.addFreshEntity(skeleton);
            // LogicBlockActions set the action block to air at the end of
            // each loop iteration, after spawning that skeleton.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void actionZombies(Level level, BlockPos pos, boolean advanced) {
        if (this.phase != 1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
            zombie.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
            if (advanced) {
                LegacyMobEquipment.assignAdvanced(zombie);
            } else {
                LegacyMobEquipment.assignCommon(zombie);
            }
            level.addFreshEntity(zombie);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void actionAberrator(Level level, BlockPos pos) {
        if ((this.phase == 1 || this.phase == 2) && this.timer == 0) {
            spawnAberratorWave(level, pos);
        }
        if (this.phase > 2) {
            BlockEntity target = level.getBlockEntity(pos.above(18));
            if (target instanceof LegacyDisplayStandBlockEntity stand) {
                stand.setDisplayedItem(level.random.nextInt(5) == 0
                        ? LegacyVariantItem.stackFor(HbmItems.ITEM_SECRET.get(), "aberrator")
                        : new ItemStack(HbmItems.CLAY_TABLET.get()));
            }
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }

    private void actionPuzzleTest(Level level, BlockPos pos) {
        if (this.phase == 2) {
            setLegacyBlock(level, pos, "crate_steel", 0);
        }
    }

    private void actionMissileStrike(Level level, BlockPos pos) {
        if (this.phase != 1) {
            return;
        }
        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0D, false);
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("[COMMAND UNIT] Missile Fired"), false);
        }
        setLegacyBlock(level, pos, "block_electrical_scrap", 0);
    }

    private void actionIrradiateEntities(Level level, BlockPos pos) {
        Direction direction = this.direction.getOpposite();
        Direction rot = direction.getClockWise();
        AABB box = new AABB(
                pos.getX() - rot.getStepX(),
                pos.getY() - 1,
                pos.getZ() - rot.getStepZ(),
                pos.getX() + rot.getStepX() + direction.getStepX() * 15,
                pos.getY() + 1,
                pos.getZ() + rot.getStepZ() + direction.getStepZ() * 15
        ).inflate(2.0D, 2.0D, 2.0D);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        Vec3 source = Vec3.atCenterOf(pos);
        for (LivingEntity entity : entities) {
            Vec3 delta = entity.getEyePosition().subtract(source);
            double len = Math.max(1.0D, delta.length());
            Vec3 step = delta.normalize();
            float resistance = 0.0F;
            for (int i = 1; i < len; i++) {
                BlockPos check = BlockPos.containing(source.add(step.scale(i)));
                resistance += level.getBlockState(check).getExplosionResistance(level, check, null);
            }
            resistance = Math.max(1.0F, resistance);
            float dose = (float) (100.0D / resistance / (len * len));
            HbmLivingRadiation data = HbmLivingRadiation.get(entity);
            data.addRadiationWithReadout(dose);
            HbmLivingRadiation.set(entity, data);
        }
        if (this.phase == 2 && this.timer > 40) {
            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0D, false);
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("[RAD CONTAINMENT SYSTEM] Diagnostics found containment failure, commencing lockdown"), false);
            }
            this.phase = 3;
            this.timer = 0;
        }
    }

    private boolean conditionBombCrane(Level level, BlockPos pos) {
        if (this.phase == 0) {
            setLegacyBlock(level, pos.above(), "charge_c4", Direction.UP.get3DDataValue());
        }
        return hasPlayerCube(level, pos, 10);
    }

    private boolean conditionAberrator(Level level, BlockPos pos) {
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return false;
        }
        boolean playerNear = hasPlayerCube(level, pos, 10);
        if (this.phase == 0) {
            return level.getGameTime() % 20L == 0L && playerNear;
        }
        if (this.phase < 3) {
            return level.getGameTime() % 20L == 0L && this.timer >= 60
                    && noAberratorSoldiersRemain(level, pos) && playerNear;
        }
        return false;
    }

    private boolean conditionPuzzleTest(Level level, BlockPos pos) {
        if (this.phase == 0 && level.hasNeighborSignal(pos)) {
            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0D, false);
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Find a great ancient weapon, of questionable use in the modern age"), false);
            }
            setLegacyBlock(level, pos.above(), "pedestal", 0);
            return true;
        }
        if (this.phase != 1) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos.above());
        if (blockEntity instanceof net.minecraft.world.Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (isItem(container.getItem(slot), "big_sword")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasPlayerCube(Level level, BlockPos pos, int radius) {
        // 1.7.10: getBoundingBox(x, y, z, x + 1, y - 2, z + 1).expand(radius, radius, radius).
        AABB box = new AABB(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + 1.0D + radius, pos.getY() - 2.0D + radius, pos.getZ() + 1.0D + radius);
        return !level.getEntitiesOfClass(Player.class, box).isEmpty();
    }

    /** LogicBlockActions.FODDER_WAVE, retaining its original single origin-height lookup. */
    private void spawnFodderWave(Level level, BlockPos pos) {
        double vectorX = 5.0D;
        double vectorZ = 0.0D;
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                pos.getX(), pos.getZ());
        double cosine = Math.cos(Math.toRadians(36.0D));
        double sine = Math.sin(Math.toRadians(36.0D));
        for (int index = 0; index < 10; index++) {
            Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
            zombie.moveTo(pos.getX() + 0.5D + vectorX, surfaceY,
                    pos.getZ() + 0.5D + vectorZ, index * 36.0F, 0.0F);
            LegacyMobEquipment.assignAdvanced(zombie);
            level.addFreshEntity(zombie);
            double rotatedX = vectorX * cosine + vectorZ * sine;
            vectorZ = vectorZ * cosine - vectorX * sine;
            vectorX = rotatedX;
        }
    }

    /** LogicBlockActions.PHASE_ABERRATOR's separate 20-block soldier wave. */
    private void spawnAberratorWave(Level level, BlockPos pos) {
        Player target = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25.0D, false);
        double vectorX = 20.0D;
        double vectorZ = 0.0D;
        double cosine = Math.cos(Math.toRadians(36.0D));
        double sine = Math.sin(Math.toRadians(36.0D));
        for (int index = 0; index < 10; index++) {
            if (vectorX > 8.0D) {
                vectorX += level.random.nextInt(10) - 5;
            }
            double x = pos.getX() + 0.5D + vectorX;
            double z = pos.getZ() + 0.5D + vectorZ;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    (int) x, (int) z);
            for (int attempt = 0; attempt < 7; attempt++) {
                LegacyUndeadSoldierEntity soldier = new LegacyUndeadSoldierEntity(HbmEntityTypes.UNDEAD_SOLDIER.get(), level);
                soldier.moveTo(x, y, z, index * 36.0F, 0.0F);
                if (!canSpawnAberratorSoldier(level, soldier)) {
                    continue;
                }
                EventHooks.finalizeMobSpawn(soldier, (ServerLevel) level,
                        level.getCurrentDifficultyAt(soldier.blockPosition()), MobSpawnType.EVENT, null);
                if (target != null) {
                    soldier.setTarget(target);
                }
                level.addFreshEntity(soldier);
                break;
            }
            double rotatedX = vectorX * cosine + vectorZ * sine;
            vectorZ = vectorZ * cosine - vectorX * sine;
            vectorX = rotatedX;
        }
    }

    private static boolean canSpawnAberratorSoldier(Level level, LegacyUndeadSoldierEntity soldier) {
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && level.noCollision(soldier)
                && level.getEntities(soldier, soldier.getBoundingBox()).isEmpty()
                && !level.containsAnyLiquid(soldier.getBoundingBox());
    }

    private static boolean noAberratorSoldiersRemain(Level level, BlockPos pos) {
        // 1.7.10 starts with a reversed X extent, then expands it by (50, 20, 50).
        AABB box = new AABB(pos.getX() - 50.0D, pos.getY() - 20.0D, pos.getZ() - 20.0D,
                pos.getX() + 48.0D, pos.getY() + 21.0D, pos.getZ() + 21.0D);
        return level.getEntitiesOfClass(LegacyUndeadSoldierEntity.class, box).isEmpty();
    }

    private void setLegacyBlock(Level level, BlockPos pos, String id, int meta) {
        BlockState state = HbmLegacyNbtTemplate.stateFromLegacyId("reinhardtshbm:" + id, meta);
        if (state.is(Blocks.BARRIER)) {
            state = Blocks.AIR.defaultBlockState();
        }
        level.setBlock(pos, state, 3);
    }

    private Optional<PowerEndpoint> adjacentPowerEndpoint() {
        if (this.level == null) {
            return Optional.empty();
        }
        for (Direction side : Direction.values()) {
            BlockEntity blockEntity = this.level.getBlockEntity(this.worldPosition.relative(side));
            if (blockEntity instanceof PowerEndpoint endpoint) {
                return Optional.of(endpoint);
            }
        }
        return Optional.empty();
    }

    private static boolean isItem(ItemStack stack, String path) {
        return !stack.isEmpty() && itemById(path).map(stack::is).orElse(false);
    }

    private static Optional<Item> itemById(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == net.minecraft.world.item.Items.AIR ? Optional.empty() : Optional.of(item);
    }

    public Block disguiseBlock() {
        ResourceLocation id = ResourceLocation.tryParse(HbmStructureIO.normalizeId(this.disguise));
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id) : Blocks.AIR;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("phase", this.phase);
        tag.putInt("timer", this.timer);
        tag.putString("actionID", WandLogicBlockEntity.normalizeActionId(this.actionID));
        tag.putString("conditionID", WandLogicBlockEntity.normalizeConditionId(this.conditionID));
        String interaction = WandLogicBlockEntity.normalizeInteractionId(this.interactionID);
        if (!interaction.isBlank()) {
            tag.putString("interactionID", interaction);
        }
        tag.putInt("direction", this.direction.get3DDataValue());
        if (this.disguise != null && !this.disguise.isBlank()) {
            tag.putString("disguise", this.disguise);
            tag.putInt("disguiseMeta", this.disguiseMeta);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.phase = tag.getInt("phase");
        this.timer = tag.getInt("timer");
        this.actionID = WandLogicBlockEntity.normalizeActionId(tag.getString("actionID"));
        this.conditionID = WandLogicBlockEntity.normalizeConditionId(tag.getString("conditionID"));
        this.interactionID = WandLogicBlockEntity.normalizeInteractionId(tag.getString("interactionID"));
        this.direction = Direction.from3DDataValue(tag.getInt("direction"));
        this.disguise = tag.getString("disguise");
        this.disguiseMeta = tag.getInt("disguiseMeta");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
