package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HbmLegacyJigsawGenerator {
    private static final String PROFILE_BRICKS = "bricks";
    private static final String PROFILE_CRATES = "crates";
    private static final String PROFILE_OOZE = "ooze";
    private static final Map<String, Pool> METEOR_POOLS = meteorPools();

    private HbmLegacyJigsawGenerator() {
    }

    static boolean canGenerate(HbmLegacyStructureSelection.SelectedStructure selected) {
        return selected.jigsaw() && "meteor_dungeon".equals(selected.spawnName());
    }

    static List<HbmLegacyNbtPiece> generate(
            HbmLegacyStructureSelection.SelectedStructure selected,
            BlockPos origin,
            int rotation,
            RandomSource random
    ) {
        if (!canGenerate(selected)) {
            return List.of();
        }

        Pool startPool = METEOR_POOLS.get(selected.startPool());
        if (startPool == null) {
            return List.of();
        }

        Piece startPiece = startPool.copy().get(random);
        if (startPiece == null) {
            return List.of();
        }

        List<Component> components = new ArrayList<>();
        List<Component> queuedComponents = new ArrayList<>();
        Component startComponent = new Component(startPiece, origin.getX(), origin.getY(), origin.getZ(), rotation);
        components.add(startComponent);
        queuedComponents.add(startComponent);

        while (!queuedComponents.isEmpty()) {
            queuedComponents.sort((a, b) -> Integer.compare(b.priority, a.priority));
            int matchPriority = queuedComponents.getFirst().priority;
            int max = 1;
            while (max < queuedComponents.size() && queuedComponents.get(max).priority == matchPriority) {
                max++;
            }

            Component fromComponent = queuedComponents.remove(random.nextInt(max));
            List<List<HbmLegacyNbtTemplate.JigsawConnection>> fromConnections = fromComponent.template().fromConnections();
            if (fromConnections.isEmpty()) {
                continue;
            }

            int distance = distanceTo(fromComponent.box, origin);
            boolean fallbacksOnly = components.size() >= selected.sizeLimit()
                    || distance >= selected.rangeLimit()
                    || components.size() > 1024;

            for (List<HbmLegacyNbtTemplate.JigsawConnection> unshuffled : fromConnections) {
                List<HbmLegacyNbtTemplate.JigsawConnection> connections = new ArrayList<>(unshuffled);
                shuffle(connections, random);

                for (HbmLegacyNbtTemplate.JigsawConnection fromConnection : connections) {
                    if (fromConnection == fromComponent.connectedFrom) {
                        continue;
                    }

                    Pool sourcePool = METEOR_POOLS.get(fromConnection.poolName());
                    if (sourcePool == null) {
                        ReinhardtsHBM.LOGGER.warn("[Jigsaw] Jigsaw block points to invalid pool: {}", fromConnection.poolName());
                        continue;
                    }

                    if (fallbacksOnly) {
                        addFallback(selected, components, sourcePool, fromComponent, fromConnection, random);
                        continue;
                    }

                    Pool nextPool = sourcePool.copy();
                    Component nextComponent = null;
                    while (nextPool.totalWeight > 0) {
                        nextComponent = buildNextComponent(nextPool, fromComponent, fromConnection, random);
                        if (nextComponent != null && !intersectsAny(components, nextComponent.box, fromComponent)) {
                            break;
                        }
                        nextComponent = null;
                    }

                    if (nextComponent != null) {
                        nextComponent.priority = fromConnection.placementPriority();
                        components.add(nextComponent);
                        queuedComponents.add(nextComponent);
                        continue;
                    }

                    if (sourcePool.fallback != null) {
                        BlockPos target = connectionTargetPosition(fromComponent, fromConnection);
                        if (!insideAny(components, target, fromComponent)) {
                            addFallback(selected, components, sourcePool, fromComponent, fromConnection, random);
                        }
                    }
                }
            }
        }

        List<HbmLegacyNbtPiece> pieces = new ArrayList<>(components.size());
        for (Component component : components) {
            pieces.add(new HbmLegacyNbtPiece(
                    selected.spawnName(),
                    component.piece.templateName,
                    new BlockPos(component.box.minX(), component.box.minY(), component.box.minZ()),
                    component.rotation,
                    component.piece.heightOffset,
                    component.piece.conformToTerrain,
                    component.box,
                    component.piece.replacementProfile
            ));
        }
        return pieces;
    }

    static HbmLegacyNbtTemplate.BlockReplacement replacement(String profile) {
        return (blockName, meta, state, random) -> switch (profile == null ? "" : profile) {
            case PROFILE_BRICKS -> replaceBricks(blockName, state, random);
            case PROFILE_CRATES -> replaceCrates(blockName, state, random);
            case PROFILE_OOZE -> replaceOoze(blockName, state, random);
            default -> state;
        };
    }

    private static BlockState replaceCrates(String blockName, BlockState state, RandomSource random) {
        if ("reinhardtshbm:crate".equals(blockName)) {
            float chance = random.nextFloat();
            if (chance < 0.6F) {
                return Blocks.AIR.defaultBlockState();
            }
            if (chance < 0.8F) {
                return HbmBlocks.CRATE_AMMO.get().defaultBlockState();
            }
            if (chance < 0.9F) {
                return HbmBlocks.CRATE_CAN.get().defaultBlockState();
            }
            return HbmBlocks.CRATE.get().defaultBlockState();
        }
        if ("reinhardtshbm:meteor_spawner".equals(blockName)) {
            return random.nextFloat() < 0.8F
                    ? HbmBlocks.METEOR_BRICK.get().defaultBlockState()
                    : HbmBlocks.METEOR_SPAWNER.get().defaultBlockState();
        }
        return replaceBricks(blockName, state, random);
    }

    private static BlockState replaceOoze(String blockName, BlockState state, RandomSource random) {
        if ("reinhardtshbm:concrete_colored".equals(blockName)) {
            return random.nextFloat() < 0.8F
                    ? HbmBlocks.TOXIC_BLOCK.get().defaultBlockState()
                    : HbmBlocks.METEOR_POLISHED.get().defaultBlockState();
        }
        return replaceBricks(blockName, state, random);
    }

    private static BlockState replaceBricks(String blockName, BlockState state, RandomSource random) {
        if (!"reinhardtshbm:meteor_brick".equals(blockName)) {
            return state;
        }

        float chance = random.nextFloat();
        if (chance < 0.4F) {
            return HbmBlocks.METEOR_BRICK.get().defaultBlockState();
        }
        if (chance < 0.7F) {
            return HbmBlocks.METEOR_BRICK_MOSSY.get().defaultBlockState();
        }
        return HbmBlocks.METEOR_BRICK_CRACKED.get().defaultBlockState();
    }

    private static void addFallback(
            HbmLegacyStructureSelection.SelectedStructure selected,
            List<Component> components,
            Pool sourcePool,
            Component fromComponent,
            HbmLegacyNbtTemplate.JigsawConnection fromConnection,
            RandomSource random
    ) {
        if (sourcePool.fallback == null) {
            return;
        }

        Pool fallback = METEOR_POOLS.get(sourcePool.fallback);
        if (fallback == null) {
            return;
        }

        Component component = buildNextComponent(fallback.copy(), fromComponent, fromConnection, random);
        if (component != null) {
            component.priority = fromConnection.placementPriority();
            components.add(component);
        }
    }

    private static Component buildNextComponent(
            Pool pool,
            Component fromComponent,
            HbmLegacyNbtTemplate.JigsawConnection fromConnection,
            RandomSource random
    ) {
        Piece nextPiece = pool.get(random);
        if (nextPiece == null) {
            ReinhardtsHBM.LOGGER.warn("[Jigsaw] Pool returned null piece: {}", fromConnection.poolName());
            return null;
        }

        HbmLegacyNbtTemplate nextTemplate = HbmLegacyNbtTemplate.get(nextPiece.templateName);
        List<HbmLegacyNbtTemplate.JigsawConnection> connectionPool = nextTemplate.getConnectionPool(
                fromConnection.direction(),
                fromConnection.targetName()
        );
        if (connectionPool.isEmpty()) {
            ReinhardtsHBM.LOGGER.warn("[Jigsaw] No valid connections for {} in piece {}", fromConnection.targetName(), nextPiece.name);
            return null;
        }

        HbmLegacyNbtTemplate.JigsawConnection toConnection = connectionPool.get(random.nextInt(connectionPool.size()));
        int nextRotation = nextRotation(fromComponent, fromConnection, toConnection, random);
        BlockPos target = connectionTargetPosition(fromComponent, fromConnection);
        int offsetX = HbmLegacyNbtTemplate.rotateX(toConnection.x(), toConnection.z(), nextRotation, nextTemplate.sizeX(), nextTemplate.sizeZ());
        int offsetZ = HbmLegacyNbtTemplate.rotateZ(toConnection.x(), toConnection.z(), nextRotation, nextTemplate.sizeX(), nextTemplate.sizeZ());
        return new Component(
                nextPiece,
                target.getX() - offsetX,
                target.getY() - toConnection.y(),
                target.getZ() - offsetZ,
                nextRotation
        ).connectedFrom(toConnection);
    }

    private static int nextRotation(
            Component fromComponent,
            HbmLegacyNbtTemplate.JigsawConnection fromConnection,
            HbmLegacyNbtTemplate.JigsawConnection toConnection,
            RandomSource random
    ) {
        if (fromConnection.direction() == Direction.DOWN || fromConnection.direction() == Direction.UP) {
            return fromConnection.rollable() ? random.nextInt(4) : fromComponent.rotation;
        }
        return directionOffsetToRotation(fromConnection.direction().getOpposite(), toConnection.direction(), fromComponent.rotation);
    }

    private static int directionOffsetToRotation(Direction from, Direction to, int baseRotation) {
        Direction current = from;
        for (int i = 0; i < 4; i++) {
            if (current == to) {
                return (i + baseRotation) & 3;
            }
            current = current.getCounterClockWise();
        }
        return baseRotation;
    }

    private static BlockPos connectionTargetPosition(Component component, HbmLegacyNbtTemplate.JigsawConnection connection) {
        Direction extendDirection = rotateDirection(connection.direction(), component.rotation);
        HbmLegacyNbtTemplate template = component.template();
        return new BlockPos(
                component.box.minX() + HbmLegacyNbtTemplate.rotateX(connection.x(), connection.z(), component.rotation, template.sizeX(), template.sizeZ()) + extendDirection.getStepX(),
                component.box.minY() + connection.y() + extendDirection.getStepY(),
                component.box.minZ() + HbmLegacyNbtTemplate.rotateZ(connection.x(), connection.z(), component.rotation, template.sizeX(), template.sizeZ()) + extendDirection.getStepZ()
        );
    }

    private static Direction rotateDirection(Direction direction, int rotation) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return direction;
        }
        return switch (rotation & 3) {
            case 1 -> direction.getClockWise();
            case 2 -> direction.getOpposite();
            case 3 -> direction.getCounterClockWise();
            default -> direction;
        };
    }

    private static boolean intersectsAny(List<Component> components, BoundingBox box, Component ignored) {
        for (Component component : components) {
            if (component == ignored) {
                continue;
            }
            if (intersects(component.box, box)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideAny(List<Component> components, BlockPos pos, Component ignored) {
        for (Component component : components) {
            if (component == ignored) {
                continue;
            }
            if (component.box.isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(BoundingBox a, BoundingBox b) {
        return a.minX() <= b.maxX()
                && a.maxX() >= b.minX()
                && a.minY() <= b.maxY()
                && a.maxY() >= b.minY()
                && a.minZ() <= b.maxZ()
                && a.maxZ() >= b.minZ();
    }

    private static int distanceTo(BoundingBox box, BlockPos origin) {
        int centerX = box.minX() + (box.maxX() - box.minX() + 1) / 2;
        int centerZ = box.minZ() + (box.maxZ() - box.minZ() + 1) / 2;
        return Math.max(Math.abs(centerX - origin.getX()), Math.abs(centerZ - origin.getZ()));
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size(); i > 1; i--) {
            int swap = random.nextInt(i);
            T value = list.get(i - 1);
            list.set(i - 1, list.get(swap));
            list.set(swap, value);
        }
    }

    private static Map<String, Pool> meteorPools() {
        Map<String, Pool> pools = new HashMap<>();
        pools.put("start", pool().add(piece("meteor_core", "meteor/meteor-core", PROFILE_BRICKS), 1));
        pools.put("spike", pool().add(piece("meteor_spike", "meteor/meteor-spike", "", -3, true), 1));
        pools.put("default", pool()
                .add(piece("meteor_corner", "meteor/meteor-corner", PROFILE_BRICKS), 2)
                .add(piece("meteor_t", "meteor/meteor-t", PROFILE_BRICKS), 3)
                .add(piece("meteor_stairs", "meteor/meteor-stairs", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_base_thru", "meteor/room10/room-base-thru", PROFILE_BRICKS), 3)
                .add(piece("meteor_room_base_end", "meteor/room10/room-base-end", PROFILE_BRICKS), 4)
                .fallback("fallback"));
        pools.put("10room", pool()
                .add(piece("meteor_room_basic", "meteor/room10/room-basic", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_balcony", "meteor/room10/room-balcony", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_dragon", "meteor/room10/room-dragon", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_ladder", "meteor/room10/room-ladder", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_ooze", "meteor/room10/room-ooze", PROFILE_OOZE), 1)
                .add(piece("meteor_room_split", "meteor/room10/room-split", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_stairs", "meteor/room10/room-stairs", PROFILE_BRICKS), 1)
                .add(piece("meteor_room_triple", "meteor/room10/room-triple", PROFILE_BRICKS), 1)
                .fallback("roomback"));
        pools.put("3x3loot", pool()
                .add(piece("meteor_3_bale", "meteor/loot3x3/meteor-3-bale", ""), 1)
                .add(piece("meteor_3_blank", "meteor/loot3x3/meteor-3-blank", ""), 1)
                .add(piece("meteor_3_block", "meteor/loot3x3/meteor-3-block", ""), 1)
                .add(piece("meteor_3_crab", "meteor/loot3x3/meteor-3-crab", ""), 1)
                .add(piece("meteor_3_crab_tesla", "meteor/loot3x3/meteor-3-crab-tesla", ""), 1)
                .add(piece("meteor_3_crate", "meteor/loot3x3/meteor-3-crate", ""), 1)
                .add(piece("meteor_3_dirt", "meteor/loot3x3/meteor-3-dirt", ""), 1)
                .add(piece("meteor_3_lead", "meteor/loot3x3/meteor-3-lead", ""), 1)
                .add(piece("meteor_3_ooze", "meteor/loot3x3/meteor-3-ooze", ""), 1)
                .add(piece("meteor_3_pillar", "meteor/loot3x3/meteor-3-pillar", ""), 1)
                .add(piece("meteor_3_star", "meteor/loot3x3/meteor-3-star", ""), 1)
                .add(piece("meteor_3_tesla", "meteor/loot3x3/meteor-3-tesla", ""), 1)
                .add(piece("meteor_3_book", "meteor/loot3x3/meteor-3-book", ""), 1)
                .add(piece("meteor_3_mku", "meteor/loot3x3/meteor-3-mku", ""), 1)
                .add(piece("meteor_3_statue", "meteor/loot3x3/meteor-3-statue", ""), 1)
                .add(piece("meteor_3_glow", "meteor/loot3x3/meteor-3-glow", ""), 1)
                .fallback("3x3loot"));
        pools.put("headloot", pool()
                .add(piece("meteor_dragon_chest", "meteor/room10/headloot/loot-chest", PROFILE_CRATES), 1)
                .add(piece("meteor_dragon_tesla", "meteor/room10/headloot/loot-tesla", PROFILE_CRATES), 1)
                .add(piece("meteor_dragon_trap", "meteor/room10/headloot/loot-trap", PROFILE_CRATES), 1)
                .add(piece("meteor_dragon_crate_crab", "meteor/room10/headloot/loot-crate-crab", PROFILE_CRATES), 1)
                .fallback("headback"));
        pools.put("fallback", pool().add(piece("meteor_fallback", "meteor/meteor-fallback", PROFILE_BRICKS), 1));
        pools.put("roomback", pool().add(piece("meteor_room_fallback", "meteor/room10/room-fallback", PROFILE_BRICKS), 1));
        pools.put("headback", pool().add(piece("meteor_loot_fallback", "meteor/room10/headloot/loot-fallback", PROFILE_CRATES), 1));
        return Map.copyOf(pools);
    }

    private static Pool pool() {
        return new Pool();
    }

    private static Piece piece(String name, String templateName, String replacementProfile) {
        return piece(name, templateName, replacementProfile, 0, false);
    }

    private static Piece piece(String name, String templateName, String replacementProfile, int heightOffset, boolean conformToTerrain) {
        return new Piece(name, templateName, replacementProfile, heightOffset, conformToTerrain);
    }

    private static final class Pool {
        private final List<Entry> entries = new ArrayList<>();
        private int totalWeight;
        private String fallback;
        private boolean clone;

        private Pool add(Piece piece, int weight) {
            if (weight <= 0) {
                throw new IllegalArgumentException("Jigsaw pool spawn weight must be positive");
            }
            this.entries.add(new Entry(piece, weight));
            this.totalWeight += weight;
            return this;
        }

        private Pool fallback(String fallback) {
            this.fallback = fallback;
            return this;
        }

        private Pool copy() {
            Pool copy = new Pool();
            copy.entries.addAll(this.entries);
            copy.totalWeight = this.totalWeight;
            copy.fallback = this.fallback;
            copy.clone = true;
            return copy;
        }

        private Piece get(RandomSource random) {
            if (this.totalWeight <= 0) {
                return null;
            }

            int weight = random.nextInt(this.totalWeight);
            for (int i = 0; i < this.entries.size(); i++) {
                Entry entry = this.entries.get(i);
                weight -= entry.weight;
                if (weight < 0) {
                    if (this.clone) {
                        this.entries.remove(i);
                        this.totalWeight -= entry.weight;
                    }
                    return entry.piece;
                }
            }
            return null;
        }
    }

    private record Entry(Piece piece, int weight) {
    }

    private record Piece(String name, String templateName, String replacementProfile, int heightOffset, boolean conformToTerrain) {
    }

    private static final class Component {
        private final Piece piece;
        private final int rotation;
        private final BoundingBox box;
        private int priority;
        private HbmLegacyNbtTemplate.JigsawConnection connectedFrom;

        private Component(Piece piece, int x, int y, int z, int rotation) {
            this.piece = piece;
            this.rotation = rotation & 3;
            HbmLegacyNbtTemplate template = template();
            this.box = new BoundingBox(
                    x,
                    y,
                    z,
                    x + template.rotatedSizeX(this.rotation) - 1,
                    y + template.sizeY() - 1,
                    z + template.rotatedSizeZ(this.rotation) - 1
            );
        }

        private Component connectedFrom(HbmLegacyNbtTemplate.JigsawConnection connection) {
            this.connectedFrom = connection;
            return this;
        }

        private HbmLegacyNbtTemplate template() {
            return HbmLegacyNbtTemplate.get(this.piece.templateName);
        }
    }
}
