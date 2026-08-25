package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.BigAssTankBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import com.reinhardt.hbm.blockentity.CentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.blockentity.DrainBlockEntity;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.RadarScreenBlockEntity;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.block.TurretJeremyBlock;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.blockentity.TurretJeremyBlockEntity;
import com.reinhardt.hbm.blockentity.WatzBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.item.WiringRedCopperItem;
import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MachineDummyBlock extends Block implements EntityBlock {
    private static final VoxelShape FULL_BLOCK = Shapes.block();
    private static final VoxelShape HALF_BLOCK = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final VoxelShape RBMK_LID_COLLISION = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
    private static final VoxelShape CENTRIFUGE_TOWER = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
    private static final VoxelShape GAS_CENTRIFUGE_TOWER = Shapes.box(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);
    private static final ThreadLocal<Boolean> SUPPRESS_CORE_DESTROY = ThreadLocal.withInitial(() -> false);

    public MachineDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof LegacyMachineBlockEntity machine
                && machine.machineId().equals("machine_radar_large")) {
            BlockPos delta = pos.subtract(dummy.getCorePos());
            // TileEntityMachineRadarLarge#getConPos: exactly +/-2 on the two horizontal core axes.
            if ((Math.abs(delta.getX()) == 2 && delta.getY() == 0 && delta.getZ() == 0)
                    || (Math.abs(delta.getZ()) == 2 && delta.getY() == 0 && delta.getX() == 0)) {
            return machine.radarRedPower();
            }
        }
        return 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDummyBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockState coreState = level.getBlockState(dummy.getCorePos());
            return coreState.getBlock().getCloneItemStack(level, dummy.getCorePos(), coreState);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof LegacyTurretBlockEntity turret
                && turret.type() == LegacyTurretType.HOWARD_DAMAGED) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            BlockState coreState = level.getBlockState(corePos);
            BlockEntity coreEntity = level.getBlockEntity(corePos);
            if (coreEntity instanceof IndustrialTurbineBlockEntity) {
                InteractionResult result = IndustrialTurbineBlock.useOnPart(level, pos, corePos, player);
                if (result.consumesAction()) {
                    return result;
                }
            }
            if (coreEntity instanceof LeviathanTurbineBlockEntity) {
                InteractionResult result = LeviathanTurbineBlock.useOnPart(level, pos, corePos, player);
                if (result.consumesAction()) {
                    return result;
                }
            }
            if (coreEntity instanceof RbmkComponentBlockEntity rbmk) {
                if (coreEntity instanceof MenuProvider menuProvider
                        && rbmk.kind().hasMenu()
                        && !player.isShiftKeyDown()
                        && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(corePos));
                    return InteractionResult.CONSUME;
                }
                if (rbmk.handleEmptyHand(player)) {
                    return InteractionResult.CONSUME;
                }
                rbmk.printInfo(player);
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof RadarScreenBlockEntity screen
                    && player instanceof ServerPlayer serverPlayer) {
                screen.openLinkedRadar(serverPlayer);
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof LegacyMachineBlockEntity machine
                    && machine.handleEmptyHandInteraction(player)) {
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof LegacyMachineBlockEntity machine
                    && machine.machineId().equals("machine_orbus") && player.isCrouching()) {
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof FractionTowerBlockEntity tower) {
                FractionTowerBlock.printTowerInfo(player, corePos, tower);
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof FusionMachineBlockEntity fusion) {
                if (coreEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(corePos));
                    return InteractionResult.CONSUME;
                }
                fusion.printInfo(player);
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof CatalyticCrackerBlockEntity cracker) {
                CatalyticCrackerBlock.printInfo(player, cracker);
                return InteractionResult.CONSUME;
            }
            if (coreEntity instanceof HeaterBlockEntity heater
                    && coreEntity instanceof MenuProvider menuProvider
                    && heater.hasMenu()
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(menuProvider, buffer -> {
                    buffer.writeBlockPos(corePos);
                    buffer.writeVarInt(heater.kind().ordinal());
                });
            } else if (coreEntity instanceof ElectrolyzerBlockEntity electrolyzer && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(electrolyzer, buffer -> {
                    buffer.writeBlockPos(corePos);
                    buffer.writeVarInt(electrolyzer.selectedGui());
                });
            } else if (coreEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(corePos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof WiringRedCopperItem) {
            return WiringRedCopperItem.useItemOnBlock(stack, level, player, pos);
        }
        if (stack.getItem() instanceof com.reinhardt.hbm.item.BlowtorchItem blowtorch
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof FluidTankBlockEntity tank) {
            if (tank.isDamaged()) {
                if (!level.isClientSide && blowtorch.canTorch(stack) && tank.repair(player)) {
                    blowtorch.consumeTorchFuel(stack);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof ConveyorPressBlockEntity press
                && press.canHandleItem(stack)) {
            if (!level.isClientSide && press.handleItemInteraction(player, stack)
                    && press.lastInteractionInstalledStamp()) {
                level.playSound(null, dummy.getCorePos(), HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof StirlingGeneratorBlockEntity stirling
                && stirling.tryInsertCog(player, hand, stack, pos)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof RbmkComponentBlockEntity rbmk
                && rbmk.handleItemUse(player, hand, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof LegacyMachineBlockEntity machine
                && machine.handleItemInteraction(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof StrandCasterBlockEntity caster) {
            if (stack.getItem() instanceof FoundryMoldItem && caster.installMold(stack, player)) {
                if (!level.isClientSide) {
                    level.playSound(null, dummy.getCorePos(), HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stack.is(ItemTags.SHOVELS) && caster.scrape(player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (ScrewdriverItem.isScrewdriver(stack) && caster.removeMold(player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof GasCentrifugeBlockEntity gasCentrifuge) {
            if (!level.isClientSide && gasCentrifuge.applyFluidIdentifier(stack)) {
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof GasTurbineBlockEntity turbine) {
            if (!level.isClientSide) {
                turbine.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof GeothermalHeatExchangerBlockEntity exchanger) {
            if (!level.isClientSide && exchanger.applyFluidIdentifier(FluidIdentifierItem.primary(stack))) {
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.25F, 1.2F);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "chat.reinhardtshbm.changed_fluid_type",
                        net.minecraft.network.chat.Component.translatable(FluidIdentifierItem.primary(stack).translationKey())), false);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CombustionEngineBlockEntity engine) {
            if (!level.isClientSide) {
                engine.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof DrainBlockEntity drain) {
            if (!level.isClientSide) {
                drain.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof FluidTankBlockEntity tank) {
            if (!level.isClientSide) {
                if (!tank.isDamaged()) {
                    tank.setType(FluidIdentifierItem.primary(stack));
                }
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof BigAssTankBlockEntity tank) {
            if (!level.isClientSide) {
                tank.setType(FluidIdentifierItem.primary(stack));
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof FractionTowerBlockEntity tower) {
            if (!level.isClientSide) {
                FractionTowerBlock.applyIdentifier(level, dummy.getCorePos(), player, stack, tower);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CatalyticCrackerBlockEntity cracker) {
            if (!level.isClientSide) {
                CatalyticCrackerBlock.applyIdentifier(player, stack, cracker);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof LeviathanTurbineBlockEntity turbine) {
            if (!level.isClientSide) {
                turbine.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof HeatBoilerBlockEntity boiler) {
            if (!level.isClientSide) {
                boiler.setConfiguredInput(FluidIdentifierItem.primary(stack));
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CrystallizerBlockEntity crystallizer) {
            if (!level.isClientSide) {
                crystallizer.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof ElectrolyzerBlockEntity electrolyzer) {
            if (!level.isClientSide) {
                electrolyzer.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, pos);
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof LegacyMachineBlockEntity machine
                && machine.machineId().equals("machine_turbofan")) {
            if (!level.isClientSide) {
                machine.pasteFluidSetting(FluidIdentifierItem.primary(stack), level, player, dummy.getCorePos());
                level.playSound(null, dummy.getCorePos(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.25F, 1.2F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isScrewdriver(stack)) {
            return ScrewdriverItem.useItemOnHeater(stack, level, player, hand, pos);
        }
        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            BlockState coreState = level.getBlockState(corePos);
            if (coreState.getBlock() instanceof BigAssTankBlock tankBlock) {
                return tankBlock.useItemOn(stack, coreState, level, corePos, player, hand,
                        hitResult.withPosition(corePos));
            }
            if (coreState.getBlock() instanceof LargeFluidTankBlock tankBlock) {
                return tankBlock.useItemOn(stack, coreState, level, corePos, player, hand,
                        hitResult.withPosition(corePos));
            }
        }
        if (stack.is(ItemTags.SHOVELS)
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof CrucibleBlockEntity crucible) {
            if (!level.isClientSide) {
                crucible.clearMoltenToScraps(player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof WiringRedCopperItem) {
            return WiringRedCopperItem.useItemOnBlock(stack, level, player, pos);
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            BlockState coreState = level.getBlockState(corePos);
            if (coreState.getBlock() instanceof PowerPylonBlock pylonBlock) {
                return pylonBlock.useItemOn(stack, coreState, level, corePos, player, hand, hitResult.withPosition(corePos));
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!SUPPRESS_CORE_DESTROY.get()
                && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            BlockState coreState = level.getBlockState(corePos);
            if (level.getBlockEntity(corePos) instanceof RbmkComponentBlockEntity rbmk
                    && pos.getX() == corePos.getX()
                    && pos.getZ() == corePos.getZ()
                    && pos.getY() - corePos.getY() == RbmkComponentBlock.columnHeight(level)
                    && rbmk.hasLid()) {
                ItemStack lid = rbmk.removeLidStack();
                if (!lid.isEmpty() && !level.isClientSide) {
                    Block.popResource(level, pos, lid);
                }
                super.onRemove(state, level, pos, newState, movedByPiston);
                return;
            }
            if (coreState.getBlock() instanceof LargeMachineBlock
                    || coreState.getBlock() instanceof LargeFluidTankBlock
                    || coreState.getBlock() instanceof GasTurbineBlock
                    || coreState.getBlock() instanceof ElectrolyzerBlock
                    || coreState.getBlock() instanceof StirlingGeneratorBlock
                    || coreState.getBlock() instanceof WoodBurnerBlock
                    || coreState.getBlock() instanceof PowerPylonBlock
                    || coreState.getBlock() instanceof TurretChekhovBlock
                    || coreState.getBlock() instanceof TurretJeremyBlock
                    || coreState.getBlock() instanceof LegacyTurretBlock
                    || coreState.getBlock() instanceof RbmkComponentBlock
                    || level.getBlockEntity(corePos) instanceof FusionMachineBlockEntity
                    || level.getBlockEntity(corePos) instanceof WatzBlockEntity) {
                level.destroyBlock(corePos, true);
            }
        }
        if (!state.is(newState.getBlock())) {
            com.reinhardt.hbm.power.PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof FluidTankBlockEntity tank) {
            tank.handleExplosion(explosion);
            runWithoutCoreDestroy(() -> level.removeBlock(pos, false));
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    public static void runWithoutCoreDestroy(Runnable action) {
        boolean previous = SUPPRESS_CORE_DESTROY.get();
        SUPPRESS_CORE_DESTROY.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_CORE_DESTROY.set(previous);
        }
    }

    private static VoxelShape shapeFor(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            int yOffset = pos.getY() - corePos.getY();
            BlockEntity core = level.getBlockEntity(corePos);
            if (core instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_sawmill")) {
                BlockState coreState = level.getBlockState(corePos);
                return SawmillBlock.shapeForPart(coreState.getValue(LargeMachineBlock.FACING), pos.subtract(corePos));
            }
            if (core instanceof GasFlareBlockEntity) {
                return GasFlareBlock.shapeForPart(pos.subtract(corePos));
            }
            if (core instanceof com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher
                    && launcher.kind() == com.reinhardt.hbm.blockentity.LauncherBlockEntity.Kind.PAD_SMALL) {
                return LaunchPadBlock.shapeForPart(LaunchPadBlock.Kind.SILO, pos.subtract(corePos));
            }
            if (core instanceof com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher
                    && launcher.kind() == com.reinhardt.hbm.blockentity.LauncherBlockEntity.Kind.PAD_RUSTED) {
                return LaunchPadBlock.shapeForPart(LaunchPadBlock.Kind.RUSTED, pos.subtract(corePos));
            }
            if (core instanceof com.reinhardt.hbm.blockentity.LauncherBlockEntity launcher
                    && launcher.kind() == com.reinhardt.hbm.blockentity.LauncherBlockEntity.Kind.PAD_LARGE) {
                return LaunchPadBlock.shapeForPart(LaunchPadBlock.Kind.LARGE, pos.subtract(corePos));
            }
            if (pos.getX() == corePos.getX() && pos.getZ() == corePos.getZ() && yOffset >= 1) {
                if (core instanceof RbmkComponentBlockEntity rbmk && yOffset < RbmkComponentBlock.columnHeight(level)) {
                    return RbmkComponentBlock.columnSegmentShape(
                            rbmk.kind(),
                            yOffset,
                            RbmkComponentBlock.columnHeight(level) - 1,
                            rbmk.hasLid()
                    );
                }
                if (core instanceof RbmkComponentBlockEntity rbmk
                        && yOffset == RbmkComponentBlock.columnHeight(level)) {
                    return rbmk.hasLid() ? RBMK_LID_COLLISION : Shapes.empty();
                }
                if (yOffset <= 3 && core instanceof CentrifugeBlockEntity) {
                    return CENTRIFUGE_TOWER;
                }
                if (yOffset <= 3 && core instanceof GasCentrifugeBlockEntity) {
                    return GAS_CENTRIFUGE_TOWER;
                }
            }
            if (corePos.getY() < pos.getY()
                    && level.getBlockEntity(corePos) instanceof RbmkComponentBlockEntity rbmk
                    && rbmk.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                return HALF_BLOCK;
            }
            if (core instanceof TurretJeremyBlockEntity || core instanceof TurretChekhovBlockEntity) {
                return HALF_BLOCK;
            }
            if (core instanceof LegacyTurretBlockEntity turret && turret.type().layout() == LegacyTurretType.Layout.NT) {
                return HALF_BLOCK;
            }
        }
        return FULL_BLOCK;
    }

}
