package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.explosion.BalefireExplosionManager;
import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.block.TaintBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Exact dispatch point for EntityMissileBaseNT's per-warhead impact hooks. */
public final class LegacyLauncherImpact {
    private LegacyLauncherImpact() {}

    public static void impact(LegacyLauncherMissileEntity missile, BlockHitResult hit) {
        Level level = missile.level();
        Vec3 pos = hit == null ? missile.position() : hit.getLocation();
        String id = missile.missileId();
        if (missile.missileItem().getItem() instanceof CustomMissileItem) {
            CustomMissileData data = CustomMissileData.read(missile.missileItem());
            if (data == null) throw new IllegalStateException("Invalid assembled missile at impact");
            custom(missile, data, pos);
            return;
        }
        if (!(level instanceof ServerLevel server)) return;
        switch (id) {
            case "missile_anti_ballistic" -> antiBallisticImpact(server, missile, pos);
            case "missile_test" -> missileTest(server, pos);
            case "missile_micro" -> NukeExplosionManager.scheduleMicroNuke(server, pos.x, pos.y + .5D, pos.z, 35);
            case "missile_schrabidium" -> {
                if (!NukeExplosionManager.isFleijaSuppressed(server, BlockPos.containing(pos)))
                    NukeExplosionManager.scheduleFleijaNuke(server, pos.x, pos.y, pos.z, 20);
            }
            case "missile_bhole" -> {
                LegacyProjectileUtil.standardExplosion(missile, pos, 1.5F, true);
                LegacyBlackHoleEntity.spawn(level, pos, 1.5F, false);
            }
            case "missile_taint" -> taintMissile(server, missile, pos);
            case "missile_emp", "missile_emp_strong" -> {
                BlockPos empPos = BlockPos.containing(pos);
                // 1.7.10 uses the dedicated ExplosionNukeGeneric EMP blast;
                // keep this on the explicit legacy EMP path rather than the
                // modern redstone-bomb helper.
                if (id.endsWith("strong")) {
                    LegacyEmpEntity.spawnPersistent(server, pos.x, pos.y, pos.z);
                } else {
                    LegacyEmpUtil.empBlast(server, empPos.getX(), empPos.getY(), empPos.getZ(), 50);
                    LegacyEmpEntity.spawnBlast(server, pos.x, pos.y, pos.z, 100);
                }
            }
            case "missile_generic" -> { standard(missile, pos, 15, 24, false); effectSmall(server, pos); }
            case "missile_stealth" -> { standard(missile, pos, 20, 24, false); effectStandard(server, pos); }
            case "missile_incendiary" -> { standard(missile, pos, 15, 24, true); effectSmall(server, pos); }
            case "missile_cluster" -> { level.explode(missile, pos.x, pos.y, pos.z, 5, true, Level.ExplosionInteraction.TNT); ClusterSubmunitionEntity.spawn(missile.level(), pos, 25, missile.getYRot(), missile.getXRot()); }
            case "missile_buster" -> verticalBuster(missile, pos, 5.0F, 15);
            // EntityMissileDecoy used newExplosion(..., 4F, false, false).
            case "missile_decoy" -> level.explode(missile, pos.x, pos.y, pos.z, 4, false, Level.ExplosionInteraction.NONE);
            case "missile_strong" -> { standard(missile, pos, 30, 32, false); effectStandard(server, pos); }
            case "missile_incendiary_strong" -> { standard(missile, pos, 30, 32, true); effectStandard(server, pos); LegacyProjectileUtil.igniteAllBlocksLegacy(server, BlockPos.containing(pos), 25); }
            case "missile_cluster_strong" -> { level.explode(missile, pos.x, pos.y, pos.z, 15, true, Level.ExplosionInteraction.TNT); ClusterSubmunitionEntity.spawn(missile.level(), pos, 50, missile.getYRot(), missile.getXRot()); }
            case "missile_buster_strong" -> busterWithLegacyEffects(missile, pos, 7.5F, 20, 8);
            case "missile_burst" -> { standard(missile, pos, 50, 48, false); effectLarge(server, pos); }
            case "missile_inferno" -> { standard(missile, pos, 50, 48, true); effectLarge(server, pos); LegacyProjectileUtil.igniteAllBlocksLegacy(level, BlockPos.containing(pos), 10); }
            case "missile_rain" -> { level.explode(missile, pos.x, pos.y, pos.z, 25, true, Level.ExplosionInteraction.TNT); ClusterSubmunitionEntity.spawn(missile.level(), pos, 100, missile.getYRot(), missile.getXRot()); }
            case "missile_drill" -> drill(missile, pos);
            case "missile_nuclear" -> NukeExplosionManager.scheduleMk5Nuclear(server, pos.x, pos.y, pos.z, 100);
            case "missile_nuclear_cluster" -> NukeExplosionManager.scheduleMirv(server, pos.x, pos.y, pos.z, 100);
            case "missile_doomsday" -> NukeExplosionManager.scheduleDoomsday(server, pos.x, pos.y, pos.z, 200);
            case "missile_doomsday_rusted" -> NukeExplosionManager.scheduleRustedDoomsday(server, pos.x, pos.y, pos.z, 100);
            case "missile_volcano" -> volcano(server, pos);
            case "missile_shuttle" -> shuttleImpact(server, missile, pos);
            default -> throw new IllegalStateException("Unmapped legacy missile impact: " + id);
        }
    }

