package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.block.TrappedBrickBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of HBM 1.7.10's CellularDungeonFactory.jungle setup and
 * JungleDungeon/JungleDungeonRoom variants.  The old generator used queued
 * timed jobs; this class keeps the same queue ordering and only clips writes to
 * the current structure piece bounding box.
 */
final class HbmLegacyJungleDungeonGenerator {
    private static final int WIDTH = 5;
    private static final int HEIGHT = 5;
    private static final int DIM_X = 25;
    private static final int DIM_Z = 25;
    private static final int TRIES = 700;
    private static final int BRANCHES = 6;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final RoomType[] ROOMS = {
            RoomType.BASIC, RoomType.BASIC, RoomType.BASIC, RoomType.BASIC, RoomType.BASIC,
            RoomType.BASIC, RoomType.BASIC, RoomType.BASIC, RoomType.BASIC, RoomType.BASIC,
            RoomType.ARROW, RoomType.ARROW_FIRE, RoomType.FIRE, RoomType.MAGIC, RoomType.MINE,
            RoomType.PILLAR, RoomType.POISON, RoomType.RAD, RoomType.RUBBLE, RoomType.SLOWNESS,
            RoomType.SPIDERS, RoomType.SPIKES, RoomType.WEAKNESS, RoomType.WEB, RoomType.ZOMBIE
    };

    private HbmLegacyJungleDungeonGenerator() {
    }

    static void generate(WorldGenLevel level, BoundingBox chunkBox, long seed, BlockPos origin) {
        RandomSource random = RandomSource.create(seed);
        generateLayer(level, chunkBox, random, origin.getX(), 20, origin.getZ());
        generateLayer(level, chunkBox, random, origin.getX(), 24, origin.getZ());
        generateLayer(level, chunkBox, random, origin.getX(), 28, origin.getZ());
        int markerY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        for (int offset = 0; offset < 3; offset++) {
            set(level, chunkBox, origin.getX(), markerY + offset, origin.getZ(), state("reinhardtshbm:deco_titanium", 0));
        }
        set(level, chunkBox, origin.getX(), markerY + 3, origin.getZ(), Blocks.REDSTONE_BLOCK.defaultBlockState());
    }

    static BoundingBox boundingBox(BlockPos origin, int minBuildHeight, int maxBuildHeight) {
        int startX = origin.getX() - DIM_X * WIDTH / 2;
        int startZ = origin.getZ() - DIM_Z * WIDTH / 2;
        int maxX = startX + (DIM_X - 1) * (WIDTH - 1) + WIDTH - 1;
        int maxZ = startZ + (DIM_Z - 1) * (WIDTH - 1) + WIDTH - 1;
        return new BoundingBox(startX, minBuildHeight, startZ, maxX, maxBuildHeight - 1, maxZ);
    }

