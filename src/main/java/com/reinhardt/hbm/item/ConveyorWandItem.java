package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        ConveyorType type = type(stack);
        if (player.isShiftKeyDown() && !selecting(data(stack))) return placeSingle(context, type);

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
            tag.putInt(COUNT, player.getAbilities().instabuild ? 256 : countAvailable(player, type));
            save(stack, tag);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos start = new BlockPos(tag.getInt(START_X), tag.getInt(START_Y), tag.getInt(START_Z));
        List<Placement> placements = buildPath(level, type, player, start, direction(tag.getInt(START_SIDE)), clicked, side, tag.getInt(COUNT));
        tag.remove(SELECTING);
        save(stack, tag);
        if (!level.isClientSide) {
            if (placements == null) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.obstructed"), true);
            else if (placements.isEmpty()) player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.not_enough"), true);
            else {
                for (Placement placement : placements) level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
                refreshPlacedConveyors(level, placements);
                if (!player.getAbilities().instabuild) consume(player, type, placements.size());
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.conveyor.built"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult placeSingle(UseOnContext context, ConveyorType type) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clicked);
        Block toPlace = blockFor(type);
        if (type == ConveyorType.REGULAR && clickedState.is(HbmBlocks.CONVEYOR.get()) && clickedState.getValue(ConveyorBlock.CURVE) == 0) {
            if (face == Direction.UP) level.setBlock(clicked, HbmBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(ConveyorBlock.FACING, clickedState.getValue(ConveyorBlock.FACING)), Block.UPDATE_ALL);
            else if (face == Direction.DOWN) level.setBlock(clicked, HbmBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(ConveyorBlock.FACING, clickedState.getValue(ConveyorBlock.FACING)), Block.UPDATE_ALL);
            clickedState = level.getBlockState(clicked);
        }
        if (type == ConveyorType.REGULAR && clickedState.getBlock() instanceof ConveyorBlock conveyor) {
            if (conveyor.kind() == ConveyorBlock.Kind.LIFT && face == Direction.UP) toPlace = HbmBlocks.CONVEYOR_LIFT.get();
            if (conveyor.kind() == ConveyorBlock.Kind.CHUTE && face == Direction.DOWN) toPlace = HbmBlocks.CONVEYOR_CHUTE.get();
        }
        BlockPos target = clicked.relative(face);
        if (!isReplaceable(level, target)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(target, placementState(toPlace, context.getHorizontalDirection().getOpposite(), 0), Block.UPDATE_ALL);
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

    private static List<Placement> buildPath(Level level, ConveyorType type, Player player, BlockPos first, Direction firstSide,
                                             BlockPos target, Direction targetSide, int max) {
        if (max <= 0) return List.of();
        if (first.equals(target) && firstSide == targetSide && firstSide.getAxis().isVertical()) {
            BlockPos pos = first.relative(firstSide);
            return isReplaceable(level, pos) ? List.of(new Placement(pos, placementState(blockFor(type), player.getDirection().getOpposite(), 0))) : null;
        }
        boolean vertical = type == ConveyorType.REGULAR;
        BlockPos finalTarget = target.relative(targetSide);
        BlockPos current = first.relative(firstSide);
        Direction direction = firstSide.getAxis().isVertical() ? targetDirection(current, target, target, null, false, vertical) : firstSide;
        BlockState targetState = level.getBlockState(target);
        boolean turnToTarget = targetSide.getAxis().isHorizontal()
                || isLegacyCraneBase(targetState)
                || (targetState.getBlock() instanceof ConveyorBlock conveyor
                && (conveyor.kind() == ConveyorBlock.Kind.LIFT || conveyor.kind() == ConveyorBlock.Kind.CHUTE));
        Direction horizontal = direction.getAxis().isVertical() ? player.getDirection() : direction;
        if (vertical && current.getY() > finalTarget.getY() && isReplaceable(level, current.below())) direction = Direction.DOWN;
        List<Placement> placements = new ArrayList<>();
        for (int depth = 1; depth <= max; depth++) {
            if (!isReplaceable(level, current)) return null;
            Block block = blockForDirection(type, direction);
            Direction facing = conveyorFacing(block, direction, targetSide, horizontal);
            int curve = 0;
            BlockPos next = current.relative(direction);
            int fromDistance = manhattan(current, finalTarget);
            int toDistance = manhattan(next, finalTarget);
            int finalDistance = manhattan(next, target);
            boolean notAtTarget = (turnToTarget ? finalDistance : fromDistance) > 0;
            boolean obstructed = notAtTarget && !isReplaceable(level, next);
            if ((toDistance >= fromDistance && notAtTarget) || obstructed) {
                Direction nextDirection = targetDirection(current, turnToTarget ? target : finalTarget, finalTarget, direction, obstructed, vertical);
                if (nextDirection == Direction.UP) block = HbmBlocks.CONVEYOR_LIFT.get();
                else if (nextDirection == Direction.DOWN) block = HbmBlocks.CONVEYOR_CHUTE.get();
                else if (LegacyMachineGeometry.forgeRotateUp(direction) == nextDirection) curve = 2;
                else if (LegacyMachineGeometry.forgeRotateDown(direction) == nextDirection) curve = 1;
                direction = nextDirection;
                if (direction.getAxis().isHorizontal()) horizontal = direction;
            }
            placements.add(new Placement(current, placementState(block, facing, curve)));
            if (current.equals(finalTarget)) return placements;
            current = current.relative(direction);
        }
        return List.of();
    }

    private static Direction targetDirection(BlockPos from, BlockPos target, BlockPos finalTarget, Direction heading, boolean obstructed, boolean vertical) {
        if (vertical && (from.getY() != target.getY() || from.getY() != finalTarget.getY())
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

    private static Block blockForDirection(ConveyorType type, Direction direction) {
        if (direction == Direction.UP) return HbmBlocks.CONVEYOR_LIFT.get();
        if (direction == Direction.DOWN) return HbmBlocks.CONVEYOR_CHUTE.get();
        return blockFor(type);
    }

    private static Direction conveyorFacing(Block block, Direction direction, Direction targetSide, Direction horizontal) {
        if (block == HbmBlocks.CONVEYOR_LIFT.get() || block == HbmBlocks.CONVEYOR_CHUTE.get()) return targetSide.getAxis().isVertical() ? horizontal.getOpposite() : targetSide;
        return direction.getOpposite();
    }

    private static BlockState placementState(Block block, Direction facing, int curve) {
        BlockState state = block.defaultBlockState().setValue(ConveyorBlock.FACING, facing);
        return state.hasProperty(ConveyorBlock.CURVE) ? state.setValue(ConveyorBlock.CURVE, curve) : state;
    }

    private static Block blockFor(ConveyorType type) {
        return switch (type) {
            case REGULAR -> HbmBlocks.CONVEYOR.get();
            case EXPRESS -> HbmBlocks.CONVEYOR_EXPRESS.get();
            case DOUBLE -> HbmBlocks.CONVEYOR_DOUBLE.get();
            case TRIPLE -> HbmBlocks.CONVEYOR_TRIPLE.get();
        };
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

    private static int manhattan(BlockPos one, BlockPos two) { return Math.abs(one.getX() - two.getX()) + Math.abs(one.getY() - two.getY()) + Math.abs(one.getZ() - two.getZ()); }
    private static Direction direction(int value) { return Direction.from3DDataValue(value); }
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
