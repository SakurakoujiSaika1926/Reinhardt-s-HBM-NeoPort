package com.reinhardt.hbm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class HbmRailBlock extends RailBlock {
    public static final float VANILLA_SPEED = 0.4F;

    private final float maxSpeed;
    private final boolean flexible;
    private final boolean slopable;
    private final boolean booster;

    public HbmRailBlock(BlockBehaviour.Properties properties, float maxSpeed, boolean flexible, boolean slopable, boolean booster) {
        super(properties);
        this.maxSpeed = maxSpeed;
        this.flexible = flexible;
        this.slopable = slopable;
        this.booster = booster;
    }

    public float maxSpeed() {
        return this.maxSpeed;
    }

    public boolean flexible() {
        return this.flexible;
    }

    public boolean slopable() {
        return this.slopable;
    }

    @Override
    public boolean isFlexibleRail(BlockState state, BlockGetter level, BlockPos pos) {
        return this.flexible;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter level, BlockPos pos) {
        return this.slopable;
    }

    @Override
    public boolean isValidRailShape(RailShape shape) {
        if (!this.slopable && shape.isAscending()) {
            return false;
        }
        return this.flexible || !isCurve(shape);
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return this.maxSpeed;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (!this.booster) {
            return;
        }
        Vec3 motion = cart.getDeltaMovement();
        cart.setDeltaMovement(motion.x * 1.15D, motion.y * 1.15D, motion.z * 1.15D);
    }

    private static boolean isCurve(RailShape shape) {
        return switch (shape) {
            case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST -> true;
            default -> false;
        };
    }
}
