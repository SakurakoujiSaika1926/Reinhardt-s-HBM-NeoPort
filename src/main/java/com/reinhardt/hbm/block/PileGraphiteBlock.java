package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PileGraphiteBlockEntity;
import com.reinhardt.hbm.item.PileRodItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The complete 1.7.10 Chicago Pile graphite block family. Metadata bits from
 * the legacy implementation map directly to axis, aluminium shrouding, and
 * active/bred state properties.
 */
public final class PileGraphiteBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty ALUMINIUM = BooleanProperty.create("aluminium");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final Kind kind;

    public PileGraphiteBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        BlockState state = this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(ALUMINIUM, false)
                .setValue(ACTIVE, false);
        this.registerDefaultState(state);
    }

    public Kind kind() {
        return this.kind;
    }

    /** Exact 1.7.10 fan callback for a fuel pile. */
    public static void applyFan(Level level, BlockPos pos, Direction direction, int distance) {
        if (level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile) {
            pile.coolByFan();
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 30;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return this.kind == Kind.FUEL;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (this.kind != Kind.FUEL || !(level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile)) {
            return 0;
        }
        return pile.comparatorOutput();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.kind.hasBlockEntity() ? new PileGraphiteBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !this.kind.hasBlockEntity()) {
            return null;
        }
        return createTicker(type, HbmBlockEntities.PILE_GRAPHITE.get(), PileGraphiteBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (this.kind == Kind.CONTROL && isAxisFace(state, hitResult.getDirection()) && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                toggleControlRods(level, pos, state);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        Direction side = hitResult.getDirection();
        if (!isAxisFace(state, side)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (this.kind == Kind.GRAPHITE && ScrewdriverItem.isHandDrill(stack)) {
            if (!level.isClientSide) {
                level.setBlock(pos, stateFor(Kind.DRILLED, side.getAxis(), false, false), Block.UPDATE_ALL);
                eject(level, pos, side, new ItemStack(HbmItems.INGOT_GRAPHITE.get()));
                level.playSound(null, pos, SoundType.METAL.getBreakSound(), SoundSource.BLOCKS, 0.8F, 0.8F);
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (this.kind == Kind.DRILLED) {
            if (ScrewdriverItem.isScrewdriver(stack) && state.getValue(ALUMINIUM)) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(ALUMINIUM, false), Block.UPDATE_ALL);
                    eject(level, pos, side, aluminiumShell());
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.85F);
                    ScrewdriverItem.damageTool(stack, level, player, hand);
                }
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (tryInsert(level, pos, state, player, hand, stack)) {
                return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (this.kind == Kind.CONTROL && !player.isShiftKeyDown() && !ScrewdriverItem.isScrewdriver(stack)) {
            if (!level.isClientSide) {
                toggleControlRods(level, pos, state);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (this.kind == Kind.DETECTOR && ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile) {
                pile.adjustMaxNeutrons(player.isShiftKeyDown() ? -1 : 1);
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (this.kind == Kind.DETECTOR && ScrewdriverItem.isScrewdriver(stack) && player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile
                    && player instanceof ServerPlayer serverPlayer) {
                pile.sendInspection(serverPlayer);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (ScrewdriverItem.isHandDrill(stack) && this.kind.supportsInspection()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof PileGraphiteBlockEntity pile) {
                pile.sendInspection(serverPlayer);
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (ScrewdriverItem.isScrewdriver(stack) && this.kind.insertedRod() != null) {
            if (!level.isClientSide) {
                extractRod(level, pos, state, side, player, hand, stack);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private boolean tryInsert(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(HbmItems.INGOT_GRAPHITE.get()) && !state.getValue(ALUMINIUM)) {
            if (!level.isClientSide) {
                level.setBlock(pos, stateFor(Kind.GRAPHITE, state.getValue(AXIS), false, false), Block.UPDATE_ALL);
                consumeHeld(player, hand, stack);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.75F, 1.0F);
            }
            return true;
        }

        if (isAluminiumShell(stack) && !state.getValue(ALUMINIUM)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(ALUMINIUM, true), Block.UPDATE_ALL);
                consumeHeld(player, hand, stack);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.75F, 1.0F);
            }
            return true;
        }

        Kind target = targetFor(stack);
        if (target == null) {
            return false;
        }

        if (!level.isClientSide) {
            boolean active = stack.is(HbmItems.PILE_ROD_PU239.get());
            level.setBlock(pos, stateFor(target, state.getValue(AXIS), state.getValue(ALUMINIUM), active), Block.UPDATE_ALL);
            consumeHeld(player, hand, stack);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.75F, 1.0F);
        }
        return true;
    }

    private void extractRod(Level level, BlockPos pos, BlockState state, Direction side, Player player, InteractionHand hand, ItemStack screwdriver) {
        Item rod = this.kind.insertedRod();
        if (this.kind == Kind.FUEL && state.getValue(ACTIVE)) {
            rod = HbmItems.PILE_ROD_PU239.get();
        }
        level.setBlock(pos, stateFor(Kind.DRILLED, state.getValue(AXIS), state.getValue(ALUMINIUM), false), Block.UPDATE_ALL);
        eject(level, pos, side, new ItemStack(rod));
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
        ScrewdriverItem.damageTool(screwdriver, level, player, hand);
    }

    private static void toggleControlRods(Level level, BlockPos pos, BlockState state) {
        boolean previous = state.getValue(ACTIVE);
        boolean next = !previous;
        if (level.getBlockState(pos).getBlock() instanceof PileGraphiteBlock block && block.kind == Kind.CONTROL) {
            level.setBlock(pos, state.setValue(ACTIVE, next), Block.UPDATE_ALL);
        }

        Direction axis = Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.POSITIVE);
        for (Direction direction : List.of(axis, axis.getOpposite())) {
            BlockPos cursor = pos.relative(direction);
            while (level.getBlockState(cursor).getBlock() instanceof PileGraphiteBlock block
                    && block.kind == Kind.CONTROL) {
                BlockState rodState = level.getBlockState(cursor);
                if (rodState.getValue(AXIS) != state.getValue(AXIS)
                        || rodState.getValue(ALUMINIUM) != state.getValue(ALUMINIUM)
                        || rodState.getValue(ACTIVE) != previous) {
                    break;
                }
                level.setBlock(cursor, rodState.setValue(ACTIVE, next), Block.UPDATE_ALL);
                cursor = cursor.relative(direction);
            }
        }
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, next ? 0.65F : 0.75F);
    }

    public static void toggleDetectorRods(Level level, BlockPos pos, BlockState detectorState) {
        boolean previous = detectorState.getValue(ACTIVE);
        boolean next = !previous;
        level.setBlock(pos, detectorState.setValue(ACTIVE, next), Block.UPDATE_ALL);

        Direction axis = Direction.fromAxisAndDirection(detectorState.getValue(AXIS), Direction.AxisDirection.POSITIVE);
        for (Direction direction : List.of(axis, axis.getOpposite())) {
            BlockPos cursor = pos.relative(direction);
            while (level.getBlockState(cursor).getBlock() instanceof PileGraphiteBlock block
                    && block.kind == Kind.CONTROL) {
                BlockState rodState = level.getBlockState(cursor);
                if (rodState.getValue(AXIS) != detectorState.getValue(AXIS)
                        || rodState.getValue(ALUMINIUM) != detectorState.getValue(ALUMINIUM)
                        || rodState.getValue(ACTIVE) != previous) {
                    break;
                }
                level.setBlock(cursor, rodState.setValue(ACTIVE, next), Block.UPDATE_ALL);
                cursor = cursor.relative(direction);
            }
        }
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.15F, 1.0F);
    }

    public static BlockState stateFor(Kind kind, Direction.Axis axis, boolean aluminium, boolean active) {
        BlockState state = blockFor(kind).defaultBlockState()
                .setValue(AXIS, axis)
                .setValue(ALUMINIUM, aluminium);
        if (kind.usesActiveState()) {
            state = state.setValue(ACTIVE, active);
        }
        return state;
    }

    /**
     * Recreates {@code BlockGraphiteDrilledBase.insertItem}: automation may
     * feed a rod through either axial face, shifting at most three occupied
     * graphite channels and preserving each channel's reactor state.
     */
    public static boolean insertAutomatedRod(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        Kind incomingKind = targetFor(stack);
        if (incomingKind == null || stack.isEmpty()) {
            return false;
        }

        BlockState initialState = level.getBlockState(pos);
        if (!(initialState.getBlock() instanceof PileGraphiteBlock initial)
                || initial.kind == Kind.GRAPHITE
                || initialState.getValue(AXIS) != side.getAxis()) {
            return false;
        }

        List<PileSegment> segments = new ArrayList<>(4);
        boolean hasEmptyChannel = false;
        BlockPos cursor = pos;
        for (int index = 0; index <= 3; index++) {
            BlockState segmentState = level.getBlockState(cursor);
            if (segmentState.getBlock() instanceof PileGraphiteBlock segment
                    && segment.kind != Kind.GRAPHITE) {
                if (segmentState.getValue(AXIS) != initialState.getValue(AXIS)) {
                    return false;
                }
                PileGraphiteBlockEntity.PileState pileState = level.getBlockEntity(cursor) instanceof PileGraphiteBlockEntity pile
                        ? pile.snapshot()
                        : null;
                segments.add(new PileSegment(cursor.immutable(), segmentState, pileState));
                if (segment.kind == Kind.DRILLED) {
                    hasEmptyChannel = true;
                    break;
                }
                if (index == 3) {
                    return false;
                }
                cursor = cursor.relative(side);
                continue;
            }

            // Legacy code could eject the terminal rod into any non-solid space.
            if (segmentState.isSolidRender(level, cursor)) {
                return false;
            }
            break;
        }

        if (segments.isEmpty()) {
            return false;
        }
        if (simulate) {
            return true;
        }

        BlockState carriedState = stateFor(
                incomingKind,
                initialState.getValue(AXIS),
                initialState.getValue(ALUMINIUM),
                stack.is(HbmItems.PILE_ROD_PU239.get())
        );
        PileGraphiteBlockEntity.PileState carriedPileState = null;

        for (PileSegment segment : segments) {
            PileGraphiteBlock carriedBlock = (PileGraphiteBlock) carriedState.getBlock();
            BlockState placedState = stateFor(
                    carriedBlock.kind,
                    segment.state.getValue(AXIS),
                    segment.state.getValue(ALUMINIUM),
                    carriedBlock.kind.usesActiveState() && carriedState.getValue(ACTIVE)
            );

            level.setBlock(segment.pos, placedState, Block.UPDATE_ALL);
            if (carriedPileState != null
                    && level.getBlockEntity(segment.pos) instanceof PileGraphiteBlockEntity placedPile) {
                placedPile.restore(carriedPileState);
            }

            carriedState = segment.state;
            carriedPileState = segment.pileState;
        }

        if (!hasEmptyChannel) {
            Item ejected = itemForState(carriedState);
            if (ejected != null) {
                eject(level, segments.getLast().pos, side, new ItemStack(ejected));
            }
        }
        return true;
    }

    private static PileGraphiteBlock blockFor(Kind kind) {
        return switch (kind) {
            case GRAPHITE -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE.get();
            case DRILLED -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_DRILLED.get();
            case FUEL -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_FUEL.get();
            case PLUTONIUM -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_PLUTONIUM.get();
            case CONTROL -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_ROD.get();
            case SOURCE -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_SOURCE.get();
            case LITHIUM -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_LITHIUM.get();
            case TRITIUM -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_TRITIUM.get();
            case DETECTOR -> (PileGraphiteBlock) HbmBlocks.BLOCK_GRAPHITE_DETECTOR.get();
        };
    }

    private static Kind targetFor(ItemStack stack) {
        if (stack.is(HbmItems.PILE_ROD_URANIUM.get()) || stack.is(HbmItems.PILE_ROD_PU239.get())) {
            return Kind.FUEL;
        }
        if (stack.is(HbmItems.PILE_ROD_PLUTONIUM.get())) {
            return Kind.PLUTONIUM;
        }
        if (stack.is(HbmItems.PILE_ROD_SOURCE.get())) {
            return Kind.SOURCE;
        }
        if (stack.is(HbmItems.PILE_ROD_BORON.get())) {
            return Kind.CONTROL;
        }
        if (stack.is(HbmItems.PILE_ROD_LITHIUM.get())) {
            return Kind.LITHIUM;
        }
        if (stack.is(HbmItems.CELL_TRITIUM.get())) {
            return Kind.TRITIUM;
        }
        if (stack.is(HbmItems.PILE_ROD_DETECTOR.get())) {
            return Kind.DETECTOR;
        }
        return null;
    }

    @Nullable
    private static Item itemForState(BlockState state) {
        if (!(state.getBlock() instanceof PileGraphiteBlock block)) {
            return null;
        }
        if (block.kind == Kind.FUEL && state.getValue(ACTIVE)) {
            return HbmItems.PILE_ROD_PU239.get();
        }
        return block.kind.insertedRod();
    }

    private static boolean isAluminiumShell(ItemStack stack) {
        return !stack.isEmpty() && itemPath(stack).equals("shell_aluminium");
    }

    private static ItemStack aluminiumShell() {
        return new ItemStack(BuiltInRegistries.ITEM.get(hbmItem("shell_aluminium")));
    }

    private static String itemPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("reinhardtshbm") ? id.getPath() : "";
    }

    private static ResourceLocation hbmItem(String path) {
        return ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path);
    }

    private static boolean isAxisFace(BlockState state, Direction side) {
        return state.getValue(AXIS) == side.getAxis();
    }

    private static void consumeHeld(Player player, InteractionHand hand, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void eject(Level level, BlockPos pos, Direction direction, ItemStack stack) {
        ItemEntity entity = new ItemEntity(
                level,
                pos.getX() + 0.5D + direction.getStepX() * 0.75D,
                pos.getY() + 0.5D + direction.getStepY() * 0.75D,
                pos.getZ() + 0.5D + direction.getStepZ() * 0.75D,
                stack
        );
        entity.setDeltaMovement(direction.getStepX() * 0.25D, direction.getStepY() * 0.25D, direction.getStepZ() * 0.25D);
        level.addFreshEntity(entity);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.kind == Kind.GRAPHITE) {
            return super.getDrops(state, params);
        }
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(HbmItems.INGOT_GRAPHITE.get(), 8));
        if (state.getValue(ALUMINIUM)) {
            drops.add(aluminiumShell());
        }
        Item rod = this.kind.insertedRod();
        if (rod != null) {
            if (this.kind == Kind.FUEL && state.getValue(ACTIVE)) {
                rod = HbmItems.PILE_ROD_PU239.get();
            }
            drops.add(new ItemStack(rod));
        }
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Block invokes this override from its constructor, before this.kind is assigned.
        // Keep the state schema shared by the entire pile family; inactive kinds retain false.
        builder.add(AXIS, ALUMINIUM, ACTIVE);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }

    private record PileSegment(BlockPos pos, BlockState state, @Nullable PileGraphiteBlockEntity.PileState pileState) {
    }

    public enum Kind {
        GRAPHITE(false, false, false),
        DRILLED(false, false, false),
        FUEL(true, true, true),
        PLUTONIUM(true, false, false),
        CONTROL(false, true, false),
        SOURCE(true, false, false),
        LITHIUM(true, false, true),
        TRITIUM(false, false, false),
        DETECTOR(true, true, false);

        private final boolean hasBlockEntity;
        private final boolean usesActiveState;
        private final boolean supportsInspection;

        Kind(boolean hasBlockEntity, boolean usesActiveState, boolean supportsInspection) {
            this.hasBlockEntity = hasBlockEntity;
            this.usesActiveState = usesActiveState;
            this.supportsInspection = supportsInspection;
        }

        public boolean hasBlockEntity() {
            return this.hasBlockEntity;
        }

        public boolean usesActiveState() {
            return this.usesActiveState;
        }

        public boolean supportsInspection() {
            return this.supportsInspection;
        }

        @Nullable
        public Item insertedRod() {
            return switch (this) {
                case FUEL -> HbmItems.PILE_ROD_URANIUM.get();
                case PLUTONIUM -> HbmItems.PILE_ROD_PLUTONIUM.get();
                case CONTROL -> HbmItems.PILE_ROD_BORON.get();
                case SOURCE -> HbmItems.PILE_ROD_SOURCE.get();
                case LITHIUM -> HbmItems.PILE_ROD_LITHIUM.get();
                case TRITIUM -> HbmItems.CELL_TRITIUM.get();
                case DETECTOR -> HbmItems.PILE_ROD_DETECTOR.get();
                default -> null;
            };
        }
    }
}
