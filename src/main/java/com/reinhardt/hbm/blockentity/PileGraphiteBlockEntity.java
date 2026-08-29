package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.neutron.PileNeutronNetwork;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Server state for the old Chicago Pile fuel, source, breeder and detector blocks. */
public final class PileGraphiteBlockEntity extends BlockEntity {
    public static final int MAX_HEAT = 1_000;

    private int heat;
    private int neutrons;
    private int lastNeutrons;
    private int progress;
    private int maxNeutrons = 10;

    public PileGraphiteBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PILE_GRAPHITE.get(), pos, state);
        if (state.getBlock() instanceof PileGraphiteBlock block
                && block.kind() == PileGraphiteBlock.Kind.FUEL
                && state.getValue(PileGraphiteBlock.ACTIVE)) {
            this.progress = fuelMaxProgress() - 1_000;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileGraphiteBlockEntity pile) {
        if (level.isClientSide || !(state.getBlock() instanceof PileGraphiteBlock block)) {
            return;
        }
        switch (block.kind()) {
            case FUEL -> pile.tickFuel(level, pos, state);
            case LITHIUM -> pile.tickLithium(level, pos, state);
            case SOURCE -> pile.emit(level, pos, 1, 12);
            case PLUTONIUM -> pile.emit(level, pos, 2, 12);
            case DETECTOR -> pile.tickDetector(level, pos, state);
            default -> {
            }
        }
    }

    public void receiveNeutrons(int flux) {
        this.neutrons += Math.max(0, flux);
        setChanged();
    }

    /** 1.7.10 BlockGraphiteFuel#applyFan: cool by exactly 2.5 percent. */
    public void coolByFan() {
        this.heat = (int) (this.heat * 0.975D);
        setChanged();
    }

    /** State carried with a rod when legacy-style axial insertion pushes it onward. */
    public PileState snapshot() {
        return new PileState(this.heat, this.neutrons, this.lastNeutrons, this.progress, this.maxNeutrons);
    }

    public void restore(PileState state) {
        this.heat = state.heat();
        this.neutrons = state.neutrons();
        this.lastNeutrons = state.lastNeutrons();
        this.progress = state.progress();
        this.maxNeutrons = state.maxNeutrons();
        setChangedAndSync();
    }

    public int comparatorOutput() {
        return Math.clamp((this.progress * 15) / Math.max(1, fuelMaxProgress() - 1_000), 0, 15);
    }

    public void adjustMaxNeutrons(int adjustment) {
        this.maxNeutrons = Math.max(1, this.maxNeutrons + adjustment);
        setChangedAndSync();
    }

    public void sendInspection(ServerPlayer player) {
        PileGraphiteBlock.Kind kind = kind();
        if (kind == PileGraphiteBlock.Kind.FUEL) {
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.fuel", this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ()).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.heat", this.heat, MAX_HEAT).withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.depletion", this.progress, fuelMaxProgress()).withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.flux", this.lastNeutrons).withStyle(ChatFormatting.YELLOW));
            if (this.getBlockState().getValue(PileGraphiteBlock.ACTIVE)) {
                player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.pu239_rich").withStyle(ChatFormatting.DARK_GREEN));
            }
            return;
        }
        if (kind == PileGraphiteBlock.Kind.LITHIUM) {
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.fuel", this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ()).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.depletion", this.progress, lithiumMaxProgress()).withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.translatable("info.reinhardtshbm.pile.flux", this.lastNeutrons).withStyle(ChatFormatting.YELLOW));
        }
    }

    private void tickFuel(Level level, BlockPos pos, BlockState state) {
        int previousComparator = comparatorOutput();
        this.heat -= (int) (this.heat * (state.getValue(PileGraphiteBlock.ALUMINIUM) ? 0.065D : 0.05D));

        int reaction = (int) (this.neutrons * (1.0D - (this.heat / (double) MAX_HEAT) * 0.5D));
        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;
        this.progress += reaction;

        if (reaction > 0) {
            this.heat += reaction;
            emit(level, pos, Math.max((int) (reaction * 0.25D), 1), 12);
        }

        if (this.heat >= MAX_HEAT) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F, Level.ExplosionInteraction.BLOCK);
            level.setBlock(pos, HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        if (level.random.nextFloat() * 2.0F <= this.heat / (float) MAX_HEAT && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.25D + level.random.nextDouble() * 0.5D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 0.25D + level.random.nextDouble() * 0.5D,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }

        int maxProgress = fuelMaxProgress();
        if (state.getValue(PileGraphiteBlock.ACTIVE)) {
            if (this.progress < maxProgress - 1_000) {
                this.progress = maxProgress - 1_000;
            }
        } else if (this.progress >= maxProgress - 1_000) {
            level.setBlock(pos, state.setValue(PileGraphiteBlock.ACTIVE, true), Block.UPDATE_ALL);
        }

        if (this.progress >= maxProgress) {
            level.setBlock(pos, PileGraphiteBlock.stateFor(
                    PileGraphiteBlock.Kind.PLUTONIUM,
                    state.getValue(PileGraphiteBlock.AXIS),
                    state.getValue(PileGraphiteBlock.ALUMINIUM),
                    false
            ), Block.UPDATE_ALL);
            return;
        }

        if (previousComparator != comparatorOutput()) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        setChangedAndSync();
    }

    private void tickLithium(Level level, BlockPos pos, BlockState state) {
        this.lastNeutrons = this.neutrons;
        this.progress += this.neutrons;
        this.neutrons = 0;

        if (this.lastNeutrons > 0) {
            emit(level, pos, 1, 2);
        }

        if (this.progress >= lithiumMaxProgress()) {
            level.setBlock(pos, PileGraphiteBlock.stateFor(
                    PileGraphiteBlock.Kind.TRITIUM,
                    state.getValue(PileGraphiteBlock.AXIS),
                    state.getValue(PileGraphiteBlock.ALUMINIUM),
                    false
            ), Block.UPDATE_ALL);
            return;
        }
        setChangedAndSync();
    }

    private void tickDetector(Level level, BlockPos pos, BlockState state) {
        if (this.neutrons >= this.maxNeutrons && state.getValue(PileGraphiteBlock.ACTIVE)) {
            PileGraphiteBlock.toggleDetectorRods(level, pos, state);
        }
        if (this.neutrons < this.maxNeutrons && this.lastNeutrons < this.maxNeutrons && !state.getValue(PileGraphiteBlock.ACTIVE)) {
            PileGraphiteBlock.toggleDetectorRods(level, pos, state);
        }
        this.lastNeutrons = this.neutrons;
        this.neutrons = 0;
        setChangedAndSync();
    }

    private void emit(Level level, BlockPos pos, int flux, int count) {
        for (int index = 0; index < count; index++) {
            PileNeutronNetwork.emit(level, pos, flux);
        }
    }

    public static int fuelMaxProgress() {
        return HbmConfig.ENABLE_528_MODE.get() ? 75_000 : 50_000;
    }

    public static int lithiumMaxProgress() {
        return HbmConfig.ENABLE_528_MODE.get() ? 50_000 : 30_000;
    }

    public record PileState(int heat, int neutrons, int lastNeutrons, int progress, int maxNeutrons) {
    }

    private PileGraphiteBlock.Kind kind() {
        return this.getBlockState().getBlock() instanceof PileGraphiteBlock block
                ? block.kind()
                : PileGraphiteBlock.Kind.DRILLED;
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Heat", this.heat);
        tag.putInt("Neutrons", this.neutrons);
        tag.putInt("LastNeutrons", this.lastNeutrons);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxNeutrons", this.maxNeutrons);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.heat = tag.getInt("Heat");
        this.neutrons = tag.getInt("Neutrons");
        this.lastNeutrons = tag.getInt("LastNeutrons");
        this.progress = tag.getInt("Progress");
        this.maxNeutrons = Math.max(1, tag.getInt("MaxNeutrons"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
