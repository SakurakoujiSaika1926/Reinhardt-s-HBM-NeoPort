package com.reinhardt.hbm.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.network.RbmkCraneControlPayload;
import com.reinhardt.hbm.network.SettingsToolKeysPayload;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class SettingsToolClientEvents {
    private static boolean lastCtrl;
    private static boolean lastAlt;
    private static BlockPos lastCranePos;
    private static boolean lastCraneUp;
    private static boolean lastCraneDown;
    private static boolean lastCraneLeft;
    private static boolean lastCraneRight;
    private static boolean lastCraneLoad;
    private static int craneResendTimer;
    private static int resendTimer;

    private SettingsToolClientEvents() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        boolean holdingTool = isKeyAwareTool(player.getMainHandItem()) || isKeyAwareTool(player.getOffhandItem());
        boolean ctrl = holdingTool && Screen.hasControlDown();
        boolean alt = holdingTool && Screen.hasAltDown();
        if (ctrl != lastCtrl || alt != lastAlt || (holdingTool && ++resendTimer >= 20)) {
            PacketDistributor.sendToServer(new SettingsToolKeysPayload(ctrl, alt));
            lastCtrl = ctrl;
            lastAlt = alt;
            resendTimer = 0;
        }

        tickCraneControls(minecraft, player);
    }

    private static boolean isKeyAwareTool(ItemStack stack) {
        return stack.is(HbmItems.SETTINGS_TOOL.get()) || stack.is(HbmItems.FLUID_IDENTIFIER_MULTI.get());
    }

    private static void tickCraneControls(Minecraft minecraft, Player player) {
        BlockPos cranePos = findActiveCraneConsole(player);
        boolean up = cranePos != null && minecraft.options.keyUp.isDown();
        boolean down = cranePos != null && minecraft.options.keyDown.isDown();
        boolean left = cranePos != null && minecraft.options.keyLeft.isDown();
        boolean right = cranePos != null && minecraft.options.keyRight.isDown();
        boolean load = cranePos != null && minecraft.options.keyJump.isDown();
        boolean changed = !java.util.Objects.equals(cranePos, lastCranePos)
                || up != lastCraneUp
                || down != lastCraneDown
                || left != lastCraneLeft
                || right != lastCraneRight
                || load != lastCraneLoad;
        if (cranePos != null && (changed || ++craneResendTimer >= 5)) {
            PacketDistributor.sendToServer(new RbmkCraneControlPayload(cranePos, up, down, left, right, load));
            craneResendTimer = 0;
        } else if (cranePos == null && lastCranePos != null) {
            PacketDistributor.sendToServer(new RbmkCraneControlPayload(lastCranePos, false, false, false, false, false));
            craneResendTimer = 0;
        }
        lastCranePos = cranePos;
        lastCraneUp = up;
        lastCraneDown = down;
        lastCraneLeft = left;
        lastCraneRight = right;
        lastCraneLoad = load;
    }

    private static BlockPos findActiveCraneConsole(Player player) {
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-3, -1, -3), playerPos.offset(3, 2, 3))) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineDummyBlockEntity dummy) {
                blockEntity = level.getBlockEntity(dummy.getCorePos());
            }
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk
                    && rbmk.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE
                    && rbmk.isPlayerInCraneOperationArea(player)) {
                return rbmk.getBlockPos();
            }
        }
        return null;
    }
}
