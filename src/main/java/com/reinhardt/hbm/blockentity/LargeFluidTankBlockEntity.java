package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeFluidTankBlock;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class LargeFluidTankBlockEntity extends FluidTankBlockEntity {
    public static final int CAPACITY = 16_000_000;
    private static final int FLOOR_CHECK_COUNT = 16;

    private boolean tilted;
    private int floorBlocksChecked;
    private int floorBlocksValid;
    private volatile RenderSnapshot renderSnapshot;

    public LargeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LARGE_FLUID_TANK.get(), pos, state, CAPACITY,
                "container.reinhardtshbm.big_ass_tank");
        this.renderSnapshot = buildRenderSnapshot();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LargeFluidTankBlockEntity tank) {
        tank.checkTilt(level);
        FluidTankBlockEntity.tick(level, pos, state, tank);
        if (!tank.tank().type().isNone() && tank.tank().type().hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    10.0F, true, Level.ExplosionInteraction.BLOCK);
        }
    }

    public boolean tilted() {
        return this.tilted;
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    @Override
    public boolean stillValid(Player player) {
        if (MachineDummyMenuAccess.validDummyAccess(this, player)) {
            return true;
        }
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        Direction facing = getBlockState().hasProperty(LargeFluidTankBlock.FACING)
                ? getBlockState().getValue(LargeFluidTankBlock.FACING)
                : Direction.SOUTH;
        for (BlockPos part : LargeFluidTankBlock.occupiedPositions(this.worldPosition, facing)) {
            if (player.distanceToSqr(
                    part.getX() + 0.5D,
                    part.getY() + 0.5D,
                    part.getZ() + 0.5D
            ) <= 64.0D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeFluidTankBlock.FACING)
                ? state.getValue(LargeFluidTankBlock.FACING)
                : Direction.SOUTH;
        return List.of(
                new Port(this.worldPosition.relative(facing, 6), facing),
                new Port(this.worldPosition.relative(facing.getOpposite(), 6), facing.getOpposite())
        );
    }

    @Override
    protected int maxProviderTransfer() {
        return Math.max(50_000, tank().amount() / 100);
    }

    @Override
    protected int maxReceiverTransfer() {
        return Math.max(50_000, (tank().capacity() - tank().amount()) / 100);
    }

    private void checkTilt(Level level) {
        if (Math.floorMod(level.getGameTime() + this.worldPosition.hashCode(), 20L) != 0L) {
            return;
        }

        if (this.floorBlocksChecked >= FLOOR_CHECK_COUNT) {
            boolean nextTilted = this.floorBlocksValid < this.floorBlocksChecked * 0.95D;
            if (nextTilted && !this.tilted) {
                level.playSound(null, this.worldPosition, HbmSoundEvents.METAL_IMPACT.get(),
                        SoundSource.BLOCKS, 3.0F, 1.0F);
            }
            if (nextTilted != this.tilted) {
                this.tilted = nextTilted;
                syncTilt();
            }
            this.floorBlocksChecked = 0;
            this.floorBlocksValid = 0;
        }

        int index = this.floorBlocksChecked;
        BlockPos floorPos = this.worldPosition.offset(-3 + index / 4 * 2, -1, -3 + index % 4 * 2);
        this.floorBlocksChecked++;
        if (isValidHeavyFoundation(level, floorPos)) {
            this.floorBlocksValid++;
        }
    }

    private static boolean isValidHeavyFoundation(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isFaceSturdy(level, pos, Direction.UP) || !state.isSolidRender(level, pos)) {
            return false;
        }
        if (state.is(BlockTags.SAND) || state.is(BlockTags.WOOL) || state.is(BlockTags.DIRT)) {
            return false;
        }
        if (state.is(HbmBlocks.DIRT_DEAD.get()) || state.is(HbmBlocks.DIRT_OILY.get())
                || state.is(HbmBlocks.STONE_CRACKED.get())) {
            return false;
        }
        return state.getBlock().getExplosionResistance() >= Blocks.STONE.getExplosionResistance();
    }

    private void syncTilt() {
        setChanged();
        if (this.level != null) {
            RenderSnapshot previous = this.renderSnapshot;
            RenderSnapshot next = buildRenderSnapshot();
            this.renderSnapshot = next;
            if (!this.level.isClientSide || previous != null
                    && !sameSectionCoverage(previous.bounds(), next.bounds())) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        } else {
            this.renderSnapshot = buildRenderSnapshot();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Tilted", this.tilted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tilted = tag.getBoolean("Tilted");
        RenderSnapshot previous = this.renderSnapshot;
        RenderSnapshot next = buildRenderSnapshot();
        this.renderSnapshot = next;
        if (this.level != null && this.level.isClientSide && previous != null
                && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private RenderSnapshot buildRenderSnapshot() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(LargeFluidTankBlock.FACING)
                ? state.getValue(LargeFluidTankBlock.FACING) : Direction.SOUTH;
        BlockPos pos = this.worldPosition;
        AABB bounds;
        if (!this.tilted) {
            bounds = new AABB(pos.getX() - 6.0D, pos.getY() - 0.125D, pos.getZ() - 6.0D,
                    pos.getX() + 7.0D, pos.getY() + 6.5D, pos.getZ() + 7.0D);
        } else {
            bounds = switch (facing) {
                case WEST -> relativeBounds(pos,
                        -6.093455621661639D, -2.131985263329315D, -5.228119688339022D,
                        6.919807529826361D, 5.487744106676333D, 6.228119688339022D);
                case SOUTH -> relativeBounds(pos,
                        -6.009337567592793D, -1.951430666587992D, -6.0188434089701754D,
                        5.8958316137105395D, 5.487744091541897D, 7.018843321814432D);
                case EAST -> relativeBounds(pos,
                        -6.093455707493291D, -2.131985278463751D, -5.228119688339022D,
                        6.919807443994709D, 5.487744091541897D, 6.228119688339022D);
                default -> relativeBounds(pos,
                        -6.009337567592793D, -1.9514306968568635D, -6.018843321814432D,
                        5.895831442047237D, 5.487744091541897D, 7.0188434089701754D);
            };
        }
        return new RenderSnapshot(state, facing, this.tilted, bounds);
    }

    private static AABB relativeBounds(BlockPos pos,
                                       double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ) {
        return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return section(first.minX) == section(second.minX)
                && section(inclusiveMaximum(first.minX, first.maxX)) == section(inclusiveMaximum(second.minX, second.maxX))
                && section(first.minY) == section(second.minY)
                && section(inclusiveMaximum(first.minY, first.maxY)) == section(inclusiveMaximum(second.minY, second.maxY))
                && section(first.minZ) == section(second.minZ)
                && section(inclusiveMaximum(first.minZ, first.maxZ)) == section(inclusiveMaximum(second.minZ, second.maxZ));
    }

    private static int section(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }

    public record RenderSnapshot(BlockState blockState, Direction facing, boolean tilted, AABB bounds) {
    }
}
