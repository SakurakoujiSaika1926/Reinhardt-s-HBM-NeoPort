package com.reinhardt.hbm.util;

import com.reinhardt.hbm.block.LargeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Centralized transforms for 1.7.10 HBM dummyable machines.
 *
 * Old HBM's MultiblockHandlerXR uses SOUTH as the unrotated basis. Keeping that
 * basis explicit avoids mixing footprint, renderer and port rotations.
 */
public final class LegacyMachineGeometry {
    private LegacyMachineGeometry() {
    }

    public static BlockPos rotate(BlockPos offset, Direction facing, LargeMachineBlock.RotationBasis basis) {
        if (basis == LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH) {
            return rotateLegacySouth(offset, facing);
        }
        return rotateModernNorth(offset, facing);
    }

    public static BlockPos rotateLegacySouth(BlockPos offset, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> offset;
        };
    }

    public static BlockPos rotateModernNorth(BlockPos offset, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> offset;
        };
    }

    public static Direction rotateDirection(Direction direction, Direction facing, LargeMachineBlock.RotationBasis basis) {
        if (direction.getAxis().isVertical()) {
            return direction;
        }
        BlockPos rotated = rotate(new BlockPos(direction.getStepX(), 0, direction.getStepZ()), facing, basis);
        return Direction.fromDelta(rotated.getX(), 0, rotated.getZ());
    }

    /**
     * Exact 1.7.10 MultiblockHandlerXR.rotate() dimensions.
     * Array order: {up, down, north, south, west, east}; SOUTH is unrotated.
     */
    public static int[] rotateLegacyDimensions(int[] dim, Direction facing) {
        return switch (facing) {
            case NORTH -> new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim.clone();
        };
    }

    public static int[] rotateLegacyDimensions(int up, int down, int north, int south, int west, int east, Direction facing) {
        return rotateLegacyDimensions(new int[]{up, down, north, south, west, east}, facing);
    }

    public static void addLegacyBox(Set<BlockPos> positions, BlockPos corePos, Direction facing, int up, int down, int north, int south, int west, int east) {
        int[] rot = rotateLegacyDimensions(up, down, north, south, west, east, facing);
        for (int x = -rot[4]; x <= rot[5]; x++) {
            for (int y = -rot[1]; y <= rot[0]; y++) {
                for (int z = -rot[2]; z <= rot[3]; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
    }

    public static List<BlockPos> positionsForLegacyFootprint(BlockPos corePos, Direction facing, LargeMachineBlock.Footprint footprint) {
        ArrayList<BlockPos> positions = new ArrayList<>(footprint.offsets().size());
        for (BlockPos offset : footprint.offsets()) {
            positions.add(corePos.offset(rotateLegacySouth(offset, facing)));
        }
        return List.copyOf(positions);
    }

    public static Set<BlockPos> newOrderedPosSet() {
        return new LinkedHashSet<>();
    }

    /**
     * ForgeDirection#getRotation(UP) from 1.7.10. This is the old code's
     * left-hand side direction and is not Minecraft's clockwise API.
     */
    public static Direction forgeRotateUp(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case EAST -> Direction.SOUTH;
            default -> facing;
        };
    }

    /**
     * ForgeDirection#getRotation(DOWN) from 1.7.10.
     */
    public static Direction forgeRotateDown(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
            default -> facing;
        };
    }

    public static BlockPos legacyOffset(Direction facing, int alongFacing, Direction side, int alongSide, int y) {
        return new BlockPos(
                facing.getStepX() * alongFacing + side.getStepX() * alongSide,
                y,
                facing.getStepZ() * alongFacing + side.getStepZ() * alongSide
        );
    }

    public static float yawModernNorth(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    public static float yawLegacySouth(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    public static float legacyWavefrontYaw(Direction facing, float modelSouthYaw) {
        return normalizeDegrees(modelSouthYaw + yawLegacySouth(facing));
    }

    public static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }
}