    private static void generateLayer(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z) {
        RoomType[][] cells = new RoomType[DIM_X][DIM_Z];
        DoorDirection[][] doors = new DoorDirection[DIM_X][DIM_Z];
        List<int[]> order = new ArrayList<>();

        int startX = DIM_X / 2;
        int startZ = DIM_Z / 2;
        cells[startX][startZ] = randomRoom(random);
        doors[startX][startZ] = DoorDirection.UNKNOWN;
        order.add(new int[]{startX, startZ});

        int[] recursion = {0};
        addRoom(startX, startZ, random, DoorDirection.UNKNOWN, randomRoom(random), cells, doors, order, recursion);

        int dungeonX = x - DIM_X * WIDTH / 2;
        int dungeonZ = z - DIM_Z * WIDTH / 2;
        boolean[] hasHole = {false};
        List<Runnable> jobs = new ArrayList<>();

        for (int[] coord : order) {
            if (coord == null || coord.length != 2) {
                continue;
            }
            int cellX = coord[0];
            int cellZ = coord[1];
            RoomType room = cells[cellX][cellZ];
            if (room == null) {
                continue;
            }
            DoorDirection door = doors[cellX][cellZ] == null ? DoorDirection.UNKNOWN : doors[cellX][cellZ];
            int roomX = dungeonX + cellX * (WIDTH - 1);
            int roomZ = dungeonZ + cellZ * (WIDTH - 1);
            addRoomJobs(jobs, level, chunkBox, random, room, roomX, y, roomZ, door, hasHole);
        }

        jobs.add(() -> {
            if (hasHole[0]) {
                hasHole[0] = false;
                return;
            }
            List<int[]> rooms = new ArrayList<>();
            for (int cellX = 0; cellX < cells.length; cellX++) {
                for (int cellZ = 0; cellZ < cells[0].length; cellZ++) {
                    if (cells[cellX][cellZ] != null) {
                        rooms.add(new int[]{cellX, cellZ});
                    }
                }
            }
            if (!rooms.isEmpty()) {
                int[] room = rooms.get(random.nextInt(rooms.size()));
                int circleX = dungeonX + room[0] * (WIDTH - 1) + WIDTH / 2;
                int circleZ = dungeonZ + room[1] * (WIDTH - 1) + WIDTH / 2;
                set(level, chunkBox, circleX, y, circleZ, state("reinhardtshbm:brick_jungle_circle", 0));
            }
            hasHole[0] = false;
        });

        for (int index = 0; index < jobs.size(); index++) {
            jobs.get(index).run();
        }
    }

    private static boolean addRoom(
            int x,
            int z,
            RandomSource random,
            DoorDirection door,
            RoomType room,
            RoomType[][] cells,
            DoorDirection[][] doors,
            List<int[]> order,
            int[] recursion
    ) {
        recursion[0]++;
        if (recursion[0] > TRIES) {
            return false;
        }
        if (x < 0 || z < 0 || x >= DIM_X || z >= DIM_Z) {
            return false;
        }
        if (cells[x][z] != null) {
            DoorDirection direction = randomDirection(random);
            addRoom(x + direction.offsetX, z + direction.offsetZ, random, direction.opposite(), randomRoom(random), cells, doors, order, recursion);
            return false;
        }

        cells[x][z] = room;
        doors[x][z] = door;
        order.add(new int[]{x, z});

        for (int branch = 0; branch < BRANCHES; branch++) {
            DoorDirection direction = randomDirection(random);
            addRoom(x + direction.offsetX, z + direction.offsetZ, random, direction.opposite(), randomRoom(random), cells, doors, order, recursion);
        }
        return true;
    }

    private static RoomType randomRoom(RandomSource random) {
        return ROOMS[random.nextInt(ROOMS.length)];
    }

