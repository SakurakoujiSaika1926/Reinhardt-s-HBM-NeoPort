package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.WandStructureBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.blockentity.AirCompressorBlockEntity;
import com.reinhardt.hbm.blockentity.CapacitorBlockEntity;
import com.reinhardt.hbm.blockentity.CatalyticCrackerBlockEntity;
import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import com.reinhardt.hbm.blockentity.CoolingTowerBlockEntity;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.DeuteriumExtractorBlockEntity;
import com.reinhardt.hbm.blockentity.DrainBlockEntity;
import com.reinhardt.hbm.blockentity.FractionTowerBlockEntity;
import com.reinhardt.hbm.blockentity.HeatBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.blockentity.GroundwaterPumpBlockEntity;
import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.IndustrialTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.PowerGaugeBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.SolarBoilerBlockEntity;
import com.reinhardt.hbm.blockentity.SteamCondenserBlockEntity;
import com.reinhardt.hbm.blockentity.SteamEngineBlockEntity;
import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.blockentity.WandJigsawBlockEntity;
import com.reinhardt.hbm.blockentity.WandLogicBlockEntity;
import com.reinhardt.hbm.blockentity.WandLootBlockEntity;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.blockentity.WandTandemBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class HbmLookOverlay {
    private static final int X_OFFSET = 28;
    private static final int Y_OFFSET = 14;
    private static final int TITLE_TO_LINES = 14;
    private static final int LINE_SPACING = 13;

    private HbmLookOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.level == null || minecraft.hitResult == null) {
            return;
        }
        if (minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) minecraft.hitResult;
        Level level = minecraft.level;
        BlockPos corePos = corePos(level, hit.getBlockPos());
        if (corePos == null) {
            return;
        }

        BlockState coreState = level.getBlockState(corePos);
        BlockEntity coreEntity = level.getBlockEntity(corePos);
        if (coreState.isAir() || coreEntity == null) {
            return;
        }

        if (coreEntity instanceof RotaryFurnaceBlockEntity furnace) {
            OverlayData data = rotaryFurnaceOverlay(coreState, furnace, corePos, hit.getBlockPos());
            if (data != null) {
                renderGeneric(event.getGuiGraphics(), minecraft.font, data);
            }
            return;
        }

        if (coreEntity instanceof GasTurbineBlockEntity turbine) {
            OverlayData data = gasTurbineOverlay(coreState, turbine, corePos, hit.getBlockPos());
            if (data != null) {
                renderGeneric(event.getGuiGraphics(), minecraft.font, data);
            }
            return;
        }

        OverlayData data = overlayData(coreState, coreEntity);
        if (data == null) {
            return;
        }
        renderGeneric(event.getGuiGraphics(), minecraft.font, data);
    }

    @Nullable
    private static BlockPos corePos(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            return corePos.equals(BlockPos.ZERO) && !pos.equals(BlockPos.ZERO) ? null : corePos;
        }
        if (entity instanceof HeaterBlockEntity
                || entity instanceof StirlingGeneratorBlockEntity
                || entity instanceof HeatBoilerBlockEntity
                || entity instanceof SolarBoilerBlockEntity
                || entity instanceof SteamEngineBlockEntity
                || entity instanceof GasTurbineBlockEntity
                || entity instanceof IndustrialTurbineBlockEntity
                || entity instanceof LeviathanTurbineBlockEntity
                || entity instanceof SteamCondenserBlockEntity
                || entity instanceof PoweredSteamCondenserBlockEntity
                || entity instanceof CoolingTowerBlockEntity
                || entity instanceof FractionTowerBlockEntity
                || entity instanceof CatalyticCrackerBlockEntity
                || entity instanceof ChimneyBlockEntity
                || entity instanceof DrainBlockEntity
                || entity instanceof DeuteriumExtractorBlockEntity
                || entity instanceof AirCompressorBlockEntity
                || entity instanceof CapacitorBlockEntity
                || entity instanceof GroundwaterPumpBlockEntity
                || entity instanceof PowerGaugeBlockEntity
                || entity instanceof GeothermalHeatExchangerBlockEntity
                || entity instanceof StrandCasterBlockEntity
                || entity instanceof ConveyorPressBlockEntity
                || entity instanceof RotaryFurnaceBlockEntity
                || entity instanceof LegacyMachineBlockEntity
                || entity instanceof RbmkComponentBlockEntity
                || entity instanceof WandStructureBlockEntity
                || entity instanceof WandJigsawBlockEntity
                || entity instanceof WandTandemBlockEntity
                || entity instanceof WandLootBlockEntity
                || entity instanceof WandLogicBlockEntity) {
            return pos;
        }
        return null;
    }

    @Nullable
    private static OverlayData overlayData(BlockState state, BlockEntity entity) {
        if (entity instanceof HeaterBlockEntity heater) {
            return heaterOverlay(state, heater);
        }
        if (entity instanceof StirlingGeneratorBlockEntity stirling) {
            return stirlingOverlay(state, stirling);
        }
        if (entity instanceof HeatBoilerBlockEntity boiler) {
            return boilerOverlay(state, boiler);
        }
        if (entity instanceof SolarBoilerBlockEntity boiler) {
            return solarBoilerOverlay(state, boiler);
        }
        if (entity instanceof SteamEngineBlockEntity engine) {
            return steamEngineOverlay(state, engine);
        }
        if (entity instanceof IndustrialTurbineBlockEntity turbine) {
            return industrialTurbineOverlay(state, turbine);
        }
        if (entity instanceof LeviathanTurbineBlockEntity turbine) {
            return leviathanTurbineOverlay(state, turbine);
        }
        if (entity instanceof SteamCondenserBlockEntity condenser) {
            return condenserOverlay(state, condenser);
        }
        if (entity instanceof PoweredSteamCondenserBlockEntity condenser) {
            return poweredCondenserOverlay(state, condenser);
        }
        if (entity instanceof CoolingTowerBlockEntity tower) {
            return coolingTowerOverlay(state, tower);
        }
        if (entity instanceof FractionTowerBlockEntity tower) {
            return fractionTowerOverlay(state, tower);
        }
        if (entity instanceof CatalyticCrackerBlockEntity cracker) {
            return catalyticCrackerOverlay(state, cracker);
        }
        if (entity instanceof ChimneyBlockEntity chimney) {
            return chimneyOverlay(state, chimney);
        }
        if (entity instanceof DrainBlockEntity drain) {
            return drainOverlay(state, drain);
        }
        if (entity instanceof DeuteriumExtractorBlockEntity extractor) {
            return deuteriumOverlay(state, extractor);
        }
        if (entity instanceof AirCompressorBlockEntity compressor) {
            return airCompressorOverlay(state, compressor);
        }
        if (entity instanceof CapacitorBlockEntity capacitor) {
            return capacitorOverlay(state, capacitor);
        }
        if (entity instanceof GroundwaterPumpBlockEntity pump) {
            return groundwaterPumpOverlay(state, pump);
        }
        if (entity instanceof PowerGaugeBlockEntity gauge) {
            return powerGaugeOverlay(state, gauge);
        }
        if (entity instanceof GeothermalHeatExchangerBlockEntity exchanger) {
            return geothermalHeatExchangerOverlay(state, exchanger);
        }
        if (entity instanceof StrandCasterBlockEntity caster) {
            return strandCasterOverlay(state, caster);
        }
        if (entity instanceof ConveyorPressBlockEntity press) {
            return conveyorPressOverlay(state, press);
        }
        if (entity instanceof LegacyMachineBlockEntity machine) {
            return legacyMachineOverlay(state, machine);
        }
        if (entity instanceof RbmkComponentBlockEntity rbmk && rbmk.kind().isColumn()) {
            return rbmkDoddOverlay(state, rbmk);
        }
        if (entity instanceof WandStructureBlockEntity structure) {
            return wandStructureOverlay(state, structure);
        }
        if (entity instanceof WandTandemBlockEntity tandem) {
            return wandTandemOverlay(state, tandem);
        }
        if (entity instanceof WandJigsawBlockEntity jigsaw) {
            return wandJigsawOverlay(state, jigsaw);
        }
        if (entity instanceof WandLootBlockEntity loot) {
            return wandLootOverlay(state, loot);
        }
        if (entity instanceof WandLogicBlockEntity logic) {
            return wandLogicOverlay(state, logic);
        }
        return null;
    }

    @Nullable
    private static OverlayData heaterOverlay(BlockState state, HeaterBlockEntity heater) {
        List<OverlayLine> lines = new ArrayList<>();
        switch (heater.kind()) {
            case FIREBOX, OVEN -> {
                lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
                lines.add(OverlayLine.white("<- " + String.format("%,d TU/t", heater.burnHeat())));
            }
            case OILBURNER -> {
                lines.add(flueOrSmokeLine());
                lines.add(OverlayLine.white("<- " + String.format("%,d TU/t", heater.setting() * 160)));
            }
            case ELECTRIC -> {
                lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
                lines.add(flueOrSmokeLine());
                lines.add(OverlayLine.white("<- " + heater.heatGeneration() + " TU/t"));
            }
            case HEATEX -> lines.add(OverlayLine.white(String.format("%,d TU", heater.getHeatStored())));
            default -> {
                return null;
            }
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData boilerOverlay(BlockState state, HeatBoilerBlockEntity boiler) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d TU", boiler.heat())));
        lines.add(fluidLine("-> ", boiler.inputTank()));
        lines.add(fluidLine("<- ", boiler.outputTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData solarBoilerOverlay(BlockState state, SolarBoilerBlockEntity boiler) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d TU", boiler.heat())));
        lines.add(flueOrSmokeLine());
        lines.add(fluidLine("-> ", boiler.inputTank()));
        lines.add(fluidLine("<- ", boiler.outputTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData steamEngineOverlay(BlockState state, SteamEngineBlockEntity engine) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", engine.inputTank()));
        lines.add(fluidLine("<- ", engine.outputTank()));
        lines.add(OverlayLine.white(Component.translatable(
                "message.reinhardtshbm.power.steam_engine",
                String.format("%,d", engine.lastOutput())
        ).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData industrialTurbineOverlay(BlockState state, IndustrialTurbineBlockEntity turbine) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", turbine.inputTank()));
        lines.add(fluidLine("<- ", turbine.outputTank()));

        int spinColor = ((int) (0xFF - 0xFF * Math.min(turbine.spin(), 1.0D))) << 16
                | ((int) (0xFF * Math.min(turbine.spin(), 1.0D)) << 8);
        lines.add(new OverlayLine(
                "<- " + String.format("%,d HE (%d%%)", turbine.powerBuffer(), Math.round(turbine.spin() * 100.0D)),
                spinColor
        ));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData leviathanTurbineOverlay(BlockState state, LeviathanTurbineBlockEntity turbine) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", turbine.inputTank()));
        lines.add(fluidLine("<- ", turbine.outputTank()));
        lines.add(OverlayLine.white("<- " + String.format("%,d HE", turbine.powerBuffer())));
        lines.add(OverlayLine.white("<- " + String.format("%,d HE/t", turbine.lastOutput())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData condenserOverlay(BlockState state, SteamCondenserBlockEntity condenser) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", condenser.inputTank()));
        lines.add(fluidLine("<- ", condenser.outputTank()));
        lines.add(OverlayLine.white(String.format("%,d mB/t", condenser.throughput())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData poweredCondenserOverlay(BlockState state, PoweredSteamCondenserBlockEntity condenser) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,dHE / %,dHE", condenser.power(), PoweredSteamCondenserBlockEntity.MAX_POWER)));
        lines.add(fluidLine("-> ", condenser.inputTank()));
        lines.add(fluidLine("<- ", condenser.outputTank()));
        lines.add(OverlayLine.white(String.format("%,d mB/t", condenser.throughput())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData coolingTowerOverlay(BlockState state, CoolingTowerBlockEntity tower) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", tower.inputTank()));
        lines.add(fluidLine("<- ", tower.outputTank()));
        lines.add(OverlayLine.white(String.format("%,d mB/t", tower.throughput())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData fractionTowerOverlay(BlockState state, FractionTowerBlockEntity tower) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", tower.tank(0)));
        lines.add(fluidLine("<- ", tower.tank(1)));
        lines.add(fluidLine("<- ", tower.tank(2)));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData catalyticCrackerOverlay(BlockState state, CatalyticCrackerBlockEntity cracker) {
        List<OverlayLine> lines = new ArrayList<>();
        for (int index = 0; index < CatalyticCrackerBlockEntity.TANK_COUNT; index++) {
            lines.add(fluidLine(index < CatalyticCrackerBlockEntity.OUTPUT_LEFT_TANK ? "-> " : "<- ", cracker.tank(index)));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData chimneyOverlay(BlockState state, ChimneyBlockEntity chimney) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(new OverlayLine(chimney.active() ? "<- \u6392\u70df\u4e2d" : "<- \u5f85\u673a", chimney.active() ? 0x00FF00 : 0xFFFFFF));
        lines.add(flueOrSmokeLine());
        lines.add(OverlayLine.white(chimney.industrial() ? "\u98de\u7070/\u7164\u70df\u6355\u96c6" : "\u98de\u7070\u6355\u96c6"));
        lines.add(OverlayLine.white("\u6c61\u67d3\u6392\u653e\u500d\u7387: " + (chimney.industrial() ? "10%" : "25%")));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData groundwaterPumpOverlay(BlockState state, GroundwaterPumpBlockEntity pump) {
        List<OverlayLine> lines = new ArrayList<>();
        if (pump.kind() == com.reinhardt.hbm.block.GroundwaterPumpBlock.Kind.STEAM) {
            lines.add(fluidLine("-> ", pump.steamTank()));
            lines.add(fluidLine("<- ", pump.spentSteamTank()));
            lines.add(fluidLine("<- ", pump.waterTank()));
        } else {
            lines.add(new OverlayLine(
                    "-> " + String.format("%,d / %,d HE", pump.power(), GroundwaterPumpBlockEntity.ELECTRIC_MAX_POWER),
                    0x00FF00
            ));
            lines.add(fluidLine("<- ", pump.waterTank()));
        }

        int warningColor = System.currentTimeMillis() % 1000L < 500L ? 0xFF0000 : 0xFFFF00;
        if (!pump.hasValidWaterSource()) {
            lines.add(new OverlayLine(Component.translatable("overlay.reinhardtshbm.pump.no_water_source").getString(), warningColor));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData airCompressorOverlay(BlockState state, AirCompressorBlockEntity compressor) {
        List<OverlayLine> lines = new ArrayList<>();
        int powerColor = compressor.power() < AirCompressorBlockEntity.POWER_PER_TICK ? 0xFF0000 : 0x00FF00;
        lines.add(new OverlayLine("Power: " + String.format("%,d", compressor.power()) + "HE", powerColor));
        lines.add(fluidLine("<- ", compressor.airTank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData capacitorOverlay(BlockState state, CapacitorBlockEntity capacitor) {
        long power = capacitor.power();
        long capacity = capacitor.capacity();
        double percent = capacity <= 0L ? 0.0D : (double) power * 100.0D / (double) capacity;
        int color = ((int) (0xFF - 0xFF * percent / 100.0D)) << 16
                | ((int) (0xFF * percent / 100.0D) << 8);
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d / %,d HE", power, capacity)));
        lines.add(new OverlayLine(String.format("%.2f%%", percent), color));
        lines.add(new OverlayLine("-> +" + String.format("%,d HE/t", capacitor.lastReceived()), 0x00FF00));
        lines.add(new OverlayLine("<- -" + String.format("%,d HE/t", capacitor.lastSent()), 0xFF5555));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData powerGaugeOverlay(BlockState state, PowerGaugeBlockEntity gauge) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d HE/t", gauge.deltaTick())));
        lines.add(OverlayLine.white(String.format("%,d HE/s", gauge.deltaLastSecond())));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData geothermalHeatExchangerOverlay(BlockState state, GeothermalHeatExchangerBlockEntity exchanger) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(String.format("%,d TU", exchanger.bufferedHeat())));
        lines.add(new OverlayLine(fluidText("-> ", exchanger.inputTank()), 0x00FF00));
        lines.add(new OverlayLine(fluidText("<- ", exchanger.outputTank()), 0xFF5555));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData strandCasterOverlay(BlockState state, StrandCasterBlockEntity caster) {
        List<OverlayLine> lines = new ArrayList<>();
        FoundryMoldItem.Mold mold = caster.getInstalledMold();
        if (mold == null) {
            lines.add(new OverlayLine(Component.translatable("foundry.noCast").getString(), 0xFF5555));
        } else {
            lines.add(new OverlayLine(mold.title().getString(), 0x5555FF));
        }
        return new OverlayData(state.getBlock().getName(), 0xFF4000, lines);
    }

    /** MachineRotaryFurnace#printHook from 1.7.10: only the four actual ports show data. */
    @Nullable
    private static OverlayData rotaryFurnaceOverlay(BlockState state, RotaryFurnaceBlockEntity furnace,
                                                    BlockPos corePos, BlockPos hitPos) {
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        Direction turn = LegacyMachineGeometry.forgeRotateDown(facing);
        BlockPos steamNear = corePos.relative(facing.getOpposite()).relative(turn.getOpposite());
        BlockPos steamFar = corePos.relative(facing.getOpposite()).relative(turn.getOpposite(), 2);
        BlockPos additiveFront = corePos.relative(facing).relative(turn, 2);
        BlockPos additiveBack = corePos.relative(facing.getOpposite()).relative(turn, 2);
        BlockPos fuel = corePos.relative(facing).relative(turn);
        List<OverlayLine> lines = new ArrayList<>();

        if (hitPos.equals(steamNear) || hitPos.equals(steamFar)) {
            lines.add(new OverlayLine("-> " + Component.translatable(furnace.steamTank().type().translationKey()).getString(), 0x00FF00));
            lines.add(new OverlayLine("<- " + Component.translatable(furnace.spentSteamTank().type().translationKey()).getString(), 0xFF0000));
        } else if (hitPos.equals(additiveFront) || hitPos.equals(additiveBack)) {
            lines.add(new OverlayLine("-> " + Component.translatable(furnace.additiveTank().type().translationKey()).getString(), 0x00FF00));
        } else if (hitPos.equals(fuel)) {
            lines.add(new OverlayLine("-> Fuel", 0xFFFF00));
        } else {
            return null;
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    /** MachineTurbineGas#printHook from 1.7.10: only service dummies expose an overlay. */
    @Nullable
    private static OverlayData gasTurbineOverlay(BlockState state, GasTurbineBlockEntity turbine,
                                                  BlockPos corePos, BlockPos hitPos) {
        Direction facing = state.hasProperty(GasTurbineBlock.FACING)
                ? state.getValue(GasTurbineBlock.FACING)
                : Direction.SOUTH;
        List<OverlayLine> lines = new ArrayList<>();

        for (GasTurbineBlockEntity.Port port : GasTurbineBlockEntity.portsFor(corePos, facing)) {
            if (!port.pos().equals(hitPos)) {
                continue;
            }
            switch (port.kind()) {
                case FUEL_LUBE -> {
                    lines.add(fluidNameLine("-> ", turbine.fuelTank()));
                    lines.add(fluidNameLine("-> ", turbine.lubricantTank()));
                }
                case WATER -> lines.add(fluidNameLine("-> ", turbine.waterTank()));
                case STEAM -> lines.add(fluidNameLine("<- ", turbine.steamTank()));
            }
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }

        if (GasTurbineBlockEntity.powerConnectors(corePos, facing).contains(hitPos)) {
            lines.add(OverlayLine.white("<- Power"));
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }
        return null;
    }

    /** 1.7.10 MachineConveyorPress#printHook. */
    private static OverlayData conveyorPressOverlay(BlockState state, ConveyorPressBlockEntity press) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable(
                "overlay.reinhardtshbm.conveyor_press.power",
                String.format("%,d", press.power()),
                String.format("%,d", ConveyorPressBlockEntity.MAX_POWER)
        ).getString()));

        if (press.stamp().isEmpty()) {
            lines.add(new OverlayLine(Component.translatable(
                    "overlay.reinhardtshbm.conveyor_press.stamp",
                    Component.translatable("overlay.reinhardtshbm.conveyor_press.none")
            ).getString(), 0xFF5555));
        } else {
            lines.add(OverlayLine.white(Component.translatable(
                    "overlay.reinhardtshbm.conveyor_press.stamp",
                    press.stamp().getHoverName()
            ).getString()));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData rbmkDoddOverlay(BlockState state, RbmkComponentBlockEntity rbmk) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(new OverlayLine(state.getBlock().getName().getString(), 0xFFFF00));
        for (Map.Entry<String, String> entry : rbmk.diagnosticData().entrySet()) {
            String label = Component.translatable("tile.rbmk.dodd." + entry.getKey()).getString();
            lines.add(OverlayLine.white(label + ": " + entry.getValue()));
        }
        return new OverlayData(Component.literal("Dump of Ordered Data Diagnostic (DODD)"), 0x00FF00, lines);
    }

    private static OverlayData wandStructureOverlay(BlockState state, WandStructureBlockEntity structure) {
        if (state.hasProperty(WandStructureBlock.LOAD) && state.getValue(WandStructureBlock.LOAD)) {
            return null;
        }
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.name", structure.name).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.size", structure.sizeX, structure.sizeY, structure.sizeZ).getString()));
        lines.add(new OverlayLine(Component.translatable("overlay.reinhardtshbm.wand.blacklist").getString(), 0xA0A0A0));
        for (var key : structure.blacklist()) {
            lines.add(new OverlayLine("- " + key.id() + " : " + key.meta(), 0xFF5555));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandJigsawOverlay(BlockState state, WandJigsawBlockEntity jigsaw) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.target_pool", jigsaw.pool).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.name", jigsaw.name).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.target_name", jigsaw.target).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.turns_into", jigsaw.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.meta", jigsaw.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.priority", jigsaw.selectionPriority, jigsaw.placementPriority).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.joint_type", jointName(jigsaw.rollable)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandTandemOverlay(BlockState state, WandTandemBlockEntity tandem) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.target_pool", tandem.pool).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.target_name", tandem.target).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.turns_into", tandem.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.meta", tandem.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.joint_type", jointName(tandem.rollable)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandLootOverlay(BlockState state, WandLootBlockEntity loot) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.loot_replace", loot.replaceBlock).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.meta", loot.replaceMeta).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.loot_pool", loot.poolName).getString()));
        if (!loot.replaceBlock.endsWith(":deco_loot")) {
            lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.min_items", loot.minItems).getString()));
            lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.max_items", loot.maxItems).getString()));
        }
        if (loot.lockCode != 0) {
            lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.container_locked").getString()));
            lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.lockpick_chance", loot.lockMod).getString()));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData wandLogicOverlay(BlockState state, WandLogicBlockEntity logic) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.action", logic.actionID).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.condition", logic.conditionID).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.interaction", blankFallback(logic.interactionID)).getString()));
        lines.add(OverlayLine.white(Component.translatable("overlay.reinhardtshbm.wand.disguise", blankFallback(logic.disguise)).getString()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    @Nullable
    private static OverlayData legacyMachineOverlay(BlockState state, LegacyMachineBlockEntity machine) {
        if (machine.machineId().equals("machine_teleporter")) {
            List<OverlayLine> lines = new ArrayList<>();
            if (!machine.hasTeleporterTarget()) {
                lines.add(new OverlayLine(Component.translatable("overlay.reinhardtshbm.teleporter.no_destination").getString(), 0xFF5555));
            } else {
                int powerColor = machine.energyStored() >= 1_000_000L ? 0x00FF00 : 0xFF5555;
                lines.add(new OverlayLine(Component.translatable(
                        "overlay.reinhardtshbm.teleporter.power",
                        String.format(Locale.US, "%,d", machine.energyStored()),
                        String.format(Locale.US, "%,d", machine.energyCapacity())
                ).getString(), powerColor));
                lines.add(OverlayLine.white(Component.translatable(
                        "overlay.reinhardtshbm.teleporter.destination",
                        machine.teleporterTargetX(),
                        machine.teleporterTargetY(),
                        machine.teleporterTargetZ(),
                        machine.teleporterLegacyDimensionId()
                ).getString()));
            }
            return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
        }
        if (!machine.machineId().equals("machine_thresher") && !machine.machineId().equals("machine_autosaw")) {
            return null;
        }
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("", machine.inputTank()));
        if (machine.machineId().equals("machine_thresher") && machine.thresherSuspended()) {
            lines.add(new OverlayLine(Component.translatable("block.reinhardtshbm.machine_thresher.suspended").getString(), 0xFF5555));
        }
        if (machine.machineId().equals("machine_autosaw") && machine.autosawSuspended()) {
            lines.add(new OverlayLine(Component.translatable("block.reinhardtshbm.machine_autosaw.suspended").getString(), 0xFF5555));
        }
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static String jointName(boolean rollable) {
        return Component.translatable(rollable ? "gui.reinhardtshbm.wand.rollable" : "gui.reinhardtshbm.wand.aligned").getString();
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? Component.translatable("overlay.reinhardtshbm.wand.none").getString() : value;
    }

    private static OverlayLine fluidLine(String prefix, HbmFluidTank tank) {
        return OverlayLine.white(fluidText(prefix, tank));
    }

    private static OverlayLine fluidNameLine(String prefix, HbmFluidTank tank) {
        return OverlayLine.white(prefix + Component.translatable(tank.type().translationKey()).getString());
    }

    private static String fluidText(String prefix, HbmFluidTank tank) {
        Component name = Component.translatable(tank.type().translationKey());
        return prefix + name.getString() + ": " + tank.amount() + "/" + tank.capacity() + "mB";
    }

    private static OverlayData drainOverlay(BlockState state, DrainBlockEntity drain) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(fluidLine("-> ", drain.tank()));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayData deuteriumOverlay(BlockState state, DeuteriumExtractorBlockEntity extractor) {
        List<OverlayLine> lines = new ArrayList<>();
        int powerColor = extractor.power() < extractor.powerPerOperation() ? 0xFF0000 : 0x00FF00;
        lines.add(new OverlayLine("Power: " + String.format("%,d", extractor.power()) + "HE", powerColor));
        lines.add(new OverlayLine(fluidText("-> ", extractor.inputTank()), 0x00FF00));
        lines.add(new OverlayLine(fluidText("<- ", extractor.outputTank()), 0xFF5555));
        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static OverlayLine flueOrSmokeLine() {
        return OverlayLine.white("-> "
                + Component.translatable("hbmfluid.flue").getString()
                + " / "
                + Component.translatable("hbmfluid.smoke").getString());
    }

    private static OverlayData stirlingOverlay(BlockState state, StirlingGeneratorBlockEntity stirling) {
        List<OverlayLine> lines = new ArrayList<>();
        lines.add(OverlayLine.white(stirling.heat() + "TU/t"));
        lines.add(OverlayLine.white(stirling.currentOutput() + "HE/t"));

        if (!stirling.isCreative()) {
            int maxHeat = stirling.maxHeat();
            double percent = (double) stirling.heat() / (double) Math.max(1, maxHeat);
            int color = ((int) (0xFF - 0xFF * Math.min(percent, 1.0D))) << 16
                    | ((int) (0xFF * Math.min(percent, 1.0D)) << 8);
            if (percent > 1.0D) {
                color = 0xFF0000;
            }

            lines.add(new OverlayLine(((stirling.heat() * 1000 / maxHeat) / 10.0D) + "%", color));
            if (stirling.heat() > maxHeat) {
                lines.add(new OverlayLine(Component.translatable(
                        "message.reinhardtshbm.stirling.overspeed"
                ).getString(), 0xFF0000));
            }
            if (!stirling.hasCog()) {
                lines.add(new OverlayLine(Component.translatable(
                        "message.reinhardtshbm.stirling.gear_missing"
                ).getString(), 0xFF0000));
            }
        }

        return new OverlayData(state.getBlock().getName(), 0xFFFF00, lines);
    }

    private static void renderGeneric(GuiGraphics graphics, Font font, OverlayData data) {
        int x = graphics.guiWidth() / 2 + X_OFFSET;
        int y = graphics.guiHeight() / 2 + Y_OFFSET;

        graphics.drawString(font, data.title(), x + 1, y + 1, 0x404000, false);
        graphics.drawString(font, data.title(), x, y, data.titleColor(), false);

        int lineY = y + TITLE_TO_LINES;
        for (OverlayLine line : data.lines()) {
            graphics.drawString(font, line.text(), x, lineY, line.color(), true);
            lineY += LINE_SPACING;
        }
    }

    private record OverlayData(Component title, int titleColor, List<OverlayLine> lines) {
    }

    private record OverlayLine(String text, int color) {
        private static OverlayLine white(String text) {
            return new OverlayLine(text, 0xFFFFFF);
        }
    }
}

