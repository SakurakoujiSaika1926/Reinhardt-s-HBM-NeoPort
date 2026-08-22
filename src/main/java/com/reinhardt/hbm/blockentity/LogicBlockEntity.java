package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
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
            case "SKELETON_GUN_TIER_1", "SKELETON_GUN_TIER_2", "SKELETON_GUN_TIER_3" -> actionSkeletons(level, pos);
            case "ZOMBIE_TIER_1", "ZOMBIE_TIER_2" -> actionZombies(level, pos);
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
        spawnZombieRing(level, pos, 10, 5.0D);
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

    private void actionSkeletons(Level level, BlockPos pos) {
        if (this.phase != 1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Skeleton skeleton = EntityType.SKELETON.create(level);
            if (skeleton != null) {
                skeleton.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(skeleton);
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void actionZombies(Level level, BlockPos pos) {
        if (this.phase != 1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(zombie);
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void actionAberrator(Level level, BlockPos pos) {
        if ((this.phase == 1 || this.phase == 2) && this.timer == 0) {
            spawnZombieRing(level, pos, 10, 20.0D);
        }
        if (this.phase > 2) {
            setLegacyBlock(level, pos.above(18), "skeleton_holder", 0);
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }

    private void actionPuzzleTest(Level level, BlockPos pos) {
        if (this.phase == 2) {
            setLegacyBlock(level, pos, "crate_steel", 0);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof net.minecraft.world.Container container) {
                itemById("gun_bolter").ifPresent(item -> container.setItem(Math.min(15, container.getContainerSize() - 1), new ItemStack(item)));
            }
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
            data.addRadiation(dose);
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
            return level.getGameTime() % 20L == 0L && this.timer >= 60 && playerNear;
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
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() - 2.0D, pos.getZ() + 1.0D)
                .inflate(radius, radius, radius);
        return !level.getEntitiesOfClass(Player.class, box, player -> !player.isSpectator()).isEmpty();
    }

    private void spawnZombieRing(Level level, BlockPos pos, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(i * (360.0D / count));
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                int x = pos.getX() + (int) Math.round(Math.cos(angle) * radius);
                int z = pos.getZ() + (int) Math.round(Math.sin(angle) * radius);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                zombie.moveTo(x + 0.5D, y, z + 0.5D, (float) (i * (360.0D / count)), 0.0F);
                level.addFreshEntity(zombie);
            }
        }
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