    private static void standard(Entity source, Vec3 pos, float strength, int resolution, boolean fire) {
        // EntityMissileBaseNT.explodeStandard has a dedicated resolution per
        // tier and no-drop processor; never route it through shared defaults.
        LegacyProjectileUtil.standardLegacyMissileExplosion(source, pos, strength, resolution, fire);
    }
    private static void effectSmall(ServerLevel level, Vec3 pos) {
        LegacyProjectileUtil.composeExplosionEffect(level, pos, 10, 2.0F, 0.5F, 25.0F, 5, 8, 20, 0.75F, 1.0F, -2.0F, 150.0F);
    }
    private static void effectStandard(ServerLevel level, Vec3 pos) {
        LegacyProjectileUtil.composeExplosionEffect(level, pos, 15, 5.0F, 1.0F, 45.0F, 10, 16, 50, 1.0F, 3.0F, -2.0F, 200.0F);
    }
    private static void effectLarge(ServerLevel level, Vec3 pos) {
        LegacyProjectileUtil.composeExplosionEffect(level, pos, 30, 6.5F, 2.0F, 65.0F, 25, 16, 50, 1.25F, 3.0F, -2.0F, 350.0F);
    }
    private static void shuttleImpact(ServerLevel level, Entity source, Vec3 pos) {
        standard(source, pos.add(0.5D, 0.5D, 0.5D), 20, 64, false);
        level.sendParticles(com.reinhardt.hbm.registry.HbmParticleTypes.RBMK_MUSH.get(), pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D, 0, 10.0D, 0.0D, 0.0D, 1.0D);
    }
    private static void buster(Entity source, Vec3 pos, float strength, int depth) {
        Vec3 v = source.getDeltaMovement().normalize();
        for (int i = 0; i < depth; i += 2) source.level().explode(null, pos.x + v.x * i, pos.y + v.y * i, pos.z + v.z * i, strength, Level.ExplosionInteraction.TNT);
    }
    private static void verticalBuster(Entity source, Vec3 pos, float strength, int depth) {
        for (int i = 0; i < depth; i++) {
            source.level().explode(source, pos.x, pos.y - i, pos.z, strength, true, Level.ExplosionInteraction.TNT);
        }
    }
    private static void missileTest(ServerLevel level, Vec3 pos) {
        // EntityMissileTest is a direct Sellafield conversion pass, not an
        // explosion.  Preserve its 50-block spherical traversal and replace
        // solid blocks with the modern slaked Sellafield block.
        int x0 = (int) Math.floor(pos.x), y0 = (int) Math.floor(pos.y), z0 = (int) Math.floor(pos.z);
        for (int dx = -50; dx <= 50; dx++) for (int dy = -50; dy <= 50; dy++) for (int dz = -50; dz <= 50; dz++) {
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > 50.0D) continue;
            BlockPos target = new BlockPos(x0 + dx, y0 + dy, z0 + dz);
            BlockState state = level.getBlockState(target);
            if (state.isCollisionShapeFullBlock(level, target)) {
                level.setBlock(target, HbmBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
            } else {
                level.setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
    private static void taint(ServerLevel level, Vec3 pos, int radius) {
        for (int i = 0; i < radius * 10; i++) {
            int x = level.random.nextInt(radius) + (int) pos.x - (radius / 2 - 1);
            int y = level.random.nextInt(radius) + (int) pos.y - (radius / 2 - 1);
            int z = level.random.nextInt(radius) + (int) pos.z - (radius / 2 - 1);
            BlockPos p = new BlockPos(x, y, z);
            if (!level.getBlockState(p).isAir()) level.setBlock(p, HbmBlocks.TAINT.get().defaultBlockState()
                    .setValue(TaintBlock.AGE, level.random.nextInt(3) + 4), 2);
        }
    }

    private static void taintMissile(ServerLevel level, Entity source, Vec3 pos) {
        // EntityMissileTaint: explosion first, then exactly 100 attempts in
        // the 11x11x11 cube around the block hit, with legacy taint age 4..6.
        level.explode(source, pos.x, pos.y, pos.z, 5.0F, true, Level.ExplosionInteraction.TNT);
        BlockPos hit = BlockPos.containing(pos);
        for (int i = 0; i < 100; i++) {
            BlockPos target = new BlockPos(hit.getX() + level.random.nextInt(11) - 5,
                    hit.getY() + level.random.nextInt(11) - 5,
                    hit.getZ() + level.random.nextInt(11) - 5);
            if (level.getBlockState(target).isCollisionShapeFullBlock(level, target)
                    && !level.getBlockState(target).isAir()) {
                level.setBlock(target, HbmBlocks.TAINT.get().defaultBlockState()
                        .setValue(TaintBlock.AGE, level.random.nextInt(3) + 4), 2);
            }
        }
    }

    private static void busterWithLegacyEffects(Entity source, Vec3 pos, float strength, int depth, int shrapnelCount) {
        verticalBuster(source, pos, strength, depth);
        // Tier2 adds the same standard smoke/shrapnel effect after its 20
        // separate downward explosions.
        LegacyProjectileUtil.sendSmallExplosionEffect(source.level(), pos, 8, 2.5F, 1.0F);
        LegacyProjectileUtil.spawnShrapnel(source.level(), pos, shrapnelCount);
    }

    private static void drill(Entity source, Vec3 pos) {
        // Tier3 intentionally uses thirty separate downward explosions. A
        // single radius/depth helper changes the old per-step collision.
        for (int i = 0; i < 30; i++) {
            Vec3 step = new Vec3(pos.x, pos.y - i, pos.z);
            source.level().explode(source, step.x, step.y, step.z, 10.0F, true, Level.ExplosionInteraction.TNT);
        }
        LegacyProjectileUtil.sendSmallExplosionEffect(source.level(), pos, 25, 2.5F, 1.0F);
        LegacyProjectileUtil.spawnShrapnel(source.level(), pos, 12);
    }

    private static void volcano(ServerLevel level, Vec3 pos) {
        level.explode(null, pos.x, pos.y, pos.z, 10.0F, true, Level.ExplosionInteraction.TNT);
        int x0 = (int) Math.floor(pos.x);
        int y0 = (int) Math.floor(pos.y);
        int z0 = (int) Math.floor(pos.z);
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            level.setBlock(new BlockPos(x0 + x, y0 + y, z0 + z), HbmBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState(), 3);
        level.setBlock(new BlockPos(x0, y0, z0), HbmBlocks.VOLCANO_CORE.get().defaultBlockState(), 3);
    }

    private static void custom(LegacyLauncherMissileEntity m, CustomMissileData data, Vec3 pos) {
        ServerLevel level = (ServerLevel)m.level();
        MissilePartItem.Warhead w = MissilePartItem.definition(data.warhead()).warhead();
        float s = MissilePartItem.definition(data.warhead()).primary();
        switch (w) {
            case HE -> {
                LegacyProjectileUtil.legacyLargeMissileWarheadExplosion(m, pos, s, false);
                jolt(level, pos, s);
                effectLarge(level, pos);
            }
            case INC -> {
                LegacyProjectileUtil.legacyLargeMissileWarheadExplosion(m, pos, s, true);
                jolt(level, pos, s * 1.5F);
                effectLarge(level, pos);
            }
            case BUSTER -> { buster(m, pos, s, Math.max(1, (int)(s * 4))); effectLarge(level, pos); }
            case NUCLEAR, TX -> NukeExplosionManager.scheduleMk5Nuclear(level, pos.x, pos.y, pos.z, Math.max(1, (int)s));
            case N2 -> NukeExplosionManager.scheduleMk5NoRadiation(level, pos.x, pos.y, pos.z, Math.max(1, (int)s));
            case BALEFIRE -> BalefireExplosionManager.schedule(level, BlockPos.containing(pos), Math.max(1, (int)s));
            case TAINT -> taint(level, pos, Math.max(1, (int)s));
            // EntityMissileCustom's CLUSTER hook is intentionally empty in
            // 1.7.10; preserve that exact behaviour (no invented payload).
            case CLUSTER -> { }
            case CLOUD -> customCloud(m, pos);
            case TURBINE -> turbine(level, m, pos, Math.max(1, (int) s));
            // These values are not present in 1.7.10 ItemCustomMissilePart.
            // Refuse them explicitly instead of silently producing a wrong
            // generic blast.
            case SCHRAB, MIRV, VOLCANO -> throw new IllegalStateException(
                    "Unregistered 1.7.10 custom warhead type: " + w);
        }
    }

    private static void customCloud(LegacyLauncherMissileEntity missile, Vec3 pos) {
        if (!(missile.level() instanceof ServerLevel level)) {
            return;
        }
        // ExplosionChaos.spawnPoisonCloud(..., 750, 2.5, 2): this is a
        // client-only effect in the old source, so emit the matching legacy
        // pink-cloud particle directly rather than creating a gameplay gas.
        Vec3 effect = pos.subtract(missile.getDeltaMovement());
        level.sendParticles(com.reinhardt.hbm.registry.HbmParticleTypes.LEGACY_PINK_CLOUD.get(),
                effect.x, effect.y, effect.z, 750, 0.0D, 0.0D, 0.0D, 2.5D);
    }
    private static void turbine(ServerLevel level, Entity source, Vec3 pos, int count) {
        // Dedicated ExplosionLarge + rotating blade projectiles from the old
        // TURBINE payload. LegacyShrapnelEntity is the modern registered
        // projectile carrying the same collision/damage semantics.
        LegacyProjectileUtil.legacyLargeMissileWarheadExplosion(source, pos, 10.0F, false);
        Vec3 base = new Vec3(0.5D, 0.0D, 0.0D);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 motion = base.yRot((float) angle);
            level.addFreshEntity(new LegacyShrapnelEntity(level, pos.x - source.getDeltaMovement().x,
                    pos.y - source.getDeltaMovement().y + level.random.nextGaussian(),
                    pos.z - source.getDeltaMovement().z, motion, false));
        }
    }
    private static void jolt(ServerLevel level, Vec3 pos, float strength) {
        double radius = Math.max(1.0D, strength);
        for (Entity target : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(pos, pos).inflate(radius), Entity::isAlive)) {
            Vec3 delta = target.position().subtract(pos);
            double distance = delta.length();
            if (distance < 1.0E-4D || distance > radius) continue;
            target.setDeltaMovement(target.getDeltaMovement().add(delta.normalize().scale((radius - distance) / radius * 0.25D)));
            target.hurtMarked = true;
        }
    }
    public static void antiBallistic(LegacyLauncherMissileEntity missile, float radius) {
        if (missile.level() instanceof ServerLevel server)
            LegacyProjectileUtil.fixedDamageExplosion(server, missile, missile.position(), radius, 0, true);
    }
    private static void antiBallisticImpact(ServerLevel level, Entity source, Vec3 pos) {
        // EntityMissileAntiBallistic.onImpact: ExplosionLarge radius 20,
        // cloud/shrapnel disabled, block destruction enabled.
        level.explode(source, pos.x, pos.y, pos.z, 20.0F, true, Level.ExplosionInteraction.TNT);
    }
    public static void destroyInFlight(LegacyLauncherMissileEntity missile) {
        // Old killMissile() used ExplosionLarge, not the shared vanilla path.
        LegacyProjectileUtil.legacyLargeMissileWarheadExplosion(missile, missile.position(), 5F, false);
        LegacyProjectileUtil.spawnShrapnelShower(missile.level(), missile.position(), missile.getDeltaMovement(), 15, .075D);
        if (missile.level() instanceof ServerLevel level) {
            spawnLegacyDebris(level, missile);
        }
    }

    private static void spawnLegacyDebris(ServerLevel level, LegacyLauncherMissileEntity missile) {
        String id = missile.missileId();
        java.util.List<ItemStack> stacks = switch (id) {
            case "missile_stealth" -> java.util.List.of(
                    FoundryShapeItem.stackFor(HbmItems.BOLT.get(), FoundryMaterial.get("steel"), 4));
            case "missile_shuttle" -> java.util.List.of(item("plate_steel", 8), item("thruster_medium", 2), item("canister_empty", 1), new ItemStack(net.minecraft.world.item.Items.GLASS_PANE, 2));
            case "missile_generic", "missile_incendiary", "missile_cluster", "missile_buster", "missile_decoy" -> java.util.List.of(item("plate_titanium", 4), item("thruster_small", 1));
            case "missile_strong", "missile_incendiary_strong", "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong" -> java.util.List.of(item("plate_steel", 10), item("plate_titanium", 6), item("thruster_medium", 1));
            case "missile_burst", "missile_inferno", "missile_rain", "missile_drill" -> java.util.List.of(item("plate_steel", 16), item("plate_titanium", 10), item("thruster_large", 1));
            case "missile_nuclear", "missile_nuclear_cluster", "missile_volcano", "missile_doomsday", "missile_doomsday_rusted" -> java.util.List.of(item("plate_titanium", 16), item("plate_steel", 20), item("plate_aluminium", 12), item("thruster_large", 1));
            default -> java.util.List.of(item("wire_fine", 4), item("plate_titanium", 4),
                    FoundryShapeItem.stackFor(HbmItems.SHELL.get(), FoundryMaterial.get("steel"), 2), item("ducttape", 1));
        };
        for (ItemStack stack : stacks) {
            ItemEntity entity = new ItemEntity(level, missile.getX(), missile.getY(), missile.getZ(), stack);
            entity.setDeltaMovement(level.random.nextGaussian() * 0.25D,
                    0.25D + level.random.nextDouble() * 0.35D,
                    level.random.nextGaussian() * 0.25D);
            level.addFreshEntity(entity);
        }
    }

    private static ItemStack item(String path, int count) {
        var key = ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path);
        var item = BuiltInRegistries.ITEM.get(key);
        if (item == net.minecraft.world.item.Items.AIR) {
            throw new IllegalStateException("Missing legacy missile debris item: " + key);
        }
        return new ItemStack(item, count);
    }
}
