package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class WandTandemBlockEntity extends WandJigsawBlockEntity {
    public boolean armed;
    public String structure = "";

    public WandTandemBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WAND_TANDEM.get(), pos, state);
        this.pool = "default";
        this.target = "default";
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandTandemBlockEntity tandem) {
        if (!level.isClientSide && tandem.armed && level instanceof ServerLevel serverLevel) {
            tandem.tryGenerate(serverLevel);
        }
    }

    private void tryGenerate(ServerLevel level) {
        if (this.structure.isBlank() || this.pool.isBlank() || this.target.isBlank()) {
            return;
        }
        TandemPiece piece = selectPiece(this.structure, this.pool, level.random);
        if (piece == null) {
            return;
        }
        HbmLegacyNbtTemplate template = HbmLegacyNbtTemplate.get(piece.templateName());
        Direction direction = facing();
        List<HbmLegacyNbtTemplate.JigsawConnection> connections = template.getConnectionPool(direction, this.target);
        if (connections.isEmpty()) {
            return;
        }
        HbmLegacyNbtTemplate.JigsawConnection targetConnection = connections.get(level.random.nextInt(connections.size()));
        int rotation = directionToRotation(direction.getOpposite(), targetConnection.direction());
        BlockPos front = this.worldPosition.relative(direction);
        BlockPos origin = front.offset(
                -HbmLegacyNbtTemplate.rotateX(targetConnection.x(), targetConnection.z(), rotation, template.sizeX(), template.sizeZ()),
                -targetConnection.y(),
                -HbmLegacyNbtTemplate.rotateZ(targetConnection.x(), targetConnection.z(), rotation, template.sizeX(), template.sizeZ())
        );
        HbmStructureIO.placeResourceStructure(level, piece.templateName(), origin, rotation, false);
        Block block = replacementBlock();
        BlockState replacement = HbmLegacyNbtTemplate.stateFromLegacyId(BuiltInRegistries.BLOCK.getKey(block).toString(), this.replaceMeta);
        level.setBlock(this.worldPosition, replacement, 3);
    }

    private Direction facing() {
        if (getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.SOUTH;
    }

    private static int directionToRotation(Direction from, Direction to) {
        Direction current = from;
        for (int i = 0; i < 4; i++) {
            if (current == to) {
                return i & 3;
            }
            current = current.getClockWise();
        }
        return 0;
    }

    private static TandemPiece selectPiece(String structureName, String poolName, RandomSource random) {
        try {
            CompoundTag root = HbmStructureIO.loadResourceStructure(structureName);
            if (!root.contains("pools", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                return new TandemPiece(structureName, 1);
            }
            CompoundTag pools = root.getCompound("pools");
            if (!pools.contains(poolName, net.minecraft.nbt.Tag.TAG_LIST)) {
                return null;
            }
            var list = pools.getList(poolName, net.minecraft.nbt.Tag.TAG_COMPOUND);
            List<TandemPiece> pieces = new ArrayList<>();
            int total = 0;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                String name = tag.getString("name");
                if (name.isBlank()) {
                    name = tag.getString("structure");
                }
                int weight = Math.max(1, tag.getInt("weight"));
                total += weight;
                pieces.add(new TandemPiece(name, weight));
            }
            int roll = random.nextInt(Math.max(1, total));
            for (TandemPiece piece : pieces) {
                roll -= piece.weight();
                if (roll < 0) {
                    return piece;
                }
            }
        } catch (Exception ignored) {
            return new TandemPiece(structureName, 1);
        }
        return new TandemPiece(structureName, 1);
    }

    @Override
    public CompoundTag configTag() {
        CompoundTag tag = super.configTag();
        tag.remove("selection");
        tag.remove("placement");
        tag.remove("name");
        tag.putBoolean("isArmed", this.armed);
        if (!this.structure.isBlank()) {
            tag.putString("structure", this.structure);
        }
        return tag;
    }

    @Override
    public void applyConfig(CompoundTag tag) {
        super.applyConfig(tag);
        this.armed = tag.getBoolean("isArmed");
        this.structure = tag.getString("structure");
    }

    public CompoundTag paperConfigTag() {
        CompoundTag tag = super.configTag();
        tag.remove("selection");
        tag.remove("placement");
        tag.remove("name");
        return tag;
    }

    public void applyPaperConfig(CompoundTag tag) {
        boolean oldArmed = this.armed;
        String oldStructure = this.structure;
        super.applyConfig(tag);
        this.armed = oldArmed;
        this.structure = oldStructure;
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.armed) {
            tag.putBoolean("isArmed", true);
            tag.putString("structure", this.structure);
        }
    }

    private record TandemPiece(String templateName, int weight) {
    }
}
