package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.blockentity.HbmStructureLoot;
import com.reinhardt.hbm.blockentity.LegacyDisplayStandBlockEntity;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Direct room-generation port of BlockKeyhole and BlockRedBrickKeyhole. */
public final class StoneKeyholeBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final int ROOM_WIDTH = 4;
    private static final int ROOM_HEIGHT = 5;

    private final Kind kind;

    public StoneKeyholeBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!hit.getDirection().getAxis().isHorizontal() || !isRedKey(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean cracked = BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id("key_red_cracked"));
            if (cracked && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            Direction entrance = hit.getDirection();
            BlockPos center = pos.relative(entrance.getOpposite(), 4).below(2);
            if (this.kind == Kind.STONE) {
                generateStoneRoom(level, center);
            } else {
                generateMetaRoom(level, center, entrance);
            }
            placeRedDoor(level, pos.below(), entrance);
            level.playSound(null, pos, HbmSoundEvents.LOCK_OPEN.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            clearLooseItems(level, center);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return this.kind == Kind.STONE ? new ItemStack(Blocks.STONE) : new ItemStack(HbmBlocks.BRICK_RED.get());
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    private static boolean isRedKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.equals(ReinhardtsHBM.id("key_red")) || id.equals(ReinhardtsHBM.id("key_red_cracked"));
    }

    private static void generateStoneRoom(Level level, BlockPos center) {
        buildShell(level, center, false, null);
        int choice = level.random.nextInt(4);
        Direction wall = Direction.from2DDataValue(choice);
        BlockPos keyhole = center.relative(wall, ROOM_WIDTH).above(2);
            level.setBlock(keyhole, HbmBlocks.STONE_KEYHOLE_META.get().defaultBlockState()
                .setValue(FACING, wall.getOpposite()), Block.UPDATE_ALL);

        int torchDist = ROOM_WIDTH - 1;
        int torchOff = torchDist - 1;
        int[][] torches = {{torchDist, torchOff}, {torchDist, -torchOff}, {-torchDist, torchOff}, {-torchDist, -torchOff},
                {torchOff, torchDist}, {-torchOff, torchDist}, {torchOff, -torchDist}, {-torchOff, -torchDist}};
        for (int[] offset : torches) {
            level.setBlock(center.offset(offset[0], 2, offset[1]), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (level.random.nextInt(4) == 0) {
            for (int x = -ROOM_WIDTH + 1; x < ROOM_WIDTH; x++) {
                for (int z = -ROOM_WIDTH + 1; z < ROOM_WIDTH; z++) {
                    if (level.random.nextBoolean()) {
                        level.setBlock(center.offset(x, ROOM_HEIGHT - 2, z), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        if (level.random.nextInt(4) == 0) {
            int[] corners = {-ROOM_WIDTH + 2, ROOM_WIDTH - 2};
            for (int x : corners) for (int z : corners) for (int y = 1; y <= ROOM_HEIGHT - 2; y++) {
                level.setBlock(center.offset(x, y, z), HbmBlocks.CONCRETE_COLORED.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (level.random.nextInt(4) == 0) {
            int[] corners = {-ROOM_WIDTH + 1, ROOM_WIDTH - 1};
            for (int x : corners) for (int z : corners) {
                level.setBlock(center.offset(x, 0, z), Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(center.offset(x, 1, z), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (level.random.nextInt(4) == 0) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (x != 0 || z != 0) {
                level.setBlock(center.offset(x, 0, z), HbmBlocks.CONCRETE_COLORED.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (level.random.nextInt(4) == 0) {
            for (int side : new int[]{-1, 1}) {
                for (int offset : new int[]{ROOM_WIDTH - 3, ROOM_WIDTH - 2}) {
                    level.setBlock(center.offset(side * offset, 0, ROOM_WIDTH - 1), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(center.offset(side * offset, 0, -ROOM_WIDTH + 1), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(center.offset(ROOM_WIDTH - 1, 0, side * offset), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(center.offset(-ROOM_WIDTH + 1, 0, side * offset), Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        if (level.random.nextInt(20) == 0) {
            level.setBlock(center.above(), HbmBlocks.DECO_LOOT.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(center.above()) instanceof DecoLootBlockEntity loot) {
                loot.addItem(new ItemStack(HbmItems.NCRPA_HELMET.get()), 0.0D, 0.0D, 0.0D);
                loot.addItem(new ItemStack(HbmItems.NCRPA_PLATE.get()), 0.0D, 0.0D, 0.0D);
                loot.addItem(new ItemStack(HbmItems.NCRPA_LEGS.get()), 0.0D, 0.0D, 0.0D);
                loot.addItem(new ItemStack(HbmItems.NCRPA_BOOTS.get()), 0.0D, 0.0D, 0.0D);
            }
        } else {
            placePedestal(level, center.above(), HbmStructureLoot.randomStack("POOL_RED_PEDESTAL", level.random));
        }
    }

    private static void generateMetaRoom(Level level, BlockPos center, Direction entrance) {
        buildShell(level, center, true, entrance);
        BlockPos[] pedestals = {
                center.above(), center.offset(2, 1, 2), center.offset(-2, 1, 2),
                center.offset(2, 1, -2), center.offset(-2, 1, -2)
        };
        for (BlockPos pedestal : pedestals) {
            if (pedestal.equals(center.above()) || level.random.nextBoolean()) {
                level.setBlock(pedestal, HbmBlocks.PEDESTAL.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void buildShell(Level level, BlockPos center, boolean leaveEntrance, Direction entrance) {
        for (int x = -ROOM_WIDTH; x <= ROOM_WIDTH; x++) {
            for (int z = -ROOM_WIDTH; z <= ROOM_WIDTH; z++) {
                boolean edge = Math.abs(x) == ROOM_WIDTH || Math.abs(z) == ROOM_WIDTH;
                setNeutralBrick(level, center.offset(x, 0, z));
                setNeutralBrick(level, center.offset(x, ROOM_HEIGHT - 1, z));
                for (int y = 1; y < ROOM_HEIGHT - 1; y++) {
                    BlockPos target = center.offset(x, y, z);
                    boolean doorway = leaveEntrance && edge && isEntranceCell(x, z, y, entrance);
                    if (edge && !doorway) {
                        level.setBlock(target, leaveEntrance ? neutralBrick() : wallBrick(x, z), Block.UPDATE_ALL);
                    } else {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        for (int x = -ROOM_WIDTH + 1; x < ROOM_WIDTH; x++) {
            for (int z = -ROOM_WIDTH + 1; z < ROOM_WIDTH; z++) {
                level.setBlock(center.offset(x, 0, z), horizontalBrick(Direction.UP), Block.UPDATE_ALL);
                level.setBlock(center.offset(x, ROOM_HEIGHT - 1, z), horizontalBrick(Direction.DOWN), Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState neutralBrick() {
        return HbmBlocks.BRICK_RED.get().defaultBlockState()
                .setValue(RedBrickBlock.FACING, Direction.NORTH)
                .setValue(RedBrickBlock.NEUTRAL, true);
    }

    private static BlockState horizontalBrick(Direction facing) {
        return HbmBlocks.BRICK_RED.get().defaultBlockState()
                .setValue(RedBrickBlock.FACING, facing)
                .setValue(RedBrickBlock.NEUTRAL, false);
    }

    private static BlockState wallBrick(int x, int z) {
        Direction facing;
        if (x == ROOM_WIDTH) {
            facing = Direction.WEST;
        } else if (x == -ROOM_WIDTH) {
            facing = Direction.EAST;
        } else if (z == ROOM_WIDTH) {
            facing = Direction.NORTH;
        } else {
            facing = Direction.SOUTH;
        }
        return HbmBlocks.BRICK_RED.get().defaultBlockState()
                .setValue(RedBrickBlock.FACING, facing)
                .setValue(RedBrickBlock.NEUTRAL, false);
    }

    private static void setNeutralBrick(Level level, BlockPos pos) {
        level.setBlock(pos, HbmBlocks.BRICK_RED.get().defaultBlockState()
                .setValue(RedBrickBlock.FACING, Direction.NORTH)
                .setValue(RedBrickBlock.NEUTRAL, true), Block.UPDATE_ALL);
    }

    private static void placePedestal(Level level, BlockPos pos, ItemStack stack) {
        level.setBlock(pos, HbmBlocks.PEDESTAL.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof LegacyDisplayStandBlockEntity pedestal) {
            pedestal.setDisplayedItem(stack);
        }
    }

    private static boolean isEntranceCell(int x, int z, int y, Direction entrance) {
        if (y > 2) return false;
        return switch (entrance) {
            case NORTH -> z == -ROOM_WIDTH && x == 0;
            case SOUTH -> z == ROOM_WIDTH && x == 0;
            case WEST -> x == -ROOM_WIDTH && z == 0;
            case EAST -> x == ROOM_WIDTH && z == 0;
            default -> false;
        };
    }

    private static void placeRedDoor(Level level, BlockPos lowerPos, Direction entrance) {
        if (!level.getBlockState(lowerPos).canBeReplaced() || !level.getBlockState(lowerPos.above()).canBeReplaced()) {
            return;
        }
        BlockState lower = HbmBlocks.DOOR_RED.get().defaultBlockState()
                .setValue(DoorBlock.FACING, entrance)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, false)
                .setValue(DoorBlock.POWERED, false);
        level.setBlock(lowerPos, lower, Block.UPDATE_ALL);
        level.setBlock(lowerPos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    private static void clearLooseItems(Level level, BlockPos center) {
        AABB box = new AABB(
                center.getX() - ROOM_WIDTH, center.getY(), center.getZ() - ROOM_WIDTH,
                center.getX() + ROOM_WIDTH + 1, center.getY() + ROOM_HEIGHT,
                center.getZ() + ROOM_WIDTH + 1);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            item.discard();
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public enum Kind {
        STONE,
        RED_BRICK
    }
}
