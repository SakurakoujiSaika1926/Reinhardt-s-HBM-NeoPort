package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Direct port of ItemConveyorWand's placement, routing and creative removal. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class ConveyorWandItem extends LegacyVariantItem {
    private static final String START_X = "conveyorStartX";
    private static final String START_Y = "conveyorStartY";
    private static final String START_Z = "conveyorStartZ";
    private static final String START_SIDE = "conveyorStartSide";
    private static final String COUNT = "conveyorCount";
    private static final String SELECTING = "conveyorSelecting";
    private static final ThreadLocal<Boolean> BREAKING_CONNECTED = ThreadLocal.withInitial(() -> false);

    public ConveyorWandItem(Properties properties) {
        super(properties, "conveyor_wand", LegacyVariantItem.variants("regular", "express", "double", "triple"));
    }

    public ConveyorType type(ItemStack stack) { return ConveyorType.byId(variant(stack).id()); }
    public static ItemStack stackFor(Supplier<? extends Item> item, ConveyorType type) { return LegacyVariantItem.stackFor(item, type.id()); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return switch (type(context.getItemInHand())) {
            case REGULAR -> useRegular(context, player);
            case EXPRESS -> useExpress(context, player);
            case DOUBLE -> useDouble(context, player);
            case TRIPLE -> useTriple(context, player);
        };
    }

    private InteractionResult useRegular(UseOnContext context, Player player) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown() && !selecting(data(stack))) return placeRegularSingle(context);

        BlockPos clicked = context.getClickedPos();
        Direction side = context.getClickedFace();
        CompoundTag tag = data(stack);
        if (level.getBlockState(clicked).getBlock() instanceof ConveyorBlock conveyor && conveyor.kind().bendable()) {
            Direction snap = selecting(tag) ? conveyor.inputDirection(level.getBlockState(clicked)) : conveyor.outputDirection(level.getBlockState(clicked));
            if (isReplaceable(level, clicked.relative(snap))) side = snap;
        }
        if (!selecting(tag)) {
            tag.putBoolean(SELECTING, true);
            tag.putInt(START_X, clicked.getX());
            tag.putInt(START_Y, clicked.getY());
            tag.putInt(START_Z, clicked.getZ());
            tag.putInt(START_SIDE, side.get3DDataValue());
            tag.putInt(COUNT, player.getAbilities().instabuild ? 256 : countAvailable(player, ConveyorType.REGULAR));
            save(stack, tag);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos start = new BlockPos(tag.getInt(START_X), tag.getInt(START_Y), tag.getInt(START_Z));
        List<Placement> placements = buildRegularPath(level, player, start, Direction.from3DDataValue(tag.getInt(START_SIDE)), clicked, side, tag.getInt(COUNT));
        tag.remove(SELECTING);
        save(stack, tag);
        if (!level.isClientSide) {
            if (placements == null) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.obstructed"), true);
            else if (placements.isEmpty()) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.not_enough"), true);
            else {
                for (Placement placement : placements) level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
                refreshPlacedConveyors(level, placements);
                if (!player.getAbilities().instabuild) consume(player, ConveyorType.REGULAR, placements.size());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.built"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult useExpress(UseOnContext context, Player player) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown() && !selecting(data(stack))) return placeExpressSingle(context);

        BlockPos clicked = context.getClickedPos();
        Direction side = context.getClickedFace();
        CompoundTag tag = data(stack);
        if (level.getBlockState(clicked).getBlock() instanceof ConveyorBlock conveyor && conveyor.kind().bendable()) {
            Direction snap = selecting(tag) ? conveyor.inputDirection(level.getBlockState(clicked)) : conveyor.outputDirection(level.getBlockState(clicked));
            if (isReplaceable(level, clicked.relative(snap))) side = snap;
        }
        if (!selecting(tag)) {
            tag.putBoolean(SELECTING, true);
            tag.putInt(START_X, clicked.getX());
            tag.putInt(START_Y, clicked.getY());
            tag.putInt(START_Z, clicked.getZ());
            tag.putInt(START_SIDE, side.get3DDataValue());
            tag.putInt(COUNT, player.getAbilities().instabuild ? 256 : countAvailable(player, ConveyorType.EXPRESS));
            save(stack, tag);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos start = new BlockPos(tag.getInt(START_X), tag.getInt(START_Y), tag.getInt(START_Z));
        List<Placement> placements = buildExpressPath(level, player, start, Direction.from3DDataValue(tag.getInt(START_SIDE)), clicked, side, tag.getInt(COUNT));
        tag.remove(SELECTING);
        save(stack, tag);
        if (!level.isClientSide) {
            if (placements == null) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.obstructed"), true);
            else if (placements.isEmpty()) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.not_enough"), true);
            else {
                for (Placement placement : placements) level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
                refreshPlacedConveyors(level, placements);
                if (!player.getAbilities().instabuild) consume(player, ConveyorType.EXPRESS, placements.size());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.built"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult useDouble(UseOnContext context, Player player) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown() && !selecting(data(stack))) return placeDoubleSingle(context);

        BlockPos clicked = context.getClickedPos();
        Direction side = context.getClickedFace();
        CompoundTag tag = data(stack);
        if (level.getBlockState(clicked).getBlock() instanceof ConveyorBlock conveyor && conveyor.kind().bendable()) {
            Direction snap = selecting(tag) ? conveyor.inputDirection(level.getBlockState(clicked)) : conveyor.outputDirection(level.getBlockState(clicked));
            if (isReplaceable(level, clicked.relative(snap))) side = snap;
        }
        if (!selecting(tag)) {
            tag.putBoolean(SELECTING, true);
            tag.putInt(START_X, clicked.getX());
            tag.putInt(START_Y, clicked.getY());
            tag.putInt(START_Z, clicked.getZ());
            tag.putInt(START_SIDE, side.get3DDataValue());
            tag.putInt(COUNT, player.getAbilities().instabuild ? 256 : countAvailable(player, ConveyorType.DOUBLE));
            save(stack, tag);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos start = new BlockPos(tag.getInt(START_X), tag.getInt(START_Y), tag.getInt(START_Z));
        List<Placement> placements = buildDoublePath(level, player, start, Direction.from3DDataValue(tag.getInt(START_SIDE)), clicked, side, tag.getInt(COUNT));
        tag.remove(SELECTING);
        save(stack, tag);
        if (!level.isClientSide) {
            if (placements == null) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.obstructed"), true);
            else if (placements.isEmpty()) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.not_enough"), true);
            else {
                for (Placement placement : placements) level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
                refreshPlacedConveyors(level, placements);
                if (!player.getAbilities().instabuild) consume(player, ConveyorType.DOUBLE, placements.size());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.built"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult useTriple(UseOnContext context, Player player) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player.isShiftKeyDown() && !selecting(data(stack))) return placeTripleSingle(context);

        BlockPos clicked = context.getClickedPos();
        Direction side = context.getClickedFace();
        CompoundTag tag = data(stack);
        if (level.getBlockState(clicked).getBlock() instanceof ConveyorBlock conveyor && conveyor.kind().bendable()) {
            Direction snap = selecting(tag) ? conveyor.inputDirection(level.getBlockState(clicked)) : conveyor.outputDirection(level.getBlockState(clicked));
            if (isReplaceable(level, clicked.relative(snap))) side = snap;
        }
        if (!selecting(tag)) {
            tag.putBoolean(SELECTING, true);
            tag.putInt(START_X, clicked.getX());
            tag.putInt(START_Y, clicked.getY());
            tag.putInt(START_Z, clicked.getZ());
            tag.putInt(START_SIDE, side.get3DDataValue());
            tag.putInt(COUNT, player.getAbilities().instabuild ? 256 : countAvailable(player, ConveyorType.TRIPLE));
            save(stack, tag);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos start = new BlockPos(tag.getInt(START_X), tag.getInt(START_Y), tag.getInt(START_Z));
        List<Placement> placements = buildTriplePath(level, player, start, Direction.from3DDataValue(tag.getInt(START_SIDE)), clicked, side, tag.getInt(COUNT));
        tag.remove(SELECTING);
        save(stack, tag);
        if (!level.isClientSide) {
            if (placements == null) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.obstructed"), true);
            else if (placements.isEmpty()) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.not_enough"), true);
            else {
                for (Placement placement : placements) level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
                refreshPlacedConveyors(level, placements);
                if (!player.getAbilities().instabuild) consume(player, ConveyorType.TRIPLE, placements.size());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.built"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult placeRegularSingle(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clicked);
        Block toPlace = HbmBlocks.CONVEYOR.get();
        if (clickedState.is(HbmBlocks.CONVEYOR.get()) && clickedState.getValue(ConveyorBlock.CURVE) == 0) {
            if (face == Direction.UP) level.setBlock(clicked, HbmBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(ConveyorBlock.FACING, clickedState.getValue(ConveyorBlock.FACING)), Block.UPDATE_ALL);
            else if (face == Direction.DOWN) level.setBlock(clicked, HbmBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(ConveyorBlock.FACING, clickedState.getValue(ConveyorBlock.FACING)), Block.UPDATE_ALL);
            clickedState = level.getBlockState(clicked);
        }
        if (clickedState.getBlock() instanceof ConveyorBlock conveyor) {
            if (conveyor.kind() == ConveyorBlock.Kind.LIFT && face == Direction.UP) toPlace = HbmBlocks.CONVEYOR_LIFT.get();
            if (conveyor.kind() == ConveyorBlock.Kind.CHUTE && face == Direction.DOWN) toPlace = HbmBlocks.CONVEYOR_CHUTE.get();
        }
        BlockPos target = clicked.relative(face);
        if (!isReplaceable(level, target)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(target, regularPlacementState(toPlace, context.getHorizontalDirection().getOpposite(), 0), Block.UPDATE_ALL);
            ConveyorBlock.refreshVisualStateAt(level, clicked);
            ConveyorBlock.refreshVisualStateAt(level, target);
            if (!context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult placeExpressSingle(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockPos target = clicked.relative(context.getClickedFace());
        if (!isReplaceable(level, target)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(target, expressPlacementState(context.getHorizontalDirection().getOpposite(), 0), Block.UPDATE_ALL);
            ConveyorBlock.refreshVisualStateAt(level, clicked);
            ConveyorBlock.refreshVisualStateAt(level, target);
            if (!context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult placeDoubleSingle(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockPos target = clicked.relative(context.getClickedFace());
        if (!isReplaceable(level, target)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(target, doublePlacementState(context.getHorizontalDirection().getOpposite(), 0), Block.UPDATE_ALL);
            ConveyorBlock.refreshVisualStateAt(level, clicked);
            ConveyorBlock.refreshVisualStateAt(level, target);
            if (!context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult placeTripleSingle(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockPos target = clicked.relative(context.getClickedFace());
        if (!isReplaceable(level, target)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(target, triplePlacementState(context.getHorizontalDirection().getOpposite(), 0), Block.UPDATE_ALL);
            ConveyorBlock.refreshVisualStateAt(level, clicked);
            ConveyorBlock.refreshVisualStateAt(level, target);
            if (!context.getPlayer().getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @SubscribeEvent
    public static void onConveyorBreak(BlockEvent.BreakEvent event) {
        if (BREAKING_CONNECTED.get()) return;
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ConveyorWandItem wand)) return;
        wand.breakConnected(stack, event.getPos(), player);
    }

    private void breakConnected(ItemStack stack, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild || !(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level) || !(level.getBlockState(pos).getBlock() instanceof ConveyorBlock conveyor)) return;
        BlockState state = level.getBlockState(pos);
        BREAKING_CONNECTED.set(true);
        try {
            breakExtra(level, serverPlayer, pos.relative(conveyor.inputDirection(state)), 32);
            breakExtra(level, serverPlayer, pos.relative(conveyor.outputDirection(state)), 32);
        } finally {
            BREAKING_CONNECTED.remove();
        }
    }

    private static void breakExtra(ServerLevel level, ServerPlayer player, BlockPos pos, int depth) {
        if (--depth <= 0) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock conveyor)) return;
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;
        Direction input = conveyor.inputDirection(state);
        Direction output = conveyor.outputDirection(state);
        level.destroyBlock(pos, false, player);
        breakExtra(level, player, pos.relative(input), depth);
        breakExtra(level, player, pos.relative(output), depth);
    }

    private static List<Placement> buildRegularPath(Level level, Player player, BlockPos first, Direction firstSide,
                                                    BlockPos target, Direction targetSide, int max) {
        if (max <= 0) return List.of();
        if (first.equals(target) && firstSide == targetSide && firstSide.getAxis().isVertical()) {
            BlockPos pos = first.relative(firstSide);
            return isReplaceable(level, pos) ? List.of(new Placement(pos, regularPlacementState(HbmBlocks.CONVEYOR.get(), player.getDirection().getOpposite(), 0))) : null;
        }
        BlockPos finalTarget = target.relative(targetSide);
        BlockPos current = first.relative(firstSide);
        Direction direction = firstSide.getAxis().isVertical() ? regularTargetDirection(current, target, target, null, false) : firstSide;
        BlockState targetState = level.getBlockState(target);
        boolean turnToTarget = targetSide.getAxis().isHorizontal()
                || isLegacyCraneBase(targetState)
                || (targetState.getBlock() instanceof ConveyorBlock conveyor
                && (conveyor.kind() == ConveyorBlock.Kind.LIFT || conveyor.kind() == ConveyorBlock.Kind.CHUTE));
        Direction horizontal = direction.getAxis().isVertical() ? player.getDirection() : direction;
        if (current.getY() > finalTarget.getY() && isReplaceable(level, current.below())) direction = Direction.DOWN;
        List<Placement> placements = new ArrayList<>();
        for (int depth = 1; depth <= max; depth++) {
            if (!isReplaceable(level, current)) return null;
            Block block = regularBlockForDirection(direction);
            Direction facing = regularConveyorFacing(block, direction, targetSide, horizontal);
            int curve = 0;
            BlockPos next = current.relative(direction);
            int fromDistance = regularManhattan(current, finalTarget);
            int toDistance = regularManhattan(next, finalTarget);
            int finalDistance = regularManhattan(next, target);
            boolean notAtTarget = (turnToTarget ? finalDistance : fromDistance) > 0;
            boolean obstructed = notAtTarget && !isReplaceable(level, next);
            if ((toDistance >= fromDistance && notAtTarget) || obstructed) {
                Direction nextDirection = regularTargetDirection(current, turnToTarget ? target : finalTarget, finalTarget, direction, obstructed);
                if (nextDirection == Direction.UP) block = HbmBlocks.CONVEYOR_LIFT.get();
                else if (nextDirection == Direction.DOWN) block = HbmBlocks.CONVEYOR_CHUTE.get();
                else if (direction.getAxis().isHorizontal() && regularCurveTwoDirection(direction) == nextDirection) curve = 2;
                else if (direction.getAxis().isHorizontal() && regularCurveOneDirection(direction) == nextDirection) curve = 1;
                direction = nextDirection;
                if (direction.getAxis().isHorizontal()) horizontal = direction;
            }
            placements.add(new Placement(current, regularPlacementState(block, facing, curve)));
            if (current.equals(finalTarget)) return placements;
            current = current.relative(direction);
        }
        return List.of();
    }

    private static Direction regularCurveTwoDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Regular conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction regularCurveOneDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Regular conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction regularTargetDirection(BlockPos from, BlockPos target, BlockPos finalTarget, Direction heading, boolean obstructed) {
        if ((from.getY() != target.getY() || from.getY() != finalTarget.getY())
                && (obstructed || (from.getX() == target.getX() && from.getZ() == target.getZ()) || (from.getX() == finalTarget.getX() && from.getZ() == finalTarget.getZ()))) {
            return from.getY() > target.getY() ? Direction.DOWN : Direction.UP;
        }
        if (Math.abs(from.getX() - target.getX()) > Math.abs(from.getZ() - target.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
            return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        }
        if (heading == Direction.NORTH || heading == Direction.SOUTH) return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static Block regularBlockForDirection(Direction direction) {
        if (direction == Direction.UP) return HbmBlocks.CONVEYOR_LIFT.get();
        if (direction == Direction.DOWN) return HbmBlocks.CONVEYOR_CHUTE.get();
        return HbmBlocks.CONVEYOR.get();
    }

    private static Direction regularConveyorFacing(Block block, Direction direction, Direction targetSide, Direction horizontal) {
        if (block == HbmBlocks.CONVEYOR_LIFT.get() || block == HbmBlocks.CONVEYOR_CHUTE.get()) return targetSide.getAxis().isVertical() ? horizontal.getOpposite() : targetSide;
        return direction.getOpposite();
    }

    private static BlockState regularPlacementState(Block block, Direction facing, int curve) {
        BlockState state = block.defaultBlockState().setValue(ConveyorBlock.FACING, facing);
        return state.hasProperty(ConveyorBlock.CURVE) ? state.setValue(ConveyorBlock.CURVE, curve) : state;
    }

    private static int regularManhattan(BlockPos one, BlockPos two) {
        return Math.abs(one.getX() - two.getX()) + Math.abs(one.getY() - two.getY()) + Math.abs(one.getZ() - two.getZ());
    }

    private static List<Placement> buildExpressPath(Level level, Player player, BlockPos first, Direction firstSide,
                                                    BlockPos target, Direction targetSide, int max) {
        if (max <= 0) return List.of();
        if (first.equals(target) && firstSide == targetSide && firstSide.getAxis().isVertical()) {
            BlockPos pos = first.relative(firstSide);
            return isReplaceable(level, pos) ? List.of(new Placement(pos, expressPlacementState(player.getDirection().getOpposite(), 0))) : null;
        }
        BlockPos finalTarget = target.relative(targetSide);
        BlockPos current = first.relative(firstSide);
        Direction direction = firstSide.getAxis().isVertical() ? expressTargetDirection(current, target, target, null) : firstSide;
        BlockState targetState = level.getBlockState(target);
        boolean turnToTarget = targetSide.getAxis().isHorizontal()
                || isLegacyCraneBase(targetState)
                || (targetState.getBlock() instanceof ConveyorBlock conveyor
                && (conveyor.kind() == ConveyorBlock.Kind.LIFT || conveyor.kind() == ConveyorBlock.Kind.CHUTE));
        List<Placement> placements = new ArrayList<>();
        for (int depth = 1; depth <= max; depth++) {
            if (!isReplaceable(level, current)) return null;
            Direction facing = expressConveyorFacing(direction);
            int curve = 0;
            BlockPos next = current.relative(direction);
            int fromDistance = expressManhattan(current, finalTarget);
            int toDistance = expressManhattan(next, finalTarget);
            int finalDistance = expressManhattan(next, target);
            boolean notAtTarget = (turnToTarget ? finalDistance : fromDistance) > 0;
            boolean obstructed = notAtTarget && !isReplaceable(level, next);
            if ((toDistance >= fromDistance && notAtTarget) || obstructed) {
                Direction nextDirection = expressTargetDirection(current, turnToTarget ? target : finalTarget, finalTarget, direction);
                if (direction.getAxis().isHorizontal() && expressCurveTwoDirection(direction) == nextDirection) curve = 2;
                else if (direction.getAxis().isHorizontal() && expressCurveOneDirection(direction) == nextDirection) curve = 1;
                direction = nextDirection;
            }
            placements.add(new Placement(current, expressPlacementState(facing, curve)));
            if (current.equals(finalTarget)) return placements;
            current = current.relative(direction);
        }
        return List.of();
    }

    private static Direction expressCurveTwoDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Express conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction expressCurveOneDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Express conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction expressTargetDirection(BlockPos from, BlockPos target, BlockPos finalTarget, Direction heading) {
        if (Math.abs(from.getX() - target.getX()) > Math.abs(from.getZ() - target.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
            return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        }
        if (heading == Direction.NORTH || heading == Direction.SOUTH) return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static Direction expressConveyorFacing(Direction direction) { return direction.getOpposite(); }

    private static BlockState expressPlacementState(Direction facing, int curve) {
        return HbmBlocks.CONVEYOR_EXPRESS.get().defaultBlockState()
                .setValue(ConveyorBlock.FACING, facing)
                .setValue(ConveyorBlock.CURVE, curve);
    }

    private static int expressManhattan(BlockPos one, BlockPos two) {
        return Math.abs(one.getX() - two.getX()) + Math.abs(one.getY() - two.getY()) + Math.abs(one.getZ() - two.getZ());
    }

    private static List<Placement> buildDoublePath(Level level, Player player, BlockPos first, Direction firstSide,
                                                   BlockPos target, Direction targetSide, int max) {
        if (max <= 0) return List.of();
        if (first.equals(target) && firstSide == targetSide && firstSide.getAxis().isVertical()) {
            BlockPos pos = first.relative(firstSide);
            return isReplaceable(level, pos) ? List.of(new Placement(pos, doublePlacementState(player.getDirection().getOpposite(), 0))) : null;
        }
        BlockPos finalTarget = target.relative(targetSide);
        BlockPos current = first.relative(firstSide);
        Direction direction = firstSide.getAxis().isVertical() ? doubleTargetDirection(current, target, target, null) : firstSide;
        BlockState targetState = level.getBlockState(target);
        boolean turnToTarget = targetSide.getAxis().isHorizontal()
                || isLegacyCraneBase(targetState)
                || (targetState.getBlock() instanceof ConveyorBlock conveyor
                && (conveyor.kind() == ConveyorBlock.Kind.LIFT || conveyor.kind() == ConveyorBlock.Kind.CHUTE));
        List<Placement> placements = new ArrayList<>();
        for (int depth = 1; depth <= max; depth++) {
            if (!isReplaceable(level, current)) return null;
            Direction facing = doubleConveyorFacing(direction);
            int curve = 0;
            BlockPos next = current.relative(direction);
            int fromDistance = doubleManhattan(current, finalTarget);
            int toDistance = doubleManhattan(next, finalTarget);
            int finalDistance = doubleManhattan(next, target);
            boolean notAtTarget = (turnToTarget ? finalDistance : fromDistance) > 0;
            boolean obstructed = notAtTarget && !isReplaceable(level, next);
            if ((toDistance >= fromDistance && notAtTarget) || obstructed) {
                Direction nextDirection = doubleTargetDirection(current, turnToTarget ? target : finalTarget, finalTarget, direction);
                if (direction.getAxis().isHorizontal() && doubleCurveTwoDirection(direction) == nextDirection) curve = 2;
                else if (direction.getAxis().isHorizontal() && doubleCurveOneDirection(direction) == nextDirection) curve = 1;
                direction = nextDirection;
            }
            placements.add(new Placement(current, doublePlacementState(facing, curve)));
            if (current.equals(finalTarget)) return placements;
            current = current.relative(direction);
        }
        return List.of();
    }

    private static Direction doubleCurveTwoDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Double conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction doubleCurveOneDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Double conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction doubleTargetDirection(BlockPos from, BlockPos target, BlockPos finalTarget, Direction heading) {
        if (Math.abs(from.getX() - target.getX()) > Math.abs(from.getZ() - target.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
            return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        }
        if (heading == Direction.NORTH || heading == Direction.SOUTH) return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static Direction doubleConveyorFacing(Direction direction) { return direction.getOpposite(); }

    private static BlockState doublePlacementState(Direction facing, int curve) {
        return HbmBlocks.CONVEYOR_DOUBLE.get().defaultBlockState()
                .setValue(ConveyorBlock.FACING, facing)
                .setValue(ConveyorBlock.CURVE, curve);
    }

    private static int doubleManhattan(BlockPos one, BlockPos two) {
        return Math.abs(one.getX() - two.getX()) + Math.abs(one.getY() - two.getY()) + Math.abs(one.getZ() - two.getZ());
    }

    private static List<Placement> buildTriplePath(Level level, Player player, BlockPos first, Direction firstSide,
                                                   BlockPos target, Direction targetSide, int max) {
        if (max <= 0) return List.of();
        if (first.equals(target) && firstSide == targetSide && firstSide.getAxis().isVertical()) {
            BlockPos pos = first.relative(firstSide);
            return isReplaceable(level, pos) ? List.of(new Placement(pos, triplePlacementState(player.getDirection().getOpposite(), 0))) : null;
        }
        BlockPos finalTarget = target.relative(targetSide);
        BlockPos current = first.relative(firstSide);
        Direction direction = firstSide.getAxis().isVertical() ? tripleTargetDirection(current, target, target, null) : firstSide;
        BlockState targetState = level.getBlockState(target);
        boolean turnToTarget = targetSide.getAxis().isHorizontal()
                || isLegacyCraneBase(targetState)
                || (targetState.getBlock() instanceof ConveyorBlock conveyor
                && (conveyor.kind() == ConveyorBlock.Kind.LIFT || conveyor.kind() == ConveyorBlock.Kind.CHUTE));
        List<Placement> placements = new ArrayList<>();
        for (int depth = 1; depth <= max; depth++) {
            if (!isReplaceable(level, current)) return null;
            Direction facing = tripleConveyorFacing(direction);
            int curve = 0;
            BlockPos next = current.relative(direction);
            int fromDistance = tripleManhattan(current, finalTarget);
            int toDistance = tripleManhattan(next, finalTarget);
            int finalDistance = tripleManhattan(next, target);
            boolean notAtTarget = (turnToTarget ? finalDistance : fromDistance) > 0;
            boolean obstructed = notAtTarget && !isReplaceable(level, next);
            if ((toDistance >= fromDistance && notAtTarget) || obstructed) {
                Direction nextDirection = tripleTargetDirection(current, turnToTarget ? target : finalTarget, finalTarget, direction);
                if (direction.getAxis().isHorizontal() && tripleCurveTwoDirection(direction) == nextDirection) curve = 2;
                else if (direction.getAxis().isHorizontal() && tripleCurveOneDirection(direction) == nextDirection) curve = 1;
                direction = nextDirection;
            }
            placements.add(new Placement(current, triplePlacementState(facing, curve)));
            if (current.equals(finalTarget)) return placements;
            current = current.relative(direction);
        }
        return List.of();
    }

    private static Direction tripleCurveTwoDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Triple conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction tripleCurveOneDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Triple conveyor wand curve requires a horizontal direction: " + direction);
        };
    }

    private static Direction tripleTargetDirection(BlockPos from, BlockPos target, BlockPos finalTarget, Direction heading) {
        if (Math.abs(from.getX() - target.getX()) > Math.abs(from.getZ() - target.getZ())) {
            if (heading == Direction.EAST || heading == Direction.WEST) return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
            return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        }
        if (heading == Direction.NORTH || heading == Direction.SOUTH) return from.getX() > target.getX() ? Direction.WEST : Direction.EAST;
        return from.getZ() > target.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static Direction tripleConveyorFacing(Direction direction) { return direction.getOpposite(); }

    private static BlockState triplePlacementState(Direction facing, int curve) {
        return HbmBlocks.CONVEYOR_TRIPLE.get().defaultBlockState()
                .setValue(ConveyorBlock.FACING, facing)
                .setValue(ConveyorBlock.CURVE, curve);
    }

    private static int tripleManhattan(BlockPos one, BlockPos two) {
        return Math.abs(one.getX() - two.getX()) + Math.abs(one.getY() - two.getY()) + Math.abs(one.getZ() - two.getZ());
    }

    private static boolean isReplaceable(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    private static void refreshPlacedConveyors(Level level, List<Placement> placements) {
        for (Placement placement : placements) {
            ConveyorBlock.refreshVisualStateAt(level, placement.pos());
            for (Direction direction : Direction.values()) {
                ConveyorBlock.refreshVisualStateAt(level, placement.pos().relative(direction));
            }
        }
    }

    private static boolean isLegacyCraneBase(BlockState state) {
        if (!(state.getBlock() instanceof CraneMachineBlock crane)) return false;
        return switch (crane.kind()) {
            case BOXER, EXTRACTOR, GRABBER, INSERTER, UNBOXER -> true;
            case PARTITIONER, ROUTER, SPLITTER -> false;
        };
    }

    private static int countAvailable(Player player, ConveyorType type) {
        int count = 0;
        for (ItemStack candidate : player.getInventory().items) if (candidate.getItem() instanceof ConveyorWandItem wand && wand.type(candidate) == type) count += candidate.getCount();
        return count;
    }

    private static void consume(Player player, ConveyorType type, int amount) {
        for (ItemStack candidate : player.getInventory().items) {
            if (!(candidate.getItem() instanceof ConveyorWandItem wand) || wand.type(candidate) != type) continue;
            int removed = Math.min(amount, candidate.getCount());
            candidate.shrink(removed);
            amount -= removed;
            if (amount <= 0) return;
        }
    }

    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static void save(ItemStack stack, CompoundTag tag) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); }
    private static boolean selecting(CompoundTag tag) { return tag.getBoolean(SELECTING); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (selected || !(entity instanceof Player player) || !selecting(data(stack))) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() == this && type(held) == type(stack)) return;
        CompoundTag tag = data(stack);
        tag.remove(SELECTING);
        save(stack, tag);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= 5; line++) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.conveyor_wand." + line).withStyle(ChatFormatting.YELLOW));
        }
        if (type(stack) == ConveyorType.REGULAR) tooltip.add(Component.translatable("desc.reinhardtshbm.conveyor_wand.vertical").withStyle(ChatFormatting.AQUA));
        if (selecting(data(stack))) tooltip.add(Component.translatable("desc.reinhardtshbm.conveyor_wand.selecting").withStyle(ChatFormatting.YELLOW));
    }

    private record Placement(BlockPos pos, BlockState state) { }

    public enum ConveyorType {
        REGULAR("regular"), EXPRESS("express"), DOUBLE("double"), TRIPLE("triple");
        private final String id;
        ConveyorType(String id) { this.id = id; }
        public String id() { return id; }
        private static ConveyorType byId(String id) {
            for (ConveyorType type : values()) if (type.id.equals(id)) return type;
            return REGULAR;
        }
    }
}
