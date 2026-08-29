package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CargoElevatorBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CargoElevatorBlockEntity extends BlockEntity {
    public static final double SPEED = 2.0D / 20.0D;
    private BlockPos corePos;
    private int height;
    private double extension;
    private double previousExtension;
    private boolean extending;
    private boolean renderPlatform;

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CARGO_ELEVATOR.get(), pos, state);
        this.corePos = pos.immutable();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CargoElevatorBlockEntity elevator) {
        if (!elevator.isCore()) {
            return;
        }
        elevator.previousExtension = elevator.extension;
        if (!level.isClientSide) {
            if (elevator.extending && elevator.extension < elevator.height) {
                elevator.extension += SPEED;
            } else if (!elevator.extending && elevator.extension > 0.0D) {
                elevator.extension -= SPEED;
            }
            elevator.extension = Math.max(0.0D, Math.min(elevator.height, elevator.extension));
            elevator.renderPlatform = true;
            if (elevator.extension != elevator.previousExtension) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
        if (elevator.extension != elevator.previousExtension) {
            double lower = elevator.getBlockPos().getY() + 1.0D + Math.min(elevator.extension, elevator.previousExtension);
            double upper = elevator.getBlockPos().getY() + 1.0D + Math.max(elevator.extension, elevator.previousExtension);
            AABB box = new AABB(elevator.getBlockPos().getX() - 0.99D, lower, elevator.getBlockPos().getZ() - 0.99D,
                    elevator.getBlockPos().getX() + 1.99D, upper, elevator.getBlockPos().getZ() + 1.99D);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
                if (entity instanceof Player && !level.isClientSide) {
                    continue;
                }
                if (entity.getY() >= lower && entity.getY() <= upper) {
                    double delta = entity.getY() - (elevator.getBlockPos().getY() + 1.0D + elevator.extension);
                    entity.setPos(entity.getX(), entity.getY() - delta - 0.125D, entity.getZ());
                    entity.setOnGround(true);
                }
            }
        }
        elevator.setChanged();
    }

    public boolean isCore() { return getBlockState().getValue(CargoElevatorBlock.CORE) && getBlockPos().equals(corePos); }
    public int height() { return height; }
    public double extension(float partialTick) { return previousExtension + (extension - previousExtension) * partialTick; }
    public boolean renderPlatform() { return renderPlatform; }
    public Optional<CargoElevatorBlockEntity> core() {
        if (isCore()) return Optional.of(this);
        return level != null && level.getBlockEntity(corePos) instanceof CargoElevatorBlockEntity core && core.isCore()
                ? Optional.of(core) : Optional.empty();
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
    }

    public void toggleElevator() {
        if (extension >= height) extending = false;
        if (extension <= 0.0D) extending = true;
        setChanged();
    }

    public boolean addLayer(ItemStack stack, Player player) {
        if (level == null || !isCore()) return false;
        int y = getBlockPos().getY() + height + 1;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (!level.getBlockState(getBlockPos().offset(x, height + 1, z)).canBeReplaced()) return false;
        }
        CargoElevatorBlock.setBuilding(true);
        try {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos target = getBlockPos().offset(x, height + 1, z);
                level.setBlock(target, com.reinhardt.hbm.registry.HbmBlocks.CARGO_ELEVATOR.get().defaultBlockState().setValue(CargoElevatorBlock.CORE, false), Block.UPDATE_ALL);
                if (level.getBlockEntity(target) instanceof CargoElevatorBlockEntity segment) segment.setCorePos(getBlockPos());
            }
        } finally {
            CargoElevatorBlock.setBuilding(false);
        }
        height++;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        setChanged();
        return true;
    }

    public void removeStructure() {
        if (level == null || !isCore()) return;
        BlockPos origin = getBlockPos();
        CargoElevatorBlock.setBuilding(true);
        try {
            for (int y = 0; y <= height; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos target = getBlockPos().offset(x, y, z);
                if (level.getBlockState(target).is(com.reinhardt.hbm.registry.HbmBlocks.CARGO_ELEVATOR.get())
                        && (target.equals(origin)
                        || level.getBlockEntity(target) instanceof CargoElevatorBlockEntity segment
                        && segment.corePos.equals(origin))) {
                    level.removeBlock(target, false);
                }
            }
        } finally {
            CargoElevatorBlock.setBuilding(false);
        }
    }

    public static VoxelShape shapeFor(BlockGetter level, BlockPos queried) {
        if (!(level.getBlockEntity(queried) instanceof CargoElevatorBlockEntity segment)) {
            return Shapes.empty();
        }
        Optional<CargoElevatorBlockEntity> optional = segment.core();
        if (optional.isEmpty()) return Shapes.empty();
        CargoElevatorBlockEntity core = optional.get();
        BlockPos origin = core.getBlockPos();
        int h = core.height + 1;
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY(), origin.getZ() - 1,
                origin.getX() - 0.75D, origin.getY() + h, origin.getZ() - 0.75D, queried));
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY(), origin.getZ() + 1.75D,
                origin.getX() - 0.75D, origin.getY() + h, origin.getZ() + 2, queried));
        shape = Shapes.or(shape, box(origin.getX() + 1.75D, origin.getY(), origin.getZ() - 1,
                origin.getX() + 2, origin.getY() + h, origin.getZ() - 0.75D, queried));
        shape = Shapes.or(shape, box(origin.getX() + 1.75D, origin.getY(), origin.getZ() + 1.75D,
                origin.getX() + 2, origin.getY() + h, origin.getZ() + 2, queried));
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY() + 0.75D + core.extension, origin.getZ() - 1,
                origin.getX() + 2, origin.getY() + 1 + core.extension, origin.getZ() + 2, queried));
        return shape;
    }

    private static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, BlockPos relativeTo) {
        return Shapes.box(minX - relativeTo.getX(), minY - relativeTo.getY(), minZ - relativeTo.getZ(),
                maxX - relativeTo.getX(), maxY - relativeTo.getY(), maxZ - relativeTo.getZ());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", corePos.getX()); tag.putInt("CoreY", corePos.getY()); tag.putInt("CoreZ", corePos.getZ());
        tag.putInt("Height", height); tag.putDouble("Extension", extension); tag.putBoolean("Extending", extending);
        tag.putBoolean("RenderPlatform", renderPlatform);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        height = Math.max(0, tag.getInt("Height")); extension = tag.getDouble("Extension"); previousExtension = extension;
        extending = tag.getBoolean("Extending"); renderPlatform = tag.getBoolean("RenderPlatform");
    }
}
