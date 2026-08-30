package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RefuelerBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Legacy fuel dispenser: exactly one 100 mB configured tank and player-worn container filling. */
public final class RefuelerBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100;
    private final HbmFluidTank tank = new HbmFluidTank(HbmFluids.byName("kerosene").orElse(HbmFluids.none()), CAPACITY);
    private boolean operating;
    private int operatingTime;
    private float fillLevel;
    private float previousFillLevel;

    public RefuelerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.REFUELER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RefuelerBlockEntity refueler) {
        if (level.isClientSide) {
            refueler.tickClient(level, state);
        } else {
            refueler.tickServer(level, state);
        }
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public void setConfiguredFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || tank.type() == fluid) {
            return;
        }
        tank.setType(fluid);
        sync();
    }

    public float fillLevel(float partialTick) {
        return previousFillLevel + (fillLevel - previousFillLevel) * partialTick;
    }

    private void tickServer(Level level, BlockState state) {
        Direction back = state.getValue(RefuelerBlock.FACING).getOpposite();
        if (tank.amount() < CAPACITY && !tank.type().isNone()) {
            int missing = CAPACITY - tank.amount();
            var drained = HbmFluidNetworks.drainFrom(level, worldPosition.relative(back), back,
                    tank.type(), missing, worldPosition, true);
            if (!drained.isEmpty()) {
                tank.fill(tank.type(), drained.getAmount(), false);
            }
        }

        boolean wasOperating = operating;
        operating = false;
        AABB area = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1.0D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 1.0D).inflate(0.5D, 0.0D, 0.5D);
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            for (ItemStack stack : fillTargets(player)) {
                if (stack.isEmpty() || tank.amount() <= 0 || tank.type().isNone()) {
                    continue;
                }
                FluidUtil.getFluidHandler(stack).ifPresent(handler -> {
                    int accepted = handler.fill(HbmFluids.toNeoStack(tank.type(), tank.amount()), IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        tank.drain(tank.type(), accepted, false);
                        operating = true;
                    }
                });
            }
        }

        if (operating) {
            if (operatingTime % 20 == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
            operatingTime++;
        } else {
            operatingTime = 0;
        }
        if (wasOperating != operating || operating || level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void tickClient(Level level, BlockState state) {
        previousFillLevel = fillLevel;
        float target = CAPACITY == 0 ? 0.0F : tank.amount() / (float) CAPACITY;
        float rate = target > fillLevel || !operating ? 0.1F : 0.01F;
        fillLevel += (target - fillLevel) * rate;
        if (operating && level.random.nextBoolean()) {
            Direction front = state.getValue(RefuelerBlock.FACING);
            Direction sideways = front.getClockWise();
            float red = ((tank.type().color() >> 16) & 0xFF) / 255.0F;
            float green = ((tank.type().color() >> 8) & 0xFF) / 255.0F;
            float blue = (tank.type().color() & 0xFF) / 255.0F;
            level.addParticle(new DustParticleOptions(new Vector3f(red, green, blue), 0.65F),
                    worldPosition.getX() + 0.5D + front.getStepX() * 0.5D + sideways.getStepX() * 0.25D,
                    worldPosition.getY() + 0.375D,
                    worldPosition.getZ() + 0.5D + front.getStepZ() * 0.5D + sideways.getStepZ() * 0.25D,
                    -front.getStepX() + level.random.nextGaussian() * 0.1D, 0.0D,
                    -front.getStepZ() + level.random.nextGaussian() * 0.1D);
        }
    }

    private static List<ItemStack> fillTargets(Player player) {
        return List.of(player.getMainHandItem(), player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        Direction back = getBlockState().getValue(RefuelerBlock.FACING).getOpposite();
        return side == null || side == back ? tank : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.save());
        tag.putBoolean("operating", operating);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tank")) {
            tank.load(tag.getCompound("tank"));
        }
        operating = tag.getBoolean("operating");
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

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
