package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LaunchPadBlock extends LargeMachineBlock implements EntityBlock {
    public enum Kind {
        SILO,
        LARGE,
        RUSTED
    }

    private static final List<Box> SMALL_BOXES = List.of(
            new Box(-1.5D, 0.0D, -1.5D, -0.5D, 1.0D, -0.5D),
            new Box(0.5D, 0.0D, -1.5D, 1.5D, 1.0D, -0.5D),
            new Box(-1.5D, 0.0D, 0.5D, -0.5D, 1.0D, 1.5D),
            new Box(0.5D, 0.0D, 0.5D, 1.5D, 1.0D, 1.5D),
            new Box(-0.5D, 0.5D, -1.5D, 0.5D, 1.0D, 1.5D),
            new Box(-1.5D, 0.5D, -0.5D, 1.5D, 1.0D, 0.5D)
    );
    private static final List<Box> LARGE_BOXES = List.of(
            new Box(-4.5D, 0.0D, -4.5D, 4.5D, 1.0D, -0.5D),
            new Box(-4.5D, 0.0D, 0.5D, 4.5D, 1.0D, 4.5D),
            new Box(-4.5D, 0.875D, -0.5D, 4.5D, 1.0D, 0.5D)
    );

    private final Kind kind;

    public LaunchPadBlock(Properties properties, Kind kind) {
        super(properties,
                kind == Kind.LARGE ? Footprint.centered(4, 1, 4) : Footprint.centered(1, 1, 1),
                Shapes.empty(),
                RotationBasis.MODERN_NORTH);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            placeDummies(level, pos, state.getValue(FACING), this.machineFootprint(), this.machineRotationBasis());
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), this.machineFootprint(), placer);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        LauncherBlockEntity.Kind launcherKind = switch (this.kind) {
            case LARGE -> LauncherBlockEntity.Kind.PAD_LARGE;
            case RUSTED -> LauncherBlockEntity.Kind.PAD_RUSTED;
            default -> LauncherBlockEntity.Kind.PAD_SMALL;
        };
        return new LauncherBlockEntity(pos, state, launcherKind);
    }

    public static VoxelShape shapeForPart(Kind kind, BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (Box box : kind == Kind.LARGE ? LARGE_BOXES : SMALL_BOXES) {
            shape = Shapes.or(shape, box.slice(offsetFromCore));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        VoxelShape slice(BlockPos offset) {
            double minXLocal = clamp(this.minX - offset.getX() + 0.5D);
            double maxXLocal = clamp(this.maxX - offset.getX() + 0.5D);
            double minYLocal = clamp(this.minY - offset.getY());
            double maxYLocal = clamp(this.maxY - offset.getY());
            double minZLocal = clamp(this.minZ - offset.getZ() + 0.5D);
            double maxZLocal = clamp(this.maxZ - offset.getZ() + 0.5D);
            if (minXLocal >= maxXLocal || minYLocal >= maxYLocal || minZLocal >= maxZLocal) {
                return Shapes.empty();
            }
            return Shapes.box(minXLocal, minYLocal, minZLocal, maxXLocal, maxYLocal, maxZLocal);
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