    private static DoorDirection randomDirection(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> DoorDirection.NORTH;
            case 1 -> DoorDirection.SOUTH;
            case 2 -> DoorDirection.WEST;
            default -> DoorDirection.EAST;
        };
    }

    private static void addRoomJobs(
            List<Runnable> jobs,
            WorldGenLevel level,
            BoundingBox chunkBox,
            RandomSource random,
            RoomType room,
            int x,
            int y,
            int z,
            DoorDirection door,
            boolean[] hasHole
    ) {
        jobs.add(() -> generateMain(level, chunkBox, random, x, y, z, hasHole));
        addSpecialJob(jobs, level, chunkBox, random, room, x, y, z);
        for (DoorDirection wall : DoorDirection.WALLS) {
            jobs.add(() -> generateWall(level, chunkBox, random, x, y, z, wall, wall == door));
        }
    }

    private static void addSpecialJob(
            List<Runnable> jobs,
            WorldGenLevel level,
            BoundingBox chunkBox,
            RandomSource random,
            RoomType room,
            int x,
            int y,
            int z
    ) {
        switch (room) {
            case BASIC -> {
            }
            case ARROW -> jobs.add(() -> jobs.add(() -> arrowWalls(level, chunkBox, x, y, z, Trap.ARROW)));
            case ARROW_FIRE -> jobs.add(() -> jobs.add(() -> arrowWallsCenter(level, chunkBox, x, y, z, Trap.FLAMING_ARROW)));
            case POISON -> jobs.add(() -> jobs.add(() -> poisonWalls(level, chunkBox, x, y, z)));
            case FIRE -> jobs.add(() -> floorRing(level, chunkBox, x, y, z, Trap.FIRE, true));
            case MINE -> jobs.add(() -> randomFloor(level, chunkBox, random, x, y, z, Trap.MINE, 3));
            case SLOWNESS -> jobs.add(() -> randomFloor(level, chunkBox, random, x, y, z, Trap.SLOWNESS, 2));
            case WEAKNESS -> jobs.add(() -> randomFloor(level, chunkBox, random, x, y, z, Trap.WEAKNESS, 2));
            case WEB -> jobs.add(() -> randomFloor(level, chunkBox, random, x, y, z, Trap.WEB, 3));
            case MAGIC -> jobs.add(() -> oneRandomFloor(level, chunkBox, random, x, y, z, Trap.MAGIC_CONVERSION));
            case RAD -> jobs.add(() -> oneRandomFloor(level, chunkBox, random, x, y, z, Trap.RAD_CONVERSION));
            case PILLAR -> jobs.add(() -> ceilingRing(level, chunkBox, x, y, z, Trap.PILLAR));
            case RUBBLE -> jobs.add(() -> trapIfJungleCore(level, chunkBox, x + 2, y + 4, z + 2, Trap.FALLING_ROCKS, false));
            case SPIDERS -> jobs.add(() -> trapIfJungleCore(level, chunkBox, x + 2, y + 4, z + 2, Trap.SPIDERS, false));
            case SPIKES -> jobs.add(() -> trapIfJungleCore(level, chunkBox, x + 2, y, z + 2, Trap.SPIKES, false));
            case ZOMBIE -> jobs.add(() -> trapIfJungleCore(level, chunkBox, x + 2, y, z + 2, Trap.ZOMBIE, false));
        }
    }

    private static void generateMain(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z, boolean[] hasHole) {
        box(level, chunkBox, random, x, y, z, WIDTH, 1, WIDTH, BlockChoice.FLOOR);
        box(level, chunkBox, random, x, y + 1, z, WIDTH, HEIGHT - 1, WIDTH, BlockChoice.AIR);
        box(level, chunkBox, random, x, y + HEIGHT - 1, z, WIDTH, 1, WIDTH, BlockChoice.CEILING);

        int rtd = random.nextInt(50);
        if (rtd < 5) {
            box(level, chunkBox, random, x + WIDTH / 2 - 1, y, z + WIDTH / 2 - 1, 3, 1, 3, BlockChoice.LAVA_FLOOR);
        } else if (rtd < 10) {
            set(level, chunkBox, x + 1 + random.nextInt(WIDTH - 1), y + 1, z + random.nextInt(WIDTH - 1), state("reinhardtshbm:crate_jungle", 0));
        } else if (rtd < 20 && !hasHole[0]) {
            boolean punched = false;
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    BlockPos floorBelow = new BlockPos(x + 1 + a, y - 4, z + 1 + b);
                    BlockPos spaceBelow = new BlockPos(x + 1 + a, y - 1, z + 1 + b);
                    if (level.getBlockState(spaceBelow).isAir() && isJungleFloorMaterial(level.getBlockState(floorBelow), true)) {
                        set(level, chunkBox, x + 1 + a, y, z + 1 + b, state("reinhardtshbm:brick_jungle_fragile", 0));
                        punched = true;
                    }
                }
            }
            if (punched) {
                hasHole[0] = true;
            }
        }
    }

    private static void generateWall(
            WorldGenLevel level,
            BoundingBox chunkBox,
            RandomSource random,
            int x,
            int y,
            int z,
            DoorDirection wall,
            boolean door
    ) {
        if (wall == DoorDirection.NORTH) {
            box(level, chunkBox, random, x, y + 1, z, WIDTH, HEIGHT - 2, 1, BlockChoice.WALL);
            if (door) {
                box(level, chunkBox, random, x + WIDTH / 2 - 1, y + 1, z, 3, 3, 1, BlockChoice.AIR);
            }
        } else if (wall == DoorDirection.SOUTH) {
            box(level, chunkBox, random, x, y + 1, z + WIDTH - 1, WIDTH, HEIGHT - 2, 1, BlockChoice.WALL);
            if (door) {
                box(level, chunkBox, random, x + WIDTH / 2 - 1, y + 1, z + WIDTH - 1, 3, 3, 1, BlockChoice.AIR);
            }
        } else if (wall == DoorDirection.WEST) {
            box(level, chunkBox, random, x, y + 1, z, 1, HEIGHT - 2, WIDTH, BlockChoice.WALL);
            if (door) {
                box(level, chunkBox, random, x, y + 1, z + WIDTH / 2 - 1, 1, 3, 3, BlockChoice.AIR);
            }
        } else if (wall == DoorDirection.EAST) {
            box(level, chunkBox, random, x + WIDTH - 1, y + 1, z, 1, HEIGHT - 2, WIDTH, BlockChoice.WALL);
            if (door) {
                box(level, chunkBox, random, x + WIDTH - 1, y + 1, z + WIDTH / 2 - 1, 1, 3, 3, BlockChoice.AIR);
            }
        }
    }

    private static void arrowWalls(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Trap trap) {
        for (int i = 1; i < 4; i++) {
            trapIfJungleCore(level, chunkBox, x + WIDTH / 2, y + i, z, trap, false);
            trapIfJungleCore(level, chunkBox, x + WIDTH / 2, y + i, z + WIDTH - 1, trap, false);
            trapIfJungleCore(level, chunkBox, x, y + i, z + WIDTH / 2, trap, false);
            trapIfJungleCore(level, chunkBox, x + WIDTH - 1, y + i, z + WIDTH / 2, trap, false);
        }
    }

    private static void arrowWallsCenter(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Trap trap) {
        trapIfJungleCore(level, chunkBox, x + WIDTH / 2, y + 2, z, trap, false);
        trapIfJungleCore(level, chunkBox, x + WIDTH / 2, y + 2, z + WIDTH - 1, trap, false);
        trapIfJungleCore(level, chunkBox, x, y + 2, z + WIDTH / 2, trap, false);
        trapIfJungleCore(level, chunkBox, x + WIDTH - 1, y + 2, z + WIDTH / 2, trap, false);
    }

    private static void poisonWalls(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z) {
        for (int i = 1; i < 4; i++) {
            trapIfJungleCore(level, chunkBox, x, y + 2, z + i, Trap.POISON_DART, false);
            trapIfJungleCore(level, chunkBox, x + WIDTH - 1, y + 2, z + i, Trap.POISON_DART, false);
            trapIfJungleCore(level, chunkBox, x + i, y + 2, z, Trap.POISON_DART, false);
            trapIfJungleCore(level, chunkBox, x + i, y + 2, z + WIDTH - 1, Trap.POISON_DART, false);
        }
    }

    private static void floorRing(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Trap trap, boolean includeTrap) {
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (a == 1 && b == 1) {
                    continue;
                }
                trapIfJungleCore(level, chunkBox, x + 1 + a, y, z + 1 + b, trap, includeTrap);
            }
        }
    }

    private static void ceilingRing(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Trap trap) {
        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                if (a == 1 && b == 1) {
                    continue;
                }
                trapIfJungleCore(level, chunkBox, x + 1 + a, y + 4, z + 1 + b, trap, true);
            }
        }
    }

    private static void randomFloor(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z, Trap trap, int chance) {
        for (int a = 1; a < 4; a++) {
            for (int b = 1; b < 4; b++) {
                if (random.nextInt(chance) == 0) {
                    trapIfJungleCore(level, chunkBox, x + a, y, z + b, trap, false);
                }
            }
        }
    }

    private static void oneRandomFloor(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z, Trap trap) {
        int ix = random.nextInt(3) + 1;
        int iz = random.nextInt(3) + 1;
        trapIfJungleCore(level, chunkBox, x + ix, y, z + iz, trap, false);
    }

    private static void trapIfJungleCore(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Trap trap, boolean includeTrap) {
        BlockState current = level.getBlockState(new BlockPos(x, y, z));
        if (isJungleFloorMaterial(current, includeTrap)) {
            set(level, chunkBox, x, y, z, trapState(trap));
        }
    }

    private static boolean isJungleFloorMaterial(BlockState state, boolean includeTrap) {
        return state.is(HbmBlocks.BRICK_JUNGLE.get())
                || state.is(HbmBlocks.BRICK_JUNGLE_CRACKED.get())
                || state.is(HbmBlocks.BRICK_JUNGLE_LAVA.get())
                || includeTrap && state.is(HbmBlocks.BRICK_JUNGLE_TRAP.get());
    }

    private static void box(
            WorldGenLevel level,
            BoundingBox chunkBox,
            RandomSource random,
            int x,
            int y,
            int z,
            int sizeX,
            int sizeY,
            int sizeZ,
            BlockChoice choice
    ) {
        for (int ix = x; ix < x + sizeX; ix++) {
            for (int iy = y; iy < y + sizeY; iy++) {
                for (int iz = z; iz < z + sizeZ; iz++) {
                    set(level, chunkBox, ix, iy, iz, choice.state(random));
                }
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState state) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (!chunkBox.isInside(pos)) {
            return;
        }
        level.setBlock(pos, state, FLAGS);
    }

    private static BlockState trapState(Trap trap) {
        return HbmBlocks.BRICK_JUNGLE_TRAP.get().defaultBlockState()
                .setValue(TrappedBrickBlock.TRAP, trap.ordinal());
    }

    private static BlockState state(String id, int meta) {
        return HbmLegacyNbtTemplate.stateFromLegacyId(id, meta);
    }

    private enum RoomType {
        BASIC,
        ARROW,
        ARROW_FIRE,
        FIRE,
        MAGIC,
        MINE,
        PILLAR,
        POISON,
        RAD,
        RUBBLE,
        SLOWNESS,
        SPIDERS,
        SPIKES,
        WEAKNESS,
        WEB,
        ZOMBIE
    }

    private enum DoorDirection {
        UNKNOWN(0, 0),
        NORTH(0, -1),
        SOUTH(0, 1),
        WEST(-1, 0),
        EAST(1, 0);

        private static final DoorDirection[] WALLS = {NORTH, SOUTH, WEST, EAST};

        private final int offsetX;
        private final int offsetZ;

        DoorDirection(int offsetX, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }

        private DoorDirection opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
                default -> UNKNOWN;
            };
        }
    }

    private enum Trap {
        FALLING_ROCKS,
        FIRE,
        ARROW,
        SPIKES,
        MINE,
        WEB,
        FLAMING_ARROW,
        PILLAR,
        RAD_CONVERSION,
        MAGIC_CONVERSION,
        SLOWNESS,
        WEAKNESS,
        POISON_DART,
        ZOMBIE,
        SPIDERS
    }

    private enum BlockChoice {
        AIR {
            @Override
            BlockState state(RandomSource random) {
                return Blocks.AIR.defaultBlockState();
            }
        },
        FLOOR {
            @Override
            BlockState state(RandomSource random) {
                return random.nextInt(2) == 0
                        ? HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle", 0)
                        : HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_cracked", 0);
            }
        },
        CEILING {
            @Override
            BlockState state(RandomSource random) {
                return random.nextInt(2) == 0
                        ? HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle", 0)
                        : HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_cracked", 0);
            }
        },
        WALL {
            @Override
            BlockState state(RandomSource random) {
                int roll = random.nextInt(116);
                if (roll < 100) {
                    return (roll & 1) == 0
                            ? HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle", 0)
                            : HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_cracked", 0);
                }
                return HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_glyph", roll - 100);
            }
        },
        LAVA_FLOOR {
            @Override
            BlockState state(RandomSource random) {
                return random.nextInt(3) == 0
                        ? HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_cracked", 0)
                        : HbmLegacyJungleDungeonGenerator.state("reinhardtshbm:brick_jungle_lava", 0);
            }
        };

        abstract BlockState state(RandomSource random);
    }
}
